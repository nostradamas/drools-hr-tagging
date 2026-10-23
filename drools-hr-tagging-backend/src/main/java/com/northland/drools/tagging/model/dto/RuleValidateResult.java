package com.northland.drools.tagging.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 规则验证结果
 */
@Data
@Accessors(chain = true)
public class RuleValidateResult {
    /**
     * 是否验证通过
     */
    private boolean valid;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    public static RuleValidateResult success() {
        return new RuleValidateResult()
                .setValid(true)
                .setErrorMessage("");
    }
    
    public static RuleValidateResult error(String message) {
        return new RuleValidateResult()
                .setValid(false)
                .setErrorMessage(message);
    }
}
