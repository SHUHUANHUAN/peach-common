package com.peach.common.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peach.common.constant.RedisModeConstant;
import com.peach.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2024/10/10 16:18
 */
@Slf4j
@Configuration
public class RedisConfig {

    /**
     * 主机地址
     */
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

    /**
     * 主节点名称
     */
    @Value("${spring.redis.sentinelMaster:master}")
    private String sentinelMaster;

    /**
     * 最大连接数
     */
    @Value("${spring.redis.pool.maxTotal:600}")
    private Integer maxTotal;

    /**
     * 最小空闲连接数
     */
    @Value("${spring.redis.pool.minIdle:1}")
    private Integer minIdle;

    /**
     * 最大空闲连接数
     */
    @Value("${spring.redis.pool.maxIdle:10}")
    private Integer maxIdle;

    /**
     * 最大等待时间
     */
    @Value("${spring.redis.pool.maxWait:60}")
    private Long maxWait;

    /**
     * 开启空闲连接的校验，确保空闲的连接是否可用。
     */
    @Value("${spring.redis.pool.testWhileIdle:true}")
    private Boolean testWhileIdle;

    /**
     * 开启连接借用前的校验，确保从连接池借出的连接是有效的。
     */
    @Value("${spring.redis.pool.testOnBorrow:true}")
    private Boolean testOnBorrow;

    /**
     * 连接空闲时的最小生存时间，超过该时间将会被清理掉
     */
    @Value("${spring.redis.pool.minEvictableIdleDuration:60}")
    private Long minEvictableIdleDuration;

    /**
     * 周期清理时间间隔
     */
    @Value("${spring.redis.pool.timeBetweenEvictionRuns:60}")
    private Long timeBetweenEvictionRuns;

    /**
     * 周期清理时清理的连接数大小
     */
    @Value("${spring.redis.pool.numTestsPerEvictionRun:10}")
    private Integer  numTestsPerEvictionRun;

    /**
     * 连接池参数配置
     * @return
     */
    @Bean(name = "poolConfig")
    public JedisPoolConfig poolConfig() {
        JedisPoolConfig pool = new JedisPoolConfig();
        // 设置连接池中可以同时分配的最大连接数，即最大活跃连接数
        pool.setMaxTotal(maxTotal);
        // 设置最小空闲连接数，即连接池在任何时间点至少要保持的空闲连接数量。
        pool.setMinIdle(minIdle);
        // 设置最大空闲连接数，即连接池中最多可以有多少 Jedis 连接处于空闲状态。
        pool.setMaxIdle(maxIdle);
        // 设置客户端请求连接时的最大等待时间。
        pool.setMaxWait(Duration.ofSeconds(maxWait));
        // 开启空闲连接的校验，确保空闲的连接是否可用。
        pool.setTestWhileIdle(testWhileIdle);
        // 开启连接 Jedis 获取连接时的检测。
        pool.setTestOnBorrow(testOnBorrow);
        // 设置连接空闲时的最小生存时间，超过该时间
        pool.setMinEvictableIdleTime(Duration.ofSeconds(minEvictableIdleDuration));
        pool.setTimeBetweenEvictionRuns(Duration.ofSeconds(timeBetweenEvictionRuns));
        // 设置每次执行空闲连接清理任务时，检测的连接数目
        pool.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
        return pool;
    }

    @Bean(name = "jedisConnectionFactory")
    public JedisConnectionFactory jedisConnectionFactory(@Qualifier("poolConfig") JedisPoolConfig jedisPoolConfig) {
        JedisConnectionFactory jedisConnectionFactory = null;
        log.info("redis host: [{}] ,has been init" ,host);
        //单机模式
        switch (mode) {
            case RedisModeConstant.STANDALONE:
                //获得默认的连接池构造
                //JedisConnectionFactory对于Standalone模式的没有（RedisStandaloneConfiguration，JedisPoolConfig）的构造函数，对此
                //我们用JedisClientConfiguration接口的builder方法实例化一个构造器，还得类型转换
                JedisClientConfiguration.JedisPoolingClientConfigurationBuilder jpcf = (JedisClientConfiguration.JedisPoolingClientConfigurationBuilder) JedisClientConfiguration.builder();
                //修改我们的连接池配置
                jpcf.poolConfig(jedisPoolConfig);
                //通过构造器来构造jedis客户端配置
                JedisClientConfiguration jedisClientConfiguration = jpcf.build();
                jedisConnectionFactory = new JedisConnectionFactory(redisStandaloneConfiguration(), jedisClientConfiguration);
                log.info("redis standalone mode  init success!");
                break;
            case RedisModeConstant.SENTINEL:
                //哨兵模式
                jedisConnectionFactory = new JedisConnectionFactory(sentinelConfiguration(), jedisPoolConfig);
                log.info("redis sentinel mode  init success!");
                break;
            case RedisModeConstant.CLUSTER:
                //Cluster模式
                jedisConnectionFactory = new JedisConnectionFactory(redisClusterConfiguration(), jedisPoolConfig);
                log.info("redis cluster mode  init success!");
                break;
            default:
                break;
        }
        return jedisConnectionFactory;
    }


