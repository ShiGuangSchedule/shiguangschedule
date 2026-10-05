#!/bin/bash

# 配置信息
REPO_APP="ShiGuangSchedule/shiguangschedule"
REPO_JIAOWU="ShiGuangSchedule/shiguang_warehouse"

API_URL_APP="https://api.github.com/repos/${REPO_APP}/contributors"
API_URL_JIAOWU="https://api.github.com/repos/${REPO_JIAOWU}/contributors"

CONTRIBUTORS_BASE_DIR="./shared/src/commonMain/composeResources/files/contributors_data"
OUTPUT_JSON_FILE="${CONTRIBUTORS_BASE_DIR}/contributors.json"
AVATAR_DIR="${CONTRIBUTORS_BASE_DIR}/avatars"

# 头像压缩参数
AVATAR_SIZE=48
AVATAR_QUALITY=75

# 排除的贡献者用户名
EXCLUDE_USERS_ARRAY=(
    "github-actions"
    "dependabot[bot]"
    "renovate[bot]"
    "some-ai-bot"
)

EXCLUDE_JSON=$(printf "%s\n" "${EXCLUDE_USERS_ARRAY[@]}" | jq -R . | jq -s .)

# 依赖检查
command -v curl  >/dev/null || { echo "错误: 需要 'curl' 工具" >&2; exit 1; }
command -v jq    >/dev/null || { echo "错误: 需要 'jq' 工具来解析 JSON" >&2; exit 1; }
command -v cwebp >/dev/null || { echo "错误: 需要 'cwebp' 工具来压缩头像" >&2; exit 1}

# 初始化目录
echo "--- 正在初始化目录和文件... ---" >&2

rm -rf "${CONTRIBUTORS_BASE_DIR}"
mkdir -p "${AVATAR_DIR}"

# 核心处理函数
fetch_and_process_repo() {
    local API_URL="$1"
    local REPO_NAME="$2"

    local PAGE=1
    local PER_PAGE=100
    local RAW_DATA="[]"

    echo "--- 正在从 [${REPO_NAME}] 获取全部贡献者列表... ---" >&2

    # 分页抓取
    while true; do
        echo "正在读取第 ${PAGE} 页..." >&2

        local PAGE_DATA
        PAGE_DATA=$(curl -sL -f "${API_URL}?per_page=${PER_PAGE}&page=${PAGE}")

        if [ -z "$PAGE_DATA" ] || ! echo "$PAGE_DATA" | jq -e 'if type == "array" then true else false end' >/dev/null 2>&1; then
            local ERR_MSG
            ERR_MSG=$(echo "$PAGE_DATA" | jq -r '.message? // "未知错误/格式非法"')
            echo "错误: 无法从 ${REPO_NAME} 获取第 ${PAGE} 页数据 (原因: ${ERR_MSG})，终止此仓库抓取。" >&2
            break
        fi

        local DATA_LENGTH
        DATA_LENGTH=$(echo "$PAGE_DATA" | jq '. | length')
        if [ "$DATA_LENGTH" -eq 0 ]; then
            break
        fi

        RAW_DATA=$(jq -n --argjson base "$RAW_DATA" --argjson new "$PAGE_DATA" '$base + $new')

        if [ "$DATA_LENGTH" -lt "$PER_PAGE" ]; then
            break
        fi

        PAGE=$((PAGE + 1))
    done

    if [ "$RAW_DATA" = "[]" ]; then
        echo "警告: 没有从 ${REPO_NAME} 获取到任何数据。" >&2
        echo "[]"
        return
    fi

    # 过滤并格式化
    local FINAL_LIST
    FINAL_LIST=$(echo "$RAW_DATA" | jq --argjson EXCLUDES "$EXCLUDE_JSON" -c '
        map(
            select(.login != null)
            | select((.login | IN($EXCLUDES[])) | not)
        )
        | map({
            name:       .login,
            url:        .html_url,
            id:         .id,
            avatar_url: .avatar_url
        })
    ')

    echo "--- 成功获取数据，正在下载并压缩头像 (${AVATAR_SIZE}x${AVATAR_SIZE} WebP, q=${AVATAR_QUALITY})... ---" >&2

    # 下载并压缩头像
    echo "$FINAL_LIST" | jq -c '.[]' | while read -r contributor; do
        local ID
        local LOGIN
        local AVATAR_URL_BASE
        local AVATAR_URL_RESIZED
        local FINAL_FILE
        local TEMP_RAW

        ID=$(echo "$contributor" | jq -r '.id')
        LOGIN=$(echo "$contributor" | jq -r '.name')
        AVATAR_URL_BASE=$(echo "$contributor" | jq -r '.avatar_url')

        if [[ "$AVATAR_URL_BASE" == *"?"* ]]; then
            AVATAR_URL_RESIZED="${AVATAR_URL_BASE}&s=${AVATAR_SIZE}"
        else
            AVATAR_URL_RESIZED="${AVATAR_URL_BASE}?s=${AVATAR_SIZE}"
        fi

        FINAL_FILE="${AVATAR_DIR}/${ID}.webp"
        TEMP_RAW="/tmp/avatar_${ID}_raw"

        if [ ! -f "${FINAL_FILE}" ]; then
            echo "    [下载+压缩] ${ID}.webp  (${LOGIN})" >&2

            if ! curl -sf -o "${TEMP_RAW}" "${AVATAR_URL_RESIZED}"; then
                echo "    [跳过] 下载失败: ${LOGIN}" >&2
                rm -f "${TEMP_RAW}"
                continue
            fi

            if ! cwebp -quiet \
                    -resize "${AVATAR_SIZE}" "${AVATAR_SIZE}" \
                    -q "${AVATAR_QUALITY}" \
                    -m 6 \
                    -sharp_yuv \
                    -preset photo \
                    -f 0 \
                    "${TEMP_RAW}" \
                    -o "${FINAL_FILE}"; then
                echo "    [警告] cwebp 压缩失败: ${LOGIN}" >&2
                rm -f "${TEMP_RAW}" "${FINAL_FILE}"
                continue
            fi

            rm -f "${TEMP_RAW}"

            local SIZE_BYTES
            SIZE_BYTES=$(stat -c%s "${FINAL_FILE}" 2>/dev/null || echo "?")
            echo "      -> ${SIZE_BYTES} bytes" >&2
        fi
    done

    # 输出最终 JSON
    echo "$FINAL_LIST" | jq -c '
        map({
            name:   .name,
            url:    .url,
            avatar: ("avatars/" + (.id | tostring) + ".webp")
        })
    '
}

# 执行流程
APP_DEV_LIST=$(fetch_and_process_repo "${API_URL_APP}" "App 开发")
JIAOWU_ADAPTER_LIST=$(fetch_and_process_repo "${API_URL_JIAOWU}" "教务适配")

jq -n \
    --argjson app_dev "$APP_DEV_LIST" \
    --argjson jiaowu_adapter "$JIAOWU_ADAPTER_LIST" \
    '{
        app_dev: $app_dev,
        jiaowu_adapter: $jiaowu_adapter
    }' > "${OUTPUT_JSON_FILE}"

echo "--- 任务完成，数据已生成至 ${OUTPUT_JSON_FILE}。 ---" >&2