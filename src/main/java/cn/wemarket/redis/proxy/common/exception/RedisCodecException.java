package cn.wemarket.redis.proxy.common.exception;

public class RedisCodecException extends Exception{
    public RedisCodecException(String errMsg){
        super(errMsg);
    }
}
