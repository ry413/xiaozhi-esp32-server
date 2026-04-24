<template>
  <div class="llm-report-page">
    <HeaderBar />

    <div class="operation-bar">
      <div>
        <h2 class="page-title">大模型调用记录</h2>
        <p class="page-subtitle">按用户、设备、会话和关键词排查 LLM 输入与输出。</p>
      </div>
    </div>

    <div class="main-wrapper">
      <div class="content-panel">
        <div class="summary-band">
          <div>
            <span class="eyebrow">LLM Debug Console</span>
            <h3>用户输入与模型回复追踪</h3>
            <p>记录服务端实际发给大模型的 messages，以及流式结束后的完整输出。</p>
          </div>
          <div class="summary-stats">
            <div class="stat-pill">
              <span>当前页</span>
              <strong>{{ reportList.length }}</strong>
            </div>
            <div class="stat-pill">
              <span>总记录</span>
              <strong>{{ total }}</strong>
            </div>
          </div>
        </div>

        <el-card shadow="never" class="table-card">
          <div class="filter-bar">
            <el-input
              v-model="filters.keyword"
              placeholder="关键词: 用户/设备/会话/输入/输出"
              clearable
              class="filter-item filter-wide"
              @keyup.enter.native="handleSearch"
            />
            <el-input
              v-model="filters.userId"
              placeholder="用户ID"
              clearable
              class="filter-item"
              @keyup.enter.native="handleSearch"
            />
            <el-input
              v-model="filters.macAddress"
              placeholder="设备ID/MAC"
              clearable
              class="filter-item"
              @keyup.enter.native="handleSearch"
            />
            <el-input
              v-model="filters.sessionId"
              placeholder="会话ID"
              clearable
              class="filter-item filter-wide"
              @keyup.enter.native="handleSearch"
            />
            <el-date-picker
              v-model="filters.timeRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              value-format="yyyy-MM-dd HH:mm:ss"
              class="time-filter"
            />
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="resetFilters">重置</el-button>
          </div>

          <el-table
            :data="reportList"
            v-loading="loading"
            class="transparent-table"
            @row-dblclick="openDetail"
          >
            <el-table-column prop="id" label="ID" width="90" />
            <el-table-column label="用户" min-width="150">
              <template slot-scope="{ row }">
                <div class="stack-cell">
                  <span>{{ row.username || "-" }}</span>
                  <small>{{ row.userId || "-" }}</small>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="设备" min-width="190" show-overflow-tooltip>
              <template slot-scope="{ row }">
                <div class="stack-cell">
                  <span>{{ row.macAddress || "-" }}</span>
                  <small>{{ row.clientIp || "-" }}</small>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="clientId" label="客户端ID" min-width="150" show-overflow-tooltip />
            <el-table-column prop="sessionId" label="会话ID" min-width="210" show-overflow-tooltip />
            <el-table-column label="输入预览" min-width="260">
              <template slot-scope="{ row }">
                <span class="text-preview">{{ compactText(row.llmInput) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="输出预览" min-width="260">
              <template slot-scope="{ row }">
                <span class="text-preview">{{ compactText(row.llmOutput) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="时间" min-width="170" />
            <el-table-column label="操作" width="90" fixed="right">
              <template slot-scope="{ row }">
                <el-button type="text" size="mini" @click="openDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-bar">
            <el-pagination
              background
              layout="total, prev, pager, next, sizes"
              :current-page.sync="page.page"
              :page-size.sync="page.limit"
              :page-sizes="[10, 20, 50, 100]"
              :total="total"
              @current-change="fetchReports"
              @size-change="handleSizeChange"
            />
          </div>
        </el-card>
      </div>
    </div>

    <el-dialog
      title="大模型调用详情"
      :visible.sync="detailVisible"
      width="78%"
      top="5vh"
      custom-class="llm-detail-dialog"
    >
      <div v-if="currentReport" class="detail-layout">
        <div class="meta-grid">
          <div><label>ID</label><span>{{ currentReport.id }}</span></div>
          <div><label>用户</label><span>{{ currentReport.username || "-" }} / {{ currentReport.userId || "-" }}</span></div>
          <div><label>设备</label><span>{{ currentReport.macAddress || "-" }}</span></div>
          <div><label>客户端</label><span>{{ currentReport.clientId || "-" }}</span></div>
          <div><label>IP</label><span>{{ currentReport.clientIp || "-" }}</span></div>
          <div><label>时间</label><span>{{ currentReport.createdAt || "-" }}</span></div>
          <div class="meta-wide"><label>会话</label><span>{{ currentReport.sessionId || "-" }}</span></div>
          <div class="meta-wide"><label>智能体</label><span>{{ currentReport.agentId || "-" }}</span></div>
        </div>

        <div class="io-grid">
          <section>
            <header>
              <span>LLM 输入 <small>{{ textLength(currentReport.llmInput) }} 字符</small></span>
              <el-button type="text" size="mini" @click="copyText(currentReport.llmInput)">复制</el-button>
            </header>
            <pre>{{ prettyInput(currentReport.llmInput) }}</pre>
          </section>
          <section>
            <header>
              <span>LLM 输出 <small>{{ textLength(currentReport.llmOutput) }} 字符</small></span>
              <el-button type="text" size="mini" @click="copyText(currentReport.llmOutput)">复制</el-button>
            </header>
            <pre>{{ currentReport.llmOutput }}</pre>
          </section>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import Api from "@/apis/api";
import HeaderBar from "@/components/HeaderBar.vue";

export default {
  name: "LlmReportManagement",
  components: {
    HeaderBar,
  },
  data() {
    return {
      loading: false,
      reportList: [],
      total: 0,
      page: { page: 1, limit: 10 },
      filters: {
        keyword: "",
        userId: "",
        macAddress: "",
        sessionId: "",
        timeRange: [],
      },
      detailVisible: false,
      currentReport: null,
    };
  },
  created() {
    this.fetchReports();
  },
  methods: {
    fetchReports() {
      this.loading = true;
      const [startTime, endTime] = this.filters.timeRange || [];
      Api.llmReport.page({
        page: this.page.page,
        limit: this.page.limit,
        keyword: this.filters.keyword,
        userId: this.filters.userId,
        macAddress: this.filters.macAddress,
        sessionId: this.filters.sessionId,
        startTime,
        endTime,
      }, ({ data }) => {
        this.loading = false;
        if (data.code === 0) {
          this.reportList = data.data.list || [];
          this.total = data.data.total || 0;
        } else {
          this.reportList = [];
          this.total = 0;
          this.$message.error(data.msg || "获取大模型调用记录失败");
        }
      });
    },
    handleSearch() {
      this.page.page = 1;
      this.fetchReports();
    },
    resetFilters() {
      this.filters = {
        keyword: "",
        userId: "",
        macAddress: "",
        sessionId: "",
        timeRange: [],
      };
      this.handleSearch();
    },
    handleSizeChange(limit) {
      this.page.limit = limit;
      this.page.page = 1;
      this.fetchReports();
    },
    openDetail(row) {
      this.currentReport = row;
      this.detailVisible = true;
    },
    compactText(text) {
      if (!text) {
        return "-";
      }
      return String(text).replace(/\s+/g, " ").slice(0, 180);
    },
    prettyInput(text) {
      if (!text) {
        return "";
      }
      try {
        return JSON.stringify(JSON.parse(text), null, 2);
      } catch (e) {
        return text;
      }
    },
    textLength(text) {
      return text ? String(text).length : 0;
    },
    copyText(text) {
      if (!text) {
        return;
      }
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(() => {
          this.$message.success("已复制");
        });
        return;
      }
      const textarea = document.createElement("textarea");
      textarea.value = text;
      document.body.appendChild(textarea);
      textarea.select();
      document.execCommand("copy");
      document.body.removeChild(textarea);
      this.$message.success("已复制");
    },
  },
};
</script>

<style lang="scss" scoped>
.llm-report-page {
  min-width: 1180px;
  min-height: 100vh;
  background: linear-gradient(135deg, #e8f3ff 0%, #f4f7fb 48%, #f6eee4 100%);
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
  color: #667085;
}

.main-wrapper {
  margin: 0 22px 24px;
}

.content-panel {
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(255, 255, 255, 0.82);
  border-radius: 20px;
  box-shadow: 0 18px 50px rgba(70, 89, 129, 0.12);
  padding: 18px;
}

.summary-band {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 22px 24px;
  border-radius: 16px;
  background: linear-gradient(120deg, #18314f, #2f5e7c 58%, #6d8ea0);
  color: #fff;
  margin-bottom: 18px;
}

.eyebrow {
  display: inline-block;
  margin-bottom: 8px;
  font-size: 12px;
  letter-spacing: 1.2px;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.72);
}

.summary-band h3 {
  margin: 0 0 8px;
  font-size: 28px;
}

.summary-band p {
  margin: 0;
  color: rgba(255, 255, 255, 0.82);
}

.summary-stats {
  display: flex;
  gap: 12px;
}

.stat-pill {
  min-width: 110px;
  padding: 14px 16px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.14);
}

.stat-pill span {
  display: block;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.76);
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
  width: 190px;
}

.filter-wide {
  width: 280px;
}

.time-filter {
  width: 360px;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.transparent-table {
  width: 100%;
}

.stack-cell {
  display: flex;
  flex-direction: column;
  line-height: 1.45;
}

.stack-cell small {
  color: #8a94a6;
}

.text-preview {
  display: block;
  color: #344054;
  line-height: 1.45;
}

.detail-layout {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px 16px;
}

.meta-grid div {
  display: flex;
  gap: 8px;
  min-width: 0;
}

.meta-grid label {
  flex: 0 0 56px;
  color: #667085;
}

.meta-grid span {
  min-width: 0;
  word-break: break-all;
}

.meta-wide {
  grid-column: span 3;
}

.io-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.io-grid section {
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  overflow: hidden;
  background: #fbfcfe;
}

.io-grid header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  border-bottom: 1px solid #e4e7ed;
  background: #f5f7fb;
  font-weight: 600;
}

.io-grid header small {
  margin-left: 8px;
  color: #667085;
  font-weight: 400;
}

.io-grid pre {
  height: 52vh;
  margin: 0;
  padding: 14px;
  overflow: auto;
  text-align: left;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
  font-size: 13px;
  line-height: 1.55;
}

::v-deep .el-table th {
  background: #f7f9fd;
  color: #22324d;
}
</style>
