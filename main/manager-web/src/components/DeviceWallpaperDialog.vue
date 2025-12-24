<template>
  <el-dialog :visible="visible" @close="handleClose" width="80%">
    <div slot="title" class="dialog-title-wrapper">
      <span class="dialog-title-text">壁纸设置</span>
    </div>

    <div class="dialog-content">
      <div class="main-layout">
        <div class="left-panel">
          <div class="tool-list-section">
            <div class="section-body">
              <div class="wallpaper-toolbar">
                <div class="toolbar-left">
                  <span class="toolbar-badge">
                    当前设备选中：{{ selectedIds.length }} 张
                  </span>
                  <!-- <el-button type="text" size="mini" :disabled="selectedIds.length === 0" @click="clearSelection">
                    清空选择
                  </el-button> -->
                </div>

              </div>

              <div v-if="loading" class="tool-list-loading">
                <i class="el-icon-loading"></i>
                <div class="loading-text">正在加载壁纸…</div>
              </div>

              <div v-else class="wallpaper-grid">
                <div v-for="wp in allWallpapers" :key="wp.id" class="wallpaper-card"
                  :class="{ 'wallpaper-card--selected': isSelected(wp.id) }" @click="toggleSelection(wp.id)">


                  <div class="wallpaper-thumb">

                    <img :src="wp.url" :alt="wp.name || ('壁纸 ' + wp.id)" />
                    <button v-if="!wp.isBuiltin" class="wallpaper-delete-btn" @click.stop="handleDelete(wp)"
                      title="删除此壁纸">
                      X
                    </button>
                    <span v-if="wp.isBuiltin" class="wallpaper-tag isBuiltin">内置</span>
                  </div>

                  <!-- <div class="wallpaper-info">
                    <div class="wallpaper-name" :title="wp.name || ('壁纸 ' + wp.id)">
                      {{ wp.name || ('壁纸 ' + wp.id) }}
                    </div>
                    <div class="wallpaper-meta-text">
                      ID: {{ wp.id }}
                    </div>
                  </div> -->

                  <div class="wallpaper-check">
                    <el-checkbox :value="isSelected(wp.id)" @change.prevent="toggleSelection(wp.id)">
                    </el-checkbox>
                  </div>
                </div>

                <div v-if="!allWallpapers.length" class="empty">
                  暂无可用壁纸
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧：设备当前壁纸集预览 & 提示 -->
        <!-- <div class="right-panel">
          <div class="result-section">
            <div class="result-header">
              <div class="result-title-row">
                <div class="result-title">设备当前壁纸集</div>
                <div class="result-subtitle">
                  将在设备上线时一次性下发，轮换策略由设备自行决定。
                </div>
              </div>
            </div>

            <div class="result-body wallpaper-summary">
              <div v-if="!selectedIds.length" class="summary-empty">
                当前未为此设备选择任何壁纸。
              </div>
              <div v-else class="summary-list">
                <div
                  v-for="id in selectedIds"
                  :key="id"
                  class="summary-item"
                >
                  <div class="summary-thumb">
                    <img
                      v-if="findWallpaper(id)?.url"
                      :src="findWallpaper(id).url"
                      :alt="findWallpaper(id).name || ('壁纸 ' + id)"
                    />
                    <div v-else class="summary-thumb-placeholder">
                      {{ id }}
                    </div>
                  </div>
                  <div class="summary-info">
                    <div class="summary-name">
                      {{ findWallpaper(id)?.name || ('壁纸 ' + id) }}
                    </div>
                    <div class="summary-meta">
                      ID: {{ id }}
                      <span v-if="findWallpaper(id)?.isBuiltin" class="summary-tag">内置</span>
                    </div>
                  </div>
                </div>
              </div>

              <div class="params-help">
                提示：设备每次启动时会拉取最新的壁纸 ID 集与 URL 列表，
                本页面只负责配置「这台设备可以使用哪些壁纸」。
              </div>
            </div>
          </div>
        </div> -->
      </div>
    </div>

    <span slot="footer" class="dialog-footer">
      <div class="dialog-file-actions">
        <label class="el-button el-button--default el-button--mini">
          <span>选择文件</span>
          <input ref="uploadInput" type="file" accept="image/*" multiple style="display: none"
            @change="handleFileChange" />
        </label>
        <el-button size="mini" type="primary" :disabled="!uploadFiles.length || uploading" @click="doUpload">
          {{ uploading ? `上传中…(${uploadingIndex + 1}/${uploadFiles.length})` : '上传' }}
        </el-button>
        <div class="toolbar-right" v-if="uploadFiles.length">
          <span class="upload-hint">
            待上传：{{ uploadFiles.length }} 张
          </span>
        </div>
      </div>

      <el-button @click="handleClose" :disabled="saving || uploading">取 消</el-button>
      <el-button type="primary" @click="handleSave" :loading="saving">
        保 存
      </el-button>
    </span>
  </el-dialog>
