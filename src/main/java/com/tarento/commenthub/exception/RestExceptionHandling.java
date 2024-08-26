package com.tarento.commenthub.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class RestExceptionHandling {

  @ExceptionHandler(Exception.class)
  public ResponseEntity handleException(Exception ex) {
    log.debug("RestExceptionHandler::handleException::" + ex);
    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
    ErrorResponse errorResponse = null;
    if (ex instanceof CommentException) {
      CommentException commentException = (CommentException) ex;
      status = HttpStatus.OK;
      errorResponse = ErrorResponse.builder()
          .code(commentException.getCode())
          .message(commentException.getMessage())
          .httpStatusCode(commentException.getHttpStatusCode() != null
              ? commentException.getHttpStatusCode()
              : HttpStatus.OK.value())
          .build();
      if (StringUtils.isNotBlank(commentException.getMessage())) {
        log.error(commentException.getMessage());
      }

      return new ResponseEntity<>(errorResponse, status);
    }
    errorResponse = ErrorResponse.builder()
        .code(ex.getMessage()).build();
    return new ResponseEntity<>(errorResponse, status);
  }

}