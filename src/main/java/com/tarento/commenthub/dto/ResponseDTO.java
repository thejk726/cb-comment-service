package com.tarento.commenthub.dto;

import com.tarento.commenthub.entity.Comment;
import com.tarento.commenthub.entity.CommentTree;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResponseDTO {

    private CommentTree commentTree;

    private Comment comment;
}
