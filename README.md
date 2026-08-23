# ChatBlock

Minecraft 1.21.8 / Fabric 纯客户端聊天关键词屏蔽 Mod：**别人**发送的含关键词消息在自己的客户端不显示，**自己**发送的消息永远正常显示（豁免）。

## 功能特性

- 屏蔽别人发送的含关键词聊天消息（仅安装者自己看不到，其他玩家正常可见）
- 自己发送的消息不受影响（豁免）
- 包含匹配 + 不区分大小写（中文、英文均适用）
- 游戏内命令实时管理关键词
- ModMenu + YACL 图形配置界面
- JSON 配置文件持久化（损坏自动容错，不崩溃）

## 安装

1. 安装 [Fabric Loader](https://fabricmc.net/use/)（Minecraft 1.21.8，Java 21+）
2. 将 `chatblock-1.0.0.jar` 放入 `.minecraft/mods/`
3. 前置依赖：
   - [Fabric API](https://modrinth.com/mod/fabric-api)（0.136.1+1.21.8）
   - [YetAnotherConfigLib (YACL)](https://modrinth.com/mod/yacl)（3.8.x）
   - 可选：[ModMenu](https://modrinth.com/mod/modmenu)（15.0.2+，提供配置界面入口）

## 使用

### 游戏内命令

| 命令 | 说明 |
|---|---|
| `/chatblock add <关键词>` | 添加屏蔽关键词（自动去重并写盘） |
| `/chatblock remove <关键词>` | 移除屏蔽关键词 |
| `/chatblock list` | 列出当前全部关键词 |
| `/chatblock reload` | 从配置文件重新加载 |

### 配置界面

主菜单 → Mods → ChatBlock → 配置按钮（需安装 ModMenu），可编辑"启用屏蔽"开关与关键词列表。

### 配置文件

位置：`.minecraft/config/chatblock.json`

```json
{
  "enabled": true,
  "keywords": ["示例词1", "badword"]
}
```

## 从源码构建

```powershell
.\gradlew.bat build
```

产物位于 `build/libs/chatblock-1.0.0.jar`（需 JDK 21+）。

## 许可证

[MIT](LICENSE)
