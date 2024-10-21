package com.tarento.commenthub.repository;

import com.tarento.commenthub.entity.Comment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CommentRepository extends JpaRepository<Comment, String> {


  Optional<Comment> findByCommentIdAndStatus(String id, String status);

  List<Comment> findByCommentIdInAndStatus(List<String> ids, String status);

  List<Comment> findByStatus(String status);

  @Query(value = "SELECT created_date FROM comment WHERE comment_id = ?1", nativeQuery = true)
  LocalDateTime getCreatedDateByCommentId(String commentId);

  Page<Comment> findByCommentIdIn(List<String> ids, Pageable pageable);

  List<Comment> findByCommentIdIn(List<String> commentIds, Sort sort);

}