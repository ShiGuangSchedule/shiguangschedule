import subprocess
import os
import re
import sys
from datetime import date

# --- 1. 常量与配置 ---

# Conventional Commits 类型与 CHANGELOG 分类标题的映射
COMMIT_TYPES = {
    'feat': '✨ 新增功能 (Features)',
    'fix': '🐛 Bug 修复 (Bug Fixes)',
    'improve': '💡 功能与体验优化 (Improvements)',
    'perf': '🚀 性能与代码改进 (Improvements)',
    'refactor': '🚀 性能与代码改进 (Improvements)',
    'style': '🚀 性能与代码改进 (Improvements)',
}

# 不列入 CHANGELOG 的提交类型
EXCLUDED_TYPES = ['chore', 'ci', 'build', 'test', 'docs']

# 未匹配到上述类型时的默认分类
OTHER_CATEGORY = '🚧 其他提交 (Other Commits)'

CHANGELOG_PATH = 'CHANGELOG.md'

# action 目录,用于解析 footer 的相对路径
ACTION_DIR = os.path.dirname(os.path.abspath(__file__))


# --- 2. 辅助函数 ---

def get_latest_tag(is_prerelease=False):
    """
    获取离 HEAD 最近的前一个 Tag。

    :param is_prerelease: 预发布时允许任意 Tag 作基准;正式版仅取纯数字正式 Tag。
    :return: Tag 名称,或 None
    """
    if is_prerelease:
        # 预发布:取 HEAD~1 的最近 Tag,避免匹配到刚打在当前 HEAD 的 Tag
        try:
            output = subprocess.check_output(
                'git describe --tags --abbrev=0 HEAD~1',
                shell=True, text=True, encoding='utf-8', stderr=subprocess.DEVNULL
            ).strip()
            if output:
                return output
        except Exception:
            pass

        # 兜底:取当前仓库最近 Tag
        try:
            output = subprocess.check_output(
                'git describe --tags --abbrev=0',
                shell=True, text=True, encoding='utf-8', stderr=subprocess.DEVNULL
            ).strip()
            return output or None
        except Exception:
            return None

    # 正式版:按创建时间倒序取第一个纯数字版本号,过滤 rc/beta 等预发布 Tag
    try:
        output = subprocess.check_output(
            'git tag --sort=-creatordate',
            shell=True, text=True, encoding='utf-8', stderr=subprocess.DEVNULL
        ).strip()
        if not output:
            return None

        official_pattern = re.compile(r'^v?\d+\.\d+\.\d+$')
        for tag in output.split('\n'):
            tag = tag.strip()
            if official_pattern.match(tag):
                return tag
        return None
    except Exception:
        return None


def get_first_commit_hash():
    """获取仓库初始 Commit Hash,用于无历史 Tag 时的全量提取"""
    try:
        return subprocess.check_output(
            'git rev-list --max-parents=0 HEAD',
            shell=True, text=True, encoding='utf-8'
        ).strip()
    except Exception:
        return None


def is_valid_ref(ref):
    """校验 Git 引用/Tag 是否在仓库中真实存在"""
    if not ref:
        return False
    try:
        subprocess.check_output(
            f'git rev-parse --verify {ref}',
            shell=True, text=True, encoding='utf-8', stderr=subprocess.DEVNULL
        )
        return True
    except Exception:
        return False


# --- 3. 核心逻辑 ---

