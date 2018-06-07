package dao.cache;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import entity.Seckill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * @Author: Li Guangwei
 * @Descriptions: TODO
 * @Date: 2018/6/7 14:51
 * @Version: 1.0
 */
public class RedisDao {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final JedisPool jedisPool;

    public RedisDao(String ip, int port){
        jedisPool = new JedisPool(ip,port);
    }
    private RuntimeSchema<Seckill> schema = RuntimeSchema.createFrom(Seckill.class);
    public Seckill getSeckill(long seckillId){
        //redis操作逻辑

        try {
            Jedis jedis = jedisPool.getResource();
            try{
                String key = "seckill:"+seckillId;
                //并没有实现内部序列化擦操作
                //get->byte[]->反序列化->object（Seckill）
                //采用自定义序列化
                //protostuff: pojo
                byte[] bytes = jedis.get(key.getBytes());
                //缓存重获取到
                if(bytes!=null){
                    //空对象
                    Seckill seckill = schema.newMessage();
                    ProtostuffIOUtil.mergeFrom(bytes,seckill,schema);
                    //seckill 被反序列化
                    return seckill;
                }
            }finally {
                jedis.close();
            }

        } catch (Exception e) {
           logger.error(e.getMessage(),e);
        }
        return  null;
    }

    public String putSeckill(Seckill seckill){
        //set object(Seckill) -》序列化-> byte【】

        try {
            Jedis jedis = jedisPool.getResource();
            try{
                String key = "seckill:"+seckill.getSeckillId();
                byte[] bytes = ProtostuffIOUtil.toByteArray(seckill,schema,
                        LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));
                int timeOut = 60*60; //缓存一个小时
                String result = jedis.setex(key.getBytes(),timeOut,bytes);
                return result;
            }finally {
                jedis.close();
            }
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
        }
        return null;
    }
}