</template>

<script>
import Api from '@/apis/api';

export default {
  name: 'DeviceWallpaperDialog',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    deviceId: {
      type: [String, Number],
      required: true
    }
  },
  emits: ['update:visible'],
  data() {
    return {
      loading: false,
      saving: false,
      allWallpapers: [],   // [{ id, url, isBuiltin }]
      selectedIds: [],

      // 批量上传
      uploadFiles: [],     // File[]
      uploading: false,
      uploadingIndex: 0
    }
  },
  watch: {
    visible(val) {
      if (val) this.loadData()
    },
    deviceId() {
      if (this.visible) this.loadData()
    }
  },
  methods: {
    loadData() {
      if (!this.deviceId) return
      this.loading = true

      let idsDone = false
      let listDone = false

      Api.device.getDeviceWallpaperIds(this.deviceId, (res) => {
        const data = res.data?.data || res.data || res || []
        this.selectedIds = Array.isArray(data) ? [...data] : []
        idsDone = true
        this.checkLoadingDone(idsDone, listDone)
      })

      Api.wallpaper.getWallpaperList((res) => {
        const data = res.data?.data || res.data || res || []
        this.allWallpapers = Array.isArray(data) ? data : []
        listDone = true
        this.checkLoadingDone(idsDone, listDone)
      })
    },
    checkLoadingDone(a, b) {
      if (a && b) this.loading = false
    },

    isSelected(id) {
      return this.selectedIds.includes(id)
    },
    toggleSelection(id) {
      const idx = this.selectedIds.indexOf(id)
      if (idx === -1) {
        this.selectedIds.push(id)
      } else {
        this.selectedIds.splice(idx, 1)
      }
    },
    clearSelection() {
      this.selectedIds = []
    },

    handleFileChange(e) {
      const files = e.target.files ? Array.from(e.target.files) : []
      this.uploadFiles = files
      this.uploadingIndex = 0
    },
    refreshWallpaperList() {
      Api.wallpaper.getWallpaperList((res) => {
        const data = res.data?.data || res.data || res || []
        this.allWallpapers = Array.isArray(data) ? data : []
      })
    },
    doUpload() {
      if (!this.uploadFiles.length || this.uploading) return

      this.uploading = true
      this.uploadingIndex = 0
      const files = [...this.uploadFiles]

      const uploadNext = (index) => {
        if (index >= files.length) {
          this.uploading = false
          this.uploadFiles = []
          this.uploadingIndex = 0
          if (this.$refs.uploadInput) {
            this.$refs.uploadInput.value = ''
          }
          this.refreshWallpaperList()
          return
        }

        this.uploadingIndex = index
        const file = files[index]

        Api.wallpaper.uploadWallpaper(file, file.name, () => {
          uploadNext(index + 1)
        })
      }

      uploadNext(0)
    },

    handleDelete(wp) {
      if (!wp || wp.isBuiltin) return
      if (!window.confirm(`确定要删除壁纸「${wp.name || wp.id}」吗？`)) return

      Api.wallpaper.deleteWallpaper(wp.id, () => {
        // 简单起见：删完重新拉一遍列表
        this.refreshWallpaperList()
        const idx = this.selectedIds.indexOf(wp.id)
        if (idx !== -1) this.selectedIds.splice(idx, 1)
      })
    },

    findWallpaper(id) {
      return this.allWallpapers.find(w => w.id === id) || null
    },

    handleSave() {
      if (!this.deviceId) return
      this.saving = true
      Api.device.setDeviceWallpaperIds(this.deviceId, this.selectedIds, () => {
        this.saving = false
        this.$emit('update:visible', false)
      })
    },
    handleClose() {
      this.$emit('update:visible', false)
    }
  }
}
</script>

<style scoped>
.dialog-content {
  padding: 0;
}

