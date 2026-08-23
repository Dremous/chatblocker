# AGENTS.md

## 项目概览

- ChatBlocker：Minecraft 1.21.8 / Fabric **纯客户端**聊天关键词屏蔽 Mod（modid: `chatblocker`，包名 `net.chatblocker`，Mojang 官方映射，无 mixin）
- 远程仓库：https://github.com/Dremous/chatblocker ，分支 `main`
- 行为语义：**别人**发送的含关键词消息自己不显示；**自己**发送的消息豁免不屏蔽

## 构建（本机代理是硬性前置）

本机 Clash 代理拦截 JVM 流量，**联网构建前必须设置代理**，否则 TLS 中断 / 403：

```powershell
$env:GRADLE_OPTS="-Dhttp.proxyHost=::1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=::1 -Dhttps.proxyPort=7897"
```

- 全量构建 + 测试：`.\gradlew.bat build`
- 运行单个测试类：`.\gradlew.bat test --tests "net.chatblocker.filter.KeywordFilterTest"`
- 启动客户端：`.\gradlew.bat runClient` —— GUI 会阻塞命令行；冒烟测试需用 WMI 后台启动（`Invoke-CimInstance Win32_Process`），轮询 `run/logs/latest.log` 直到出现 `Loading 53 mods` / `Setting user:` / `Backend library: LWJGL`，验证 mod 加载行 `- chatblocker 1.0.0`

## 版本集中管理

所有版本号在 `gradle.properties`（minecraft 1.21.8、loader 0.19.3、loom 1.17-SNAPSHOT、fabric-api 0.136.1+1.21.8、yacl 3.8.2+1.21.6-fabric、modmenu 15.0.2）；Gradle wrapper 9.5.1；编译目标 Java 21（本机 JDK 25 可用）。不要在 build.gradle 硬编码版本。

## 源码布局（Loom splitEnvironmentSourceSets）

- `src/client/java` — 客户端专用类（入口 `ChatBlockMod`、事件、命令、GUI）
- `src/main/java` — 公共纯逻辑（`KeywordFilter`、`MessageFilter`、`ChatBlockConfig`）
- `src/test/java` — **只能引用 `src/main` 的类，不能引用 `src/client`**（否则编译失败）。需要单元测试的逻辑必须放 `src/main`。

## 关键 API 差异（容易踩坑）

- 客户端命令执行方法参数是 `FabricClientCommandSource`（fabric-command-api-v2），**不是** `CommandSourceStack`；反馈用其 `sendFeedback(Component)`
- YACL 3.8.2：保存回调是 `.save(Runnable)`（**没有** `savingRunnable`）；`ListOption` 必须通过 `ConfigCategory.Builder#group(...)` 添加而非 `.option(...)`；控制器 Builder 在 `dev.isxander.yacl3.api.controller` 包（`TickBoxControllerBuilder`、`string.StringControllerBuilder`）
- 消息拦截：`ClientReceiveMessageEvents.ALLOW_CHAT` 返回 `false` 阻止显示；判定逻辑集中在 `MessageFilter.shouldBlock`（src/main，可单测）
- `ChatBlockConfig.loadFromDisk()` 失败时**保留内存配置**并返回 false（不重置）；`saveToDisk()`/`loadFromDisk()` 为 synchronized；`getKeywords()` 返回内部可变列表（修改后需调用 `saveToDisk()`）
- 命令为 `/chatblocker add|remove|list|reload`

## 运行时

- 配置文件：`.minecraft/config/chatblocker.json`（modid 曾为 `chatblock`，旧配置文件名为 `chatblock.json`）
- `run/` 是 dev 运行目录；`logs/` 是运行时日志

## 仓库约定

- 提交信息用中文 conventional 风格（`feat:` / `fix:` / `docs:` / `chore:` / `rename:`），每次任务改动后提交并推送
- `docs/` 与 `MODRINTH_DESCRIPTION.md` 已被 `.gitignore` 排除——它们存在于本地但**不得提交推送**（Modrinth 介绍文案放后者）
- 许可证 MIT（`LICENSE` 与 `fabric.mod.json` 需一致）
- `fabric.mod.json` 的 minecraft 约束为 `~1.21.8`；作者已实测该 jar 在 1.21.11 可直接运行（跨版本依赖引用面小 + 稳定 Fabric API）
