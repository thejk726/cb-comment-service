package com.tarento.commenthub.dto;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.Embeddable;

@Embeddable
public class UserCourseCommentsId implements Serializable {

  private String userId;       // Corresponds to user_id in the table
  private String courseId;   // Corresponds to course_id in the table

  // Default Constructor
  public UserCourseCommentsId() {}

  // Constructor with fields
  public UserCourseCommentsId(String userId, String courseId) {
    this.userId = userId;
    this.courseId = courseId;
  }

  // Getters and Setters
  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getCourseId() {
    return courseId;
  }

  public void setCourseId(String courseId) {
    this.courseId = courseId;
  }

  // Equals and HashCode
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UserCourseCommentsId that = (UserCourseCommentsId) o;
    return Objects.equals(userId, that.userId) && Objects.equals(courseId, that.courseId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, courseId);
  }

}
