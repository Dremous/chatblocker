# ChatBlock 聊天关键词屏蔽 Mod 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现一个 Minecraft 1.21.8 Fabric 纯客户端 Mod，使玩家自己发送的包含屏蔽关键词的聊天消息仅自己不显示（其他玩家正常可见）。

**Architecture:** 纯客户端、无 mixin。监听 Fabric API 的 `ClientReceiveMessageEvents.ALLOW_CHAT` 事件，当消息发送者是自己且文本命中关键词时返回 `false` 阻止显示。配置由 `ChatBlockConfig`（Gson + JSON 文件）管理，提供 Brigadier 客户端命令 `/chatblock add|remove|list|reload` 与 YACL + ModMenu 图形配置界面。代码采用 Mojang 官方映射（`src/client` 客户端代码与 `src/main` 公共代码分离）。

**Tech Stack:** Java 21、Fabric Loader 0.19.3、Fabric API 0.136.1+1.21.8、Fabric Loom 1.17-SNAPSHOT（officialMojangMappings）、Gradle 9.5.1（Wrapper）、YACL 3.8.2+1.21.6-fabric、ModMenu 15.0.2、JUnit 5.11.4、Gson 2.11.0（测试用）

**设计文档:** `docs/superpowers/specs/2026-08-23-chatblock-mod-design.md`

**版本速查（已全部在线核实）：**

| 项 | 值 |
|---|---|
| minecraft_version | 1.21.8 |
| loader_version | 0.19.3 |
| loom_version | 1.17-SNAPSHOT |
| fabric_api_version | 0.136.1+1.21.8 |
| modmenu_version | 15.0.2 |
| yacl_version | 3.8.2+1.21.6-fabric |
| 模板来源 | github.com/FabricMC/fabric-example-mod 分支 1.21.8 |

---

### Task 0: 项目脚手架（模板下载、改造、首次构建）

**Files:**
- Create: `build.gradle`、`gradle.properties`、`settings.gradle`、`fabric.mod.json`、`gradlew.bat`、`gradle/wrapper/*`、`src/main/resources/assets/chatblock/icon.png`、`.gitattributes`、`LICENSE`
- Modify: `.gitignore`

- [ ] **Step 1: 下载并解压官方模板到工作目录**

Run:
```powershell
Invoke-WebRequest "https://github.com/FabricMC/fabric-example-mod/archive/refs/heads/1.21.8.zip" -OutFile "C:\Users\L1950\AppData\Local\Temp\opencode\fem.zip"
Expand-Archive "C:\Users\L1950\AppData\Local\Temp\opencode\fem.zip" -DestinationPath "C:\Users\L1950\AppData\Local\Temp\opencode\fem" -Force
Copy-Item "C:\Users\L1950\AppData\Local\Temp\opencode\fem\fabric-example-mod-1.21.8\*" -Destination "E:\ai\chatBlock" -Recurse -Force
```
Expected: 工作目录出现 `build.gradle`、`gradle.properties`、`gradlew.bat`、`gradle/`、`src/` 等模板文件。

- [ ] **Step 2: 删除模板示例代码**

Run:
```powershell
Remove-Item "E:\ai\chatBlock\src\client" -Recurse -Force
Remove-Item "E:\ai\chatBlock\src\main\java" -Recurse -Force
Remove-Item "E:\ai\chatBlock\src\main\resources\modid.mixins.json" -Force
Remove-Item "E:\ai\chatBlock\src\main\resources\assets\modid" -Recurse -Force
Remove-Item "E:\ai\chatBlock\.github" -Recurse -Force
Remove-Item "E:\ai\chatBlock\README.md" -Force
```
Expected: `src/` 下仅剩 `src/main/resources/fabric.mod.json`。

- [ ] **Step 3: 创建资源目录并放置图标**

Run:
```powershell
New-Item -ItemType Directory -Path "E:\ai\chatBlock\src\main\resources\assets\chatblock" -Force | Out-Null
Copy-Item "C:\Users\L1950\AppData\Local\Temp\opencode\fem\fabric-example-mod-1.21.8\src\main\resources\assets\modid\icon.png" -Destination "E:\ai\chatBlock\src\main\resources\assets\chatblock\icon.png" -Force
```
Expected: `src/main/resources/assets/chatblock/icon.png` 存在。

- [ ] **Step 4: 改写 `gradle.properties`**

Create `gradle.properties`（覆盖模板文件）：

```properties
# 提高 Gradle 可用内存
org.gradle.jvmargs=-Xmx1G
org.gradle.parallel=true

# IntelliJ IDEA 与配置缓存尚未完全兼容
org.gradle.configuration-cache=false

# Fabric 属性
# 版本请以 https://fabricmc.net/develop 为准
minecraft_version=1.21.8
loader_version=0.19.3
loom_version=1.17-SNAPSHOT

# Mod 属性
version=1.0.0
group=net.chatblock

# 依赖版本
fabric_api_version=0.136.1+1.21.8
modmenu_version=15.0.2
yacl_version=3.8.2+1.21.6-fabric
```

