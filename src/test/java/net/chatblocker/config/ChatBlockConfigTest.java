package net.chatblocker.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** ChatBlockConfig 的单元测试：默认值、读写往返、损坏回退、失败保留内存。 */
class ChatBlockConfigTest {

    /** 临时目录由 JUnit 自动创建与清理 */
    @TempDir
    Path tempDir;

    /** 文件不存在时返回默认配置，loadFromDisk 返回 false */
    @Test
    void loadDefaultWhenFileMissing() {
        ChatBlockConfig config = ChatBlockConfig.fromDisk(tempDir.resolve("not_exist.json"));
        assertTrue(config.isEnabled());
        assertTrue(config.getKeywords().isEmpty());
        assertFalse(config.loadFromDisk());
    }

    /** 保存后重新加载，内容一致（读写往返） */
    @Test
    void saveAndLoadRoundTrip() {
        Path file = tempDir.resolve("chatblocker.json");
        // fromDisk 对不存在的文件返回默认配置实例
        ChatBlockConfig config = ChatBlockConfig.fromDisk(file);
        config.setEnabled(false);
        config.setKeywords(List.of("垃圾", "badword"));
        config.saveToDisk();

        ChatBlockConfig loaded = ChatBlockConfig.fromDisk(file);
        assertFalse(loaded.isEnabled());
        assertEquals(List.of("垃圾", "badword"), loaded.getKeywords());
        assertTrue(loaded.loadFromDisk());
    }

    /** JSON 损坏时回退默认配置且不抛异常，loadFromDisk 返回 false */
    @Test
    void fallbackWhenJsonBroken() throws Exception {
        Path file = tempDir.resolve("broken.json");
        Files.writeString(file, "{ 这不是合法 JSON !!! ");

        ChatBlockConfig config = ChatBlockConfig.fromDisk(file);
        assertTrue(config.isEnabled());
        assertTrue(config.getKeywords().isEmpty());
        assertFalse(config.loadFromDisk());
    }

    /** JSON 字面 null 时不崩溃，回退默认配置 */
    @Test
    void fallbackWhenJsonLiteralNull() throws Exception {
        Path file = tempDir.resolve("null.json");
        Files.writeString(file, "null");

        ChatBlockConfig config = ChatBlockConfig.fromDisk(file);
        assertTrue(config.isEnabled());
        assertTrue(config.getKeywords().isEmpty());
    }

    /** 加载失败时保留当前内存配置，不重置 */
    @Test
    void keepMemoryConfigWhenReloadFails() throws Exception {
        Path file = tempDir.resolve("broken2.json");
        ChatBlockConfig config = ChatBlockConfig.fromDisk(file);
        config.setEnabled(false);
        config.setKeywords(List.of("keep"));
        Files.writeString(file, "{{{ 损坏");

        assertFalse(config.loadFromDisk());
        // 内存配置未被重置
        assertFalse(config.isEnabled());
        assertEquals(List.of("keep"), config.getKeywords());
    }

    /** 加载时过滤空白与 null 关键词 */
    @Test
    void filterBlankKeywordsOnLoad() throws Exception {
        Path file = tempDir.resolve("with_blanks.json");
        Files.writeString(file, "{\"enabled\": true, \"keywords\": [\"ok\", \"\", \"   \", null]}");

        ChatBlockConfig config = ChatBlockConfig.fromDisk(file);
        assertEquals(List.of("ok"), config.getKeywords());
    }

    /** JSON 缺少 enabled 字段时使用默认值 true */
    @Test
    void defaultEnabledWhenFieldMissing() throws Exception {
        Path file = tempDir.resolve("no_enabled.json");
        Files.writeString(file, "{\"keywords\": [\"test\"]}");

        ChatBlockConfig config = ChatBlockConfig.fromDisk(file);
        assertTrue(config.isEnabled());
        assertEquals(List.of("test"), config.getKeywords());
    }

    /** 配置文件路径不参与序列化 */
    @Test
    void filePathNotSerialized() throws Exception {
        Path file = tempDir.resolve("no_path.json");
        // fromDisk 对不存在的文件返回默认配置实例
        ChatBlockConfig config = ChatBlockConfig.fromDisk(file);
        config.setKeywords(List.of("test"));
        config.saveToDisk();

        String json = Files.readString(file);
        assertFalse(json.contains("filePath"));
    }
}