def generate_changelog(version_title, previous_tag=None, is_prerelease=False, footer_path=None):
    """
    分析 Git 提交记录并生成 CHANGELOG.md。

    :param version_title: 版本大标题 (例: "v1.0.1")
    :param previous_tag: 对比基准 Tag。为 None 时自动推导
    :param is_prerelease: 是否预发布,影响基准 Tag 的筛选策略
    :param footer_path: 附加说明文件路径。相对路径基于 action 目录解析;
                        为空或文件不存在则跳过
    """
    # 1. 自动推导基准 Tag
    if not previous_tag:
        previous_tag = get_latest_tag(is_prerelease=is_prerelease)
        if previous_tag:
            print(f"自动识别到的对比版本标签: {previous_tag}")

    # 2. 基准无效时降级为全量提取
    if previous_tag and not is_valid_ref(previous_tag):
        print(f"警告:对比标签 '{previous_tag}' 不存在,将从首个提交开始计算。")
        previous_tag = None

    # 3. 确定 git log 范围
    if not previous_tag:
        initial_commit = get_first_commit_hash()
        range_str = f"{initial_commit}...HEAD" if initial_commit else "HEAD"
    else:
        range_str = f"{previous_tag}...HEAD"

    # 4. 拉取提交历史
    log_format = '%H|||%s|||%an'
    log_command = f'git -c i18n.logOutputEncoding=UTF-8 log --pretty=format:"{log_format}" {range_str}'
    try:
        logs_output = subprocess.check_output(
            log_command, shell=True, text=True, encoding='utf-8'
        ).strip()
        logs = logs_output.split('\n')
    except Exception as e:
        print(f"执行 git log 失败: {e}")
        return

    # 5. 解析 Conventional Commits 并分类
    categories = {}
    commit_regex = re.compile(r'^(\w+)(?:\([^)]+\))?[:：]\s*(.*)', re.UNICODE)

    for log in logs:
        if not log or '|||' not in log:
            continue
        parts = log.split('|||')
        if len(parts) < 3:
            continue

        subject = parts[1]
        match = commit_regex.match(subject)

        description = subject
        category_title = OTHER_CATEGORY

        if match:
            type_prefix = match.group(1).lower()
            if type_prefix in EXCLUDED_TYPES:
                continue
            description = match.group(2)
            category_title = COMMIT_TYPES.get(type_prefix, OTHER_CATEGORY)
        else:
            # 非规范格式:过滤 Merge 及带排除前缀的提交
            if subject.startswith('Merge ') or any(
                subject.startswith(f"{ex}:") or subject.startswith(f"{ex}：")
                for ex in EXCLUDED_TYPES
            ):
                continue

        categories.setdefault(category_title, []).append(f"- {description}")

    # 6. 拼装 Markdown
    new_changelog = f"## {version_title}\n\n"

    # 分类显示顺序
    ordered_titles = [
        COMMIT_TYPES['feat'],
        COMMIT_TYPES['fix'],
        COMMIT_TYPES['improve'],
        COMMIT_TYPES['perf'],
        OTHER_CATEGORY,
    ]

    has_content = False
    for title in ordered_titles:
        if categories.get(title):
            new_changelog += f"### {title}\n\n"
            new_changelog += '\n'.join(categories[title]) + '\n\n'
            has_content = True

    if not has_content:
        print("警告:指定范围内没有有效提交,跳过文件生成。")
        return

    # 7. 追加附加说明(footer)
    if footer_path:
        resolved_footer = (
            footer_path if os.path.isabs(footer_path)
            else os.path.join(ACTION_DIR, footer_path)
        )
        if os.path.isfile(resolved_footer):
            with open(resolved_footer, 'r', encoding='utf-8') as f:
                footer_content = f.read().strip()
            if footer_content:
                new_changelog += "\n" + footer_content + "\n"
                print(f"已追加附加说明: {resolved_footer}")
            else:
                print(f"附加说明为空,已跳过: {resolved_footer}")
        else:
            print(f"未找到附加说明,已跳过: {resolved_footer}")
    else:
        print("未指定附加说明,已跳过。")

    # 8. 写入文件
    with open(CHANGELOG_PATH, 'w', encoding='utf-8') as f:
        f.write(new_changelog)

    print(f"CHANGELOG.md 已更新: {version_title}")


# --- 4. 命令行入口 ---

if __name__ == '__main__':
    try:
        v_title = sys.argv[1] if len(sys.argv) > 1 else f"v{date.today().isoformat()}"
        p_tag = sys.argv[2] if len(sys.argv) > 2 and sys.argv[2] != '' else None
        is_pre = sys.argv[3].lower() == 'true' if len(sys.argv) > 3 else False
        f_path = sys.argv[4] if len(sys.argv) > 4 and sys.argv[4] != '' else None

        generate_changelog(v_title, p_tag, is_prerelease=is_pre, footer_path=f_path)
    except Exception:
        import traceback
        traceback.print_exc(file=sys.stdout)
        sys.exit(1)