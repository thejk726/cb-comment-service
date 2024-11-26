package com.tarento.commenthub.authentication.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarento.commenthub.constant.Constants;
import com.tarento.commenthub.transactional.cassandrautils.CassandraOperation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class FetchUserDetails {

  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  @Autowired
  private CassandraOperation cassandraOperation;

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

  public List<Object> fetchUserFromprimary(List<String> userIds) {
    List<Object> userList = new ArrayList<>();
    Map<String, Object> propertyMap = new HashMap<>();
    propertyMap.put(Constants.ID, userIds);
    List<Map<String, Object>> userInfoList = cassandraOperation.getRecordsByPropertiesWithoutFiltering(
        Constants.KEYSPACE_SUNBIRD, Constants.TABLE_USER, propertyMap,
        Arrays.asList(Constants.PROFILE_DETAILS, Constants.FIRST_NAME, Constants.ID), null);

    userList = userInfoList.stream()
        .map(userInfo -> {
          Map<String, Object> userMap = new HashMap<>();

          // Extract user ID and user name
          String userId = (String) userInfo.get(Constants.ID);
          String userName = (String) userInfo.get(Constants.FIRST_NAME);

          userMap.put(Constants.USER_ID_KEY, userId);
          userMap.put(Constants.FIRST_NAME_KEY, userName);

          // Process profile details if present
          String profileDetails = (String) userInfo.get(Constants.PROFILE_DETAILS);
          if (StringUtils.isNotBlank(profileDetails)) {
            try {
              // Convert JSON profile details to a Map
              Map<String, Object> profileDetailsMap = objectMapper.readValue(profileDetails,
                  new TypeReference<HashMap<String, Object>>() {});

              // Check for profile image and add to userMap if available
              if (MapUtils.isNotEmpty(profileDetailsMap)) {
                if (profileDetailsMap.containsKey(Constants.PROFILE_IMG) && StringUtils.isNotBlank((String) profileDetailsMap.get(Constants.PROFILE_IMG))){
                  userMap.put(Constants.PROFILE_IMG_KEY, (String) profileDetailsMap.get(Constants.PROFILE_IMG));
                }
                if (profileDetailsMap.containsKey(Constants.DESIGNATION_KEY) && StringUtils.isNotEmpty((String) profileDetailsMap.get(Constants.DESIGNATION_KEY))) {

                  userMap.put(Constants.DESIGNATION_KEY, (String) profileDetailsMap.get(Constants.PROFILE_IMG));
                }
                if(profileDetailsMap.containsKey(Constants.EMPLOYMENT_DETAILS) && MapUtils.isNotEmpty(
                    (Map<?, ?>) profileDetailsMap.get(Constants.EMPLOYMENT_DETAILS)) && ((Map<?, ?>) profileDetailsMap.get(Constants.EMPLOYMENT_DETAILS)).containsKey(Constants.DEPARTMENT_KEY) && StringUtils.isNotBlank(
                    (String) ((Map<?, ?>) profileDetailsMap.get(Constants.EMPLOYMENT_DETAILS)).get(Constants.DEPARTMENT_KEY))){
                  userMap.put(Constants.DEPARTMENT, (String) ((Map<?, ?>) profileDetailsMap.get(Constants.EMPLOYMENT_DETAILS)).get(Constants.DEPARTMENT_KEY));

                }

              }
            } catch (JsonProcessingException e) {
              throw new RuntimeException(e);
            }
          }

          return userMap;
        })
        .collect(Collectors.toList());
    return userList;
    }
}
