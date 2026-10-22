# RP6 + 2.4G USB 手柄 已验证适配

以下是 2026-10-22 的本机验收基线，供识别和复用；不代表其他型号或更新后的 ROM 已通过。先核对实机，不依赖旧 IP/端口。

## 材料与复用边界

- [参考源码与 APK](../assets/rp6-usb-bridge/)：源代码、资源、Manifest、当前通用名称 1.7.0 像素界面 APK、可选启动脚本和 SHA256.json。未打包签名私钥、口令、ADB 密钥。
- 当前公开包 `io.github.thomasfissh.androidgamepadbridge`，versionCode 10，versionName `1.7.0-pixel`；APK SHA256 `7a9adafc56500d6caef14ff70d5e9460f4f8ad9e6fb348a85e3adc91d238d41f`。本包源码/状态测试通过，尚未重新安装到实体设备验证。
- 更新前备份使用者自己已安装的稳定版本和配置；本仓库不附历史试验版，不能为恢复直接卸载清数据。
- 已安装的稳定版本优先保留。APK 是 RP6 指定 USB 接收器参考实现，不是任意 Android 通用兼容包；新机器至少完成普通权限最小验证，兼容路径需用户提供其 root 入口并重新适配。
- [便携构建脚本](../assets/rp6-usb-bridge/build.py) 与源代码一同提供，使用方法见 [构建说明](build.md)。在副本中改动；升级原包需要相同签名，自己的新签名不能覆盖官方发布包。本仓库不包含签名私钥。
- 构建需要 JDK 17、Android SDK Platform 32 或更高及对应 build-tools，路径由使用者提供；不读取作者电脑的工具链。
- 公开适配结论见本文件末尾；私有设备日志、序列号、网络地址和个人存档不随仓库分发。新适配可填写 [验收记录](acceptance-record.md)。

## 接收器与报告

- 已验证的 2.4G USB 接收器 VID:PID `2345:e053`；interface 0，class 255/subclass 93/protocol 1，IN `0x82`，OUT `0x02`。其他固件/接收器需重新核对。
- X360 风格报告至少 20 字节：byte0=0，byte1=20；byte2–3 按键 LE，LT=byte4，RT=byte5，LX/LY/RX/RY 为 byte6/8/10/12 的 int16 LE。扳机原始 0..255；摇杆 -32768..32767。
- 方向 mask 为上/下/左/右 `1/2/4/8`；Start/Select `0x10/0x20`，L3/R3 `0x40/0x80`，LB/RB `0x100/0x200`，ABXY `0x1000/0x2000/0x4000/0x8000`。以代码及本机采样交叉核对。
- 使用 UsbRequest 排队、`requestWait(1000)`，TimeoutException 继续等待同一请求；不要每空闲一秒就判失败。解绑/停止需取消请求并关闭连接。
- M1/M2 此次工厂状态未采到可独立映射报告；没有把它们设置为 Back/Home。Select+Start 触发 Back（KEY_BACK 158），组合触发后抑制两键，完全松开再解锁。
- 震动包 `[0,8,0,left,right,0,0,0]`，左右强度 0..255。只有报告确实属于该协议才发送。

## 普通模式

普通 App UID 通过 UsbManager 读取接收器，子进程 `/system/bin/uinput -` 同为 App UID，创建 `Gamepad Bridge Controller`（VID `045e`/PID `028e`）和 `Gamepad Bridge Menu`。虚拟设备能力与轴范围见 `VirtualPad.java`，USB 到事件转换见 `BridgeService.java`。

`uinput` 普通 App 可调用是这台设备的实测，不是 Android 通用保证。输出 JSON 可能没有换行，不能只按逐行读取。FF 回调只有状态，当前实现固定强度并有停震处理，不提供精确效果强度/时长重现。退出发零强度。标准路径及其震动已获用户实测确认。

设备 `persist.sys.handle.mode=1`（Retroid 新接管模式）当时已启用，重启后虚拟设备身份从系统克隆变为 Xbox 身份；这单独没有解决《死亡细胞》。不要为复用本技能直接设置属性或重启，先核对 UI/当前状态及任务需要。

