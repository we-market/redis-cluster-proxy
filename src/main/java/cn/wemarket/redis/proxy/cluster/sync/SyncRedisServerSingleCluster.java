package cn.wemarket.redis.proxy.cluster.sync;

import cn.wemarket.redis.proxy.cluster.BaseRedisSingleCluster;
import cn.wemarket.redis.proxy.cluster.sync.pool.SyncRedisServerPool;
import io.netty.channel.EventLoopGroup;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SyncRedisServerSingleCluster extends BaseRedisSingleCluster {
    private static final Logger LOGGER = LoggerFactory.getLogger(SyncRedisServerSingleCluster.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int DEFAULT_MAX_TOTAL = 500;
    private static final int DEFAULT_MAX_IDLE = 500;
    private static final int DEFAULT_MIN_IDLE = 100;

    private String password;
    private int poolMaxTotal = DEFAULT_MAX_TOTAL;
    private int poolMaxIdle = DEFAULT_MAX_IDLE;
    private int poolMinIdle = DEFAULT_MIN_IDLE;

    //Address Mapping
    private volatile String[] slots = new String[16384];
    private Map<String, SyncRedisServerPool> clusterServerMap = new ConcurrentHashMap<>();

    private EventLoopGroup pubSubWorkerGroup;

    public void setPoolMaxTotal(int poolMaxTotal) {
        this.poolMaxTotal = poolMaxTotal;
    }

    public void setPoolMaxIdle(int poolMaxIdle) {
        this.poolMaxIdle = poolMaxIdle;
    }

    public void setPoolMinIdle(int poolMinIdle) { this.poolMinIdle = poolMinIdle; }

    public EventLoopGroup getPubSubWorkerGroup() {
        return pubSubWorkerGroup;
    }

    public SyncRedisServerSingleCluster(String password, String serverList) {
        this.password = password;
        if (StringUtils.isEmpty(serverList)){
            LOGGER.warn("Can NOT initialize sync redis server cluster with EMPTY server list...");
        }else {
            LOGGER.info("========== Sync redis server cluster with server list {} ==========", serverList);
        }

        String[] servers = serverList.split(",");
        for (String server : servers) {
            addServer(server);
        }
    }

    public synchronized SyncRedisServerNode addServer(String address){
        return getClusterServer(address);
    }


    public SyncRedisServerNode getServerByKey(String key){
        if (clusterServerMap.isEmpty()){
            return null;
        }

        int slotIdx = getKeySlot(key);
        SyncRedisServerNode server = getClusterServer(slots[slotIdx]);
        if (server != null){
            LOGGER.info("server {}:{} connected : {}", server.getHost(), server.getPort(), server.isConnected());
        }

        if (server == null || !server.isConnected()){
            //指定位置没有redis服务节点，或redis节点未连接，随机选择一个节点
            server = getRandomServer();
        }
        return server;
    }

    public SyncRedisServerNode getRandomServer(){
        int r = 0;
        if (clusterServerMap.size() > 1){
            r = RANDOM.nextInt(clusterServerMap.size());
        }

        List<String> addressList = new ArrayList<>(clusterServerMap.keySet());
        return clusterServerMap.get(addressList.get(r)).getResource();
    }

    private SyncRedisServerNode getClusterServer(String address){
        if (StringUtils.isBlank(address)){
            LOGGER.info("address is null...");
            return null;
        }

        if (clusterServerMap.containsKey(address)){
            return clusterServerMap.get(address).getResource();
        }else {
            String[] hostAndPort = address.split(":");
            String host = hostAndPort[0];
            int port = 6379;
            if (hostAndPort.length >=2){
                port = Integer.parseInt(hostAndPort[1]);
            }

            //redis节点池初始化
            SyncRedisServerPool syncRedisServerPool = initServerPool(host, port, password, poolMaxTotal, poolMaxIdle, poolMinIdle, 2000, false);
            clusterServerMap.put(address, syncRedisServerPool);
            SyncRedisServerNode clusterServer = syncRedisServerPool.getResource();
            if (clusterServer != null){
                if (!clusterServer.isConnected()){
                    LOGGER.info("Disconnected cluster server {}:{} may recover, retry to connect");
                }
                clusterServer.connect();
            }
            return clusterServer;
        }
    }

    private SyncRedisServerPool initServerPool(String host, int port, String password, int poolMaxTotal, int poolMaxIdle, int poolMinIdle, long maxWaitMillis, boolean testOnBorrow){
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxTotal(poolMaxTotal);
        poolConfig.setMaxIdle(poolMaxIdle);
        poolConfig.setMinIdle(poolMinIdle);
        poolConfig.setMaxWaitMillis(maxWaitMillis);
        poolConfig.setTestOnBorrow(testOnBorrow);
        return new SyncRedisServerPool(host, port, password, poolConfig);
    }

    private static int getKeySlot(String key){
        if (key.indexOf("{") != -1 && key.indexOf("}") != -1){
            String among = key.substring(key.indexOf("{")+1, key.indexOf("}"));
            if (!among.isEmpty()){
                key = among;
            }
        }

        return CRC_XModem(key.getBytes()) % 16384;
    }

    //循环冗余校验（CRC）
    private static int CRC_XModem(byte[] bytes){
        //初始化
        int crc = 0x00;
        int polynomial = 0x1021;
        for (int index = 0; index < bytes.length; index++){
            byte b = bytes[index];
            //1个字节等于8个比特
            for (int i =0; i < 8; i++){
                boolean bit = ((b >> (7-i) & 1) == 1);
                boolean c15 = ((crc >> 15 & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit){
                    crc ^= polynomial;
                }
            }
        }

        crc &= 0xffff;
        return crc;
    }
}