    /**
     * 指定缓存的序列化方式
     * @param jedisConnectionFactory
     * @return
     */
    @Bean(name = "redisTemplate")
    public RedisTemplate redisTemplate(@Qualifier("jedisConnectionFactory") JedisConnectionFactory jedisConnectionFactory) {
        RedisTemplate<String ,Object> template = new RedisTemplate();
        template.setConnectionFactory(jedisConnectionFactory);
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL);
        serializer.setObjectMapper(mapper);
        template.setHashValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setKeySerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }


    /**
     * 单机模式初始化配置
     * @return
     */
    public RedisStandaloneConfiguration redisStandaloneConfiguration() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        Set<RedisNode> nodes = getNodes(host);
        Iterator<RedisNode> nodesIterator = nodes.iterator();
        String hostName = null;
        Integer port = 0;
        while (nodesIterator.hasNext()) {
            RedisNode redisNode = nodesIterator.next();
            port = redisNode.getPort();
            hostName = redisNode.getHost();

        }
        assert hostName != null;
        redisStandaloneConfiguration.setHostName(hostName);
        redisStandaloneConfiguration.setPort(port == null ? 6379 : port);
        if (!StringUtil.isBlank(password)) {
            redisStandaloneConfiguration.setPassword(RedisPassword.of(password));
        }
        redisStandaloneConfiguration.setDatabase(database);
        return redisStandaloneConfiguration;
    }

    /**
     * 集群模式初始化配置
     * @return
     */
    public RedisClusterConfiguration redisClusterConfiguration() {
        RedisClusterConfiguration redisClusterConfiguration = new RedisClusterConfiguration();
        Set<RedisNode> nodes = getNodes(host);
        redisClusterConfiguration.setClusterNodes(nodes);
        redisClusterConfiguration.setMaxRedirects(5);
        if (!StringUtil.isBlank(password)) {
            redisClusterConfiguration.setPassword(RedisPassword.of(password));
        }
        return redisClusterConfiguration;
    }

    /**
     * 哨兵模式初始化配置
     * @return
     */
    public RedisSentinelConfiguration sentinelConfiguration() {
        RedisSentinelConfiguration redisSentinelConfiguration = new RedisSentinelConfiguration();
        //配置master的名称
        redisSentinelConfiguration.master(sentinelMaster);
        //配置redis的哨兵sentinel
        Set<RedisNode> nodes = getNodes(host);
        redisSentinelConfiguration.setSentinels(nodes);
        if (!StringUtil.isBlank(password)) {
            redisSentinelConfiguration.setPassword(RedisPassword.of(password));
        }
        redisSentinelConfiguration.setDatabase(database);
        return redisSentinelConfiguration;
    }

    /**
     * 根据配置的host 获取到所有的节点信息
     * eg：host
     * 127.0.0.1:6379,127.0.0.1:6389,127.0.0.1:6399
     * @param nodesStr
     * @return
     */
    private Set<RedisNode> getNodes(String nodesStr) {
        Set<RedisNode> nodes = new HashSet<>();
        String[] nodeStr = nodesStr.replaceAll("\\s*", "").split(",");
        IntStream.range(0, nodeStr.length).forEach(i -> {
            String hostName = nodeStr[i].split(":")[0];
            int port = Integer.parseInt(nodeStr[i].split(":")[1]);
            RedisNode node = new RedisNode(hostName, port);
            nodes.add(node);
        });
        return nodes;
    }

}
