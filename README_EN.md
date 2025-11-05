<div align="center">
  <a href="https://github.com/ZZHow1024/MagicKeyboard">
    <img src="src/main/resources/image/MagicKeyboard.png" width="20%" alt="MagicKeyboard" />
  </a>
  <h1>MagicKeyboard</h1>
</div>
<div align="center" style="line-height: 1;">
  <a href="https://github.com/ZZHow1024/MagicKeyboard/releases"><img alt="MagicKeyboard2.0.0"
    src="https://img.shields.io/badge/MagicKeyboard-v2.0.0-blue"/>
  </a>
  <a href="LICENSE"><img alt="Code License"
    src="https://img.shields.io/github/license/ZZHow1024/MagicKeyboard">
  </a>
</div>

# **MagicKeyboard(English)**

[**中文说明**](./README.md)

---

Website:

[MagicKeyboard(English) | ZZHow](https://www.zzhow.com/MagicKeyboardEN)

Source Code:

https://github.com/ZZHow1024/MagicKeyboard

Releases:

[**https://github.com/ZZHow1024/MagicKeyboard/releases**](https://github.com/ZZHow1024/MagicKeyboard/releases)

---

## What is it?

**MagicKeyboard** is a cross-platform simulated keyboard input tool that supports automatic simulated keyboard input operations on Windows, macOS, and Linux operating systems.

---

## Technical route

- Programming Language: **Java**
- GUI: **JavaFX**
- Build Tool: **Maven**

---

## **License**

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](./LICENSE) file for details.

---

## **Instructions for use**

Download address:

https://github.com/ZZHow1024/MagicKeyboard/releases

- Determine the operating system you are using.
    - Linux:
        - Select the .deb installation package (Debian, Ubuntu) / .rpm (Red Hat, Fedora, SUSE) installation package.
    - macOS:
        - Determine the chip of the Mac you are using (Apple Silicon / Intel).
        - Select the .dmg disk image / .pkg installation package.
    - Windows:
        - Select the .zip compressed package / .exe installation package / .msi installation package.
    - General:
        - Select the .jar package (the computer needs to have JRE configured)
- Download the corresponding file.
- Linux and macOS need to be installed before running. Windows can directly run the .exe executable program in the .zip compressed package or select the .exe installation package and .msi installation package to perform the installation operation. The .jar package can be directly run through the `java -jar` command.
- Launch **MagicKeyboard**.
- Type or paste the text you want to type into the text box.
- Switch typing mode (Compatible Mode/Rapid Mode).
- Adjust the typing interval and floating window position.
- Choose whether to enable Code Mode (Ignore leading whitespace).
- Press the "Start Typing" button to start a 3-second countdown, after which the simulated keyboard typing begins.
    - At this point, the "Start Typing" button will function as "Pause" or "Resume", and the "Clear" button will function as "Stop".
- Press the "About" button to view an introduction.
- Select language in the bottom right corner.
    - Currently supports Chinese (Simplified/Traditional) and English.

---

## Mode Description

**MagicKeyboard** offers two keyboard input modes: Rapid Mode and **Rapid Mode**, to meet the needs of different scenarios.

1. **Compatibility Mode**
    - Features: Prioritizes compatibility, suitable for most operating systems and application scenarios.
    - Suitable Scenarios:
        - When stability and accuracy of input are required.
        - Suitable for scenarios where input speed is not critical but compatibility is necessary.
    - Implementation Details: In this mode, **MagicKeyboard** simulates keyboard input in a more conservative manner to ensure proper operation across different applications.
2. **Speed Mode**
    - Features: Prioritizes speed, suitable for scenarios with high input efficiency requirements.
    - Suitable Scenarios:
        - Scenarios involving large amounts of text input or requiring fast response times.
        - Scenarios with lower compatibility requirements but prioritizing extreme input speed.
    - Implementation Details: In this mode, **MagicKeyboard** simulates keyboard input in a more efficient manner, potentially sacrificing some compatibility for faster input speed.
- Users can choose different modes for keyboard input as needed.
    - The default mode is **Compatibility Mode**.
    - Users can switch between typing modes (Compatibility Mode/Rapid Mode) before starting to type.

---

## Version Information

### Current Version

- **MagicKeyboard Version**: 2.0.0
- **Java Version**: 21+
- **JavaFX Version**: 21
- **Maven Version**: 3.6+

### System Requirements

- Windows 10+, macOS 10.15+, Linux(X11)

---

## Cross-Platform Support Notes

### Fully Supported Platforms

- **Windows 10/11**: Fully supports all features, including Chinese input.
- **macOS 10.15+**: Fully supports all features, including Chinese input.

### Limitedly Supported Platforms

- **Linux(X11)**:
- Supports English character input.
- Does not support non-English characters such as Chinese.
- Only supports the X11 display server; Wayland is not currently supported.

---

## Quick Start

### Environment Preparation

1. Install JDK 21 or later.
2. Ensure the JavaFX 21 runtime is installed on your system.

### Compile and Run

- Clone the Project

    ```bash
    git clone https://github.com/ZZHow1024/MagicKeyboard.git
    ```

- Enter the project directory

    ```bash
    cd MagicKeyboard
    ```

- Compile the project

    ```bash
    mvn clean compile
    ```

- Run the application

    ```bash
    mvn javafx:run
    ```


---

## Feature Introduction by Version

- MagicKeyboard1.0.0
    - Simulate keyboard typing.
    - Adjust the typing interval.
    - Floating window notifications.
        - Adjust the floating window position.
        - Control typing status using the floating window.
- MagicKeyboard 2.0.0
    - Switch typing mode (Compatible Mode/Rapid Mode).
    - Code Mode (Ignore leading whitespace).
    - Support multiple languages.
        - Chinese (Simplified/Traditional)
        - English

---

## **Main interface of each version**

### MagicKeyboard1.0.0

![MagicKeyboard1.0.0](https://www.notion.so/image/attachment%3A8a91fbce-ecd8-48c8-8dac-b8338679a882%3AMagicKeyboard1.0.0.png?table=block&id=28de64bd-e40f-8096-b15b-dd13f5664ebe&t=28de64bd-e40f-8096-b15b-dd13f5664ebe)

MagicKeyboard1.0.0

### MagicKeyboard2.0.0

![MagicKeyboard2.0.0-EN](https://www.notion.so/image/attachment%3A65cbcb19-db48-480a-b4b9-50c63c937c6f%3AMagicKeyboard2.0.0-EN.png?table=block&id=2a2e64bd-e40f-8026-b84b-ef8949581d5d&t=2a2e64bd-e40f-8026-b84b-ef8949581d5d)

MagicKeyboard2.0.0-EN

---

## Contribution Guidelines

Welcome to submit issues and pull requests to improve project features, especially compatibility improvements on the Linux platform.