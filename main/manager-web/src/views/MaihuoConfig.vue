<template>
  <div class="welcome">
    <HeaderBar />

    <div class="main-wrapper">
      <div class="content-panel">
        <div class="maihuo-layout">
          <el-card class="scheme-sidebar" shadow="never">
            <div class="panel-title">方案列表</div>

            <el-input v-model.trim="newSchemeRoomId" class="room-input" placeholder="输入直播间 ID 后点添加" clearable
              @keyup.enter.native="handleAddScheme">
              <i slot="prefix" class="el-icon-plus"></i>
            </el-input>

            <el-radio-group v-model="activePlatform" size="medium" class="platform-switch">
              <el-radio-button v-for="item in platformOptions" :key="item.value" :label="item.value">
                {{ item.label }}
              </el-radio-button>
            </el-radio-group>

            <el-button type="primary" class="sidebar-add-btn" :loading="addPlanLoading" @click="handleAddScheme">
              添加方案
            </el-button>

            <div v-if="schemes.length" class="scheme-list">
              <div v-for="scheme in schemes" :key="scheme.id" class="scheme-item"
                :class="{ active: selectedSchemeId === scheme.id }" @click="selectScheme(scheme.id)">
                <div class="scheme-item__header">
                  <div class="scheme-item__id">{{ scheme.roomId }}</div>
                  <div class="scheme-item__actions">
                    <button type="button" class="icon-btn" title="复制方案" @click.stop="duplicateScheme(scheme.id)">
                      <i class="el-icon-document-copy"></i>
                    </button>
                    <button type="button" class="icon-btn danger" title="删除方案" @click.stop="removeScheme(scheme.id)">
                      <i class="el-icon-delete"></i>
                    </button>
                  </div>
                </div>
                <div class="scheme-item__name">{{ scheme.name }}</div>
                <div class="scheme-item__meta">
                  <el-tag size="mini" effect="plain" type="success">{{ scheme.status }}</el-tag>
                  <span>{{ scheme.updatedAt }}</span>
                </div>
              </div>
            </div>

            <div v-else class="sidebar-empty">
              <i class="el-icon-folder-opened empty-icon"></i>
              <div class="empty-text">暂无方案</div>
            </div>
          </el-card>

          <div class="scheme-main">
            <template v-if="selectedScheme">
              <el-card class="scheme-summary-card" shadow="never">
                <div class="scheme-summary">
                  <div class="scheme-summary__top">
                    <el-tag effect="plain" class="platform-tag">{{ selectedScheme.platform }}</el-tag>
                    <template v-if="editingField === 'name'">
                      <div class="summary-name-editor">
                        <el-input ref="schemeNameInput" v-model="selectedScheme.name"
                          class="summary-edit summary-name-input" @keyup.enter.native="finishEdit"></el-input>
                        <el-button type="success" icon="el-icon-check" class="summary-action-btn"
                          @click="finishEdit"></el-button>
                        <el-button type="danger" icon="el-icon-close" class="summary-action-btn"
                          @click="cancelEdit"></el-button>
                      </div>
                    </template>
                    <template v-else>
                      <div class="summary-name-text">{{ selectedScheme.name }}</div>
                      <button type="button" class="edit-trigger" @click="startEdit('name')">
                        <i class="el-icon-edit-outline"></i>
                      </button>
                    </template>
                    <el-button type="success" plain class="summary-ai-btn">
                      智能话术
                    </el-button>
                    <el-button plain class="summary-side-btn" @click="handleImportConfig">
                      导入
                    </el-button>
                    <el-button plain class="summary-side-btn" @click="handleExportConfig">
                      导出
                    </el-button>
                  </div>
                  <div class="scheme-summary__bottom">
                    <template v-if="editingField === 'roomId'">
                      <div class="summary-room-editor">
                        <el-input ref="schemeRoomInput" v-model="selectedScheme.roomId"
                          class="summary-edit summary-room-input" @keyup.enter.native="finishEdit"></el-input>
                        <el-button type="success" icon="el-icon-check" class="summary-action-btn"
                          @click="finishEdit"></el-button>
                        <el-button type="danger" icon="el-icon-close" class="summary-action-btn"
                          @click="cancelEdit"></el-button>
                      </div>
                    </template>
                    <template v-else>
                      <div class="summary-room-text">{{ selectedScheme.roomId }}</div>
                      <button type="button" class="edit-trigger" @click="startEdit('roomId')">
                        <i class="el-icon-edit-outline"></i>
                      </button>
                    </template>
                  </div>
                </div>
              </el-card>

              <el-card class="scheme-config-card" shadow="never">
                <div class="config-header">
                  <div class="panel-title">方案面板</div>
                </div>

                <el-tabs v-model="activeTab" class="config-tabs">
                  <el-tab-pane v-for="tab in tabs" :key="tab.name" :label="tab.label" :name="tab.name">
                    <div class="tab-body">
                      <template v-if="tab.name === 'timed'">
                        <div class="form-section">
                          <div class="field-label">启用定时强制</div>
                          <el-switch v-model="selectedScheme.panels.timed.enabled"></el-switch>
                        </div>

                        <div class="form-section compact-section">
                          <div class="field-label">间隔时长（秒）</div>
                          <div class="number-stepper">
                            <input :value="selectedScheme.panels.timed.interval" type="text"
                              class="number-stepper__input"
                              @input="updateNumberField(selectedScheme.panels.timed, 'interval', $event.target.value)" />
                            <div class="number-stepper__actions">
                              <button type="button" class="number-stepper__btn"
                                @click="stepNumberField(selectedScheme.panels.timed, 'interval', -10)">
                                <i class="el-icon-minus"></i>
                              </button>
                              <button type="button" class="number-stepper__btn"
                                @click="stepNumberField(selectedScheme.panels.timed, 'interval', 10)">
                                <i class="el-icon-plus"></i>
                              </button>
                            </div>
                          </div>
                        </div>

                        <div class="form-section">
                          <div class="field-label">按顺序循环发送</div>
                          <el-switch v-model="selectedScheme.panels.timed.sequential"></el-switch>
                        </div>

                        <div class="form-section">
                          <div class="field-label">随机模板</div>
                          <div class="dynamic-list">
                            <div v-for="(item, index) in selectedScheme.panels.timed.templates" :key="`timed-${index}`"
                              class="dynamic-row">
                              <el-input v-model="selectedScheme.panels.timed.templates[index]"></el-input>
                              <div class="row-actions">
                                <button type="button" class="step-btn"
                                  @click="removeListItem(selectedScheme.panels.timed.templates, index)">
                                  <i class="el-icon-minus"></i>
                                </button>
                                <button type="button" class="step-btn"
                                  @click="addTextListItem(selectedScheme.panels.timed.templates, '新增定时强制话术')">
                                  <i class="el-icon-plus"></i>
                                </button>
                              </div>
                            </div>
                          </div>
                        </div>
                      </template>

                      <template v-else-if="tab.name === 'awkward'">
                        <div class="form-section">
                          <div class="field-label">启用防冷场</div>
                          <el-switch v-model="selectedScheme.panels.awkward.enabled"></el-switch>
                        </div>

                        <div class="form-section compact-section">
                          <div class="field-label">冷场判断时长（秒）</div>
                          <div class="number-stepper">
                            <input :value="selectedScheme.panels.awkward.interval" type="text"
                              class="number-stepper__input"
                              @input="updateNumberField(selectedScheme.panels.awkward, 'interval', $event.target.value)" />
                            <div class="number-stepper__actions">
                              <button type="button" class="number-stepper__btn"
                                @click="stepNumberField(selectedScheme.panels.awkward, 'interval', -10)">
                                <i class="el-icon-minus"></i>
                              </button>
                              <button type="button" class="number-stepper__btn"
                                @click="stepNumberField(selectedScheme.panels.awkward, 'interval', 10)">
                                <i class="el-icon-plus"></i>
                              </button>
                            </div>
                          </div>
                        </div>

                        <div class="form-section">
                          <div class="field-label">按顺序循环发送</div>
                          <el-switch v-model="selectedScheme.panels.awkward.sequential"></el-switch>
                        </div>

                        <div class="form-section">
                          <div class="field-label">防冷场可被打断</div>
                          <el-switch v-model="selectedScheme.panels.awkward.interruptEnabled"></el-switch>
                        </div>

                        <div class="form-section">
                          <div class="field-label">随机模板</div>
                          <div class="dynamic-list">
                            <div v-for="(item, index) in selectedScheme.panels.awkward.templates"
                              :key="`awkward-${index}`" class="dynamic-row">
                              <el-input v-model="selectedScheme.panels.awkward.templates[index]"></el-input>
                              <div class="row-actions">
                                <button type="button" class="step-btn"
                                  @click="removeListItem(selectedScheme.panels.awkward.templates, index)">
                                  <i class="el-icon-minus"></i>
                                </button>
                                <button type="button" class="step-btn"
                                  @click="addTextListItem(selectedScheme.panels.awkward.templates, '新增防冷场话术')">
                                  <i class="el-icon-plus"></i>
                                </button>
                              </div>
                            </div>
                          </div>
                        </div>
                      </template>

                      <template v-else-if="tab.name === 'manual'">
                        <div class="form-section compact-section">
                          <div class="field-label">只处理最新的几条消息</div>
                          <div class="number-stepper">
                            <input :value="selectedScheme.panels.manual.latestCount" type="text"
                              class="number-stepper__input"
                              @input="updateNumberField(selectedScheme.panels.manual, 'latestCount', $event.target.value)" />
                            <div class="number-stepper__actions">
                              <button type="button" class="number-stepper__btn"
                                @click="stepNumberField(selectedScheme.panels.manual, 'latestCount', -1)">
                                <i class="el-icon-minus"></i>
                              </button>
                              <button type="button" class="number-stepper__btn"
                                @click="stepNumberField(selectedScheme.panels.manual, 'latestCount', 1)">
                                <i class="el-icon-plus"></i>
                              </button>
                            </div>
                          </div>
                        </div>

                        <div class="form-section">
                          <div class="field-label">固定模板</div>
                          <el-input v-model="selectedScheme.panels.manual.fixedTemplate"></el-input>
                        </div>
                      </template>

                      <template v-else-if="tab.name === 'basic'">
                        <div class="form-section">
                          <div class="field-label">忽略纯数字用户名</div>
                          <el-switch v-model="selectedScheme.panels.basic.ignoreNumericName"></el-switch>
                        </div>

                        <div class="form-section">
                          <div class="field-label">忽略包含多个*的用户名</div>
                          <el-switch v-model="selectedScheme.panels.basic.ignoreMaskedName"></el-switch>
                        </div>
                      </template>

                      <template v-else-if="tab.name === 'danmu'">
                        <div class="form-section compact-section">
                          <div class="field-label">只处理最新的几条消息</div>
                          <div class="number-stepper">
                            <input :value="selectedScheme.panels.danmu.latestCount" type="text"
                              class="number-stepper__input"
                              @input="updateNumberField(selectedScheme.panels.danmu, 'latestCount', $event.target.value)" />
                            <div class="number-stepper__actions">
                              <button type="button" class="number-stepper__btn"
                                @click="stepNumberField(selectedScheme.panels.danmu, 'latestCount', -1)">
                                <i class="el-icon-minus"></i>
                              </button>
                              <button type="button" class="number-stepper__btn"
                                @click="stepNumberField(selectedScheme.panels.danmu, 'latestCount', 1)">
                                <i class="el-icon-plus"></i>
                              </button>
                            </div>
                          </div>
                        </div>

                        <div class="form-section">
                          <div class="field-label">固定模板</div>
                          <el-input v-model="selectedScheme.panels.danmu.fixedTemplate"></el-input>
                        </div>

                        <div class="form-section">
                          <div class="field-label">关键词回复</div>
                          <div class="dynamic-list">
                            <div v-for="(item, index) in selectedScheme.panels.danmu.keywordReplies"
                              :key="`keyword-${index}`" class="dynamic-row pair-row">
                              <el-input v-model="item.keyword" class="pair-keyword" placeholder="关键词"></el-input>
                              <el-input v-model="item.reply" placeholder="回复内容"></el-input>
                              <div class="row-actions">
                                <button type="button" class="step-btn" @click="removeKeywordReply(index)">
                                  <i class="el-icon-minus"></i>
                                </button>
                                <button type="button" class="step-btn" @click="addKeywordReply">
                                  <i class="el-icon-plus"></i>
                                </button>
                              </div>
                            </div>
                          </div>
                        </div>

                        <div class="form-section">
                          <div class="field-label">屏蔽词跳过</div>
                          <div class="dynamic-list">
                            <div v-for="(item, index) in selectedScheme.panels.danmu.blockedKeywords"
                              :key="`block-${index}`" class="dynamic-row">
                              <el-input v-model="selectedScheme.panels.danmu.blockedKeywords[index]"></el-input>
                              <div class="row-actions">
                                <button type="button" class="step-btn"
                                  @click="removeListItem(selectedScheme.panels.danmu.blockedKeywords, index)">
                                  <i class="el-icon-minus"></i>
                                </button>
                                <button type="button" class="step-btn"
                                  @click="addTextListItem(selectedScheme.panels.danmu.blockedKeywords, '新增屏蔽词')">
                                  <i class="el-icon-plus"></i>
                                </button>
                              </div>
                            </div>
                          </div>
                        </div>
                      </template>

                      <template v-else-if="
                        ['welcome', 'like', 'follow', 'gift'].includes(tab.name)
                      ">
                        <div class="form-section compact-section">
                          <div class="field-label">只处理最新的几条消息</div>
                          <div class="number-stepper">
                            <input :value="selectedScheme.panels[tab.name].latestCount" type="text"
                              class="number-stepper__input"
                              @input="updateNumberField(selectedScheme.panels[tab.name], 'latestCount', $event.target.value)" />
                            <div class="number-stepper__actions">
                              <button type="button" class="number-stepper__btn"
                                @click="stepNumberField(selectedScheme.panels[tab.name], 'latestCount', -1)">
                                <i class="el-icon-minus"></i>
                              </button>
                              <button type="button" class="number-stepper__btn"
                                @click="stepNumberField(selectedScheme.panels[tab.name], 'latestCount', 1)">
                                <i class="el-icon-plus"></i>
                              </button>
                            </div>
                          </div>
                        </div>

                        <div class="form-section">
                          <div class="field-label">
                            {{ tab.name === "welcome" ? "固定模板" : "随机模板" }}
                          </div>
                          <div class="dynamic-list">
                            <div v-for="(item, index) in selectedScheme.panels[tab.name].templates"
                              :key="`${tab.name}-${index}`" class="dynamic-row">
                              <el-input v-model="selectedScheme.panels[tab.name].templates[index]"></el-input>
                              <div class="row-actions">
                                <button type="button" class="step-btn"
                                  @click="removeListItem(selectedScheme.panels[tab.name].templates, index)">
                                  <i class="el-icon-minus"></i>
                                </button>
                                <button type="button" class="step-btn"
                                  @click="addTextListItem(selectedScheme.panels[tab.name].templates, getTemplatePlaceholder(tab.name))">
                                  <i class="el-icon-plus"></i>
                                </button>
                              </div>
                            </div>
                          </div>
                        </div>
                      </template>

                      <template v-else>
                        <div class="placeholder-pane">该分组暂未补充，后续可继续扩展。</div>
                      </template>
                    </div>
                  </el-tab-pane>
                </el-tabs>

                <div class="footer-actions">
                  <el-button @click="resetScheme">重置</el-button>
                  <el-button type="primary" :loading="savePlanLoading" @click="handleSaveScheme">
                    保存方案
                  </el-button>
                </div>
              </el-card>
            </template>

            <div v-else class="main-empty">
              <i class="el-icon-folder-opened empty-icon"></i>
              <div class="empty-title">请先添加方案</div>
              <el-button type="primary" disabled>添加方案</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script>
