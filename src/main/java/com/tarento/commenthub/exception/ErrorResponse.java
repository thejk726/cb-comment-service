package com.tarento.commenthub.exception;

import java.util.Map;
import lombok.Builder;
import lombok.Value;
import org.springframework.http.HttpStatus;

@Value
@Builder
public class ErrorResponse {

  private String code;
  private String message;
  private Map<String, String> errors;
  private Integer httpStatusCode;
}