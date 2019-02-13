package cn.wemarket.redis.proxy.client;

import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

public class ClientProxy {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientProxy.class);

    public ClientProxy(ChannelHandlerContext ctx, ServerClusterGroup serverClusterGroup){
        Assert.notNull(ctx, "ChannelHandlerContext is null");
        Assert.notNull();

    }
}
