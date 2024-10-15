package com.peach.common.redis;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.peach.common.IRedisDao;
import com.peach.common.constant.PubCommonConst;
import com.peach.common.constant.RedisModeConstant;
import com.peach.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;
import redis.clients.jedis.*;
import redis.clients.jedis.util.JedisClusterCRC16;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //TODO
 * @CreateTime 2024/10/10 16:09
 */
@Slf4j
@Component
public class RedisDaoImpl extends AbstractRedisService implements IRedisDao {

    @Override
    public boolean existsKey(Object key) {
        Boolean exist =  redisTemplate.hasKey(key);
        if (exist == null){
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    @Override
    public boolean delete(final Object key) {
        int reTryCount = 0;
        while (reTryCount < RETRY_TIMES){
            try {
                if (Boolean.TRUE.equals(redisTemplate.delete(key))){
                    break;
                }
            }catch (Exception e){
                log.error("Filed to delete key:[{}] attempt : ｛｝/{}", key,reTryCount,RETRY_TIMES,e);
            }
        }
        return Boolean.FALSE;
    }

    @Override
    public void delete(final String[] keys) {
        if (keys.length == PubCommonConst.LOGIC_FLASE){
            return;
        }
        for (int index = 0; index < keys.length; index++) {
            delete(keys[index]);
        }
    }

    @Override
    public long deletePattern(Object pattern, Integer count) {
        Set<Object> keys = scan(String.valueOf(pattern), count);
        if ((keys != null ? keys.size() : 0) > 0) {
            return delete(keys);
        } else {
            return 0;
        }
    }

    @Override
    public long delete(final Set keys) {
        long size = 0;
        Set<Object> deleteKeys = new HashSet<>();
        for (Object key : keys) {
            deleteKeys.add(key);
            if (deleteKeys.size() < BATCH_DELETE_SIZE){
                continue;
            }
            Long count = redisTemplate.delete(deleteKeys);
            if (count == null) {
                count = 0L;
            }
            size = size + count;
            deleteKeys.clear();
        }
        if (deleteKeys.size() > PubCommonConst.LOGIC_FLASE){
            Long count = redisTemplate.delete(deleteKeys);
            if (count == null){
                count = 0L;
            }
            size = size + count;
            deleteKeys.clear();
        }
        return size;
    }

    @Override
    public boolean vSet(Object key, Object value) {
        Boolean isSuccess = Boolean.FALSE;
        try {
            ValueOperations valueOperations = redisTemplate.opsForValue();
            valueOperations.set(key,value);
            isSuccess = Boolean.TRUE;
        }catch (Exception ex){
            log.error("Filed to set key:[{}] value:[{}]",key,value,ex);
        }
        return isSuccess;
    }

    @Override
    public boolean vSet(Object key, Object value, Long expireTime) {
        Boolean isSuccess = Boolean.FALSE;
        try {
            ValueOperations valueOperations = redisTemplate.opsForValue();
            valueOperations.set(key,value,expireTime);
            isSuccess = Boolean.TRUE;
        }catch (Exception ex){
            log.error("Filed to set key:[{}] value:[{}] expireTime:[{}]",key,value,expireTime,ex);
        }
        return isSuccess;
    }

    @Override
    public boolean vSet(Object key, Object value, Duration expire) {
        Boolean isSuccess = Boolean.FALSE;
        try {
            ValueOperations valueOperations = redisTemplate.opsForValue();
            valueOperations.set(key,value,expire);
            isSuccess = Boolean.TRUE;
        }catch (Exception ex){
            log.error("Filed to set key:[{}] value:[{}] expire:[{}]",key,value,expire,ex);
        }
        return isSuccess;
    }

    @Override
    public boolean vSetUpdate(Object key, Long expireTime) {
        return false;
    }

    @Override
    public Object vGet(Object key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void hmSet(Object key, Object hashKey, Object value) {
        HashOperations hashOperations = redisTemplate.opsForHash();
        hashOperations.put(key,hashKey,value);
    }

    @Override
    public void hmSetAll(Object key, Map<Object, Object> map) {
        HashOperations hashOperations = redisTemplate.opsForHash();
        hashOperations.putAll(key,map);
    }

    @Override
    public Map<Object, Object> hmGet(Object key) {
        HashOperations hash = redisTemplate.opsForHash();
        return hash.entries(key);
    }

    @Override
    public Object hmGet(Object key, Object hashKey) {
        HashOperations hash = redisTemplate.opsForHash();
        return hash.get(key, hashKey);
    }

    @Override
    public Object hmDel(Object key, Object hashKey) {
        HashOperations<Object, Object, Object> hash = redisTemplate.opsForHash();
        return hash.delete(key, hashKey);
    }

    @Override
    public long lSize(final Object key) {
        ListOperations listOption = redisTemplate.opsForList();
        Long size = listOption.size(key);
        return size == null ? 0L : size;
    }

    @Override
    public Object lLeftIndexFirst(Object key) {
        ListOperations listOption = redisTemplate.opsForList();
        Long size = listOption.size(key);
        return size == null ? 0L : size;
    }

    @Override
    public Object lRightIndexFirst(Object key) {
        ListOperations<Object, Object> list = redisTemplate.opsForList();
        return list.index(key, -1);
    }

    @Override
    public Object lindex(Object key, long index) {
        ListOperations<Object, Object> list = redisTemplate.opsForList();
        return list.index(key, index);
    }

    @Override
    public void lLeftPush(Object k, Object v) {
        ListOperations<Object, Object> list = redisTemplate.opsForList();
        list.leftPush(k, v);
    }

    @Override
    public void lLeftPush(Object k, Object v, boolean bool) {
        ListOperations<Object, Object> list = redisTemplate.opsForList();
        list.leftPush(k, v, bool);
    }

    @Override
    public void lLeftPushAll(Object k, List<Object> list) {
        ListOperations<Object, Object> listOperations = redisTemplate.opsForList();
        listOperations.leftPush(k,list);
    }

    @Override
    public void lRightPush(Object k, Object v) {
        ListOperations<Object, Object> listOperations = redisTemplate.opsForList();
        listOperations.rightPush(k,v);
    }

    @Override
    public void lRightPush(Object k, Object v, boolean bool) {
        ListOperations<Object, Object> listOperations = redisTemplate.opsForList();
        listOperations.rightPush(k,v,bool);
    }

    @Override
    public void lRightPushAll(Object k, List<Object> list) {
        ListOperations<Object, Object> listOperations = redisTemplate.opsForList();
        listOperations.rightPush(k, list);
    }

    @Override
    public Object lLeftPop(Object key) {
        ListOperations<Object, Object> listOperations = redisTemplate.opsForList();
        return listOperations.leftPop(key);
    }

    @Override
    public Object lRightPop(Object key) {
        ListOperations<Object, Object> listOperations = redisTemplate.opsForList();
        return listOperations.rightPop(key);
    }

    @Override
    public long lRemove(Object key, long count) {
        ListOperations<Object, Object> listOperations = redisTemplate.opsForList();
        Long size = listOperations.remove(key, count, null);
        return size == null ? 0 : size;
    }

    @Override
    public long lRemove(Object key, long count, Object value) {
        ListOperations<Object, Object> listOperations = redisTemplate.opsForList();
        Long size = listOperations.remove(key, count, value);
        return size == null ? 0 : size;
    }

    @Override
    public long lRemove(Object k, Object v) {
        ListOperations<Object, Object> list = redisTemplate.opsForList();
        Long count = list.size(k);
        if (count != null) {
            Long size = list.remove(k, count, v);
            return size == null ? 0 : size;
        }
        return 0L;
    }

    @Override
    public Object lRange(Object key) {
        ListOperations<Object, Object> list = redisTemplate.opsForList();
        Long size = list.size(key);
        if (size == null) {
            return new ArrayList<>();
        }
        return list.range(key, 0, size);
    }

    @Override
    public List<?> lRange(Object key, long start, long end) {
        ListOperations<Object, Object> list = redisTemplate.opsForList();
        Long size = list.size(key);
        if (size == null) {
            return new ArrayList<>();
        }
        return list.range(key, start, end);
    }

    @Override
    public void sAdd(Object key, Object value) {
        SetOperations<Object, Object> set = redisTemplate.opsForSet();
        set.add(key, value);
    }

    @Override
    public Set<Object> sMembers(Object key) {
        SetOperations<Object, Object> set = redisTemplate.opsForSet();
        Set<Object> members = set.members(key);
        return members == null ? new HashSet<>() : members;
    }

    @Override
    public Object sRandomMember(String key) {
        return redisTemplate.opsForSet().randomMember(key);
    }

    @Override
    public Object sPop(String key) {
        return redisTemplate.opsForSet().pop(key);
    }

    @Override
    public void sRemove(String key, Object... values) {
        redisTemplate.opsForSet().remove(key,values);
    }

    @Override
    public void zAdd(Object key, Object value, double scoure) {
        redisTemplate.opsForSet().add(key,value,scoure);
    }

    @Override
    public Set<Object> rangeByScore(Object key, double scoure, double scoure1) {
        ZSetOperations zSetOperations = redisTemplate.opsForZSet();
        return zSetOperations.rangeByScore(key,scoure,scoure1);
    }

    @Override
    public void hmSetIncrement(Object key, Object hashKey, Long value) {
        HashOperations<Object, Object, Object> hash = redisTemplate.opsForHash();
        hash.increment(key, hashKey, value);
    }

    @Override
    public void convertAndSend(String topic, String message) {
        try {
            redisTemplate.convertAndSend(topic, message);
        }catch (Exception e){
            log.error("method: convertAndSend has been field,topic:[{}] message:[{}]"+e.getMessage(),topic,message,e);
        }
    }

    @Override
    public void convertAndSend(String topic, Object message) {
        try {
            redisTemplate.convertAndSend(topic, message);
        }catch (Exception e){
            log.error("method: convertAndSend has been field,topic:[{}] message:[{}]"+e.getMessage(),topic,message,e);
        }
    }

    @Override
    public Set<Object> scan(String matchKey, Integer count) {
        Integer scanLine = (count == null || count == 0) ? scanLineNumber : count;
        if (RedisModeConstant.STANDALONE.equals(mode)) {
            if (scanCommandIsUsed == IS_USED_REDIS_COMMAND) {
                return (Set<Object>) redisTemplate.execute((RedisCallback<Set<Object>>) connection -> {
                    Set<Object> keysTmp = new HashSet<>();
                    Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match(matchKey).count(scanLine).build());
                    while (cursor.hasNext()) {
                        keysTmp.add(new String(cursor.next()));
                    }
                    return keysTmp;
                });
            }
            return redisTemplate.keys(matchKey);
        }

        List<String> resultList = new ArrayList<>();
        RedisClusterConnection redisClusterConnection = Objects.requireNonNull(redisTemplate.getConnectionFactory()).getClusterConnection();
        //这里是获取redispool的另外一种方式与上边的pipline可以对比下，两种方式都可以实现
        Map<String, JedisPool> clusterNodes = ((JedisCluster) redisClusterConnection.getNativeConnection()).getClusterNodes();
        for (Map.Entry<String, JedisPool> entry : clusterNodes.entrySet()) {
            //获取单个的jedis对象
            JedisPool jedisPool = entry.getValue();
            Jedis jedis = jedisPool.getResource();
            // 判断非从节点(因为若主从复制，从节点会跟随主节点的变化而变化)，此处要使用主节点从主节点获取数据
            try {
                if (jedis.info("replication").contains("role:slave")) {
                    continue;
                }
                Collection<String> keys;
                if (scanCommandIsUsed == IS_USED_REDIS_COMMAND) {
                    keys = getScanResult(jedis, matchKey, scanLine);
                } else {
                    keys = jedis.keys(matchKey);
                }
                if (keys.isEmpty()) {
                    continue;
                }
                Map<Integer, List<String>> map = new HashMap<>(8);
                //接下来的循环不是多余的，需要注意
                for (String key : keys) {
                    // cluster模式执行多key操作的时候，这些key必须在同一个slot上，不然会报:JedisDataException:
                    int slot = JedisClusterCRC16.getSlot(key);
                    // 按slot将key分组，相同slot的key一起提交
                    if (map.containsKey(slot)) {
                        map.get(slot).add(key);
                    } else {
                        List<String> list = new ArrayList<>();
                        list.add(key);
                        map.put(slot, list);
                    }
                }
                for (Map.Entry<Integer, List<String>> integerListEntry : map.entrySet()) {
                    resultList.addAll(integerListEntry.getValue());
                }
            } catch (Exception e) {
                log.error("redis scan key:[{}],scanLine:[{}]:"+e.getMessage(),matchKey,count,e);
            } finally {
                try {
                    jedisPool.returnResource(jedis);
                } catch (Exception e) {
                    log.error("jedisPool.returnResource error:"+e.getMessage(), e);
                }
            }
        }
        return new HashSet<>(resultList);
    }

    @Override
    public Map hscan(String matchKey, Integer count) {

        Map<Object, Object> map = new HashMap<>();
        try {
            Cursor<Map.Entry<Object, Object>> cursor = redisTemplate.opsForHash().scan("field", ScanOptions.scanOptions().match(matchKey).count(count).build());
            while (cursor.hasNext()) {
                Object key = cursor.next().getKey();
                Object valueSet = cursor.next().getValue();
                map.put(key, valueSet);
            }
            //关闭cursor
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    @Override
    public Set<String> sscan(String matchKey, Integer count) {

        Set<String> keys = new HashSet<>();
        try {
            Cursor<Object> cursor = redisTemplate.opsForSet().scan("setValue", ScanOptions.scanOptions().match(matchKey).count(count).build());
            while (cursor.hasNext()) {
                keys.add(cursor.next().toString());
            }
            //关闭cursor
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return keys;
    }

    @Override
    public Map zscan(String matchKey, Integer count) {

        Map<Object, Object> map = new HashMap<>();
        try {
            Cursor<ZSetOperations.TypedTuple<Object>> cursor = redisTemplate.opsForZSet().scan("zSetValue", ScanOptions.scanOptions().match(matchKey).count(count).build());
            while (cursor.hasNext()) {
                ZSetOperations.TypedTuple<Object> typedTuple = cursor.next();
                Object value = typedTuple.getValue();
                Object valueScore = typedTuple.getScore();
                map.put(value, valueScore);
            }
            //关闭cursor
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    @Override
    public void setKeySerializer(StringRedisSerializer serializer) {
        redisTemplate.setKeySerializer(serializer);
    }

    @Override
    public Set<Object> keys(String matchKey) {
        return scan(matchKey, null);
    }

    @Override
    public void expire(Object k, long timeout) {
        try {
            redisTemplate.expire(k, timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("method: expire has field,key:[{}],timeout:[{}]"+e.getMessage(),k,timeout,e);
        }
    }

    /**
     * 递增数
     * @param key
     * @param fromIndex
     * @return: long
     */
    @Override
    public long increaseNum(String key, long fromIndex) {
        if (StringUtil.isBlank(key)){
            log.error("method:increaseNum key can't be null");
            throw new RuntimeException("method:increaseNum key can't be null");
        }
        if (fromIndex < PubCommonConst.LOGIC_FLASE){
            log.error("method:increaseNum fromIndex can't less than zero");
            throw new RuntimeException("method:increaseNum fromIndex can't less than zero");
        }
        ValueOperations valueOperations = redisTemplate.opsForValue();
        Long increment = valueOperations.increment(key);
        return increment == null ? 0L : increment;
    }

    /**
     * 递减数
     * @param key
     * @param decIndex
     * @return: long
     */
    @Override
    public long decreaseNum(String key, long decIndex) {
        if (StringUtil.isBlank(key)){
            log.error("method:increaseNum key can't be null");
            throw new RuntimeException("method:increaseNum key can't be null");
        }
        if (decIndex < PubCommonConst.LOGIC_FLASE){
            log.error("method:increaseNum fromIndex can't less than zero");
            throw new RuntimeException("method:increaseNum fromIndex can't less than zero");
        }
        ValueOperations valueOperations = redisTemplate.opsForValue();
        Long decrement = valueOperations.decrement(key);
        return decrement == null ? 0L : decrement;
    }


    /**
     * 获取扫描结果
     * @param redisService
     * @param key
     * @param count
     * @return
     */
    private static List<String> getScanResult(Jedis redisService, String key, Integer count) {
        Date startTime = new Date();
        //扫描的参数对象创建与封装
        ScanParams params = new ScanParams();
        params.match(key);
        //扫描返回一百行，这里可以根据业务需求进行修改
        params.count(count);
        String cursor = PubCommonConst.STR_LOGIC_FLASE;
        ScanResult<String> scanResult = redisService.scan(cursor, params);
        List<String> list = new ArrayList<>();
        //获取当前游标值
        cursor = scanResult.getCursor();
        //取得本轮scan的结果
        List<String> results = scanResult.getResult();
        if (results != null && results.size() > PubCommonConst.LOGIC_FLASE) {
            list.addAll(results);
        }
        // 如果游标值为0，表示已经遍历完所有键值对
        while (!PubCommonConst.STR_LOGIC_FLASE.equals(cursor)) {
            // 更新游标并继续遍历下⼀轮
            scanResult = redisService.scan(cursor, params);
            cursor = scanResult.getCursor();
            results = scanResult.getResult();
            if (results != null && results.size() > PubCommonConst.LOGIC_FLASE) {
                list.addAll(results);
            }
            long timeOut = DateUtil.between(startTime, new Date(), DateUnit.SECOND);
            if (StringUtil.isBlank(cursor)) {
                log.error("the cursor is null");
                cursor = PubCommonConst.STR_LOGIC_FLASE;
            }
            if (timeOut >= 10) {
                log.info("redis scan more than 10s");
                cursor = PubCommonConst.STR_LOGIC_FLASE;
            }
        }
        return list;
    }
}
