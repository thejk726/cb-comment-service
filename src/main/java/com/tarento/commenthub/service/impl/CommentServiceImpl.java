package com.tarento.commenthub.service.impl;

import static com.tarento.commenthub.constant.Constants.COMMENT_KEY;
import static com.tarento.commenthub.utility.CommentsUtility.containsNull;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.uuid.Generators;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;
import com.tarento.commenthub.authentication.util.AccessTokenValidator;
import com.tarento.commenthub.authentication.util.FetchUserDetails;
import com.tarento.commenthub.constant.Constants;
import com.tarento.commenthub.dto.CommentTreeIdentifierDTO;
import com.tarento.commenthub.dto.CommentsResoponseDTO;
import com.tarento.commenthub.dto.ResponseDTO;
import com.tarento.commenthub.dto.SearchCriteria;
import com.tarento.commenthub.dto.UserCourseCommentsId;
import com.tarento.commenthub.entity.Comment;
import com.tarento.commenthub.entity.CommentTree;
import com.tarento.commenthub.entity.UserCourseCommentLike;
import com.tarento.commenthub.exception.CommentException;
import com.tarento.commenthub.repository.CommentRepository;
import com.tarento.commenthub.repository.CommentTreeRepository;
import com.tarento.commenthub.repository.UserCommentLikeRepository;
import com.tarento.commenthub.service.CommentService;
import com.tarento.commenthub.service.CommentTreeService;
import com.tarento.commenthub.transactional.cassandrautils.CassandraOperation;
import com.tarento.commenthub.transactional.utils.ApiResponse;
import com.tarento.commenthub.utility.Status;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

  @Autowired
  private CassandraOperation cassandraOperation;

  @Value("${jwt.secret.key}")
  private String jwtSecretKey;

  @Value("${default.page.size}")
  private int defaultLimit;

  @Value("${default.offset}")
  private int defaultOffset;

  @Autowired
  private CommentTreeRepository commentTreeRepository;

  @Autowired
  private AccessTokenValidator accessTokenValidator;

  @Autowired
  private FetchUserDetails fetchUser;

  @Autowired
  private UserCommentLikeRepository userCommentLikeRepository;

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

    deleteRedisKey(String.valueOf(payload.get(Constants.COMMENT_TREE_ID)));
    ResponseDTO responseDTO = new ResponseDTO(commentTree, comment);
    return responseDTO;
  }

  private void deleteAndUpdateTheRedisKey(String commentTreeId, Map<String, Object> resultMap) {
    String token = generateRedisJwtTokenKey(commentTreeId
        , defaultOffset, defaultLimit);
    deleteRedisKey(commentTreeId);
    redisTemplate.opsForValue()
        .set(token,
            resultMap, redisTtl,
            TimeUnit.SECONDS);
  }

  private void deleteRedisKey(String commentTreeId) {
    String token = generateRedisJwtTokenKey(commentTreeId
        , defaultOffset, defaultLimit);
    redisTemplate.delete(token);
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
    if (!paylaod.get(Constants.COMMENT_DATA).get(Constants.COMMENT_SOURCE).get(Constants.USER_ID)
        .asText()
        .equalsIgnoreCase(optComment.get().getCommentData().get(Constants.COMMENT_SOURCE)
            .get(Constants.USER_ID).asText())) {
      throw new CommentException(
          Constants.ERROR, "No access to edit the comment");

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
    ResponseDTO responseDTO = new ResponseDTO(commentTree, updatedComment);
    deleteRedisKey(String.valueOf(paylaod.get(Constants.COMMENT_TREE_ID)));
    return responseDTO;
  }

  @Override
  public CommentsResoponseDTO getComments(CommentTreeIdentifierDTO commentTreeIdentifierDTO) {
    CommentTree commentTree = commentTreeService.getCommentTree(commentTreeIdentifierDTO);
    JsonNode childNodes = commentTree.getCommentTreeData().get(Constants.CHILD_NODES);
    //check whether this is present in redis or not based on the key cmmentTreeId
    List<String> childNodeList = objectMapper.convertValue(childNodes, List.class);
    log.info("CommentServiceImpl::getComments::fetch comments from redis");
    //call it from postgres remove redis reading
    List<Comment> comments = null;
    if (containsNull(comments)) {
      log.info("CommentServiceImpl::getComments::fetch Comments from postgres");
      // Fetch from db and add fetched comments into redis
      comments = commentRepository.findByCommentIdInAndStatus(childNodeList,
          Status.ACTIVE.name().toLowerCase());
      List<Map<String, Object>> userList = new ArrayList<>();
      userList = fetchUsersByCommentData(comments);
      Set<String> uniqueTaggedUserIds = new HashSet<>();
      Set<String> uniqueTaggedUserIdWithoutPrefixs = new HashSet<>();
// Iterate through each comment to extract tagged users
      comments.forEach(comment -> {
        JsonNode taggedUsersNode = comment.getCommentData().get(Constants.TAGGED_USERS);

        // Check if taggedUsersNode exists and is an array
        if (taggedUsersNode != null && taggedUsersNode.isArray()) {
          // Add each tagged user ID to the set to maintain uniqueness
          taggedUsersNode.forEach(taggedUser -> {
            uniqueTaggedUserIds.add(Constants.USER_PREFIX + taggedUser.asText());

            // Add the tagged user ID without the prefix to uniqueTaggedUserIdWithoutPrefixs
            uniqueTaggedUserIdWithoutPrefixs.add(taggedUser.asText());
          });
        }
      });
      List<String> taggedUserList = new ArrayList<>(uniqueTaggedUserIds);
      List<String> taggedUserListWithoutPrefix = new ArrayList<>(uniqueTaggedUserIdWithoutPrefixs);
      List<Object> taggedUsers = fetchUser.fetchDataForKeys(taggedUserList);
      if (taggedUsers == null || taggedUsers.isEmpty()) {
        // Handle the case where taggedUsers is empty or null
        taggedUsers = fetchUser.fetchUserFromprimary(taggedUserListWithoutPrefix);
      }
      // Collect unique IDs
      CommentsResoponseDTO commentsResoponseDTO = new CommentsResoponseDTO(commentTree, comments,
          userList, taggedUsers);
      //store it in redis with commentTreeId as key:: TO DO
      Optional.ofNullable(comments)
          .ifPresent(commentsList -> commentsResoponseDTO.setCommentCount(commentsList.size()));
      return commentsResoponseDTO;
    }
    return null;
  }


  @Override
  public Comment deleteCommentById(
      String commentId, CommentTreeIdentifierDTO commentTreeIdentifierDTO, String token) {
    log.info("CommentServiceImpl::deleteCommentById: Deleting comment with ID: {}", commentId);
    String userId = accessTokenValidator.verifyUserToken(token);
    if (StringUtils.isBlank(userId) || userId.equalsIgnoreCase(Constants.UNAUTHORIZED_USER)) {
      throw new CommentException(Constants.ERROR, "Not a valid user");
    }
    Optional<Comment> fetchedComment = commentRepository.findById(commentId);
    if (!fetchedComment.isPresent()) {
      throw new CommentException(Constants.ERROR, "No such comment found");
    }
    Comment comment = fetchedComment.get();
    if (!userId
        .equalsIgnoreCase(comment.getCommentData().get(Constants.COMMENT_SOURCE)
            .get(Constants.USER_ID).asText())) {
      throw new CommentException(
          Constants.ERROR, "No access to edit the comment");

    }
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

  private Comment getPersistedComment(JsonNode commentPayload) {
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


  //need for refactoring later
  @Override
  public ApiResponse likeComment(Map<String, Object> likePayload) {
    ApiResponse response = new ApiResponse();
    response.setResponseCode(HttpStatus.OK);
    String error = validateLikeCommentPayload(likePayload);
    if (StringUtils.isNotBlank(error)) {
      response.setResponseCode(HttpStatus.BAD_REQUEST);
      response.getParams().setErr(error);
      return response;
    }
    Optional<Comment> optComment = commentRepository.findById(
        (String) likePayload.get(Constants.COMMENT_ID));
    if (!optComment.isPresent() && optComment.get().getCommentData().isEmpty()) {
      response.setResponseCode(HttpStatus.BAD_REQUEST);
      response.getParams().setErr(error);
      return response;
    }
    try {
      String commentId = (String) likePayload.get(Constants.COMMENT_ID);
      String userId = (String) likePayload.get(Constants.USERID);
      String courseId = (String) likePayload.get(Constants.COURSEID);
      UserCourseCommentsId id = new UserCourseCommentsId(userId, courseId);
      Optional<UserCourseCommentLike> records = userCommentLikeRepository.findById(id);
      JsonNode commentData = optComment.get().getCommentData();
      if (records.isPresent() && !records.get().getCommentIds().isEmpty()) {
        UserCourseCommentLike existingRecord = records.get();
        List<String> alreadyLikedComments = existingRecord.getCommentIds();

        if (alreadyLikedComments.contains(commentId)) {
          // Remove the commentId if it is already liked
          alreadyLikedComments.remove(commentId);
          // Update the record in the database
          existingRecord.setCommentIds(alreadyLikedComments);
          userCommentLikeRepository.save(existingRecord);
          if (commentData.has((String) likePayload.get(Constants.FLAG))
              && commentData.get((String) likePayload.get(Constants.FLAG)).asLong() > 0) {
            Long decrementCount = commentData.get((String) likePayload.get(Constants.FLAG))
                .asLong();
            ((ObjectNode) commentData).put(Constants.LIKE, decrementCount - 1);
          } else {
            ((ObjectNode) commentData).put(Constants.LIKE, 1);
          }
        } else {
          // Add the commentId if it is not already liked
          alreadyLikedComments.add(commentId); // Add the commentId (already String)
          // Update the record in the database
          existingRecord.setCommentIds(alreadyLikedComments);
          userCommentLikeRepository.save(existingRecord);
          if (commentData.has((String) likePayload.get(Constants.FLAG))
              && commentData.get((String) likePayload.get(Constants.FLAG)).asLong() > 0) {
            Long incrementCount = commentData.get((String) likePayload.get(Constants.FLAG))
                .asLong();
            ((ObjectNode) commentData).put(Constants.LIKE, incrementCount + 1);
          } else {
            ((ObjectNode) commentData).put(Constants.LIKE, 1);
          }
        }
      } else {
        // If no existing record found, create a new one with the commentId
        UserCourseCommentLike newRecord = new UserCourseCommentLike();
        newRecord.setId(id);
        // Ensure that commentId is wrapped in a List<UUID>
        List<String> commentIds = new ArrayList<>();
        commentIds.add(commentId);  // Add the commentId directly (as String)
        newRecord.setCommentIds(commentIds);
        userCommentLikeRepository.save(newRecord);
        if (commentData.has((String) likePayload.get(Constants.FLAG))
            && commentData.get((String) likePayload.get(Constants.FLAG)).asLong() > 0) {
          Long incrementCount = commentData.get((String) likePayload.get(Constants.FLAG))
              .asLong();
          ((ObjectNode) commentData).put(Constants.LIKE, incrementCount + 1);
        } else {
          ((ObjectNode) commentData).put(Constants.LIKE, 1);
        }
      }
      Comment commentToBeUpdated = optComment.get();
      commentToBeUpdated.setCommentData(commentData);
      commentRepository.save(commentToBeUpdated);
      return response;
    } catch (Exception e) {
      log.error("error occured while liking a comment::" + String.valueOf(e));
      throw new RuntimeException(e);
    }
  }

    //need to write a method to fetch all the likes and dilikes of a  coment
  @Override
  public ApiResponse getCommentLike(String commentId, String userId) {
    ApiResponse response = new ApiResponse();
    response.setResponseCode(HttpStatus.OK);
    String error = validatePayloadForCommAndUser(commentId, userId);
    if (StringUtils.isNotBlank(error)) {
      response.setResponseCode(HttpStatus.BAD_REQUEST);
      response.getParams().setErr(error);
      return response;
    }

    Map<String, Object> propertyMap = new HashMap<>();
    propertyMap.put(Constants.COMMENT_ID, commentId);
    propertyMap.put(Constants.USERID, userId);
    List<Map<String, Object>> records = cassandraOperation.getRecordsByPropertiesWithoutFiltering(
        Constants.KEYSPACE_SUNBIRD, "comment_likes", propertyMap,
        Collections.singletonList("flag"), null);
    if (!records.isEmpty()) {
      response.setResult(records.get(0));
      return response;
    } else {
      response.getParams().setErr("This user not liked this comment");
      return response;
    }
  }

  @Override
  public ApiResponse paginatedComment(SearchCriteria searchCriteria) {
    String error = validateSearchPayload(searchCriteria);
    ApiResponse response = new ApiResponse();
    response.setResponseCode(HttpStatus.OK);
    if (StringUtils.isNotBlank(error)) {
      return returnErrorMsg(error, HttpStatus.BAD_REQUEST, response);
    }
    String commentTreeId = "";
    if (searchCriteria.getCommentTreeId().isEmpty()) {
      CommentTreeIdentifierDTO commentTreeIdentifierDTO = new CommentTreeIdentifierDTO(
          searchCriteria.getEntityType(), searchCriteria.getEntityId(),
          searchCriteria.getWorkflow());
      commentTreeId = generateJwtTokenKey(commentTreeIdentifierDTO);
    } else {
      commentTreeId = searchCriteria.getCommentTreeId();
    }
    Optional<CommentTree> commentTree = commentTreeRepository.findById(commentTreeId);
    if (!commentTree.isPresent()) {
      response.getParams().setErr("CommentTree Not found");
      return returnErrorMsg("CommentTree Not found", HttpStatus.NOT_FOUND, response);
    }
    JsonNode childNodes = commentTree.get().getCommentTreeData().get(Constants.FIRST_LEVEL_NODES);
    List<String> childNodeList = objectMapper.convertValue(childNodes, List.class);
    int limit = (searchCriteria.getLimit() != null) ? searchCriteria.getLimit() : defaultLimit;
    int offset =
        (searchCriteria.getOffset() != null) ? searchCriteria.getOffset() : defaultOffset;
    Map<String, Object> resultMap = new HashMap<>();
    if (!searchCriteria.isOverrideCache()) {
      resultMap = (Map<String, Object>) redisTemplate.opsForValue()
          .get(generateRedisJwtTokenKey(commentTreeId, offset, limit));
    } else {
      resultMap = fetchCommentFromPrimary(offset, limit, childNodeList, commentTree.get(), searchCriteria.isEnrichedUser());
      redisTemplate.opsForValue()
          .set(generateRedisJwtTokenKey(commentTreeId, offset, limit), resultMap, redisTtl,
              TimeUnit.SECONDS);
      response.setResult(resultMap);
      return response;
    }
    if (MapUtils.isEmpty(resultMap)) {
      log.info("CommentServiceImpl::getComments::fetch Comments from postgres");
      resultMap = fetchCommentFromPrimary(offset, limit, childNodeList, commentTree.get(), searchCriteria.isEnrichedUser());
      redisTemplate.opsForValue()
          .set(generateRedisJwtTokenKey(commentTreeId, offset, limit), resultMap, redisTtl,
              TimeUnit.SECONDS);
      response.setResult(resultMap);
      return response;
    } else {
      log.info("CommentServiceImpl::getComments::fetch comments from redis");
      response.setResult(resultMap);
      return response;
    }
  }

  private Map<String, Object> fetchCommentFromPrimary(int offset, int limit,
      List<String> childNodeList, CommentTree commentTree, boolean isUserEnriched) {
    Map<String, Object> resultMap = new HashMap<>();
    Pageable pageable = PageRequest.of(offset, limit,
        Sort.by(Sort.Direction.DESC, Constants.CREATED_DATE));
    List<Comment> comments = commentRepository.findByCommentIdIn(childNodeList, pageable)
        .getContent();
    List<Map<String, Object>> userList = new ArrayList<>();
    Set<String> uniqueTaggedUserIds = new HashSet<>();
    Set<String> uniqueTaggedUserIdWithoutPrefixs = new HashSet<>();
// Iterate through each comment to extract tagged users
    comments.forEach(comment -> {
      JsonNode taggedUsersNode = comment.getCommentData().get(Constants.TAGGED_USERS);

      // Check if taggedUsersNode exists and is an array
      if (taggedUsersNode != null && taggedUsersNode.isArray()) {
        // Add each tagged user ID to the set to maintain uniqueness
        taggedUsersNode.forEach(taggedUser -> {
          uniqueTaggedUserIds.add(Constants.USER_PREFIX + taggedUser.asText());

          // Add the tagged user ID without the prefix to uniqueTaggedUserIdWithoutPrefixs
          uniqueTaggedUserIdWithoutPrefixs.add(taggedUser.asText());
        });
      }
    });
    List<String> taggedUserList = new ArrayList<>(uniqueTaggedUserIds);
    List<String> taggedUserListWithoutPrefix = new ArrayList<>(uniqueTaggedUserIdWithoutPrefixs);
    List<Object> taggedUsers = fetchUser.fetchDataForKeys(taggedUserList);
    if (taggedUsers == null || taggedUsers.isEmpty()) {
      // Handle the case where taggedUsers is empty or null
      taggedUsers = fetchUser.fetchUserFromprimary(taggedUserListWithoutPrefix);
    }
    // Collect unique IDs
    if (isUserEnriched){
      userList = fetchUsersByCommentData(comments);
    }
    CommentsResoponseDTO commentsResoponseDTO = new CommentsResoponseDTO(commentTree,
        comments, userList, taggedUsers);
    Optional.ofNullable(comments)
        .ifPresent(commentsList -> commentsResoponseDTO.setCommentCount(childNodeList.size()));
    resultMap = objectMapper.convertValue(commentsResoponseDTO, Map.class);
    return resultMap;
  }

  @Override
  public ApiResponse listOfComments(List<String> commentIds) {
    ApiResponse response = new ApiResponse();
    response.setResponseCode(HttpStatus.OK);
    if (commentIds.size()==0){
      return returnErrorMsg("Bad rqst", HttpStatus.BAD_REQUEST, response);
    }
    //added sorting
    int offset = defaultOffset;
    int limit = defaultLimit;
    Sort sort = Sort.by(Sort.Direction.DESC, Constants.CREATED_DATE);
    List<Comment> comments = commentRepository.findByCommentIdIn(commentIds, sort);
    List<Map<String, Object>> userList = new ArrayList<>();
    userList = fetchUsersByCommentData(comments);
    Set<String> uniqueTaggedUserIds = new HashSet<>();
    Set<String> uniqueTaggedUserIdWithoutPrefixs = new HashSet<>();
// Iterate through each comment to extract tagged users
    comments.forEach(comment -> {
      JsonNode taggedUsersNode = comment.getCommentData().get(Constants.TAGGED_USERS);

      // Check if taggedUsersNode exists and is an array
      if (taggedUsersNode != null) {
        // Add each tagged user ID to the set to maintain uniqueness
        taggedUsersNode.forEach(taggedUser -> {
          uniqueTaggedUserIds.add(Constants.USER_PREFIX + taggedUser.asText());

          // Add the tagged user ID without the prefix to uniqueTaggedUserIdWithoutPrefixs
          uniqueTaggedUserIdWithoutPrefixs.add(taggedUser.asText());
        });
      }
    });
    List<String> taggedUserList = new ArrayList<>(uniqueTaggedUserIds);
    List<String> taggedUserListWithoutPrefix = new ArrayList<>(uniqueTaggedUserIdWithoutPrefixs);
    List<Object> taggedUsers = fetchUser.fetchDataForKeys(taggedUserList);
    if (taggedUsers == null || taggedUsers.isEmpty()) {
      // Handle the case where taggedUsers is empty or null
      taggedUsers = fetchUser.fetchUserFromprimary(taggedUserListWithoutPrefix);
    }
    CommentsResoponseDTO commentsResoponseDTO = new CommentsResoponseDTO(comments, userList, taggedUsers);
    //store it in redis with commentTreeId as key:: TO DO
    Optional.ofNullable(comments)
        .ifPresent(commentsList -> commentsResoponseDTO.setCommentCount(commentsList.size()));
    Map<String, Object> resultMap = objectMapper.convertValue(commentsResoponseDTO, Map.class);

    response.setResult(resultMap);
    return response;
  }

  @Override
  public ApiResponse reportComment(Map<String, Object> request, String token) {
    log.info("CommentServiceImpl:reportComment::inside the method");
    ApiResponse response = new ApiResponse();
    response.setResponseCode(HttpStatus.OK);
    String userId = accessTokenValidator.verifyUserToken(token);
    if (StringUtils.isBlank(userId) || userId.equalsIgnoreCase(Constants.UNAUTHORIZED_USER)) {
      return returnErrorMsg(Constants.INVALID_USER, HttpStatus.BAD_REQUEST, response);
    }
    String error = validateReportCommentPayload(request);
    if (StringUtils.isNotBlank(error)) {
      return returnErrorMsg(error, HttpStatus.BAD_REQUEST, response);
    }
    Optional<Comment> fetchedComment = commentRepository.findById(
        (String) request.get(Constants.COMMENT_ID));
    if (!fetchedComment.isPresent()) {
      return returnErrorMsg(Constants.NOT_FOUND, HttpStatus.NOT_FOUND, response);
    }
    Comment comment = fetchedComment.get();
    if (!comment.getStatus().equalsIgnoreCase(Status.ACTIVE.name())) {
      return returnErrorMsg(Constants.NOT_ACTIVE_STATUS, HttpStatus.NOT_FOUND, response);
    }
    ObjectNode commentData = (ObjectNode) comment.getCommentData();
    commentData.put(Constants.REPORTED_BY, userId);
    commentData.put(Constants.REPORTED_REASON,
        objectMapper.valueToTree(request.get(Constants.REPORTED_REASON)));
    if (request.containsKey(Constants.REPORTED_REASON) &&
        request.get(Constants.REPORTED_REASON) instanceof List) {
      List<String> reportedReasonList = (List<String>) request.get(Constants.REPORTED_REASON);

      if (reportedReasonList.contains("Others") && request.containsKey(Constants.OTHER_REASON)) {
        String otherReason = (String) request.get(Constants.OTHER_REASON);
        if (!StringUtils.isBlank(otherReason)) {
          commentData.put(Constants.OTHER_REASON, otherReason);
        }
      }
    }
    comment.setStatus(Status.SUSPENDED.name().toLowerCase());
    comment = commentRepository.save(comment);
    response.setResult(objectMapper.convertValue(comment, Map.class));
    return response;
  }

  @Override
  public ApiResponse deleteReportedComments(Map<String, Object> request, String token) {
    log.info("CommentServiceImpl:reportComment::inside the method");
    ApiResponse response = new ApiResponse();
    response.setResponseCode(HttpStatus.OK);
    String userId = accessTokenValidator.verifyUserToken(token);
    if (StringUtils.isBlank(userId) || userId.equalsIgnoreCase(Constants.UNAUTHORIZED_USER)) {
      return returnErrorMsg(Constants.INVALID_USER, HttpStatus.BAD_REQUEST, response);
    }
    String error = validateReportCommentPayload(request);
    if (StringUtils.isNotBlank(error)) {
      return returnErrorMsg(error, HttpStatus.BAD_REQUEST, response);
    }
    Optional<Comment> fetchedComment = commentRepository.findById(
        (String) request.get(Constants.COMMENT_ID));
    if (!fetchedComment.isPresent()) {
      return returnErrorMsg(Constants.NOT_FOUND, HttpStatus.NOT_FOUND, response);
    }
    Comment comment = fetchedComment.get();
    if (!comment.getStatus().equalsIgnoreCase(Status.SUSPENDED.name())) {
      return returnErrorMsg(Constants.NOT_SUSPENDED_STATUS, HttpStatus.NOT_FOUND, response);
    }
    ObjectNode commentData = (ObjectNode) comment.getCommentData();
    commentData.put(Constants.DELETED_BY, userId);
    comment.setStatus(Status.INACTIVE.name().toLowerCase());
    comment = commentRepository.save(comment);
    response.setResult(objectMapper.convertValue(comment, Map.class));
    return response;
  }

  @Override
  public ApiResponse getCommentsLikedByUser(String courseId, String token) {
    log.info("CommentServiceImpl:getCommentsLikedByUser::inside the method");
    ApiResponse response = new ApiResponse();
    response.setResponseCode(HttpStatus.OK);
    String userId = accessTokenValidator.verifyUserToken(token);
    if (StringUtils.isBlank(userId) || userId.equalsIgnoreCase(Constants.UNAUTHORIZED_USER)) {
      return returnErrorMsg(Constants.INVALID_USER, HttpStatus.BAD_REQUEST, response);
    }
    if (StringUtils.isBlank(courseId)) {
      return returnErrorMsg(Constants.EMPTY_COURSEID, HttpStatus.BAD_REQUEST, response);
    }
    UserCourseCommentsId id = new UserCourseCommentsId(userId, courseId);
    Optional<UserCourseCommentLike> records = userCommentLikeRepository.findById(id);
    if (records.isPresent() && !records.get().getCommentIds().isEmpty()) {
      List<String> commentIds = records.get().getCommentIds();
      // Convert List<String> to Map<String, Object>
      Map<String, Object> commentIdsMap = new HashMap<>();
      commentIdsMap.put(Constants.COMMENT_ID, commentIds);
      // Set the result as Map<String, Object>
      response.setResult(commentIdsMap);
    } else {
      // Handle case where commentIds is empty or record is not present
      response.setResult(Collections.emptyMap());
    }
    return response;
  }

  private String validateReportCommentPayload(Map<String, Object> request) {
    StringBuffer str = new StringBuffer();
    List<String> errList = new ArrayList<>();

    if (request.containsKey(Constants.COMMENT_ID) &&
    StringUtils.isBlank((String) request.get(Constants.COMMENT_ID))){
      errList.add(Constants.COMMENT_ID);
    }
    // Check if REPORTED_REASON is a list of strings
    if (request.containsKey(Constants.REPORTED_REASON)) {
      Object reportedReasonObj = request.get(Constants.REPORTED_REASON);
      // Check if REPORTED_REASON is a list of strings
      if (reportedReasonObj instanceof List) {
        List<String> reportedReasonList = (List<String>) reportedReasonObj;
        // Check if the list is empty
        if (reportedReasonList.isEmpty()) {
          errList.add(Constants.REPORTED_REASON);
        } else if (reportedReasonList.contains("Others")) {
          // Check if OTHER_REASON is provided
          if (!request.containsKey(Constants.OTHER_REASON) ||
              StringUtils.isBlank((String) request.get(Constants.OTHER_REASON))) {
            errList.add(Constants.OTHER_REASON);
          }
        }
      } else {
        // If REPORTED_REASON is not a list, add an error
        errList.add(Constants.REPORTED_REASON);
      }
    }
    if (!errList.isEmpty()) {
      str.append("Failed Due To Missing Params - ").append(errList).append(".");
    }
    return str.toString();
  }

  private  ApiResponse returnErrorMsg(String error, HttpStatus type, ApiResponse response){
    response.setResponseCode(type);
    response.getParams().setErr(error);
    return response;
  }

  private String validateSearchPayload(SearchCriteria searchCriteria) {
    StringBuffer str = new StringBuffer();
    List<String> errList = new ArrayList<>();

    if (StringUtils.isBlank(searchCriteria.getCommentTreeId())) {
      if(searchCriteria.getEntityType().isEmpty() && searchCriteria.getEntityType().isEmpty() && searchCriteria.getWorkflow().isEmpty())
      errList.add(Constants.COMMENT_TREE_ID);
    }
    if (!errList.isEmpty()) {
      str.append("Failed Due To Missing Params - ").append(errList).append(".");
    }
    return str.toString();
  }

  private String validatePayloadForCommAndUser(String commentId, String userId) {
    StringBuffer str = new StringBuffer();
    List<String> errList = new ArrayList<>();

    if (StringUtils.isBlank(commentId)) {
      errList.add(Constants.COMMENT_ID);
    }
    if (StringUtils.isBlank(userId)) {
      errList.add(Constants.USERID);
    }
    if (!errList.isEmpty()) {
      str.append("Failed Due To Missing Params - ").append(errList).append(".");
    }
    return str.toString();
  }

  private String validateLikeCommentPayload(Map<String, Object> likePayload) {
    StringBuffer str = new StringBuffer();
    List<String> errList = new ArrayList<>();

    if (StringUtils.isBlank((String) likePayload.get(Constants.COMMENT_ID))) {
      errList.add(Constants.COMMENT_ID);
    }
    if (StringUtils.isBlank((String) likePayload.get(Constants.USERID))) {
      errList.add(Constants.USERID);
    }
    if (StringUtils.isBlank((String) likePayload.get(Constants.COURSEID))) {
      errList.add(Constants.COURSEID);
    }
    String voteType = (String) likePayload.get(Constants.FLAG);
    if (StringUtils.isBlank(voteType)) {
      errList.add(Constants.FLAG);
    } else if (!Constants.LIKE.equalsIgnoreCase(voteType) && !Constants.DISLIKE.equalsIgnoreCase(
        voteType)) {
      errList.add("flag must be either 'like' or 'dislike'");
    }
    if (!errList.isEmpty()) {
      str.append("Failed Due To Missing Params - ").append(errList).append(".");
    }
    return str.toString();
  }

  public String generateJwtTokenKey(CommentTreeIdentifierDTO commentTreeIdentifierDTO) {
    log.info("generating JwtTokenKey");

    if (StringUtils.isAnyBlank(
        commentTreeIdentifierDTO.getEntityId(),
        commentTreeIdentifierDTO.getEntityType(),
        commentTreeIdentifierDTO.getWorkflow())) {
      throw new CommentException(Constants.ERROR,
          "Please provide values for 'entityType', 'entityId', and 'workflow' as all of these fields are mandatory.");
    }

    String jwtToken = JWT.create()
        .withClaim(Constants.ENTITY_ID, commentTreeIdentifierDTO.getEntityId())
        .withClaim(Constants.ENTITY_TYPE, commentTreeIdentifierDTO.getEntityType())
        .withClaim(Constants.WORKFLOW, commentTreeIdentifierDTO.getWorkflow())
        .sign(Algorithm.HMAC256(jwtSecretKey));

    log.info("commentTreeId: {}", jwtToken);
    return jwtToken;
  }

  private List<Map<String, Object>> fetchUsersByCommentData (List<Comment> comments) {
    List<Map<String, Object>> userList = new ArrayList<>();
    List<String> userIds = comments.stream()
        .map(comment -> comment.getCommentData().get(Constants.COMMENT_SOURCE)
            .get(Constants.USER_ID).asText())
        .collect(Collectors.toList());
    Map<String, Object> propertyMap = new HashMap<>();
    propertyMap.put(Constants.ID, userIds);
    List<Map<String, Object>> userInfoList = cassandraOperation.getRecordsByPropertiesWithoutFiltering(
        Constants.KEYSPACE_SUNBIRD, Constants.TABLE_USER, propertyMap,
        Arrays.asList(Constants.PROFILE_DETAILS, Constants.FIRST_NAME, Constants.ID), null);

    userList = userInfoList.stream()
        .map(userInfo -> {
          Map<String, Object> userMap = new HashMap<>();

          // Extract user ID and user name
          String userId = (String) userInfo.get(Constants.ID);
          String userName = (String) userInfo.get(Constants.FIRST_NAME);

          userMap.put(Constants.USER_ID, userId);
          userMap.put(Constants.USER_NAME, userName);

          // Process profile details if present
          String profileDetails = (String) userInfo.get(Constants.PROFILE_DETAILS);
          if (StringUtils.isNotBlank(profileDetails)) {
            try {
              // Convert JSON profile details to a Map
              Map<String, Object> profileDetailsMap = objectMapper.readValue(profileDetails,
                  new TypeReference<HashMap<String, Object>>() {});

              // Check for profile image and add to userMap if available
              if (MapUtils.isNotEmpty(profileDetailsMap) && profileDetailsMap.containsKey(Constants.PROFILE_IMG)) {
                String profileImageUrl = (String) profileDetailsMap.get(Constants.PROFILE_IMG);
                if (StringUtils.isNotEmpty(profileImageUrl)) {
                  userMap.put(Constants.PROFILE_IMG, profileImageUrl);
                }
              }
            } catch (JsonProcessingException e) {
              throw new RuntimeException(e);
            }
          }

          return userMap;
        })
        .collect(Collectors.toList());
    return userList;
  }

  public String generateRedisJwtTokenKey(String commentTreeId, Integer offset, Integer limit) {
      try {
        return JWT.create()
            .withClaim(Constants.COMMENT_TREE_ID, commentTreeId)  // Add commentTreeId
            .withClaim(Constants.OFFSET, offset)                  // Add offset
            .withClaim(Constants.LIMIT, limit)
            .sign(Algorithm.HMAC256(jwtSecretKey));
      }catch (Exception e){
        log.error("Excpetion occured while creating the redis token::", e);
      }
    return "";
  }


}