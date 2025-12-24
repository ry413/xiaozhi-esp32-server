package xiaozhi.modules.wallpaper.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
// @AllArgsConstructor
@Schema(description = "壁纸简单信息")
public class WallpaperSimpleDTO {
    
    @Schema(description = "壁纸id")
    private Integer id;

    @Schema(description = "壁纸url")
    private String url;
}
