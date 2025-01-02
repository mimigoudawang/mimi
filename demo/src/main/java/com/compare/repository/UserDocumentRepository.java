package com.compare.repository;

import com.compare.entity.UserDocument;
import com.compare.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户文档数据访问接口
 * 提供用户文档相关的数据库操作方法
 */
@Repository
public interface UserDocumentRepository extends JpaRepository<UserDocument, Long> {
    /**
     * 查找指定用户的所有文档，按上传时间降序排序
     *
     * @param user 用户对象
     * @return 该用户的文档列表
     */
    List<UserDocument> findByUserOrderByUploadTimeDesc(User user);

    /**
     * 根据用户和报告ID查找文档
     *
     * @param user     用户对象
     * @param reportId 报告ID
     * @return 可能包含文档的 Optional 对象
     */
    Optional<UserDocument> findByUserAndReportId(User user, Long reportId);

    /**
     * 根据报告ID查找文档
     *
     * @param reportId 报告ID
     * @return 可能包含文档的 Optional 对象
     */
    Optional<UserDocument> findByReportId(Long reportId);
}