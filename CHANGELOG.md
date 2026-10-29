# 更新日志

本项目遵循[语义化版本](https://semver.org/lang/zh-CN/)。

## 1.1.1+26.3

- 修复：配置文件改为「临时文件 + 原子替换」写入，中断或磁盘异常不再截断已有配置；`saveToDisk()` 返回结果，GUI 与命令在保存失败时明确提示
- 修复：离线服务器回显豁免对短文本改为「全等或结尾匹配」，避免发出 `1`、`ok` 之后把别人包含该字符的消息一并放行
- 修复：回显记录最多保留 32 条并改为单调时钟计时，长时间游戏不再无限增长，系统时间回拨也不会让旧记录超期存活；断开连接时清空
- 修复：他人复述你的话不再被误豁免——消息带发送者 UUID 时只按 UUID 判定，只有匿名消息才按文本匹配
- 修复：离线回显豁免只在你确实处于游戏中时生效，避免其他界面误放行
- 修复：关键词在添加与替换时忽略大小写去重，单个关键词限制 256 字符；`getKeywords()` 改为不可变快照
- 修复：带 UTF-8 BOM 或空文件的配置不再回退默认值失败的问题（BOM 自动跳过，空文件容错）
- 修复：`/say`、`/me`、`/msg`、`/tell`、`/w`、`/r` 的聊天回显不再被自己屏蔽
- 修复：`/chatblocker` 无子命令时显示用法；`remove` 空关键词给出提示；`list` 显示启用状态
- 优化：入站判定抽为 `InboundMessageFilter`，玩家聊天与系统消息共用一套逻辑
- 构建：删除未使用的 `maven-publish`；发布游戏版本列表统一为 `publishGameVersions`（自动 trim、忽略空项、留空回退 `minecraft_version`）
- 构建：移除 Minotaur 的 `failSilently`，上传失败不再被静默吞掉
- 构建：Gradle wrapper 增加 SHA-256 校验、超时放宽至 60 秒并允许重试
- CI：仅在本地启用阿里云镜像，GitHub Actions 只走官方源；发布流程改为「构建 → 平台上传 → 创建 Release」，并校验标签版本与 CHANGELOG 段落
- 依赖：`fabric.mod.json` 的 `fabric-api` 下界改为 `>=0.165.0`
- 文档：README 补充豁免规则、命令回显与发布校验说明

## 1.1.0+26.3

- 适配 Minecraft 26.3（Fabric API 0.165.0+26.3、YACL 3.9.7+26.3-fabric、ModMenu 21.0.0、Fabric Loader 0.20.1）
- 构建：Gradle wrapper 升级至 9.8.2，Fabric Loom 升级至 1.19.3
- CI：构建校验改为覆盖所有分支（版本分支也纳入校验，tag push 不重复触发）

## 1.1.0+26.2

- 新增：语言文件支持（en_us / zh_cn），ModMenu 可显示对应翻译
- 新增：GitHub Actions 常规构建校验 CI（PR/push）
- 新增：SPDX 许可证文件头
- 修复：`setKeywords` 未 trim 首尾空格，`getKeywords` 暴露可变内部列表改为不可变视图
- 修复：action bar 消息不再被误屏蔽
- 优化：Gradle 构建配置（toolchain、archivesName、JVM 内存 2G、阿里云 Maven 镜像）
- 优化：KeywordFilter 新增 `preNormalize` 预缓存方法
- 优化：入口类添加启动日志
- 文档：README 强化包含匹配说明，contact 补全

## 1.0.5+26.2

- 版本号统一为 1.0.5

## 1.0.3+26.2

- 适配 Minecraft 26.2（Fabric API 0.158.0+26.2、YACL 3.9.6+26.2-fabric、ModMenu 20.0.1）
- 更新 Mod 图标

## 1.0.3+26.1.2

- 适配 Minecraft 26.1.2（Fabric API 0.155.2+26.1.2、YACL 3.9.2+26.1-fabric、ModMenu 18.0.0）
- 迁移至无混淆构建（fabric-loom 插件、Java 25、官方 Mojang 命名）

## 1.0.3+1.21.1

- 适配 Minecraft 1.21.1（Fabric API 0.116.5+1.21.1、YACL 3.8.1+1.21.1-fabric、ModMenu 11.0.3）

## 1.0.3

- 修复：`/chatblocker add` 与 `remove` 改为忽略大小写，与屏蔽匹配语义一致（去重不再区分大小写）

## 1.0.2

- 新增：离线服务器自己消息回显豁免（配置项 exemptOwnEcho，默认开启）
- 配置界面新增"豁免自己消息的回显"开关

## 1.0.1

- 修复：多人服务器上聊天屏蔽失效（匿名消息与系统消息）
- 新增：屏蔽服务器公告、插件消息等系统消息（ALLOW_GAME 通道）
- 修复：离线服务器匿名聊天消息（发送者为 null）不再一律放行

## 1.0.0

- 首个版本：屏蔽别人发送的含关键词消息，自己发送的消息豁免
- 游戏内命令、ModMenu + YACL 配置界面、JSON 配置持久化
