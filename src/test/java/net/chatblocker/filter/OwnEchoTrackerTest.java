package net.chatblocker.filter;

import org.junit.jupiter.api.Test;

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

    /** 超过存活时间后不再匹配（懒清理） */
    @Test
    void expireAfterTtl() throws Exception {
        OwnEchoTracker tracker = new OwnEchoTracker(50L);
        tracker.record("过期消息");

        Thread.sleep(80L);

        assertFalse(tracker.matches("<玩家> 过期消息"));
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
}
