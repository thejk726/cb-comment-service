package com.tarento.commenthub.utility;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DataCacheManager {

  private Map<String, Map<String, Object>> contentCacheMap = new HashMap<String, Map<String, Object>>();

  public Map<String, Object> getContentFromCache(String key) {
    if (contentCacheMap.containsKey(key)) {
      return contentCacheMap.get(key);
    }
    return null;
  }

  public void putContentInCache(String key, Map<String, Object> value) {
    contentCacheMap.put(key, value);
  }

}
