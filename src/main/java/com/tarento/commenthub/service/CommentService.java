package com.tarento.commenthub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tarento.commenthub.dto.CommentTreeIdentifierDTO;
import com.tarento.commenthub.dto.MultipleWorkflowsCommentResponseDTO;
import com.tarento.commenthub.dto.CommentsResoponseDTO;
import com.tarento.commenthub.dto.ResponseDTO;
import com.tarento.commenthub.entity.Comment;
import io.swagger.v3.core.util.Json;
import java.util.List;

public interface CommentService {

  ResponseDTO addFirstCommentToCreateTree(JsonNode payload);

  ResponseDTO addNewCommentToTree(JsonNode payload);

  ResponseDTO updateExistingComment(JsonNode paylaod);

  CommentsResoponseDTO getComments(CommentTreeIdentifierDTO commentTreeIdentifierDTO);

  List<MultipleWorkflowsCommentResponseDTO> getComments(String entityType, String entityId,
      List<String> workflowList);

  Comment deleteCommentById(String commentId, CommentTreeIdentifierDTO commentTreeIdentifierDTO);

  Comment resolveComment(String commentId);

}
