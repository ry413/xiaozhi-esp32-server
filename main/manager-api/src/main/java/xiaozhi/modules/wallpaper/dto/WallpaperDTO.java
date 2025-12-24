package xiaozhi.modules.wallpaper.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "壁纸简单信息")
public class WallpaperDTO {
    
    @Schema(description = "壁纸id")
    private Integer id;

    @Schema(description = "壁纸url")
    private String url;

    @Schema(description = "是否为内置壁纸")
    @Max(1)
    @Min(0)
    private Integer isBuiltin;
}
