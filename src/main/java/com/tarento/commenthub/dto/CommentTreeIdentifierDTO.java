package com.tarento.commenthub.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CommentTreeIdentifierDTO {

  private String entityType;

  private String entityId;

  private String workflow;
}