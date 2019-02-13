package cn.wemarket.redis.proxy.common.dto;

import cn.wemarket.redis.proxy.common.codec.RedisCodec;
import cn.wemarket.redis.proxy.common.codec.RedisMessage;
import cn.wemarket.redis.proxy.common.exception.MessageParseException;

public class ResponseMessage {
    private RedisMessage message;

    public ResponseMessage() {
    }

    public ResponseMessage(RedisMessage message) {
        this.message = message;
    }

    public int parseBytes(byte[] content, int start, int end) throws MessageParseException{
        message = RedisCodec.decode(content, start, end);
        return message == null ? 0 : message.getReadBytes();
    }

    public RedisMessage getMessage() {
        return message;
    }

    public void setMessage(RedisMessage message) {
        this.message = message;
    }
}