- [ ] **Step 5: 改写 `settings.gradle`**

Modify `settings.gradle`，将项目名改为 modid 一致的名字：

```gradle
pluginManagement {
	repositories {
		maven {
			name = 'Fabric'
			url = 'https://maven.fabricmc.net/'
		}
		mavenCentral()
		gradlePluginPortal()
	}
}

// 项目名应匹配 modid
rootProject.name = 'chatblock'
```

- [ ] **Step 6: 改写 `build.gradle`（加 YACL/ModMenu 仓库与依赖、JUnit 测试配置）**

Create `build.gradle`（覆盖模板文件）：

```gradle
plugins {
	id 'net.fabricmc.fabric-loom-remap' version "${loom_version}"
	id 'maven-publish'
}

repositories {
	// ModMenu 仓库
	maven {
		name = 'Terraformers'
		url = 'https://maven.terraformersmc.com/releases/'
	}
	// YACL 仓库
	maven {
		name = 'Xander Maven'
		url = 'https://maven.isxander.dev/releases'
	}
}

loom {
	// 客户端代码与公共代码分离（src/client 与 src/main）
	splitEnvironmentSourceSets()

	mods {
		"chatblock" {
			sourceSet sourceSets.main
			sourceSet sourceSets.client
		}
	}
}

dependencies {
	// 版本见 gradle.properties
	minecraft "com.mojang:minecraft:${project.minecraft_version}"
	mappings loom.officialMojangMappings()
	modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"

	// Fabric API：提供聊天事件与客户端命令 API
	modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}"

	// ModMenu：Mod 列表中的配置入口
	modImplementation "com.terraformersmc:modmenu:${project.modmenu_version}"

	// YACL：配置界面库
	modImplementation "dev.isxander:yet-another-config-lib:${project.yacl_version}"

	// 单元测试
	testImplementation "org.junit.jupiter:junit-jupiter:5.11.4"
	testRuntimeOnly "org.junit.platform:junit-platform-launcher:1.11.4"
	// 测试源集不继承 Minecraft 的 Gson，显式声明
	testImplementation "com.google.code.gson:gson:2.11.0"
}

processResources {
	def version = project.version
	inputs.property "version", version

	filesMatching("fabric.mod.json") {
		expand "version": version
	}
}

tasks.withType(JavaCompile).configureEach {
	it.options.release = 21
}

java {
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}

test {
	useJUnitPlatform()
}

jar {
	def projectName = project.name
	inputs.property "projectName", projectName

	from("LICENSE") {
		rename { "${it}_$projectName"}
	}
}

// 配置 Maven 发布
publishing {
	publications {
		create("mavenJava", MavenPublication) {
			from components.java
		}
	}

	repositories {
		// 发布用仓库留空，本项目仅本地构建
	}
}
```

- [ ] **Step 7: 改写 `fabric.mod.json`**

Create `src/main/resources/fabric.mod.json`（覆盖模板文件）：

```json
{
	"schemaVersion": 1,
	"id": "chatblock",
	"version": "${version}",
	"name": "ChatBlock",
	"description": "聊天关键词屏蔽：自己发送的含关键词消息仅自己不显示，其他玩家正常可见。",
	"authors": [
		"L1950"
	],
	"contact": {},
	"license": "CC0-1.0",
	"icon": "assets/chatblock/icon.png",
	"environment": "client",
	"entrypoints": {
		"client": [
			"net.chatblock.ChatBlockMod"
		]
	},
	"depends": {
		"fabricloader": ">=0.19.3",
		"minecraft": "~1.21.8",
		"java": ">=21",
		"fabric-api": "*",
		"modmenu": "*",
		"yet_another_config_lib_v3": "*"
	}
}
```

- [ ] **Step 8: 检查 `.gitignore` 覆盖运行目录**

