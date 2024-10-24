package com.tarento.commenthub.dto;

import jnr.ffi.annotations.In;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SearchCriteria {

  private Integer limit;

  private Integer offset;

  private  String commentTreeId;

  private  String entityType;

  private  String entityId;

  private  String workflow;

  //one variable for override cache

  private boolean overrideCache;

}
