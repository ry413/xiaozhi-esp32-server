package xiaozhi.modules.activation.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("activation_code")
@Schema(description = "激活码")
public class ActivationCodeEntity {

    @TableId(type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id;

    @Schema(description = "批次ID")
    private Long batchId;

    @Schema(description = "激活码")
    private String code;

    @Schema(description = "状态:0未使用,1已使用,2已作废,3已过期")
    private Integer status;

    @Schema(description = "可使用开始时间")
    private Date validFrom;

    @Schema(description = "可使用结束时间")
    private Date validUntil;

    @Schema(description = "使用者用户ID")
    private Long usedUserId;

    @Schema(description = "使用时间")
    private Date usedAt;

    @Schema(description = "作废时间")
    private Date voidAt;

    @Schema(description = "作废原因")
    private String voidReason;

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
