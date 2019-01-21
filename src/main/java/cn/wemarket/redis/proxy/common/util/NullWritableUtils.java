package cn.wemarket.redis.proxy.common.util;

import java.io.Serializable;

/**
 * 服务器可能返回空的处理
 */
public class NullWritableUtils implements Serializable {

    private static final long serialVersionUID = -8191640400484155111L;
    private static NullWritableUtils instance = new NullWritableUtils();

    private NullWritableUtils() {
    }

    public static NullWritableUtils nullWritable() {
        return instance;
    }
}
