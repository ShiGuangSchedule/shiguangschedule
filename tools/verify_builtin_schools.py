#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""校验内置学校索引片段，以及它与离线索引拼接后的合并结果。

模拟 `SchoolRepository.loadIndex()` 的行为：把 `builtin_schools.pb` 直接追加到本地索引字节之后，
按 Protobuf 规范 repeated 字段重复出现即视为追加，因此学校列表应当是两者的并集，
而 protocol_version / version_id 必须仍然来自本地索引。

用法：
    python tools/verify_builtin_schools.py
退出码 0 表示通过。
"""

from __future__ import annotations

import os
import sys

sys.stdout.reconfigure(encoding="utf-8")

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FRAGMENT = os.path.join(
    REPO_ROOT, "shared", "src", "commonMain", "composeResources", "files", "builtin_schools.pb"
)
LOCAL_INDEX = os.path.join(REPO_ROOT, "shared", "assets", "offline_repo", "index", "school_index.pb")

SCHOOL_NAMES = {1: "id", 2: "name", 3: "initial", 4: "resource_folder"}
ADAPTER_NAMES = {1: "adapter_id", 2: "adapter_name", 3: "category", 4: "asset_js_path",
                 5: "import_url", 6: "description", 7: "maintainer"}


def read_varint(data: bytes, index: int):
    result = 0
    shift = 0
    while True:
        byte = data[index]
        index += 1
        result |= (byte & 0x7F) << shift
        if not byte & 0x80:
            return result, index
        shift += 7


def parse_fields(data: bytes):
    """产出 (字段号, wire_type, 值)；字符串/子消息返回 bytes。"""
    index = 0
    out = []
    while index < len(data):
        key, index = read_varint(data, index)
        field, wire_type = key >> 3, key & 7
        if wire_type == 0:
            value, index = read_varint(data, index)
        elif wire_type == 2:
            length, index = read_varint(data, index)
            value = data[index:index + length]
            index += length
        elif wire_type == 5:
            value = data[index:index + 4]
            index += 4
        elif wire_type == 1:
            value = data[index:index + 8]
            index += 8
        else:
            raise ValueError("不支持的 wire type: %d" % wire_type)
        out.append((field, wire_type, value))
    return out


def text(raw: bytes) -> str:
    return raw.decode("utf-8")


def decode_school_index(data: bytes):
    """返回 (protocol_version, version_id, [学校 dict])。"""
    protocol_version = 0
    version_id = ""
    schools = []
    for field, _wire_type, value in parse_fields(data):
        if field == 1:
            protocol_version = value
        elif field == 2:
            version_id = text(value)
        elif field == 3:
            schools.append(decode_school(value))
    return protocol_version, version_id, schools


def decode_school(data: bytes):
    school = {"adapters": []}
    for field, wire_type, value in parse_fields(data):
        if field == 5:
            school["adapters"].append(decode_adapter(value))
        else:
            school[SCHOOL_NAMES.get(field, "field%d" % field)] = text(value) if wire_type == 2 else value
    return school


def decode_adapter(data: bytes):
    adapter = {}
    for field, wire_type, value in parse_fields(data):
        adapter[ADAPTER_NAMES.get(field, "field%d" % field)] = text(value) if wire_type == 2 else value
    return adapter


def describe(label: str, data: bytes):
    protocol_version, version_id, schools = decode_school_index(data)
    print("=== %s (%d 字节)" % (label, len(data)))
    print("  protocol_version = %s" % protocol_version)
    print("  version_id       = %s" % (version_id or "(空)"))
    for school in schools:
        print("  School %s / %s (initial=%s, folder=%s)"
              % (school.get("id"), school.get("name"), school.get("initial"), school.get("resource_folder")))
        for adapter in school["adapters"]:
            print("    Adapter %s | %s | category=%s | %s"
                  % (adapter.get("adapter_id"), adapter.get("adapter_name"),
                     adapter.get("category"), adapter.get("asset_js_path")))
            print("      import_url  = %s" % adapter.get("import_url"))
            print("      maintainer  = %s" % adapter.get("maintainer"))
    return protocol_version, version_id, schools


def main() -> int:
    for path in (FRAGMENT, LOCAL_INDEX):
        if not os.path.exists(path):
            print("缺少文件: %s" % path)
            return 1

    fragment = open(FRAGMENT, "rb").read()
    local = open(LOCAL_INDEX, "rb").read()

    _, _, fragment_schools = describe("builtin_schools.pb（内置片段）", fragment)
    local_version, local_version_id, local_schools = describe("offline_repo/index/school_index.pb（本地索引）", local)
    merged_version, merged_version_id, merged_schools = describe(
        "拼接后 = 本地索引 + 内置片段（运行期实际解码的输入）", local + fragment
    )

    problems = []
    if not fragment_schools:
        problems.append("内置片段里没有学校")
    if merged_version != local_version or merged_version_id != local_version_id:
        problems.append("拼接后 protocol_version/version_id 被片段污染")

    local_ids = [s.get("id") for s in local_schools]
    merged_ids = [s.get("id") for s in merged_schools]
    expected_ids = local_ids + [s.get("id") for s in fragment_schools]
    if merged_ids != expected_ids:
        problems.append("拼接后的学校列表不等于两者并集：%s != %s" % (merged_ids, expected_ids))

    for school in fragment_schools:
        if not school.get("id") or not school.get("name") or not school.get("resource_folder"):
            problems.append("内置学校缺少 id/name/resource_folder: %s" % school)
        for adapter in school["adapters"]:
            if not adapter.get("asset_js_path"):
                problems.append("适配器缺少 asset_js_path: %s" % adapter)
            if adapter.get("category") not in (1, 2, 3):
                problems.append("适配器 category 非预期: %s" % adapter)

    print()
    if problems:
        print("校验失败：")
        for item in problems:
            print("  - %s" % item)
        return 1

    print("校验通过：内置片段可解析，拼接后 version_id 保持本地值，学校列表为并集")
    print("  本地索引学校 : %s" % ", ".join(local_ids))
    print("  内置学校     : %s" % ", ".join(s.get("id") for s in fragment_schools))
    return 0


if __name__ == "__main__":
    sys.exit(main())
