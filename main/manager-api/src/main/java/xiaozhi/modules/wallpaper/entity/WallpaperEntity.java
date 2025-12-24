package xiaozhi.modules.wallpaper.entity;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_wallpapers")
@Schema(description = "壁纸信息")
public class WallpaperEntity {

    @TableId(type = IdType.AUTO)
    @Schema(description = "ID")
    private Integer id;

    @Schema(description = "关联用户ID")
    private Long userId;

    @Schema(description = "是否为内置壁纸(0不是/1是)")
    private Integer isBuiltin;

    @Schema(description = "壁纸访问URL")
    private String url;

    @Schema(description = "上传时间")
    @TableField(fill = FieldFill.INSERT)
    private Date createDate;
}
