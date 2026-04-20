package xiaozhi.modules.activation.controller;

import java.util.Map;

import org.apache.shiro.authz.annotation.RequiresPermissions;
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
import xiaozhi.modules.activation.dto.ActivationCodeBatchCreateDTO;
import xiaozhi.modules.activation.dto.ActivationCodeBatchPageDTO;
import xiaozhi.modules.activation.dto.ActivationCodePageDTO;
import xiaozhi.modules.activation.dto.ActivationCodeVoidDTO;
import xiaozhi.modules.activation.dto.AdminUserBenefitPageDTO;
import xiaozhi.modules.activation.dto.AdminUserBenefitRecordPageDTO;
import xiaozhi.modules.activation.service.ActivationCodeService;
import xiaozhi.modules.activation.vo.AdminUserBalanceLogVO;
import xiaozhi.modules.activation.vo.AdminUserBenefitVO;
import xiaozhi.modules.activation.vo.AdminUserMembershipLogVO;
import xiaozhi.modules.activation.vo.AdminUserMembershipVO;
import xiaozhi.modules.activation.vo.ActivationCodeBatchVO;
import xiaozhi.modules.activation.vo.ActivationCodeVO;

@AllArgsConstructor
@RestController
@RequestMapping("/activation-code/admin")
@Tag(name = "激活码管理-管理员")
public class ActivationCodeAdminController {

    private final ActivationCodeService activationCodeService;

