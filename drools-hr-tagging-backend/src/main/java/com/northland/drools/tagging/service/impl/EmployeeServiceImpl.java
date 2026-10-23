package com.northland.drools.tagging.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.northland.drools.mapper.EmployeeMapper;
import com.northland.drools.entity.Employee;
import com.northland.drools.tagging.service.EmployeeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 员工服务实现类
 */
@Service
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee> implements EmployeeService {
    
    @Override
    public IPage<Employee> queryEmployees(Page<Employee> page, String keyword) {
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Employee::getName, keyword)
                  .or()
                  .like(Employee::getDepartment, keyword);
        }
        
        wrapper.orderByDesc(Employee::getId);
        return this.page(page, wrapper);
    }

    @Override
    public List<Employee> getEmployeesByDepartment(String department) {
        if (!StringUtils.hasText(department)) {
            return new ArrayList<>();
        }
        
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Employee::getDepartment, department)
               .orderByDesc(Employee::getId);
               
        return this.list(wrapper);
    }

    @Override
    public List<Employee> getEmployeesBySkill(String skill) {
        if (!StringUtils.hasText(skill)) {
            return new ArrayList<>();
        }
        
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Employee::getTechnicalSkills, skill)
               .orderByDesc(Employee::getId);
               
        return this.list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateEmployeeSkills(Long employeeId, List<String> skills) {
        if (employeeId == null || skills == null) {
            return false;
        }
        
        Employee employee = this.getById(employeeId);
        if (employee == null) {
            return false;
        }
        
        StringBuilder skillsStr = new StringBuilder();
        for (int i = 0; i < skills.size(); i++) {
            if (i > 0) {
                skillsStr.append(",");
            }
            skillsStr.append(skills.get(i));
        }
        
        employee.setTechnicalSkills(skillsStr.toString());
        return this.updateById(employee);
    }
}
