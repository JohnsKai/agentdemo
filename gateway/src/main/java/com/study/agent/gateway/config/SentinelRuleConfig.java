package com.study.agent.gateway.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @date 2026/06/06
 **/
@Component
public class SentinelRuleConfig implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) {
        initGlobalFlowRules();
        initUserFlowRules();
        initDegradeRules();
    }

    private void initGlobalFlowRules() {
        List<FlowRule> rules = new ArrayList<>();
        FlowRule globalRule = (FlowRule) new FlowRule("agent-service")
                .setCount(100)
                .setGrade(RuleConstant.FLOW_GRADE_QPS)
                .setLimitApp("default");
        rules.add(globalRule);
        FlowRuleManager.loadRules(rules);
    }

    private void initUserFlowRules() {
        ParamFlowRule ruleUser = new ParamFlowRule("user-limit")
                // 0 表示SphU.entry(String name, EntryType trafficType, int batchCount, Object... args)中，args传入的第一个参数，1即为第二个参数
                .setParamIdx(0)
                // 每个用户每秒最多10个请求
                .setCount(10)
                .setGrade(RuleConstant.FLOW_GRADE_QPS);
        ParamFlowRuleManager.loadRules(Collections.singletonList(ruleUser));
    }

    private void initDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>();
        DegradeRule slowRule = new DegradeRule("agent-service")
                .setGrade(RuleConstant.DEGRADE_GRADE_RT)
                .setCount(1000)
                .setTimeWindow(30)
                .setMinRequestAmount(10)
                .setStatIntervalMs(10000)
                .setSlowRatioThreshold(0.5);
        rules.add(slowRule);

        // 新增：异常比例熔断（例如后端服务异常率超过 50% 时熔断）
        DegradeRule exceptionRule = new DegradeRule("agent-service")
                .setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO)
                // 异常比例阈值 50%
                .setCount(0.5)
                // 熔断时长 20秒
                .setTimeWindow(20)
                // 最小请求数
                .setMinRequestAmount(5)
                .setStatIntervalMs(10000);
        rules.add(exceptionRule);
        DegradeRuleManager.loadRules(rules);
    }
}
