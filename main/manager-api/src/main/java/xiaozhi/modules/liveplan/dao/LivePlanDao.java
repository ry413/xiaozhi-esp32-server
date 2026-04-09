package xiaozhi.modules.liveplan.dao;

import org.apache.ibatis.annotations.Mapper;

import xiaozhi.common.dao.BaseDao;
import xiaozhi.modules.liveplan.entity.LivePlanEntity;

@Mapper
public interface LivePlanDao extends BaseDao<LivePlanEntity> {
}
