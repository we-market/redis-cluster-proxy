package cn.wemarket.redis.proxy.cluster;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class BaseRedisSingleCluster {
    protected AtomicBoolean avaliable = new AtomicBoolean(true);
    protected AtomicInteger failureCount = new AtomicInteger(0);
}
