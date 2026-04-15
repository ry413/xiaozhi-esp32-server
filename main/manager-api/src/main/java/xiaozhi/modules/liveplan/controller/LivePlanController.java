package xiaozhi.modules.liveplan.controller;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import xiaozhi.common.constant.Constant;
import xiaozhi.common.page.PageData;
import xiaozhi.common.utils.Result;
import xiaozhi.common.validator.ValidatorUtils;
import xiaozhi.modules.liveplan.dto.LivePlanAdminPageDTO;
import xiaozhi.modules.liveplan.dto.LivePlanPageDTO;
import xiaozhi.modules.liveplan.dto.LivePlanSaveDTO;
import xiaozhi.modules.liveplan.dto.LivePlanUpdateDTO;
import xiaozhi.modules.liveplan.entity.LivePlanEntity;
import xiaozhi.modules.liveplan.service.LivePlanService;
import xiaozhi.modules.security.user.SecurityUser;

@AllArgsConstructor
@RestController
@Tag(name = "直播方案")
public class LivePlanController {

    private final LivePlanService livePlanService;

    private static final Pattern LIVE_ROOM_ID_PATTERN = Pattern.compile("live\\.douyin\\.com/(\\d+)");
    private static final Pattern WEB_RID_PATTERN = Pattern.compile("\"webRid\":\"(\\d+)\"");
    private static final Pattern WEB_RID_ESCAPED_PATTERN = Pattern.compile("\\\\\"webRid\\\\\":\\\\\"(\\d+)\\\\\"");
    private static final Pattern SHORT_URL_PATTERN = Pattern.compile("https?://v\\.douyin\\.com/[A-Za-z0-9_]+/?");

