package cn.wemarket.redis.proxy.common.codec;

public class RedisErrorMessage extends RedisMessage {
    private String content;

    public RedisErrorMessage() {
        setType(RedisMessageType.ERROR);
    }

    public RedisErrorMessage(String content) {
        setType(RedisMessageType.ERROR);
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
