package com.compare.repository;

import com.compare.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户数据访问接口
 * 提供用户相关的数据库操作方法
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * 根据用户名查找用户
     * 使用 Spring Data JPA 的命名方法查询
     *
     * @param username 用户名
     * @return 可能包含用户的 Optional 对象
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据用户名查找用户
     * 使用自定义 JPQL 查询语句
     *
     * @param username 用户名
     * @return 可能包含用户的 Optional 对象
     */
    @Query("SELECT u FROM User u WHERE u.username = :username")
    Optional<User> findByUsernameCustom(@Param("username") String username);
}