import Api from '@/apis/api';
import HeaderBar from "@/components/HeaderBar.vue";

const createFeedbackPanel = (latestCount, templates) => ({
  latestCount,
  templates,
});

const createDanmuPanel = () => ({
  latestCount: 5,
  fixedTemplate:
    "【弹幕】{name} 说: [{text}]",
  keywordReplies: [
    { keyword: "合作", reply: "{name} 想合作，你让他私信主播联系" },
    { keyword: "价格", reply: "{name} 问价格，你可以告诉他今天的优惠" },
  ],
  blockedKeywords: ["垃圾", "100遍", "伊斯兰教", "基督教", "佛教", "股票", "风水", "算命", "比特币", "区块链", "主席", "书记", "习大大", "习近平", "毛泽东", "邓小平", "江泽民", "胡锦涛", "你妈", "傻逼", "举报", "共产党", "违规", "无人", "少羽", "音乐", "运营", "管理员", "开发者", "音量", "退款", "无聊"],
});

const createDefaultPanels = () => ({
  timed: {
    enabled: false,
    interval: 180,
    sequential: false,
    templates: [
      "三二一上一下链接，制造一下库存的紧张感",
      "说3个让用户非买不可的理由",
      "今天直播间有哪些优惠给大家介绍下",
    ],
  },
  awkward: {
    enabled: false,
    interval: 120,
    sequential: false,
    interruptEnabled: false,
    templates: [
      "现在冷场了，从你上次冷场聊的话题从找一个点接着聊。你可以这样开头'刚才我们聊到...'",
      "随便强调一下直播间福利和限时活动",
      "问观众还有什么问题或者想看的内容，调动一下氛围",
    ],
  },
  manual: {
    latestCount: 10,
    fixedTemplate: "【Admin】{text}",
  },
  basic: {
    ignoreNumericName: true,
    ignoreMaskedName: true,
  },
  welcome: createFeedbackPanel(3, ['【进场】{name} 进入直播间']),
  danmu: createDanmuPanel(),
  like: createFeedbackPanel(3, ["【点赞】{name} 给你点了 {count} 个赞"]),
  follow: createFeedbackPanel(3, ["【关注】{name} 关注了你"]),
  gift: createFeedbackPanel(3, ["【送礼】{name} 给你送了 {count} 个 {giftName}"]),
});

