package xiaozhi.modules.activation.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import xiaozhi.common.constant.Constant;
import xiaozhi.common.page.PageData;
import xiaozhi.common.exception.ErrorCode;
import xiaozhi.common.utils.Result;
import xiaozhi.common.validator.ValidatorUtils;
import xiaozhi.modules.activation.dto.ActivationCodeRedeemDTO;
import xiaozhi.modules.activation.dto.UserBalanceConsumeDTO;
import xiaozhi.modules.activation.dto.UserBenefitLogPageDTO;
import xiaozhi.modules.activation.entity.UserBalanceLogEntity;
import xiaozhi.modules.activation.entity.UserMembershipEntity;
import xiaozhi.modules.activation.service.ActivationCodeService;
import xiaozhi.modules.activation.vo.UserBenefitVO;
import xiaozhi.modules.agent.entity.AgentEntity;
import xiaozhi.modules.agent.service.AgentService;
import xiaozhi.modules.device.entity.DeviceEntity;
import xiaozhi.modules.device.service.DeviceService;
import xiaozhi.modules.model.entity.ModelConfigEntity;
import xiaozhi.modules.model.service.ModelConfigService;
import xiaozhi.modules.security.user.SecurityUser;

@AllArgsConstructor
@RestController
@RequestMapping("/activation-code")
@Tag(name = "激活码-用户权益")
public class ActivationCodeController {

    private static final BigDecimal DEFAULT_CONSUME_MULTIPLIER = BigDecimal.ONE;

    private final ActivationCodeService activationCodeService;
    private final DeviceService deviceService;
    private final AgentService agentService;
    private final ModelConfigService modelConfigService;

    @PostMapping("/redeem")
    @Operation(summary = "兑换激活码")
    @RequiresPermissions("sys:role:normal")
    public Result<Void> redeem(@RequestBody ActivationCodeRedeemDTO dto) {
        ValidatorUtils.validateEntity(dto);
        activationCodeService.redeemCode(SecurityUser.getUserId(), dto.getCode());
        return new Result<>();
    }

    @GetMapping("/me/benefits")
    @Operation(summary = "查询我的权益摘要")
    @RequiresPermissions("sys:role:normal")
    public Result<UserBenefitVO> getMyBenefit() {
        return new Result<UserBenefitVO>().ok(activationCodeService.getUserBenefit(SecurityUser.getUserId()));
    }

    @GetMapping("/device/{deviceId}/benefits")
    @Operation(summary = "按设备查询所属用户权益摘要")
    public Result<UserBenefitVO> getDeviceOwnerBenefit(@org.springframework.web.bind.annotation.PathVariable String deviceId) {
        DeviceEntity device = deviceService.selectById(deviceId);
        if (device == null || device.getUserId() == null) {
            return new Result<UserBenefitVO>().error(ErrorCode.DEVICE_NOT_EXIST);
        }

        return new Result<UserBenefitVO>().ok(activationCodeService.getUserBenefit(device.getUserId()));
    }

    @PostMapping("/device/{deviceId}/benefit/consume")
    @Operation(summary = "按设备消费所属用户权益")
    public Result<UserBenefitVO> consumeDeviceOwnerBenefit(@org.springframework.web.bind.annotation.PathVariable String deviceId,
            @Valid @RequestBody UserBalanceConsumeDTO dto) {
        DeviceEntity device = deviceService.selectById(deviceId);
        if (device == null || device.getUserId() == null) {
            return new Result<UserBenefitVO>().error(ErrorCode.DEVICE_NOT_EXIST);
        }

        UserBalanceConsumeDTO consumeDTO = buildAdjustedConsumeDTO(device, dto);
        return new Result<UserBenefitVO>().ok(activationCodeService.consumeUserBenefit(device.getUserId(), consumeDTO));
    }

    @GetMapping("/me/balance/logs")
    @Operation(summary = "查询我的余额流水")
    @RequiresPermissions("sys:role:normal")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开始", required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录数", required = true),
    })
    public Result<PageData<UserBalanceLogEntity>> pageMyBalanceLogs(
            @Parameter(hidden = true) @RequestParam Map<String, Object> params) {
        UserBenefitLogPageDTO dto = new UserBenefitLogPageDTO();
        dto.setPage((String) params.get(Constant.PAGE));
        dto.setLimit((String) params.get(Constant.LIMIT));
        ValidatorUtils.validateEntity(dto);

        return new Result<PageData<UserBalanceLogEntity>>()
                .ok(activationCodeService.pageUserBalanceLog(SecurityUser.getUserId(), dto));
    }

    @GetMapping("/me/memberships")
    @Operation(summary = "查询我的月卡权益")
    @RequiresPermissions("sys:role:normal")
    @Parameters({
            @Parameter(name = Constant.PAGE, description = "当前页码，从1开始", required = true),
            @Parameter(name = Constant.LIMIT, description = "每页显示记录数", required = true),
    })
    public Result<PageData<UserMembershipEntity>> pageMyMemberships(
            @Parameter(hidden = true) @RequestParam Map<String, Object> params) {
        UserBenefitLogPageDTO dto = new UserBenefitLogPageDTO();
        dto.setPage((String) params.get(Constant.PAGE));
        dto.setLimit((String) params.get(Constant.LIMIT));
        ValidatorUtils.validateEntity(dto);

        return new Result<PageData<UserMembershipEntity>>()
                .ok(activationCodeService.pageUserMembership(SecurityUser.getUserId(), dto));
    }

    private UserBalanceConsumeDTO buildAdjustedConsumeDTO(DeviceEntity device, UserBalanceConsumeDTO dto) {
        BigDecimal multiplier = getConsumeMultiplier(device);
        if (DEFAULT_CONSUME_MULTIPLIER.compareTo(multiplier) == 0) {
            return dto;
        }

        UserBalanceConsumeDTO adjustedDTO = new UserBalanceConsumeDTO();
        adjustedDTO.setSeconds(applyConsumeMultiplier(dto.getSeconds(), multiplier));
        adjustedDTO.setSourceType(dto.getSourceType());
        adjustedDTO.setSourceId(dto.getSourceId());
        adjustedDTO.setRemark(dto.getRemark());
        return adjustedDTO;
    }

    private BigDecimal getConsumeMultiplier(DeviceEntity device) {
        if (device == null || device.getAgentId() == null || device.getAgentId().isBlank()) {
            return DEFAULT_CONSUME_MULTIPLIER;
        }

        AgentEntity agent = agentService.selectById(device.getAgentId());
        if (agent == null || agent.getTtsModelId() == null || agent.getTtsModelId().isBlank()) {
            return DEFAULT_CONSUME_MULTIPLIER;
        }

        ModelConfigEntity ttsModel = modelConfigService.getModelByIdFromCache(agent.getTtsModelId());
        if (ttsModel == null || ttsModel.getBenefitConsumeMultiplier() == null
                || ttsModel.getBenefitConsumeMultiplier().compareTo(BigDecimal.ZERO) <= 0
                || !"tts".equalsIgnoreCase(ttsModel.getModelType())) {
            return DEFAULT_CONSUME_MULTIPLIER;
        }
        return ttsModel.getBenefitConsumeMultiplier();
    }

    private int applyConsumeMultiplier(Integer seconds, BigDecimal multiplier) {
        BigDecimal adjustedSeconds = BigDecimal.valueOf(seconds).multiply(multiplier);
        return adjustedSeconds.setScale(0, RoundingMode.CEILING).intValueExact();
    }
}
