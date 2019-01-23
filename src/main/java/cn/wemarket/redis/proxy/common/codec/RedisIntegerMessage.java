package cn.wemarket.redis.proxy.common.codec;

public class RedisIntegerMessage extends RedisMessage {
    private long content;

    public RedisIntegerMessage(){
        setType(RedisMessageType.INTEGER);
    }

    public RedisIntegerMessage(long content) {
        this.content = content;
        setType(RedisMessageType.INTEGER);
    }

    public long getContent() {
        return content;
    }

    public void setContent(long content) {
        this.content = content;
    }

    public String toString() {
        return this.getType() + ":" + this.content;
    }
}
