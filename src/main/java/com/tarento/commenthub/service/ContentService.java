package com.tarento.commenthub.service;

import java.util.List;
import java.util.Map;

public interface ContentService {

  public Map<String, Object> readContentFromCache(String contentId, List<String> fields);

  public Map<String, Object> readContent(String contentId, List<String> fields);

  public Map<String, Object> readContent(String contentId);

}
