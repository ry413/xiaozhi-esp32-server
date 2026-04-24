package xiaozhi.modules.agent.service;

import com.baomidou.mybatisplus.extension.service.IService;

import xiaozhi.common.page.PageData;
import xiaozhi.modules.agent.dto.AgentLlmReportPageDTO;
import xiaozhi.modules.agent.dto.AgentLlmReportDTO;
import xiaozhi.modules.agent.entity.AgentLlmReportEntity;
import xiaozhi.modules.agent.vo.AgentLlmReportVO;

/**
 * 智能体大模型调用记录service
 */
public interface AgentLlmReportService extends IService<AgentLlmReportEntity> {
    Boolean report(AgentLlmReportDTO report);

    PageData<AgentLlmReportVO> pageAdmin(AgentLlmReportPageDTO dto);
}