    @GetMapping("/batches")
    @Operation(summary = "分页查询批次")
    @RequiresPermissions("sys:role:superAdmin")
    @Parameters({
            @Parameter(name = "batchNo", description = "批次号", required = false),
            @Parameter(name = "batchName", description = "批次名称", required = false),
            @Parameter(name = "cardType", description = "卡类型(point/month)", required = false),
            @Parameter(name = "status", description = "批次状态:0禁用,1启用", required = false),
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开始", required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录数", required = true),
    })
    public Result<PageData<ActivationCodeBatchVO>> pageBatch(
            @Parameter(hidden = true) @RequestParam Map<String, Object> params) {
        ActivationCodeBatchPageDTO dto = new ActivationCodeBatchPageDTO();
        dto.setBatchNo((String) params.get("batchNo"));
        dto.setBatchName((String) params.get("batchName"));
        dto.setCardType((String) params.get("cardType"));
        dto.setPage((String) params.get(Constant.PAGE));
        dto.setLimit((String) params.get(Constant.LIMIT));
        Object statusObj = params.get("status");
        if (statusObj != null) {
            dto.setStatus(Integer.parseInt(statusObj.toString()));
        }

        ValidatorUtils.validateEntity(dto);
        return new Result<PageData<ActivationCodeBatchVO>>().ok(activationCodeService.pageBatch(dto));
    }

    @PostMapping("/batches")
    @Operation(summary = "创建批次并生成激活码")
    @RequiresPermissions("sys:role:superAdmin")
    public Result<String> createBatch(@RequestBody ActivationCodeBatchCreateDTO dto) {
        ValidatorUtils.validateEntity(dto);
        String batchNo = activationCodeService.createBatchAndGenerateCodes(dto);
        return new Result<String>().ok(batchNo);
    }

    @PutMapping("/batches/{batchNo}/status/{status}")
    @Operation(summary = "修改批次状态")
    @RequiresPermissions("sys:role:superAdmin")
    public Result<Void> updateBatchStatus(@PathVariable String batchNo, @PathVariable Integer status) {
        activationCodeService.updateBatchStatus(batchNo, status);
        return new Result<>();
    }

    @GetMapping("/codes")
    @Operation(summary = "分页查询激活码")
    @RequiresPermissions("sys:role:superAdmin")
    @Parameters({
            @Parameter(name = "batchNo", description = "批次号", required = false),
            @Parameter(name = "code", description = "激活码(模糊查询)", required = false),
            @Parameter(name = "status", description = "激活码状态:0未使用,1已使用,2已作废,3已过期", required = false),
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开始", required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录数", required = true),
    })
    public Result<PageData<ActivationCodeVO>> pageCode(
            @Parameter(hidden = true) @RequestParam Map<String, Object> params) {
        ActivationCodePageDTO dto = new ActivationCodePageDTO();
        dto.setCode((String) params.get("code"));
        dto.setPage((String) params.get(Constant.PAGE));
        dto.setLimit((String) params.get(Constant.LIMIT));

        dto.setBatchNo((String) params.get("batchNo"));

        Object statusObj = params.get("status");
        if (statusObj != null) {
            dto.setStatus(Integer.parseInt(statusObj.toString()));
        }

        ValidatorUtils.validateEntity(dto);
        return new Result<PageData<ActivationCodeVO>>().ok(activationCodeService.pageCode(dto));
    }

    @PutMapping("/codes/{code}/void")
    @Operation(summary = "作废激活码")
    @RequiresPermissions("sys:role:superAdmin")
    public Result<Void> voidCode(@PathVariable String code, @RequestBody ActivationCodeVoidDTO dto) {
        ValidatorUtils.validateEntity(dto);
        activationCodeService.voidCode(code, dto.getReason());
        return new Result<>();
    }

    @GetMapping("/users/benefits")
    @Operation(summary = "分页查询用户权益摘要")
    @RequiresPermissions("sys:role:superAdmin")
    public Result<PageData<AdminUserBenefitVO>> pageUserBenefits(
            @Parameter(hidden = true) @RequestParam Map<String, Object> params) {
        AdminUserBenefitPageDTO dto = new AdminUserBenefitPageDTO();
        dto.setKeyword((String) params.get("keyword"));
        dto.setPage((String) params.get(Constant.PAGE));
        dto.setLimit((String) params.get(Constant.LIMIT));
        ValidatorUtils.validateEntity(dto);
        return new Result<PageData<AdminUserBenefitVO>>().ok(activationCodeService.pageAdminUserBenefit(dto));
    }

    @GetMapping("/users/balance-logs")
    @Operation(summary = "分页查询用户点卡流水")
    @RequiresPermissions("sys:role:superAdmin")
    public Result<PageData<AdminUserBalanceLogVO>> pageUserBalanceLogs(
            @Parameter(hidden = true) @RequestParam Map<String, Object> params) {
        AdminUserBenefitRecordPageDTO dto = buildAdminRecordPageDTO(params);
        return new Result<PageData<AdminUserBalanceLogVO>>().ok(activationCodeService.pageAdminUserBalanceLog(dto));
    }

    @GetMapping("/users/memberships")
    @Operation(summary = "分页查询用户月卡权益")
    @RequiresPermissions("sys:role:superAdmin")
    public Result<PageData<AdminUserMembershipVO>> pageUserMemberships(
            @Parameter(hidden = true) @RequestParam Map<String, Object> params) {
        AdminUserBenefitRecordPageDTO dto = buildAdminRecordPageDTO(params);
        return new Result<PageData<AdminUserMembershipVO>>().ok(activationCodeService.pageAdminUserMembership(dto));
    }

    @GetMapping("/users/membership-logs")
    @Operation(summary = "分页查询用户月卡流水")
    @RequiresPermissions("sys:role:superAdmin")
    public Result<PageData<AdminUserMembershipLogVO>> pageUserMembershipLogs(
            @Parameter(hidden = true) @RequestParam Map<String, Object> params) {
        AdminUserBenefitRecordPageDTO dto = buildAdminRecordPageDTO(params);
        return new Result<PageData<AdminUserMembershipLogVO>>().ok(activationCodeService.pageAdminUserMembershipLog(dto));
    }

    private AdminUserBenefitRecordPageDTO buildAdminRecordPageDTO(Map<String, Object> params) {
        AdminUserBenefitRecordPageDTO dto = new AdminUserBenefitRecordPageDTO();
        dto.setKeyword((String) params.get("keyword"));
        dto.setPage((String) params.get(Constant.PAGE));
        dto.setLimit((String) params.get(Constant.LIMIT));
        dto.setChangeType((String) params.get("changeType"));
        dto.setSourceType((String) params.get("sourceType"));
        Object userId = params.get("userId");
        if (userId != null && !userId.toString().isBlank()) {
            dto.setUserId(Long.parseLong(userId.toString()));
        }
        Object status = params.get("status");
        if (status != null && !status.toString().isBlank()) {
            dto.setStatus(Integer.parseInt(status.toString()));
        }
        ValidatorUtils.validateEntity(dto);
        return dto;
    }
}
