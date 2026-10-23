package com.northland.drools.tagging.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.northland.drools.common.response.Result;
import com.northland.drools.entity.Employee;
import com.northland.drools.tagging.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 员工管理Controller
 */
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    /**
     * 分页查询员工
     */
    @GetMapping
    public Result<Page<Employee>> list(@RequestParam(defaultValue = "1") Integer current,
                                     @RequestParam(defaultValue = "10") Integer size,
                                     @RequestParam(required = false) String keyword) {
        Page<Employee> page = employeeService.page(new Page<>(current, size),
                new LambdaQueryWrapper<Employee>()
                        .like(keyword != null, Employee::getName, keyword)
                        .orderByDesc(Employee::getId));
        return Result.success(page);
    }

    /**
     * 添加员工
     */
    @PostMapping
    public Result<Void> add(@RequestBody Employee employee) {
        employeeService.save(employee);
        return Result.success();
    }

    /**
     * 修改员工
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Employee employee) {
        employee.setId(id);
        employeeService.updateById(employee);
        return Result.success();
    }

    /**
     * 删除员工
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        employeeService.removeById(id);
        return Result.success();
    }
}
