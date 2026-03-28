# 无线群控系统 - 设备端Xposed模块开发

## 项目概述
- **目标平台：** Android 10-14
- **开发框架：** Xposed + LSPosed
- **API端口：** 8080
- **主要功能：**
  - 接收主控服务器指令
  - 执行设备操作
  - 上报设备状态
  - Hook系统应用

## 技术栈
- **Xposed框架：** LSPosed v1.9.3
- **HTTP服务器：** NanoHTTPD / Ktor
- **JSON处理：** Gson
- **调试工具：** adb logcat

## 开发计划
1. 创建Android项目结构
2. 实现Xposed Hook入口
3. 实现HTTP API服务器
4. 实现设备控制功能
5. 实现应用Hook功能
6. 实现状态上报功能
