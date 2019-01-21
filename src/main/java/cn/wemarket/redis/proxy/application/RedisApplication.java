package cn.wemarket.redis.proxy.application;

import cn.wemarket.redis.proxy.configuration.NettyAutoConfiguration;
import cn.wemarket.redis.proxy.server.ProxyServer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.Resource;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = "cn.wemarket", lazyInit = true)
public class RedisApplication implements CommandLineRunner {

    @Resource
    ProxyServer proxyServer;

    public static void main(String[] args) {

        SpringApplication.run(RedisApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        proxyServer.start();
    }
}

