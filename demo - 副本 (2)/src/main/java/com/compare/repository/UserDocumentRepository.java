package com.compare.repository;

import com.compare.entity.UserDocument;
import com.compare.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDocumentRepository extends JpaRepository<UserDocument, Long> {
    List<UserDocument> findByUser(User user);

    Optional<UserDocument> findByUserAndReportId(User user, Long reportId);

    Optional<UserDocument> findByReportId(Long reportId);
}