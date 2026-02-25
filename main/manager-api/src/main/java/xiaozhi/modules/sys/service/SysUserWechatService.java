package xiaozhi.modules.sys.service;

import org.springframework.stereotype.Service;

import xiaozhi.modules.sys.entity.SysUserWechatEntity;

@Service
public interface SysUserWechatService {
    Long getUserId(String openid);

    SysUserWechatEntity get(String openid);

    SysUserWechatEntity getByUnionid(String unionid);

    void bind(String openid, String unionid, Long userId);

    void touch(String openid, String unionid);
}