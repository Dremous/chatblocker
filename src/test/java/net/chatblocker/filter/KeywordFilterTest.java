package net.chatblocker.filter;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
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
        assertFalse(KeywordFilter.containsBlockedKeyword("hello world", Arrays.asList("", "   ", null)));
        assertTrue(KeywordFilter.containsBlockedKeyword("hello world", List.of("", "world")));
    }
}
