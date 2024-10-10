package com.tarento.commenthub.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.tarento.commenthub.constant.Constants;
import com.tarento.commenthub.dto.CommentTreeIdentifierDTO;
import com.tarento.commenthub.dto.MultipleWorkflowsCommentResponseDTO;
import com.tarento.commenthub.dto.CommentsResoponseDTO;
import com.tarento.commenthub.dto.ResponseDTO;
import com.tarento.commenthub.entity.Comment;
import com.tarento.commenthub.entity.CommentTree;
import com.tarento.commenthub.service.CommentService;
import com.tarento.commenthub.service.CommentTreeService;
import com.tarento.commenthub.transactional.utils.ApiResponse;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/comment")
@Slf4j
public class CommentController {

  @Autowired
  CommentTreeService commentTreeService;

  @Autowired
  private CommentService commentService;

  @PostMapping("/v1/addFirst")
  public ResponseDTO addFirstComment(@RequestBody JsonNode payload) {
    return commentService.addFirstCommentToCreateTree(payload);
  }

  @PostMapping("/v1/addNew")
  public ResponseDTO addNewComment(@RequestBody JsonNode payload) {
    return commentService.addNewCommentToTree(payload);
  }

  @PutMapping("/v1/update")
  public ResponseDTO updateExistingComment(@RequestBody JsonNode payload) {
    return commentService.updateExistingComment(payload);
  }

  @GetMapping("/v1/getAll")
  public CommentsResoponseDTO getComments(
      @RequestParam(name = "entityType") String entityType,
      @RequestParam(name = "entityId") String entityId,
      @RequestParam(name = "workflow") String workflow) {

    CommentTreeIdentifierDTO commentTreeIdentifierDTO = new CommentTreeIdentifierDTO();
    commentTreeIdentifierDTO.setEntityType(entityType);
    commentTreeIdentifierDTO.setEntityId(entityId);
    commentTreeIdentifierDTO.setWorkflow(workflow);

    return commentService.getComments(commentTreeIdentifierDTO);
  }

  @GetMapping("/v1/multipleWorkflows")
  public List<MultipleWorkflowsCommentResponseDTO> getCommentsForMultipleWorkflows(
      @RequestParam(name = "entityType") String entityType,
      @RequestParam(name = "entityId") String entityId,
      @RequestParam(name = "workflow") List<String> workflows) {
    return commentService.getComments(entityType, entityId, workflows);
  }

  @DeleteMapping("/v1/delete/{commentId}")
  public Comment deleteComment(
      @PathVariable String commentId,
      @RequestParam(name = "entityType") String entityType,
      @RequestParam(name = "entityId") String entityId,
      @RequestParam(name = "workflow") String workflow) {

    CommentTreeIdentifierDTO commentTreeIdentifierDTO = new CommentTreeIdentifierDTO();
    commentTreeIdentifierDTO.setEntityType(entityType);
    commentTreeIdentifierDTO.setEntityId(entityId);
    commentTreeIdentifierDTO.setWorkflow(workflow);

    return commentService.deleteCommentById(commentId, commentTreeIdentifierDTO);
  }

  @PostMapping("/v1/setStatusToResolved")
  public CommentTree setCommentTreeStatusToResolved(
      @RequestParam(name = "entityType") String entityType,
      @RequestParam(name = "entityId") String entityId,
      @RequestParam(name = "workflow") String workflow) {

    CommentTreeIdentifierDTO commentTreeIdentifierDTO = new CommentTreeIdentifierDTO();
    commentTreeIdentifierDTO.setEntityType(entityType);
    commentTreeIdentifierDTO.setEntityId(entityId);
    commentTreeIdentifierDTO.setWorkflow(workflow);
    return commentTreeService.setCommentTreeStatusToResolved(commentTreeIdentifierDTO);
  }

  @PostMapping("/v1/resolve/{commentId}")
  public Comment resolveComment(@PathVariable String commentId) {
    return commentService.resolveComment(commentId);
  }

  @GetMapping("/health")
  public String healthCheck() {
    return Constants.SUCCESS_STRING;
  }

  @PostMapping("/v1/like")
  public ResponseEntity likeComment(@RequestBody Map<String, Object> likePayload) {
    ApiResponse response = commentService.likeComment(likePayload);
    return new ResponseEntity<>(response, response.getResponseCode());
  }

  @GetMapping("/v1/like/read/{commentId}/{userId}")
  public ResponseEntity getCommentLike(@RequestParam String commentId,
      @RequestParam String userId) {
    ApiResponse response = commentService.getCommentLike(commentId, userId);
    return new ResponseEntity<>(response, response.getResponseCode());
  }
}
