package com.company.tooldashboard.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * 语义化版本号校验注解
 * 用于校验字符串是否符合语义化版本规范 2.0.0
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SemanticVersionValidator.class)
@Documented
public @interface SemanticVersion {
    
    /**
     * 错误消息
     */
    String message() default "版本号格式不正确，必须符合语义化版本规范（如：1.0.0）";
    
    /**
     * 分组
     */
    Class<?>[] groups() default {};
    
    /**
     * 负载
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * 是否允许为空
     */
    boolean allowNull() default true;
}
