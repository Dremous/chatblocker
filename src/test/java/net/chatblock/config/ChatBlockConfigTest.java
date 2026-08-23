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
        // fromDisk 对不存在的文件返回默认配置实例
        ChatBlockConfig config = ChatBlockConfig.fromDisk(file);
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
        // fromDisk 对不存在的文件返回默认配置实例
        ChatBlockConfig config = ChatBlockConfig.fromDisk(file);
        config.setFilePath(file);
        config.setKeywords(List.of("test"));
        config.saveToDisk();

        String json = Files.readString(file);
        assertFalse(json.contains("filePath"));
    }
}
