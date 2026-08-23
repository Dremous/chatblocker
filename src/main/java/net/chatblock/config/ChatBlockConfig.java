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

    /** 私有构造器：仅允许通过单例 INSTANCE 或静态工厂 fromDisk 获取实例 */
    private ChatBlockConfig() {
    }

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