## 内置兼容与厂商 root 入口

用户提供入口：Retroid 设置 → 高级 → 以 Root 身份运行脚本。经正常选择文件和执行按钮验证 `uid=0(root)`、context `u:r:pservice:s0`。非导出的 RunScriptActivity 不直接启动。

当前 ROM 的普通 App 也能调用该厂商已有服务（另一个 UID 的探针已验证），因此主 App 可一键授权，而不必每次进入设置执行脚本：

- 服务名 `PServerBinder`，transaction code 0。
- request `Parcel.writeStringArray(new String[]{command, "1"})`；第二项为捕获输出标记。
- reply `createByteArray()` 返回 UTF-8；此协议不使用 `readException()` 头。以实际厂商代码及 `OemInputGrant.java` 为准。
- App 仅实现固定 `id` 和验证后的单节点所有权授权/回收，不导出任意 root 命令入口。不得把厂商协议泛化到别的 ROM 或利用它执行无关操作。

目标为唯一 `Retroid Pocket Controller`，sysfs VID/PID/version `2022/3001/0000`。动态扫描 event0..63，拒绝重复匹配。event 编号和应用 UID 都会变化；代码使用 `Process.myUid()`。

授权前验证字符设备、owner root、gid1004(input)、mode0660，或本 App 遗留的同组同权限租借状态。已有厂商 root 服务只执行 `chown <当前AppUID>:1004 <已验证event节点>`；保留0660，App 本身写入输入。停止时释放本 App 的按键/轴、关 FD，再恢复 owner0/group1004/mode0660；正常获取和释放回读 `0→AppUID→0`。重启恢复原权限后仍须重新获取，不能依赖之前临时0666状态。

当前构建使用 `OemInputGrant.acquire` 获取授权；以实际权限回读为准，不能依赖旧测试中暂时可写的节点。

内置轴 X/Y=0/1，右摇杆 Z/RZ=2/5，BRAKE/GAS=10/9、范围0..32767，HAT=16/17；键码和扳机数字阈值见 BridgeService。Linux input_event 按进程 ABI 24字节(64位)或16字节(32位)，使用本机字节序和 SYN_REPORT。此模式不创建额外虚拟 gamepad。

## 启动与功能基线验收范围

以下实体测试来自改名前的功能基线。通用名称构建已更换包名、虚拟设备名和签名，编译/静态验证通过，但尚未重新真机验收。原设备配置不会因此自动迁移。

- 普通 App：打开“安卓手柄桥接”，选择模式后点启动。兼容勾选标签“内置手柄兼容（Retroid 系统授权，死亡细胞）”；模式持久化。普通模式保留旧虚拟手柄配置。
- 可选脚本 [start-gamepad-bridge.sh](../assets/rp6-usb-bridge/start-gamepad-bridge.sh)，设备原位置 `/sdcard/Download/start-gamepad-bridge.sh`；从厂商脚本入口实际执行通过。内容仅用 `am start --user 0 -f 0x10008000 -n io.github.thomasfissh.androidgamepadbridge/.MainActivity --ez gamepad_start true` 启动保存模式。当前 ROM 无清任务标记时热启动未触发自动开始；该标记重建 bridge Activity 任务，不 force-stop 运行服务。
- 原功能基线 1.6.1 已完成以下重启验收：用户不先开无线调试、不用 Shizuku，直接 App 点启动，再进《死亡细胞》测试方向、A/B、左摇杆和 Select+Start，确认成功。测试期间电脑未启动桥接或发送游戏输入。
- PS2 全局双手柄、RetroArch 快捷键、3DS 电视遮罩/L3 均获实体操作确认。Dolphin/DuckStation 只完成配置与 UI 回读；未验证全部游戏。兼容模式的游戏震动未单独验收，不能沿用普通模式的震动结论。

## 实时测试界面

显示层与跨机型复用见 [输入测试界面与移植](controller-ui.md)。桥接核心四类源码与上一公开版一致，新增显示层不意味着新的 USB 协议或 ROM 已适配。历史游戏/冷启动结果只属于原功能基线。