const createScheme = (roomId, platform, index = 0) => ({
  id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
  planNo: "",
  roomId,
  platform,
  name: `${platform}直播方案${index ? ` ${index}` : ""}`,
  status: "空闲",
  updatedAt: "刚刚",
  panels: createDefaultPanels(),
});

const parsePanelNumber = (value, fallback) => {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
};

const normalizePanels = (rawPanels = {}) => {
  const defaults = createDefaultPanels();
  const merged = JSON.parse(JSON.stringify(defaults));

  if (rawPanels.timed) {
    merged.timed.enabled = !!rawPanels.timed.enabled;
    merged.timed.interval = parsePanelNumber(rawPanels.timed.interval, defaults.timed.interval);
    merged.timed.sequential = !!rawPanels.timed.sequential;
    merged.timed.templates = Array.isArray(rawPanels.timed.templates)
      ? rawPanels.timed.templates.map((item) => String(item))
      : defaults.timed.templates;
  }

  if (rawPanels.awkward) {
    merged.awkward.enabled = !!rawPanels.awkward.enabled;
    merged.awkward.interval = parsePanelNumber(
      rawPanels.awkward.interval,
      defaults.awkward.interval
    );
    merged.awkward.sequential = !!rawPanels.awkward.sequential;
    merged.awkward.interruptEnabled = !!rawPanels.awkward.interruptEnabled;
    merged.awkward.templates = Array.isArray(rawPanels.awkward.templates)
      ? rawPanels.awkward.templates.map((item) => String(item))
      : defaults.awkward.templates;
  }

  if (rawPanels.manual) {
    merged.manual.latestCount = parsePanelNumber(
      rawPanels.manual.latestCount,
      defaults.manual.latestCount
    );
    merged.manual.fixedTemplate =
      typeof rawPanels.manual.fixedTemplate === "string"
        ? rawPanels.manual.fixedTemplate
        : defaults.manual.fixedTemplate;
  }

  if (rawPanels.basic) {
    merged.basic.ignoreNumericName = !!rawPanels.basic.ignoreNumericName;
    merged.basic.ignoreMaskedName = !!rawPanels.basic.ignoreMaskedName;
  }

  ["welcome", "like", "follow", "gift"].forEach((key) => {
    if (!rawPanels[key]) {
      return;
    }
    merged[key].latestCount = parsePanelNumber(
      rawPanels[key].latestCount,
      defaults[key].latestCount
    );
    merged[key].templates = Array.isArray(rawPanels[key].templates)
      ? rawPanels[key].templates.map((item) => String(item))
      : defaults[key].templates;
  });

  if (rawPanels.danmu) {
    merged.danmu.latestCount = parsePanelNumber(
      rawPanels.danmu.latestCount,
      defaults.danmu.latestCount
    );
    merged.danmu.fixedTemplate =
      typeof rawPanels.danmu.fixedTemplate === "string"
        ? rawPanels.danmu.fixedTemplate
        : defaults.danmu.fixedTemplate;
    merged.danmu.keywordReplies = Array.isArray(rawPanels.danmu.keywordReplies)
      ? rawPanels.danmu.keywordReplies.map((item) => ({
        keyword: item && item.keyword ? String(item.keyword) : "",
        reply: item && item.reply ? String(item.reply) : "",
      }))
      : defaults.danmu.keywordReplies;
    merged.danmu.blockedKeywords = Array.isArray(rawPanels.danmu.blockedKeywords)
      ? rawPanels.danmu.blockedKeywords.map((item) => String(item))
      : defaults.danmu.blockedKeywords;
  }

  return merged;
};

