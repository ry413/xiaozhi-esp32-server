package xiaozhi.modules.liveplan.entity;

import java.util.Date;
import java.util.Map;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName(value = "live_plan", autoResultMap = true)
@Schema(description = "直播方案")
public class LivePlanEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    @Schema(description = "方案ID")
    private String id;

    @Schema(description = "方案编号(对外业务号)")
    private String planNo;

    @Schema(description = "关联用户ID")
    private Long userId;

    @Schema(description = "方案名称")
    private String planName;

    @Schema(description = "直播平台")
    private String platform;

    @Schema(description = "直播间ID")
    private String roomId;

    @TableField(typeHandler = JacksonTypeHandler.class)
    @Schema(description = "方案配置JSON")
    private Map<String, Object> configJson;

    @Schema(description = "状态: 0=空闲, 1=使用中")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建者")
    @TableField(fill = FieldFill.INSERT)
    private Long creator;

    @Schema(description = "更新者")
    @TableField(fill = FieldFill.UPDATE)
    private Long updater;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private Date createDate;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.UPDATE)
    private Date updateDate;
}
