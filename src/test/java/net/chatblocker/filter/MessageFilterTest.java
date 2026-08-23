package net.chatblocker.filter;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** MessageFilter 的单元测试：屏蔽别人发送的命中消息，自己发送的消息豁免。 */
class MessageFilterTest {

    /** 本地玩家 UUID 与其他玩家 UUID（随机生成，必然不同） */
    private static final UUID SELF = UUID.randomUUID();
    private static final UUID OTHER = UUID.randomUUID();

    /** 测试关键词列表 */
    private static final List<String> KEYWORDS = List.of("垃圾", "badword");

    /** 别人发送的消息命中关键词 → 屏蔽 */
    @Test
    void blockOthersWhenHit() {
        assertTrue(MessageFilter.shouldBlock("这地图真垃圾", OTHER, SELF, true, KEYWORDS));
    }

    /** 别人发送的消息未命中 → 放行 */
    @Test
    void allowOthersWhenMiss() {
        assertFalse(MessageFilter.shouldBlock("今天天气不错", OTHER, SELF, true, KEYWORDS));
    }

    /** 自己发送的消息即使命中关键词也放行（豁免） */
    @Test
    void exemptSelfWhenHit() {
        assertFalse(MessageFilter.shouldBlock("这地图真垃圾", SELF, SELF, true, KEYWORDS));
    }

    /** 大小写不敏感：别人发送的大写消息命中 */
    @Test
    void blockOthersCaseInsensitive() {
        assertTrue(MessageFilter.shouldBlock("THIS IS BADWORD", OTHER, SELF, true, KEYWORDS));
    }

    /** 功能关闭时不屏蔽 */
    @Test
    void allowAllWhenDisabled() {
        assertFalse(MessageFilter.shouldBlock("这地图真垃圾", OTHER, SELF, false, KEYWORDS));
    }

    /** 发送者信息缺失时不屏蔽（不误伤系统/插件消息） */
    @Test
    void allowWhenSenderIdNull() {
        assertFalse(MessageFilter.shouldBlock("这地图真垃圾", null, SELF, true, KEYWORDS));
    }

    /** 本地玩家信息缺失时不屏蔽 */
    @Test
    void allowWhenSelfIdNull() {
        assertFalse(MessageFilter.shouldBlock("这地图真垃圾", OTHER, null, true, KEYWORDS));
    }

    /** 空关键词列表不屏蔽 */
    @Test
    void allowWhenKeywordsEmpty() {
        assertFalse(MessageFilter.shouldBlock("这地图真垃圾", OTHER, SELF, true, List.of()));
    }
}
