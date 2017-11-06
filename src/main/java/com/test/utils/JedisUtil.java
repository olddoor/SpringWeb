package com.test.utils;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
 
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
 
public class JedisUtil
{
    private Logger logger = Logger.getLogger(this.getClass().getName());
 
    private JedisUtil(){}
 
    private static class RedisUtilHolder{
        private static final JedisUtil instance = new JedisUtil();
    }
 
    public static JedisUtil getInstance(){
        return RedisUtilHolder.instance;
    }
 
    private static Map<String,JedisPool> maps = new HashMap<String,JedisPool>();
 
    private static JedisPool getPool(String ip, int port){
        String key = ip+":"+port;
        JedisPool pool = null;
        if(!maps.containsKey(key))
        {
            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(RedisConfig.MAX_ACTIVE);
//            config.setMaxActive(RedisConfig.MAX_ACTIVE);
            config.setMaxIdle(RedisConfig.MAX_IDLE);
            //连接在池中保持空闲而不被空闲连接回收器线程(如果有)回收的最小时间值，单位毫秒
            config.setSoftMinEvictableIdleTimeMillis(RedisConfig.MAX_WAIT);
//            config.setMaxWait(RedisConfig.MAX_WAIT);
            config.setTestOnBorrow(true);
            config.setTestOnReturn(true);
 
            pool = new JedisPool(config,ip,port,RedisConfig.TIMEOUT);
            maps.put(key, pool);
        }
        else
        {
            pool = maps.get(key);
        }
        return pool;        
    }
 
    public Jedis getJedis(String ip, int port)
    {
        Jedis jedis = null;
        int count = 0;
        do
        {
            try
            {
                jedis = getPool(ip,port).getResource();
            }
            catch (Exception e)
            {
                logger.error("get redis master1 failed!",e);
                getPool(ip,port).returnBrokenResource(jedis);
            }
        }
        while(jedis == null && count<RedisConfig.RETRY_NUM);
        return jedis;
    }
 
    public void closeJedis(Jedis jedis, String ip, int port){
        if(jedis != null)
        {
            getPool(ip,port).returnResource(jedis);
        }
    }
}

 class RedisConfig
{
    //可用连接实例的最大数目，默认值为8；
    //如果赋值为-1，则表示不限制；如果pool已经分配了maxActive个jedis实例，则此时pool的状态为exhausted(耗尽)。
    public static int MAX_ACTIVE = 1024;
 
    //控制一个pool最多有多少个状态为idle(空闲的)的jedis实例，默认值也是8。
    public static int MAX_IDLE = 200;
 
    //等待可用连接的最大时间，单位毫秒，默认值为-1，表示永不超时。如果超过等待时间，则直接抛出JedisConnectionException；
    public static int MAX_WAIT = 10000;
 
    public static int TIMEOUT = 10000;
 
    public static int RETRY_NUM = 5;
}
