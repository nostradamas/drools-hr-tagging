package com.northland.drools.tagging.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.northland.drools.entity.Employee;

import java.util.List;

/**
 * 员工服务接口
 */
public interface EmployeeService extends IService<Employee> {
    
    /**
     * 分页查询员工信息
     *
     * @param page     分页参数
     * @param keyword  关键字（姓名/部门）
     * @return 分页结果
     */
    IPage<Employee> queryEmployees(Page<Employee> page, String keyword);
    
    /**
     * 根据部门获取员工列表
     *
     * @param department 部门名称
     * @return 员工列表
     */
    List<Employee> getEmployeesByDepartment(String department);
    
    /**
     * 获取具有特定技能的员工
     *
     * @param skill 技能名称
     * @return 员工列表
     */
    List<Employee> getEmployeesBySkill(String skill);
    
    /**
     * 更新员工技能
     *
     * @param employeeId 员工ID
     * @param skills 技能列表
     * @return 是否成功
     */
    boolean updateEmployeeSkills(Long employeeId, List<String> skills);
}
