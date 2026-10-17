#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""把 App 内置适配脚本同步成「适配仓库提交包」里的副本。

官方适配仓库（shiguang_warehouse）里，脚本与 adapters.yaml 必须放在
`resources/<resource_folder>/` 下；而本仓库把它作为 App 内置资源维护在
`shared/src/commonMain/composeResources/files/builtin_NWAFU_wdkbby.js`。

为了保持单一来源，本脚本负责把内置脚本复制成提交包所需的 `wdkbby.js`：

    python tools/warehouse-submission/sync.py

提交 PR 前跑一次即可。两个文件内容应完全一致（可用 --check 校验，不一致时退出码 1）。
"""

from __future__ import annotations

import filecmp
import os
import shutil
import sys

sys.stdout.reconfigure(encoding="utf-8")

HERE = os.path.dirname(os.path.abspath(__file__))
REPO_ROOT = os.path.dirname(os.path.dirname(HERE))

SOURCE = os.path.join(
    REPO_ROOT, "shared", "src", "commonMain", "composeResources", "files", "builtin_NWAFU_wdkbby.js"
)
TARGET = os.path.join(HERE, "NWAFU", "wdkbby.js")


def main() -> int:
    if not os.path.exists(SOURCE):
        print("找不到内置适配脚本: %s" % SOURCE)
        return 1

    check_only = "--check" in sys.argv

    if check_only:
        if not os.path.exists(TARGET):
            print("提交包里缺少 wdkbby.js（请先运行 python tools/warehouse-submission/sync.py）")
            return 1
        if not filecmp.cmp(SOURCE, TARGET, shallow=False):
            print("提交包的 wdkbby.js 与内置脚本不一致（请重新运行 sync.py）")
            return 1
        print("一致：NWAFU/wdkbby.js == builtin_NWAFU_wdkbby.js")
        return 0

    os.makedirs(os.path.dirname(TARGET), exist_ok=True)
    shutil.copyfile(SOURCE, TARGET)
    print("已同步: %s" % TARGET)
    print("大小  : %d 字节" % os.path.getsize(TARGET))
    return 0


if __name__ == "__main__":
    sys.exit(main())
