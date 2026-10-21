# 全局映射、菜单与双手柄保留

按当前安装的包和版本识别生效配置。以下是 RP6 上 2026-10-22 的经验；设备名、descriptor、Android 输入索引、按键码和路径均须当前回读。不要无条件复制到其他机器。

## 共用方法

1. 备份全局配置和输入 profile；先用 GUI 新增一项外接绑定，回读其保存语法，再批量追加。配置文件需在模拟器退出后写，避免退出覆盖。
2. 内置/外接共同控制玩家1时，采用软件真正支持的多绑定 OR。逻辑 AND 通常表示组合键，不是多手柄任选；重复 INI 键在有些软件中有效，有些只保留最后一项。
3. 全局配置加载后重新打开设置回读；检查已有 per-game/core override 是否遮盖全局。本次要求全局时，不单独修当前游戏；发现冲突后先确认其用途，保留原文件再做授权范围内的调整。
4. 原生 Android 兼容模式与普通模式的设备身份不同，分别验证玩家路由；不要因多出新手柄就删除旧内置设备。传感器、触控和原震动等非目标设置保留。
5. 菜单组合按用户指定配置。普通桥接的 Select+Start→Android Back 不是所有模拟器的通用菜单；软件需要原生快捷键时单独配置。

## PS2 / AetherSX2

- RP6 包 `xyz.aethersx2.android`。备份完整全局输入 profile，在全局加载合并后的 profile，不能只在当前游戏中配置。
- 当次 profile 用重复同名绑定键保留内置和两种外接 descriptor；`&` 是组合键。先按一项“新增绑定”验证当前版本保存格式，避免通用 INI 解析器丢弃重复键。
- 当次经历了厂商克隆设备与接管后 Xbox 设备两种 descriptor；保存时保留原内置和外接绑定，并以当前系统设备列表为准，不硬编码作者的设备描述符。
- Select+Start 用模拟器原生暂停/菜单绑定。回读“上”仍有内置和外接绑定，再实测 ABXY、方向、双摇杆、肩键及双端菜单。

## RetroArch

- 当次包 `com.retroarch.aarch64` 1.22.2，主配置 `/sdcard/Android/data/com.retroarch.aarch64/files/retroarch.cfg`。原玩家绑定保持不动。
- 用户已接受：L2+Start 菜单；L2+左/右 上一/下一存档位；L2+上 读档；L2+下 存档。早先 Select+Home 请求已被 L2+Start 替代。
- 该机 hotkey enabler btn104、axis `+6`；menu108；load `h0up`，save `h0down`，slot decrease `h0left`、increase `h0right`；原 slot axis 绑定清为 `nul`。这些数字是当次 Android 映射，其他设备需采样。
- 保留其他快捷键，说明更换热键修饰键后它们也受新修饰键影响；发生冲突才处理。备份后在安全空档位做“保存→移动→读取”验收，避免覆盖有用存档。
- 当次内置和指定外接手柄的原功能基线均已通过，无需调整 Preferred Device。只有玩家路由确实失败才考虑支持回退的 Preferred；Reserved 可能排除内置，不能盲用。

## Dolphin 与 DuckStation

- Dolphin 当次设备列表内置 `Android/1/Retroid Pocket Controller`，外接曾为 Android/3 后变 Android/2；用多设备 OR 表达式，GUI 验证解析。GC1、Wii1 分开配置，保留传感器、其他玩家及原震动输出。Android 索引易变，重插后检查。
- DuckStation 原无完整玩家1绑定；备份原 profile 后加载内置+外接全局 profile，原生 OpenPauseMenu 组合 Select+Start。不可照抄 PS2 的所有配置语法，需读其保存结构。
- 这两款当次仅配置/UI 回读成功，未做游戏实测；后续仍需实体测试。

## 3DS / Azahar 系列

- 当次 `io.github.lime3ds.android` 实际 AzaharPlus 2126.2-A；生效配置 `/sdcard/Azahar-RP6/config/config.ini`，并非名字相似的旧目录。
- 用户要求电视与掌机同样显示外框、大上屏+小下屏：关闭全局 Enable Secondary Display，将电视由独立上屏输出改为系统镜像。开关在 Android 偏好里，INI 无差异不能当作没生效，重开 GUI 回读。
- L3 当次绑定以 keycode 工作，UI 内置设备名只是标签；外接已能切换时不覆盖绑定。保留已选定的 RP6 外框和布局，不替换为取消的通用试验版。
- 配置回读后必须实际接电视，检查上下屏、遮罩与内置/外接 L3；截图不能证明实体电视输出。

## 其他模拟器

- PPSSPP 当次 controls.ini 使用通用 Android device10，不能仅凭 UI 名字判断拒绝外接；先测后改。
- DraStic 当次已有内置 profile/扳机设置，备份并保留；映射向导未完成不能称已配置。
- Eden、Mupen64Plus 等先看现有全局玩家设置及实际响应，不为凑“全部”盲写未知配置。没有测试条件的条目说明已做项和待验项。
