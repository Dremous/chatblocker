# 更新日志

本项目遵循[语义化版本](https://semver.org/lang/zh-CN/)。

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
