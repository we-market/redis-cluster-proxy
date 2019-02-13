package cn.wemarket.redis.proxy.common.exception;

import org.springframework.core.NestedRuntimeException;

public class SysException extends NestedRuntimeException {
    public SysException(String message) {
        super(message);
    }

    public SysException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