Run:
```powershell
Get-Content "E:\ai\chatBlock\.gitignore"
```
Expected: 若缺少以下条目则追加到文件末尾：`run/`、`out/`、`.vscode/`。追加命令：
```powershell
Add-Content "E:\ai\chatBlock\.gitignore" "`n# Fabric Loom 运行目录`nrun/`n# 其他 IDE`n.vscode/`nout/"
```

- [ ] **Step 9: 首次构建验证**

Run:
```powershell
.\gradlew.bat build --console=plain
```
Expected: 首次运行下载 Gradle 9.5.1 与全部依赖（约 5-15 分钟），最终输出 `BUILD SUCCESSFUL`，`build/libs/chatblock-1.0.0.jar` 生成。
若失败：检查错误信息（常见为网络中断，重试即可）。

- [ ] **Step 10: 提交**

```powershell
git add -A
git commit -m "脚手架：基于 fabric-example-mod 1.21.8 模板搭建 chatblock 项目（Loom 1.17 + Mojang 映射 + YACL/ModMenu/JUnit 依赖）"
```

---

### Task 1: KeywordFilter 关键词匹配（TDD 纯逻辑）

**Files:**
- Create: `src/main/java/net/chatblock/filter/KeywordFilter.java`
- Test: `src/test/java/net/chatblock/filter/KeywordFilterTest.java`

- [ ] **Step 1: 写失败测试**

Create `src/test/java/net/chatblock/filter/KeywordFilterTest.java`：

```java
package net.chatblock.filter;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** KeywordFilter 的单元测试：包含匹配 + 不区分大小写 */
class KeywordFilterTest {

    /** 中文关键词命中 */
    @Test
    void hitChineseKeyword() {
        assertTrue(KeywordFilter.containsBlockedKeyword("我觉得这真垃圾啊", List.of("垃圾")));
    }

    /** 英文关键词命中 */
    @Test
    void hitEnglishKeyword() {
        assertTrue(KeywordFilter.containsBlockedKeyword("this is badword", List.of("badword")));
    }

    /** 消息大写、关键词小写仍命中（不区分大小写） */
    @Test
    void hitWhenMessageUppercase() {
        assertTrue(KeywordFilter.containsBlockedKeyword("THIS IS BADWORD", List.of("badword")));
    }

    /** 关键词大写、消息小写仍命中（不区分大小写） */
    @Test
    void hitWhenKeywordUppercase() {
        assertTrue(KeywordFilter.containsBlockedKeyword("this is badword", List.of("BADWORD")));
    }

    /** 未命中返回 false */
    @Test
    void missWhenNoKeyword() {
        assertFalse(KeywordFilter.containsBlockedKeyword("你好，今天天气不错", List.of("badword", "垃圾")));
    }

    /** 关键词是消息的一部分（包含匹配语义） */
    @Test
    void hitWhenKeywordIsSubstring() {
        assertTrue(KeywordFilter.containsBlockedKeyword("我讨厌badword这种词", List.of("badword")));
    }

    /** 空消息返回 false */
    @Test
    void missWhenMessageBlank() {
        assertFalse(KeywordFilter.containsBlockedKeyword("   ", List.of("badword")));
    }

    /** null 消息返回 false */
    @Test
    void missWhenMessageNull() {
        assertFalse(KeywordFilter.containsBlockedKeyword(null, List.of("badword")));
    }

    /** 空关键词列表返回 false */
    @Test
    void missWhenKeywordsEmpty() {
        assertFalse(KeywordFilter.containsBlockedKeyword("hello world", List.of()));
    }

