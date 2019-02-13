package cn.wemarket.redis.proxy.common.util;

import cn.wemarket.redis.proxy.common.codec.RedisErrorMessage;
import cn.wemarket.redis.proxy.common.constant.ResponseCodeEnum;
import cn.wemarket.redis.proxy.common.dto.ResponseMessage;

public class ResponseMessageUtil {
    /**
     * 请求失败返回的数据结构
     * @param errorMessage 错误信息
     * @return 结果集
     */
    public static ResponseMessage error(String errorMessage) {
        RedisErrorMessage redisErrorMessage = new RedisErrorMessage(errorMessage);
        return new ResponseMessage(redisErrorMessage);
    }
}
