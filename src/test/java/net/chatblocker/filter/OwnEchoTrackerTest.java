// SPDX-License-Identifier: MIT
package net.chatblocker.filter;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OwnEchoTracker 的单元测试：记录自己发出的文本，回显消息包含原文时判定为自己的回显。
 */
class OwnEchoTrackerTest {

    /** 记录原文后，包含该原文的消息判定为自己的回显 */
    @Test
    void matchRecordedEcho() {
        OwnEchoTracker tracker = new OwnEchoTracker(10_000L);
        tracker.record("这地图真垃圾");

        assertTrue(tracker.matches("<玩家> 这地图真垃圾"));
    }

    /** 未记录过的文本不匹配 */
    @Test
    void missWhenNotRecorded() {
        OwnEchoTracker tracker = new OwnEchoTracker(10_000L);
        tracker.record("你好");

        assertFalse(tracker.matches("<玩家> 今天天气不错"));
    }

    /** 大小写不敏感：自己发出的英文原文，回显大小写不同仍匹配 */
    @Test
    void matchCaseInsensitive() {
        OwnEchoTracker tracker = new OwnEchoTracker(10_000L);
        tracker.record("Hello BadWord");

        assertTrue(tracker.matches("[玩家] hello badword!"));
    }

    /** 超过存活时间后不再匹配（懒清理），使用可注入时钟避免依赖真实等待 */
    @Test
    void expireAfterTtl() {
        AtomicLong now = new AtomicLong(0L);
        OwnEchoTracker tracker = new OwnEchoTracker(50L, now::get);
        tracker.record("过期消息");

        now.set(51L);

        assertFalse(tracker.matches("<玩家> 过期消息"));
    }

    /** 存活时间边界：恰好等于 TTL 仍命中，超过 1 毫秒才失效 */
    @Test
    void ttlBoundaryIsInclusive() {
        AtomicLong now = new AtomicLong(0L);
        OwnEchoTracker tracker = new OwnEchoTracker(1_000L, now::get);
        tracker.record("long-enough-text");

        now.set(1_000L);
        assertTrue(tracker.matches("<玩家> long-enough-text"));

        now.set(1_001L);
        assertFalse(tracker.matches("<玩家> long-enough-text"));
    }

    /** 部分条目过期时只清理过期项，未过期项仍可匹配 */
    @Test
    void partialExpiryKeepsFreshEntries() {
        AtomicLong now = new AtomicLong(0L);
        OwnEchoTracker tracker = new OwnEchoTracker(1_000L, now::get);
        tracker.record("first-long-text");
        now.set(600L);
        tracker.record("second-long-text");

        now.set(1_001L);

        assertFalse(tracker.matches("<玩家> first-long-text"));
        assertTrue(tracker.matches("<玩家> second-long-text"));
    }

    /** 断开连接时清空记录，避免换服后误豁免 */
    @Test
    void clearRemovesAllEntries() {
        OwnEchoTracker tracker = new OwnEchoTracker(10_000L);
        tracker.record("long-enough-text");
        tracker.clear();

        assertFalse(tracker.matches("<玩家> long-enough-text"));
    }

    /** 空文本与空白文本记录后不参与匹配 */
    @Test
    void ignoreBlankRecord() {
        OwnEchoTracker tracker = new OwnEchoTracker(10_000L);
        tracker.record("   ");

        assertFalse(tracker.matches("<玩家> 今天天气不错"));
    }

    /** 无记录时匹配空文本返回 false，不抛异常 */
    @Test
    void matchEmptyMessage() {
        OwnEchoTracker tracker = new OwnEchoTracker(10_000L);

        assertFalse(tracker.matches(""));
    }

    /**
     * 短文本（不超过 3 字符）只按「结尾相同」判定回显，
     * 避免把自己发的 "1" 误当成任何包含 1 的他人消息。
     */
    @Test
    void shortTextRequiresSuffixMatch() {
        OwnEchoTracker tracker = new OwnEchoTracker(10_000L);
        tracker.record("1");

        assertFalse(tracker.matches("<玩家> 你没有 1 个空位"));
        assertTrue(tracker.matches("<玩家> 1"));
    }

    /** 两字中文关键词命中自己的回显（结尾匹配），但不会误豁免句中出现的同一词 */
    @Test
    void shortChineseKeywordMatchesSuffixOnly() {
        OwnEchoTracker tracker = new OwnEchoTracker(10_000L);
        tracker.record("垃圾");

        assertTrue(tracker.matches("<玩家> 这地图真垃圾"));
        assertFalse(tracker.matches("<玩家> 垃圾场真好玩"));
    }

    /** 记录数超过上限时丢弃最旧的条目，避免长期游戏内无限增长 */
    @Test
    void evictOldestWhenExceedingCapacity() {
        OwnEchoTracker tracker = new OwnEchoTracker(10_000L);
        for (int i = 0; i < 40; i++) {
            tracker.record("message-number-" + i + "-long");
        }

        assertFalse(tracker.matches("<玩家> message-number-0-long"));
        assertTrue(tracker.matches("<玩家> message-number-39-long"));
    }
}
