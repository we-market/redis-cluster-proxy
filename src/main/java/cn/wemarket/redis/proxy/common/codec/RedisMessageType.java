package cn.wemarket.redis.proxy.common.codec;

import cn.wemarket.redis.proxy.common.exception.RedisCodecException;
/**
 * Redis 协议将传输的结构数据分为 5 种最小单元类型
 * 单元结束时统一加上回车换行符号\r\n，来表示该单元的结束
 * 1.单行字符串 以 + 符号开头。
 * 2.多行字符串 以 $ 符号开头，后跟字符串长度。
 * 3.整数值 以 : 符号开头，后跟整数的字符串形式。
 * 4.错误消息 以 - 符号开头。
 * 5.数组 以 * 号开头，后跟数组的长度。
 * */
public enum RedisMessageType {
    //inline命令格式
    INLINE((byte)0),
    // 以 + 开头的单行字符串
    SIMPLE_STRING((byte)43),
    // 以 - 开头的错误信息
    ERROR((byte)45),
    // 以 : 开头的整型数据
    INTEGER((byte)58),
    // 以 $ 开头的多行字符串
    BULK_STRING((byte)36),
    // 以 * 开头的数组
    ARRAY((byte)42);

    private final byte value;
    //private final boolean inline;

    private RedisMessageType(byte value) {
        this.value = value;
    }

    public byte value() {
        return this.value;
    }


    public static RedisMessageType valueOf(byte value) throws RedisCodecException {
        switch(value) {
            case 36:
                return BULK_STRING;
            case 42:
                return ARRAY;
            case 43:
                return SIMPLE_STRING;
            case 45:
                return ERROR;
            case 58:
                return INTEGER;
            default:
                throw new RedisCodecException("Unknown RedisMessageType: " + value);
        }
    }
}
