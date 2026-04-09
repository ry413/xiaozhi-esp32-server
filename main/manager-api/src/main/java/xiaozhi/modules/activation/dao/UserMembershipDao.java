package xiaozhi.modules.activation.dao;

import org.apache.ibatis.annotations.Mapper;

import xiaozhi.common.dao.BaseDao;
import xiaozhi.modules.activation.entity.UserMembershipEntity;

@Mapper
public interface UserMembershipDao extends BaseDao<UserMembershipEntity> {
}
