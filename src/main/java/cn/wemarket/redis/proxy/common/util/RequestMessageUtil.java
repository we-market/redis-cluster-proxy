package cn.wemarket.redis.proxy.common.util;

import cn.wemarket.redis.proxy.common.codec.RedisArrayMessage;
import cn.wemarket.redis.proxy.common.codec.RedisBulkStringMessage;
import cn.wemarket.redis.proxy.common.codec.RedisMessage;
import cn.wemarket.redis.proxy.common.dto.RequestMessage;

import java.io.UnsupportedEncodingException;

public class RequestMessageUtil {
    public static RequestMessage ping(boolean isNoReply) throws UnsupportedEncodingException {
        RequestMessage pingRequest = new RequestMessage();
        pingRequest.setClient(null);

        //构造请求参数 --- 命令名 参数1 参数2 ... 参数n
        RedisBulkStringMessage pingStringMessage = new RedisBulkStringMessage();
        pingStringMessage.setContent("PING".getBytes("UTF-8"));
        //参数数组
        RedisArrayMessage redisArrayMessage = new RedisArrayMessage();
        RedisMessage[] array = new RedisMessage[]{pingStringMessage};
        redisArrayMessage.setContent(array);

        pingRequest.setMessage(redisArrayMessage);
        pingRequest.setNoReply(isNoReply);

        return pingRequest;

    }

    public static RequestMessage auth(String password, boolean isNoReply) throws UnsupportedEncodingException {
        RequestMessage authRequest = new RequestMessage();
        authRequest.setClient(null);
        //构造请求参数 --- 命令名 参数1 参数2 ... 参数n
        RedisBulkStringMessage authStringMessage1 = new RedisBulkStringMessage();
        authStringMessage1.setContent("AUTH".getBytes("UTF-8"));
        RedisBulkStringMessage authStringMessage2 = new RedisBulkStringMessage();
        authStringMessage2.setContent(password.getBytes("UTF-8"));
        //参数数组
        RedisArrayMessage redisArrayMessage = new RedisArrayMessage();
        RedisMessage[] array = new RedisMessage[]{authStringMessage1, authStringMessage2};
        redisArrayMessage.setContent(array);

        authRequest.setMessage(redisArrayMessage);
        authRequest.setNoReply(isNoReply);

        return authRequest;
    }
}
