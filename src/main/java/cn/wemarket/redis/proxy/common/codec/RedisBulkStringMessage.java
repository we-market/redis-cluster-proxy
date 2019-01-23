package cn.wemarket.redis.proxy.common.codec;


/**
 * * -------------Bulk Strings-------------
 * 用于表示一个二进制安全的字符串，最大长度为512M。
 * Bulk Strings 的编码格式如下：
 * · “$” 后跟字符串字节数（prefix length），以CRLF结束
 * · 实际的字符串
 * · CRLF结束
 * · exp
 *      【foobar----"$6\r\nfoobar\r\n"】
 *      【空字符串----"$0\r\n\r\n"】
 *       NULL，这种特殊格式的长度值为-1， 并且没数据
 *      【NULL----"$-1\r\n"】
 */
public class RedisBulkStringMessage extends RedisMessage {
    boolean isNull = false;
    private byte[] content;

    public RedisBulkStringMessage() {
        setType(RedisMessageType.BULK_STRING);
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public boolean isNull() {
        return isNull;
    }

    public void setNull(boolean aNull) {
        isNull = aNull;
    }

    @Override
    public String toString() {
        return "RedisBulkStringMessage{" +
                "isNull=" + isNull + (content == null ? "" : (", content=" + new String(content))) +
                '}';
    }

}
