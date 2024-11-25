package com.tarento.commenthub.entity;

import com.tarento.commenthub.dto.UserCourseCommentsId;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.io.Serializable;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import com.vladmihalcea.hibernate.type.array.ListArrayType;
import org.hibernate.annotations.TypeDef;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user_course_comments_like")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Embeddable
public class UserCourseCommentLike implements Serializable {

  @EmbeddedId
  private UserCourseCommentsId id;

  @Column(name = "comment_ids")
  @Type(type = "com.vladmihalcea.hibernate.type.array.ListArrayType")
  private List<String> commentIds; // Maps to the comment_ids column



}
