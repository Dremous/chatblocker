# ChatBlocker

Minecraft 1.21.8 / Fabric 纯客户端聊天关键词屏蔽 Mod：**别人**发送的含关键词消息在自己的客户端不显示，**自己**发送的消息正常显示（豁免）。同时覆盖服务器公告等系统消息；离线服务器按文本匹配豁免自己消息的回显（可配置关闭）。

## 功能特性

- 屏蔽别人发送的含关键词聊天消息（仅安装者自己看不到，其他玩家正常可见）
- 自己发送的消息不受影响（离线服务器按回显文本匹配豁免，别人数秒内发送完全相同文本时可能被误豁免）
- 屏蔽服务器公告、插件消息等系统消息（命中关键词时不显示）
- 包含匹配 + 不区分大小写（中文、英文均适用）
- 游戏内命令实时管理关键词
- ModMenu + YACL 图形配置界面
- JSON 配置文件持久化（损坏自动容错，不崩溃）

## 安装

1. 安装 [Fabric Loader](https://fabricmc.net/use/)（Minecraft 1.21.8，Java 21+）
2. 将 `chatblocker-1.0.2.jar` 放入 `.minecraft/mods/`
3. 前置依赖：
   - [Fabric API](https://modrinth.com/mod/fabric-api)（0.136.1+1.21.8）
   - [YetAnotherConfigLib (YACL)](https://modrinth.com/mod/yacl)（3.8.x）
   - 可选：[ModMenu](https://modrinth.com/mod/modmenu)（15.0.2+，提供配置界面入口）

## 使用

### 游戏内命令

| 命令 | 说明 |
|---|---|
| `/chatblocker add <关键词>` | 添加屏蔽关键词（自动去重并写盘） |
| `/chatblocker remove <关键词>` | 移除屏蔽关键词 |
| `/chatblocker list` | 列出当前全部关键词 |
| `/chatblocker reload` | 从配置文件重新加载 |

### 配置界面

主菜单 → Mods → chatblocker → 配置按钮（需安装 ModMenu），可编辑"启用屏蔽"开关与关键词列表。

### 配置文件

位置：`.minecraft/config/chatblocker.json`

```json
{
  "enabled": true,
  "exemptOwnEcho": true,
  "keywords": ["示例词1", "badword"]
}
```

## 从源码构建

```powershell
.\gradlew.bat build
```

产物位于 `build/libs/chatblocker-1.0.2.jar`（需 JDK 21+）。

## 发布流程

1. 更新 `gradle.properties` 中的 `version`，并在 `CHANGELOG.md` 顶部新增对应版本段落
2. 提交后推送标签触发 GitHub Actions 自动发布：
   ```powershell
   git tag v1.0.3
   git push origin v1.0.3
   ```
3. CI 自动构建并上传到 [Modrinth](https://modrinth.com/mod/chatblocker_) 与 CurseForge（需在仓库 Settings → Secrets 配置 `MODRINTH_TOKEN` 与 `CURSEFORGE_TOKEN`）

## 许可证

[MIT](LICENSE)
