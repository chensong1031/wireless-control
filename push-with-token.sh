#!/bin/bash
# 推送代码到 GitHub (需要 Personal Access Token)

set -e

REPO_URL="https://github.com/chensong1031/wireless-control.git"

echo "=========================================="
echo "推送代码到 GitHub"
echo "=========================================="
echo ""
echo "仓库地址: $REPO_URL"
echo ""

# 检查是否提供了 token
if [ -z "$1" ]; then
    echo "❌ 错误：需要提供 GitHub Personal Access Token"
    echo ""
    echo "获取方法："
    echo "1. 访问 https://github.com/settings/tokens"
    echo "2. 点击 'Generate new token' → 'Generate new token (classic)'"
    echo "3. 勾选 'repo' 权限"
    echo "4. 生成并复制 token"
    echo ""
    echo "然后运行："
    echo "  $0 你的token"
    echo ""
    exit 1
fi

TOKEN="$1"

# 设置远程地址（带 token）
git remote set-url origin "https://$TOKEN@github.com/chensong1031/wireless-control.git"

echo "🚀 正在推送..."
git push -u origin main

if [ $? -eq 0 ]; then
    echo ""
    echo "=========================================="
    echo "✅ 推送成功！"
    echo "=========================================="
    echo ""
    echo "查看构建状态："
    echo "  https://github.com/chensong1031/wireless-control/actions"
    echo ""
    echo "构建完成后下载 APK（约 3-5 分钟）"
    echo ""
else
    echo ""
    echo "❌ 推送失败，请检查 token 是否正确"
    echo ""
fi

# 恢复原始地址（移除 token）
git remote set-url origin "$REPO_URL"
