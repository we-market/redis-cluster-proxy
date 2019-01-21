package cn.wemarket.redis.proxy.common.constant;

import java.io.Serializable;

public enum ResponseCodeEnum implements Serializable {

    REQUEST_SUCCESS(10000, "请求成功"),
    SERVER_ERROR(99999, "服务器内部错误");

    /**
     * 响应码
     */
    private Integer code;
    /**
     * 响应信息
     */
    private String msg;

    ResponseCodeEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
