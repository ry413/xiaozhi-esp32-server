<template>
  <div class="benefit-page">
    <HeaderBar />

    <div class="operation-bar">
      <div>
        <h2 class="page-title">用户权益管理</h2>
        <p class="page-subtitle">查看全量用户的点卡余额、月卡权益与流水记录。</p>
      </div>
    </div>

    <div class="main-wrapper">
      <div class="content-panel">
        <div class="summary-strip">
          <div class="summary-item">
            <span class="summary-label">当前页用户</span>
            <strong>{{ benefitList.length }}</strong>
          </div>
          <div class="summary-item">
            <span class="summary-label">有效月卡</span>
            <strong>{{ activeMembershipCount }}</strong>
          </div>
          <div class="summary-item">
            <span class="summary-label">今日额度耗尽</span>
            <strong>{{ exhaustedMembershipCount }}</strong>
          </div>
          <div class="summary-item wide">
            <span class="summary-label">当前页点卡余额</span>
            <strong>{{ formatDuration(totalBalanceSeconds) }}</strong>
          </div>
        </div>

        <el-tabs v-model="activeTab" class="benefit-tabs" @tab-click="handleTabChange">
          <el-tab-pane label="权益摘要" name="benefits">
            <el-card shadow="never" class="table-card">
              <div class="filter-bar">
                <el-input v-model="benefitFilters.keyword" placeholder="用户ID / 用户名" clearable class="filter-item" @keyup.enter.native="handleBenefitSearch" />
                <el-button type="primary" @click="handleBenefitSearch">查询</el-button>
                <el-button @click="resetBenefitFilters">重置</el-button>
                <el-button icon="el-icon-refresh" @click="fetchBenefits">刷新</el-button>
              </div>

              <el-table :data="benefitList" v-loading="benefitLoading" class="transparent-table" empty-text="暂无权益数据">
                <el-table-column prop="userId" label="用户ID" min-width="120" />
                <el-table-column prop="username" label="用户名" min-width="160" />
                <el-table-column label="用户状态" width="100">
                  <template slot-scope="{ row }">
                    <el-tag :type="row.userStatus === 1 ? 'success' : 'info'">
                      {{ row.userStatus === 1 ? '正常' : '禁用' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="点卡余额" min-width="130">
                  <template slot-scope="{ row }">
                    <span class="duration-text">{{ formatDuration(row.balanceSeconds) }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="累计充值" min-width="120">
                  <template slot-scope="{ row }">
                    {{ formatDuration(row.totalRechargedSeconds) }}
                  </template>
                </el-table-column>
                <el-table-column label="累计消费" min-width="120">
                  <template slot-scope="{ row }">
                    {{ formatDuration(row.totalConsumedSeconds) }}
                  </template>
                </el-table-column>
                <el-table-column label="余额账户" width="110">
                  <template slot-scope="{ row }">
                    <el-tag v-if="row.accountStatus === 1" type="success">正常</el-tag>
                    <el-tag v-else-if="row.accountStatus === 0" type="info">禁用</el-tag>
                    <span v-else>-</span>
                  </template>
                </el-table-column>
                <el-table-column label="月卡状态" width="110">
                  <template slot-scope="{ row }">
                    <el-tag :type="row.membershipActive ? 'warning' : 'info'">
                      {{ row.membershipActive ? '生效中' : '无有效月卡' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="月卡今日额度" min-width="210">
                  <template slot-scope="{ row }">
                    <div v-if="row.membershipActive" class="quota-cell">
                      <div class="quota-line">
                        <span>{{ formatDuration(row.membershipDailyConsumedSeconds) }}</span>
                        <span class="quota-muted">/ {{ formatDuration(row.membershipDailyLimitSeconds) }}</span>
                      </div>
                      <el-progress
                        :percentage="quotaPercent(row)"
                        :status="quotaStatus(row)"
                        :stroke-width="8"
                        :show-text="false"
                      />
                      <div :class="['quota-remaining', { exhausted: Number(row.membershipDailyRemainingSeconds) <= 0 }]">
                        剩余 {{ formatDuration(row.membershipDailyRemainingSeconds) }}
                      </div>
                    </div>
                    <span v-else>-</span>
                  </template>
                </el-table-column>
                <el-table-column prop="membershipStartAt" label="月卡开始时间" min-width="170" />
                <el-table-column prop="membershipEndAt" label="月卡结束时间" min-width="170" />
              </el-table>

              <div class="pagination-bar">
                <el-pagination
                  background
                  layout="total, prev, pager, next, sizes"
                  :current-page.sync="benefitPage.page"
                  :page-size.sync="benefitPage.limit"
                  :page-sizes="[10, 20, 50, 100]"
                  :total="benefitTotal"
                  @current-change="fetchBenefits"
                  @size-change="handleBenefitSizeChange"
                />
              </div>
            </el-card>
          </el-tab-pane>

          <el-tab-pane label="点卡流水" name="balanceLogs">
            <el-card shadow="never" class="table-card">
              <div class="filter-bar">
                <el-input v-model="balanceLogFilters.keyword" placeholder="用户ID / 用户名" clearable class="filter-item" @keyup.enter.native="handleBalanceLogSearch" />
                <el-select v-model="balanceLogFilters.changeType" placeholder="变更类型" clearable class="filter-item">
                  <el-option label="充值" value="recharge" />
                  <el-option label="消费" value="consume" />
                </el-select>
                <el-input v-model="balanceLogFilters.sourceType" placeholder="来源类型" clearable class="filter-item" @keyup.enter.native="handleBalanceLogSearch" />
                <el-button type="primary" @click="handleBalanceLogSearch">查询</el-button>
                <el-button @click="resetBalanceLogFilters">重置</el-button>
              </div>

              <el-table :data="balanceLogList" v-loading="balanceLogLoading" class="transparent-table" empty-text="暂无点卡流水">
                <el-table-column prop="id" label="流水ID" min-width="90" />
                <el-table-column prop="userId" label="用户ID" min-width="110" />
                <el-table-column prop="username" label="用户名" min-width="150" />
                <el-table-column prop="changeType" label="变更类型" min-width="100" />
                <el-table-column label="变更时长" min-width="110">
                  <template slot-scope="{ row }">{{ formatSignedDuration(row.deltaMinutes) }}</template>
                </el-table-column>
                <el-table-column label="变更前" min-width="110">
                  <template slot-scope="{ row }">{{ formatDuration(row.balanceBefore) }}</template>
                </el-table-column>
                <el-table-column label="变更后" min-width="110">
                  <template slot-scope="{ row }">{{ formatDuration(row.balanceAfter) }}</template>
                </el-table-column>
                <el-table-column prop="sourceType" label="来源类型" min-width="120" />
                <el-table-column prop="sourceId" label="来源ID" min-width="100" />
                <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
                <el-table-column prop="createdAt" label="创建时间" min-width="170" />
              </el-table>

              <div class="pagination-bar">
                <el-pagination
                  background
                  layout="total, prev, pager, next, sizes"
                  :current-page.sync="balanceLogPage.page"
                  :page-size.sync="balanceLogPage.limit"
                  :page-sizes="[10, 20, 50, 100]"
                  :total="balanceLogTotal"
                  @current-change="fetchBalanceLogs"
                  @size-change="handleBalanceLogSizeChange"
                />
              </div>
            </el-card>
          </el-tab-pane>

          <el-tab-pane label="月卡权益" name="memberships">
            <el-card shadow="never" class="table-card">
              <div class="filter-bar">
                <el-input v-model="membershipFilters.keyword" placeholder="用户ID / 用户名" clearable class="filter-item" @keyup.enter.native="handleMembershipSearch" />
                <el-select v-model="membershipFilters.status" placeholder="状态" clearable class="filter-item">
                  <el-option label="失效" :value="0" />
                  <el-option label="生效" :value="1" />
                  <el-option label="回收" :value="2" />
                </el-select>
                <el-input v-model="membershipFilters.sourceType" placeholder="来源类型" clearable class="filter-item" @keyup.enter.native="handleMembershipSearch" />
                <el-button type="primary" @click="handleMembershipSearch">查询</el-button>
                <el-button @click="resetMembershipFilters">重置</el-button>
              </div>

              <el-table :data="membershipList" v-loading="membershipLoading" class="transparent-table" empty-text="暂无月卡权益">
                <el-table-column prop="id" label="权益ID" min-width="90" />
                <el-table-column prop="userId" label="用户ID" min-width="110" />
                <el-table-column prop="username" label="用户名" min-width="150" />
                <el-table-column prop="membershipType" label="权益类型" min-width="120" />
                <el-table-column label="状态" width="100">
                  <template slot-scope="{ row }">
                    <el-tag :type="membershipStatusTag(row.status)">{{ membershipStatusText(row.status) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="startAt" label="开始时间" min-width="170" />
                <el-table-column prop="endAt" label="结束时间" min-width="170" />
                <el-table-column prop="sourceType" label="来源类型" min-width="120" />
                <el-table-column prop="sourceId" label="来源ID" min-width="100" />
                <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
              </el-table>

              <div class="pagination-bar">
                <el-pagination
                  background
                  layout="total, prev, pager, next, sizes"
                  :current-page.sync="membershipPage.page"
                  :page-size.sync="membershipPage.limit"
                  :page-sizes="[10, 20, 50, 100]"
                  :total="membershipTotal"
                  @current-change="fetchMemberships"
                  @size-change="handleMembershipSizeChange"
                />
              </div>
            </el-card>
          </el-tab-pane>

          <el-tab-pane label="月卡流水" name="membershipLogs">
            <el-card shadow="never" class="table-card">
              <div class="filter-bar">
                <el-input v-model="membershipLogFilters.keyword" placeholder="用户ID / 用户名" clearable class="filter-item" @keyup.enter.native="handleMembershipLogSearch" />
                <el-input v-model="membershipLogFilters.changeType" placeholder="变更类型" clearable class="filter-item" @keyup.enter.native="handleMembershipLogSearch" />
                <el-input v-model="membershipLogFilters.sourceType" placeholder="来源类型" clearable class="filter-item" @keyup.enter.native="handleMembershipLogSearch" />
                <el-button type="primary" @click="handleMembershipLogSearch">查询</el-button>
                <el-button @click="resetMembershipLogFilters">重置</el-button>
              </div>

              <el-table :data="membershipLogList" v-loading="membershipLogLoading" class="transparent-table" empty-text="暂无月卡流水">
                <el-table-column prop="id" label="流水ID" min-width="90" />
                <el-table-column prop="userId" label="用户ID" min-width="110" />
                <el-table-column prop="username" label="用户名" min-width="150" />
                <el-table-column prop="membershipId" label="权益ID" min-width="90" />
                <el-table-column prop="changeType" label="变更类型" min-width="100" />
                <el-table-column prop="startAtBefore" label="变更前开始" min-width="170" />
                <el-table-column prop="endAtBefore" label="变更前结束" min-width="170" />
                <el-table-column prop="startAtAfter" label="变更后开始" min-width="170" />
                <el-table-column prop="endAtAfter" label="变更后结束" min-width="170" />
                <el-table-column prop="sourceType" label="来源类型" min-width="120" />
                <el-table-column prop="sourceId" label="来源ID" min-width="100" />
                <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
              </el-table>

              <div class="pagination-bar">
                <el-pagination
                  background
                  layout="total, prev, pager, next, sizes"
                  :current-page.sync="membershipLogPage.page"
                  :page-size.sync="membershipLogPage.limit"
                  :page-sizes="[10, 20, 50, 100]"
                  :total="membershipLogTotal"
                  @current-change="fetchMembershipLogs"
                  @size-change="handleMembershipLogSizeChange"
                />
              </div>
            </el-card>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script>
import Api from "@/apis/api";
import HeaderBar from "@/components/HeaderBar.vue";

export default {
  name: "UserBenefitManagement",
  components: {
    HeaderBar,
  },
  data() {
    return {
      activeTab: "benefits",
      benefitLoading: false,
      balanceLogLoading: false,
      membershipLoading: false,
      membershipLogLoading: false,
      benefitList: [],
      balanceLogList: [],
      membershipList: [],
      membershipLogList: [],
      benefitTotal: 0,
      balanceLogTotal: 0,
      membershipTotal: 0,
      membershipLogTotal: 0,
      benefitFilters: { keyword: "" },
      balanceLogFilters: { keyword: "", changeType: "", sourceType: "" },
      membershipFilters: { keyword: "", status: undefined, sourceType: "" },
      membershipLogFilters: { keyword: "", changeType: "", sourceType: "" },
      benefitPage: { page: 1, limit: 10 },
      balanceLogPage: { page: 1, limit: 10 },
      membershipPage: { page: 1, limit: 10 },
      membershipLogPage: { page: 1, limit: 10 },
    };
  },
  computed: {
    activeMembershipCount() {
      return this.benefitList.filter(item => item.membershipActive).length;
    },
    exhaustedMembershipCount() {
      return this.benefitList.filter(item => item.membershipActive && Number(item.membershipDailyRemainingSeconds) <= 0).length;
    },
    totalBalanceSeconds() {
      return this.benefitList.reduce((total, item) => total + (Number(item.balanceSeconds) || 0), 0);
    },
  },
  created() {
    this.fetchBenefits();
  },
  methods: {
    handleTabChange() {
      if (this.activeTab === "benefits" && this.benefitList.length === 0) {
        this.fetchBenefits();
      }
      if (this.activeTab === "balanceLogs" && this.balanceLogList.length === 0) {
        this.fetchBalanceLogs();
      }
      if (this.activeTab === "memberships" && this.membershipList.length === 0) {
        this.fetchMemberships();
      }
      if (this.activeTab === "membershipLogs" && this.membershipLogList.length === 0) {
        this.fetchMembershipLogs();
      }
    },
    fetchBenefits() {
      this.benefitLoading = true;
      Api.activationCode.pageUserBenefits({
        page: this.benefitPage.page,
        limit: this.benefitPage.limit,
        keyword: this.benefitFilters.keyword,
      }, ({ data }) => {
        this.benefitLoading = false;
        if (data.code === 0) {
          this.benefitList = data.data.list || [];
          this.benefitTotal = data.data.total || 0;
        } else {
          this.$message.error(data.msg || "获取用户权益失败");
        }
      });
    },
    fetchBalanceLogs() {
      this.balanceLogLoading = true;
      Api.activationCode.pageUserBalanceLogs({
        page: this.balanceLogPage.page,
        limit: this.balanceLogPage.limit,
        keyword: this.balanceLogFilters.keyword,
        changeType: this.balanceLogFilters.changeType,
        sourceType: this.balanceLogFilters.sourceType,
      }, ({ data }) => {
        this.balanceLogLoading = false;
        if (data.code === 0) {
          this.balanceLogList = data.data.list || [];
          this.balanceLogTotal = data.data.total || 0;
        } else {
          this.$message.error(data.msg || "获取点卡流水失败");
        }
      });
    },
    fetchMemberships() {
      this.membershipLoading = true;
      Api.activationCode.pageUserMemberships({
        page: this.membershipPage.page,
        limit: this.membershipPage.limit,
        keyword: this.membershipFilters.keyword,
        status: this.membershipFilters.status,
        sourceType: this.membershipFilters.sourceType,
      }, ({ data }) => {
        this.membershipLoading = false;
        if (data.code === 0) {
          this.membershipList = data.data.list || [];
          this.membershipTotal = data.data.total || 0;
        } else {
          this.$message.error(data.msg || "获取用户月卡权益失败");
        }
      });
    },
    fetchMembershipLogs() {
      this.membershipLogLoading = true;
      Api.activationCode.pageUserMembershipLogs({
        page: this.membershipLogPage.page,
        limit: this.membershipLogPage.limit,
        keyword: this.membershipLogFilters.keyword,
        changeType: this.membershipLogFilters.changeType,
        sourceType: this.membershipLogFilters.sourceType,
      }, ({ data }) => {
        this.membershipLogLoading = false;
        if (data.code === 0) {
          this.membershipLogList = data.data.list || [];
          this.membershipLogTotal = data.data.total || 0;
        } else {
          this.$message.error(data.msg || "获取用户月卡流水失败");
        }
      });
    },
    handleBenefitSearch() {
      this.benefitPage.page = 1;
      this.fetchBenefits();
    },
    handleBalanceLogSearch() {
      this.balanceLogPage.page = 1;
      this.fetchBalanceLogs();
    },
    handleMembershipSearch() {
      this.membershipPage.page = 1;
      this.fetchMemberships();
    },
    handleMembershipLogSearch() {
      this.membershipLogPage.page = 1;
      this.fetchMembershipLogs();
    },
    resetBenefitFilters() {
      this.benefitFilters = { keyword: "" };
      this.handleBenefitSearch();
    },
    resetBalanceLogFilters() {
      this.balanceLogFilters = { keyword: "", changeType: "", sourceType: "" };
      this.handleBalanceLogSearch();
    },
    resetMembershipFilters() {
      this.membershipFilters = { keyword: "", status: undefined, sourceType: "" };
      this.handleMembershipSearch();
    },
    resetMembershipLogFilters() {
      this.membershipLogFilters = { keyword: "", changeType: "", sourceType: "" };
      this.handleMembershipLogSearch();
    },
    handleBenefitSizeChange(limit) {
      this.benefitPage.limit = limit;
      this.benefitPage.page = 1;
      this.fetchBenefits();
    },
    handleBalanceLogSizeChange(limit) {
      this.balanceLogPage.limit = limit;
      this.balanceLogPage.page = 1;
      this.fetchBalanceLogs();
    },
    handleMembershipSizeChange(limit) {
      this.membershipPage.limit = limit;
      this.membershipPage.page = 1;
      this.fetchMemberships();
    },
    handleMembershipLogSizeChange(limit) {
      this.membershipLogPage.limit = limit;
      this.membershipLogPage.page = 1;
      this.fetchMembershipLogs();
    },
    membershipStatusText(status) {
      if (status === 1) return "生效";
      if (status === 2) return "回收";
      return "失效";
    },
    membershipStatusTag(status) {
      if (status === 1) return "success";
      if (status === 2) return "danger";
      return "info";
    },
    formatDuration(seconds) {
      const value = Math.max(0, Number(seconds) || 0);
      const hours = Math.floor(value / 3600);
      const minutes = Math.floor((value % 3600) / 60);
      const restSeconds = value % 60;

      if (hours > 0) {
        return `${hours}小时${minutes}分`;
      }
      if (minutes > 0) {
        return `${minutes}分${restSeconds}秒`;
      }
      return `${restSeconds}秒`;
    },
    formatSignedDuration(seconds) {
      const value = Number(seconds) || 0;
      const prefix = value > 0 ? "+" : value < 0 ? "-" : "";
      return `${prefix}${this.formatDuration(Math.abs(value))}`;
    },
    quotaPercent(row) {
      const limit = Number(row.membershipDailyLimitSeconds) || 0;
      if (limit <= 0) {
        return 0;
      }
      const consumed = Number(row.membershipDailyConsumedSeconds) || 0;
      return Math.min(100, Math.round((consumed / limit) * 100));
    },
    quotaStatus(row) {
      const remaining = Number(row.membershipDailyRemainingSeconds) || 0;
      if (remaining <= 0) {
        return "exception";
      }
      if (this.quotaPercent(row) >= 80) {
        return "warning";
      }
      return undefined;
    },
  },
};
</script>

<style lang="scss" scoped>
.benefit-page {
  min-width: 1100px;
  min-height: 100vh;
  background: #f4f7fb;
}

.operation-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 18px 24px 8px;
}

.page-title {
  margin: 0;
  font-size: 26px;
}

.page-subtitle {
  margin: 8px 0 0;
  color: #6b7280;
}

.main-wrapper {
  margin: 0 22px 24px;
}

.content-panel {
  background: #fff;
  border: 1px solid #e5eaf2;
  border-radius: 8px;
  box-shadow: 0 10px 30px rgba(28, 43, 70, 0.06);
  padding: 16px;
}

.summary-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(150px, 1fr)) minmax(220px, 1.4fr);
  gap: 12px;
  margin-bottom: 16px;
}

.summary-item {
  min-height: 76px;
  padding: 14px 16px;
  border: 1px solid #e7edf5;
  border-radius: 8px;
  background: #fbfcfe;
}

.summary-label {
  display: block;
  margin-bottom: 8px;
  color: #697386;
  font-size: 13px;
}

.summary-item strong {
  color: #1f2d3d;
  font-size: 24px;
  font-weight: 650;
}

.benefit-tabs ::v-deep .el-tabs__header {
  margin-bottom: 18px;
}

.benefit-tabs ::v-deep .el-tabs__nav-wrap::after {
  background: rgba(23, 52, 95, 0.08);
}

.table-card {
  border: 1px solid #edf1f7;
  border-radius: 8px;
  box-shadow: none;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.filter-item {
  width: 220px;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.transparent-table {
  width: 100%;
}

.duration-text {
  color: #1f2d3d;
  font-weight: 600;
}

.quota-cell {
  min-width: 190px;
}

.quota-line {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 6px;
  color: #1f2d3d;
  font-size: 13px;
  white-space: nowrap;
}

.quota-muted {
  color: #7b8794;
}

.quota-remaining {
  margin-top: 6px;
  color: #5f6f85;
  font-size: 12px;
  line-height: 1.2;
}

.quota-remaining.exhausted {
  color: #f56c6c;
  font-weight: 600;
}

::v-deep .el-table th {
  background: #f7f9fd;
  color: #22324d;
}
</style>
