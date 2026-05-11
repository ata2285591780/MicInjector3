# MicInjector

一个 Android LSPosed 模块，用于 Hook 指定应用的麦克风输入，实现语音包实时注入。

## 功能特性

- 🎤 **麦克风 Hook** - Hook 目标应用的 AudioRecord，拦截麦克风数据
- 📁 **本地音频播放** - 支持播放本地音频文件作为麦克风输入
- 🔄 **多种播放模式** - 支持单曲播放、循环播放、PTT 按住说话模式
- 🖥️ **系统音频捕获** - 可捕获系统音频输出作为麦克风输入
- 🪟 **悬浮窗控制** - 提供悬浮窗控制面板，方便实时操作

## 使用方法

### 1. 安装模块

1. 确保已安装 LSPosed 或 LSPosed-based 框架（如 KernelSU + LSPosed）
2. 将编译好的 APK 安装到设备
3. 在 LSPosed 模块管理器中启用 MicInjector
4. 勾选需要 Hook 的目标应用

### 2. 配置

1. 打开 MicInjector 应用
2. 在"目标应用"中输入需要 Hook 的包名（多个用逗号分隔）
3. 选择音频来源：
   - **音频文件**：选择本地音频文件
   - **系统音频**：捕获系统音频输出
4. 设置播放模式：
   - **单曲播放**：播放一次后停止
   - **循环播放**：循环播放音频
   - **PTT 模式**：按住说话时使用麦克风录音

### 3. 悬浮窗控制

启用悬浮窗后，可以：
- 播放/暂停音频
- 停止播放
- 在 PTT 模式下按住按钮说话

## 技术原理

1. **Hook AudioRecord** - 使用 Xposed 框架 Hook `android.media.AudioRecord` 类的 `read()` 方法
2. **音频数据注入** - 在目标应用读取麦克风数据时，替换为预设的音频数据
3. **音频引擎** - 支持播放本地音频文件和捕获系统音频

## 项目结构

```
MicInjector/
├── app/src/main/
│   ├── java/com/micinjector/
│   │   ├── hook/
│   │   │   ├── MainHook.java           # 核心 Hook 逻辑
│   │   │   └── XposedEntry.java        # Xposed 模块入口
│   │   ├── audio/
│   │   │   ├── AudioFileEngine.java           # 本地音频文件引擎
│   │   │   └── AudioSystemCaptureEngine.java  # 系统音频捕获引擎
│   │   ├── config/
│   │   │   ├── ConfigManager.java       # 配置管理器
│   │   │   ├── ConfigProvider.java      # 跨进程配置通信
│   │   │   └── PrefsHelper.java         # SharedPreferences 封装
│   │   └── ui/
│   │       ├── MainActivity.java               # 主配置界面
│   │       ├── FloatingWindowService.java      # 悬浮窗服务
│   │       └── FileUtils.java                  # 文件工具类
│   └── res/
│       ├── layout/               # 布局文件
│       └── values/               # 资源值
└── build.gradle                  # Gradle 构建配置
```

## 构建

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- Android SDK 34
- Gradle 8.4
- Android 14 (API 34) 设备或模拟器

### 构建步骤

1. 克隆仓库
2. 用 Android Studio 打开项目
3. 等待 Gradle 同步完成
4. 连接 Android 设备
5. 执行 `Build > Make Project` 或按 `Ctrl+F9`

### 签名配置

发布版本需要配置签名密钥。在 `app/build.gradle` 中添加：

```gradle
android {
    signingConfigs {
        release {
            storeFile file('your-keystore.jks')
            storePassword 'your-password'
            keyAlias 'your-alias'
            keyPassword 'your-key-password'
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
        }
    }
}
```

## 权限说明

| 权限 | 用途 |
|------|------|
| `RECORD_AUDIO` | 录制和播放音频 |
| `SYSTEM_ALERT_WINDOW` | 显示悬浮窗 |
| `FOREGROUND_SERVICE` | 前台服务 |
| `READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE` | 读取音频文件 |

## 兼容性

- Android 8.0 (API 26) 及以上
- 需要 LSPosed、EdXposed 或 KernelSU + LSPosed 框架

## 注意事项

1. 部分应用可能有防 Hook 检测机制
2. 系统音频捕获需要用户授权屏幕录制权限
3. 使用前请确保目标应用已正确配置

## 许可证

本项目仅供学习和研究使用，请遵守相关法律法规。

## 致谢

- [LSPosed](https://github.com/LSPosed/LSPosed) - Xposed 框架
- [Xposed](https://github.com/rovo89/XposedBridge) - Hook 框架基础
