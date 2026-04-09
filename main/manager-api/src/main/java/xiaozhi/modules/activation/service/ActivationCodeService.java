package xiaozhi.modules.activation.service;

import xiaozhi.common.page.PageData;
import xiaozhi.common.service.BaseService;
import xiaozhi.modules.activation.dto.ActivationCodeBatchCreateDTO;
import xiaozhi.modules.activation.dto.ActivationCodeBatchPageDTO;
import xiaozhi.modules.activation.dto.ActivationCodePageDTO;
import xiaozhi.modules.activation.dto.UserBenefitLogPageDTO;
import xiaozhi.modules.activation.entity.ActivationCodeEntity;
import xiaozhi.modules.activation.entity.UserBalanceLogEntity;
import xiaozhi.modules.activation.entity.UserMembershipEntity;
import xiaozhi.modules.activation.vo.ActivationCodeBatchVO;
import xiaozhi.modules.activation.vo.ActivationCodeVO;
import xiaozhi.modules.activation.vo.UserBenefitVO;

public interface ActivationCodeService extends BaseService<ActivationCodeEntity> {

    PageData<ActivationCodeBatchVO> pageBatch(ActivationCodeBatchPageDTO dto);

    String createBatchAndGenerateCodes(ActivationCodeBatchCreateDTO dto);

    void updateBatchStatus(String batchNo, Integer status);

    PageData<ActivationCodeVO> pageCode(ActivationCodePageDTO dto);

    void voidCode(String code, String reason);

    void redeemCode(Long userId, String code);

    UserBenefitVO getUserBenefit(Long userId);

    PageData<UserBalanceLogEntity> pageUserBalanceLog(Long userId, UserBenefitLogPageDTO dto);

    PageData<UserMembershipEntity> pageUserMembership(Long userId, UserBenefitLogPageDTO dto);
}