    @GetMapping("/livePlan")
    @Operation(summary = "我的直播方案分页")
    @RequiresPermissions("sys:role:normal")
    @Parameters({
            @Parameter(name = "planName", description = "方案名称", required = false),
            @Parameter(name = "platform", description = "平台", required = false),
            @Parameter(name = "status", description = "状态:0空闲,1使用中", required = false),
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开始", required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录数", required = true),
    })
    public Result<PageData<LivePlanEntity>> pageMine(@Parameter(hidden = true) @RequestParam Map<String, Object> params) {
        LivePlanPageDTO dto = new LivePlanPageDTO();
        dto.setPlanName((String) params.get("planName"));
        dto.setPlatform((String) params.get("platform"));
        dto.setPage((String) params.get(Constant.PAGE));
        dto.setLimit((String) params.get(Constant.LIMIT));
        Object statusObj = params.get("status");
        if (statusObj != null) {
            dto.setStatus(Integer.parseInt(statusObj.toString()));
        }

        ValidatorUtils.validateEntity(dto);
        return new Result<PageData<LivePlanEntity>>().ok(livePlanService.pageMine(SecurityUser.getUserId(), dto));
    }

    @GetMapping("/livePlan/{planNo}")
    @Operation(summary = "我的直播方案详情")
    @RequiresPermissions("sys:role:normal")
    public Result<LivePlanEntity> getMineById(@PathVariable String planNo) {
        return new Result<LivePlanEntity>().ok(livePlanService.getMineByPlanNo(SecurityUser.getUserId(), planNo));
    }

    @GetMapping("/livePlan/douyinRoomId")
    @Operation(summary = "解析抖音分享链接中的直播间ID")
    @RequiresPermissions("sys:role:normal")
    public Result<String> getDouyinRoomId(@RequestParam String input) {
        if (StringUtils.isBlank(input)) {
            return new Result<String>().error("输入不能为空");
        }

        String trimmedInput = input.trim();
        if (trimmedInput.matches("^\\d{6,}$")) {
            return new Result<String>().ok(trimmedInput);
        }

        Matcher liveMatcher = LIVE_ROOM_ID_PATTERN.matcher(trimmedInput);
        if (liveMatcher.find()) {
            return new Result<String>().ok(liveMatcher.group(1));
        }

        Matcher shortUrlMatcher = SHORT_URL_PATTERN.matcher(trimmedInput);
        if (!shortUrlMatcher.find()) {
            return new Result<String>().error("未能解析出直播间ID");
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(shortUrlMatcher.group()))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String body = response.body();
            if (StringUtils.isBlank(body)) {
                return new Result<String>().error("未能解析出直播间ID");
            }

            String webRid = extractFirstNumericMatch(body, WEB_RID_PATTERN);
            if (StringUtils.isBlank(webRid)) {
                webRid = extractFirstNumericMatch(body, WEB_RID_ESCAPED_PATTERN);
            }
            if (StringUtils.isNotBlank(webRid)) {
                return new Result<String>().ok(webRid);
            }

            Matcher fallbackLiveMatcher = LIVE_ROOM_ID_PATTERN.matcher(body);
            if (fallbackLiveMatcher.find()) {
                return new Result<String>().ok(fallbackLiveMatcher.group(1));
            }

            return new Result<String>().error("未能解析出直播间ID");
        } catch (Exception e) {
            return new Result<String>().error("解析抖音链接失败");
        }
    }

    private String extractFirstNumericMatch(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String value = matcher.group(1);
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    @PostMapping("/livePlan")
    @Operation(summary = "创建直播方案")
    @RequiresPermissions("sys:role:normal")
    public Result<String> save(@RequestBody LivePlanSaveDTO dto) {
        ValidatorUtils.validateEntity(dto);
        return new Result<String>().ok(livePlanService.save(SecurityUser.getUserId(), dto));
    }

    @PutMapping("/livePlan/{planNo}")
    @Operation(summary = "更新直播方案")
    @RequiresPermissions("sys:role:normal")
    public Result<Void> update(@PathVariable String planNo, @RequestBody LivePlanUpdateDTO dto) {
        ValidatorUtils.validateEntity(dto);
        livePlanService.update(SecurityUser.getUserId(), planNo, dto);
        return new Result<>();
    }

    @PutMapping("/livePlan/{planNo}/status/{status}")
    @Operation(summary = "更新直播方案状态")
    @RequiresPermissions("sys:role:normal")
    public Result<Void> updateStatus(@PathVariable String planNo, @PathVariable Integer status) {
        livePlanService.updateStatus(SecurityUser.getUserId(), planNo, status);
        return new Result<>();
    }

    @DeleteMapping("/livePlan/{planNo}")
    @Operation(summary = "删除直播方案")
    @RequiresPermissions("sys:role:normal")
    public Result<Void> delete(@PathVariable String planNo) {
        livePlanService.delete(SecurityUser.getUserId(), planNo);
        return new Result<>();
    }

    @GetMapping("/admin/livePlan")
    @Operation(summary = "管理员分页查询直播方案")
    @RequiresPermissions("sys:role:superAdmin")
    @Parameters({
            @Parameter(name = "userId", description = "用户ID", required = false),
            @Parameter(name = "planName", description = "方案名称", required = false),
            @Parameter(name = "platform", description = "平台", required = false),
            @Parameter(name = "status", description = "状态:0空闲,1使用中", required = false),
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开始", required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录数", required = true),
    })
    public Result<PageData<LivePlanEntity>> pageAdmin(@Parameter(hidden = true) @RequestParam Map<String, Object> params) {
        LivePlanAdminPageDTO dto = new LivePlanAdminPageDTO();
        dto.setPlanName((String) params.get("planName"));
        dto.setPlatform((String) params.get("platform"));
        dto.setPage((String) params.get(Constant.PAGE));
        dto.setLimit((String) params.get(Constant.LIMIT));

        Object userIdObj = params.get("userId");
        if (userIdObj != null) {
            dto.setUserId(Long.parseLong(userIdObj.toString()));
        }
        Object statusObj = params.get("status");
        if (statusObj != null) {
            dto.setStatus(Integer.parseInt(statusObj.toString()));
        }

        ValidatorUtils.validateEntity(dto);
        return new Result<PageData<LivePlanEntity>>().ok(livePlanService.pageAdmin(dto));
    }
}
