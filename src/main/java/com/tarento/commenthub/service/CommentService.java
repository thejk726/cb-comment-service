package com.tarento.commenthub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tarento.commenthub.dto.CommentTreeIdentifierDTO;
import com.tarento.commenthub.dto.MultipleWorkflowsCommentResponseDTO;
import com.tarento.commenthub.dto.CommentsResoponseDTO;
import com.tarento.commenthub.dto.ResponseDTO;
import com.tarento.commenthub.entity.Comment;
import com.tarento.commenthub.transactional.utils.ApiResponse;
import io.swagger.v3.core.util.Json;
import java.util.List;
import java.util.Map;

public interface CommentService {

  ResponseDTO addFirstCommentToCreateTree(JsonNode payload);

  ResponseDTO addNewCommentToTree(JsonNode payload);

  ResponseDTO updateExistingComment(JsonNode paylaod);

  CommentsResoponseDTO getComments(CommentTreeIdentifierDTO commentTreeIdentifierDTO);

  List<MultipleWorkflowsCommentResponseDTO> getComments(String entityType, String entityId,
      List<String> workflowList);

  Comment deleteCommentById(String commentId, CommentTreeIdentifierDTO commentTreeIdentifierDTO);

  Comment resolveComment(String commentId);

  ApiResponse likeComment(Map<String, Object> likePayload);

  ApiResponse getCommentLike(String commentId, String userId);
}
