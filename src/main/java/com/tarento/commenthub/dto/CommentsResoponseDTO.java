package com.tarento.commenthub.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.tarento.commenthub.entity.Comment;
import com.tarento.commenthub.entity.CommentTree;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CommentsResoponseDTO {

  private CommentTree commentTree;

  private List<Comment> comments;

  private List<Map<String, Object>> users;

  private int commentCount;

  public CommentsResoponseDTO(CommentTree commentTree, List<Comment> comments, List<Map<String, Object>> userList) {
    this.commentTree = commentTree;
    this.comments = comments;
    this.users = userList;
  }


  public CommentsResoponseDTO(List<Comment> comments, List<Map<String, Object>> userList) {
    this.comments = comments;
    this.users = userList;
  }
}
