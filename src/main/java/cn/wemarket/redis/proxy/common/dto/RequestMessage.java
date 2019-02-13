package cn.wemarket.redis.proxy.common.dto;

import cn.wemarket.redis.proxy.client.ClientProxy;
import cn.wemarket.redis.proxy.common.codec.*;
import cn.wemarket.redis.proxy.common.exception.MessageParseException;

public class RequestMessage {
    private RedisMessage message;
    private ClientProxy client;
    private boolean noReply = false;
    private boolean pipelined = false;
    private boolean pubSub = false;
    private long startTime = 0;
    private ResponseMessage response;

    public RedisMessage getMessage() {
        return message;
    }

    public void setMessage(RedisMessage message) {
        this.message = message;
    }

    public ClientProxy getClient() {
        return client;
    }

    public void setClient(ClientProxy client) {
        this.client = client;
    }

    public boolean isNoReply() {
        return noReply;
    }

    public void setNoReply(boolean noReply) {
        this.noReply = noReply;
    }

    public boolean isPipelined() {
        return pipelined;
    }

    public void setPipelined(boolean pipelined) {
        this.pipelined = pipelined;
    }

    public boolean isPubSub() {
        return pubSub;
    }

    public void setPubSub(boolean pubSub) {
        this.pubSub = pubSub;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public ResponseMessage getResponse() {
        return response;
    }

    public void setResponse(ResponseMessage response) {
        this.response = response;
        if (!isNoReply()){
            if (client.isInEventloop()){
                client.handleResponse();
            }else{
                client.postHandleResponseTask();
            }
        }
    }

    public int parseBytes(byte[] content, int start, int end) throws MessageParseException {
        message = RedisCodec.decode(content, start, end);
        if (message != null){
            if (message.getType() != RedisMessageType.INLINE && message.getType() != RedisMessageType.ARRAY){
                throw new MessageParseException("parse fail...");
            }
        }

        return message == null ? 0 : message.getReadBytes();
    }

    //argc, argv用于命令行编译程序
    //主函数在UNIX和Linux中的标准写法 ---- 【main(int argc, char *argv[], char *env[])】
    //argc ---- 整数，统计运行程序时传给main函数的命令行参数的个数
    public int argc(){
        if (message.getType().equals(RedisMessageType.INLINE)){
            return ((RedisInlineMessage)message).getContent().split(" ").length;
        }else {
            return ((RedisArrayMessage)message).getContent().length;
        }
    }

    //*argv ---- 字符串数组，存放指向字符串参数的指针数组，每一个元素指向一个参数
    //argv[0] ---- 指向程序运行的全路径名【可运行程序名】
    //argv[1] ---- 指向在DOS命令中执行程序名后的第一个参数
    //argv[2] ---- 指向执行程序名后的第二个参数
    //argv[..] ---- 【argv[argc]为NULL】
    public String argv(int idx){
        if (message.getType().equals(RedisMessageType.INLINE)){
            //inline命令
            return ((RedisInlineMessage)message).getContent().split(" ")[idx];
        }else {
            return new String(((RedisBulkStringMessage)((RedisArrayMessage)message).getContent()[idx]).getContent());
        }
    }
}
