package xiaozhi.modules.liveplan.service.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import cn.hutool.core.util.RandomUtil;
import lombok.AllArgsConstructor;
import xiaozhi.common.constant.Constant;
import xiaozhi.common.exception.RenException;
import xiaozhi.common.page.PageData;
import xiaozhi.common.service.impl.BaseServiceImpl;
import xiaozhi.common.utils.ConvertUtils;
import xiaozhi.modules.liveplan.dao.LivePlanDao;
import xiaozhi.modules.liveplan.dto.LivePlanAdminPageDTO;
import xiaozhi.modules.liveplan.dto.LivePlanPageDTO;
import xiaozhi.modules.liveplan.dto.LivePlanSaveDTO;
import xiaozhi.modules.liveplan.dto.LivePlanUpdateDTO;
import xiaozhi.modules.liveplan.entity.LivePlanEntity;
import xiaozhi.modules.liveplan.service.LivePlanService;

@Service
@AllArgsConstructor
public class LivePlanServiceImpl extends BaseServiceImpl<LivePlanDao, LivePlanEntity> implements LivePlanService {

    private final LivePlanDao livePlanDao;

    @Override
    public PageData<LivePlanEntity> pageMine(Long userId, LivePlanPageDTO dto) {
        Map<String, Object> params = new HashMap<>();
        params.put(Constant.PAGE, dto.getPage());
        params.put(Constant.LIMIT, dto.getLimit());

        IPage<LivePlanEntity> page = livePlanDao.selectPage(
                getPage(params, "create_date", false),
                new QueryWrapper<LivePlanEntity>()
                        .eq("user_id", userId)
                        .eq(dto.getStatus() != null, "status", dto.getStatus())
                        .eq(StringUtils.isNotBlank(dto.getPlatform()), "platform", dto.getPlatform())
                        .like(StringUtils.isNotBlank(dto.getPlanName()), "plan_name", dto.getPlanName()));

        return getPageData(page, LivePlanEntity.class);
    }

    @Override
    public PageData<LivePlanEntity> pageAdmin(LivePlanAdminPageDTO dto) {
        Map<String, Object> params = new HashMap<>();
        params.put(Constant.PAGE, dto.getPage());
        params.put(Constant.LIMIT, dto.getLimit());

        IPage<LivePlanEntity> page = livePlanDao.selectPage(
                getPage(params, "create_date", false),
                new QueryWrapper<LivePlanEntity>()
                        .eq(dto.getUserId() != null, "user_id", dto.getUserId())
                        .eq(dto.getStatus() != null, "status", dto.getStatus())
                        .eq(StringUtils.isNotBlank(dto.getPlatform()), "platform", dto.getPlatform())
                        .like(StringUtils.isNotBlank(dto.getPlanName()), "plan_name", dto.getPlanName()));

        return getPageData(page, LivePlanEntity.class);
    }

    @Override
    public LivePlanEntity getMineByPlanNo(Long userId, String planNo) {
        if (StringUtils.isBlank(planNo)) {
            throw new RenException("planNo不能为空");
        }
        LivePlanEntity entity = livePlanDao.selectOne(
                new QueryWrapper<LivePlanEntity>().eq("plan_no", planNo));
        if (entity == null || !userId.equals(entity.getUserId())) {
            throw new RenException("方案不存在");
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String save(Long userId, LivePlanSaveDTO dto) {
        validateStatus(dto.getStatus());

        LivePlanEntity entity = ConvertUtils.sourceToTarget(dto, LivePlanEntity.class);
        entity.setPlanNo(generatePlanNo());
        entity.setUserId(userId);
        if (entity.getStatus() == null) {
            entity.setStatus(0);
        }
        if (entity.getRemark() == null) {
            entity.setRemark("");
        }

        livePlanDao.insert(entity);
        return entity.getPlanNo();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, String planNo, LivePlanUpdateDTO dto) {
        validateStatus(dto.getStatus());
        LivePlanEntity current = getMineByPlanNo(userId, planNo);

        LivePlanEntity entity = ConvertUtils.sourceToTarget(dto, LivePlanEntity.class);
        entity.setId(current.getId());
        entity.setUserId(current.getUserId());
        if (entity.getStatus() == null) {
            entity.setStatus(current.getStatus());
        }
        if (entity.getRemark() == null) {
            entity.setRemark("");
        }

        livePlanDao.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long userId, String planNo, Integer status) {
        validateStatus(status);
        LivePlanEntity entity = getMineByPlanNo(userId, planNo);
        entity.setStatus(status);
        livePlanDao.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, String planNo) {
        LivePlanEntity entity = getMineByPlanNo(userId, planNo);
        livePlanDao.deleteById(entity.getId());
    }

    private void validateStatus(Integer status) {
        if (status == null) {
            return;
        }
        if (status != 0 && status != 1) {
            throw new RenException("status只允许为0或1");
        }
    }

    private String generatePlanNo() {
        for (int i = 0; i < 50; i++) {
            String planNo = "LP" + RandomUtil.randomStringUpper(14);
            Long count = livePlanDao.selectCount(new QueryWrapper<LivePlanEntity>().eq("plan_no", planNo));
            if (count == null || count == 0) {
                return planNo;
            }
        }
        throw new RenException("生成planNo失败，请重试");
    }
}
