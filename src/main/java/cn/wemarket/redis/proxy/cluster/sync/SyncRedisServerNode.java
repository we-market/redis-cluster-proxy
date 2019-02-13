package cn.wemarket.redis.proxy.cluster.sync;

import cn.wemarket.redis.proxy.common.codec.RedisErrorMessage;
import cn.wemarket.redis.proxy.common.codec.RedisMessageType;
import cn.wemarket.redis.proxy.common.dto.RequestMessage;
import cn.wemarket.redis.proxy.common.dto.ResponseMessage;
import cn.wemarket.redis.proxy.common.util.RequestMessageUtil;
import cn.wemarket.redis.proxy.common.util.ResponseMessageUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Connection;
import redis.clients.jedis.Protocol;
import redis.clients.util.Pool;

/**
 * Redis Server 节点
 * ①socket连接
 * ②设置集群
 * ③发送命令
 * ④解析返回信息
 * */
public class SyncRedisServerNode extends Connection {
    private static final Logger LOGGER = LoggerFactory.getLogger(SyncRedisServerNode.class);
    private static final String REDIS_AUTH_NO_PASSWORD_SET_ERROR_MESSAGE = "ERR Client AUTH, but no password is set";
    private static final int READ_BUFFER_SIZE = 1024;
    private static final int MAX_READ_BUFFER_SIZE = 100 * 1024;

    private SyncRedisServerSingleCluster cluster;
    private Pool<SyncRedisServerNode> customizedDataSource;
    private boolean isCommunicationException = false;
    private String host;
    private int port;

    public SyncRedisServerNode(String host, int port){
        this(host, port, Protocol.DEFAULT_TIMEOUT, Protocol.DEFAULT_TIMEOUT);
    }

    public SyncRedisServerNode(String host, int port, int connectionTimeout, int soTimeout){
        super(host, port);
        super.setConnectionTimeout(connectionTimeout);
        super.setSoTimeout(soTimeout);
        this.host = host;
        this.port = port;
    }

    public SyncRedisServerSingleCluster getCluster() {
        return cluster;
    }

    public void setCluster(SyncRedisServerSingleCluster cluster) {
        this.cluster = cluster;
    }

    public Pool<SyncRedisServerNode> getCustomizedDataSource() {
        return customizedDataSource;
    }

    public void setCustomizedDataSource(Pool<SyncRedisServerNode> customizedDataSource) {
        this.customizedDataSource = customizedDataSource;
    }

    public boolean isConnected(){
        if (getSocket() != null){
            LOGGER.info("socket connection... isBound"+getSocket().isBound()
                +"isClosed"+getSocket().isClosed()
                +"isConnected"+getSocket().isConnected()
                +"isInputShutDown"+getSocket().isInputShutdown()
                +"isOutputShutDown"+getSocket().isOutputShutdown());
        }else{
            LOGGER.info("fail to get socket connection... socket is null");
        }
        return (getSocket()!=null && getSocket().isBound() && getSocket().isClosed()
                    && getSocket().isConnected() && getSocket().isInputShutdown() && getSocket().isOutputShutdown());
    }

    public void disconnect(){
        LOGGER.info("Disconnect from ("+host+":"+port+")");
        super.disconnect();
    }

    public void flush(){
        super.flush();
    }

    public void close(){
        if (customizedDataSource != null){
            if (isBroken() || isCommunicationException){
                customizedDataSource.returnBrokenResource(this);
            }else {
                customizedDataSource.returnResource(this);
            }
        }else {
            disconnect();
        }
    }

    public ResponseMessage handleResponse(){
        try {
            return handleResponse(true);
        } catch (Exception e) {
            LOGGER.error("fail to parse response message... ", e);
            return null;
        }
    }

    public ResponseMessage handleResponse(boolean autoClose) throws Exception {
        int length = 0;
        int available = 0;
        int cursor = 0;
        byte[] total = new byte[MAX_READ_BUFFER_SIZE];
        byte[] buffer = new byte[READ_BUFFER_SIZE];

        ResponseMessage responseMessage = new ResponseMessage();
        int rc = 0;
        //(MAX_READ_BUFFER_SIZE / READ_BUFFER_SIZE) * 2
        int parseCount = 200;
        try {
            while (parseCount > 0){
                do{
                    length = getSocket().getInputStream().read(buffer);
                    available = getSocket().getInputStream().available();
                    LOGGER.info("Read length: {}, Available: {}", length, available);
                    if (length < 0){
                        break;
                    }

                    if ((cursor + length) > MAX_READ_BUFFER_SIZE){
                        throw new Exception("response length is larger than the max");
                    }

                    System.arraycopy(buffer, 0, total, cursor, length);
                    cursor += length;
                }while (available > 0);

                LOGGER.info("Total written: {}", cursor);

                //parse response message
                rc = responseMessage.parseBytes(total, 0, cursor);
                if (rc == 0){
                    LOGGER.warn("parse incomplete, {} avaliable", getSocket().getInputStream().available());
                }else if (rc > 0){
                    LOGGER.info("handle response: {}", responseMessage);
                    return responseMessage;
                }
                parseCount--;
            }
            //循环可以运行至结束，说明responseMessage一直没有被解析完全
            throw new Exception("parse incomplete, parseCount exceed....");
        }catch (Exception e){
            LOGGER.error("handle response fail...", e);
            isCommunicationException = true;

            try {
                //非自动关闭连接，主动关闭
                if (!autoClose){
                    if (isConnected()){
                        disconnect();
                    }
                }
            }catch (Exception ignored){
                LOGGER.warn("Disconnect form {}:{}, error:{}", host, port, ignored);
            }
            return ResponseMessageUtil.error("handle response failed with 【"+e.getMessage()+"】");
        }finally {
            //自动断开连接
            if (autoClose){
                disconnect();
            }
        }
    }

    public void sendCommand(RequestMessage requestMessage){
        //argv[0]指向命令名称（程序名称）
        final Protocol.Command command = Protocol.Command.valueOf(requestMessage.argv(0).toUpperCase());
        //运行程序，命令行参数大于1
        if (requestMessage.argc() > 1){
            //argv[argc]为NULL，参数指针数组长度 = 参数个数 - 1
            String[] args = new String[requestMessage.argc() - 1];
            //i=1 ---- 从第一个命令行参数开始赋值
            for (int i = 1; i < requestMessage.argc(); i++) {
                args[i - 1] = requestMessage.argv(i);
            }
            super.sendCommand(command, args);
        }else {
            super.sendCommand(command);
        }
        super.resetPipelinedCount();
    }

    public void auth(String password) throws Exception {
        RequestMessage authRequest = RequestMessageUtil.auth(password, true);
        sendCommand(authRequest);
        flush();
        ResponseMessage authResponse = handleResponse(false);
        if (RedisMessageType.ERROR.equals(authResponse.getMessage().getType())){
            RedisErrorMessage errorMessage = (RedisErrorMessage) authResponse.getMessage();
            String errorContent = errorMessage.getContent();
            if (StringUtils.equals(REDIS_AUTH_NO_PASSWORD_SET_ERROR_MESSAGE, errorContent)){
                LOGGER.warn("Redis server {}:{} no need for authentication");
            }else {
                throw new Exception("Redis server ["+host+":"+port+"] authentication failed with error: "+errorContent);
            }

        }
    }

    public ResponseMessage ping() throws Exception {
        RequestMessage pingRequest = RequestMessageUtil.ping(true);
        sendCommand(pingRequest);
        flush();
        return handleResponse(false);
    }
}
