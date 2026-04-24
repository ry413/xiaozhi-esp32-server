package xiaozhi.modules.activation.service;

import xiaozhi.common.page.PageData;
import xiaozhi.common.service.BaseService;
import xiaozhi.modules.activation.dto.ActivationCodeBatchCreateDTO;
import xiaozhi.modules.activation.dto.ActivationCodeBatchPageDTO;
import xiaozhi.modules.activation.dto.ActivationCodePageDTO;
import xiaozhi.modules.activation.dto.AdminUserBenefitPageDTO;
import xiaozhi.modules.activation.dto.AdminUserBenefitRecordPageDTO;
import xiaozhi.modules.activation.dto.UserBalanceConsumeDTO;
import xiaozhi.modules.activation.dto.UserBenefitLogPageDTO;
import xiaozhi.modules.activation.entity.ActivationCodeEntity;
import xiaozhi.modules.activation.entity.UserBalanceLogEntity;
import xiaozhi.modules.activation.entity.UserMembershipEntity;
import xiaozhi.modules.activation.vo.AdminUserBalanceLogVO;
import xiaozhi.modules.activation.vo.AdminUserBenefitVO;
import xiaozhi.modules.activation.vo.AdminUserMembershipLogVO;
import xiaozhi.modules.activation.vo.AdminUserMembershipVO;
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

    void consumeUserBenefit(Long userId, UserBalanceConsumeDTO dto);

    PageData<UserBalanceLogEntity> pageUserBalanceLog(Long userId, UserBenefitLogPageDTO dto);

    PageData<UserMembershipEntity> pageUserMembership(Long userId, UserBenefitLogPageDTO dto);

    PageData<AdminUserBenefitVO> pageAdminUserBenefit(AdminUserBenefitPageDTO dto);

    PageData<AdminUserBalanceLogVO> pageAdminUserBalanceLog(AdminUserBenefitRecordPageDTO dto);

    PageData<AdminUserMembershipVO> pageAdminUserMembership(AdminUserBenefitRecordPageDTO dto);

    PageData<AdminUserMembershipLogVO> pageAdminUserMembershipLog(AdminUserBenefitRecordPageDTO dto);
}
