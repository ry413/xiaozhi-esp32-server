package xiaozhi.modules.liveplan.service;

import xiaozhi.common.page.PageData;
import xiaozhi.common.service.BaseService;
import xiaozhi.modules.liveplan.dto.LivePlanAdminPageDTO;
import xiaozhi.modules.liveplan.dto.LivePlanPageDTO;
import xiaozhi.modules.liveplan.dto.LivePlanSaveDTO;
import xiaozhi.modules.liveplan.dto.LivePlanUpdateDTO;
import xiaozhi.modules.liveplan.entity.LivePlanEntity;

public interface LivePlanService extends BaseService<LivePlanEntity> {

    PageData<LivePlanEntity> pageMine(Long userId, LivePlanPageDTO dto);

    PageData<LivePlanEntity> pageAdmin(LivePlanAdminPageDTO dto);

    LivePlanEntity getMineByPlanNo(Long userId, String planNo);

    String save(Long userId, LivePlanSaveDTO dto);

    void update(Long userId, String planNo, LivePlanUpdateDTO dto);

    void updateStatus(Long userId, String planNo, Integer status);

    void delete(Long userId, String planNo);
}
