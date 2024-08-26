package com.tarento.commenthub.constant;

public class Constants {

  private Constants() {

  }

  public static final String ENTITY_ID = "entityId";

  public static final String ENTITY_TYPE = "entityType";

  public static final String WORKFLOW = "workflow";

  public static final String COMMENT_ID = "commentId";

  public static final String COMMENT_TREE_ID = "commentTreeId";

  public static final String COMMENT_KEY = "comment_";

  public static final String COMMENTS = "comments";

  public static final String CHILD_NODES = "childNodes";

  public static final String ERROR = "Not Found";

  public static final String HIERARCHY_PATH = "hierarchyPath";

  public static final String COMMENT_DATA = "commentData";

  public static final String COMMENT_TREE_DATA = "commentTreeData";

  public static final String CHILDREN = "children";

  public static final String FIRST_LEVEL_NODES = "firstLevelNodes";

  public static final String COMMENT_SOURCE = "commentSource";
  public static final String FILE = "file";
  public static final String SUCCESS_STRING = "success";

  public static final String DUPLICATE_TREE_ERROR = "DUPLICATE TREE CREATION ERROR";

  public static final String DUPLICATE_TREE_ERROR_MESSAGE =
      "Failed to create a new comment tree. " +
          "A comment tree with the same 'entityType,' 'entityId,' and 'workflow' already exists.";

  public static final String WRONG_HIERARCHY_PATH_ERROR = "WRONG HIERARCHY PATH ERROR";
  public static final String ADD_FIRST_COMMENT_PAYLOAD_VALIDATION_FILE = "/payloadValidation/firstComment.json";
  public static final String ADD_NEW_COMMENT_PAYLOAD_VALIDATION_FILE = "/payloadValidation/newComment.json";
  public static final String UPDATE_EXISTING_COMMENT_VALIDATION_FILE = "/payloadValidation/updateComment.json";
  public static final String RESOLVED = "resolved";

  public static final String COMMENT_RESOLVED = "commentResolved";

  public static final String TRUE = "true";

  public static final String FALSE = "false";



}