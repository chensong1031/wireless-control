#!/bin/bash
# GitHub Actions 推送脚本

set -e

echo "=========================================="
echo "GitHub Actions 推送助手"
echo "=========================================="
echo ""

# 检查 Git 配置
if ! git config user.name >/dev/null 2>&1; then
    echo "⚠️  未配置 Git 用户信息"
    read -p "请输入你的名字: " name
    git config user.name "$name"
    read -p "请输入你的邮箱: " email
    git config user.email "$email"
fi

echo ""
echo "当前 Git 配置："
echo "  用户: $(git config user.name)"
echo "  邮箱: $(git config user.email)"
echo ""

# 检查远程仓库
if ! git remote get-url origin >/dev/null 2>&1; then
    echo "📍 未配置远程仓库"
    read -p "请输入你的 GitHub 仓库地址 (例如: https://github.com/zhangsan/wireless-control.git): " repo_url

    if [ -z "$repo_url" ]; then
        echo "❌ 仓库地址不能为空"
        exit 1
    fi

    git remote add origin "$repo_url"
    echo "✓ 远程仓库已配置"
else
    echo "✓ 远程仓库已配置: $(git remote get-url origin)"
fi

echo ""
echo "📦 准备推送代码..."
echo ""

# 检查是否有未提交的更改
if [ -n "$(git status --porcelain)" ]; then
    echo "⚠️  有未提交的更改，先提交..."
    git add -A
    git commit -m "Add GitHub Actions workflow and build configuration"
    echo "✓ 更改已提交"
fi

echo "🚀 推送到 GitHub..."
echo ""

# 推送代码
git push -u origin main

echo ""
echo "=========================================="
echo "✅ 推送成功！"
echo "=========================================="
echo ""
echo "下一步："
echo "1. 访问你的 GitHub 仓库"
echo "2. 点击 Actions 标签"
echo "3. 等待构建完成（约 3-5 分钟）"
echo "4. 下载构建好的 APK"
echo ""
echo "仓库地址: $(git remote get-url origin)"
echo "Actions 地址: $(git remote get-url origin | sed 's|\.git$||')/actions"
echo ""
