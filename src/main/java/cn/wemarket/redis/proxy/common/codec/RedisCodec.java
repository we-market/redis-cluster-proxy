package cn.wemarket.redis.proxy.common.codec;

import cn.wemarket.redis.proxy.common.dto.RedisDecodeState;
import cn.wemarket.redis.proxy.common.exception.MessageParseException;
import org.springframework.util.Assert;

import java.util.Arrays;

/**
 * Redis解码器
 * 将服务器返回的数据根据协议反序列化成易于阅读的信息
 * 指令的编码格式【SET key value => *3\r\n$5\r\nSET\r\n$1\r\nkey\r\n$1\r\nvalue\r\n】
 *
 * 客户端和服务器通过 TCP 连接来进行数据交互
 * 客户端和服务器发送的命令或数据一律以 \r\n （CRLF）结尾
 * */
public class RedisCodec {
    public static <T extends RedisMessage> T decode(String content) throws MessageParseException {
        Assert.notNull(content, "content is null");
        return decode(content.getBytes(), 0, content.length());
    }

    //解析redis数据类型
    public static <T extends RedisMessage> T decode(byte[] content, int start, int end) throws MessageParseException {
        try {
            //TCP传输过程中产生的粘包问题
            RedisDecodeState redisDecodeState = new RedisDecodeState();
            redisDecodeState.setParseBuff(content);
            redisDecodeState.setStartPos(start);
            redisDecodeState.setParsePos(start);
            redisDecodeState.setEndPos(end);
            if (redisDecodeState.getParsePos() >= redisDecodeState.getEndPos()){
                redisDecodeState.setIncomplete(true);
                return null;
            }
            //判断是否为redis协议的五种基本数据类型
            char type = (char) redisDecodeState.getParseBuff()[redisDecodeState.getParsePos()];
            if (":".equals(type) || "+".equals(type) || "-".equals(type) || "$".equals(type) || "*".equals(type)){
                return decode(redisDecodeState);
            }else {
                //inline 命令
                String inLineContent = readLine(redisDecodeState);
                if (redisDecodeState.isIncomplete()){
                    return null;
                }
                RedisInlineMessage inlineMessage = new RedisInlineMessage(inLineContent);
                inlineMessage.setReadBytes(redisDecodeState.getParsePos()-redisDecodeState.getStartPos());
                return (T) inlineMessage;
            }
        }catch (Exception e){
            throw new MessageParseException("decode fail...");
        }
    }

    private static  <T extends RedisMessage> T decode(RedisDecodeState message) throws MessageParseException {
        if (message.getParsePos() >= message.getEndPos()){
            message.setIncomplete(true);
            return null;
        }

        char type = (char) message.getParseBuff()[message.getParsePos()];
        //解析位置从类型符号位后一位开始
        message.increaseParsePos(1);
        switch (type){
            case '-':{
                String content = readLine(message);
                if (message.isIncomplete()){
                    return null;
                }
                RedisErrorMessage errorMessage = new RedisErrorMessage(content);
                errorMessage.setReadBytes(message.getParsePos()-message.getStartPos());
                return (T) errorMessage;
            }
            case ':':{
                long num = readNum(message);
                if (message.isIncomplete()){
                    return null;
                }
                RedisIntegerMessage integerMessage = new RedisIntegerMessage(num);
                integerMessage.setReadBytes(message.getParsePos()-message.getStartPos());
                return (T) integerMessage;
            }
            case '+':{
                String content = readLine(message);
                if (message.isIncomplete()){
                    return null;
                }
                RedisSimpleStringMessage simpleStringMessage = new RedisSimpleStringMessage(content);
                simpleStringMessage.setReadBytes(message.getParsePos()-message.getStartPos());
                return (T) simpleStringMessage;
            }
            case '*':{
                int size = (int) readNum(message);
                if (message.isIncomplete()){
                    return null;
                }
                RedisArrayMessage arrayMessage = new RedisArrayMessage();
                if (size == -1){
                    arrayMessage.setNull(true);
                    arrayMessage.setReadBytes(message.getParsePos()-message.getStartPos());
                }else if (size == 0){
                    arrayMessage.setContent(null);
                    arrayMessage.setReadBytes(message.getParsePos()-message.getStartPos());
                }else {
                    RedisMessage[] array = new RedisMessage[size];
                    //递归解析字节数组数据内容
                    for(int i = 0; i < size; i++){
                        //readNum()方法之后message的解析位置发生变化（解析位置移至下一行起始位置）
                        RedisMessage element = decode(message);
                        if (element == null){
                            message.setIncomplete(true);
                            return null;
                        }
                        array[i] = element;
                    }
                    arrayMessage.setContent(array);
                    arrayMessage.setReadBytes(message.getParsePos()-message.getStartPos());
                }
                return (T) arrayMessage;
            }
            case '$':{
                int size = (int) readNum(message);
                if (message.isIncomplete()){
                    return null;
                }
                RedisBulkStringMessage bulkStringMessage = new RedisBulkStringMessage();
                if (size == -1){
                    bulkStringMessage.setNull(true);
                    bulkStringMessage.setReadBytes(message.getParsePos()-message.getStartPos());
                }else{
                    byte[] content = readBytes(message, size);
                    if (message.isIncomplete()){
                        return null;
                    }
                    bulkStringMessage.setContent(content);
                    bulkStringMessage.setReadBytes(message.getParsePos()-message.getStartPos());
                }
                return (T) bulkStringMessage;
            }
            default:{
                throw new MessageParseException("parse fail...");
            }
        }




    }

