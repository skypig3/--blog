package com.sky.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.blog.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 统计"除指定用户之外"还有多少个启用状态的管理员。
     *
     * <p>用于作业 2.3.2 的硬规则：禁用或降级前调用，返回 0 就拒绝操作，
     * 防止误操作导致后台无人可管。
     * 注意：自定义 SQL 不会被 MyBatis-Plus 的逻辑删除自动改写，条件要自己写全。
     */
    @Select("""
            SELECT COUNT(*) FROM t_user
            WHERE role = 'ADMIN' AND status = 'ACTIVE' AND deleted = 0 AND id <> #{exceptId}
            """)
    long countOtherActiveAdmins(@Param("exceptId") Long exceptId);
}
