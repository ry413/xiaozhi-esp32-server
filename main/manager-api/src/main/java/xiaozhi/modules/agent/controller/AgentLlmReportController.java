package xiaozhi.modules.agent.controller;

import java.util.Map;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import xiaozhi.common.constant.Constant;
import xiaozhi.common.page.PageData;
import xiaozhi.common.utils.Result;
import xiaozhi.common.validator.ValidatorUtils;
import xiaozhi.modules.agent.dto.AgentLlmReportPageDTO;
import xiaozhi.modules.agent.dto.AgentLlmReportDTO;
import xiaozhi.modules.agent.service.AgentLlmReportService;
import xiaozhi.modules.agent.vo.AgentLlmReportVO;

@Tag(name = "智能体大模型调用记录")
@RequiredArgsConstructor
@RestController
@RequestMapping("/agent/llm-report")
public class AgentLlmReportController {
    private final AgentLlmReportService agentLlmReportService;

    @Operation(summary = "分页查询大模型调用记录")
    @GetMapping("/page")
    @RequiresPermissions("sys:role:superAdmin")
    public Result<PageData<AgentLlmReportVO>> page(
            @Parameter(hidden = true) @RequestParam Map<String, Object> params) {
        AgentLlmReportPageDTO dto = new AgentLlmReportPageDTO();
        dto.setPage((String) params.get(Constant.PAGE));
        dto.setLimit((String) params.get(Constant.LIMIT));
        dto.setKeyword((String) params.get("keyword"));
        dto.setMacAddress((String) params.get("macAddress"));
        dto.setSessionId((String) params.get("sessionId"));
        dto.setStartTime((String) params.get("startTime"));
        dto.setEndTime((String) params.get("endTime"));
        Object userId = params.get("userId");
        if (userId != null && !userId.toString().isBlank()) {
            dto.setUserId(Long.parseLong(userId.toString()));
        }
        ValidatorUtils.validateEntity(dto);
        return new Result<PageData<AgentLlmReportVO>>().ok(agentLlmReportService.pageAdmin(dto));
    }

    @Operation(summary = "小智服务大模型调用上报请求")
    @PostMapping("/report")
    public Result<Boolean> report(@Valid @RequestBody AgentLlmReportDTO request) {
        Boolean result = agentLlmReportService.report(request);
        return new Result<Boolean>().ok(result);
    }
}
