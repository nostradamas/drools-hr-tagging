#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
实体表与建表脚本一致性检查（Issue #23 防回归）。

规则：后端每个实体类 @TableName 声明的表，必须在 database/create_tables.sql
中有对应的 CREATE TABLE。脚本中允许存在无实体的表（白名单 NON_ENTITY_TABLES，
如由触发器/配置逻辑使用的表），但实体表缺失即判定失败。

用法（仓库根目录）：python3 scripts/check_entity_tables.py
退出码：0 一致；1 不一致（缺失的表名会逐行打出）。
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ENTITY_DIR = ROOT / "drools-hr-tagging-backend" / "src" / "main" / "java" / "com" / "northland" / "drools" / "entity"
SQL_FILE = ROOT / "database" / "create_tables.sql"

# 脚本中存在、但没有对应实体类的表（触发器维护的变更历史表、系统配置表）
NON_ENTITY_TABLES = {"tag_change_history", "system_config"}


def entity_tables():
    tables = {}
    for java_file in sorted(ENTITY_DIR.glob("*.java")):
        text = java_file.read_text(encoding="utf-8")
        m = re.search(r'@TableName\("([^"]+)"\)', text)
        if m:
            tables[m.group(1)] = java_file.name
    return tables


def sql_tables():
    text = SQL_FILE.read_text(encoding="utf-8")
    # 兼容 CREATE TABLE `x` 与 CREATE TABLE IF NOT EXISTS `x`
    return set(re.findall(r"CREATE TABLE(?:\s+IF NOT EXISTS)?\s+`([^`]+)`", text))


def main():
    entities = entity_tables()
    created = sql_tables()

    if not entities:
        print("ERROR: 未在实体目录解析到任何 @TableName，路径可能已变更：%s" % ENTITY_DIR)
        return 1

    missing = sorted(set(entities) - created)
    extra = sorted(created - set(entities) - NON_ENTITY_TABLES)

    for name in sorted(entities):
        status = "OK " if name in created else "MISS"
        print("[%s] %-20s <- %s" % (status, name, entities[name]))

    if extra:
        print("WARN: 脚本中存在既无实体、也不在白名单中的表：%s（请确认是否应加入 NON_ENTITY_TABLES）" % ", ".join(extra))

    if missing:
        print("\nFAIL: 以下实体表未在 %s 中建表：%s" % (SQL_FILE.name, ", ".join(missing)))
        return 1

    print("\nPASS: %d 张实体表全部在 %s 中建表。" % (len(entities), SQL_FILE.name))
    return 0


if __name__ == "__main__":
    sys.exit(main())
