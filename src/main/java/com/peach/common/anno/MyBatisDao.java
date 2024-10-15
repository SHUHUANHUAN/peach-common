package com.peach.common.anno;

import java.lang.annotation.*;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description // 用于扫描Dao
 * @CreateTime 2024/10/14 15:53
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MyBatisDao {
}
