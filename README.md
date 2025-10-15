<div align="center">
  <a href="https://github.com/ZZHow1024/MagicKeyboard">
    <img src="src/main/resources/image/MagicKeyboard.png" width="20%" alt="MagicKeyboard" />
  </a>
  <h1>MagicKeyboard</h1>
</div>
<div align="center" style="line-height: 1;">
  <a href="https://github.com/ZZHow1024/MagicKeyboard/releases"><img alt="MagicKeyboard1.0.0"
    src="https://img.shields.io/badge/MagicKeyboard-v1.0.0-blue"/>
  </a>
  <a href="LICENSE"><img alt="Code License"
    src="https://img.shields.io/github/license/ZZHow1024/MagicKeyboard">
  </a>
</div>

# 神奇键盘_**MagicKeyboard**（中文说明）

[**English**](./README_EN.md)

---

Website:

[神奇键盘_MagicKeyboard（中文说明） | ZZHow](https://www.zzhow.com/MagicKeyboard)

Source Code:

https://github.com/ZZHow1024/MagicKeyboard

Releases:

[**https://github.com/ZZHow1024/MagicKeyboard/releases**](https://github.com/ZZHow1024/MagicKeyboard/releases)

---

## 它是什么？

**MagicKeyboard** 是一款跨平台的模拟键盘输入工具，支持在 Windows、macOS 和 Linux 操作系统上自动模拟键盘输入操作。

---

## 技术路线

- 编程语言：**Java**
- 图形化界面：**JavaFX**
- 构建工具：**Maven**

---

## 许可证

该项目根据 GNU 通用公共许可证 v3.0 获得许可 - 有关详细信息，请参阅 [LICENSE](./LICENSE) 文件。

---

## 使用说明

下载地址：

https://github.com/ZZHow1024/MagicKeyboard/releases

- 确定您使用的操作系统。
    - Linux：
        - 选择 .deb安装包（Debian, Ubuntu） / .rpm（Red Hat, Fedora, SUSE）安装包。
    - macOS：
        - 确定您使用的 Mac 的芯片(Apple Silicon / Intel)。
        - 选择 .dmg磁盘镜像 / .pkg安装包。
    - Windows：
        - 选择 .zip压缩包 / .exe安装包 / .msi安装包。
    - 通用：
        - 选择 .jar包（计算机需要配置好 JRE）
- 下载对应的文件。
- Linux 和 macOS 需要执行安装操作后再运行，Windows 可直接运行 .zip 压缩包中的 .exe 可执行程序或选择 .exe 安装包与 .msi 安装包执行安装操作，.jar 包可直接通过 `java -jar` 命令运行。
- 启动 **MagicKeyboard**。
- 将待键入的内容输入或粘贴到文本框内。
- 调节键入时间间隔与悬浮窗位置。
- 按下 “开始键入” 按钮将进入 3s 倒计时，倒计时结束后开始模拟键盘键入文本。
    - 此时，“开始键入” 按钮的功能将变为 ”暂停“/”继续“，”清空“按钮的功能将变为 “停止”。
- 按下 “关于” 按钮可查看简介。
- 按下 “退出” 按钮可退出程序。

---

## 版本信息

### 当前版本

- **MagicKeyboard 版本号**: 1.0.0
- **Java 版本**: 21+
- **JavaFX 版本**: 21
- **Maven 版本**: 3.6+

### 系统要求

- Windows 10+，macOS 10.15+，Linux(X11)

---

## 跨平台支持说明

### 完全支持平台

- **Windows 10/11**：完整支持所有功能，包括中文输入。
- **macOS 10.15+**：完整支持所有功能，包括中文输入。

### 有限支持平台

- **Linux(X11)**：
    - 支持英文字符输入。
    - 不支持中文等非英文字符。
    - 仅支持 X11 显示服务器，Wayland 暂不支持。

---

## 快速开始

### 环境准备

1. 安装 JDK 21 或更高版本。
2. 确保系统已安装 JavaFX 21 运行时。

### 编译运行

- 克隆项目

    ```bash
    git clone <https://github.com/ZZHow1024/MagicKeyboard.git>
    ```

- 进入项目目录

    ```bash
    cd MagicKeyboard
    ```

- 编译项目

    ```bash
    mvn clean compile
    ```

- 运行应用

    ```bash
    mvn javafx:run
    ```


---

## 各版本功能介绍

- MagicKeyboard1.0.0
    - 模拟键盘键入。
    - 键入间隔时间调节。
    - 悬浮窗提示。
        - 支持调节悬浮窗位置
        - 支持悬浮窗控制键入状态

---

## 各版本主界面

### MagicKeyboard1.0.0

![MagicKeyboard1.0.0](https://www.notion.so/image/attachment%3A8a91fbce-ecd8-48c8-8dac-b8338679a882%3AMagicKeyboard1.0.0.png?table=block&id=28de64bd-e40f-8096-b15b-dd13f5664ebe&t=28de64bd-e40f-8096-b15b-dd13f5664ebe)

MagicKeyboard1.0.0

---

## 贡献指南

欢迎提交 Issue 和 Pull Request 来改进项目功能，特别是 Linux 平台的兼容性改进。