    /** 忽略空白与 null 关键词，仅有效关键词参与匹配 */
    @Test
    void ignoreBlankKeywords() {
        assertFalse(KeywordFilter.containsBlockedKeyword("hello world", List.of("", "   ", null)));
        assertTrue(KeywordFilter.containsBlockedKeyword("hello world", List.of("", "world")));
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `.\gradlew.bat test --tests "net.chatblock.filter.KeywordFilterTest" --console=plain`
Expected: FAIL（编译错误：找不到 `KeywordFilter` 类）。

- [ ] **Step 3: 实现最小代码使测试通过**

Create `src/main/java/net/chatblock/filter/KeywordFilter.java`：

```java
package net.chatblock.filter;

import java.util.Collection;
import java.util.Locale;

/** 聊天关键词过滤器：包含匹配、不区分大小写。纯逻辑，不依赖 Minecraft 类。 */
public final class KeywordFilter {

    private KeywordFilter() {
    }

    /**
     * 判断消息是否命中任一屏蔽关键词。
     *
     * @param message  待检测的消息文本
     * @param keywords 屏蔽关键词集合
     * @return 命中返回 true；消息为空、关键词为空或全部无效时返回 false
     */
    public static boolean containsBlockedKeyword(String message, Collection<String> keywords) {
        // 消息为空或没有有效关键词时直接放行
        if (message == null || message.isBlank() || keywords == null || keywords.isEmpty()) {
            return false;
        }
        // 统一转小写实现大小写不敏感
        String normalized = message.toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            // 跳过空白与 null 关键词
            if (keyword == null || keyword.isBlank()) {
                continue;
            }
            if (normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `.\gradlew.bat test --tests "net.chatblock.filter.KeywordFilterTest" --console=plain`
Expected: PASS（10 个测试全部通过）。

- [ ] **Step 5: 提交**

```powershell
git add -A
git commit -m "feat: KeywordFilter 关键词匹配逻辑（包含匹配 + 不区分大小写）及单元测试"
```

---

### Task 2: ChatBlockConfig 配置模型（TDD）

**Files:**
- Create: `src/main/java/net/chatblock/config/ChatBlockConfig.java`
- Test: `src/test/java/net/chatblock/config/ChatBlockConfigTest.java`

- [ ] **Step 1: 写失败测试**

Create `src/test/java/net/chatblock/config/ChatBlockConfigTest.java`：

```java
package net.chatblock.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** ChatBlockConfig 的单元测试：默认值、读写往返、损坏回退。 */
class ChatBlockConfigTest {

    /** 临时目录由 JUnit 自动创建与清理 */
    @TempDir
    Path tempDir;

    /** 文件不存在时返回默认配置 */
    @Test
    void loadDefaultWhenFileMissing() {
        ChatBlockConfig config = ChatBlockConfig.fromDisk(tempDir.resolve("not_exist.json"));
        assertTrue(config.isEnabled());
        assertTrue(config.getKeywords().isEmpty());
    }

    /** 保存后重新加载，内容一致（读写往返） */
    @Test
    void saveAndLoadRoundTrip() {
        Path file = tempDir.resolve("chatblock.json");
        ChatBlockConfig config = new ChatBlockConfig();
        config.setFilePath(file);
        config.setEnabled(false);
        config.setKeywords(List.of("垃圾", "badword"));
        config.saveToDisk();

        ChatBlockConfig loaded = ChatBlockConfig.fromDisk(file);
        assertFalse(loaded.isEnabled());
        assertEquals(List.of("垃圾", "badword"), loaded.getKeywords());
    }

    /** JSON 损坏时回退默认配置且不抛异常 */
    @Test
    void fallbackWhenJsonBroken() throws Exception {
        Path file = tempDir.resolve("broken.json");
        Files.writeString(file, "{ 这不是合法 JSON !!! ");

        ChatBlockConfig config = ChatBlockConfig.fromDisk(file);
        assertTrue(config.isEnabled());
        assertTrue(config.getKeywords().isEmpty());
    }

    /** 加载时过滤空白与 null 关键词 */
    @Test
    void filterBlankKeywordsOnLoad() throws Exception {
        Path file = tempDir.resolve("with_blanks.json");
        Files.writeString(file, "{\"enabled\": true, \"keywords\": [\"ok\", \"\", \"   \", null]}");

        ChatBlockConfig config = ChatBlockConfig.fromDisk(file);
        assertEquals(List.of("ok"), config.getKeywords());
    }

    /** 配置文件路径不参与序列化 */
    @Test
    void filePathNotSerialized() throws Exception {
        Path file = tempDir.resolve("no_path.json");
        ChatBlockConfig config = new ChatBlockConfig();
        config.setFilePath(file);
        config.setKeywords(List.of("test"));
        config.saveToDisk();

        String json = Files.readString(file);
        assertFalse(json.contains("filePath"));
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `.\gradlew.bat test --tests "net.chatblock.config.ChatBlockConfigTest" --console=plain`
Expected: FAIL（编译错误：找不到 `ChatBlockConfig` 类）。

- [ ] **Step 3: 实现最小代码使测试通过**

Create `src/main/java/net/chatblock/config/ChatBlockConfig.java`：

```java
package net.chatblock.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Mod 配置模型：enabled 开关 + 屏蔽关键词列表。
 * 游戏内使用单例 INSTANCE；静态工厂 fromDisk(Path) 供测试使用。
 */
public final class ChatBlockConfig {

    /** 全局单例，Mod 入口在初始化时设置文件路径并加载 */
    public static final ChatBlockConfig INSTANCE = new ChatBlockConfig();

    private static final Logger LOGGER = LoggerFactory.getLogger("chatblock");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** 是否启用屏蔽 */
    private boolean enabled = true;

    /** 屏蔽关键词列表（原始书写，匹配时忽略大小写） */
    private List<String> keywords = new ArrayList<>();

    /** 配置文件路径，不参与序列化 */
    private transient Path filePath;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** 返回内部可变列表；修改后需调用 saveToDisk() 持久化 */
    public List<String> getKeywords() {
        return keywords;
    }

    /** 替换关键词列表，自动过滤 null 与空白项 */
    public void setKeywords(List<String> keywords) {
        this.keywords = new ArrayList<>();
        if (keywords != null) {
            for (String keyword : keywords) {
                if (keyword != null && !keyword.isBlank()) {
                    this.keywords.add(keyword);
                }
            }
        }
    }

    /** 设置配置文件路径（由 Mod 入口在启动时调用一次） */
    public void setFilePath(Path filePath) {
        this.filePath = filePath;
    }

    /**
     * 从磁盘加载配置到当前实例。
     *
     * @return 成功解析返回 true；文件不存在或损坏返回 false（内存中已回退为默认配置）
     */
    public boolean loadFromDisk() {
        if (filePath == null || !Files.exists(filePath)) {
            return false;
        }
        try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            ChatBlockConfig loaded = GSON.fromJson(reader, ChatBlockConfig.class);
            this.enabled = loaded.enabled;
            this.setKeywords(loaded.keywords);
            return true;
        } catch (IOException | JsonParseException e) {
            LOGGER.warn("配置文件解析失败，已回退默认配置：{}", filePath, e);
            this.enabled = true;
            this.keywords = new ArrayList<>();
            return false;
        }
    }

    /** 将当前配置写入磁盘；路径未设置时静默跳过。写入失败仅记录警告，不影响游戏。 */
    public void saveToDisk() {
        if (filePath == null) {
            return;
        }
        try {
            Files.createDirectories(filePath.getParent());
            try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            LOGGER.warn("配置写入失败：{}", filePath, e);
        }
    }

    /** 工厂方法：从指定路径加载并返回新实例（供测试与独立加载使用） */
    public static ChatBlockConfig fromDisk(Path path) {
        ChatBlockConfig config = new ChatBlockConfig();
        config.setFilePath(path);
        config.loadFromDisk();
        return config;
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `.\gradlew.bat test --tests "net.chatblock.config.ChatBlockConfigTest" --console=plain`
Expected: PASS（5 个测试全部通过）。
若报 `NoClassDefFoundError: org/slf4j/LoggerFactory`：说明测试 classpath 缺少 slf4j，在 `build.gradle` 的 dependencies 块追加 `testImplementation "org.slf4j:slf4j-api:2.0.16"` 后重跑。

- [ ] **Step 5: 提交**

```powershell
git add -A
git commit -m "feat: ChatBlockConfig 配置模型（Gson 读写、损坏回退）及单元测试"
```

---

### Task 3: Mod 入口与消息过滤事件

**Files:**
- Create: `src/client/java/net/chatblock/ChatBlockMod.java`
- Create: `src/client/java/net/chatblock/event/ChatMessageHandler.java`

- [ ] **Step 1: 创建 Mod 入口类**

Create `src/client/java/net/chatblock/ChatBlockMod.java`：

```java
package net.chatblock;

import net.chatblock.config.ChatBlockConfig;
import net.chatblock.event.ChatMessageHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/** Mod 客户端入口：加载配置并注册事件监听。 */
public class ChatBlockMod implements ClientModInitializer {

    /** Mod ID */
    public static final String MOD_ID = "chatblock";

    @Override
    public void onInitializeClient() {
        // 配置文件位于 .minecraft/config/chatblock.json
        ChatBlockConfig.INSTANCE.setFilePath(
                FabricLoader.getInstance().getConfigDir().resolve("chatblock.json"));
        ChatBlockConfig.INSTANCE.loadFromDisk();

        // 注册消息过滤事件
        ChatMessageHandler.register();
    }
}
```

- [ ] **Step 2: 创建消息过滤事件处理器**

Create `src/client/java/net/chatblock/event/ChatMessageHandler.java`：

```java
package net.chatblock.event;

import net.chatblock.config.ChatBlockConfig;
import net.chatblock.filter.KeywordFilter;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;

/**
 * 聊天消息过滤：当服务器广播的消息发送者是自己且命中关键词时，
 * 返回 false 阻止该消息在本地聊天框显示。其他玩家的客户端不受影响。
 */
public final class ChatMessageHandler {

    private ChatMessageHandler() {
    }

    /** 注册过滤回调（Mod 入口调用一次） */
    public static void register() {
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            ChatBlockConfig config = ChatBlockConfig.INSTANCE;
            // 屏蔽功能关闭时放行
            if (!config.isEnabled()) {
                return true;
            }
            Minecraft client = Minecraft.getInstance();
            // 不在游戏中或发送者信息缺失时放行（不误伤系统/插件消息）
            if (client.player == null || sender == null) {
                return true;
            }
            // 只过滤自己发送的消息，别人的消息正常显示
            if (!sender.getId().equals(client.player.getUUID())) {
                return true;
            }
            // 命中关键词则阻止显示
            return !KeywordFilter.containsBlockedKeyword(message.getString(), config.getKeywords());
        });
    }
}
```

- [ ] **Step 3: 编译验证**

Run: `.\gradlew.bat build --console=plain`
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 4: 提交**

```powershell
git add -A
git commit -m "feat: Mod 入口与消息过滤事件（ALLOW_CHAT 拦截自己发送的命中消息）"
```

---

### Task 4: 客户端命令 /chatblock

**Files:**
- Create: `src/client/java/net/chatblock/command/ChatBlockCommand.java`
- Modify: `src/client/java/net/chatblock/ChatBlockMod.java`（注册命令）

- [ ] **Step 1: 创建命令类**

Create `src/client/java/net/chatblock/command/ChatBlockCommand.java`：

```java
package net.chatblock.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.chatblock.config.ChatBlockConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.List;

/** 客户端命令 /chatblock add|remove|list|reload，用于维护屏蔽关键词。 */
public final class ChatBlockCommand {

    private ChatBlockCommand() {
    }

    /** 注册命令树（Mod 入口调用一次） */
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) ->
                dispatcher.register(ClientCommandManager.literal("chatblock")
                        .then(ClientCommandManager.literal("add")
                                .then(ClientCommandManager.argument("keyword", StringArgumentType.greedyString())
                                        .executes(ChatBlockCommand::executeAdd)))
                        .then(ClientCommandManager.literal("remove")
                                .then(ClientCommandManager.argument("keyword", StringArgumentType.greedyString())
                                        .executes(ChatBlockCommand::executeRemove)))
                        .then(ClientCommandManager.literal("list")
                                .executes(ChatBlockCommand::executeList))
                        .then(ClientCommandManager.literal("reload")
                                .executes(ChatBlockCommand::executeReload))));
    }

    /** /chatblock add <关键词>：添加关键词并立即写盘 */
    private static int executeAdd(CommandContext<CommandSourceStack> context) {
        String keyword = StringArgumentType.getString(context, "keyword").trim();
        CommandSourceStack source = context.getSource();
        ChatBlockConfig config = ChatBlockConfig.INSTANCE;

        if (keyword.isEmpty()) {
            source.sendFeedback(Component.literal("§c关键词不能为空"));
            return 0;
        }
        if (config.getKeywords().contains(keyword)) {
            source.sendFeedback(Component.literal("§e关键词已存在：" + keyword));
            return 0;
        }
        config.getKeywords().add(keyword);
        config.saveToDisk();
        source.sendFeedback(Component.literal("§a已添加关键词：" + keyword));
        return 1;
    }

    /** /chatblock remove <关键词>：移除关键词并立即写盘 */
    private static int executeRemove(CommandContext<CommandSourceStack> context) {
        String keyword = StringArgumentType.getString(context, "keyword").trim();
        CommandSourceStack source = context.getSource();
        ChatBlockConfig config = ChatBlockConfig.INSTANCE;

        if (!config.getKeywords().remove(keyword)) {
            source.sendFeedback(Component.literal("§e关键词不存在：" + keyword));
            return 0;
        }
        config.saveToDisk();
        source.sendFeedback(Component.literal("§a已移除关键词：" + keyword));
        return 1;
    }

    /** /chatblock list：列出当前全部关键词 */
    private static int executeList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        List<String> keywords = ChatBlockConfig.INSTANCE.getKeywords();

        if (keywords.isEmpty()) {
            source.sendFeedback(Component.literal("当前没有屏蔽关键词"));
        } else {
            source.sendFeedback(Component.literal(
                    "当前屏蔽关键词（" + keywords.size() + " 个）：" + String.join("、", keywords)));
        }
        return 1;
    }

    /** /chatblock reload：从磁盘重新加载配置 */
    private static int executeReload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        boolean success = ChatBlockConfig.INSTANCE.loadFromDisk();

        if (success) {
            source.sendFeedback(Component.literal("§a已重新加载配置"));
        } else {
            source.sendFeedback(Component.literal("§c配置文件不存在或损坏，已回退默认配置"));
        }
        return 1;
    }
}
```

- [ ] **Step 2: 在入口类中注册命令**

Modify `src/client/java/net/chatblock/ChatBlockMod.java`，在 `import` 区追加：
```java
import net.chatblock.command.ChatBlockCommand;
```
并在 `onInitializeClient` 方法末尾（`ChatMessageHandler.register();` 之后）追加：
```java
        // 注册客户端命令
        ChatBlockCommand.register();
