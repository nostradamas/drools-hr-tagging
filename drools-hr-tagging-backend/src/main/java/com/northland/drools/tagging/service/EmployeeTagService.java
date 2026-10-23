package com.northland.drools.tagging.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.northland.drools.entity.EmployeeTag;

import java.util.List;

/**
 * 员工标签服务接口
 */
public interface EmployeeTagService extends IService<EmployeeTag> {
    
    /**
     * 查询员工的标签
     *
     * @param employeeId 员工ID
     * @return 标签列表
     */
    List<EmployeeTag> getEmployeeTags(Long employeeId);
    
    /**
     * 分页查询员工标签
     *
     * @param page 分页参数
     * @param employeeId 员工ID
     * @param dimension 标签维度
     * @return 分页结果
     */
    IPage<EmployeeTag> queryEmployeeTags(Page<EmployeeTag> page, Long employeeId, String dimension);
    
    /**
     * 批量添加员工标签
     *
     * @param employeeId 员工ID
     * @param tags 标签列表
     * @return 是否成功
     */
    boolean addEmployeeTags(Long employeeId, List<EmployeeTag> tags);
    
    /**
     * 删除员工标签
     *
     * @param employeeId 员工ID
     * @param dimension 标签维度
     * @return 是否成功
     */
    boolean removeEmployeeTagsByDimension(Long employeeId, String dimension);
    
    /**
     * 清理过期标签
     *
     * @return 清理的标签数量
     */
    int cleanupExpiredTags();
}
