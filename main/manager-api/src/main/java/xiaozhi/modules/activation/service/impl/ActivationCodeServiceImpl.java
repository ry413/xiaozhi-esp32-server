package xiaozhi.modules.activation.service.impl;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import cn.hutool.core.util.RandomUtil;
import lombok.AllArgsConstructor;
import xiaozhi.common.constant.Constant;
import xiaozhi.common.exception.RenException;
import xiaozhi.common.page.PageData;
import xiaozhi.common.service.impl.BaseServiceImpl;
import xiaozhi.common.utils.ConvertUtils;
import xiaozhi.modules.activation.dao.ActivationCodeBatchDao;
import xiaozhi.modules.activation.dao.ActivationCodeDao;
import xiaozhi.modules.activation.dao.UserBalanceAccountDao;
import xiaozhi.modules.activation.dao.UserBalanceLogDao;
import xiaozhi.modules.activation.dao.UserMembershipDailyUsageDao;
import xiaozhi.modules.activation.dao.UserMembershipDao;
import xiaozhi.modules.activation.dao.UserMembershipLogDao;
import xiaozhi.modules.activation.dto.ActivationCodeBatchCreateDTO;
import xiaozhi.modules.activation.dto.ActivationCodeBatchPageDTO;
import xiaozhi.modules.activation.dto.ActivationCodePageDTO;
import xiaozhi.modules.activation.dto.AdminUserBenefitPageDTO;
import xiaozhi.modules.activation.dto.AdminUserBenefitRecordPageDTO;
import xiaozhi.modules.activation.dto.UserBalanceConsumeDTO;
import xiaozhi.modules.activation.dto.UserBenefitLogPageDTO;
import xiaozhi.modules.activation.entity.ActivationCodeBatchEntity;
import xiaozhi.modules.activation.entity.ActivationCodeEntity;
import xiaozhi.modules.activation.entity.UserBalanceAccountEntity;
import xiaozhi.modules.activation.entity.UserBalanceLogEntity;
import xiaozhi.modules.activation.entity.UserMembershipDailyUsageEntity;
import xiaozhi.modules.activation.entity.UserMembershipEntity;
import xiaozhi.modules.activation.entity.UserMembershipLogEntity;
import xiaozhi.modules.activation.service.ActivationCodeService;
import xiaozhi.modules.activation.vo.AdminUserBalanceLogVO;
import xiaozhi.modules.activation.vo.AdminUserBenefitVO;
import xiaozhi.modules.activation.vo.AdminUserMembershipLogVO;
import xiaozhi.modules.activation.vo.AdminUserMembershipVO;
import xiaozhi.modules.activation.vo.ActivationCodeBatchVO;
import xiaozhi.modules.activation.vo.ActivationCodeVO;
import xiaozhi.modules.activation.vo.UserBenefitVO;
import xiaozhi.modules.sys.dao.SysUserDao;
import xiaozhi.modules.sys.entity.SysUserEntity;