```

- [ ] **Step 3: 编译验证**

Run: `.\gradlew.bat build --console=plain`
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 4: 提交**

```powershell
git add -A
git commit -m "feat: /chatblock add|remove|list|reload 客户端命令"
```

---

### Task 5: YACL 配置界面与 ModMenu 集成

**Files:**
- Create: `src/client/java/net/chatblock/gui/ChatBlockConfigScreen.java`
- Create: `src/client/java/net/chatblock/gui/ChatBlockModMenu.java`
- Modify: `src/main/resources/fabric.mod.json`（增加 modmenu 入口点）

- [ ] **Step 1: 创建 YACL 配置界面工厂**

Create `src/client/java/net/chatblock/gui/ChatBlockConfigScreen.java`：

```java
package net.chatblock.gui;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.gui.controllers.TickBoxControllerBuilder;
import dev.isxander.yacl3.gui.controllers.string.StringControllerBuilder;
import net.chatblock.config.ChatBlockConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** YACL 配置界面工厂：每次调用生成新的配置 Screen 实例。 */
public final class ChatBlockConfigScreen {

    private ChatBlockConfigScreen() {
    }

    /** 基于当前配置生成配置界面；parent 为关闭界面后返回的屏幕 */
    public static Screen create(Screen parent) {
        ChatBlockConfig config = ChatBlockConfig.INSTANCE;

        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("ChatBlock 设置"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("屏蔽设置"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("启用屏蔽"))
                                .description(OptionDescription.of(
                                        Component.literal("关闭后不再过滤任何消息")))
                                .binding(true, config::isEnabled, config::setEnabled)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(ListOption.<String>createBuilder()
                                .name(Component.literal("屏蔽关键词"))
                                .description(OptionDescription.of(
                                        Component.literal("消息包含任一关键词时，自己不显示该消息（不区分大小写）")))
                                .binding(List.of(), config::getKeywords, config::setKeywords)
                                .controller(StringControllerBuilder::create)
                                .initial("")
                                .build())
                        .build())
                // 点击保存按钮时写盘
                .savingRunnable(config::saveToDisk)
                .build()
                .generateScreen(parent);
    }
}
```

- [ ] **Step 2: 创建 ModMenu 入口实现**

Create `src/client/java/net/chatblock/gui/ChatBlockModMenu.java`：

```java
package net.chatblock.gui;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** ModMenu 集成：在 Mod 列表中为本 Mod 提供配置按钮。 */
public class ChatBlockModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ChatBlockConfigScreen::create;
    }
}
```

- [ ] **Step 3: 注册 ModMenu 入口点**

Modify `src/main/resources/fabric.mod.json`，将 `entrypoints` 改为：

```json
	"entrypoints": {
		"client": [
			"net.chatblock.ChatBlockMod"
		],
		"modmenu": [
			"net.chatblock.gui.ChatBlockModMenu"
		]
	},