const formatRelativeTime = (value) => {
  if (!value) {
    return "刚刚";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "刚刚";
  }

  const diffMs = Date.now() - date.getTime();
  const diffMinutes = Math.floor(diffMs / 60000);

  if (diffMinutes < 1) {
    return "刚刚";
  }
  if (diffMinutes < 60) {
    return `${diffMinutes} 分钟前`;
  }

  const diffHours = Math.floor(diffMinutes / 60);
  if (diffHours < 24) {
    return `${diffHours} 小时前`;
  }

  const diffDays = Math.floor(diffHours / 24);
  return `${diffDays} 天前`;
};

const mapLivePlanToScheme = (plan) => ({
  id: plan.planNo || plan.id,
  planNo: plan.planNo || "",
  roomId: plan.roomId || "",
  platform: plan.platform || "抖音",
  name: plan.planName || "",
  status: Number(plan.status) === 1 ? "使用中" : "空闲",
  updatedAt: formatRelativeTime(plan.updateDate || plan.createDate),
  panels: normalizePanels(plan.configJson),
});

export default {
  name: "MaihuoConfig",
  components: {
    HeaderBar,
  },
  created() {
    this.fetchLivePlanList();
  },
  data() {
    return {
      newSchemeRoomId: "",
      activePlatform: "抖音",
      selectedSchemeId: "",
      activeTab: "timed",
      addPlanLoading: false,
      listLoading: false,
      savePlanLoading: false,
      editingField: "",
      editSnapshot: "",
      platformOptions: [
        { label: "抖音", value: "抖音" },
        // { label: "TikTok", value: "TikTok" },
        // { label: "视频号", value: "视频号" },
      ],
      tabs: [
        { label: "定时强制", name: "timed" },
        { label: "防冷场", name: "awkward" },
        { label: "手动指挥机器人", name: "manual" },
        { label: "直播间基础设置", name: "basic" },
        { label: "进入欢迎设置", name: "welcome" },
        { label: "弹幕互动设置", name: "danmu" },
        { label: "点赞反馈设置", name: "like" },
        { label: "关注反馈设置", name: "follow" },
        { label: "礼物反馈设置", name: "gift" },
      ],
      schemes: [],
    };
  },
  computed: {
    selectedScheme() {
      return this.schemes.find((item) => item.id === this.selectedSchemeId) || null;
    },
  },
  methods: {
    async readClipboardText() {
      if (!navigator.clipboard || !navigator.clipboard.readText) {
        throw new Error("当前环境不支持读取剪切板");
      }
      return navigator.clipboard.readText();
    },
    async writeClipboardText(text) {
      if (!navigator.clipboard || !navigator.clipboard.writeText) {
        throw new Error("当前环境不支持写入剪切板");
      }
      return navigator.clipboard.writeText(text);
    },
    sanitizePanels(rawPanels = {}) {
      return normalizePanels(rawPanels);
    },
    buildExportPanels() {
      if (!this.selectedScheme) {
        return null;
      }
      return JSON.parse(JSON.stringify(this.selectedScheme.panels));
    },
    buildLivePlanPayload(roomId) {
      return {
        planName: `${this.activePlatform}${roomId}方案`,
        platform: this.activePlatform,
        roomId,
        configJson: createDefaultPanels(),
        status: 0,
        remark: "",
      };
    },
    buildUpdatePayload(scheme) {
      return {
        planName: scheme.name,
        platform: scheme.platform,
        roomId: scheme.roomId,
        configJson: JSON.parse(JSON.stringify(scheme.panels)),
        status: scheme.status === "使用中" ? 1 : 0,
        remark: "",
      };
    },
    fetchLivePlanList() {
      this.listLoading = true;
      Api.livePlan.getLivePlanList({ page: 1, limit: 100 }, ({ data }) => {
        this.listLoading = false;

        if (data && data.code === 0) {
          const list = (((data.data || {}).list) || []).map((item) => mapLivePlanToScheme(item));
          this.schemes = list;
          if (!list.length) {
            this.selectedSchemeId = "";
            return;
          }
          const stillExists = list.some((item) => item.id === this.selectedSchemeId);
          this.selectedSchemeId = stillExists ? this.selectedSchemeId : list[0].id;
          return;
        }

        this.$message.error((data && data.msg) || "获取方案列表失败");
      });
    },
    async handleExportConfig() {
      if (!this.selectedScheme) {
        this.$message.warning("请先选择方案");
        return;
      }

      try {
        const exportText = JSON.stringify(this.buildExportPanels(), null, 2);
        await this.writeClipboardText(exportText);
        this.$message.success("当前方案配置已复制到剪切板");
      } catch (error) {
        this.$message.error(error.message || "导出失败");
      }
    },
    async handleImportConfig() {
      if (!this.selectedScheme) {
        this.$message.warning("请先选择方案");
        return;
      }

      try {
        const clipboardText = await this.readClipboardText();
        const parsed = JSON.parse(clipboardText);
        this.selectedScheme.panels = this.sanitizePanels(parsed);
        this.$message.success("已从剪切板导入当前方案配置");
      } catch (error) {
        this.$message.error(error.message || "导入失败，请确认剪切板内容为合法 JSON");
      }
    },
    handleAddScheme() {
      if (!this.newSchemeRoomId) {
        this.$message.warning("请输入直播间 ID");
        return;
      }

      if (this.addPlanLoading) {
        return;
      }

      const roomId = this.newSchemeRoomId;
      const payload = this.buildLivePlanPayload(roomId);
      this.addPlanLoading = true;

      Api.livePlan.addLivePlan(payload, ({ data }) => {
        this.addPlanLoading = false;

        if (data && data.code === 0) {
          this.selectedSchemeId = data.data || "";
          this.activeTab = "timed";
          this.newSchemeRoomId = "";
          this.fetchLivePlanList();
          this.$message.success("方案创建成功");
          return;
        }

        this.$message.error((data && data.msg) || "方案创建失败");
      });
    },
    selectScheme(id) {
      this.selectedSchemeId = id;
    },
    duplicateScheme(id) {
      const target = this.schemes.find((item) => item.id === id);
      if (!target) {
        return;
      }

      if (this.addPlanLoading) {
        return;
      }

      const payload = {
        planName: `${target.name} - 复制`,
        platform: target.platform,
        roomId: target.roomId,
        configJson: JSON.parse(JSON.stringify(target.panels)),
        status: target.status === "使用中" ? 1 : 0,
        remark: "",
      };

      this.addPlanLoading = true;
      Api.livePlan.addLivePlan(payload, ({ data }) => {
        this.addPlanLoading = false;

        if (data && data.code === 0) {
          this.selectedSchemeId = data.data || "";
          this.fetchLivePlanList();
          this.$message.success("方案复制成功");
          return;
        }

        this.$message.error((data && data.msg) || "方案复制失败");
      });
    },
    removeScheme(id) {
      const target = this.schemes.find((item) => item.id === id);
      if (!target || !target.planNo) {
        return;
      }

      this.$confirm("确定删除这个方案吗？删除后不可恢复。", "删除确认", {
        confirmButtonText: "删除",
        cancelButtonText: "取消",
        type: "warning",
      })
        .then(() => {
          Api.livePlan.deleteLivePlan(target.planNo, ({ data }) => {
            if (data && data.code === 0) {
              if (this.selectedSchemeId === id) {
                this.selectedSchemeId = "";
              }
              this.fetchLivePlanList();
              this.$message.success("方案删除成功");
              return;
            }

            this.$message.error((data && data.msg) || "方案删除失败");
          });
        })
        .catch(() => { });
    },
    addTextListItem(list, value) {
      list.push(value);
    },
    updateNumberField(target, key, value, min = 0) {
      const digitsOnly = String(value).replace(/[^\d]/g, "");
      if (digitsOnly === "") {
        target[key] = min;
        return;
      }
      target[key] = Math.max(min, Number(digitsOnly));
    },
    stepNumberField(target, key, delta, min = 0) {
      const currentValue = Number(target[key]) || min;
      target[key] = Math.max(min, currentValue + delta);
    },
    removeListItem(list, index) {
      if (list.length === 1) {
        list.splice(index, 1, "");
        return;
      }
      list.splice(index, 1);
    },
    getTemplatePlaceholder(tabName) {
      const map = {
        welcome: "新增欢迎话术",
        like: "新增点赞反馈话术",
        follow: "新增关注反馈话术",
        gift: "新增礼物反馈话术",
      };
      return map[tabName] || "新增模板";
    },
    addKeywordReply() {
      if (!this.selectedScheme) {
        return;
      }
      this.selectedScheme.panels.danmu.keywordReplies.push({
        keyword: "",
        reply: "",
      });
    },
    removeKeywordReply(index) {
      if (!this.selectedScheme) {
        return;
      }
      const list = this.selectedScheme.panels.danmu.keywordReplies;
      if (list.length === 1) {
        list.splice(index, 1, { keyword: "", reply: "" });
        return;
      }
      list.splice(index, 1);
    },
    startEdit(field) {
      if (!this.selectedScheme) {
        return;
      }
      this.editingField = field;
      this.editSnapshot = this.selectedScheme[field];
      this.$nextTick(() => {
        const refName = field === "name" ? "schemeNameInput" : "schemeRoomInput";
        const inputRef = this.$refs[refName];
        if (inputRef && inputRef.focus) {
          inputRef.focus();
        }
      });
    },
    finishEdit() {
      this.editingField = "";
      this.editSnapshot = "";
    },
    cancelEdit() {
      if (!this.selectedScheme || !this.editingField) {
        return;
      }
      this.selectedScheme[this.editingField] = this.editSnapshot;
      this.editingField = "";
      this.editSnapshot = "";
    },
    resetScheme() {
      if (!this.selectedScheme) {
        return;
      }
      this.selectedScheme.panels = createDefaultPanels();
      this.activeTab = "timed";
    },
    handleSaveScheme() {
      if (!this.selectedScheme || !this.selectedScheme.planNo) {
        this.$message.warning("请先选择方案");
        return;
      }

      if (this.savePlanLoading) {
        return;
      }

      this.savePlanLoading = true;
      const payload = this.buildUpdatePayload(this.selectedScheme);

      Api.livePlan.updateLivePlan(this.selectedScheme.planNo, payload, ({ data }) => {
        this.savePlanLoading = false;

        if (data && data.code === 0) {
          this.fetchLivePlanList();
          this.$message.success("方案保存成功");
          return;
        }

        this.$message.error((data && data.msg) || "方案保存失败");
      });
    },
  },
};
</script>

