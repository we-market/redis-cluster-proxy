package cn.wemarket.redis.proxy.common.codec;

public class RedisMessage {
    private RedisMessageType type;
    private int readBytes;

    public RedisMessageType getType() {
        return type;
    }

    public void setType(RedisMessageType type) {
        this.type = type;
    }

    public int getReadBytes() {
        return readBytes;
    }

    public void setReadBytes(int readBytes) {
        if (readBytes < 0){
            this.readBytes = 0;
        }else {
            this.readBytes = readBytes;
        }
    }
}
