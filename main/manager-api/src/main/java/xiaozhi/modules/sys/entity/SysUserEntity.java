package xiaozhi.modules.sys.entity;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import lombok.Data;
import lombok.EqualsAndHashCode;
import xiaozhi.common.entity.BaseEntity;

/**
 * 系统用户
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "sys_user", autoResultMap = true)
public class SysUserEntity extends BaseEntity {
    /**
     * 用户名
     */
    private String username;
    /**
     * 密码
     */
    private String password;
    /**
     * 超级管理员 0：否 1：是
     */
    private Integer superAdmin;
    /**
     * 状态 0：停用 1：正常
     */
    private Integer status;
    /**
     * 更新者
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updater;
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateDate;
    /**
     * 隐藏的内置壁纸id集
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Integer> hiddenBuiltinWallpaperIds;

}