```

- [ ] **Step 4: 编译验证**

Run: `.\gradlew.bat build --console=plain`
Expected: `BUILD SUCCESSFUL`。
若报 YACL 相关编译错误（API 形态差异），对照 `C:\Users\L1950\.gradle\caches\modules-2\files-2.1\dev.isxander\yet-another-config-lib` 中解压的源码或 javadoc 调整方法名，保持界面功能不变。

- [ ] **Step 5: 提交**

```powershell
git add -A
git commit -m "feat: YACL 配置界面与 ModMenu 集成"
```

---

### Task 6: 元数据收尾与全量验证

**Files:**
- Modify: `src/main/resources/fabric.mod.json`（最终检查）
- Verify: 全量构建产物

- [ ] **Step 1: 核对 fabric.mod.json 最终内容**

Read `src/main/resources/fabric.mod.json`，确认与以下一致（重点：`id` 为 `chatblock`、`environment` 为 `client`、两个入口点齐全、depends 含 yacl 与 modmenu）：

```json
{
	"schemaVersion": 1,
	"id": "chatblock",
	"version": "${version}",
	"name": "ChatBlock",
	"description": "聊天关键词屏蔽：自己发送的含关键词消息仅自己不显示，其他玩家正常可见。",
	"authors": [
		"L1950"
	],
	"contact": {},
	"license": "CC0-1.0",
	"icon": "assets/chatblock/icon.png",
	"environment": "client",
	"entrypoints": {
		"client": [
			"net.chatblock.ChatBlockMod"
		],
		"modmenu": [
			"net.chatblock.gui.ChatBlockModMenu"
		]
	},
	"depends": {
		"fabricloader": ">=0.19.3",
		"minecraft": "~1.21.8",
		"java": ">=21",
		"fabric-api": "*",
		"modmenu": "*",
		"yet_another_config_lib_v3": "*"
	}
}
```
如有差异，以本内容为准修正。

- [ ] **Step 2: 全量构建（含单元测试）**

Run: `.\gradlew.bat clean build --console=plain`
Expected: `BUILD SUCCESSFUL`，测试任务通过；`build/libs/chatblock-1.0.0.jar` 与 `chatblock-1.0.0-sources.jar` 生成。

- [ ] **Step 3: 核对最终文件清单**

Run:
```powershell
Get-ChildItem "E:\ai\chatBlock\src" -Recurse -File | ForEach-Object { $_.FullName.Replace("E:\ai\chatBlock\", "") }
```
Expected:
```
src/client/java/net/chatblock/ChatBlockMod.java
src/client/java/net/chatblock/command/ChatBlockCommand.java
src/client/java/net/chatblock/event/ChatMessageHandler.java
src/client/java/net/chatblock/gui/ChatBlockConfigScreen.java
src/client/java/net/chatblock/gui/ChatBlockModMenu.java
src/main/java/net/chatblock/config/ChatBlockConfig.java
src/main/java/net/chatblock/filter/KeywordFilter.java
src/main/resources/fabric.mod.json
src/main/resources/assets/chatblock/icon.png
src/test/java/net/chatblock/config/ChatBlockConfigTest.java
src/test/java/net/chatblock/filter/KeywordFilterTest.java
```

- [ ] **Step 4: 提交**

```powershell
git add -A
git commit -m "chore: 元数据核对与全量构建验证"
```

---

### Task 7: 手动集成验证（runClient）

**Files:** 无代码改动，仅验证。

- [ ] **Step 1: 启动客户端**

Run: `.\gradlew.bat runClient --console=plain`
Expected: Minecraft 1.21.8 窗口启动。首次会生成 `run/` 目录。
注意：需要先安装对应版本的 Fabric Loader 客户端或依赖 Loom 自动处理的开发环境（Loom 会自动注入 loader 与依赖 mod，无需手动安装）。启动后主菜单左下角应显示 Fabric 与 ModMenu 按钮。

- [ ] **Step 2: 验证 ModMenu 集成**

操作：主菜单 → Mods → 找到 ChatBlock → 点击配置按钮。
Expected: 打开 "ChatBlock 设置" 界面，含"启用屏蔽"开关与"屏蔽关键词"列表；修改后点保存，检查 `run/config/chatblock.json` 内容已更新。

- [ ] **Step 3: 验证命令**

操作：进入单人世界（创建新世界），依次执行：
1. `/chatblock add 垃圾` → 提示"已添加关键词：垃圾"
2. `/chatblock add 垃圾` → 提示"关键词已存在"
3. `/chatblock add badword` → 提示"已添加关键词：badword"
4. `/chatblock list` → 列出两个关键词
5. `/chatblock remove badword` → 提示"已移除关键词：badword"
6. `/chatblock reload` → 提示"已重新加载配置"
Expected: 提示内容与上述一致；`run/config/chatblock.json` 实时更新。

- [ ] **Step 4: 验证核心屏蔽功能**

操作：单人世界聊天框发送消息：
1. 发送 `这个地图真垃圾` → **自己的聊天框不显示该消息**
2. 发送 `今天天气不错` → 正常显示
3. 发送 `THIS IS BADWORD`（先将 badword 加回）→ 不显示（大小写不敏感生效）
4. 通过配置界面关闭"启用屏蔽" → 发送含关键词消息 → 正常显示
Expected: 与上述一致。

- [ ] **Step 5: 验证配置文件损坏回退**

操作：关闭游戏，手动把 `run/config/chatblock.json` 内容改成非法 JSON（如 `{{{`），重新 `.\gradlew.bat runClient`，执行 `/chatblock list`。
Expected: 游戏正常启动不崩溃，列表显示"当前没有屏蔽关键词"，日志中有警告。

- [ ] **Step 6: 多客户端验证（可选，需局域网或服务器）**

操作：同一局域网开放世界，第二台电脑（未装本 Mod）进入；本机发送含关键词消息。
Expected: 本机看不到，第二台客户端正常看到消息。

- [ ] **Step 7: 完成标记**

所有验证通过后，在回复中汇总验证结果；无需额外提交。

---

## Self-Review 记录

- **Spec 覆盖**：设计文档第 1-8 节均有对应任务——屏蔽行为（Task 3）、JSON+命令配置（Task 2/4）、包含匹配不区分大小写（Task 1）、YACL+ModMenu 界面（Task 5）、数据流与错误处理（Task 2/3）、测试（Task 1/2 单测 + Task 7 集成）、脚手架版本（Task 0）。范围外项均未实现。
- **占位符扫描**：无 TBD/TODO；所有步骤含完整代码或精确命令。
- **类型一致性**：`KeywordFilter.containsBlockedKeyword(String, Collection<String>)` 在 Task 1 定义、Task 3 调用，签名一致；`ChatBlockConfig` 的 `INSTANCE`、`isEnabled()`、`setEnabled(boolean)`、`getKeywords()`、`setKeywords(List)`、`setFilePath(Path)`、`loadFromDisk()`、`saveToDisk()`、`fromDisk(Path)` 在 Task 2 定义，Task 3/4/5 调用一致；入口类 `ChatBlockMod` 位于 `src/client/java/net/chatblock/`，与 fabric.mod.json 的 `net.chatblock.ChatBlockMod` 一致。
