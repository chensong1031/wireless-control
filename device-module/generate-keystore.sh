#!/bin/bash

# 签名密钥生成脚本
# 用途：为Release版本生成签名密钥

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
KEYSTORE_DIR="$SCRIPT_DIR/keystore"
KEYSTORE_FILE="$KEYSTORE_DIR/wireless-control.jks"

# 创建keystore目录
mkdir -p "$KEYSTORE_DIR"

# 检查keytool是否可用
if ! command -v keytool &> /dev/null; then
    echo "错误: 未找到keytool命令"
    echo "请确保已安装Java JDK并配置环境变量"
    echo ""
    echo "或在Android Studio中使用："
    echo "  Build -> Generate Signed Bundle / APK"
    exit 1
fi

# 检查keystore文件是否已存在
if [ -f "$KEYSTORE_FILE" ]; then
    echo "警告: 签名密钥文件已存在: $KEYSTORE_FILE"
    read -p "是否覆盖? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "操作已取消"
        exit 0
    fi
    rm -f "$KEYSTORE_FILE"
fi

# 输入密钥信息
echo "=========================================="
echo "  无线群控系统 - 签名密钥生成"
echo "=========================================="
echo ""

read -p "输入密钥库密码: " -s STORE_PASSWORD
echo
read -p "再次输入密钥库密码: " -s STORE_PASSWORD_CONFIRM
echo

if [ "$STORE_PASSWORD" != "$STORE_PASSWORD_CONFIRM" ]; then
    echo "错误: 密码不匹配"
    exit 1
fi

read -p "输入密钥别名: " KEY_ALIAS
read -p "输入密钥密码 (直接回车使用密钥库密码): " -s KEY_PASSWORD
echo

if [ -z "$KEY_PASSWORD" ]; then
    KEY_PASSWORD="$STORE_PASSWORD"
fi

read -p "输入你的名字: " YOUR_NAME
read -p "输入组织名称: " ORGANIZATION
read -p "输入城市: " CITY
read -p "输入省份/州: " STATE
read -p "输入国家代码 (如: CN): " COUNTRY

# 生成密钥
echo ""
echo "正在生成签名密钥..."
echo ""

keytool -genkeypair \
    -v \
    -keystore "$KEYSTORE_FILE" \
    -alias "$KEY_ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -storepass "$STORE_PASSWORD" \
    -keypass "$KEY_PASSWORD" \
    -dname "CN=$YOUR_NAME, O=$ORGANIZATION, L=$CITY, ST=$STATE, C=$COUNTRY"

echo ""
echo "=========================================="
echo "  签名密钥生成完成"
echo "=========================================="
echo ""
echo "密钥文件: $KEYSTORE_FILE"
echo ""
echo "请更新 local.properties 文件："
echo "  RELEASE_STORE_FILE=keystore/wireless-control.jks"
echo "  RELEASE_STORE_PASSWORD=$STORE_PASSWORD"
echo "  RELEASE_KEY_ALIAS=$KEY_ALIAS"
echo "  RELEASE_KEY_PASSWORD=$KEY_PASSWORD"
echo ""
echo "注意：请妥善保管密钥文件和密码，不要提交到版本控制系统！"
