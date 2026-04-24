package xiaozhi.modules.activation.dao;

import java.time.LocalDate;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import xiaozhi.common.dao.BaseDao;
import xiaozhi.modules.activation.entity.UserMembershipDailyUsageEntity;

@Mapper
public interface UserMembershipDailyUsageDao extends BaseDao<UserMembershipDailyUsageEntity> {

    @Insert("""
            INSERT IGNORE INTO user_membership_daily_usage
                (user_id, membership_id, usage_date, daily_limit_seconds, consumed_seconds, status, created_at, updated_at)
            VALUES
                (#{userId}, #{membershipId}, #{usageDate}, #{dailyLimitSeconds}, 0, 1, NOW(), NOW())
            """)
    int insertIgnoreDailyUsage(@Param("userId") Long userId,
            @Param("membershipId") Long membershipId,
            @Param("usageDate") LocalDate usageDate,
            @Param("dailyLimitSeconds") Integer dailyLimitSeconds);
}
