package xiaozhi.modules.agent.controller;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import xiaozhi.common.constant.Constant;
import xiaozhi.common.page.PageData;
import xiaozhi.common.redis.RedisKeys;
import xiaozhi.common.redis.RedisUtils;
import xiaozhi.common.user.UserDetail;
import xiaozhi.common.utils.Result;
import xiaozhi.common.utils.ResultUtils;
import xiaozhi.modules.agent.dto.AgentChatHistoryDTO;
import xiaozhi.modules.agent.dto.AgentChatSessionDTO;
import xiaozhi.modules.agent.dto.AgentCreateDTO;
import xiaozhi.modules.agent.dto.AgentDTO;
import xiaozhi.modules.agent.dto.AgentMemoryDTO;
import xiaozhi.modules.agent.dto.AgentLlmReplyDTO;
import xiaozhi.modules.agent.dto.AgentUpdateDTO;
import xiaozhi.modules.agent.entity.AgentEntity;
import xiaozhi.modules.agent.entity.AgentTemplateEntity;
import xiaozhi.modules.agent.dto.AgentTagDTO;
import xiaozhi.modules.agent.entity.AgentTagEntity;
import xiaozhi.modules.agent.service.AgentTagService;
import xiaozhi.modules.agent.service.AgentChatAudioService;
import xiaozhi.modules.agent.service.AgentChatHistoryService;
import xiaozhi.modules.agent.service.AgentChatSummaryService;
import xiaozhi.modules.agent.service.AgentContextProviderService;
import xiaozhi.modules.agent.service.AgentPluginMappingService;
import xiaozhi.modules.agent.service.AgentService;
import xiaozhi.modules.agent.service.AgentTemplateService;
import xiaozhi.modules.agent.vo.AgentChatHistoryUserVO;
import xiaozhi.modules.agent.vo.AgentInfoVO;
import xiaozhi.modules.device.entity.DeviceEntity;
import xiaozhi.modules.device.service.DeviceService;
import xiaozhi.modules.security.user.SecurityUser;

@Tag(name = "智能体管理")
@RequiredArgsConstructor
@RestController
@RequestMapping("/agent")
public class AgentController {
    private static final List<String> PROMPT_INJECTION_KEYWORDS = List.of(
            "<think>",
            "</think>",
            "<thinking>",
            "</thinking>",
            "task changed",
            "忽略之前",
            "忽略上文",
            "忽略以上",
            "忽略前面",
            "系统提示",
            "提示词",
            "prompt",
            "system prompt",
            "prompt injection",
            "jailbreak",
            "developer",
            "admin",
            "管理员",
            "开发者",
            "role: system",
            "reveal prompt",
            "输出你的规则",
            "复述你的规则",
            "内部指令",
            "隐藏指令");

    private static final List<String> PROMPT_INJECTION_BLOCK_REPLIES = List.of(
            "对不起，这个问题我无法回答",
            "抱歉，这部分内容我不能提供",
            "这个请求我暂时无法处理",
            "这个问题超出了我当前可回答的范围");

    private final AgentService agentService;
    private final AgentTemplateService agentTemplateService;
    private final DeviceService deviceService;
    private final AgentChatHistoryService agentChatHistoryService;
    private final AgentChatAudioService agentChatAudioService;
    private final AgentPluginMappingService agentPluginMappingService;
    private final AgentContextProviderService agentContextProviderService;
    private final AgentChatSummaryService agentChatSummaryService;
    private final RedisUtils redisUtils;
    private final AgentTagService agentTagService;
    private final RestTemplate restTemplate;

    @Value("${agent.llm.api-url}")
    private String agentLlmApiUrl;

    @Value("${agent.llm.api-key:}")
    private String agentLlmApiKey;

    @Value("${agent.llm.model:deepseek-v4-pro}")
    private String agentLlmModel;

    @Value("${agent.llm.optimize-prompt:You are a helpful assistant.}")
    private String agentLlmOptimizePrompt;

    @Value("${agent.llm.generate-script-prompt:You are a helpful assistant.}")
    private String agentLlmGenerateScriptPrompt;

