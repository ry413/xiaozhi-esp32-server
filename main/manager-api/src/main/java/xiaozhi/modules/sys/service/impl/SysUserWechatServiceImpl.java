package xiaozhi.modules.sys.service.impl;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;
import xiaozhi.modules.sys.dao.SysUserWechatDao;
import xiaozhi.modules.sys.entity.SysUserWechatEntity;
import xiaozhi.modules.sys.service.SysUserWechatService;

@Slf4j
@Service
public class SysUserWechatServiceImpl
        extends ServiceImpl<SysUserWechatDao, SysUserWechatEntity>
        implements SysUserWechatService {

    @Value("${wechat.mini.appid}")
    private String appid;

    @Override
    public Long getUserId(String openid) {
        SysUserWechatEntity e = get(openid);
        return e == null ? null : e.getUserId();
    }

    @Override
    public SysUserWechatEntity get(String openid) {
        QueryWrapper<SysUserWechatEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("appid", appid)
                .eq("openid", openid);
        return getOne(wrapper);
    }

    @Override
    public SysUserWechatEntity getByUnionid(String unionid) {
        if (unionid == null || unionid.isBlank())
            return null;
        QueryWrapper<SysUserWechatEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("unionid", unionid);
        return getOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bind(String openid, String unionid, Long userId) {
        SysUserWechatEntity exist = get(openid);
        if (exist != null) {
            updateLoginInfo(exist.getId(), unionid);
            return;
        }

        SysUserWechatEntity entity = new SysUserWechatEntity();
        entity.setAppid(appid);
        entity.setOpenid(openid);
        entity.setUnionid(isBlank(unionid) ? null : unionid);
        entity.setUserId(userId);
        entity.setCreateDate(new Date());
        entity.setLastLoginDate(new Date());

        try {
            save(entity);
        } catch (Exception e) {
            SysUserWechatEntity now = get(openid);
            if (now == null)
                throw e;
            updateLoginInfo(now.getId(), unionid);
        }
    }

    @Override
    public void touch(String openid, String unionid) {
        log.info("Touch wechat user info for openid: {}", openid);
        SysUserWechatEntity exist = get(openid);
        if (exist == null) {
            log.warn("Cannot touch wechat user info: no such openid {}", openid);
            return;
        }
        updateLoginInfo(exist.getId(), unionid);
    }

    private void updateLoginInfo(Long id, String unionid) {
        UpdateWrapper<SysUserWechatEntity> w1 = new UpdateWrapper<>();
        w1.eq("id", id).set("last_login", new Date());
        update(w1);

        // 补写 unionid
        if (!isBlank(unionid)) {
            UpdateWrapper<SysUserWechatEntity> w2 = new UpdateWrapper<>();
            w2.eq("id", id)
                    .and(w -> w.isNull("unionid").or().eq("unionid", ""))
                    .set("unionid", unionid);
            update(w2);
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
