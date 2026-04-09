package xiaozhi.modules.activation.dao;

import org.apache.ibatis.annotations.Mapper;

import xiaozhi.common.dao.BaseDao;
import xiaozhi.modules.activation.entity.ActivationCodeBatchEntity;

@Mapper
public interface ActivationCodeBatchDao extends BaseDao<ActivationCodeBatchEntity> {
}