    //构建redis数据类型
    public static <T extends RedisMessage> String encode(T msg){
        StringBuilder sb = new StringBuilder();
        switch (msg.getType()){
            case INLINE:{
                sb.append(((RedisInlineMessage)msg).getContent());
                sb.append("\r\n");
                break;
            }
            case ERROR:{
                //error---exp【"-Error message\r\n"】
                sb.append("-");
                sb.append(((RedisErrorMessage)msg).getContent());
                sb.append("\r\n");
                break;
            }
            case INTEGER:{
                //integer---exp【":0\r\n"】
                sb.append(":");
                sb.append(((RedisIntegerMessage)msg).getContent());
                sb.append("\r\n");
                break;
            }
            case SIMPLE_STRING:{
                //simple_string---exp【"+OK\r\n"】
                sb.append("+");
                sb.append(((RedisSimpleStringMessage)msg).getContent());
                sb.append("\r\n");
                break;
            }
            case ARRAY:{
                sb.append("*");
                RedisArrayMessage array = (RedisArrayMessage)msg;
                if (array.isNull()){
                    sb.append("-1\r\n");
                }else {
                    RedisMessage[] content = array.getContent();
                    if (content != null && content.length > 0){
                        sb.append(content.length);
                        sb.append("\r\n");
                        Arrays.stream(content).forEach((RedisMessage m) -> {
                            //递归解析数组
                            sb.append(encode(m));
                        });
                    }else {
                        sb.append("0\r\n");
                    }
                }
                break;
            }
            case BULK_STRING:{
                sb.append("$");
                RedisBulkStringMessage bulkString = (RedisBulkStringMessage)msg;
                if (bulkString.isNull()){
                    sb.append("-1\r\n");
                }else{
                    byte[] content = bulkString.getContent();
                    if (content != null){
                        sb.append(content.length);
                        sb.append("\r\n");
                        sb.append(new String(content));
                    }else {
                        sb.append("0\r\n");
                    }
                    sb.append("\r\n");
                }
                break;
            }
        }
        return sb.toString();
    }

    private static String readLine(RedisDecodeState message) throws MessageParseException {
        int start = message.getParsePos();
        //循环按字节读取完整消息内容
        while (message.getParsePos() < message.getEndPos()){
            byte b = message.getParseBuff()[message.getParsePos()];
            //根据数据类型标志位的后一位字符，判断是否为不完全信息（TCP粘包）
            message.increaseParsePos(1);
            if(b == '\r'){
                if (message.getParsePos() >= message.getEndPos()){
                    message.setIncomplete(true);
                    return null;
                }

                byte c = message.getParseBuff()[message.getParsePos()];
                message.increaseParsePos(1);
                if(c == '\n'){
                    int parseEndPos = message.getEndPos();
                    int length = parseEndPos - start - 2;
                    byte[] content = new byte[length];
                    //拷贝字节数组
                    System.arraycopy(message.getParseBuff(), start, content, 0, length);
                    return new String(content);
                }else {
                    throw new MessageParseException("fail to parse string message...");
                }
            }
        }
        message.setIncomplete(true);
        return null;
    }

    private static long readNum(RedisDecodeState message) throws MessageParseException {
        long num = 0;
        boolean isMinus = false;
        if (message.getParsePos() >= message.getEndPos()){
            message.setIncomplete(true);
            return 0;
        }

        //判断是否是负数----【NULL----"$-1\r\n"】
        byte content = message.getParseBuff()[message.getParsePos()];
        message.increaseParsePos(1);
        if (content == '-'){
            if (message.getParsePos() >= message.getEndPos()){
                message.setIncomplete(true);
                return 0;
            }
            isMinus = true;
            //如果是负数则数字内容在负数符号位的后一位
            content = message.getParseBuff()[message.getParsePos()];
            message.increaseParsePos(1);
        }

        while (content != '\r'){
            //数字可能不是个位数，使用迭代计算数字
            num = num * 10 + content - '0';
            if (message.getParsePos() >= message.getEndPos()){
                message.setIncomplete(true);
                return 0;
            }
            content = message.getParseBuff()[message.getParsePos()];
            message.increaseParsePos(1);
        }

        if (message.getParsePos() >= message.getEndPos()){
            message.setIncomplete(true);
            return 0;
        }

        //判断结束符'\n'
        byte endByte = message.getParseBuff()[message.getParsePos()];
        message.increaseParsePos(1);
        if (endByte != '\n'){
            throw new MessageParseException("fail to parse number message...");
        }
        if (isMinus){
            num = -num;
        }
        return num;
    }

    private static byte[] readBytes(RedisDecodeState message, int length) throws MessageParseException {
        //字节串起始位置+字节内容长度+"\r\n"
        if ((message.getParsePos()+length+2) > message.getEndPos()){
            message.setIncomplete(true);
            return null;
        }

        byte[] content = new byte[length];
        System.arraycopy(message.getParseBuff(), message.getParsePos(), content, 0, length);
        message.setParsePos(length);

        //判断结束符
        if ((message.getParseBuff()[message.getParsePos()] != '\r') ||
                (message.getParseBuff()[message.getParsePos()+1] != '\n')){
            throw new MessageParseException("fail to parse byte message...");
        }
        message.increaseParsePos(2);
        return content;
    }
}
