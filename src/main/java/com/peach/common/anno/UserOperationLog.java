package com.peach.common.anno;

import java.lang.annotation.*;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description // 用户操作记录日志
 * @CreateTime 2024/10/14 15:51
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UserOperationLog {

}
