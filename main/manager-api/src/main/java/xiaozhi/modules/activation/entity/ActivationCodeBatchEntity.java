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
@TableName("activation_code_batch")
@Schema(description = "激活码批次")
public class ActivationCodeBatchEntity {

    @TableId(type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id;

    @Schema(description = "批次号")
    private String batchNo;

    @Schema(description = "批次名称")
    private String batchName;

    @Schema(description = "卡类型(point/month)")
    private String cardType;

    @Schema(description = "面值")
    private Integer faceValue;

    @Schema(description = "状态:0禁用,1启用")
    private Integer status;

    @Schema(description = "可使用开始时间")
    private Date validFrom;

    @Schema(description = "可使用结束时间")
    private Date validUntil;

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
