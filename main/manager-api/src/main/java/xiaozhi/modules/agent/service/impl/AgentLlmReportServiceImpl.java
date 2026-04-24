package xiaozhi.modules.agent.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xiaozhi.common.page.PageData;
import xiaozhi.common.utils.ConvertUtils;
import xiaozhi.modules.agent.dao.AgentLlmReportDao;
import xiaozhi.modules.agent.dto.AgentLlmReportDTO;
import xiaozhi.modules.agent.dto.AgentLlmReportPageDTO;
import xiaozhi.modules.agent.entity.AgentEntity;
import xiaozhi.modules.agent.entity.AgentLlmReportEntity;
import xiaozhi.modules.agent.service.AgentLlmReportService;
import xiaozhi.modules.agent.service.AgentService;
import xiaozhi.modules.agent.vo.AgentLlmReportVO;
import xiaozhi.modules.device.entity.DeviceEntity;
import xiaozhi.modules.device.service.DeviceService;
import xiaozhi.modules.sys.dao.SysUserDao;
import xiaozhi.modules.sys.entity.SysUserEntity;

/**
 * 智能体大模型调用记录service impl
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AgentLlmReportServiceImpl extends ServiceImpl<AgentLlmReportDao, AgentLlmReportEntity>
        implements AgentLlmReportService {
    private final AgentService agentService;
    private final DeviceService deviceService;
    private final SysUserDao sysUserDao;

    @Override
    public Boolean report(AgentLlmReportDTO report) {
        Long reportTimeMillis = report.getReportTime() != null ? report.getReportTime() : System.currentTimeMillis();
        String agentId = null;
        Long userId = null;

        DeviceEntity device = deviceService.getDeviceByMacAddress(report.getMacAddress());
        if (device != null) {
            userId = device.getUserId();
            agentId = device.getAgentId();
        }

        AgentEntity agentEntity = agentService.getDefaultAgentByMacAddress(report.getMacAddress());
        if (agentEntity != null) {
            if (agentId == null) {
                agentId = agentEntity.getId();
            }
            if (userId == null) {
                userId = agentEntity.getUserId();
            }
        }

        AgentLlmReportEntity entity = AgentLlmReportEntity.builder()
                .macAddress(report.getMacAddress())
                .clientId(report.getClientId())
                .clientIp(report.getClientIp())
                .agentId(agentId)
                .userId(userId)
                .sessionId(report.getSessionId())
                .llmInput(report.getLlmInput())
                .llmOutput(report.getLlmOutput())
                .createdAt(new Date(reportTimeMillis))
                .build();

        save(entity);
        log.info("设备 {} 大模型调用上报成功, sessionId={}", report.getMacAddress(), report.getSessionId());
        return Boolean.TRUE;
    }

    @Override
    public PageData<AgentLlmReportVO> pageAdmin(AgentLlmReportPageDTO dto) {
        long current = Long.parseLong(dto.getPage());
        long limit = Long.parseLong(dto.getLimit());
        Page<AgentLlmReportEntity> pageInfo = new Page<>(current, limit);

        QueryWrapper<AgentLlmReportEntity> query = new QueryWrapper<>();
        query.eq(dto.getUserId() != null, "user_id", dto.getUserId())
                .like(StringUtils.isNotBlank(dto.getMacAddress()), "mac_address", dto.getMacAddress())
                .like(StringUtils.isNotBlank(dto.getSessionId()), "session_id", dto.getSessionId())
                .ge(StringUtils.isNotBlank(dto.getStartTime()), "created_at", dto.getStartTime())
                .le(StringUtils.isNotBlank(dto.getEndTime()), "created_at", dto.getEndTime());

        appendKeywordFilter(query, dto.getKeyword());
        query.orderByDesc("id");

        IPage<AgentLlmReportEntity> page = this.baseMapper.selectPage(pageInfo, query);
        List<AgentLlmReportVO> records = ConvertUtils.sourceToTarget(page.getRecords(), AgentLlmReportVO.class);
        fillUsernames(records);
        return new PageData<>(records, page.getTotal());
    }

    private void appendKeywordFilter(QueryWrapper<AgentLlmReportEntity> query, String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return;
        }

        List<Long> usernameMatchedUserIds = findUserIdsByKeyword(keyword);
        boolean keywordIsUserId = StringUtils.isNumeric(keyword);
        Long keywordUserId = keywordIsUserId ? Long.parseLong(keyword) : null;

        query.and(wrapper -> {
            wrapper.like("mac_address", keyword)
                    .or()
                    .like("client_id", keyword)
                    .or()
                    .like("client_ip", keyword)
                    .or()
                    .like("session_id", keyword)
                    .or()
                    .like("llm_input", keyword)
                    .or()
                    .like("llm_output", keyword);
            if (keywordUserId != null) {
                wrapper.or().eq("user_id", keywordUserId);
            }
            if (!usernameMatchedUserIds.isEmpty()) {
                wrapper.or().in("user_id", usernameMatchedUserIds);
            }
        });
    }

    private List<Long> findUserIdsByKeyword(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return List.of();
        }

        QueryWrapper<SysUserEntity> userQuery = new QueryWrapper<>();
        userQuery.select("id").like("username", keyword);
        if (StringUtils.isNumeric(keyword)) {
            userQuery.or().eq("id", Long.parseLong(keyword));
        }

        List<SysUserEntity> users = sysUserDao.selectList(userQuery);
        if (users == null || users.isEmpty()) {
            return List.of();
        }
        return users.stream().map(SysUserEntity::getId).collect(Collectors.toList());
    }

    private void fillUsernames(List<AgentLlmReportVO> records) {
        Set<Long> userIds = records.stream()
                .map(AgentLlmReportVO::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }

        List<SysUserEntity> users = sysUserDao.selectBatchIds(new ArrayList<>(userIds));
        Map<Long, SysUserEntity> userMap = users.stream()
                .collect(Collectors.toMap(SysUserEntity::getId, Function.identity(), (a, b) -> a));
        records.forEach(record -> {
            SysUserEntity user = userMap.get(record.getUserId());
            if (user != null) {
                record.setUsername(user.getUsername());
            }
        });
    }
}
