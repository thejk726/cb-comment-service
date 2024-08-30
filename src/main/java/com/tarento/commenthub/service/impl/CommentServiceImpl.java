package com.tarento.commenthub.service.impl;

import static com.tarento.commenthub.constant.Constants.COMMENT_KEY;
import static com.tarento.commenthub.utility.CommentsUtility.containsNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.uuid.Generators;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;
import com.tarento.commenthub.constant.Constants;
import com.tarento.commenthub.dto.CommentTreeIdentifierDTO;
import com.tarento.commenthub.dto.MultipleWorkflowsCommentResponseDTO;
import com.tarento.commenthub.dto.CommentsResoponseDTO;
import com.tarento.commenthub.dto.ResponseDTO;
import com.tarento.commenthub.entity.Comment;
import com.tarento.commenthub.entity.CommentTree;
import com.tarento.commenthub.exception.CommentException;
import com.tarento.commenthub.repository.CommentRepository;
import com.tarento.commenthub.service.CommentService;
import com.tarento.commenthub.service.CommentTreeService;
import com.tarento.commenthub.utility.Status;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CommentServiceImpl implements CommentService {

  @Autowired
  private CommentRepository commentRepository;

  @Autowired
  private CommentTreeService commentTreeService;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private RedisTemplate redisTemplate;

  @Value("${redis.ttl}")
  private long redisTtl;

  @Override
  public ResponseDTO addFirstCommentToCreateTree(JsonNode payload) {
    validatePayload(Constants.ADD_FIRST_COMMENT_PAYLOAD_VALIDATION_FILE, payload);
    Comment comment = getPersistedComment(payload);

    ((ObjectNode) payload).put(Constants.COMMENT_ID, comment.getCommentId());
    CommentTree commentTree = commentTreeService.createCommentTree(payload);

    ResponseDTO responseDTO = new ResponseDTO();
    responseDTO.setComment(comment);
    responseDTO.setCommentTree(commentTree);
    return responseDTO;
  }

  @Override
  public ResponseDTO addNewCommentToTree(JsonNode payload) {
    validatePayload(Constants.ADD_NEW_COMMENT_PAYLOAD_VALIDATION_FILE, payload);
    Comment comment = getPersistedComment(payload);
    ((ObjectNode) payload).put(Constants.COMMENT_ID, comment.getCommentId());
    CommentTree commentTree = commentTreeService.updateCommentTree(payload);

    ResponseDTO responseDTO = new ResponseDTO();
    responseDTO.setComment(comment);
    responseDTO.setCommentTree(commentTree);
    return responseDTO;
  }

  @Override
  public ResponseDTO updateExistingComment(JsonNode paylaod) {
    validatePayload(Constants.UPDATE_EXISTING_COMMENT_VALIDATION_FILE, paylaod);
    if (paylaod.get(Constants.COMMENT_ID) == null
        || paylaod.get(Constants.COMMENT_ID).asText().isEmpty()) {
      throw new CommentException(
          Constants.ERROR, "To update an existing comment, please provide a valid commentId.");
    }

    log.info("commentId: " + paylaod.get(Constants.COMMENT_ID).asText());

    Optional<Comment> optComment =
        commentRepository.findById(paylaod.get(Constants.COMMENT_ID).asText());

    if (!optComment.isPresent()
        || !optComment.get().getStatus().equalsIgnoreCase(Status.ACTIVE.name())) {
      throw new CommentException(
          Constants.ERROR, "The requested comment was not found or has been deleted.");
    }

    Comment commentToBeUpdated = optComment.get();
    commentToBeUpdated.setCommentData(paylaod.get(Constants.COMMENT_DATA));

    Timestamp currentTime = new Timestamp(System.currentTimeMillis());
    commentToBeUpdated.setLastUpdatedDate(currentTime);
    Comment updatedComment = commentRepository.save(commentToBeUpdated);
    redisTemplate.opsForValue()
        .set(COMMENT_KEY + commentToBeUpdated.getCommentId(), updatedComment, redisTtl,
            TimeUnit.SECONDS);
    CommentTree commentTree =
        commentTreeService.getCommentTreeById(paylaod.get(Constants.COMMENT_TREE_ID).asText());
    ResponseDTO responseDTO = new ResponseDTO();
    responseDTO.setComment(updatedComment);
    responseDTO.setCommentTree(commentTree);

    return responseDTO;
  }

  @Override
  public CommentsResoponseDTO getComments(CommentTreeIdentifierDTO commentTreeIdentifierDTO) {
    CommentTree commentTree = commentTreeService.getCommentTree(commentTreeIdentifierDTO);
    JsonNode childNodes = commentTree.getCommentTreeData().get(Constants.CHILD_NODES);
    List<String> childNodeList = objectMapper.convertValue(childNodes, List.class);
    log.info("CommentServiceImpl::getComments::fetch comments from redis");
    List<Comment> comments = redisTemplate.opsForValue().multiGet(getKeys(childNodeList));
    if (containsNull(comments)) {
      log.info("CommentServiceImpl::getComments::fetch Comments from postgres");
      // Fetch from db and add fetched comments into redis
      comments = commentRepository.findByCommentIdInAndStatus(childNodeList,
          Status.ACTIVE.name().toLowerCase());
      comments.stream().
          forEach(comment ->
              redisTemplate.opsForValue()
                  .set(COMMENT_KEY + comment.getCommentId(), comment, redisTtl, TimeUnit.SECONDS));
    }
    CommentsResoponseDTO commentsResoponseDTO = new CommentsResoponseDTO();
    commentsResoponseDTO.setComments(comments);
    commentsResoponseDTO.setCommentTree(commentTree);
    Optional.ofNullable(comments)
        .ifPresent(commentsList -> commentsResoponseDTO.setCommentCount(commentsList.size()));
    return commentsResoponseDTO;
  }

  @Override
  public List<MultipleWorkflowsCommentResponseDTO> getComments(String entityType, String entityId,
      List<String> workflowList) {

    List<CommentTree> commentTreeList = commentTreeService.getAllCommentTreeForMultipleWorkflows(entityType,
        entityId,
        workflowList);
    if(commentTreeList.isEmpty()){
      throw new CommentException(Constants.ERROR, "Comment Tree not found", HttpStatus.OK.value());
    }
    return commentTreeList.stream()
        .map(commentTree -> {
          JsonNode childNodes = commentTree.getCommentTreeData().get(Constants.CHILD_NODES);
          List<String> childNodeList = objectMapper.convertValue(childNodes, List.class);
          log.info("CommentServiceImpl::getComments::fetch comments from redis");
          List<Comment> comments = redisTemplate.opsForValue().multiGet(getKeys(childNodeList));
          if (containsNull(comments)) {
            log.info("CommentServiceImpl::getComments::fetch Comments from postgres");
            // Fetch from db and add fetched comments into redis
            comments = commentRepository.findByCommentIdInAndStatus(childNodeList,
                Status.ACTIVE.name().toLowerCase());
            comments.forEach(comment ->
                redisTemplate.opsForValue()
                    .set(COMMENT_KEY + comment.getCommentId(), comment, redisTtl, TimeUnit.SECONDS)
            );
          }
          MultipleWorkflowsCommentResponseDTO multipleWorkflowsCommentResponseDTO = new MultipleWorkflowsCommentResponseDTO();
          multipleWorkflowsCommentResponseDTO.setCommentTree(commentTree);
          Optional.ofNullable(comments)
              .ifPresent(commentsList -> multipleWorkflowsCommentResponseDTO.setCommentCount(
                  commentsList.size()));
          multipleWorkflowsCommentResponseDTO.setComments(comments);
          return multipleWorkflowsCommentResponseDTO;
        })
        .collect(Collectors.toList());
  }

  @Override
  public Comment deleteCommentById(
      String commentId, CommentTreeIdentifierDTO commentTreeIdentifierDTO) {
    log.info("CommentServiceImpl::deleteCommentById: Deleting comment with ID: {}", commentId);
    Optional<Comment> fetchedComment = commentRepository.findById(commentId);
    if (!fetchedComment.isPresent()) {
      throw new CommentException(Constants.ERROR, "No such comment found");
    }
    Comment comment = fetchedComment.get();
    if (!comment.getStatus().equalsIgnoreCase(Status.ACTIVE.name())) {
      throw new CommentException(
          Constants.ERROR, "You are trying to delete an already deleted comment");
    }
    comment.setStatus(Status.INACTIVE.name().toLowerCase());
    comment = commentRepository.save(comment);
    redisTemplate.opsForValue().getOperations().delete(COMMENT_KEY + commentId);
    commentTreeService.updateCommentTreeForDeletedComment(commentId, commentTreeIdentifierDTO);
    return comment;
  }

  private String generateCommentId() {
    UUID uuid = Generators.timeBasedGenerator().generate();
    return uuid.toString();
  }

  public Comment getPersistedComment(JsonNode commentPayload) {
    Comment comment = new Comment();
    String commentId = generateCommentId();
    comment.setCommentId(commentId);
    comment.setCommentData(commentPayload.get(Constants.COMMENT_DATA));
    // Set Status default value 'active' for new comment
    comment.setStatus(Status.ACTIVE.name().toLowerCase());
    ObjectNode objectNode = (ObjectNode) comment.getCommentData();
    // Set commentResolved default value 'false' for new comment
    objectNode.put(Constants.COMMENT_RESOLVED, Constants.FALSE);
    Timestamp currentTime = new Timestamp(System.currentTimeMillis());
    comment.setCreatedDate(currentTime);
    comment.setLastUpdatedDate(currentTime);
    comment = commentRepository.save(comment);
    redisTemplate.opsForValue()
        .set(COMMENT_KEY + comment.getCommentId(), comment, redisTtl, TimeUnit.SECONDS);
    return comment;
  }

  public void validatePayload(String fileName, JsonNode payload) {
    try {
      JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance();
      InputStream schemaStream = schemaFactory.getClass().getResourceAsStream(fileName);
      JsonSchema schema = schemaFactory.getSchema(schemaStream);

      Set<ValidationMessage> validationMessages = schema.validate(payload);
      if (!validationMessages.isEmpty()) {
        StringBuilder errorMessage = new StringBuilder("Validation error(s): \n");
        for (ValidationMessage message : validationMessages) {
          errorMessage.append(message.getMessage()).append("\n");
        }
        throw new CommentException(Constants.ERROR, errorMessage.toString());
      }
    } catch (Exception e) {
      throw new CommentException(Constants.ERROR, "Failed to validate payload: " + e.getMessage());
    }
  }

  private List<String> getKeys(List<String> childNodeList) {
    return childNodeList.stream().map(id -> COMMENT_KEY + id)
        .collect(Collectors.toList());
  }

  @Override
  public Comment resolveComment(String commentId) {
    log.info("CommentServiceImpl::resolveComment: Resolving comment with ID: {}", commentId);
    Optional<Comment> fetchedComment = commentRepository.findById(commentId);
    if (!fetchedComment.isPresent()) {
      throw new CommentException(Constants.ERROR, "No such comment found");
    }
    Comment comment = fetchedComment.get();
    ObjectNode objectNode = (ObjectNode) comment.getCommentData();
    objectNode.put(Constants.COMMENT_RESOLVED, Constants.TRUE);
    Timestamp currentTime = new Timestamp(System.currentTimeMillis());
    comment.setLastUpdatedDate(currentTime);
    comment = commentRepository.save(comment);
    redisTemplate.opsForValue()
        .set(COMMENT_KEY + commentId, comment, redisTtl,
            TimeUnit.SECONDS);
    return comment;
  }

}