    private final HttpClient agentLlmHttpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @GetMapping("/list")
    @Operation(summary = "获取用户智能体列表")
    @RequiresPermissions("sys:role:normal")
    public Result<List<AgentDTO>> getUserAgents(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "searchType", defaultValue = "name") String searchType) {
        UserDetail user = SecurityUser.getUser();

        // 直接调用整合后的getUserAgents方法，无需再区分搜索和普通查询
        List<AgentDTO> agents = agentService.getUserAgents(user.getId(), keyword, searchType);
        return new Result<List<AgentDTO>>().ok(agents);
    }

    @GetMapping("/all")
    @Operation(summary = "智能体列表（管理员）")
    @RequiresPermissions("sys:role:superAdmin")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开始", required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录数", required = true),
    })
    public Result<PageData<AgentEntity>> adminAgentList(
            @Parameter(hidden = true) @RequestParam Map<String, Object> params) {
        PageData<AgentEntity> page = agentService.adminAgentList(params);
        return new Result<PageData<AgentEntity>>().ok(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取智能体详情")
    @RequiresPermissions("sys:role:normal")
    public Result<AgentInfoVO> getAgentById(@PathVariable("id") String id) {
        AgentInfoVO agent = agentService.getAgentById(id);
        return ResultUtils.success(agent);
    }

    @PostMapping("/optimize-prompt")
    @Operation(summary = "调用环境变量配置的LLM并返回文本回复")
    @RequiresPermissions("sys:role:normal")
    public Result<String> getLlmReply(@RequestBody @Valid AgentLlmReplyDTO dto) {
        if (StringUtils.isBlank(agentLlmApiKey)) {
            return new Result<String>().error("未配置agent.llm.api-key");
        }

        String blockedReply = detectPromptInjectionReply(dto.getPrompt());
        if (blockedReply != null) {
            return new Result<String>().ok(blockedReply);
        }

        try {
            return new Result<String>().ok(callAgentLlm(agentLlmOptimizePrompt, dto.getPrompt()));
        } catch (Exception e) {
            return new Result<String>().error("LLM调用失败: " + e.getMessage());
        }
    }

    @PostMapping("/generate-script")
    @Operation(summary = "调用环境变量配置的LLM生成智能话术")
    @RequiresPermissions("sys:role:normal")
    public Result<String> generateScript(@RequestBody @Valid AgentLlmReplyDTO dto) {
        if (StringUtils.isBlank(agentLlmApiKey)) {
            return new Result<String>().error("未配置agent.llm.api-key");
        }

        String blockedReply = detectPromptInjectionReply(dto.getPrompt());
        if (blockedReply != null) {
            return new Result<String>().ok(blockedReply);
        }

        try {
            return new Result<String>().ok(callAgentLlm(agentLlmGenerateScriptPrompt, dto.getPrompt()));
        } catch (Exception e) {
            return new Result<String>().error("LLM调用失败: " + e.getMessage());
        }
    }

    private String callAgentLlm(String systemPrompt, String userPrompt) throws Exception {
        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);

        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", agentLlmModel);
        requestBody.put("messages", List.of(systemMessage, userMessage));
        requestBody.put("stream", false);
        requestBody.put("temperature", 1.0);

        String requestJson = JSONUtil.toJsonStr(requestBody);
        HttpResponse<String> response = null;
        Exception lastRetryException = null;

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(agentLlmApiUrl))
                        .timeout(Duration.ofSeconds(120))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + agentLlmApiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
                        .build();
                response = agentLlmHttpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                lastRetryException = null;
                break;
            } catch (Exception e) {
                lastRetryException = e;
                String message = e.getMessage();
                boolean retryable = Strings.CI.contains(message, "closed")
                        || Strings.CI.contains(message, "Connection reset")
                        || Strings.CI.contains(message, "EOF");
                if (!retryable || attempt == 3) {
                    throw e;
                }
                try {
                    Thread.sleep(300L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("线程被中断", ie);
                }
            }
        }

        if (response == null) {
            throw new RuntimeException(lastRetryException == null ? "LLM调用失败" : lastRetryException.getMessage(),
                    lastRetryException);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300 || StringUtils.isBlank(response.body())) {
            throw new RuntimeException("LLM调用失败");
        }

        JSONObject responseJson = JSONUtil.parseObj(response.body());
        JSONArray choices = responseJson.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("LLM响应中没有choices");
        }

        JSONObject firstChoice = choices.getJSONObject(0);
        JSONObject message = firstChoice.getJSONObject("message");
        String content = message == null ? null : message.getStr("content");
        if (StringUtils.isBlank(content)) {
            throw new RuntimeException("LLM响应中没有文本内容");
        }

        return content;
    }

    private String detectPromptInjectionReply(String prompt) {
        if (StringUtils.isBlank(prompt)) {
            return null;
        }

        String normalized = StringUtils.lowerCase(prompt);
        for (String keyword : PROMPT_INJECTION_KEYWORDS) {
            if (normalized.contains(StringUtils.lowerCase(keyword))) {
                sleepPromptInjectionDelay();
                int index = ThreadLocalRandom.current().nextInt(PROMPT_INJECTION_BLOCK_REPLIES.size());
                return PROMPT_INJECTION_BLOCK_REPLIES.get(index);
            }
        }
        return null;
    }

    private void sleepPromptInjectionDelay() {
        long delayMillis = ThreadLocalRandom.current().nextLong(8000L, 15001L);
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @PostMapping
    @Operation(summary = "创建智能体")
    @RequiresPermissions("sys:role:normal")
    public Result<String> save(@RequestBody @Valid AgentCreateDTO dto) {
        String agentId = agentService.createAgent(dto);
        return new Result<String>().ok(agentId);
    }

    @PutMapping("/saveMemory/{macAddress}")
    @Operation(summary = "根据设备id更新智能体")
    public Result<Void> updateByDeviceId(@PathVariable String macAddress, @RequestBody @Valid AgentMemoryDTO dto) {
        DeviceEntity device = deviceService.getDeviceByMacAddress(macAddress);
        if (device == null) {
            return new Result<>();
        }
        AgentUpdateDTO agentUpdateDTO = new AgentUpdateDTO();
        agentUpdateDTO.setSummaryMemory(dto.getSummaryMemory());
        agentService.updateAgentById(device.getAgentId(), agentUpdateDTO);
        return new Result<>();
    }

    @PostMapping("/chat-summary/{sessionId}/save")
    @Operation(summary = "根据会话ID生成聊天记录总结并保存（异步执行）")
    public Result<Void> generateAndSaveChatSummary(@PathVariable String sessionId) {
        try {
            // 异步执行总结生成任务，立即返回成功响应
            new Thread(() -> {
                try {
                    agentChatSummaryService.generateAndSaveChatSummary(sessionId);
                    System.out.println("异步执行会话 " + sessionId + " 的聊天记录总结完成");
                } catch (Exception e) {
                    System.err.println("异步执行会话 " + sessionId + " 的聊天记录总结失败: " + e.getMessage());
                }
            }).start();

            // 立即返回成功响应，不等待总结生成完成
            return new Result<Void>().ok(null);
        } catch (Exception e) {
            return new Result<Void>().error("启动异步总结生成任务失败: " + e.getMessage());
        }
    }

    @PostMapping("/chat-title/{sessionId}/generate")
    @Operation(summary = "根据会话ID生成聊天标题")
    public Result<Void> generateAndSaveChatTitle(@PathVariable String sessionId) {
        agentChatSummaryService.generateAndSaveChatTitle(sessionId);
        return new Result<Void>().ok(null);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新智能体")
    @RequiresPermissions("sys:role:normal")
    public Result<Void> update(@PathVariable String id, @RequestBody @Valid AgentUpdateDTO dto) {
        agentService.updateAgentById(id, dto);
        return new Result<>();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除智能体")
    @RequiresPermissions("sys:role:normal")
    public Result<Void> delete(@PathVariable String id) {
        // 先删除关联的设备
        deviceService.deleteByAgentId(id);
        // 删除关联的聊天记录
        agentChatHistoryService.deleteByAgentId(id, true, true);
        // 删除关联的插件
        agentPluginMappingService.deleteByAgentId(id);
        // 删除关联的上下文源配置
        agentContextProviderService.deleteByAgentId(id);
        // 再删除智能体
        agentService.deleteById(id);
        return new Result<>();
    }

    @GetMapping("/template")
    @Operation(summary = "智能体模板模板列表")
    @RequiresPermissions("sys:role:normal")
    public Result<List<AgentTemplateEntity>> templateList() {
        List<AgentTemplateEntity> list = agentTemplateService
                .list(new QueryWrapper<AgentTemplateEntity>().orderByAsc("sort"));
        return new Result<List<AgentTemplateEntity>>().ok(list);
    }

    @GetMapping("/{id}/sessions")
    @Operation(summary = "获取智能体会话列表")
    @RequiresPermissions("sys:role:normal")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开始", required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录数", required = true),
    })
    public Result<PageData<AgentChatSessionDTO>> getAgentSessions(
            @PathVariable("id") String id,
            @Parameter(hidden = true) @RequestParam Map<String, Object> params) {
        params.put("agentId", id);
        PageData<AgentChatSessionDTO> page = agentChatHistoryService.getSessionListByAgentId(params);
        return new Result<PageData<AgentChatSessionDTO>>().ok(page);
    }

    @GetMapping("/{id}/chat-history/{sessionId}")
    @Operation(summary = "获取智能体聊天记录")
    @RequiresPermissions("sys:role:normal")
    public Result<List<AgentChatHistoryDTO>> getAgentChatHistory(
            @PathVariable("id") String id,
            @PathVariable("sessionId") String sessionId) {
        // 获取当前用户
        UserDetail user = SecurityUser.getUser();

        // 检查权限
        if (!agentService.checkAgentPermission(id, user.getId())) {
            return new Result<List<AgentChatHistoryDTO>>().error("没有权限查看该智能体的聊天记录");
        }

        // 查询聊天记录
        List<AgentChatHistoryDTO> result = agentChatHistoryService.getChatHistoryBySessionId(id, sessionId);
        return new Result<List<AgentChatHistoryDTO>>().ok(result);
    }

    @GetMapping("/{id}/chat-history/user")
    @Operation(summary = "获取智能体聊天记录（用户）")
    @RequiresPermissions("sys:role:normal")
    public Result<List<AgentChatHistoryUserVO>> getRecentlyFiftyByAgentId(
            @PathVariable("id") String id) {
        // 获取当前用户
        UserDetail user = SecurityUser.getUser();

        // 检查权限
        if (!agentService.checkAgentPermission(id, user.getId())) {
            return new Result<List<AgentChatHistoryUserVO>>().error("没有权限查看该智能体的聊天记录");
        }

        // 查询聊天记录
        List<AgentChatHistoryUserVO> data = agentChatHistoryService.getRecentlyFiftyByAgentId(id);
        return new Result<List<AgentChatHistoryUserVO>>().ok(data);
    }

    @GetMapping("/{id}/chat-history/audio")
    @Operation(summary = "获取音频内容")
    @RequiresPermissions("sys:role:normal")
    public Result<String> getContentByAudioId(
            @PathVariable("id") String id) {
        // 查询聊天记录
        String data = agentChatHistoryService.getContentByAudioId(id);
        return new Result<String>().ok(data);
    }

    @PostMapping("/audio/{audioId}")
    @Operation(summary = "获取音频下载ID")
    @RequiresPermissions("sys:role:normal")
    public Result<String> getAudioId(@PathVariable("audioId") String audioId) {
        byte[] audioData = agentChatAudioService.getAudio(audioId);
        if (audioData == null) {
            return new Result<String>().error("音频不存在");
        }
        String uuid = UUID.randomUUID().toString();
        redisUtils.set(RedisKeys.getAgentAudioIdKey(uuid), audioId);
        return new Result<String>().ok(uuid);
    }

    @GetMapping("/play/{uuid}")
    @Operation(summary = "播放音频")
    public ResponseEntity<byte[]> playAudio(@PathVariable("uuid") String uuid) {

        String audioId = (String) redisUtils.get(RedisKeys.getAgentAudioIdKey(uuid));
        if (StringUtils.isBlank(audioId)) {
            return ResponseEntity.notFound().build();
        }

        byte[] audioData = agentChatAudioService.getAudio(audioId);
        if (audioData == null) {
            return ResponseEntity.notFound().build();
        }
        redisUtils.delete(RedisKeys.getAgentAudioIdKey(uuid));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"play.wav\"")
                .body(audioData);
    }

    @PostMapping("/tag")
    @Operation(summary = "创建标签")
    @RequiresPermissions("sys:role:normal")
    public Result<AgentTagEntity> createTag(@RequestBody Map<String, String> params) {
        String tagName = params.get("tagName");
        if (StringUtils.isBlank(tagName)) {
            return new Result<AgentTagEntity>().error("标签名称不能为空");
        }
        AgentTagEntity tag = agentTagService.saveTag(tagName);
        return new Result<AgentTagEntity>().ok(tag);
    }

    @GetMapping("/tag/list")
    @Operation(summary = "获取所有标签列表")
    @RequiresPermissions("sys:role:normal")
    public Result<List<AgentTagDTO>> getAllTags() {
        List<AgentTagDTO> tags = agentTagService.getAllTags();
        return new Result<List<AgentTagDTO>>().ok(tags);
    }

    @DeleteMapping("/tag/{id}")
    @Operation(summary = "删除标签")
    @RequiresPermissions("sys:role:normal")
    public Result<Void> deleteTag(@PathVariable String id) {
        agentTagService.deleteTag(id);
        return new Result<Void>().ok(null);
    }

    @GetMapping("/{id}/tags")
    @Operation(summary = "获取智能体的标签")
    @RequiresPermissions("sys:role:normal")
    public Result<List<AgentTagDTO>> getAgentTags(@PathVariable String id) {
        List<AgentTagDTO> tags = agentTagService.getTagsByAgentId(id);
        return new Result<List<AgentTagDTO>>().ok(tags);
    }

    @PutMapping("/{id}/tags")
    @Operation(summary = "保存智能体的标签")
    @RequiresPermissions("sys:role:normal")
    public Result<Void> saveAgentTags(@PathVariable String id, @RequestBody Map<String, Object> params) {
        List<String> tagIds = (List<String>) params.get("tagIds");
        List<String> tagNames = (List<String>) params.get("tagNames");
        agentTagService.saveAgentTags(id, tagIds, tagNames);
        return new Result<Void>().ok(null);
    }

}
