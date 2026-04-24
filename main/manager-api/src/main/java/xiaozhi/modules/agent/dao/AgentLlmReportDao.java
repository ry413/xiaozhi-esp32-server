package xiaozhi.modules.agent.dao;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import xiaozhi.modules.agent.entity.AgentLlmReportEntity;

/**
 * 智能体大模型调用记录Dao对象
 */
@Mapper
public interface AgentLlmReportDao extends BaseMapper<AgentLlmReportEntity> {
}
