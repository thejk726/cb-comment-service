package com.tarento.commenthub.repository;

import com.tarento.commenthub.entity.CommentTree;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentTreeRepository extends JpaRepository<CommentTree, String> {

  @Query(value = "SELECT * "
      + "FROM comment_tree "
      + "WHERE comment_tree_id IN ("
      + "  SELECT comment_tree_id "
      + "  FROM comment_tree "
      + "  WHERE (comment_tree_data->>'entityId' = :entityId AND comment_tree_data->>'entityType' = :entityType "
      + "    AND comment_tree_data->>'workflow' IN (:workflowList))"
      + ")", nativeQuery = true)
  List<CommentTree> getAllCommentTreeForMultipleWorkflows(
      @Param("entityId") String entityId,
      @Param("entityType") String entityType,
      @Param("workflowList") List<String> workflowList
  );

  @Query(value = "SELECT COUNT(*) FROM comment_tree WHERE comment_tree_id = ?1", nativeQuery = true)
  int getIdCount(String commentTreeId);
}