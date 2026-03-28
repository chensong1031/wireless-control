#!/bin/bash

# 设备端模块构建脚本
# 用途：构建Debug和Release版本的APK

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 打印函数
print_step() {
    echo -e "${GREEN}[${1}/6]${NC} $2"
}

print_success() {
    echo -e "${GREEN}  ✓${NC} $1"
}

print_error() {
    echo -e "${RED}  ✗${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}  ⚠${NC} $1"
}

echo "=========================================="
echo "无线群控系统 - 设备端模块构建"
echo "=========================================="
echo ""

# 检查环境
print_step "1" "检查构建环境..."

if ! command -v java &> /dev/null; then
    print_error "未找到Java"
    echo "  请先安装Java JDK"
    exit 1
fi
print_success "Java已安装"

if [ -f "./gradlew" ]; then
    print_success "Gradle wrapper已就绪"
else
    print_warning "Gradle wrapper不存在，尝试使用系统Gradle"
    if ! command -v gradle &> /dev/null; then
        print_error "未找到Gradle"
        echo "  请先安装Gradle或运行 gradle wrapper 生成wrapper"
        exit 1
    fi
    GRADLE_CMD="gradle"
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
echo "  Java版本: $JAVA_VERSION"
echo ""

# 检查签名配置
print_step "2" "检查签名配置..."

if [ -f "local.properties" ]; then
    print_success "找到local.properties配置文件"

    # 检查Release签名配置
    if grep -q "RELEASE_STORE_FILE" local.properties && \
       grep -q "RELEASE_STORE_PASSWORD" local.properties && \
       grep -q "RELEASE_KEY_ALIAS" local.properties && \
       grep -q "RELEASE_KEY_PASSWORD" local.properties; then
        print_success "Release签名配置完整"
        BUILD_RELEASE_SIGNED=true
    else
        print_warning "Release签名配置不完整，将构建未签名的Release版本"
        BUILD_RELEASE_SIGNED=false
    fi
else
    print_warning "未找到local.properties文件"
    if [ -f "local.properties.example" ]; then
        echo "  提示: 请参考 local.properties.example 创建配置文件"
    fi
    print_warning "将构建未签名的Release版本"
    BUILD_RELEASE_SIGNED=false
fi
echo ""

# 清理之前的构建
print_step "3" "清理之前的构建..."
if [ -n "$GRADLE_CMD" ]; then
    $GRADLE_CMD clean
else
    ./gradlew clean
fi
print_success "清理完成"
echo ""

# 构建Debug版本
print_step "4" "构建Debug版本..."
if [ -n "$GRADLE_CMD" ]; then
    $GRADLE_CMD assembleDebug
else
    ./gradlew assembleDebug
fi

if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    print_success "Debug版本构建成功"
    ls -lh app/build/outputs/apk/debug/app-debug.apk | awk '{printf "  大小: %s, 时间: %s %s\n", $5, $6, $7}'
else
    print_error "Debug版本构建失败"
    exit 1
fi
echo ""

# 构建Release版本
print_step "5" "构建Release版本..."
if [ -n "$GRADLE_CMD" ]; then
    $GRADLE_CMD assembleRelease
else
    ./gradlew assembleRelease
fi

if [ "$BUILD_RELEASE_SIGNED" = true ]; then
    RELEASE_APK="app/build/outputs/apk/release/app-release.apk"
    if [ -f "$RELEASE_APK" ]; then
        print_success "Release版本构建成功（已签名）"
        ls -lh "$RELEASE_APK" | awk '{printf "  大小: %s, 时间: %s %s\n", $5, $6, $7}'
    else
        print_error "Release版本构建失败"
        exit 1
    fi
else
    RELEASE_APK="app/build/outputs/apk/release/app-release-unsigned.apk"
    if [ -f "$RELEASE_APK" ]; then
        print_success "Release版本构建成功（未签名）"
        ls -lh "$RELEASE_APK" | awk '{printf "  大小: %s, 时间: %s %s\n", $5, $6, $7}'
    else
        print_error "Release版本构建失败"
        exit 1
    fi
fi
echo ""

# 复制到部署目录
print_step "6" "复制到部署目录..."
DEPLOY_DIR="$SCRIPT_DIR/deploy"
mkdir -p "$DEPLOY_DIR"

cp app/build/outputs/apk/debug/app-debug.apk "$DEPLOY_DIR/"
cp "$RELEASE_APK" "$DEPLOY_DIR/"

# 重命名Release版本
if [ "$BUILD_RELEASE_SIGNED" = true ]; then
    cp "$RELEASE_APK" "$DEPLOY_DIR/app-release-signed.apk"
else
    cp "$RELEASE_APK" "$DEPLOY_DIR/app-release-unsigned.apk"
fi

print_success "复制完成"
echo ""

# 输出构建信息
echo "=========================================="
echo -e "${GREEN}✅ 构建完成！${NC}"
echo "=========================================="
echo ""
echo "APK文件位置："
echo ""
echo -e "  Debug版本: ${GREEN}$DEPLOY_DIR/app-debug.apk${NC}"
if [ "$BUILD_RELEASE_SIGNED" = true ]; then
    echo -e "  Release版本（已签名）: ${GREEN}$DEPLOY_DIR/app-release-signed.apk${NC}"
else
    echo -e "  Release版本（未签名）: ${YELLOW}$DEPLOY_DIR/app-release-unsigned.apk${NC}"
fi
echo ""
echo "安装方法："
echo ""
echo "  1. 复制APK到设备:"
echo "     adb push $DEPLOY_DIR/app-debug.apk /sdcard/"
echo ""
echo "  2. 在Magisk中安装:"
echo "     Magisk → 模块 → 从本地安装 → 选择APK文件"
echo ""
echo "  3. 重启设备使模块生效"
echo ""
echo "注意事项："
echo ""
if [ "$BUILD_RELEASE_SIGNED" = false ]; then
    echo -e "  ${YELLOW}• Release版本未签名，如需签名请配置local.properties${NC}"
    echo "    或使用: ./generate-keystore.sh 生成签名密钥"
fi
echo "  • 首次安装后需要在LSPosed中启用模块"
echo "  • 需要在系统设置中授予必要权限"
echo "  • 推荐使用Android 10-14设备"
echo ""
echo "=========================================="

