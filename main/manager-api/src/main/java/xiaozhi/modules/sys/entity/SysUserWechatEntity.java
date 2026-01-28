package xiaozhi.modules.sys.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_user_wechat")
@Schema(description = "用户微信关联信息")
public class SysUserWechatEntity {

    @TableId(type = IdType.AUTO)
    @Schema(description = "ID")
    private Long id;

    @Schema(description = "应用ID")
    private String appid;

    @Schema(description = "微信OpenID")
    private String openid;

    @Schema(description = "微信UnionID")
    private String unionid;

    @TableField("user_id")
    @Schema(description = "关联用户ID")
    private Long userId;

    @TableField("create_time")
    @Schema(description = "创建时间")
    private Date createDate;

    @TableField("last_login")
    @Schema(description = "最后登录时间")
    private Date lastLoginDate;
}
