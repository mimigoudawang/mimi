package com.compare.repository;

import com.compare.entity.CompareReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 比对报告数据访问接口
 * 提供查重报告相关的数据库操作方法
 * 继承 JpaRepository 以获得基本的 CRUD 操作方法：
 * - save：保存或更新报告
 * - findById：根据ID查找报告
 * - findAll：查找所有报告
 * - delete：删除报告
 * 等等
 */
@Repository
public interface CompareReportRepository extends JpaRepository<CompareReport, Long> {
}