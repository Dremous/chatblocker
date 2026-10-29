# ChatBlocker

Minecraft 26.3 / Fabric 纯客户端聊天关键词屏蔽 Mod：**别人**发送的含关键词消息在自己的客户端不显示，**自己**发送的消息正常显示（豁免）。同时覆盖服务器公告等系统消息；离线服务器按文本匹配豁免自己消息的回显（可配置关闭）。

## 功能特性

- 屏蔽别人发送的含关键词聊天消息（仅安装者自己看不到，其他玩家正常可见；使用**包含匹配**而非全词匹配）
- 自己发送的消息不受影响：在线服务器按发送者 UUID 判定；离线服务器无发送者信息，按回显文本匹配豁免
- `/say`、`/me`、`/msg` 等会产生聊天回显的命令也会被记录，避免自己的回显被自己屏蔽
- 屏蔽服务器公告、插件消息等系统消息（命中关键词时不显示），action bar 消息始终放行
- 包含匹配 + 不区分大小写（中文、英文均适用，含关键词的消息即被屏蔽，非全词匹配）
- 游戏内命令实时管理关键词
- ModMenu + YACL 图形配置界面
- JSON 配置文件持久化（原子写入，损坏自动容错，不崩溃；保存失败会提示）

## 安装

1. 安装 [Fabric Loader](https://fabricmc.net/use/)（Minecraft 26.3，Java 25+；Loader 需 0.20.1 或更高）
2. 将 `chatblocker-1.1.1+26.3.jar` 放入 `.minecraft/mods/`
3. 前置依赖：
   - [Fabric API](https://modrinth.com/mod/fabric-api)（0.165.0+26.3 或更高）
   - [YetAnotherConfigLib (YACL)](https://modrinth.com/mod/yacl)（3.9.7+26.3-fabric 或更高）
   - 可选：[ModMenu](https://modrinth.com/mod/modmenu)（21.0.0+，提供配置界面入口）

## 使用

### 游戏内命令

| 命令 | 说明 |
|---|---|
| `/chatblocker add <关键词>` | 添加屏蔽关键词（忽略大小写去重并立即写盘） |
| `/chatblocker remove <关键词>` | 移除屏蔽关键词 |
| `/chatblocker list` | 列出当前全部关键词与启用状态 |
| `/chatblocker reload` | 从配置文件重新加载 |

单独输入 `/chatblocker` 会显示用法提示。

### 配置界面

主菜单 → **Mods** → **ChatBlocker** → 配置按钮（需安装 ModMenu），可编辑"启用屏蔽"开关与关键词列表。

### 自己消息的豁免规则

| 场景 | 判定方式 |
|---|---|
| 在线服务器（消息带发送者 UUID） | 只比较 UUID，他人复述你的话仍会被屏蔽 |
| 离线服务器（消息无发送者） | 10 秒内、按文本匹配自己刚发出的内容 |

离线回显的文本匹配对短内容更严格：不超过 3 个字符的词要求出现在消息**结尾**（避免发出 `1` 之后，把别人含 `1` 的消息全部放行）；4 个字符及以上仍按包含匹配。可用 `exemptOwnEcho` 关闭整个回显豁免。

### 配置文件

位置：`.minecraft/config/chatblocker.json`

```json
{
  "enabled": true,
  "exemptOwnEcho": true,
  "keywords": ["示例词1", "badword"]
}
```

保存时先写同目录临时文件再原子替换，进程中断不会把已有配置截断；单个关键词超过 256 字符会被忽略。

## 从源码构建

```powershell
.\gradlew.bat build
```

产物位于 `build/libs/chatblocker-1.1.1+26.3.jar`（需 JDK 25+）。本机默认 `JAVA_HOME` 若是 21，需先指向 JDK 25。

## 发布流程

1. 更新 `gradle.properties` 中的 `mod_version`，并在 `CHANGELOG.md` 顶部新增对应版本段落（最终版本号 = mod 版本 + 游戏版本）
2. 提交后推送标签触发 GitHub Actions 自动发布：
   ```powershell
   git tag v1.1.1+26.3
   git push origin v1.1.1+26.3
   ```
3. CI 会校验标签版本与 `gradle.properties` 一致、`CHANGELOG.md` 存在对应段落，然后依次构建、上传到 [Modrinth](https://modrinth.com/mod/chatblocker_) 与 CurseForge，最后创建 GitHub Release（需在仓库 Settings → Secrets 配置 `MODRINTH_TOKEN` 与 `CURSEFORGE_TOKEN`）

## 许可证

[MIT](LICENSE)
