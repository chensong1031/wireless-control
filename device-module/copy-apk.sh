#!/bin/bash

# 自动复制APK到workspace目录
# 用法: bash copy-apk.sh

WORKSPACE_ROOT="/workspace/projects/workspace"
APK_DIR="/workspace/projects/workspace/projects/apk"
SOURCE_APK="/workspace/projects/workspace-main/device-module/app/build/outputs/apk/debug/app-debug.apk"
TARGET_APK="$APK_DIR/worktone-latest.apk"

# 创建目录
mkdir -p "$APK_DIR"

# 检查源文件是否存在
if [ -f "$SOURCE_APK" ]; then
    # 复制APK
    cp "$SOURCE_APK" "$TARGET_APK"
    
    # 显示信息
    echo "✓ APK copied successfully!"
    echo "  Source: $SOURCE_APK"
    echo "  Target: $TARGET_APK"
    echo "  Size: $(du -h "$TARGET_APK" | cut -f1)"
else
    echo "⚠ APK file not found: $SOURCE_APK"
    exit 1
fi
