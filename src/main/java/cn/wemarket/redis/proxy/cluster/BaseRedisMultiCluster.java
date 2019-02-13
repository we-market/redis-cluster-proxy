package cn.wemarket.redis.proxy.cluster;

import org.apache.commons.lang3.RandomUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseRedisMultiCluster<E extends BaseRedisSingleCluster> {
    private String clusterName;
    private String password;
    private List<E> clusters;

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<E> getClusters() {
        return clusters;
    }

    public void setClusters(List<E> clusters) {
        this.clusters = clusters;
    }

    abstract public E getReadWriteCluster(String key);

    abstract public E getSpareReadWriteCluster(String key);

    public E getRandomReadWriteCluster(){
        if (CollectionUtils.isEmpty(clusters)){
            return null;
        }
        int index = RandomUtils.nextInt(0, clusters.size());
        return clusters.get(index);
    }
}