<style scoped>
.welcome {
  min-height: 100vh;
  background: #f6f8fb;
}

.operation-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 32px 12px;
}

.page-title {
  margin: 0;
  font-size: 28px;
  font-weight: 600;
  color: #1f2937;
}

.main-wrapper {
  padding: 0 32px 32px;
}

.content-panel {
  width: 100%;
}

.maihuo-layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.scheme-sidebar {
  width: 340px;
  border-radius: 20px;
  border: 1px solid #e8edf5;
}

.panel-title {
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 16px;
}

.room-input {
  margin-bottom: 16px;
}

.room-input /deep/ .el-input__prefix {
  left: 12px;
  display: flex;
  align-items: center;
  height: 100%;
  color: #b8c1d1;
}

.room-input /deep/ .el-input__inner {
  padding-left: 40px;
}

.platform-switch {
  display: flex;
  margin-bottom: 16px;
}

.platform-switch /deep/ .el-radio-button {
  flex: 1;
}

.platform-switch /deep/ .el-radio-button__inner {
  width: 100%;
  border-radius: 0;
  border-color: #d8e1ee;
  box-shadow: none;
  padding: 10px 0;
  font-weight: 600;
}

.platform-switch /deep/ .el-radio-button:first-child .el-radio-button__inner {
  border-radius: 10px 0 0 10px;
}

