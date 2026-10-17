#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成 App 内置学校索引片段（builtin_schools.pb）。

背景
----
拾光课程表启动时会把 `shared/assets/offline_repo` 打包成的 offline_schools.zip 解压到
`<filesDir>/repo`，学校列表来自 `repo/index/school_index.pb`（来自适配仓库，可被远程覆盖）。
为了让「内置适配」不依赖适配仓库、且在仓库同步后也不会消失，App 会在读取索引时把本脚本
生成的片段**字节拼接到本地索引之后**：片段只包含 `SchoolIndex.schools`（字段 3）的编码，
按 Protobuf 规范 repeated 字段重复出现即视为追加，因此两份数据会被合并成同一份学校列表，
而本地索引的 protocol_version / version_id（字段 1/2）不受影响。

用法
----
    python tools/gen_builtin_schools.py

输出
----
    shared/src/commonMain/composeResources/files/builtin_schools.pb

对应关系（勿单独修改，需与适配脚本/文档保持一致）：
    School.id              = NWAFU
    School.resource_folder = NWAFU
    Adapter.asset_js_path  = wdkbby.js
    运行期落盘位置          = <filesDir>/repo/schools/resources/NWAFU/wdkbby.js
    （由 ResourceInitializerManager 从 composeResources/files/builtin_NWAFU_wdkbby.js 复制）
"""

from __future__ import annotations

import os
import sys

# --- proto 字段号（见 shared/src/commonMain/proto/school_index.proto） ---
ADAPTER_CATEGORY_BACHELOR_AND_ASSOCIATE = 2  # BACHELOR_AND_ASSOCIATE

# 需要内置的学校列表（新增内置适配时在这里追加条目）
SCHOOLS = [
    {
        "id": "NWAFU",
        "name": "西北农林科技大学",
        "initial": "X",
        "resource_folder": "NWAFU",
        "adapters": [
            {
                "adapter_id": "NWAFU_WDKBBY_01",
                "adapter_name": "ehall 新版教务（我的课表）",
                "category": ADAPTER_CATEGORY_BACHELOR_AND_ASSOCIATE,
                "asset_js_path": "wdkbby.js",
                # 按官方仓库规范：import_url 填登录入口。
                # 西北农林科技大学已迁移到新版门户 newehall（旧 ehall 域名/路径会 404），
                # 这里填新门户首页，学生登录后自行进入「我的课表」再点执行导入。
                "import_url": "https://newehall.nwafu.edu.cn/",
                "description": "西北农林科技大学新版门户（newehall）ehall 教务「我的课表」："
                               "登录新门户后进入「我的课表」，再点击执行导入。"
                               "接口优先、页面解析兜底，导入课程与学期总周数（暂不含作息时间与调课）。",
                "maintainer": "内置适配",
            }
        ],
    },
]


def varint(value: int) -> bytes:
    out = bytearray()
    while True:
        byte = value & 0x7F
        value >>= 7
        if value:
            out.append(byte | 0x80)
        else:
            out.append(byte)
            return bytes(out)


def tag(field_number: int, wire_type: int) -> bytes:
    return varint((field_number << 3) | wire_type)


def string_field(field_number: int, value: str) -> bytes:
    data = value.encode("utf-8")
    return tag(field_number, 2) + varint(len(data)) + data


def int_field(field_number: int, value: int) -> bytes:
    return tag(field_number, 0) + varint(value)


def message_field(field_number: int, payload: bytes) -> bytes:
    return tag(field_number, 2) + varint(len(payload)) + payload


def encode_adapter(adapter: dict) -> bytes:
    out = bytearray()
    out += string_field(1, adapter["adapter_id"])
    out += string_field(2, adapter["adapter_name"])
    out += int_field(3, adapter["category"])
    out += string_field(4, adapter["asset_js_path"])
    if adapter.get("import_url"):
        out += string_field(5, adapter["import_url"])
    out += string_field(6, adapter["description"])
    out += string_field(7, adapter["maintainer"])
    return bytes(out)


def encode_school(school: dict) -> bytes:
    out = bytearray()
    out += string_field(1, school["id"])
    out += string_field(2, school["name"])
    out += string_field(3, school["initial"])
    out += string_field(4, school["resource_folder"])
    for adapter in school["adapters"]:
        out += message_field(5, encode_adapter(adapter))
    return bytes(out)


def build_fragment() -> bytes:
    """只编码 SchoolIndex.schools（字段 3），供运行期与本地索引字节拼接。"""
    out = bytearray()
    for school in SCHOOLS:
        out += message_field(3, encode_school(school))
    return bytes(out)


def main() -> int:
    repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    target = os.path.join(
        repo_root,
        "shared", "src", "commonMain", "composeResources", "files", "builtin_schools.pb",
    )
    fragment = build_fragment()
    os.makedirs(os.path.dirname(target), exist_ok=True)
    with open(target, "wb") as handle:
        handle.write(fragment)

    print("已生成: %s (%d 字节)" % (target, len(fragment)))
    print("学校数: %d -> %s" % (len(SCHOOLS), ", ".join(s["id"] for s in SCHOOLS)))
    print("hex   : %s" % fragment.hex())
    return 0


if __name__ == "__main__":
    sys.exit(main())
