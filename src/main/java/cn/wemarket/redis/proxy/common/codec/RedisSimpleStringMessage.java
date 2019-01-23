package cn.wemarket.redis.proxy.common.codec;

public class RedisSimpleStringMessage extends RedisMessage {
    private String content;

    public RedisSimpleStringMessage() {
        setType(RedisMessageType.SIMPLE_STRING);
    }

    public RedisSimpleStringMessage(String content) {
        setType(RedisMessageType.SIMPLE_STRING);
        this.content = content;
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
