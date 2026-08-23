# ChatBlock 聊天关键词屏蔽 Mod 设计文档

- 日期：2026-08-23
- 目标平台：Minecraft 1.21.8 / Fabric（纯客户端）
- ModID：`chatblock`，包名：`net.chatblock`

## 1. 需求概述

制作一个 Fabric 客户端 Mod：当**玩家自己**发送的聊天消息包含屏蔽关键词时，**仅自己看不到**该消息；消息正常发往服务器，其他玩家正常可见。

需求要点（已与用户确认）：

| 项目 | 决定 |
|---|---|
| 屏蔽行为 | 仅自己看不到（消息照常发送与广播） |
| 关键词配置 | JSON 配置文件 + 游戏内命令，两者结合 |
| 匹配规则 | 包含匹配，不区分大小写（中文天然不受影响） |
| 图形界面 | 需要 ModMenu + YACL 配置界面 |
| 实现形态 | 纯客户端 Mod，无 mixin |

## 2. 架构与组件

```
src/main/java/net/chatblock/
├── ChatBlockMod.java           # 入口：ClientModInitializer，加载配置、注册事件与命令
├── config/
│   └── ChatBlockConfig.java    # 配置模型（enabled + keywords），Gson 读写 config/chatblock.json
├── filter/
│   └── KeywordFilter.java      # 纯逻辑：包含匹配 + 不区分大小写
├── event/
│   └── ChatMessageHandler.java # 注册 ClientReceiveMessageEvents.ALLOW_CHAT 回调
├── command/
│   └── ChatBlockCommand.java   # Brigadier 客户端命令 /chatblock add|remove|list|reload
└── gui/
    ├── ChatBlockConfigScreen.java # YACL 生成配置界面
    └── ChatBlockModMenu.java      # ModMenuApi 实现，提供 ModMenu 入口
```

### 技术选型与版本

| 依赖 | 版本/说明 |
|---|---|
| Minecraft | 1.21.8 |
| Yarn 映射 | 1.21.8+build.1 |
| Fabric Loader | 1.21.8 对应最新版（以模板生成器为准） |
| Fabric API | 0.133.4+1.21.8 |
| YACL | 3.8.x（fabric，兼容 1.21.6–1.21.8，实施时从 Modrinth 确认精确版本） |
| ModMenu | 1.21.8 对应最新版 |
| Java | 编译目标 Java 21（本机 JDK 25 可用） |
| 构建 | Gradle（使用 Wrapper，本机无全局 Gradle） |

`fabric.mod.json` 标记 `"environment": "client"`，声明依赖 fabric-api、yet_another_config_lib_v3、modmenu。

### 关键 API（已通过 1.21.8 Javadoc 验证）

- `ClientReceiveMessageEvents.ALLOW_CHAT`：回调签名
  `boolean allowReceiveChatMessage(Text message, @Nullable SignedMessage signedMessage, @Nullable GameProfile sender, MessageType.Parameters params, Instant receptionTimestamp)`
  返回 `false` 则消息不显示并触发 `CHAT_CANCELED`。
- 判定"是自己发的消息"：`sender != null && sender.getId().equals(MinecraftClient.getInstance().player.getUuid())`。
- 已确认 1.21.1+ 客户端发送消息**没有本地即时回显**（`ChatScreen.sendMessage` 仅调 `networkHandler.sendChatMessage`），服务器回传路径即唯一显示路径，因此该事件覆盖全部显示场景，无需 mixin。

## 3. 配置设计

文件位置：`config/chatblock.json`

```json
{
  "enabled": true,
  "keywords": ["示例词1", "badword"]
}
```

行为约定：

- 启动时加载；文件不存在则生成默认配置（`enabled=true`，空关键词列表）。
- 命令修改后立即写盘；`/chatblock reload` 从磁盘重读。
- 配置文件保留关键词原始书写；大小写不敏感在匹配阶段处理（`toLowerCase(Locale.ROOT)`）。
- 所有写盘操作串行化（`synchronized`），避免命令与 GUI 并发写入。

## 4. 命令设计

| 命令 | 行为 |
|---|---|
| `/chatblock add <关键词>` | 添加；去重、忽略空白词；已存在时给出提示 |
| `/chatblock remove <关键词>` | 移除；不存在时给出提示 |
| `/chatblock list` | 列出全部关键词；空列表显示"暂无" |
| `/chatblock reload` | 重读配置文件；解析失败时提示错误并保留当前内存配置 |

命令通过 `ClientCommandRegistrationCallback` + `ClientCommandManager`（Brigadier）注册，为客户端命令。

## 5. 数据流

1. 玩家在聊天框输入消息 → 客户端正常发送（**不拦截发送**）。
2. 服务器广播消息 → 本客户端触发 `ClientReceiveMessageEvents.ALLOW_CHAT`。
3. `ChatMessageHandler` 判定：
   - `enabled == false` → 放行；
   - 玩家不在游戏中（`player == null`）→ 放行；
   - `sender == null` → 放行（不误伤系统/插件消息）；
   - `sender` 不是自己 → 放行（别人的消息正常显示）；
   - `sender` 是自己且文本（小写化后）包含任一关键词 → 返回 `false`，消息不在自己聊天框显示。
4. 其他玩家的客户端：`sender` 不是他们自己 → 正常显示，屏蔽仅对发送者本人生效。

## 6. 错误处理

| 场景 | 处理 |
|---|---|
| 配置文件不存在 | 生成默认配置 |
| JSON 损坏/格式错误 | 日志警告 + 保留当前内存配置（启动时内存即默认配置，效果等同回退），不崩溃；`reload` 命令提示失败 |
| 事件回调时玩家为 null | 放行 |
| 事件回调时 sender 为 null | 放行 |
| 并发写配置 | 写盘方法 synchronized 串行化 |
| 关键词为空字符串或空白 | add 命令拒绝；加载时过滤 |

## 7. 测试与验证

- **JUnit 单元测试**：
  - `KeywordFilter`：命中、未命中、大小写不敏感、中文关键词、空消息、空关键词列表、空白消息。
  - `ChatBlockConfig`：默认生成、读写往返、损坏 JSON 回退。
- **构建验证**：`gradlew build`（含测试）通过。
- **手动集成测试**：`gradlew runClient` 启动，单人世界（集成服务器，走服务器广播路径）验证：
  1. 发送含关键词消息 → 自己的聊天框不显示；
  2. 发送正常消息 → 正常显示；
  3. `/chatblock add/remove/list/reload` 行为正确；
  4. ModMenu 中打开配置界面可编辑关键词。

## 8. 范围外（YAGNI）

- 不做服务端组件、不做全员屏蔽；
- 不做正则匹配（仅包含匹配）；
- 不做关键词白名单/权限体系；
- 不做多语言本地化（界面文案用简体中文）；
- 不发布到 Modrinth/CurseForge（仅本地构建使用）。
