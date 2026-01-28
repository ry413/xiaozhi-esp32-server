package xiaozhi.modules.sys.utils;

import lombok.Data;

@Data
public class WechatSession {
    private String openid;
    private String session_key;
    private String unionid;
    private Integer errcode;
    private String errmsg;
}
