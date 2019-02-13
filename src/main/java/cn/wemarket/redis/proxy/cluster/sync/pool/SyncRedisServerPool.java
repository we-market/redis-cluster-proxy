package cn.wemarket.redis.proxy.cluster.sync.pool;

import cn.wemarket.redis.proxy.cluster.sync.SyncRedisServerNode;
import cn.wemarket.redis.proxy.common.exception.SysException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Protocol;
import redis.clients.util.Pool;

public class SyncRedisServerPool extends Pool<SyncRedisServerNode> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SyncRedisServerPool.class);
    private static final int DEFAULT_INITIAL_IDLE_OBJECTS_COUNT = 32;
    private static final int DEFAULT_GET_RESOURCES_RETRY_TIMES = 10;

    //赋初始值
    private int initialIdleObjectsCount = DEFAULT_INITIAL_IDLE_OBJECTS_COUNT;
    private int connectionTimeout = Protocol.DEFAULT_TIMEOUT;
    private int soTimeout = Protocol.DEFAULT_TIMEOUT;
    private GenericObjectPoolConfig poolConfig;
    private SyncRedisServerFactory factory;
    private String password;
    private String host;
    private int port;

    public SyncRedisServerPool(String host, int port, String password, GenericObjectPoolConfig poolConfig){
        this(DEFAULT_INITIAL_IDLE_OBJECTS_COUNT, Protocol.DEFAULT_TIMEOUT, Protocol.DEFAULT_TIMEOUT, poolConfig, password, host, port);
    }

    public SyncRedisServerPool(int initialIdleObjectsCount, int connectionTimeout, int soTimeout, GenericObjectPoolConfig poolConfig, String password, String host, int port) {
        this.initialIdleObjectsCount = initialIdleObjectsCount;
        this.connectionTimeout = connectionTimeout;
        this.soTimeout = soTimeout;
        this.poolConfig = poolConfig;
        this.password = password;
        this.host = host;
        this.port = port;
        initPool();
    }

    public void initPool(){
        if (factory == null){
            factory = new SyncRedisServerFactory(host, port, password, connectionTimeout, soTimeout);
            initPool(poolConfig, factory);

            LOGGER.info("Ready to pre-add idle objects when firstly initialize pool");
            for (int i = 0; i < initialIdleObjectsCount; i++) {
                try {
                    internalPool.addObject();
                } catch (Exception ignore) {
                    LOGGER.info("ignore pre-add idle objects fail... continue");
                    continue;
                }
            }
        }
    }

    @Override
    public SyncRedisServerNode getResource(){
        for (int retry = 0; retry < DEFAULT_GET_RESOURCES_RETRY_TIMES; retry++){
            try {
                SyncRedisServerNode server = super.getResource();
                if (server != null){
                    server.setCustomizedDataSource(this);
                    if (server.isConnected()){
                        LOGGER.info("Connection [ {}:{} ] current idle count is {}, current active count is {}", host, port, internalPool.getNumIdle(), internalPool.getNumActive());
                    }
                    return server;
                }else {
                    returnBrokenResource(server);
                }
            }catch (Exception e){
                LOGGER.warn("fail to get resource of object {}:{}, error:{}", host, port, e);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {
                }
            }
        }
        //循环结束，则说明获取到的SyncRedisServerNode一直为空
        throw new SysException("Can NOT retrieve avaliable resource of project "+host+":"+port+" from pool");
    }


    @Override
    @Deprecated
    public void returnBrokenResource(final SyncRedisServerNode resource){
        if (resource != null){
            returnBrokenResourceObject(resource);
        }
    }

    @Override
    @Deprecated
    public void returnResource(final SyncRedisServerNode resource){
        if (resource != null){
            returnBrokenResourceObject(resource);
        }
    }

    private static class SyncRedisServerFactory implements PooledObjectFactory<SyncRedisServerNode>{
        private String host;
        private int port;
        private String password;
        private int connectionTimeout = Protocol.DEFAULT_TIMEOUT;
        private int soTimeout = Protocol.DEFAULT_TIMEOUT;

        public SyncRedisServerFactory(String host, int port, String password, int connectionTimeout, int soTimeout) {
            this.host = host;
            this.port = port;
            this.password = password;
            this.connectionTimeout = connectionTimeout;
            this.soTimeout = soTimeout;
        }

        @Override
        public PooledObject<SyncRedisServerNode> makeObject() throws Exception {
            SyncRedisServerNode server = new SyncRedisServerNode(host, port);
            server.connect();
            if (StringUtils.isBlank(password)){
                LOGGER.warn("The password is null..");
                throw new Exception("The password is null...");
            }else {
                server.auth(password);
            }
            return new DefaultPooledObject<SyncRedisServerNode>(server);
        }

        @Override
        public void destroyObject(PooledObject<SyncRedisServerNode> p) throws Exception {
            final SyncRedisServerNode server = p.getObject();
            if (server.isConnected()){
                server.disconnect();
            }
        }

        @Override
        public boolean validateObject(PooledObject<SyncRedisServerNode> p) {
            final SyncRedisServerNode server = p.getObject();
            return server.isConnected();
        }

        @Override
        public void activateObject(PooledObject<SyncRedisServerNode> p) throws Exception {

        }

        @Override
        public void passivateObject(PooledObject<SyncRedisServerNode> p) throws Exception {

        }
    }
}
