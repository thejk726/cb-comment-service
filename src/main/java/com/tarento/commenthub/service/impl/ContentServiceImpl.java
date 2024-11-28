package com.tarento.commenthub.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.tarento.commenthub.constant.Constants;
import com.tarento.commenthub.service.ContentService;
import com.tarento.commenthub.utility.CbServerProperties;
import com.tarento.commenthub.utility.DataCacheManager;
import com.tarento.commenthub.utility.RedisCacheMngr;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class ContentServiceImpl implements ContentService {

  @Autowired
  private DataCacheManager dataCacheMgr;

  @Autowired
  private CbServerProperties serverConfig;

  private RestTemplate restTemplate;

  @Autowired
  private RedisCacheMngr redisCacheMgr;

  @Autowired
  private ObjectMapper mapper;

  @Override
  public Map<String, Object> readContentFromCache(String contentId, List<String> fields) {
    log.info("ContentServiceImpl::readContentFromCache:entering");
    if (CollectionUtils.isEmpty(fields)) {
      fields = Arrays.asList(serverConfig.getDefaultContentProperties().split(",", -1));
    }
    Map<String, Object> responseData = null;

    responseData = dataCacheMgr.getContentFromCache(contentId);

    if (MapUtils.isEmpty(responseData) || responseData.size() < fields.size()) {
      // DataCacheMgr doesn't have data OR contains less content fields.
      // Let's read again
      String contentString = redisCacheMgr.getContentFromCache(contentId);
      if (StringUtils.isBlank(contentString)) {
        // Tried reading from Redis - but redis didn't have data for some reason.
        // Or connection failed ??
        responseData = readContent(contentId, fields);
      } else {
        try {
          responseData = new HashMap<String, Object>();
          Map<String, Object> contentData = mapper.readValue(contentString,
              new TypeReference<Map<String, Object>>() {
              });
          if (MapUtils.isNotEmpty(contentData)) {
            for (String field : fields) {
              if (contentData.containsKey(field)) {
                responseData.put(field, contentData.get(field));
              }
            }
            dataCacheMgr.putContentInCache(contentId, responseData);
          }
        } catch (Exception e) {
          log.error("Failed to parse content info from redis. Exception: " + e.getMessage(), e);
          responseData = readContent(contentId);
        }
      }
    } else {
      // We are going to send the data read from which might have more fields.
      // This is fine for now.
    }
    log.info("ContentServiceImpl::readContentFromCache");
    return responseData;
  }

  @Override
  public Map<String, Object> readContent(String contentId, List<String> fields) {
    log.info("ContentServiceImpl::readContent:inside");
    StringBuilder url = new StringBuilder();
    url.append(serverConfig.getContentHost()).append(serverConfig.getContentReadEndPoint())
        .append("/" + contentId)
        .append(serverConfig.getContentReadEndPointFields());
    if (CollectionUtils.isNotEmpty(fields)) {
      StringBuffer stringBuffer = new StringBuffer(String.join(",", fields));
      url.append(",").append(stringBuffer);
    }
    Map<String, Object> response = (Map<String, Object>) fetchResult(url.toString());
    if (null != response && Constants.OK.equalsIgnoreCase(
        (String) response.get(Constants.RESPONSE_CODE))) {
      Map<String, Object> contentResult = (Map<String, Object>) response.get(Constants.RESULT);
      log.info("ContentServiceImpl::readContent:read the content");
      return (Map<String, Object>) contentResult.get(Constants.CONTENT);
    }
    return null;
  }

  public Object fetchResult(String uri) {
    log.info("ContentServiceImpl::fetchResult:inside");
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    Object response = null;
    try {
      if (log.isDebugEnabled()) {
        StringBuilder str = new StringBuilder(this.getClass().getCanonicalName())
            .append(Constants.FETCH_RESULT_CONSTANT).append(System.lineSeparator());
        str.append(Constants.URI_CONSTANT).append(uri).append(System.lineSeparator());
        log.debug(str.toString());
        log.info("ContentServiceImpl::fetchResult:fetched");
      }
      response = restTemplate.getForObject(uri, Map.class);
    } catch (HttpClientErrorException e) {
      try {
        response = (new ObjectMapper()).readValue(e.getResponseBodyAsString(),
            new TypeReference<HashMap<String, Object>>() {
            });
      } catch (Exception e1) {
      }
      log.error("Error received: " + e.getResponseBodyAsString(), e);
    } catch (Exception e) {
      log.error(e.toString());
      try {
        log.warn("Error Response: " + mapper.writeValueAsString(response));
      } catch (Exception e1) {
      }
    }
    return response;
  }

  @Override
  public Map<String, Object> readContent(String contentId) {
    log.info("ContentServiceImpl::readContent:inside");
    return readContent(contentId, Collections.emptyList());
  }

}