/* 标题区域，沿用 Mcp 样式 */
.dialog-title-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.dialog-title-text {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.dialog-file-actions {
  display: flex;
  gap: 10px;
}

/* 主体布局 */
.main-layout {
  display: flex;
  gap: 20px;
  height: calc(100vh - 260px);
  min-height: 400px;
}

/* 左侧面板 */
.left-panel {
  flex: 1.3;
  display: flex;
  flex-direction: column;
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  overflow: hidden;
}

.tool-list-section {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.section-header {
  padding: 0 20px 20px 20px;
  border-bottom: 1px solid #e4e7ed;
  background: #fafafa;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-icon {
  font-size: 18px;
  color: #5778ff;
}

.section-subtitle {
  font-size: 13px;
  color: #909399;
}

.section-body {
  padding: 16px 20px 20px 20px;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: auto;
}

/* 左侧上方工具栏 */
.wallpaper-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar-badge {
  font-size: 13px;
  padding: 4px 10px;
  border-radius: 14px;
  background: #ecf5ff;
  color: #409eff;
  font-weight: 500;
}

.toolbar-right {
  font-size: 12px;
  color: #909399;
}

/* 左侧网格内容区 */
.tool-list-loading {
  flex: 1;
  min-height: 160px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #909399;
  gap: 8px;
}

.tool-list-loading .el-icon-loading {
  font-size: 24px;
}

.loading-text {
  font-size: 13px;
}

.wallpaper-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

/* 壁纸卡片，参考 .tool-radio 的卡片风格 */
.wallpaper-card {
  position: relative;
  background-color: #f8f9fa;
  border-radius: 12px;
  padding: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.wallpaper-card:hover {
  background-color: #f0f2f5;
  border-color: #e4e7ed;
  transform: translateY(-1px);
}

.wallpaper-card--selected {
  background-color: #e6f7ff;
  border-color: #5778ff;
}

/* 删除按钮 */
.wallpaper-delete-btn {
  position: absolute;
  top: 6px;
  right: 6px;
  border: none;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  font-size: 12px;
  line-height: 18px;
  text-align: center;
  cursor: pointer;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  padding: 0;
}

/* 缩略图 */
.wallpaper-thumb {
  position: relative;
  width: 100%;
  padding-top: 60%;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5f5;
}

.wallpaper-thumb img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.wallpaper-tag {
  position: absolute;
  left: 6px;
  top: 6px;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
  color: #fff;
}

.wallpaper-tag.isBuiltin {
  background: rgba(87, 120, 255, 0.9);
}

/* 卡片文字信息 */
.wallpaper-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.wallpaper-name {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wallpaper-meta-text {
  font-size: 12px;
  color: #909399;
}

.wallpaper-check {
  display: flex;
  justify-content: flex-end;
}

/* 右侧面板 */
.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  overflow: hidden;
}

.result-section {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.result-header {
  padding: 12px 18px;
  border-bottom: 1px solid #e4e7ed;
  background: #fafafa;
}

.result-title-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.result-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.result-subtitle {
  font-size: 12px;
  color: #909399;
}

.result-body {
  padding: 12px 18px 18px 18px;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: auto;
}

.summary-empty {
  font-size: 13px;
  color: #909399;
}

.summary-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 260px;
  overflow: auto;
}

.summary-item {
  display: flex;
  gap: 8px;
  align-items: center;
}

.summary-thumb {
  width: 40px;
  height: 28px;
  border-radius: 6px;
  overflow: hidden;
  background: #f5f5f5;
  flex-shrink: 0;
}

.summary-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.summary-thumb-placeholder {
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  justify-content: center;
}

.summary-info {
  flex: 1;
  min-width: 0;
}

.summary-name {
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-meta {
  font-size: 12px;
  color: #909399;
  display: flex;
  gap: 8px;
  align-items: center;
}

.summary-tag {
  padding: 2px 8px;
  border-radius: 12px;
  background: #ecf5ff;
  color: #409eff;
  font-size: 11px;
}

/* 提示区域 */
.params-help {
  background: #f8f9ff;
  border: 1px solid #ebefff;
  border-radius: 8px;
  padding: 12px;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}

/* 无数据状态 */
.empty {
  grid-column: 1 / -1;
  padding: 24px 0;
  text-align: center;
  font-size: 13px;
  color: #909399;
}

/* 对话框整体样式 */
::v-deep .el-dialog {
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.12);
  overflow: hidden;
  animation: dialogFadeIn 0.3s ease-out;
  max-width: 1200px;
  margin-top: 3% !important;
}

@keyframes dialogFadeIn {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 调整 el-dialog 内边距，跟 Mcp 保持一致 */
::v-deep .el-dialog__header {
  padding: 24px 24px 0;
}

::v-deep .el-dialog__body {
  padding: 20px 24px 0;
  max-height: 80vh;
  overflow: hidden;
}

::v-deep .el-dialog__footer {
  padding: 10px 24px 20px;
}

/* 响应式 */
@media (max-width: 1200px) {
  ::v-deep .el-dialog {
    width: 95% !important;
  }

  .main-layout {
    flex-direction: column;
    height: auto;
    min-height: 0;
  }

  .left-panel,
  .right-panel {
    max-height: 400px;
  }
}
</style>
