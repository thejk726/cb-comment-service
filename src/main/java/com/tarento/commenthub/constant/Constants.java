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
  public static final String KEYSPACE_SUNBIRD = "sunbird";
  public static final String CORE_CONNECTIONS_PER_HOST_FOR_LOCAL = "coreConnectionsPerHostForLocal";
  public static final String CORE_CONNECTIONS_PER_HOST_FOR_REMOTE = "coreConnectionsPerHostForRemote";
  public static final String MAX_CONNECTIONS_PER_HOST_FOR_LOCAL = "maxConnectionsPerHostForLocal";
  public static final String MAX_CONNECTIONS_PER_HOST_FOR_REMOTE = "maxConnectionsPerHostForRemote";
  public static final String MAX_REQUEST_PER_CONNECTION = "maxRequestsPerConnection";
  public static final String HEARTBEAT_INTERVAL = "heartbeatIntervalSeconds";
  public static final String POOL_TIMEOUT = "poolTimeoutMillis";
  public static final String CASSANDRA_CONFIG_HOST = "cassandra.config.host";
  public static final String SUNBIRD_CASSANDRA_CONSISTENCY_LEVEL = "LOCAL_QUORUM";
  public static final String EXCEPTION_MSG_FETCH = "Exception occurred while fetching record from ";
  public static final String INSERT_INTO = "INSERT INTO ";
  public static final String DOT = ".";
  public static final String OPEN_BRACE = "(";
  public static final String VALUES_WITH_BRACE = ") VALUES (";
  public static final String QUE_MARK = "?";
  public static final String COMMA = ",";
  public static final String CLOSING_BRACE = ");";
  public static final String INTEREST_ID = "interest_id";
  public static final String RESPONSE = "response";
  public static final String SUCCESS = "success";
  public static final String FAILED = "Failed";
  public static final String ERROR_MESSAGE = "errmsg";
  public static final String USERID = "userId";
  public static final String FLAG = "flag";
  public static final String LIKE = "like";
  public static final String DISLIKE = "dislike";
  public static final String DATA = "data";
  public static final String ID = "id";
  public static final String USER_ID = "userId";
  public static final String TABLE_USER = "user";
  public static final String FIRST_NAME = "firstname";
  public static final String PROFILE_DETAILS = "profiledetails";
  public static final String USER_NAME = "userName";
  public static final String PROFILE_IMG = "profileImageUrl";
  public static final String CREATED_DATE = "createdDate";
  public static final String OFFSET = "offset";
  public static final String LIMIT = "limit";
  public static final String X_AUTH_TOKEN = "x-authenticated-user-token";
  public static final String SSO_URL = "sso.url";
  public static final String SSO_REALM = "sso.realm";
  public static final String DOT_SEPARATOR = ".";
  public static final String UNAUTHORIZED_USER = "Unauthorized";
  public static final String SUB = "sub";
  public static final String SHA_256_WITH_RSA = "SHA256withRSA";
  public static final String ACCESS_TOKEN_PUBLICKEY_BASEPATH = "accesstoken.publickey.basepath";
  public static final String INVALID_USER = "Invalid user";
  public static final String NOT_FOUND = "Not found";
  public static final String NOT_ACTIVE_STATUS = "Only active coments can be reported";
  public static final String REPORTED_BY = "reportedBy";
  public static final String NOT_SUSPENDED_STATUS = "Only reported coments can be deleted by admin";
  public static final String DELETED_BY = "deletedBy";
  public static final String TAGGED_USERS = "taggedUsers";
  public static final String USER_PREFIX = "user:" ;
  public static final String USER_ID_KEY = "user_id";
  public static final String FIRST_NAME_KEY = "first_name";
  public static final String PROFILE_IMG_KEY = "user_profile_img_url";
  public static final String DESIGNATION_KEY = "designation";
  public static final String DEPARTMENT_KEY = "departmentName";
  public static final String EMPLOYMENT_DETAILS = "employmentDetails";
  public static final String DEPARTMENT = "department";
  public static final String EXCEPTION_MSG_DELETE = "Exception occurred while deleting record from ";
  public static final String COMMENT_LIKE_TABLE = "comment_likes";
  public static final String REPORTED_REASON = "reportedDueTo";
  public static final String OTHER_REASON = "otherReasons";
  public static final String COURSEID = "courseId";
  public static final String EMPTY_COURSEID = "CourseId is missing";
  public static final String OK = "OK";
  public static final String RESPONSE_CODE = "responseCode";
  public static final String RESULT = "result";
  public static final String CONTENT = "content";
  public static final String FETCH_RESULT_CONSTANT = ".fetchResult:";
  public static final String URI_CONSTANT = "URI: ";
}