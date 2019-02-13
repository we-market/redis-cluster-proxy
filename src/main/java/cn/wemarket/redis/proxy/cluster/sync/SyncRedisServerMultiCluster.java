package cn.wemarket.redis.proxy.cluster.sync;

import cn.wemarket.redis.proxy.cluster.BaseRedisMultiCluster;
import cn.wemarket.redis.proxy.common.exception.ClusterException;
import cn.wemarket.redis.proxy.common.route.RouteStrategyType;
import cn.wemarket.redis.proxy.common.route.Router;
import cn.wemarket.redis.proxy.common.route.impl.ConsistentHashStrategyRouter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class SyncRedisServerMultiCluster extends BaseRedisMultiCluster<SyncRedisServerSingleCluster> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SyncRedisServerMultiCluster.class);
    private static final String INFO_INTERNAL_SPLITTER = "|";
    private static final String ASSEMBLE_INFOS_SPLITTER =";";
    private static final int REPLICAS_COUNT = 1024;

    private String assembleInfo;
    private Router router;


    public SyncRedisServerMultiCluster(String clusterName, String assembleInfo, String password){
        this(clusterName, assembleInfo, password, RouteStrategyType.CONSISTENT_HASH);
    }

    public SyncRedisServerMultiCluster(String clusterName, String assembleInfo, String password, RouteStrategyType routeStrategyType){
        this.setClusterName(clusterName);
        this.setPassword(password);
        this.assembleInfo = assembleInfo;
        List<SyncRedisServerSingleCluster> clusters = initialClusters();
        if (CollectionUtils.isEmpty(clusters)){
            throw new ClusterException("No avaliable found...");
        }

        this.setClusters(clusters);

        switch (routeStrategyType){
            case MOD: break;
            case RANDOM: break;
            case CONSISTENT_HASH:
            default: {
                router = new ConsistentHashStrategyRouter(REPLICAS_COUNT, clusters);
                break;
            }
        }
    }

    @Override
    public SyncRedisServerSingleCluster getReadWriteCluster(String key){
        return (SyncRedisServerSingleCluster) router.route(key);
    }

    @Override
    public SyncRedisServerSingleCluster getSpareReadWriteCluster(String key){
        return null;
    }

    private List<SyncRedisServerSingleCluster> initialClusters(){
        List<SyncRedisServerSingleCluster> clusters = new ArrayList<SyncRedisServerSingleCluster>();
        //集群信息
        String[] singleInfoArr = StringUtils.split(assembleInfo, ASSEMBLE_INFOS_SPLITTER);
        for (String info:singleInfoArr) {
            if (StringUtils.indexOf(info, INFO_INTERNAL_SPLITTER) == -1){
                LOGGER.warn("Invalid single cluster information: {}", info);
                continue;
            }

            String[] propertyArr = StringUtils.split(info, INFO_INTERNAL_SPLITTER);
        }

        return null;
    }
}
