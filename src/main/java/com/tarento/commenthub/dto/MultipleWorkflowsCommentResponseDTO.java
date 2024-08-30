package com.tarento.commenthub.dto;

import com.tarento.commenthub.entity.Comment;
import com.tarento.commenthub.entity.CommentTree;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MultipleWorkflowsCommentResponseDTO {

  private CommentTree commentTree;
  private List<Comment> comments;
  private int commentCount;
}
