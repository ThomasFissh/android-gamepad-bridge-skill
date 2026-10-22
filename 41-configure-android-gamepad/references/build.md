# 从源码构建

作者：李逊。公开 APK v1.7.0 是通用名称构建，使用独立包名和签名；此前功能基线已获真机验收，改名构建未重新做真机操作验收。此脚本用于检查、修改和自行构建，不会重现作者私钥。

## 工具与命令

准备 Python 3.9+、JDK 17、Android SDK Platform 32 或更高、SDK Build Tools 35.0.0（含 aapt2、d8、zipalign、core-lambda-stubs.jar）。工具由使用者自行提供；脚本不联网下载、不修改设备，不需要 Gradle。

在仓库根目录执行，路径替换为自己的实际安装位置：

```sh
python3 41-configure-android-gamepad/assets/rp6-usb-bridge/build.py \
  --java-home /path/to/jdk-17 \
  --android-jar /path/to/android-sdk/platforms/android-32/android.jar \
  --build-tools /path/to/android-sdk/build-tools/35.0.0
```

也可设置 `JAVA_HOME`、`ANDROID_HOME` 或 `ANDROID_SDK_ROOT`，后两个目录按标准 SDK 布局选择已安装的稳定版 platform/build-tools。需要固定工具版本时使用命令参数。

输出默认位于源码目录的 `build/`：`Android-Gamepad-Bridge-unsigned.apk` 为对齐后的未签名 APK；`unsigned-manifest.json` 记录 SHA-256。源码/公开 APK 不会被覆盖。Manifest minSdk26、targetSdk32、versionCode10；构建脚本从 Manifest 读取，二者保持一致。

## 自行签名与安装

使用自己的 Android 签名密钥和 SDK `apksigner`。例如：

```sh
/path/to/build-tools/apksigner sign \
  --ks /path/to/your-release.jks \
  --ks-pass file:/path/to/your-password-file \
  --out /path/to/Android-Gamepad-Bridge-self-signed.apk \
  41-configure-android-gamepad/assets/rp6-usb-bridge/build/Android-Gamepad-Bridge-unsigned.apk
/path/to/build-tools/apksigner verify --verbose /path/to/Android-Gamepad-Bridge-self-signed.apk
```

密钥与口令保持私有，不写进仓库或问题反馈。自行签名与作者 APK 签名不同，不能直接覆盖已安装的作者版本。不要为了绕过签名错误直接卸载清数据；先备份，或为自己的适配修改包名、应用名及启动脚本，用独立包测试。

编译成功仅证明源码可构建。安装、USB 权限、系统输入、游戏响应、授权收回及重启独立启动仍需在目标设备逐项验收。新 ROM/新机型需要 root 兼容时，由机主提供并授权入口；当前源码的 OEM 适配只针对已验证 RP6。

## 输入状态测试

运行 `python3 41-configure-android-gamepad/assets/rp6-usb-bridge/test_state.py --java-home /path/to/jdk-17`，只测试显示状态模型，无需连接设备，不会启动桥接。覆盖按下/松开/重复、轴坐标、数字/模拟扳机、清空统计和失焦/断开。它不能代替设备实按、游戏或重启验收。
