package com.tarento.commenthub.authentication.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserFetchFromredis {

  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  ObjectMapper objectMapper = new ObjectMapper();

  public List<Object> fetchDataForKeys(List<String> keys) {
    // Fetch values for all keys from Redis
    List<Object> values = redisTemplate.opsForValue().multiGet(keys);

    // Create a map of key-value pairs, converting stringified JSON objects to User objects
    return keys.stream()
        .filter(key -> values.get(keys.indexOf(key)) != null) // Filter out null values
        .map(key -> {
          String stringifiedJson = (String) values.get(keys.indexOf(key)); // Cast the value to String
          try {
            // Convert the stringified JSON to a User object using ObjectMapper
            return objectMapper.readValue(stringifiedJson, Object.class); // You can map this to a specific User type if needed
          } catch (Exception e) {
            // Handle any exceptions during deserialization
            e.printStackTrace();
            return null; // Return null in case of error
          }
        })
        .collect(Collectors.toList());
  }
}
