<template>
  <div class="activation-page">
    <HeaderBar />

    <div class="operation-bar">
      <div>
        <h2 class="page-title">激活码管理</h2>
        <p class="page-subtitle">超级管理员专用，用于创建批次、检索激活码和执行作废操作。</p>
      </div>
      <el-button type="primary" class="create-btn" @click="createDialogVisible = true">
        新建批次
      </el-button>
    </div>

    <div class="main-wrapper">
      <div class="content-panel">
        <div class="hero-card">
          <div class="hero-copy">
            <span class="eyebrow">Activation Console</span>
            <h3>批次生成和激活码追踪</h3>
            <p>点卡和月卡统一在这里管理。批次面向生成，激活码面向追踪和作废。</p>
          </div>
          <div class="hero-stats">
            <div class="stat-pill">
              <span class="stat-label">批次数</span>
              <strong>{{ batchTotal }}</strong>
            </div>
            <div class="stat-pill">
              <span class="stat-label">激活码数</span>
              <strong>{{ codeTotal }}</strong>
            </div>
          </div>
        </div>

        <el-tabs v-model="activeTab" class="activation-tabs">
          <el-tab-pane label="批次管理" name="batches">
            <el-card shadow="never" class="table-card">
              <div class="filter-bar">
                <el-input v-model="batchFilters.batchNo" placeholder="批次号" clearable class="filter-item" @keyup.enter.native="handleBatchSearch" />
                <el-input v-model="batchFilters.batchName" placeholder="批次名称" clearable class="filter-item" @keyup.enter.native="handleBatchSearch" />
                <el-select v-model="batchFilters.cardType" placeholder="卡类型" clearable class="filter-item">
                  <el-option label="点卡" value="point" />
                  <el-option label="月卡" value="month" />
                </el-select>
                <el-select v-model="batchFilters.status" placeholder="状态" clearable class="filter-item">
                  <el-option label="禁用" :value="0" />
                  <el-option label="启用" :value="1" />
                </el-select>
                <el-button type="primary" @click="handleBatchSearch">查询</el-button>
                <el-button @click="resetBatchFilters">重置</el-button>
              </div>

              <el-table :data="batchList" v-loading="batchLoading" class="transparent-table">
                <el-table-column prop="batchNo" label="批次号" min-width="160" />
                <el-table-column prop="batchName" label="批次名称" min-width="160" />
                <el-table-column label="卡类型" width="110">
                  <template slot-scope="{ row }">
                    <el-tag :type="row.cardType === 'point' ? 'success' : 'warning'">
                      {{ row.cardType === 'point' ? '点卡' : '月卡' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="faceValue" label="面值" width="110" />
                <el-table-column prop="generatedCount" label="生成数量" width="110" />
                <el-table-column label="状态" width="100">
                  <template slot-scope="{ row }">
                    <el-tag :type="row.status === 1 ? 'success' : 'info'">
                      {{ row.status === 1 ? '启用' : '禁用' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="validFrom" label="开始时间" min-width="170" />
                <el-table-column prop="validUntil" label="结束时间" min-width="170" />
                <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
                <el-table-column label="操作" width="170" fixed="right">
                  <template slot-scope="{ row }">
                    <el-button type="text" @click="switchBatchStatus(row, row.status === 1 ? 0 : 1)">
                      {{ row.status === 1 ? '禁用' : '启用' }}
                    </el-button>
                    <el-button type="text" @click="viewCodesByBatch(row.batchNo)">查看激活码</el-button>
                  </template>
                </el-table-column>
              </el-table>

              <div class="pagination-bar">
                <el-pagination
                  background
                  layout="total, prev, pager, next, sizes"
                  :current-page.sync="batchPage.page"
                  :page-size.sync="batchPage.limit"
                  :page-sizes="[10, 20, 50, 100]"
                  :total="batchTotal"
                  @current-change="fetchBatches"
                  @size-change="handleBatchSizeChange"
                />
              </div>
            </el-card>
          </el-tab-pane>

          <el-tab-pane label="激活码明细" name="codes">
            <el-card shadow="never" class="table-card">
              <div class="filter-bar">
                <el-input v-model="codeFilters.batchNo" placeholder="批次号" clearable class="filter-item" @keyup.enter.native="handleCodeSearch" />
                <el-input v-model="codeFilters.code" placeholder="激活码" clearable class="filter-item" @keyup.enter.native="handleCodeSearch" />
                <el-select v-model="codeFilters.status" placeholder="状态" clearable class="filter-item">
                  <el-option label="未使用" :value="0" />
                  <el-option label="已使用" :value="1" />
                  <el-option label="已作废" :value="2" />
                  <el-option label="已过期" :value="3" />
                </el-select>
                <el-button type="primary" @click="handleCodeSearch">查询</el-button>
                <el-button @click="resetCodeFilters">重置</el-button>
              </div>

              <el-table :data="codeList" v-loading="codeLoading" class="transparent-table">
                <el-table-column prop="code" label="激活码" min-width="220" />
                <el-table-column prop="batchNo" label="批次号" min-width="160" />
                <el-table-column label="卡类型" width="110">
                  <template slot-scope="{ row }">
                    <el-tag :type="row.cardType === 'point' ? 'success' : 'warning'">
                      {{ row.cardType === 'point' ? '点卡' : '月卡' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="faceValue" label="面值" width="100" />
                <el-table-column label="状态" width="100">
                  <template slot-scope="{ row }">
                    <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="usedUserId" label="使用用户ID" min-width="130" />
                <el-table-column prop="usedAt" label="使用时间" min-width="170" />
                <el-table-column prop="voidReason" label="作废原因" min-width="180" show-overflow-tooltip />
                <el-table-column label="操作" width="120" fixed="right">
                  <template slot-scope="{ row }">
                    <el-button type="text" :disabled="row.status !== 0" @click="openVoidDialog(row)">
                      作废
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>

              <div class="pagination-bar">
                <el-pagination
                  background
                  layout="total, prev, pager, next, sizes"
                  :current-page.sync="codePage.page"
                  :page-size.sync="codePage.limit"
                  :page-sizes="[10, 20, 50, 100]"
                  :total="codeTotal"
                  @current-change="fetchCodes"
                  @size-change="handleCodeSizeChange"
                />
              </div>
            </el-card>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <el-dialog title="新建激活码批次" :visible.sync="createDialogVisible" width="620px" @closed="resetCreateForm">
      <el-form ref="createForm" :model="createForm" :rules="createRules" label-width="110px">
        <el-form-item label="批次号" prop="batchNo">
          <el-input v-model="createForm.batchNo" placeholder="例如 POINT_20260410_A" />
        </el-form-item>
        <el-form-item label="批次名称" prop="batchName">
          <el-input v-model="createForm.batchName" placeholder="例如 直播点卡首发批次" />
        </el-form-item>
        <el-form-item label="卡类型" prop="cardType">
          <el-radio-group v-model="createForm.cardType">
            <el-radio label="point">点卡</el-radio>
            <el-radio label="month">月卡</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="createForm.cardType === 'month' ? '面值(天)' : '面值(秒)'" prop="faceValue">
          <el-input-number v-model="createForm.faceValue" :min="1" :step="createForm.cardType === 'month' ? 1 : 60" controls-position="right" />
        </el-form-item>
        <el-form-item label="生成数量" prop="generateCount">
          <el-input-number v-model="createForm.generateCount" :min="1" :max="5000" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="batchEnabled" active-text="启用" inactive-text="禁用" />
        </el-form-item>
        <el-form-item label="有效时间">
          <el-date-picker
            v-model="createRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="timestamp"
            class="full-width"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="3" placeholder="可选，写一点便于追踪的说明" />
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="submitCreateBatch">创建并生成</el-button>
      </span>
    </el-dialog>

    <el-dialog title="作废激活码" :visible.sync="voidDialogVisible" width="500px" @closed="resetVoidForm">
      <el-form ref="voidForm" :model="voidForm" :rules="voidRules" label-width="90px">
        <el-form-item label="激活码">
          <el-input :value="currentVoidCode" disabled />
        </el-form-item>
        <el-form-item label="作废原因" prop="reason">
          <el-input v-model="voidForm.reason" type="textarea" :rows="4" placeholder="请输入作废原因" />
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="voidDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="voidLoading" @click="submitVoidCode">确认作废</el-button>
      </span>
    </el-dialog>
<!-- 
    <el-footer>
      <VersionFooter />
    </el-footer> -->
  </div>
</template>

<script>
import { mapState } from "vuex";
import Api from "@/apis/api";
import HeaderBar from "@/components/HeaderBar.vue";
import VersionFooter from "@/components/VersionFooter.vue";

export default {
  name: "ActivationCodeManagement",
  components: {
    HeaderBar,
    VersionFooter,
  },
  data() {
    return {
      activeTab: "batches",
      batchLoading: false,
      codeLoading: false,
      createLoading: false,
      voidLoading: false,
      createDialogVisible: false,
      voidDialogVisible: false,
      currentVoidCode: "",
      batchList: [],
      codeList: [],
      batchTotal: 0,
      codeTotal: 0,
      batchFilters: {
        batchNo: "",
        batchName: "",
        cardType: "",
        status: undefined,
      },
      codeFilters: {
        batchNo: "",
        code: "",
        status: undefined,
      },
      batchPage: {
        page: 1,
        limit: 10,
      },
      codePage: {
        page: 1,
        limit: 10,
      },
      createForm: {
        batchNo: "",
        batchName: "",
        cardType: "point",
        faceValue: 3600,
        status: 1,
        validFrom: null,
        validUntil: null,
        remark: "",
        generateCount: 100,
      },
      createRange: [],
      voidForm: {
        reason: "",
      },
      createRules: {
        batchNo: [{ required: true, message: "请输入批次号", trigger: "blur" }],
        batchName: [{ required: true, message: "请输入批次名称", trigger: "blur" }],
        cardType: [{ required: true, message: "请选择卡类型", trigger: "change" }],
        faceValue: [{ required: true, message: "请输入面值", trigger: "change" }],
        generateCount: [{ required: true, message: "请输入生成数量", trigger: "change" }],
      },
      voidRules: {
        reason: [{ required: true, message: "请输入作废原因", trigger: "blur" }],
      },
    };
  },
  computed: {
    ...mapState({
      userInfo: (state) => state.userInfo,
    }),
    effectiveUserInfo() {
      if (this.userInfo && Object.keys(this.userInfo).length > 0) {
        return this.userInfo;
      }
      try {
        return JSON.parse(localStorage.getItem("userInfo") || "{}");
      } catch (error) {
        return {};
      }
    },
    batchEnabled: {
      get() {
        return this.createForm.status === 1;
      },
      set(value) {
        this.createForm.status = value ? 1 : 0;
      },
    },
  },
  created() {
    if (!this.effectiveUserInfo?.superAdmin) {
      this.$message.error("该页面仅超级管理员可用");
      this.$router.replace("/home");
      return;
    }
    this.fetchBatches();
    this.fetchCodes();
  },
  methods: {
    fetchBatches() {
      this.batchLoading = true;
      Api.activationCode.pageBatches(
        {
          ...this.batchFilters,
          page: this.batchPage.page,
          limit: this.batchPage.limit,
        },
        ({ data }) => {
          this.batchLoading = false;
          if (data.code === 0) {
            this.batchList = data.data.list || [];
            this.batchTotal = data.data.total || 0;
          }
        }
      );
    },
    fetchCodes() {
      this.codeLoading = true;
      Api.activationCode.pageCodes(
        {
          ...this.codeFilters,
          page: this.codePage.page,
          limit: this.codePage.limit,
        },
        ({ data }) => {
          this.codeLoading = false;
          if (data.code === 0) {
            this.codeList = data.data.list || [];
            this.codeTotal = data.data.total || 0;
          }
        }
      );
    },
    handleBatchSearch() {
      this.batchPage.page = 1;
      this.fetchBatches();
    },
    handleCodeSearch() {
      this.codePage.page = 1;
      this.fetchCodes();
    },
    resetBatchFilters() {
      this.batchFilters = {
        batchNo: "",
        batchName: "",
        cardType: "",
        status: undefined,
      };
      this.handleBatchSearch();
    },
    resetCodeFilters() {
      this.codeFilters = {
        batchNo: "",
        code: "",
        status: undefined,
      };
      this.handleCodeSearch();
    },
    handleBatchSizeChange(size) {
      this.batchPage.limit = size;
      this.batchPage.page = 1;
      this.fetchBatches();
    },
    handleCodeSizeChange(size) {
      this.codePage.limit = size;
      this.codePage.page = 1;
      this.fetchCodes();
    },
    submitCreateBatch() {
      this.$refs.createForm.validate((valid) => {
        if (!valid) return;
        this.createLoading = true;
        const [validFrom, validUntil] = this.createRange || [];
        const payload = {
          ...this.createForm,
          validFrom: validFrom || null,
          validUntil: validUntil || null,
        };
        Api.activationCode.createBatch(payload, ({ data }) => {
          this.createLoading = false;
          if (data.code === 0) {
            this.$message.success(`批次创建成功：${data.data}`);
            this.createDialogVisible = false;
            this.fetchBatches();
            this.codePage.page = 1;
            this.codeFilters.batchNo = data.data;
            this.activeTab = "codes";
            this.fetchCodes();
          }
        });
      });
    },
    switchBatchStatus(row, status) {
      const actionText = status === 1 ? "启用" : "禁用";
      this.$confirm(`确认${actionText}批次 ${row.batchNo} 吗？`, "提示", {
        type: "warning",
      }).then(() => {
        Api.activationCode.updateBatchStatus(row.batchNo, status, ({ data }) => {
          if (data.code === 0) {
            this.$message.success(`${actionText}成功`);
            this.fetchBatches();
          }
        });
      }).catch(() => {});
    },
    viewCodesByBatch(batchNo) {
      this.activeTab = "codes";
      this.codeFilters.batchNo = batchNo;
      this.codePage.page = 1;
      this.fetchCodes();
    },
    openVoidDialog(row) {
      this.currentVoidCode = row.code;
      this.voidDialogVisible = true;
    },
    submitVoidCode() {
      this.$refs.voidForm.validate((valid) => {
        if (!valid) return;
        this.voidLoading = true;
        Api.activationCode.voidCode(this.currentVoidCode, this.voidForm.reason, ({ data }) => {
          this.voidLoading = false;
          if (data.code === 0) {
            this.$message.success("作废成功");
            this.voidDialogVisible = false;
            this.fetchCodes();
          }
        });
      });
    },
    resetCreateForm() {
      this.createForm = {
        batchNo: "",
        batchName: "",
        cardType: "point",
        faceValue: 3600,
        status: 1,
        validFrom: null,
        validUntil: null,
        remark: "",
        generateCount: 100,
      };
      this.createRange = [];
      if (this.$refs.createForm) {
        this.$refs.createForm.resetFields();
      }
    },
    resetVoidForm() {
      this.currentVoidCode = "";
      this.voidForm.reason = "";
      if (this.$refs.voidForm) {
        this.$refs.voidForm.resetFields();
      }
    },
    statusText(status) {
      return {
        0: "未使用",
        1: "已使用",
        2: "已作废",
        3: "已过期",
      }[status] || "未知";
    },
    statusTagType(status) {
      return {
        0: "success",
        1: "warning",
        2: "info",
        3: "danger",
      }[status] || "info";
    },
  },
};
</script>

<style scoped>
.activation-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top right, rgba(255, 196, 118, 0.18), transparent 28%),
    linear-gradient(180deg, #f7f4ec 0%, #eef3f8 54%, #f8fafc 100%);
}

.operation-bar {
  max-width: 1360px;
  margin: 24px auto 0;
  padding: 0 24px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.page-title {
  margin: 0;
  font-size: 28px;
  color: #1b2a41;
}

.page-subtitle {
  margin: 8px 0 0;
  color: #5f6c80;
  font-size: 14px;
}

.create-btn {
  border-radius: 999px;
  padding: 12px 22px;
  background: linear-gradient(135deg, #0c7a6b, #1ca48b);
  border: none;
}

.main-wrapper {
  max-width: 1360px;
  margin: 20px auto 32px;
  padding: 0 24px;
}

.hero-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  padding: 28px 32px;
  border-radius: 28px;
  background: linear-gradient(135deg, #12343b, #1e5b61 58%, #e7c77e 180%);
  color: #fff;
  box-shadow: 0 18px 40px rgba(18, 52, 59, 0.16);
}

.eyebrow {
  display: inline-block;
  margin-bottom: 10px;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  letter-spacing: 0.08em;
  font-size: 12px;
  text-transform: uppercase;
}

.hero-copy h3 {
  margin: 0 0 10px;
  font-size: 30px;
}

.hero-copy p {
  margin: 0;
  max-width: 560px;
  color: rgba(255, 255, 255, 0.8);
}

.hero-stats {
  display: flex;
  gap: 14px;
}

.stat-pill {
  min-width: 140px;
  padding: 16px 18px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(8px);
}

.stat-label {
  display: block;
  margin-bottom: 8px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.stat-pill strong {
  font-size: 28px;
}

.activation-tabs {
  margin-top: 22px;
}

.table-card {
  border: none;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.88);
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 18px;
}

.filter-item {
  width: 180px;
}

.pagination-bar {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.full-width {
  width: 100%;
}

::v-deep .el-tabs__item.is-active {
  color: #0c7a6b;
}

::v-deep .el-tabs__active-bar {
  background-color: #0c7a6b;
}

::v-deep .el-card__body {
  padding: 22px;
}

@media (max-width: 900px) {
  .operation-bar,
  .main-wrapper {
    padding: 0 16px;
  }

  .operation-bar,
  .hero-card {
    flex-direction: column;
    align-items: stretch;
  }

  .hero-stats {
    width: 100%;
    flex-direction: column;
  }

  .filter-item {
    width: 100%;
  }

  .pagination-bar {
    overflow-x: auto;
  }
}
</style>
