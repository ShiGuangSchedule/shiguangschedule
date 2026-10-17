#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成贡献者数据（等价于 CI 的 .github/scripts/build_contributors.sh）。

背景
----
贡献者页面读取的是 compose 资源 `files/contributors_data/contributors.json`（头像为同目录
`avatars/<id>.webp`）。这个资源**不在仓库里**，官方 CI（ci-build.yml 的 prepare-resources）会跑
`build_contributors.sh` 现场生成，再把 `composeResources` 打进构建。所以本地直接构建时该资源缺失，
贡献者页会显示「暂无贡献者」（ContributionRepository 已做兜底）。

本脚本用 Python 复刻同样的产出，方便本地构建也能看到完整贡献者：

    python tools/gen_contributors.py

产出：
    shared/src/commonMain/composeResources/files/contributors_data/contributors.json
    shared/src/commonMain/composeResources/files/contributors_data/avatars/<id>.webp  (48x48, webp q75)

说明：CI 构建时会 `rm -rf` 后重新生成，本目录内容不会影响 CI 结果。
"""

from __future__ import annotations

import io
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

sys.stdout.reconfigure(encoding="utf-8")

try:
    from PIL import Image
except ImportError:  # pragma: no cover
    print("需要 Pillow：pip install Pillow")
    raise SystemExit(1)

REPO_APP = "ShiGuangSchedule/shiguangschedule"
REPO_JIAOWU = "ShiGuangSchedule/shiguang_warehouse"

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BASE_DIR = os.path.join(
    REPO_ROOT, "shared", "src", "commonMain", "composeResources", "files", "contributors_data"
)
OUTPUT_JSON = os.path.join(BASE_DIR, "contributors.json")
AVATAR_DIR = os.path.join(BASE_DIR, "avatars")

AVATAR_SIZE = 48
AVATAR_QUALITY = 75

EXCLUDE_USERS = {"github-actions", "dependabot[bot]", "renovate[bot]", "some-ai-bot"}

USER_AGENT = "shiguangschedule-contributors-gen"


def http_get(url: str, timeout: int = 30) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=timeout) as response:
        return response.read()


def fetch_contributors(repo: str, label: str) -> list[dict]:
    print("--- 正在从 [%s] 获取全部贡献者列表... ---" % label, file=sys.stderr)
    collected: list[dict] = []
    page = 1

    while True:
        url = "https://api.github.com/repos/%s/contributors?per_page=100&page=%d" % (repo, page)
        print("正在读取第 %d 页..." % page, file=sys.stderr)
        try:
            raw = json.loads(http_get(url))
        except urllib.error.HTTPError as e:
            body = e.read().decode("utf-8", "replace")
            print("错误: 无法从 %s 获取第 %d 页 (HTTP %s): %s" % (label, page, e.code, body[:200]), file=sys.stderr)
            break
        except Exception as e:
            print("错误: 无法从 %s 获取第 %d 页 (%s)" % (label, page, e), file=sys.stderr)
            break

        if not isinstance(raw, list):
            print("错误: 返回内容不是数组，终止该仓库抓取", file=sys.stderr)
            break
        if not raw:
            break

        for item in raw:
            login = item.get("login")
            if not login or login in EXCLUDE_USERS:
                continue
            collected.append({
                "name": login,
                "url": item.get("html_url", ""),
                "id": item.get("id"),
                "avatar_url": item.get("avatar_url", ""),
            })

        if len(raw) < 100:
            break
        page += 1

    print("--- %s：共 %d 位贡献者 ---" % (label, len(collected)), file=sys.stderr)
    return collected


def download_avatar(contributor: dict) -> None:
    contributor_id = contributor["id"]
    login = contributor["name"]
    avatar_url = contributor["avatar_url"]
    if not avatar_url or contributor_id is None:
        print("    [跳过] 缺少头像信息: %s" % login, file=sys.stderr)
        return

    target = os.path.join(AVATAR_DIR, "%s.webp" % contributor_id)
    if os.path.exists(target):
        return

    separator = "&" if "?" in avatar_url else "?"
    resized = "%s%ss=%d" % (avatar_url, separator, AVATAR_SIZE)
    try:
        raw = http_get(resized)
    except Exception as e:
        print("    [跳过] 头像下载失败 %s (%s)" % (login, e), file=sys.stderr)
        return

    try:
        image = Image.open(io.BytesIO(raw)).convert("RGB")
        image = image.resize((AVATAR_SIZE, AVATAR_SIZE), Image.LANCZOS)
        image.save(target, format="WEBP", quality=AVATAR_QUALITY, method=6)
        print("    [下载+压缩] %s.webp (%s) -> %d bytes" % (contributor_id, login, os.path.getsize(target)),
              file=sys.stderr)
    except Exception as e:
        print("    [跳过] 头像转换失败 %s (%s)" % (login, e), file=sys.stderr)
        if os.path.exists(target):
            os.remove(target)


def to_output(contributors: list[dict]) -> list[dict]:
    return [
        {
            "name": item["name"],
            "url": item["url"],
            "avatar": "avatars/%s.webp" % item["id"],
        }
        for item in contributors
    ]


def main() -> int:
    os.makedirs(AVATAR_DIR, exist_ok=True)

    started = time.time()
    app_dev = fetch_contributors(REPO_APP, "App 开发")
    adapter_dev = fetch_contributors(REPO_JIAOWU, "教务适配")

    if not app_dev and not adapter_dev:
        print("两个仓库都没取到贡献者，未写入文件（检查网络或 GitHub API 限额）", file=sys.stderr)
        return 1

    for contributor in app_dev + adapter_dev:
        download_avatar(contributor)

    payload = {
        "app_dev": to_output(app_dev),
        "jiaowu_adapter": to_output(adapter_dev),
    }
    with open(OUTPUT_JSON, "w", encoding="utf-8") as handle:
        json.dump(payload, handle, ensure_ascii=False, indent=2)

    avatar_count = len(os.listdir(AVATAR_DIR))
    print("")
    print("已生成: %s" % OUTPUT_JSON)
    print("App 开发贡献者 : %d" % len(app_dev))
    print("教务适配贡献者 : %d" % len(adapter_dev))
    print("头像文件       : %d 个" % avatar_count)
    print("耗时           : %.1fs" % (time.time() - started))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