.platform-switch /deep/ .el-radio-button:last-child .el-radio-button__inner {
  border-radius: 0 10px 10px 0;
}

.platform-switch /deep/ .el-radio-button__orig-radio:checked+.el-radio-button__inner {
  background: #edf8f1;
  border-color: #67c587;
  color: #2f9f59;
}

.sidebar-add-btn {
  width: 100%;
  height: 44px;
  margin-bottom: 16px;
  border-radius: 10px;
  padding: 0 16px;
}

.scheme-list {
  max-height: calc(100vh - 340px);
  overflow-y: auto;
  padding-right: 4px;
}

.scheme-item {
  padding: 14px 16px;
  border: 1px solid transparent;
  border-radius: 16px;
  background: #ffffff;
  cursor: pointer;
  transition: all 0.2s ease;
}

.scheme-item+.scheme-item {
  margin-top: 14px;
}

.scheme-item:hover,
.scheme-item.active {
  background: #f3f6fb;
  border-color: #dfe8f5;
}

.scheme-item__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 6px;
}

.scheme-item__id {
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
}

.scheme-item__actions {
  display: flex;
  gap: 8px;
}

.icon-btn {
  border: none;
  padding: 0;
  background: transparent;
  color: #4e7cf0;
  cursor: pointer;
  font-size: 16px;
}

