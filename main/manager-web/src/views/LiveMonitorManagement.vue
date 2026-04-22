<template>
  <div class="monitor-page">
    <HeaderBar />

    <div class="operation-bar">
      <div>
        <h2 class="page-title">直播实例管理</h2>
        <p class="page-subtitle">查看当前所有监控实例的运行状态、权益占用、直播间和设备健康度。</p>
      </div>
      <div class="action-group">
        <el-button @click="fetchLiveMonitors" :loading="loading">立即刷新</el-button>
      </div>
    </div>

    <div class="main-wrapper">
      <div class="content-panel">
        <div class="hero-card">
          <div class="hero-copy">
            <span class="eyebrow">Monitor Console</span>
            <h3>全局直播监控视图</h3>
            <p>这里聚合设备在线、房间状态、权益扣费和最近一次 prompt 信息，适合排查异常直播实例。</p>
          </div>
          <div class="hero-stats">
            <div class="stat-pill">
              <span class="stat-label">实例总数</span>
              <strong>{{ monitorList.length }}</strong>
            </div>
            <div class="stat-pill">
              <span class="stat-label">运行中</span>
              <strong>{{ runningCount }}</strong>
            </div>
            <div class="stat-pill">
              <span class="stat-label">异常数</span>
              <strong>{{ abnormalCount }}</strong>
            </div>
          </div>
        </div>

        <el-card shadow="never" class="table-card">
          <div class="filter-bar">
            <el-input
              v-model.trim="filters.keyword"
              placeholder="设备ID / 直播ID / 用户ID"
              clearable
              class="filter-item"
            />
            <el-select v-model="filters.status" placeholder="实例状态" clearable class="filter-item">
              <el-option label="starting" value="starting" />
              <el-option label="running" value="running" />
              <el-option label="stopping" value="stopping" />
              <el-option label="stopped" value="stopped" />
              <el-option label="error" value="error" />
            </el-select>
            <el-select v-model="filters.roomStatus" placeholder="直播间状态" clearable class="filter-item">
              <el-option label="running" value="running" />
              <el-option label="ended" value="ended" />
              <el-option label="unknown" value="unknown" />
            </el-select>
            <el-select v-model="filters.deviceOnline" placeholder="设备在线状态" clearable class="filter-item">
              <el-option label="在线" :value="true" />
              <el-option label="离线" :value="false" />
            </el-select>
            <el-select v-model="filters.onlyAbnormal" placeholder="异常筛选" clearable class="filter-item">
              <el-option label="仅异常实例" :value="true" />
            </el-select>
          </div>

          <el-table
            :data="filteredMonitorList"
            :row-key="getRowKey"
            :expand-row-keys="expandedRowKeys"
            v-loading="loading"
            class="transparent-table"
            @expand-change="handleExpandChange"
          >
            <el-table-column type="expand">
              <template slot-scope="{ row }">
                <div class="expand-panel">
                  <div class="detail-grid">
                    <div class="detail-item">
                      <span class="detail-label">创建时间</span>
                      <span>{{ formatAbsoluteTime(row.created_at) }}</span>
                    </div>
                    <div class="detail-item">
                      <span class="detail-label">更新时间</span>
                      <span>{{ formatAbsoluteTime(row.updated_at) }}</span>
                    </div>
                    <div class="detail-item">
                      <span class="detail-label">启动时间</span>
                      <span>{{ formatAbsoluteTime(row.started_at) }}</span>
                    </div>
                    <div class="detail-item">
                      <span class="detail-label">停止时间</span>
                      <span>{{ formatAbsoluteTime(row.stopped_at) }}</span>
                    </div>
                    <div class="detail-item">
                      <span class="detail-label">最近计费时间</span>
                      <span>{{ formatAbsoluteTime(row.last_billed_at) }}</span>
                    </div>
                    <div class="detail-item">
                      <span class="detail-label">累计计费秒数</span>
                      <span>{{ safeNumber(row.billed_seconds_total) }}</span>
                    </div>
                    <div class="detail-item">
                      <span class="detail-label">最近发送时间</span>
                      <span>{{ formatAbsoluteTime(row.last_sent_prompt_at) }}</span>
                    </div>
                    <div class="detail-item">
                      <span class="detail-label">发送 prompt 数</span>
                      <span>{{ safeNumber(row.sent_prompt_count) }}</span>
                    </div>
                    <div class="detail-item">
                      <span class="detail-label">设备状态检查时间</span>
                      <span>{{ formatAbsoluteTime(row.device_online_checked_at) }}</span>
                    </div>
                    <div class="detail-item">
                      <span class="detail-label">设备离线开始</span>
                      <span>{{ formatAbsoluteTime(row.device_offline_since) }}</span>
                    </div>
                    <div class="detail-item">
                      <span class="detail-label">房间关闭开始</span>
                      <span>{{ formatAbsoluteTime(row.room_ended_since) }}</span>
                    </div>
                    <div class="detail-item">
                      <span class="detail-label">权益检查时间</span>
                      <span>{{ formatAbsoluteTime(row.benefit_checked_at) }}</span>
                    </div>
                    <div class="detail-item">
                      <span class="detail-label">月卡检查时间</span>
                      <span>{{ formatAbsoluteTime(row.membership_last_checked_at) }}</span>
                    </div>
                    <div class="detail-item">
                      <span class="detail-label">月卡过期开始</span>
                      <span>{{ formatAbsoluteTime(row.membership_expired_since) }}</span>
                    </div>
                    <div class="detail-item full-span">
                      <span class="detail-label">最近一次 Prompt</span>
                      <span class="preview-text">{{ row.last_sent_prompt_preview || "-" }}</span>
                    </div>
                    <div class="detail-item full-span">
                      <span class="detail-label">错误信息</span>
                      <span class="error-text">{{ row.error || "-" }}</span>
                    </div>
                    <div class="detail-item full-span">
                      <span class="detail-label">PromptState</span>
                      <pre class="json-box">{{ formatJson(row.prompt_state) }}</pre>
                    </div>
                  </div>
                </div>
              </template>
            </el-table-column>

            <el-table-column prop="device_id" label="设备ID" min-width="170" />
            <el-table-column prop="live_id" label="直播ID" min-width="160" />
            <el-table-column label="实例状态" width="110">
              <template slot-scope="{ row }">
                <el-tag :type="statusTagType(row.status)">{{ row.status || "-" }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="运行中" width="90">
              <template slot-scope="{ row }">
                <el-tag :type="row.is_active ? 'success' : 'info'">{{ row.is_active ? "是" : "否" }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="运行时长" min-width="110">
              <template slot-scope="{ row }">
                {{ formatRuntimeSeconds(row.runtime_seconds) || "-" }}
              </template>
            </el-table-column>
            <el-table-column label="设备状态" width="110">
              <template slot-scope="{ row }">
                <el-tag :type="row.device_online ? 'success' : 'danger'">
                  {{ row.device_online ? "在线" : "离线" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="直播间状态" width="120">
              <template slot-scope="{ row }">
                <el-tag :type="roomStatusTagType(row.room_status)">
                  {{ row.room_status || "-" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="观看人数" min-width="120">
              <template slot-scope="{ row }">
                {{ safeNumber(row.current_viewer_count) }} / {{ safeNumber(row.total_viewer_count) }}
              </template>
            </el-table-column>
            <el-table-column label="权益用户" min-width="160">
              <template slot-scope="{ row }">
                <div class="owner-cell">
                  <span>{{ row.benefit_user_name || "-" }}</span>
                  <span class="owner-sub">{{ row.benefit_user_id || "-" }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="点卡秒数" min-width="110">
              <template slot-scope="{ row }">
                {{ safeNumber(row.benefit_balance_seconds) }}
              </template>
            </el-table-column>
            <el-table-column label="月卡" width="100">
              <template slot-scope="{ row }">
                <el-tag :type="row.benefit_membership_active ? 'warning' : 'info'">
                  {{ row.benefit_membership_active ? "有效" : "无效" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="异常摘要" min-width="220" show-overflow-tooltip>
              <template slot-scope="{ row }">
                {{ buildAbnormalSummary(row) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script>
import Api from "@/apis/api";
import HeaderBar from "@/components/HeaderBar.vue";

export default {
  name: "LiveMonitorManagement",
  components: {
    HeaderBar,
  },
  data() {
    return {
      loading: false,
      refreshTimer: null,
      monitorList: [],
      expandedRowKeys: [],
      filters: {
        keyword: "",
        status: "",
        roomStatus: "",
        deviceOnline: undefined,
        onlyAbnormal: undefined,
      },
    };
  },
  computed: {
    filteredMonitorList() {
      return this.monitorList.filter((item) => {
        const keyword = this.filters.keyword.trim();
        if (keyword) {
          const matched =
            String(item.device_id || "").includes(keyword) ||
            String(item.live_id || "").includes(keyword) ||
            String(item.benefit_user_id || "").includes(keyword);
          if (!matched) {
            return false;
          }
        }
        if (this.filters.status && item.status !== this.filters.status) {
          return false;
        }
        if (this.filters.roomStatus && item.room_status !== this.filters.roomStatus) {
          return false;
        }
        if (typeof this.filters.deviceOnline === "boolean" && item.device_online !== this.filters.deviceOnline) {
          return false;
        }
        if (this.filters.onlyAbnormal && !this.isAbnormal(item)) {
          return false;
        }
        return true;
      });
    },
    runningCount() {
      return this.monitorList.filter((item) => item.status === "running").length;
    },
    abnormalCount() {
      return this.monitorList.filter((item) => this.isAbnormal(item)).length;
    },
  },
  created() {
    this.fetchLiveMonitors();
    this.startAutoRefresh();
  },
  beforeDestroy() {
    this.stopAutoRefresh();
  },
  methods: {
    fetchLiveMonitors() {
      this.loading = true;
      Api.liveStreaming.getAllLiveInfo(({ data }) => {
        this.loading = false;
        if (data && data.code === 0) {
          const monitorMap = data.data || {};
          this.monitorList = Object.entries(monitorMap).map(([deviceId, item]) => ({
            device_id: deviceId,
            ...(item || {}),
          }));
          this.monitorList.sort((a, b) => {
            const aTime = new Date(a.updated_at || a.created_at || 0).getTime();
            const bTime = new Date(b.updated_at || b.created_at || 0).getTime();
            return bTime - aTime;
          });
          const currentKeys = new Set(this.monitorList.map(item => this.getRowKey(item)));
          this.expandedRowKeys = this.expandedRowKeys.filter(key => currentKeys.has(key));
          return;
        }
        this.$message.error((data && data.msg) || "获取直播实例失败");
      });
    },
    startAutoRefresh() {
      this.stopAutoRefresh();
      this.refreshTimer = setInterval(() => {
        if (!this.loading) {
          this.fetchLiveMonitors();
        }
      }, 10000);
    },
    stopAutoRefresh() {
      if (this.refreshTimer) {
        clearInterval(this.refreshTimer);
        this.refreshTimer = null;
      }
    },
    formatAbsoluteTime(value) {
      if (!value) {
        return "-";
      }
      const date = new Date(value);
      if (Number.isNaN(date.getTime())) {
        return String(value);
      }
      const year = date.getFullYear();
      const month = `${date.getMonth() + 1}`.padStart(2, "0");
      const day = `${date.getDate()}`.padStart(2, "0");
      const hour = `${date.getHours()}`.padStart(2, "0");
      const minute = `${date.getMinutes()}`.padStart(2, "0");
      const second = `${date.getSeconds()}`.padStart(2, "0");
      return `${year}-${month}-${day} ${hour}:${minute}:${second}`;
    },
    formatRuntimeSeconds(value) {
      const totalSeconds = Number(value);
      if (!Number.isFinite(totalSeconds) || totalSeconds < 0) {
        return "";
      }
      const hours = Math.floor(totalSeconds / 3600);
      const minutes = Math.floor((totalSeconds % 3600) / 60);
      const seconds = totalSeconds % 60;
      if (hours > 0) {
        return `${hours}小时${minutes}分${seconds}秒`;
      }
      if (minutes > 0) {
        return `${minutes}分${seconds}秒`;
      }
      return `${seconds}秒`;
    },
    statusTagType(status) {
      if (status === "running") return "success";
      if (status === "starting" || status === "stopping") return "warning";
      if (status === "error") return "danger";
      return "info";
    },
    roomStatusTagType(status) {
      if (status === "running") return "success";
      if (status === "ended") return "danger";
      return "info";
    },
    safeNumber(value) {
      const num = Number(value);
      return Number.isFinite(num) ? num : 0;
    },
    isAbnormal(item) {
      return (
        item.status === "error" ||
        item.device_online === false ||
        item.room_status === "ended" ||
        this.safeNumber(item.membership_expired_duration_seconds) > 0 ||
        this.safeNumber(item.device_offline_duration_seconds) > 0 ||
        this.safeNumber(item.room_ended_duration_seconds) > 0 ||
        Boolean(item.error)
      );
    },
    buildAbnormalSummary(item) {
      const parts = [];
      if (item.status === "error") {
        parts.push("实例错误");
      }
      if (item.device_online === false) {
        parts.push(`设备离线 ${this.formatRuntimeSeconds(item.device_offline_duration_seconds) || "0秒"}`);
      }
      if (item.room_status === "ended") {
        parts.push(`直播间关闭 ${this.formatRuntimeSeconds(item.room_ended_duration_seconds) || "0秒"}`);
      }
      if (this.safeNumber(item.membership_expired_duration_seconds) > 0) {
        parts.push(`月卡过期 ${this.formatRuntimeSeconds(item.membership_expired_duration_seconds)}`);
      }
      if (item.error) {
        parts.push(item.error);
      }
      return parts.length ? parts.join(" | ") : "-";
    },
    formatJson(value) {
      if (!value) {
        return "-";
      }
      try {
        return JSON.stringify(value, null, 2);
      } catch (error) {
        return String(value);
      }
    },
    getRowKey(row) {
      return row.device_id;
    },
    handleExpandChange(row, expandedRows) {
      this.expandedRowKeys = expandedRows.map(item => this.getRowKey(item));
    },
  },
};
</script>

<style lang="scss" scoped>
.monitor-page {
  min-width: 1200px;
  min-height: 100vh;
  background: linear-gradient(135deg, #e9f4ff 0%, #eef4ff 45%, #f7eee7 100%);
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

.action-group {
  display: flex;
  gap: 12px;
}

.main-wrapper {
  margin: 0 22px 24px;
}

.content-panel {
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid rgba(255, 255, 255, 0.85);
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
  background: linear-gradient(120deg, #1a2f53, #28527f 55%, #487ba6);
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
  max-width: 560px;
  color: rgba(255, 255, 255, 0.82);
}

.hero-stats {
  display: flex;
  gap: 12px;
}

.stat-pill {
  min-width: 120px;
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

.transparent-table {
  width: 100%;
}

::v-deep .el-table th {
  background: #f7f9fd;
  color: #22324d;
}

.expand-panel {
  padding: 12px 16px;
  background: #f8fbff;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 20px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-label {
  font-size: 12px;
  color: #6b7280;
}

.owner-cell {
  display: flex;
  flex-direction: column;
  line-height: 1.4;
}

.owner-sub {
  font-size: 12px;
  color: #6b7280;
}

.full-span {
  grid-column: 1 / -1;
}

.preview-text,
.error-text {
  white-space: pre-wrap;
  word-break: break-all;
}

.error-text {
  color: #b42318;
}

.json-box {
  margin: 0;
  padding: 12px;
  border-radius: 10px;
  background: #0f172a;
  color: #dbeafe;
  overflow: auto;
  font-size: 12px;
}
</style>
