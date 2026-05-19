package xiaozhi.modules.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "设备自动启动方案设置")
public class DeviceAutoStartPlanDTO {

    @Size(max = 32)
    @Schema(description = "方案编号", example = "LPTQHZGMKTLYGDJG")
    private String planNo;
}
