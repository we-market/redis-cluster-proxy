package cn.wemarket.redis.proxy.common.exception;

public class ClusterException extends RuntimeException{
    public ClusterException(String message) {
        super(message);
    }
}
