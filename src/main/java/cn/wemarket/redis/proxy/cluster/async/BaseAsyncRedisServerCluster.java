package cn.wemarket.redis.proxy.cluster.async;

import cn.wemarket.redis.proxy.client.ClientProxy;
import io.netty.channel.EventLoopGroup;
import org.apache.http.impl.client.ProxyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class BaseAsyncRedisServerCluster<E extends BaseAsyncRedisServerNode> {
    private final static Logger LOGGER = LoggerFactory.getLogger(BaseAsyncRedisServerCluster.class);
    //SecureRandom类提供加密的强随机数生成器 (RNG)
    private final static SecureRandom random = new SecureRandom();
    private final static AtomicInteger counter = new AtomicInteger(0);

    private EventLoopGroup workerGroup;
    private List<E> avaliableServers = new ArrayList<E>();
    private List<E> unavaliableServers = new ArrayList<E>();
    private Map<String, E> clusterServerMap = new HashMap<String, E>();
    private Class<E> serverClass;

    protected BaseAsyncRedisServerCluster(){
        //获得集合类型对象的元素的类型
        serverClass = (Class<E>) ((ParameterizedType)getClass()
                .getGenericSuperclass()).getActualTypeArguments()[0];
    }

    public BaseAsyncRedisServerCluster(EventLoopGroup workerGroup){
        this();
        this.workerGroup = workerGroup;
    }

    public static SecureRandom getRandom() {
        return random;
    }

    public static AtomicInteger getCounter() {
        return counter;
    }

    public EventLoopGroup getWorkerGroup() {
        return workerGroup;
    }

    public void setWorkerGroup(EventLoopGroup workerGroup) {
        this.workerGroup = workerGroup;
    }

    public List<E> getAvaliableServers() {
        return avaliableServers;
    }

    public void setAvaliableServers(List<E> avaliableServers) {
        this.avaliableServers = avaliableServers;
    }

    public List<E> getUnavaliableServers() {
        return unavaliableServers;
    }

    public void setUnavaliableServers(List<E> unavaliableServers) {
        this.unavaliableServers = unavaliableServers;
    }

    public Map<String, E> getClusterServerMap() {
        return clusterServerMap;
    }

    public void setClusterServerMap(Map<String, E> clusterServerMap) {
        this.clusterServerMap = clusterServerMap;
    }

    public E getServer(String address, ClientProxy client){
        String[] ipAndPort = address.split(":");
        String ip = ipAndPort[0];
        int port = 6379;
        if (ipAndPort.length >= 2){
            port = Integer.parseInt(ipAndPort[1]);
        }

        return getServer(ip, port, client);
    }

    public E getServer(String ip, int port, ClientProxy client){
        E server = null;
        try {
            Constructor<E> constructor = serverClass.getConstructor(EventLoopGroup.class, String.class, int.class, ProxyClient.class);
            server = constructor.newInstance(getWorkerGroup(), ip, port, client);
        } catch (Exception e) {
            LOGGER.error("fail to construct a server...");
            return null;
        }

        E existServer = getRedisServer(server);
    }

    public E getRedisServer(E server){
        String address = getServerAddress(server);
        return  clusterServerMap.get(address);
    }

    public void addAvailableServer(E server){
        if (avaliableServers.contains(server)){
            return;
        }
        avaliableServers.add(server);
    }

    public void removeAvailableServer(E server){
        avaliableServers.remove(server);
    }

    public E addClusterServer(E server){
        String address = getServerAddress(server);
    }

    public String getServerAddress(E server){
        return server.get
    }
}
