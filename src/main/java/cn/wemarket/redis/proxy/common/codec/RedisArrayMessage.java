package cn.wemarket.redis.proxy.common.codec;
/**
 * * ---------------Arrays----------------
 * Arrays 的编码格式如下：
 * · “*” 为第一个字节，后跟数组的元素个数，
 * · CRLF
 * · 数组中的每一个RESP类型表示的元素
 * · exp
 *      【空数组----"*0\r\n"】
 *      Bulk Strings数组["foo", "bar"]的编码为
 *      【"*2\r\n$3\r\nfoo\r\n$3\r\nbar\r\n"】
 *      整数数组[1,2,3]编码为
 *      【"*3\r\n:1\r\n:2\r\n:3\r\n"】
 *      有4个interges和1个bulk string的数组编码为
 *      【
 *         *5\r\n
 *         :1\r\n
 *         :2\r\n
 *         :3\r\n
 *         :4\r\n
 *         $6\r\n
 *         foobar\r\n
 *       】
 */
public class RedisArrayMessage extends RedisMessage {
    private boolean isNull = false;
    private RedisMessage[] content;

    public RedisArrayMessage() {
        setType(RedisMessageType.ARRAY);
    }

    public RedisMessage[] getContent() {
        return content;
    }

    public void setContent(RedisMessage[] content) {
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
        StringBuilder sb = new StringBuilder();
        if (content != null) {
            for (int i = 0; i < content.length; i++) {
                sb.append(content[i].toString());
                if (i != (content.length - 1)) {
                    sb.append(",");
                }
            }

        }

        return "RedisArrayMessage{" +
                "isNull=" + isNull +
                ", content=[" + sb.toString() +
                "]}";
    }
}
