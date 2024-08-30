package com.tarento.commenthub.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "comment_tree")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class CommentTree {

  @Id
  private String commentTreeId;

  @Type(type = "jsonb")
  @Column(columnDefinition = "jsonb")
  private JsonNode commentTreeData;

  @Column(columnDefinition = "varchar(255) default 'active'")
  private String status;

  private Timestamp createdDate;

  private Timestamp lastUpdatedDate;
}