package cn.wemarket.redis.proxy.cluster.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;

public class BaseAsyncRedisServerNode <E extends BaseAsyncRedisServerCluster>{
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseAsyncRedisServerNode.class);
    private static final int HANDLE_MAX_TASK_ONCE = 100;
    private static final int MAX_ADD_REQUEST_RETRY_TIMES = 3;

    private Queue<RequestMessage>
}