@Service
@AllArgsConstructor
public class ActivationCodeServiceImpl extends BaseServiceImpl<ActivationCodeDao, ActivationCodeEntity>
        implements ActivationCodeService {

    private static final String CARD_TYPE_POINT = "point";
    private static final String CARD_TYPE_MONTH = "month";
    private static final String CHARGED_FROM_MEMBERSHIP_DAILY_QUOTA = "membership_daily_quota";
    private static final String CHARGED_FROM_BALANCE = "balance";
    private static final int MEMBERSHIP_DAILY_LIMIT_SECONDS = 4 * 60 * 60;

    private final ActivationCodeDao activationCodeDao;
    private final ActivationCodeBatchDao activationCodeBatchDao;
    private final UserBalanceAccountDao userBalanceAccountDao;
    private final UserBalanceLogDao userBalanceLogDao;
    private final UserMembershipDailyUsageDao userMembershipDailyUsageDao;
    private final UserMembershipDao userMembershipDao;
    private final UserMembershipLogDao userMembershipLogDao;
    private final SysUserDao sysUserDao;

    @Override
    public PageData<ActivationCodeBatchVO> pageBatch(ActivationCodeBatchPageDTO dto) {
        IPage<ActivationCodeBatchEntity> pageInfo = buildPage(dto.getPage(), dto.getLimit(), "id");

        IPage<ActivationCodeBatchEntity> page = activationCodeBatchDao.selectPage(
                pageInfo,
                new QueryWrapper<ActivationCodeBatchEntity>()
                        .like(StringUtils.isNotBlank(dto.getBatchNo()), "batch_no", dto.getBatchNo())
                        .like(StringUtils.isNotBlank(dto.getBatchName()), "batch_name", dto.getBatchName())
                        .eq(StringUtils.isNotBlank(dto.getCardType()), "card_type", dto.getCardType())
                        .eq(dto.getStatus() != null, "status", dto.getStatus()));

        List<ActivationCodeBatchVO> records = ConvertUtils.sourceToTarget(page.getRecords(), ActivationCodeBatchVO.class);
        fillBatchGeneratedCount(records);
        return new PageData<>(records, page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createBatchAndGenerateCodes(ActivationCodeBatchCreateDTO dto) {
        validateBatchCreateDTO(dto);

        Long count = activationCodeBatchDao.selectCount(
                new QueryWrapper<ActivationCodeBatchEntity>().eq("batch_no", dto.getBatchNo()));
        if (count != null && count > 0) {
            throw new RenException("批次号已存在");
        }

        ActivationCodeBatchEntity batchEntity = ConvertUtils.sourceToTarget(dto, ActivationCodeBatchEntity.class);
        if (batchEntity.getStatus() == null) {
            batchEntity.setStatus(1);
        }
        activationCodeBatchDao.insert(batchEntity);

        List<ActivationCodeEntity> codes = buildActivationCodes(batchEntity, dto.getGenerateCount());
        if (!codes.isEmpty()) {
            insertBatch(codes, 500);
        }

        return batchEntity.getBatchNo();
    }

    @Override
    public void updateBatchStatus(String batchNo, Integer status) {
        if (StringUtils.isBlank(batchNo)) {
            throw new RenException("batchNo不能为空");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new RenException("状态只允许为0或1");
        }

        ActivationCodeBatchEntity entity = activationCodeBatchDao.selectOne(
                new QueryWrapper<ActivationCodeBatchEntity>().eq("batch_no", batchNo));
        if (entity == null) {
            throw new RenException("批次不存在");
        }

        entity.setStatus(status);
        activationCodeBatchDao.updateById(entity);
    }

    @Override
    public PageData<ActivationCodeVO> pageCode(ActivationCodePageDTO dto) {
        Map<String, Object> params = new HashMap<>();
        params.put(Constant.PAGE, dto.getPage());
        params.put(Constant.LIMIT, dto.getLimit());

        IPage<ActivationCodeEntity> page = activationCodeDao.selectPage(
                getPage(params, "id", false),
                buildCodePageQuery(dto));

        List<ActivationCodeVO> records = ConvertUtils.sourceToTarget(page.getRecords(), ActivationCodeVO.class);
        fillCodeBatchInfo(records);
        return new PageData<>(records, page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidCode(String code, String reason) {
        if (StringUtils.isBlank(code)) {
            throw new RenException("激活码不能为空");
        }
        if (StringUtils.isBlank(reason)) {
            throw new RenException("作废原因不能为空");
        }

        String normalizedCode = code.trim().toUpperCase();
        ActivationCodeEntity codeEntity = activationCodeDao.selectOne(
                new QueryWrapper<ActivationCodeEntity>().eq("code", normalizedCode));
        if (codeEntity == null) {
            throw new RenException("激活码不存在");
        }
        if (Integer.valueOf(1).equals(codeEntity.getStatus())) {
            throw new RenException("激活码已使用，不能作废");
        }

        Date now = new Date();
        UpdateWrapper<ActivationCodeEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", codeEntity.getId())
                .ne("status", 1)
                .set("status", 2)
                .set("void_at", now)
                .set("void_reason", reason)
                .set("update_date", now);

        int rows = activationCodeDao.update(null, updateWrapper);
        if (rows <= 0) {
            throw new RenException("作废激活码失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void redeemCode(Long userId, String code) {
        if (userId == null) {
            throw new RenException("用户未登录");
        }
        if (StringUtils.isBlank(code)) {
            throw new RenException("激活码不能为空");
        }

        String normalizedCode = code.trim().toUpperCase();
        ActivationCodeEntity codeEntity = activationCodeDao.selectOne(
                new QueryWrapper<ActivationCodeEntity>().eq("code", normalizedCode));
        if (codeEntity == null) {
            throw new RenException("激活码不存在");
        }

        Date now = new Date();
        validateCodeUsable(codeEntity, now);

        ActivationCodeBatchEntity batchEntity = activationCodeBatchDao.selectById(codeEntity.getBatchId());
        if (batchEntity == null) {
            throw new RenException("激活码批次不存在");
        }
        validateBatchUsable(batchEntity, now);

        int locked = activationCodeDao.update(null, new UpdateWrapper<ActivationCodeEntity>()
                .eq("id", codeEntity.getId())
                .eq("status", 0)
                .set("status", 1)
                .set("used_user_id", userId)
                .set("used_at", now)
                .set("update_date", now));

        if (locked <= 0) {
            throw new RenException("激活码已被使用或已失效");
        }

        if (CARD_TYPE_POINT.equalsIgnoreCase(batchEntity.getCardType())) {
            applyPointBenefit(userId, codeEntity.getId(), batchEntity.getFaceValue(), now);
            return;
        }
        if (CARD_TYPE_MONTH.equalsIgnoreCase(batchEntity.getCardType())) {
            applyMembershipBenefit(userId, codeEntity.getId(), batchEntity.getFaceValue(), now);
            return;
        }

        throw new RenException("不支持的卡类型: " + batchEntity.getCardType());
    }

    @Override
    public UserBenefitVO getUserBenefit(Long userId) {
        UserBenefitVO vo = new UserBenefitVO();
        vo.setUserId(userId);

        UserBalanceAccountEntity accountEntity = userBalanceAccountDao.selectOne(
                new QueryWrapper<UserBalanceAccountEntity>().eq("user_id", userId));
        vo.setBalanceSeconds(accountEntity == null ? 0 : safeInt(accountEntity.getBalanceMinutes()));

        Date now = new Date();
        UserMembershipEntity membership = getActiveMembership(userId, now);

        if (membership == null) {
            vo.setMembershipActive(false);
            vo.setMembershipDailyLimitSeconds(0);
            vo.setMembershipDailyConsumedSeconds(0);
            vo.setMembershipDailyRemainingSeconds(0);
            return vo;
        }

        vo.setMembershipActive(true);
        vo.setMembershipStartAt(membership.getStartAt());
        vo.setMembershipEndAt(membership.getEndAt());
        UserMembershipDailyUsageEntity dailyUsage = getMembershipDailyUsage(userId, LocalDate.now());
        int dailyLimitSeconds = dailyUsage == null ? MEMBERSHIP_DAILY_LIMIT_SECONDS : safeInt(dailyUsage.getDailyLimitSeconds());
        if (dailyLimitSeconds <= 0) {
            dailyLimitSeconds = MEMBERSHIP_DAILY_LIMIT_SECONDS;
        }
        int consumedSeconds = dailyUsage == null ? 0 : safeInt(dailyUsage.getConsumedSeconds());
        vo.setMembershipDailyLimitSeconds(dailyLimitSeconds);
        vo.setMembershipDailyConsumedSeconds(consumedSeconds);
        vo.setMembershipDailyRemainingSeconds(Math.max(0, dailyLimitSeconds - consumedSeconds));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserBenefitVO consumeUserBenefit(Long userId, UserBalanceConsumeDTO dto) {
        if (userId == null) {
            throw new RenException("用户不存在");
        }
        if (dto == null || dto.getSeconds() == null || dto.getSeconds() <= 0) {
            throw new RenException("消费秒数必须大于0");
        }

        Date now = new Date();
        int remainingSeconds = dto.getSeconds();
        int membershipConsumedSeconds = 0;
        int balanceConsumedSeconds = 0;

        UserMembershipEntity membership = getActiveMembership(userId, now);
        if (membership != null) {
            membershipConsumedSeconds = consumeUserMembershipDailyQuota(userId, membership, remainingSeconds);
            remainingSeconds -= membershipConsumedSeconds;
        }

        if (remainingSeconds > 0) {
            balanceConsumedSeconds = consumeUserBalance(userId, dto, remainingSeconds, now);
        }

        if (membershipConsumedSeconds + balanceConsumedSeconds <= 0) {
            throw new RenException("用户权益余额不足");
        }

        UserBenefitVO snapshot = getUserBenefit(userId);
        snapshot.setChargedFrom(balanceConsumedSeconds > 0 ? CHARGED_FROM_BALANCE : CHARGED_FROM_MEMBERSHIP_DAILY_QUOTA);
        return snapshot;
    }

    private int consumeUserMembershipDailyQuota(Long userId, UserMembershipEntity membership, int seconds) {
        LocalDate usageDate = LocalDate.now();
        userMembershipDailyUsageDao.insertIgnoreDailyUsage(userId, membership.getId(), usageDate,
                MEMBERSHIP_DAILY_LIMIT_SECONDS);

        UserMembershipDailyUsageEntity usage = userMembershipDailyUsageDao.selectOne(
                new QueryWrapper<UserMembershipDailyUsageEntity>()
                        .eq("user_id", userId)
                        .eq("usage_date", usageDate)
                        .last("limit 1 for update"));
        if (usage == null || !Integer.valueOf(1).equals(usage.getStatus())) {
            return 0;
        }

        int before = safeInt(usage.getConsumedSeconds());
        int limit = safeInt(usage.getDailyLimitSeconds());
        if (limit <= 0) {
            limit = MEMBERSHIP_DAILY_LIMIT_SECONDS;
        }
        if (before >= limit) {
            return 0;
        }

        int actualDelta = Math.min(limit - before, seconds);
        int after = before + actualDelta;
        int updated = userMembershipDailyUsageDao.update(null,
                new UpdateWrapper<UserMembershipDailyUsageEntity>()
                        .eq("id", usage.getId())
                        .eq("status", 1)
                        .eq("consumed_seconds", before)
                        .set("membership_id", membership.getId())
                        .set("daily_limit_seconds", limit)
                        .set("consumed_seconds", after)
                        .set("updated_at", new Date()));
        if (updated <= 0) {
            throw new RenException("月卡每日额度扣减失败");
        }
        return actualDelta;
    }

    private int consumeUserBalance(Long userId, UserBalanceConsumeDTO dto, int seconds, Date now) {
        UserBalanceAccountEntity account = userBalanceAccountDao.selectOne(
                new QueryWrapper<UserBalanceAccountEntity>().eq("user_id", userId).last("limit 1 for update"));

        if (account == null || !Integer.valueOf(1).equals(account.getStatus())) {
            return 0;
        }

        int before = safeInt(account.getBalanceMinutes());
        if (before <= 0) {
            return 0;
        }

        int actualDelta = Math.min(before, seconds);
        int after = before - actualDelta;
        int updated = userBalanceAccountDao.update(null, new UpdateWrapper<UserBalanceAccountEntity>()
                .eq("id", account.getId())
                .eq("status", 1)
                .ge("balance_minutes", actualDelta)
                .set("balance_minutes", after)
                .set("total_consumed_minutes", safeInt(account.getTotalConsumedMinutes()) + actualDelta)
                .set("updated_at", now));
        if (updated <= 0) {
            throw new RenException("点卡余额扣减失败");
        }

        UserBalanceLogEntity logEntity = new UserBalanceLogEntity();
        logEntity.setUserId(userId);
        logEntity.setAccountId(account.getId());
        logEntity.setChangeType("consume");
        logEntity.setDeltaMinutes(-actualDelta);
        logEntity.setBalanceBefore(before);
        logEntity.setBalanceAfter(after);
        logEntity.setSourceType(StringUtils.defaultIfBlank(dto.getSourceType(), "device"));
        logEntity.setSourceId(dto.getSourceId());
        logEntity.setRemark(StringUtils.defaultIfBlank(dto.getRemark(), "点卡余额消费"));
        logEntity.setCreatedAt(now);
        userBalanceLogDao.insert(logEntity);
        return actualDelta;
    }

    private UserMembershipEntity getActiveMembership(Long userId, Date now) {
        return userMembershipDao.selectOne(new QueryWrapper<UserMembershipEntity>()
                .eq("user_id", userId)
                .eq("status", 1)
                .ge("end_at", now)
                .orderByDesc("end_at")
                .last("limit 1"));
    }

    private UserMembershipDailyUsageEntity getMembershipDailyUsage(Long userId, LocalDate usageDate) {
        UserMembershipDailyUsageEntity usage = userMembershipDailyUsageDao.selectOne(
                new QueryWrapper<UserMembershipDailyUsageEntity>()
                        .eq("user_id", userId)
                        .eq("usage_date", usageDate)
                        .last("limit 1"));
        if (usage == null || !Integer.valueOf(1).equals(usage.getStatus())) {
            return null;
        }
        return usage;
    }

    @Override
    public PageData<UserBalanceLogEntity> pageUserBalanceLog(Long userId, UserBenefitLogPageDTO dto) {
        IPage<UserBalanceLogEntity> pageInfo = buildPage(dto.getPage(), dto.getLimit(), "id");

        IPage<UserBalanceLogEntity> page = userBalanceLogDao.selectPage(
                pageInfo,
                new QueryWrapper<UserBalanceLogEntity>()
                        .eq("user_id", userId));

        return getPageData(page, UserBalanceLogEntity.class);
    }

    @Override
    public PageData<UserMembershipEntity> pageUserMembership(Long userId, UserBenefitLogPageDTO dto) {
        IPage<UserMembershipEntity> pageInfo = buildPage(dto.getPage(), dto.getLimit(), "end_at");

        IPage<UserMembershipEntity> page = userMembershipDao.selectPage(
                pageInfo,
                new QueryWrapper<UserMembershipEntity>()
                        .eq("user_id", userId));

        return getPageData(page, UserMembershipEntity.class);
    }

    @Override
    public PageData<AdminUserBenefitVO> pageAdminUserBenefit(AdminUserBenefitPageDTO dto) {
        IPage<SysUserEntity> pageInfo = buildPage(dto.getPage(), dto.getLimit(), "id");
        IPage<SysUserEntity> page = sysUserDao.selectPage(pageInfo, buildSysUserKeywordQuery(dto.getKeyword()));

        List<SysUserEntity> users = page.getRecords();
        List<Long> userIds = users.stream().map(SysUserEntity::getId).collect(Collectors.toList());

        Map<Long, UserBalanceAccountEntity> accountMap = buildUserBalanceAccountMap(userIds);
        Map<Long, UserMembershipEntity> activeMembershipMap = buildActiveMembershipMap(userIds);
        Map<Long, UserMembershipDailyUsageEntity> dailyUsageMap = buildMembershipDailyUsageMap(userIds, LocalDate.now());

        List<AdminUserBenefitVO> records = new ArrayList<>();
        for (SysUserEntity user : users) {
            AdminUserBenefitVO vo = new AdminUserBenefitVO();
            vo.setUserId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setUserStatus(user.getStatus());

            UserBalanceAccountEntity account = accountMap.get(user.getId());
            vo.setBalanceSeconds(account == null ? 0 : safeInt(account.getBalanceMinutes()));
            vo.setTotalRechargedSeconds(account == null ? 0 : safeInt(account.getTotalRechargedMinutes()));
            vo.setTotalConsumedSeconds(account == null ? 0 : safeInt(account.getTotalConsumedMinutes()));
            vo.setAccountStatus(account == null ? null : account.getStatus());

            UserMembershipEntity membership = activeMembershipMap.get(user.getId());
            vo.setMembershipActive(membership != null);
            vo.setMembershipStatus(membership == null ? null : membership.getStatus());
            vo.setMembershipStartAt(membership == null ? null : membership.getStartAt());
            vo.setMembershipEndAt(membership == null ? null : membership.getEndAt());
            if (membership != null) {
                UserMembershipDailyUsageEntity dailyUsage = dailyUsageMap.get(user.getId());
                int dailyLimitSeconds = dailyUsage == null ? MEMBERSHIP_DAILY_LIMIT_SECONDS
                        : safeInt(dailyUsage.getDailyLimitSeconds());
                if (dailyLimitSeconds <= 0) {
                    dailyLimitSeconds = MEMBERSHIP_DAILY_LIMIT_SECONDS;
                }
                int dailyConsumedSeconds = dailyUsage == null ? 0 : safeInt(dailyUsage.getConsumedSeconds());
                vo.setMembershipDailyLimitSeconds(dailyLimitSeconds);
                vo.setMembershipDailyConsumedSeconds(dailyConsumedSeconds);
                vo.setMembershipDailyRemainingSeconds(Math.max(0, dailyLimitSeconds - dailyConsumedSeconds));
            }
            records.add(vo);
        }
        return new PageData<>(records, page.getTotal());
    }

    @Override
    public PageData<AdminUserBalanceLogVO> pageAdminUserBalanceLog(AdminUserBenefitRecordPageDTO dto) {
        IPage<UserBalanceLogEntity> pageInfo = buildPage(dto.getPage(), dto.getLimit(), "id");
        QueryWrapper<UserBalanceLogEntity> query = new QueryWrapper<UserBalanceLogEntity>()
                .eq(dto.getUserId() != null, "user_id", dto.getUserId())
                .eq(StringUtils.isNotBlank(dto.getChangeType()), "change_type", dto.getChangeType())
                .eq(StringUtils.isNotBlank(dto.getSourceType()), "source_type", dto.getSourceType());
        appendUserKeywordFilter(query, dto.getKeyword(), "user_id");

        IPage<UserBalanceLogEntity> page = userBalanceLogDao.selectPage(pageInfo, query);
        List<AdminUserBalanceLogVO> records = ConvertUtils.sourceToTarget(page.getRecords(), AdminUserBalanceLogVO.class);
        fillUsernames(records, records.stream().map(AdminUserBalanceLogVO::getUserId).collect(Collectors.toSet()),
                (item, username) -> item.setUsername(username));
        return new PageData<>(records, page.getTotal());
    }

    @Override
    public PageData<AdminUserMembershipVO> pageAdminUserMembership(AdminUserBenefitRecordPageDTO dto) {
        IPage<UserMembershipEntity> pageInfo = buildPage(dto.getPage(), dto.getLimit(), "end_at");
        QueryWrapper<UserMembershipEntity> query = new QueryWrapper<UserMembershipEntity>()
                .eq(dto.getUserId() != null, "user_id", dto.getUserId())
                .eq(dto.getStatus() != null, "status", dto.getStatus())
                .eq(StringUtils.isNotBlank(dto.getSourceType()), "source_type", dto.getSourceType());
        appendUserKeywordFilter(query, dto.getKeyword(), "user_id");

        IPage<UserMembershipEntity> page = userMembershipDao.selectPage(pageInfo, query);
        List<AdminUserMembershipVO> records = ConvertUtils.sourceToTarget(page.getRecords(), AdminUserMembershipVO.class);
        fillUsernames(records, records.stream().map(AdminUserMembershipVO::getUserId).collect(Collectors.toSet()),
                (item, username) -> item.setUsername(username));
        return new PageData<>(records, page.getTotal());
    }

    @Override
    public PageData<AdminUserMembershipLogVO> pageAdminUserMembershipLog(AdminUserBenefitRecordPageDTO dto) {
        IPage<UserMembershipLogEntity> pageInfo = buildPage(dto.getPage(), dto.getLimit(), "id");
        QueryWrapper<UserMembershipLogEntity> query = new QueryWrapper<UserMembershipLogEntity>()
                .eq(dto.getUserId() != null, "user_id", dto.getUserId())
                .eq(StringUtils.isNotBlank(dto.getChangeType()), "change_type", dto.getChangeType())
                .eq(StringUtils.isNotBlank(dto.getSourceType()), "source_type", dto.getSourceType());
        appendUserKeywordFilter(query, dto.getKeyword(), "user_id");

        IPage<UserMembershipLogEntity> page = userMembershipLogDao.selectPage(pageInfo, query);
        List<AdminUserMembershipLogVO> records = ConvertUtils.sourceToTarget(page.getRecords(), AdminUserMembershipLogVO.class);
        fillUsernames(records, records.stream().map(AdminUserMembershipLogVO::getUserId).collect(Collectors.toSet()),
                (item, username) -> item.setUsername(username));
        return new PageData<>(records, page.getTotal());
    }

    private void validateBatchCreateDTO(ActivationCodeBatchCreateDTO dto) {
        if (!CARD_TYPE_POINT.equalsIgnoreCase(dto.getCardType()) && !CARD_TYPE_MONTH.equalsIgnoreCase(dto.getCardType())) {
            throw new RenException("cardType仅支持point/month");
        }
        if (dto.getStatus() != null && dto.getStatus() != 0 && dto.getStatus() != 1) {
            throw new RenException("status只允许为0或1");
        }
        if (dto.getValidFrom() != null && dto.getValidUntil() != null && dto.getValidFrom().after(dto.getValidUntil())) {
            throw new RenException("validFrom不能晚于validUntil");
        }
    }

    private List<ActivationCodeEntity> buildActivationCodes(ActivationCodeBatchEntity batch, int count) {
        List<ActivationCodeEntity> list = new ArrayList<>(count);
        Set<String> localCodeSet = new HashSet<>();

        for (int i = 0; i < count; i++) {
            ActivationCodeEntity code = new ActivationCodeEntity();
            code.setBatchId(batch.getId());
            code.setCode(generateUniqueCode(localCodeSet));
            code.setStatus(0);
            code.setValidFrom(batch.getValidFrom());
            code.setValidUntil(batch.getValidUntil());
            code.setVoidReason("");
            list.add(code);
        }
        return list;
    }

    private String generateUniqueCode(Set<String> localCodeSet) {
        for (int i = 0; i < 50; i++) {
            String code = "AC" + RandomUtil.randomStringUpper(14);
            if (localCodeSet.contains(code)) {
                continue;
            }
            Long exists = activationCodeDao.selectCount(new QueryWrapper<ActivationCodeEntity>().eq("code", code));
            if (exists != null && exists > 0) {
                continue;
            }
            localCodeSet.add(code);
            return code;
        }
        throw new RenException("生成激活码失败，请重试");
    }

    private void fillBatchGeneratedCount(List<ActivationCodeBatchVO> batches) {
        if (batches == null || batches.isEmpty()) {
            return;
        }

        List<Long> batchIds = batches.stream().map(ActivationCodeBatchVO::getId).collect(Collectors.toList());
        List<Map<String, Object>> maps = activationCodeDao.selectMaps(
                new QueryWrapper<ActivationCodeEntity>()
                        .in("batch_id", batchIds)
                        .select("batch_id, count(1) as cnt")
                        .groupBy("batch_id"));

        Map<Long, Long> countMap = new HashMap<>();
        for (Map<String, Object> row : maps) {
            Object batchIdObj = row.get("batch_id");
            Object cntObj = row.get("cnt");
            if (batchIdObj instanceof Number && cntObj instanceof Number) {
                countMap.put(((Number) batchIdObj).longValue(), ((Number) cntObj).longValue());
            }
        }

        for (ActivationCodeBatchVO batch : batches) {
            batch.setGeneratedCount(countMap.getOrDefault(batch.getId(), 0L));
        }
    }

    private void fillCodeBatchInfo(List<ActivationCodeVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }

        Set<Long> batchIds = records.stream().map(ActivationCodeVO::getBatchId).collect(Collectors.toSet());
        List<ActivationCodeBatchEntity> batchEntities = activationCodeBatchDao.selectList(
                new QueryWrapper<ActivationCodeBatchEntity>().in("id", batchIds));

        Map<Long, ActivationCodeBatchEntity> batchMap = batchEntities.stream()
                .collect(Collectors.toMap(ActivationCodeBatchEntity::getId, item -> item, (a, b) -> a));

        for (ActivationCodeVO record : records) {
            ActivationCodeBatchEntity batch = batchMap.get(record.getBatchId());
            if (batch == null) {
                continue;
            }
            record.setBatchNo(batch.getBatchNo());
            record.setCardType(batch.getCardType());
            record.setFaceValue(batch.getFaceValue());
        }
    }

    private QueryWrapper<SysUserEntity> buildSysUserKeywordQuery(String keyword) {
        QueryWrapper<SysUserEntity> query = new QueryWrapper<>();
        if (StringUtils.isBlank(keyword)) {
            return query;
        }
        String trimmed = keyword.trim();
        query.and(wrapper -> wrapper.like("username", trimmed));
        if (StringUtils.isNumeric(trimmed)) {
            query.or().eq("id", Long.parseLong(trimmed));
        }
        return query;
    }

    private void appendUserKeywordFilter(QueryWrapper<?> query, String keyword, String userIdColumn) {
        if (StringUtils.isBlank(keyword)) {
            return;
        }
        List<Long> matchedUserIds = queryUserIdsByKeyword(keyword);
        if (matchedUserIds.isEmpty()) {
            query.apply("1 = 0");
            return;
        }
        query.in(userIdColumn, matchedUserIds);
    }

    private List<Long> queryUserIdsByKeyword(String keyword) {
        String trimmed = keyword == null ? "" : keyword.trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }
        QueryWrapper<SysUserEntity> query = new QueryWrapper<SysUserEntity>().like("username", trimmed);
        if (StringUtils.isNumeric(trimmed)) {
            query.or().eq("id", Long.parseLong(trimmed));
        }
        return sysUserDao.selectList(query).stream().map(SysUserEntity::getId).collect(Collectors.toList());
    }

    private Map<Long, UserBalanceAccountEntity> buildUserBalanceAccountMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<UserBalanceAccountEntity> accounts = userBalanceAccountDao.selectList(
                new QueryWrapper<UserBalanceAccountEntity>().in("user_id", userIds));
        Map<Long, UserBalanceAccountEntity> result = new HashMap<>();
        for (UserBalanceAccountEntity account : accounts) {
            result.put(account.getUserId(), account);
        }
        return result;
    }

    private Map<Long, UserMembershipEntity> buildActiveMembershipMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Date now = new Date();
        List<UserMembershipEntity> memberships = userMembershipDao.selectList(
                new QueryWrapper<UserMembershipEntity>()
                        .in("user_id", userIds)
                        .eq("status", 1)
                        .ge("end_at", now)
                        .orderByDesc("end_at"));
        Map<Long, UserMembershipEntity> result = new HashMap<>();
        for (UserMembershipEntity membership : memberships) {
            result.putIfAbsent(membership.getUserId(), membership);
        }
        return result;
    }

    private Map<Long, UserMembershipDailyUsageEntity> buildMembershipDailyUsageMap(List<Long> userIds,
            LocalDate usageDate) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<UserMembershipDailyUsageEntity> usages = userMembershipDailyUsageDao.selectList(
                new QueryWrapper<UserMembershipDailyUsageEntity>()
                        .in("user_id", userIds)
                        .eq("usage_date", usageDate)
                        .eq("status", 1));
        Map<Long, UserMembershipDailyUsageEntity> result = new HashMap<>();
        for (UserMembershipDailyUsageEntity usage : usages) {
            result.put(usage.getUserId(), usage);
        }
        return result;
    }

    private <T> void fillUsernames(List<T> records, Set<Long> userIds, BiUserNameSetter<T> setter) {
        if (records == null || records.isEmpty() || userIds == null || userIds.isEmpty()) {
            return;
        }
        Map<Long, String> usernameMap = sysUserDao.selectBatchIds(userIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(SysUserEntity::getId, SysUserEntity::getUsername, (a, b) -> a));
        for (T record : records) {
            Long userId = extractUserId(record);
            if (userId != null) {
                setter.accept(record, usernameMap.get(userId));
            }
        }
    }

    private Long extractUserId(Object record) {
        if (record instanceof AdminUserBalanceLogVO item) {
            return item.getUserId();
        }
        if (record instanceof AdminUserMembershipVO item) {
            return item.getUserId();
        }
        if (record instanceof AdminUserMembershipLogVO item) {
            return item.getUserId();
        }
        return null;
    }

    @FunctionalInterface
    private interface BiUserNameSetter<T> {
        void accept(T target, String username);
    }

    private void validateCodeUsable(ActivationCodeEntity codeEntity, Date now) {
        Integer status = codeEntity.getStatus();
        if (status == null) {
            throw new RenException("激活码状态异常");
        }
        if (status == 1) {
            throw new RenException("激活码已使用");
        }
        if (status == 2) {
            throw new RenException("激活码已作废");
        }
        if (status == 3) {
            throw new RenException("激活码已过期");
        }

        if (codeEntity.getValidFrom() != null && now.before(codeEntity.getValidFrom())) {
            throw new RenException("激活码尚未到可使用时间");
        }
        if (codeEntity.getValidUntil() != null && now.after(codeEntity.getValidUntil())) {
            activationCodeDao.update(null, new UpdateWrapper<ActivationCodeEntity>()
                    .eq("id", codeEntity.getId())
                    .eq("status", 0)
                    .set("status", 3)
                    .set("update_date", now));
            throw new RenException("激活码已过期");
        }
    }

    private void validateBatchUsable(ActivationCodeBatchEntity batchEntity, Date now) {
        if (!Integer.valueOf(1).equals(batchEntity.getStatus())) {
            throw new RenException("批次已禁用");
        }

        if (batchEntity.getValidFrom() != null && now.before(batchEntity.getValidFrom())) {
            throw new RenException("当前批次尚未到可使用时间");
        }
        if (batchEntity.getValidUntil() != null && now.after(batchEntity.getValidUntil())) {
            throw new RenException("当前批次已过期");
        }
    }

    private void applyPointBenefit(Long userId, Long activationCodeId, Integer seconds, Date now) {
        int delta = safeInt(seconds);
        if (delta <= 0) {
            throw new RenException("点卡面值异常");
        }

        UserBalanceAccountEntity account = userBalanceAccountDao.selectOne(
                new QueryWrapper<UserBalanceAccountEntity>().eq("user_id", userId).last("limit 1"));

        int before;
        int after;
        if (account == null) {
            account = new UserBalanceAccountEntity();
            account.setUserId(userId);
            account.setBalanceMinutes(delta);
            account.setTotalRechargedMinutes(delta);
            account.setTotalConsumedMinutes(0);
            account.setStatus(1);
            account.setCreatedAt(now);
            account.setUpdatedAt(now);
            userBalanceAccountDao.insert(account);
            before = 0;
            after = delta;
        } else {
            before = safeInt(account.getBalanceMinutes());
            after = before + delta;

            UpdateWrapper<UserBalanceAccountEntity> update = new UpdateWrapper<>();
            update.eq("id", account.getId())
                    .set("balance_minutes", after)
                    .set("total_recharged_minutes", safeInt(account.getTotalRechargedMinutes()) + delta)
                    .set("status", 1)
                    .set("updated_at", now);
            userBalanceAccountDao.update(null, update);
        }

        UserBalanceLogEntity logEntity = new UserBalanceLogEntity();
        logEntity.setUserId(userId);
        logEntity.setAccountId(account.getId());
        logEntity.setChangeType("recharge");
        logEntity.setDeltaMinutes(delta);
        logEntity.setBalanceBefore(before);
        logEntity.setBalanceAfter(after);
        logEntity.setSourceType("activation_code");
        logEntity.setSourceId(activationCodeId);
        logEntity.setRemark("激活码充值");
        logEntity.setCreatedAt(now);
        userBalanceLogDao.insert(logEntity);
    }

    private void applyMembershipBenefit(Long userId, Long activationCodeId, Integer days, Date now) {
        int deltaDays = safeInt(days);
        if (deltaDays <= 0) {
            throw new RenException("月卡面值异常");
        }

        UserMembershipEntity membership = userMembershipDao.selectOne(new QueryWrapper<UserMembershipEntity>()
                .eq("user_id", userId)
                .eq("status", 1)
                .ge("end_at", now)
                .orderByDesc("end_at")
                .last("limit 1"));

        if (membership == null) {
            UserMembershipEntity newMembership = new UserMembershipEntity();
            newMembership.setUserId(userId);
            newMembership.setMembershipType("month_card");
            newMembership.setStartAt(now);
            newMembership.setEndAt(plusDays(now, deltaDays));
            newMembership.setStatus(1);
            newMembership.setSourceType("activation_code");
            newMembership.setSourceId(activationCodeId);
            newMembership.setRemark("激活码创建月卡");
            newMembership.setCreatedAt(now);
            newMembership.setUpdatedAt(now);
            userMembershipDao.insert(newMembership);

            UserMembershipLogEntity logEntity = new UserMembershipLogEntity();
            logEntity.setUserId(userId);
            logEntity.setMembershipId(newMembership.getId());
            logEntity.setChangeType("create");
            logEntity.setStartAtAfter(newMembership.getStartAt());
            logEntity.setEndAtAfter(newMembership.getEndAt());
            logEntity.setSourceType("activation_code");
            logEntity.setSourceId(activationCodeId);
            logEntity.setRemark("激活码创建月卡");
            logEntity.setCreatedAt(now);
            userMembershipLogDao.insert(logEntity);
            return;
        }

        Date beforeStart = membership.getStartAt();
        Date beforeEnd = membership.getEndAt();
        Date base = beforeEnd != null && beforeEnd.after(now) ? beforeEnd : now;
        Date afterEnd = plusDays(base, deltaDays);

        UpdateWrapper<UserMembershipEntity> update = new UpdateWrapper<>();
        update.eq("id", membership.getId())
                .set("end_at", afterEnd)
                .set("status", 1)
                .set("source_type", "activation_code")
                .set("source_id", activationCodeId)
                .set("remark", "激活码延长月卡")
                .set("updated_at", now);
        userMembershipDao.update(null, update);

        UserMembershipLogEntity logEntity = new UserMembershipLogEntity();
        logEntity.setUserId(userId);
        logEntity.setMembershipId(membership.getId());
        logEntity.setChangeType("extend");
        logEntity.setStartAtBefore(beforeStart);
        logEntity.setEndAtBefore(beforeEnd);
        logEntity.setStartAtAfter(membership.getStartAt());
        logEntity.setEndAtAfter(afterEnd);
        logEntity.setSourceType("activation_code");
        logEntity.setSourceId(activationCodeId);
        logEntity.setRemark("激活码延长月卡");
        logEntity.setCreatedAt(now);
        userMembershipLogDao.insert(logEntity);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private Date plusDays(Date base, int days) {
        return Date.from(base.toInstant().plus(days, ChronoUnit.DAYS));
    }

    private QueryWrapper<ActivationCodeEntity> buildCodePageQuery(ActivationCodePageDTO dto) {
        QueryWrapper<ActivationCodeEntity> query = new QueryWrapper<ActivationCodeEntity>()
                .eq(dto.getStatus() != null, "status", dto.getStatus())
                .like(StringUtils.isNotBlank(dto.getCode()), "code", dto.getCode());

        if (StringUtils.isBlank(dto.getBatchNo())) {
            return query;
        }

        ActivationCodeBatchEntity batch = activationCodeBatchDao.selectOne(
                new QueryWrapper<ActivationCodeBatchEntity>().eq("batch_no", dto.getBatchNo()).select("id"));
        if (batch == null) {
            query.eq("batch_id", -1L);
            return query;
        }

        query.eq("batch_id", batch.getId());
        return query;
    }

    private <T> IPage<T> buildPage(String page, String limit, String orderField) {
        long curPage = Long.parseLong(page);
        long pageSize = Long.parseLong(limit);
        Page<T> pageInfo = new Page<>(curPage, pageSize);
        pageInfo.addOrder(OrderItem.desc(orderField));
        return pageInfo;
    }
}
