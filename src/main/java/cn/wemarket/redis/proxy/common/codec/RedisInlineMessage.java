package cn.wemarket.redis.proxy.common.codec;

public class RedisInlineMessage extends RedisMessage {
    private String content;

    RedisInlineMessage() {
        setType(RedisMessageType.INLINE);
    }

    public RedisInlineMessage(String content) {
        this.content = content;
        setType(RedisMessageType.INLINE);
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String toString() {
        return this.getType() + ":" + this.content;
    }
}
