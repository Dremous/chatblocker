// SPDX-License-Identifier: MIT
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

    /** 新实例 exemptOwnEcho 默认为 true */
    @Test
    void exemptOwnEchoDefaultTrue() {
        // fromDisk 对不存在的文件返回默认配置实例
        ChatBlockConfig config = ChatBlockConfig.fromDisk(tempDir.resolve("not_exist_exempt.json"));
        assertTrue(config.isExemptOwnEcho());
    }

    /** exemptOwnEcho 保存后重新加载，值一致（读写往返） */
    @Test
    void exemptOwnEchoRoundTrip() {
        Path file = tempDir.resolve("exempt.json");
        ChatBlockConfig config = ChatBlockConfig.fromDisk(file);
        config.setExemptOwnEcho(false);
        config.saveToDisk();

        ChatBlockConfig loaded = ChatBlockConfig.fromDisk(file);
        assertFalse(loaded.isExemptOwnEcho());
    }

    /** JSON 缺少 exemptOwnEcho 字段时使用默认值 true（兼容旧配置文件） */
    @Test
    void defaultExemptOwnEchoWhenFieldMissing() throws Exception {
        Path file = tempDir.resolve("no_exempt.json");
        Files.writeString(file, "{\"enabled\": true, \"keywords\": [\"test\"]}");

        ChatBlockConfig config = ChatBlockConfig.fromDisk(file);
        assertTrue(config.isExemptOwnEcho());
    }

    /** addKeyword 正常添加新词 */
    @Test
    void addKeywordAppends() {
        ChatBlockConfig config = ChatBlockConfig.fromDisk(tempDir.resolve("add_ok.json"));
        assertTrue(config.addKeyword("垃圾"));
        assertEquals(List.of("垃圾"), config.getKeywords());
    }

    /** addKeyword 忽略大小写去重：已存在同词（大小写不同）时拒绝添加 */
    @Test
    void addKeywordRejectsCaseInsensitiveDuplicate() {
        ChatBlockConfig config = ChatBlockConfig.fromDisk(tempDir.resolve("add_dup.json"));
        config.addKeyword("BadWord");

        assertFalse(config.addKeyword("badword"));
        assertEquals(List.of("BadWord"), config.getKeywords());
    }

    /** addKeyword 拒绝空白词 */
    @Test
    void addKeywordRejectsBlank() {
        ChatBlockConfig config = ChatBlockConfig.fromDisk(tempDir.resolve("add_blank.json"));
        assertFalse(config.addKeyword("   "));
        assertTrue(config.getKeywords().isEmpty());
    }

    /** removeKeyword 忽略大小写移除已存在的词 */
    @Test
    void removeKeywordCaseInsensitive() {
        ChatBlockConfig config = ChatBlockConfig.fromDisk(tempDir.resolve("remove_ok.json"));
        config.addKeyword("BadWord");

        assertTrue(config.removeKeyword("badword"));
        assertTrue(config.getKeywords().isEmpty());
    }

    /** removeKeyword 移除不存在的词返回 false */
    @Test
    void removeKeywordMissing() {
        ChatBlockConfig config = ChatBlockConfig.fromDisk(tempDir.resolve("remove_miss.json"));
        config.addKeyword("垃圾");

        assertFalse(config.removeKeyword("badword"));
        assertEquals(List.of("垃圾"), config.getKeywords());
    }

    /** 保存成功返回 true，并自动创建不存在的父目录 */
    @Test
    void saveReturnsTrueAndCreatesParentDirectories() {
        Path file = tempDir.resolve("nested/dir/chatblocker.json");
        ChatBlockConfig config = ChatBlockConfig.fromDisk(file);
        config.setKeywords(List.of("垃圾"));

        assertTrue(config.saveToDisk());
        assertTrue(Files.exists(file));
    }

    /** 写入失败时返回 false 且不抛异常（父路径是普通文件） */
    @Test
    void saveReturnsFalseWhenWriteFails() throws Exception {
        Path blocker = tempDir.resolve("blocker");
        Files.writeString(blocker, "占位文件");
        ChatBlockConfig config = ChatBlockConfig.fromDisk(blocker.resolve("config.json"));

        assertFalse(config.saveToDisk());
    }

    /** 保存后不残留临时文件 */
    @Test
    void saveLeavesNoTemporaryFile() throws Exception {
        Path file = tempDir.resolve("atomic.json");
        ChatBlockConfig config = ChatBlockConfig.fromDisk(file);
        config.setKeywords(List.of("垃圾"));

        config.saveToDisk();

        try (var stream = Files.list(tempDir)) {
            long leftovers = stream
                    .filter(p -> p.getFileName().toString().endsWith(".tmp"))
                    .count();
            assertEquals(0L, leftovers);
        }
    }

    /** 带 UTF-8 BOM 的配置文件仍能正常加载（Windows 手工编辑常见） */
    @Test
    void loadFileWithBom() throws Exception {
        Path file = tempDir.resolve("bom.json");
        Files.writeString(file, "\uFEFF{\"enabled\": false, \"keywords\": [\"bom\"]}");

        ChatBlockConfig config = ChatBlockConfig.fromDisk(file);
        assertFalse(config.isEnabled());
        assertEquals(List.of("bom"), config.getKeywords());
    }

    /** 零字节文件回退默认配置，loadFromDisk 返回 false */
    @Test
    void fallbackWhenFileEmpty() throws Exception {
        Path file = tempDir.resolve("empty.json");
        Files.createFile(file);

        ChatBlockConfig config = ChatBlockConfig.fromDisk(file);
        assertTrue(config.isEnabled());
        assertTrue(config.getKeywords().isEmpty());
        assertFalse(config.loadFromDisk());
    }

    /** keywords 字段为 null 时按空列表处理，不抛异常 */
    @Test
    void nullKeywordsFieldLoadsEmptyList() throws Exception {
        Path file = tempDir.resolve("null_keywords.json");
        Files.writeString(file, "{\"enabled\": true, \"keywords\": null}");

        ChatBlockConfig config = ChatBlockConfig.fromDisk(file);
        assertTrue(config.getKeywords().isEmpty());
    }

    /** 缺少 enabled 字段时沿用默认值 true，而不是被 Gson 静默置为 false */
    @Test
    void missingEnabledFieldKeepsDefault() throws Exception {
        Path file = tempDir.resolve("missing_enabled.json");
        Files.writeString(file, "{\"keywords\": [\"垃圾\"]}");

        ChatBlockConfig config = ChatBlockConfig.fromDisk(file);
        assertTrue(config.isEnabled());
        assertEquals(List.of("垃圾"), config.getKeywords());
    }

    /** 缺少 exemptOwnEcho 字段时沿用默认值 true */
    @Test
    void missingExemptOwnEchoKeepsDefault() throws Exception {
        Path file = tempDir.resolve("missing_exempt.json");
        Files.writeString(file, "{\"keywords\": [\"垃圾\"]}");

        ChatBlockConfig config = ChatBlockConfig.fromDisk(file);
        assertTrue(config.isExemptOwnEcho());
    }

    /** 字段显式为 null 时同样回退默认值，避免手写配置把开关写成 null 后失效 */
    @Test
    void nullBooleanFieldKeepsDefault() throws Exception {
        Path file = tempDir.resolve("null_enabled.json");
        Files.writeString(file, "{\"enabled\": null, \"exemptOwnEcho\": null, \"keywords\": []}");

        ChatBlockConfig config = ChatBlockConfig.fromDisk(file);
        assertTrue(config.isEnabled());
        assertTrue(config.isExemptOwnEcho());
    }

    /** setKeywords 忽略大小写去重，保留首次出现的写法 */
    @Test
    void setKeywordsDeduplicatesCaseInsensitively() {
        ChatBlockConfig config = ChatBlockConfig.fromDisk(tempDir.resolve("dedupe.json"));

        config.setKeywords(List.of("BadWord", "badword", "BADWORD", "垃圾"));

        assertEquals(List.of("BadWord", "垃圾"), config.getKeywords());
    }

    /** 超长关键词被拒绝，避免 GUI 写入病态长度的词 */
    @Test
    void rejectTooLongKeyword() {
        ChatBlockConfig config = ChatBlockConfig.fromDisk(tempDir.resolve("too_long.json"));
        String tooLong = "x".repeat(ChatBlockConfig.MAX_KEYWORD_LENGTH + 1);

        assertFalse(config.addKeyword(tooLong));
        config.setKeywords(List.of(tooLong, "ok"));
        assertEquals(List.of("ok"), config.getKeywords());
    }
}
