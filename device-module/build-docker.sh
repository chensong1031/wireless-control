#!/bin/bash
# 使用 Docker 构建 APK（无需配置本地环境）

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "使用 Docker 构建 APK"
echo "=========================================="

# 检查 Docker
if ! command -v docker &> /dev/null; then
    echo "错误：未安装 Docker"
    echo "请先安装 Docker: https://docs.docker.com/get-docker/"
    exit 1
fi

# 创建 Dockerfile
cat > Dockerfile.build << 'EOF'
FROM openjdk:17-jdk-slim

# 安装 Android SDK
RUN apt-get update && apt-get install -y wget unzip git

# 下载 Android SDK
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH=${PATH}:${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools

RUN mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip -O /tmp/cmdline-tools.zip && \
    unzip -q /tmp/cmdline-tools.zip -d ${ANDROID_SDK_ROOT}/cmdline-tools && \
    mv ${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools ${ANDROID_SDK_ROOT}/cmdline-tools/latest

# 接受许可证并安装必要组件
RUN yes | sdkmanager --licenses && \
    sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

WORKDIR /workspace

# 构建命令
CMD ["./gradlew", "assembleDebug"]
EOF

echo "1. 构建 Docker 镜像..."
docker build -t wireless-control-builder -f Dockerfile.build .

echo ""
echo "2. 运行构建..."
docker run --rm \
    -v "$SCRIPT_DIR:/workspace" \
    -v "$HOME/.gradle:/root/.gradle" \
    wireless-control-builder \
    ./gradlew assembleDebug --stacktrace

echo ""
echo "3. 复制构建结果..."
mkdir -p deploy
cp app/build/outputs/apk/debug/app-debug.apk deploy/

echo ""
echo "=========================================="
echo "✅ 构建完成！"
echo "=========================================="
echo ""
echo "APK 位置: deploy/app-debug.apk"
echo ""
echo "安装命令:"
echo "  adb install -r deploy/app-debug.apk"
echo ""

# 清理
rm Dockerfile.build
