package com.kaseihaku.bpm.app.one.workflow.feature.v1.n5_join;

import com.kaseihaku.boot.starter.spring.exception.DelegateBizExBuilder;
import com.kaseihaku.bpm.app.one.workflow.feature.FeatureNodeBrand;
import com.kaseihaku.bpm.starter.ppe.i18n.BpmPpeI18nKey;
import com.kaseihaku.cloud.starter.basic.constant.CloudImmutableConst;
import com.kaseihaku.core.ppe.engine.PpeFacade;
import com.kaseihaku.core.ppe.engine.ctx.NodeCommitContext;
import com.kaseihaku.core.ppe.node.PpeNodeDto;
import com.kaseihaku.core.ppe.node.PpeNodeReadSvc;
import com.kaseihaku.core.ppe.todo.PpeToDoDto;
import com.kaseihaku.core.ppe.todo.calculator.NextStepCalculator;
import com.kaseihaku.core.ppe.todo.calculator.NextStepCalculatorDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Set;

@Slf4j
public class CounterSignJoinNsc implements NextStepCalculator<CounterSignJoinPojo> {


    @Override
    public Set<PpeToDoDto> calculate(NextStepCalculatorDto<CounterSignJoinPojo> dto, NodeCommitContext ctx) {

        log.info("这里将从 extend 分出的两个 branch 合并回来");

        Long curNodeId = dto.getCurNodeIns().getId();
        PpeFacade ppe = dto.getCurPpe();
        PpeNodeReadSvc nodeReadSvc = ppe.getNodeReadSvc();

        // 获取两个 branch 共同的父节点
        List<PpeNodeDto> extendNodes = nodeReadSvc.closestAncestors(curNodeId, FeatureNodeBrand.V1.extend);

        if(CollectionUtils.isEmpty(extendNodes)){
            throw DelegateBizExBuilder.ins().build(BpmPpeI18nKey.ex_biz, "找不到分支起始点，流程走向有问题");
        }


        PpeNodeDto parentNode = extendNodes.get(0);  // extend 没有多实例的情况， 所以直接获取第一个即可

        // 正常情况下此处不应该 报错，让当前 node commit 完成，然后由 branch1 的 AfterPersistenceHook 执行合并操作
        // 由于演示，branch1 节点没有配置 AfterPersistenceHook，所以这里报错处理
        PpeNodeDto branch1Node = nodeReadSvc.furthestDescendant(parentNode.getId(), FeatureNodeBrand.V1.branch1)
            .stream().findFirst().orElseThrow(() -> DelegateBizExBuilder.ins().build(BpmPpeI18nKey.ex_biz, "branch1 节点还未提交"));


        // 执行合并
        PpeToDoDto newToDo = new PpeToDoDto();
        newToDo.setNextNodeMetaBrand(FeatureNodeBrand.V1.overrule);
        newToDo.setNextApproverHomonymGroupId(CloudImmutableConst.PreBuryGroup.Homonym_0);

        List<PpeToDoDto> newToDos = ppe.nodeMerge(dto.getCurProcIns().getId(), Set.of(curNodeId, branch1Node.getId()),
            List.of(newToDo));
        return Set.copyOf(newToDos);
    }
}
