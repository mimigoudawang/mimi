package com.compare.repository;

import com.compare.entity.Document;
import com.compare.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文档数据访问接口
 * 提供文档相关的数据库操作方法
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    /**
     * 查找指定用户上传的所有文档
     *
     * @param uploader 上传者用户对象
     * @return 该用户上传的文档列表
     */
    List<Document> findByUploader(User uploader);
}