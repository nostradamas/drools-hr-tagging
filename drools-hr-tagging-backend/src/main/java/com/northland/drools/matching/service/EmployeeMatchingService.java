package com.northland.drools.matching.service;

import com.northland.drools.matching.model.Employee;
import com.northland.drools.matching.model.Position;
import com.northland.drools.matching.model.MatchingResult;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmployeeMatchingService {

    @Autowired
    private KieContainer kieContainer;

    /**
     * 单个员工与单个职位匹配
     */
    public MatchingResult matchEmployeeToPosition(Employee employee, Position position) {
        MatchingResult result = new MatchingResult(employee, position);

        // 每次匹配新建会话，避免共享会话的状态污染
        KieSession session = kieContainer.newKieSession();
        
        try {
            // 插入事实到工作内存
            org.kie.api.runtime.rule.FactHandle resultHandle = session.insert(result);
            session.insert(employee);
            session.insert(position);

            // 两阶段执行（#18 修复）：评分规则与推荐规则分属不同 agenda-group。
            // 推荐规则的 LHS 依赖最终 matchScore，而评分规则改分时不 update 事实，
            // 单阶段 fireAllRules 时推荐规则只按初始 0 分求值，永远落到"不推荐"。
            // 先聚焦 scoring 组算分，再 update 刷新事实、聚焦 recommendation 组定档。
            session.getAgenda().getAgendaGroup("scoring").setFocus();
            session.fireAllRules();

            session.update(resultHandle, result);
            session.getAgenda().getAgendaGroup("recommendation").setFocus();
            session.fireAllRules();

            return result;
        } finally {
            session.dispose();
        }
    }

    /**
     * 单个员工与多个职位匹配
     */
    public List<MatchingResult> matchEmployeeToPositions(Employee employee, List<Position> positions) {
        List<MatchingResult> results = new ArrayList<MatchingResult>();
        
        for (Position position : positions) {
            MatchingResult result = matchEmployeeToPosition(employee, position);
            results.add(result);
        }
        
        // 按匹配分数降序排序
        results.sort(new java.util.Comparator<MatchingResult>() {
            @Override
            public int compare(MatchingResult r1, MatchingResult r2) {
                return Double.compare(r2.getMatchScore(), r1.getMatchScore());
            }
        });
        
        return results;
    }

    /**
     * 多个员工与单个职位匹配
     */
    public List<MatchingResult> matchEmployeesToPosition(List<Employee> employees, Position position) {
        List<MatchingResult> results = new ArrayList<MatchingResult>();
        
        for (Employee employee : employees) {
            MatchingResult result = matchEmployeeToPosition(employee, position);
            results.add(result);
        }
        
        // 按匹配分数降序排序
        results.sort(new java.util.Comparator<MatchingResult>() {
            @Override
            public int compare(MatchingResult r1, MatchingResult r2) {
                return Double.compare(r2.getMatchScore(), r1.getMatchScore());
            }
        });
        
        return results;
    }

    /**
     * 批量匹配：多个员工与多个职位
     */
    public List<MatchingResult> batchMatch(List<Employee> employees, List<Position> positions) {
        List<MatchingResult> allResults = new ArrayList<MatchingResult>();
        
        for (Employee employee : employees) {
            for (Position position : positions) {
                MatchingResult result = matchEmployeeToPosition(employee, position);
                allResults.add(result);
            }
        }
        
        // 按匹配分数降序排序
        allResults.sort(new java.util.Comparator<MatchingResult>() {
            @Override
            public int compare(MatchingResult r1, MatchingResult r2) {
                return Double.compare(r2.getMatchScore(), r1.getMatchScore());
            }
        });
        
        return allResults;
    }

    /**
     * 获取推荐的匹配结果
     */
    public List<MatchingResult> getRecommendedMatches(List<MatchingResult> results) {
        List<MatchingResult> recommended = new ArrayList<MatchingResult>();
        
        for (MatchingResult result : results) {
            if (result.isRecommended()) {
                recommended.add(result);
            }
        }
        
        return recommended;
    }

    /**
     * 获取高分匹配结果（80分以上）
     */
    public List<MatchingResult> getHighScoreMatches(List<MatchingResult> results) {
        List<MatchingResult> highScore = new ArrayList<MatchingResult>();
        
        for (MatchingResult result : results) {
            if (result.getMatchScore() >= 80) {
                highScore.add(result);
            }
        }
        
        return highScore;
    }
}
