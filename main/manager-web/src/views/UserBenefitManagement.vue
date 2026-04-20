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
        <div class="hero-card">
          <div class="hero-copy">
            <span class="eyebrow">Benefit Console</span>
            <h3>用户权益、余额和流水全局视图</h3>
            <p>适合排查充值发放、点卡消费、月卡叠加和过期状态。</p>
          </div>
          <div class="hero-stats">
            <div class="stat-pill">
              <span class="stat-label">当前页用户</span>
              <strong>{{ benefitList.length }}</strong>
            </div>
            <div class="stat-pill">
              <span class="stat-label">当前页有效月卡</span>
              <strong>{{ activeMembershipCount }}</strong>
            </div>
          </div>
        </div>

        <el-tabs v-model="activeTab" class="benefit-tabs" @tab-click="handleTabChange">
          <el-tab-pane label="权益摘要" name="benefits">
            <el-card shadow="never" class="table-card">
              <div class="filter-bar">
                <el-input v-model="benefitFilters.keyword" placeholder="用户ID / 用户名" clearable class="filter-item" @keyup.enter.native="handleBenefitSearch" />
                <el-button type="primary" @click="handleBenefitSearch">查询</el-button>
                <el-button @click="resetBenefitFilters">重置</el-button>
              </div>

              <el-table :data="benefitList" v-loading="benefitLoading" class="transparent-table">
                <el-table-column prop="userId" label="用户ID" min-width="120" />
                <el-table-column prop="username" label="用户名" min-width="160" />
                <el-table-column label="用户状态" width="100">
                  <template slot-scope="{ row }">
                    <el-tag :type="row.userStatus === 1 ? 'success' : 'info'">
                      {{ row.userStatus === 1 ? '正常' : '禁用' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="balanceSeconds" label="剩余秒数" min-width="110" />
                <el-table-column prop="totalRechargedSeconds" label="累计充值秒数" min-width="130" />
                <el-table-column prop="totalConsumedSeconds" label="累计消费秒数" min-width="130" />
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

              <el-table :data="balanceLogList" v-loading="balanceLogLoading" class="transparent-table">
                <el-table-column prop="id" label="流水ID" min-width="90" />
                <el-table-column prop="userId" label="用户ID" min-width="110" />
                <el-table-column prop="username" label="用户名" min-width="150" />
                <el-table-column prop="changeType" label="变更类型" min-width="100" />
                <el-table-column prop="deltaMinutes" label="变更秒数" min-width="100" />
                <el-table-column prop="balanceBefore" label="变更前" min-width="100" />
                <el-table-column prop="balanceAfter" label="变更后" min-width="100" />
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

              <el-table :data="membershipList" v-loading="membershipLoading" class="transparent-table">
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

              <el-table :data="membershipLogList" v-loading="membershipLogLoading" class="transparent-table">
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
  },
};
</script>

<style lang="scss" scoped>
.benefit-page {
  min-width: 1100px;
  min-height: 100vh;
  background: linear-gradient(135deg, #e7f1ff 0%, #eef6ff 45%, #f8efe3 100%);
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
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: 20px;
  box-shadow: 0 18px 50px rgba(70, 89, 129, 0.12);
  padding: 18px;
}

.hero-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 22px 24px;
  border-radius: 18px;
  background: linear-gradient(120deg, #17345f, #31598f 55%, #688cb2);
  color: #fff;
  margin-bottom: 18px;
}

.eyebrow {
  display: inline-block;
  margin-bottom: 8px;
  font-size: 12px;
  letter-spacing: 1.6px;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.7);
}

.hero-copy h3 {
  margin: 0 0 8px;
  font-size: 28px;
}

.hero-copy p {
  margin: 0;
  max-width: 520px;
  color: rgba(255, 255, 255, 0.82);
}

.hero-stats {
  display: flex;
  gap: 12px;
}

.stat-pill {
  min-width: 130px;
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.14);
  backdrop-filter: blur(8px);
}

.stat-label {
  display: block;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.74);
  margin-bottom: 6px;
}

.stat-pill strong {
  font-size: 28px;
}

.benefit-tabs ::v-deep .el-tabs__header {
  margin-bottom: 18px;
}

.benefit-tabs ::v-deep .el-tabs__nav-wrap::after {
  background: rgba(23, 52, 95, 0.08);
}

.table-card {
  border: none;
  border-radius: 16px;
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

::v-deep .el-table th {
  background: #f7f9fd;
  color: #22324d;
}
</style>
