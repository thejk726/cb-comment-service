package com.tarento.commenthub.utility;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Component
@Slf4j
public class RedisCacheMngr {

  @Autowired
  private JedisPool jedisPool;

  public String getContentFromCache(String key) {
    try (Jedis jedis = jedisPool.getResource()) {
      return jedis.get(key);
    } catch (Exception e) {
      log.error(e.toString());
      return null;
    }
  }

}
