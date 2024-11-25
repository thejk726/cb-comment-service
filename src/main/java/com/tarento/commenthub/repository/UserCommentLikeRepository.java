package com.tarento.commenthub.repository;

import com.tarento.commenthub.dto.UserCourseCommentsId;
import com.tarento.commenthub.entity.UserCourseCommentLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCommentLikeRepository extends JpaRepository<UserCourseCommentLike, UserCourseCommentsId> {

}
