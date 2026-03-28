# Magisk 和 LSPosed 下载检查清单

**目标：** 在10分钟内完成下载和基本安装

---

## 📋 下载清单

### Magisk v30.7
- [ ] 下载 Magisk-v30.7.apk (12MB)
  - 官方链接：https://github.com/topjohnwu/Magisk/releases/download/v30.7/Magisk-v30.7.apk
  - 国内镜像：https://mirror.ghproxy.com/https://github.com/topjohnwu/Magisk/releases/download/v30.7/Magisk-v30.7.apk

### LSPosed v1.11.0
- [ ] 下载 LSPosed-v1.11.0.zip (2.6MB)
  - 官方链接：https://github.com/LSPosed/LSPosed/releases/download/v1.11.0/LSPosed-v1.11.0.zip
  - 国内镜像：https://mirror.ghproxy.com/https://github.com/LSPosed/LSPosed/releases/download/v1.11.0/LSPosed-v1.11.0.zip

### 无线群控模块（如已构建）
- [ ] app-debug.apk
- [ ] app-release-signed.apk

---

## ⚡ 快速下载命令

```bash
# 一键下载所有文件
cd ~/Downloads
mkdir -p android-tools && cd android-tools

# 下载Magisk
wget https://github.com/topjohnwu/Magisk/releases/download/v30.7/Magisk-v30.7.apk

# 下载LSPosed
wget https://github.com/LSPosed/LSPosed/releases/download/v1.11.0/LSPosed-v1.11.0.zip

# 查看下载的文件
ls -lh
```

---

## 🔧 安装检查清单

### Magisk安装
- [ ] 设备Bootloader已解锁
- [ ] 备份了重要数据
- [ ] 下载了Magisk APK
- [ ] 通过ADB安装：`adb install Magisk-v30.7.apk`
- [ ] 在Magisk应用中修补Boot镜像
- [ ] 通过Fastboot刷入修补后的镜像
- [ ] 重启设备
- [ ] 验证ROOT：`adb shell su -c id`
- [ ] 启用Zygisk：设置 → Zygisk → 启用
- [ ] 重启设备

### LSPosed安装
- [ ] Magisk已安装且Zygisk已启用
- [ ] 下载了LSPosed模块
- [ ] 推送到设备：`adb push LSPosed-v1.11.0.zip /sdcard/Download/`
- [ ] 在Magisk应用中安装：模块 → 从本地安装
- [ ] 选择LSPosed-v1.11.0.zip
- [ ] 重启设备
- [ ] 验证安装：`adb shell pm list packages | grep lsposed`
- [ ] 打开LSPosed应用并授予权限

### 无线群控模块安装
- [ ] 已构建APK文件
- [ ] 推送到设备：`adb push app-release-signed.apk /sdcard/`
- [ ] 在Magisk中安装：模块 → 从本地安装
- [ ] 或直接安装：`adb install -r app-release-signed.apk`
- [ ] 重启设备
- [ ] 在LSPosed中启用模块
- [ ] 勾选需要Hook的应用（微信、QQ、系统UI）
- [ ] 重启设备
- [ ] 打开应用并授予权限
- [ ] 测试HTTP服务器：`curl http://设备IP:8080/api/status`

---

## ✅ 验证清单

### 系统环境
- [ ] ROOT权限正常：`adb shell su -c id`
- [ ] Magisk版本正常：`adb shell su -c magisk -v`
- [ ] Zygisk已启用：`adb shell su -c magisk --zygisk-status`
- [ ] LSPosed已安装：`adb shell pm list packages | grep lsposed`
- [ ] 模块已安装：`adb shell pm list packages | grep wireless_control`

### 功能测试
- [ ] HTTP服务器可访问
- [ ] 心跳正常
- [ ] LSPosed模块已启用
- [ ] 目标应用已勾选
- [ ] 无障碍服务已授予
- [ ] 通知权限已授予

---

## 📊 下载统计

| 文件 | 大小 | 下载时间 | 状态 |
|------|------|---------|------|
| Magisk-v30.7.apk | 12MB | ~30秒 | [ ] |
| LSPosed-v1.11.0.zip | 2.6MB | ~10秒 | [ ] |
| **总计** | **14.6MB** | **~40秒** | [ ] |

---

## 🔗 相关文档

- [详细安装指南](./MAGISK_LSPOSED_INSTALL.md)
- [快速下载链接](./DOWNLOAD_LINKS.md)
- [快速开始指南](./QUICK_START.md)
- [测试清单](./TEST_CHECKLIST.md)

---

## ⚠️ 重要提醒

1. **备份！备份！备份！**
   - 刷入前务必备份数据
   - 使用TWRP进行完整备份

2. **版本兼容**
   - Magisk: v30.7
   - LSPosed: v1.11.0
   - Android: 10-14
   - 不要使用过旧的版本

3. **网络问题**
   - 如果下载失败，尝试国内镜像
   - 或使用下载工具（迅雷、IDM等）

4. **安装顺序**
   - Magisk → Zygisk → 重启 → LSPosed → 重启 → 模块 → 重启
   - 每次重启后都要验证

---

## 📞 获取帮助

如果遇到问题：

1. 查看详细文档：`MAGISK_LSPOSED_INSTALL.md`
2. 查看官方文档：https://topjohnwu.github.io/Magisk/
3. 查看GitHub Issues：https://github.com/LSPosed/LSPosed/issues
4. 查看日志：`adb logcat`

---

**文档版本：** v1.0
**更新时间：** 2026-03-27
**预计下载时间：** 40秒（总大小14.6MB）
**预计安装时间：** 10-15分钟
