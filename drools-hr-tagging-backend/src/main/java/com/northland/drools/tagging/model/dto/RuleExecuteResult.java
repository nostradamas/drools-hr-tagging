package com.northland.drools.tagging.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * 规则执行结果
 */
@Data
@Accessors(chain = true)
public class RuleExecuteResult {
    /**
     * 是否执行成功
     */
    private boolean success;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 执行结果列表
     */
    private List<Object> results;
    
    public RuleExecuteResult() {
        this.results = new ArrayList<>();
    }
    
    public static RuleExecuteResult success(List<Object> results) {
        return new RuleExecuteResult()
                .setSuccess(true)
                .setResults(results);
    }
    
    public static RuleExecuteResult error(String message) {
        return new RuleExecuteResult()
                .setSuccess(false)
                .setErrorMessage(message);
    }
}
