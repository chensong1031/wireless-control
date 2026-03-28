# 🚀 快速使用 GitHub Actions 构建 APK

## 一键推送

```bash
cd /workspace/projects/workspace-main
./github-push.sh
```

按照提示输入你的 GitHub 仓库地址，脚本会自动：
1. 配置远程仓库
2. 提交所有更改
3. 推送到 GitHub

---

## 手动推送（可选）

```bash
# 1. 添加远程仓库（替换为你的实际地址）
git remote add origin https://github.com/你的用户名/wireless-control.git

# 2. 推送代码
git push -u origin main
```

---

## 查看构建状态

推送后访问：
```
https://github.com/你的用户名/wireless-control/actions
```

---

## 下载 APK

构建完成后（约 3-5 分钟）：
1. 点击最新的 workflow run
2. 滚动到底部
3. 下载 `debug-apk` → `app-debug.apk`

---

## 安装测试

```bash
adb uninstall com.wireless.control.device
adb install app-debug.apk
adb reboot
```

重启后打开 LSPosed，"Wireless Control" 应该是白色（可启用）。

---

**详细说明：** 查看 `GITHUB_ACTIONS_GUIDE.md`