.icon-btn.danger {
  color: #e0565b;
}

.scheme-item__name {
  line-height: 1.6;
  min-height: 34px;
  margin-bottom: 10px;
  color: #667085;
  text-align: left;
  font-size: 14px;
}

.scheme-item__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #98a2b3;
  font-size: 13px;
}

.scheme-main {
  flex: 1;
  min-width: 0;
}

.scheme-summary-card,
.scheme-config-card {
  border-radius: 20px;
  border: 1px solid #e8edf5;
}

.scheme-summary-card {
  margin-bottom: 20px;
}

.scheme-summary {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: flex-start;
}

.scheme-summary__top {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.platform-tag {
  color: #4e7cf0;
  border-color: #bdd1ff;
  background: #f2f6ff;
}

.scheme-summary__name {
  font-size: 24px;
  font-weight: 700;
  color: #1f2937;
}

.summary-name-text {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
  line-height: 1.3;
}

.edit-trigger {
  width: 26px;
  height: 26px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #667085;
  cursor: pointer;
  font-size: 15px;
}

.edit-trigger:hover {
  background: #f2f4f7;
}

.summary-edit /deep/ .el-input__inner {
  border-radius: 10px;
  border-color: #d0d5dd;
}

.summary-name-input {
  width: 260px;
}

.summary-name-input /deep/ .el-input__inner {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
  height: 40px;
}

.summary-name-editor {
  display: flex;
  align-items: center;
  gap: 8px;
}

.scheme-summary__bottom {
  display: flex;
  align-items: center;
  gap: 8px;
}

.summary-room-text {
  font-size: 14px;
  font-weight: 600;
  color: #667085;
}

.summary-room-editor {
  display: flex;
  align-items: center;
  gap: 8px;
}

.summary-room-input {
  width: 260px;
}

.summary-room-input /deep/ .el-input__inner {
  height: 40px;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.summary-action-btn {
  width: 40px;
  height: 40px;
  padding: 0;
  border-radius: 10px;
}

.summary-ai-btn {
  border-radius: 10px;
  height: 40px;
  padding: 0 14px;
}

.summary-side-btn {
  border-radius: 10px;
  height: 40px;
  padding: 0 14px;
}

.scheme-summary__bottom {
  font-size: 18px;
  font-weight: 600;
  color: #667085;
  text-align: left;
}

.config-header {
  margin-bottom: 12px;
}

.config-tabs /deep/ .el-tabs__nav-wrap::after {
  height: 1px;
  background: #e7edf6;
}

.config-tabs /deep/ .el-tabs__nav-wrap.is-scrollable {
  padding: 0;
}

.config-tabs /deep/ .el-tabs__nav-prev,
.config-tabs /deep/ .el-tabs__nav-next {
  display: none;
}

.config-tabs /deep/ .el-tabs__nav-scroll {
  overflow-x: auto;
  scrollbar-width: none;
}

.config-tabs /deep/ .el-tabs__nav-scroll::-webkit-scrollbar {
  display: none;
}

.config-tabs /deep/ .el-tabs__nav {
  float: none;
  display: flex;
  min-width: max-content;
}

.config-tabs /deep/ .el-tabs__item {
  height: 42px;
  line-height: 42px;
  font-size: 14px;
  color: #344054;
  padding: 0 18px;
}

.config-tabs /deep/ .el-tabs__item.is-active {
  color: #2f9f59;
  font-weight: 600;
}

.config-tabs /deep/ .el-tabs__active-bar {
  background: #2f9f59;
  height: 3px;
  border-radius: 999px;
}

.tab-body {
  padding-top: 4px;
  text-align: left;
}

.form-section {
  margin-bottom: 22px;
}

.compact-section {
  max-width: 320px;
}

.field-label {
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.number-stepper {
  width: 250px;
  height: 42px;
  display: flex;
  align-items: stretch;
  border: 1px solid #d0d5dd;
  border-radius: 10px;
  background: #fff;
  overflow: hidden;
}

.number-stepper__input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  padding: 0 14px;
  font-size: 14px;
  color: #1f2937;
}

.number-stepper__actions {
  display: flex;
  align-items: center;
  border-left: 1px solid #d0d5dd;
}

.number-stepper__btn {
  width: 40px;
  border: none;
  background: transparent;
  color: #1f2937;
  font-size: 16px;
  cursor: pointer;
}

.number-stepper__btn+.number-stepper__btn {
  border-left: 1px solid #d0d5dd;
}

.dynamic-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dynamic-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dynamic-row /deep/ .el-input {
  flex: 1;
}

.pair-row {
  align-items: stretch;
}

.pair-row /deep/ .el-input {
  flex: 1;
}

.pair-keyword {
  width: 172px;
  flex: none;
}

.row-actions {
  display: flex;
  overflow: hidden;
  border-radius: 999px;
  border: 1px solid #d8e1ee;
  background: #fff;
}

.step-btn {
  width: 40px;
  height: 38px;
  border: none;
  background: transparent;
  color: #1f2937;
  cursor: pointer;
  font-size: 15px;
}

.step-btn+.step-btn {
  border-left: 1px solid #d8e1ee;
}

.placeholder-pane {
  min-height: 320px;
  border: 1px dashed #d8e1ee;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #98a2b3;
  background: #fafcff;
}

.footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 36px;
}

.sidebar-empty,
.main-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #b6bdc9;
}

.sidebar-empty {
  min-height: 240px;
}

.main-empty {
  min-height: 620px;
  border-radius: 20px;
  border: 1px dashed #d8e1ee;
  background: #fff;
}

.empty-icon {
  font-size: 54px;
  margin-bottom: 16px;
}

.empty-text,
.empty-title {
  font-size: 28px;
  margin-bottom: 14px;
}

.empty-text {
  font-size: 18px;
}

@media (max-width: 1280px) {
  .maihuo-layout {
    flex-direction: column;
  }

  .scheme-sidebar {
    width: 100%;
  }

  .scheme-list {
    max-height: none;
  }

  .dynamic-row,
  .pair-row {
    flex-wrap: wrap;
  }

  .row-actions {
    margin-left: auto;
  }
}
</style>
