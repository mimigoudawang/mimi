package com.compare.repository;

import com.compare.entity.Document;
import com.compare.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文档仓库接口
 * 提供文档实体的数据访问方法
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    // JpaRepository提供了基本的CRUD操作方法
    List<Document> findByUploader(User uploader);

}