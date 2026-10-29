// SPDX-License-Identifier: MIT
package net.chatblocker.filter;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * InboundMessageFilter 的单元测试：整合「关键词命中 + 自己豁免 + 匿名回显豁免」的完整判定，
 * 覆盖 senderId 为 null 时查询 tracker 的门控逻辑。
 */
class InboundMessageFilterTest {

    /** 本地玩家与其他玩家 UUID */
    private static final UUID SELF = UUID.randomUUID();
    private static final UUID OTHER = UUID.randomUUID();

    /** 测试关键词 */
    private static final List<String> KEYWORDS = List.of("垃圾");

    /** 匿名消息命中关键词且匹配自己刚发的回显 → 放行（离线服务器场景） */
    @Test
    void exemptAnonymousEchoWhenEnabled() {
        OwnEchoTracker tracker = new OwnEchoTracker(10_000L);
        tracker.record("这地图真垃圾");

        assertFalse(InboundMessageFilter.shouldBlock(
                "<玩家> 这地图真垃圾", null, SELF, true, KEYWORDS, true, tracker));
    }

    /** 关闭回显豁免后，匿名消息命中关键词即屏蔽 */
    @Test
    void blockAnonymousEchoWhenExemptionDisabled() {
        OwnEchoTracker tracker = new OwnEchoTracker(10_000L);
        tracker.record("这地图真垃圾");

        assertTrue(InboundMessageFilter.shouldBlock(
                "<玩家> 这地图真垃圾", null, SELF, true, KEYWORDS, false, tracker));
    }

    /** 有发送者时只按 UUID 判定，绝不查询回显追踪器（避免他人复述被放行） */
    @Test
    void neverConsultTrackerForNamedSender() {
        OwnEchoTracker tracker = new OwnEchoTracker(10_000L);
        tracker.record("这地图真垃圾");

        assertTrue(InboundMessageFilter.shouldBlock(
                "<玩家> 这地图真垃圾", OTHER, SELF, true, KEYWORDS, true, tracker));
    }

    /** 自己的消息按 UUID 豁免 */
    @Test
    void exemptSelfByUuid() {
        OwnEchoTracker tracker = new OwnEchoTracker(10_000L);

        assertFalse(InboundMessageFilter.shouldBlock(
                "这地图真垃圾", SELF, SELF, true, KEYWORDS, true, tracker));
    }

    /** 匿名消息命中关键词但没有回显记录 → 屏蔽 */
    @Test
    void blockAnonymousWhenNoEchoRecord() {
        OwnEchoTracker tracker = new OwnEchoTracker(10_000L);

        assertTrue(InboundMessageFilter.shouldBlock(
                "<玩家> 这地图真垃圾", null, SELF, true, KEYWORDS, true, tracker));
    }

    /** 匿名消息未命中关键词 → 放行 */
    @Test
    void allowAnonymousWhenMiss() {
        OwnEchoTracker tracker = new OwnEchoTracker(10_000L);

        assertFalse(InboundMessageFilter.shouldBlock(
                "<玩家> 今天天气不错", null, SELF, true, KEYWORDS, true, tracker));
    }

    /** 功能关闭时不屏蔽 */
    @Test
    void allowAllWhenDisabled() {
        OwnEchoTracker tracker = new OwnEchoTracker(10_000L);

        assertFalse(InboundMessageFilter.shouldBlock(
                "这地图真垃圾", OTHER, SELF, false, KEYWORDS, true, tracker));
    }

    /** 不在游戏中（本地玩家 UUID 缺失）时不屏蔽 */
    @Test
    void allowWhenSelfIdNull() {
        OwnEchoTracker tracker = new OwnEchoTracker(10_000L);

        assertFalse(InboundMessageFilter.shouldBlock(
                "这地图真垃圾", OTHER, null, true, KEYWORDS, true, tracker));
    }

    /** 追踪器缺失时匿名消息照常屏蔽，不抛异常 */
    @Test
    void blockAnonymousWhenTrackerNull() {
        assertTrue(InboundMessageFilter.shouldBlock(
                "这地图真垃圾", null, SELF, true, KEYWORDS, true, null));
    }
}
