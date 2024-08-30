package com.tarento.commenthub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tarento.commenthub.dto.CommentTreeIdentifierDTO;
import com.tarento.commenthub.entity.CommentTree;
import java.util.List;

public interface CommentTreeService {

  CommentTree createCommentTree(JsonNode payload);

  CommentTree updateCommentTree(JsonNode payload);

  CommentTree getCommentTreeById(String commentTreeId);

  CommentTree getCommentTree(CommentTreeIdentifierDTO commentTreeIdentifierDTO);

  void updateCommentTreeForDeletedComment(String commentId,
      CommentTreeIdentifierDTO commentTreeIdentifierDTO);

  List<CommentTree> getAllCommentTreeForMultipleWorkflows(String entityType, String entityId,
      List<String> workflows);

  CommentTree setCommentTreeStatusToResolved(CommentTreeIdentifierDTO commentTreeIdentifierDTO);

}