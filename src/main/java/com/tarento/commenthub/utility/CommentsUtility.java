package com.tarento.commenthub.utility;

import java.util.List;

public class CommentsUtility {

  public static boolean containsNull(List<?> list) {
    if (list == null) {
      return true;
    }

    for (Object element : list) {
      if (element == null) {
        return true;
      }
    }

    return false;
  }
}
