

package com.peach.common.config;


import com.peach.common.constant.RedisModeConstant;
import com.peach.common.util.StringUtil;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SentinelServersConfig;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2024/10/10 16:18
 */
@Configuration
public class RedissonConfig {

    private static final Config CONFIG = new Config();

    @Value("${spring.redis.host:127.0.0.1:6379}")
    private String host;

    /**
     * 模式
     */
    @Value("${spring.redis.mode:standalone}")
    private String mode;

    /**
     * 密码
     */
    @Value("${spring.redis.password:123456}")
    private String password;

    /**
     * 数据库
     */
    @Value("${spring.redis.database:0}")
    private Integer database;


    @Bean
    public RedissonClient getRedisson() {
        switch (mode) {
            //集群模式
            case RedisModeConstant.CLUSTER:
                ClusterServersConfig clusterServersConfig = CONFIG.useClusterServers();
                //集群状态间隔扫描时间
                clusterServersConfig.setScanInterval(10000);
                if (StringUtil.isNotBlank(password)) {
                    clusterServersConfig.setPassword(password);
                }
                String[] clusterNodes = host.replaceAll("\\s*", "").split(",");
                for (String node : clusterNodes) {
                    clusterServersConfig.addNodeAddress("redis://" + node);
                }
                break;
            //哨兵模式
            case RedisModeConstant.SENTINEL:
                SentinelServersConfig sentinelServersConfig = CONFIG.useSentinelServers();
                sentinelServersConfig.setConnectTimeout(3000);
                if (StringUtil.isNotBlank(password)) {
                    sentinelServersConfig.setPassword(password);
                }
                String[] sentNodes = host.split(",");
                for (String node : sentNodes) {
                    sentinelServersConfig.addSentinelAddress("redis://" + node);
                }
                sentinelServersConfig.setDatabase(database);
                break;
            //默认单机
            default:
                SingleServerConfig singleServerConfig = CONFIG.useSingleServer().setAddress("redis://" + host);
                singleServerConfig.setDatabase(database);
                if (StringUtil.isNotBlank(password)) {
                    singleServerConfig.setPassword(password);
                }
        }
        CONFIG.setCodec(new JsonJacksonCodec());
        return Redisson.create(CONFIG);
    }

}
