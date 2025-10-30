package com.company.tooldashboard.validation;

import com.company.tooldashboard.util.SemanticVersionUtil;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 语义化版本号校验器
 */
public class SemanticVersionValidator implements ConstraintValidator<SemanticVersion, String> {
    
    private boolean allowNull;
    
    @Override
    public void initialize(SemanticVersion constraintAnnotation) {
        this.allowNull = constraintAnnotation.allowNull();
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // 如果值为空
        if (value == null || value.trim().isEmpty()) {
            return allowNull;
        }
        
        // 使用工具类校验版本号格式
        return SemanticVersionUtil.isValidSemanticVersion(value);
    }
}
