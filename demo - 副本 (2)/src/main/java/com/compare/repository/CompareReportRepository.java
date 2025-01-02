package com.compare.repository;

import com.compare.entity.CompareReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 比对报告仓库接口
 * 提供报告的数据访问方法
 */
@Repository
public interface CompareReportRepository extends JpaRepository<CompareReport, Long> {
}