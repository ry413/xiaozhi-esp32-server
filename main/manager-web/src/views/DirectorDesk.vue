<template>
  <div class="welcome">
    <HeaderBar />

    <div class="main-wrapper">
      <div class="content-panel">
        <div class="desk-layout">
          <el-card class="robot-sidebar" shadow="never">
            <div class="panel-title">机器人列表</div>

            <div v-if="robots.length" class="robot-list">
              <div
                v-for="robot in robots"
                :key="robot.id"
                class="robot-item"
                :class="{ active: selectedRobotId === robot.id }"
                @click="selectRobot(robot.id)"
              >
                <div class="robot-item__header">
                  <div class="robot-item__id">{{ robot.mac }}</div>
                </div>
                <div class="robot-item__name">{{ robot.name }}</div>
                <div class="robot-item__meta">
                  <el-tag
                    size="mini"
                    effect="plain"
                    :type="robot.online ? 'success' : 'info'"
                  >
                    {{ robot.online ? "在线" : "离线" }}
                  </el-tag>
                  <span>{{ robot.updatedAt }}</span>
                </div>
              </div>
            </div>

            <div v-else class="sidebar-empty">
              <i class="el-icon-cpu empty-icon"></i>
              <div class="empty-text">暂无机器人</div>
            </div>
          </el-card>

          <div class="desk-main">
            <template v-if="selectedRobot">
              <el-card class="director-summary-card" shadow="never">
                <div class="director-summary">
                  <div class="director-summary__top">
                    <div class="summary-top-main">
                      <template v-if="editingRobotName">
                        <div class="name-editor">
                          <el-input
                            ref="robotNameInput"
                            v-model="selectedRobot.name"
                            class="summary-edit summary-name-input"
                            @keyup.enter.native="finishRobotNameEdit"
                          ></el-input>
                          <el-button
                            type="success"
                            icon="el-icon-check"
                            class="summary-action-btn"
                            @click="finishRobotNameEdit"
                          ></el-button>
                          <el-button
                            type="danger"
                            icon="el-icon-close"
                            class="summary-action-btn"
                            @click="cancelRobotNameEdit"
                          ></el-button>
                        </div>
                      </template>
                      <template v-else>
                        <div class="summary-name-text">{{ selectedRobot.name }}</div>
                        <!-- <button type="button" class="edit-trigger" @click="startRobotNameEdit">
                          <i class="el-icon-edit-outline"></i>
                        </button> -->
                      </template>
                    </div>

                    <div class="auto-close-notice">
                      <div class="auto-close-notice__title">系统将在以下情况自动关闭导播</div>
                      <div class="auto-close-notice__item">激活码过期超过 30 分钟</div>
                      <div class="auto-close-notice__item">机器人关闭超过 30 分钟</div>
                      <div class="auto-close-notice__item">直播间关闭超过 30 分钟</div>
                    </div>
                  </div>

                  <div class="device-meta-line">
                    <el-tag type="success" effect="plain">新固件</el-tag>
                    <span>{{ selectedRobot.meta }}</span>
                  </div>

                  <div class="benefit-row">
                    <div class="inline-info-row">
                      <el-tag type="success" effect="plain">{{ benefitLabel }}</el-tag>
                      <span>{{ benefitValue }}</span>
                    </div>
                    <div class="redeem-box">
                      <el-input
                        v-model.trim="redeemCode"
                        class="redeem-input"
                        placeholder="请输入激活码"
                        @keyup.enter.native="handleRedeem"
                      ></el-input>
                      <el-button
                        type="primary"
                        class="redeem-btn"
                        :loading="redeemLoading"
                        @click="handleRedeem"
                      >
                        兑换
                      </el-button>
                    </div>
                  </div>

                  <div class="plan-row">
                    <div class="plan-select-wrap">
                      <span class="plan-select-label">导播方案:</span>
                      <el-select
                        v-model="currentSelectedPlanNo"
                        class="plan-select"
                        placeholder="请选择导播方案"
                        filterable
                      >
                        <el-option
                          v-for="plan in livePlanOptions"
                          :key="plan.planNo"
                          :label="`${plan.planName}(${plan.roomId})`"
                          :value="plan.planNo"
                        ></el-option>
                      </el-select>
                    </div>
                    <el-button
                      type="primary"
                      class="start-btn"
                      :loading="startLiveLoading"
                      @click="startLive"
                    >
                      {{ isLiveRunning ? "停止导播" : "启动导播" }}
                    </el-button>
                  </div>

                  <div v-if="liveStartedAtText || liveRuntimeText" class="live-info-row">
                    <span v-if="liveStartedAtText">开始时间: {{ liveStartedAtText }}</span>
                    <span v-if="liveRuntimeText">已运行时长: {{ liveRuntimeText }}</span>
                    <!-- <span>直播间状态: {{ roomStatusText }}</span> -->
                  </div>

                  <div v-if="abnormalStatusList.length" class="abnormal-row">
                    <div
                      v-for="item in abnormalStatusList"
                      :key="item.key"
                      class="abnormal-item"
                    >
                      {{ item.label }}，已持续{{ item.durationText }}
                    </div>
                  </div>
                </div>
              </el-card>

              <el-card class="message-panel-card" shadow="never">
                <div class="message-panel-header">
                  <div class="panel-title panel-title--tight">消息面板</div>
                  <div class="message-toolbar">
                    <el-input
                      v-model.trim="pendingMessage"
                      class="message-input"
                      placeholder="请输入消息，指挥机器人..."
                      @keyup.enter.native="handleSendMessage"
                    ></el-input>
                    <el-button type="primary" class="send-btn" @click="handleSendMessage">
                      发送消息
                    </el-button>
                    <div class="online-indicator">
                      <span class="online-label">在线人数:</span>
                      <span>{{ currentViewerCountText }}</span>
                    </div>
                    <i class="el-icon-circle-check success-icon"></i>
                  </div>
                </div>

                <div ref="consolePanel" class="console-panel">
                  <div
                    v-for="message in consoleMessages"
                    :key="message.id"
                    class="console-item"
                  >
                    <div class="console-item__text">{{ message.text }}</div>
                    <div class="console-item__time">{{ message.time }}</div>
                  </div>
                </div>
              </el-card>
            </template>

            <div v-else class="main-empty">
              <i class="el-icon-monitor empty-icon"></i>
              <div class="empty-title">请先选择机器人</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import Api from "@/apis/api";
import HeaderBar from "@/components/HeaderBar.vue";

export default {
  name: "DirectorDesk",
  components: {
    HeaderBar,
  },
  data() {
    return {
      robots: [],
      selectedRobotId: "",
      editingRobotName: false,
      robotNameSnapshot: "",
      livePlanOptions: [],
      selectedPlanNoMap: {},
      pendingMessage: "",
      consoleMessages: [],
      consoleMessageMap: {},
      sentMsgAfterIdMap: {},
      mqttServiceAvailable: false,
      benefitLabel: "有效期",
      benefitValue: "-",
      canStartLiveByBenefits: false,
      benefitMembershipActive: false,
      benefitMembershipEndAt: "",
      redeemCode: "",
      redeemLoading: false,
      startLiveLoading: false,
      sentMsgTimer: null,
      sentMsgAfterId: 0,
      sentMsgLimit: 20,
      activeMonitorDeviceId: "",
      liveStatusTimer: null,
      isLiveRunning: false,
      liveStartedAtText: "",
      liveRuntimeText: "",
      roomStatusText: "-",
      currentViewerCountText: "-",
      roomStatusRaw: "",
      deviceOnline: null,
      deviceOfflineDurationSeconds: 0,
      roomEndedDurationSeconds: 0,
    };
  },
  computed: {
    selectedRobot() {
      return this.robots.find((item) => item.id === this.selectedRobotId) || null;
    },
    currentSelectedPlanNo: {
      get() {
        if (!this.selectedRobotId) {
          return "";
        }
        return this.selectedPlanNoMap[this.selectedRobotId] || "";
      },
      set(value) {
        if (!this.selectedRobotId) {
          return;
        }
        this.$set(this.selectedPlanNoMap, this.selectedRobotId, value || "");
      },
    },
    abnormalStatusList() {
      const list = [];
      if (this.roomStatusRaw === "ended") {
        list.push({
          key: "roomClosed",
          label: "直播间关闭",
          durationText: this.formatRuntimeSeconds(this.roomEndedDurationSeconds),
        });
      }
      if (this.deviceOnline === false) {
        list.push({
          key: "robotOffline",
          label: "机器人不在线",
          durationText: this.formatRuntimeSeconds(this.deviceOfflineDurationSeconds),
        });
      }
      return list;
    },
  },
  created() {
    this.fetchLivePlanList();
    this.fetchUserBenefits();
    this.fetchAgentList();
  },
  mounted() {
    this.scrollConsoleToBottom();
  },
  beforeDestroy() {
    this.stopSentMsgPolling();
    this.stopLiveStatusPolling();
  },
  methods: {
    fetchLivePlanList() {
      Api.livePlan.getLivePlanList({ page: 1, limit: 100 }, ({ data }) => {
        if (data && data.code === 0) {
          this.livePlanOptions = ((data.data || {}).list || []).map((item) => ({
            planNo: item.planNo,
            planName: item.planName,
            roomId: item.roomId,
          }));
          this.ensureRobotPlanSelections();
          return;
        }

        this.$message.error((data && data.msg) || "获取导播方案失败");
      });
    },
    fetchUserBenefits() {
      Api.activationCode.getMyBenefits(({ data }) => {
        if (data && data.code === 0) {
          const benefit = data.data || {};
          this.benefitMembershipActive = benefit.membershipActive === true;
          this.benefitMembershipEndAt = benefit.membershipEndAt || "";

          if (benefit.membershipActive === true && benefit.membershipEndAt) {
            this.benefitLabel = "有效期";
            this.benefitValue = this.formatAbsoluteTime(benefit.membershipEndAt);
            this.canStartLiveByBenefits = true;
            return;
          }

          const balanceSeconds = Number(benefit.balanceSeconds) || 0;
          this.benefitLabel = "剩余可用时长";
          this.benefitValue = this.formatDurationSeconds(balanceSeconds);
          this.canStartLiveByBenefits = balanceSeconds > 0;
          return;
        }

        this.benefitMembershipActive = false;
        this.benefitMembershipEndAt = "";
        this.benefitLabel = "有效期";
        this.benefitValue = "-";
        this.canStartLiveByBenefits = false;
      });
    },
    handleRedeem() {
      if (!this.redeemCode) {
        this.$message.warning("请输入激活码");
        return;
      }

      if (this.redeemLoading) {
        return;
      }

      this.redeemLoading = true;
      Api.activationCode.redeem(this.redeemCode, ({ data }) => {
        this.redeemLoading = false;
        if (data && data.code === 0) {
          this.$message.success("兑换成功");
          this.redeemCode = "";
          this.fetchUserBenefits();
          return;
        }

        this.$message.error((data && data.msg) || "兑换失败");
      });
    },
    fetchAgentList() {
      Api.agent.getAgentList(({ data }) => {
        if (data?.data) {
          const agents = data.data.map((item) => ({
            ...item,
            agentId: item.id,
          }));

          if (!agents.length) {
            this.robots = [];
            this.selectedRobotId = "";
            return;
          }

          let pendingCount = agents.length;
          const robotBuckets = [];

          agents.forEach((agent) => {
            this.fetchBindDevices(agent.agentId, agent, (robots) => {
              robotBuckets.push(...robots);
              pendingCount -= 1;

              if (pendingCount === 0) {
                this.robots = robotBuckets.sort((a, b) => a.rawBindTime - b.rawBindTime);
                if (!this.robots.length) {
                  this.selectedRobotId = "";
                  this.resetLiveStatusDisplay();
                  return;
                }
                const stillExists = this.robots.some((item) => item.id === this.selectedRobotId);
                this.selectedRobotId = stillExists ? this.selectedRobotId : this.robots[0].id;
                this.ensureRobotPlanSelections();
                this.fetchLiveStatus();
              }
            });
          });
        }
      }, (error) => {
        console.error('Failed to fetch agent list:', error);
      });
    },
    fetchBindDevices(agentId, agent, done) {
      Api.device.getAgentBindDevices(agentId, ({ data }) => {
        if (data.code === 0) {
          const deviceList = data.data
            .map((device) => ({
              id: device.id,
              agentId,
              mac: device.macAddress,
              model: device.board,
              agentName: agent.agentName,
              name: `${device.board || "Unknown"} - ${agent.agentName || "未命名智能体"}`,
              meta: `${device.macAddress || "-"} | ${device.board || "-"} | ${device.appVersion || "-"}`,
              validUntil: device.activeTime || device.expireTime || "-",
              updatedAt: this.formatDisplayTime(device.lastConnectedAt || device.createDate),
              online: false,
              deviceStatus: 'offline',
              rawBindTime: new Date(device.createDate || 0).getTime() || 0,
            }));

          this.fetchDeviceStatus(agentId, deviceList, done);
          return;
        }

        done([]);
      });
    },
    fetchDeviceStatus(agentId, robots, done) {
      Api.device.getDeviceStatus(agentId, ({ data }) => {
        if (data.code === 0) {
          try {
            const statusData = JSON.parse(data.data);
            if (statusData && typeof statusData === 'object') {
              this.mqttServiceAvailable = true;
              this.updateDeviceStatusFromResponse(robots, statusData);
              done(robots);
              return;
            }
          } catch (error) {
            this.mqttServiceAvailable = false;
          }
        } else {
          this.mqttServiceAvailable = false;
        }

        done(robots);
      });
    },
    updateDeviceStatusFromResponse(robots, deviceStatusMap) {
      robots.forEach((robot) => {
        const macAddress = robot.mac ? robot.mac.replace(/:/g, '_') : 'unknown';
        const groupId = robot.model ? robot.model.replace(/:/g, '_') : 'GID_default';
        const mqttClientId = `${groupId}@@@${macAddress}@@@${macAddress}`;

        if (deviceStatusMap[mqttClientId]) {
          const statusInfo = deviceStatusMap[mqttClientId];
          let isOnline = false;
          if (statusInfo.isAlive === true) {
            isOnline = true;
          } else if (statusInfo.isAlive === false) {
            isOnline = false;
          } else if (statusInfo.isAlive === null && statusInfo.exists === true) {
            isOnline = true;
          }

          robot.deviceStatus = isOnline ? 'online' : 'offline';
          robot.online = isOnline;
          return;
        }

        robot.deviceStatus = 'offline';
        robot.online = false;
      });
    },
    selectRobot(id) {
      this.selectedRobotId = id;
      this.ensureRobotPlanSelection(id);
      const nextRobot = this.robots.find((item) => item.id === id);
      this.stopSentMsgTracking();
      this.setConsoleMessagesForDevice(nextRobot ? nextRobot.mac : "");
      this.fetchLiveStatus();
    },
    ensureRobotPlanSelection(robotId) {
      if (!robotId || !this.livePlanOptions.length) {
        return;
      }
      if (!this.selectedPlanNoMap[robotId]) {
        this.$set(this.selectedPlanNoMap, robotId, this.livePlanOptions[0].planNo);
      }
    },
    ensureRobotPlanSelections() {
      this.robots.forEach((robot) => {
        this.ensureRobotPlanSelection(robot.id);
      });
    },
    setRobotPlanSelectionByPlanNo(robotId, planNo) {
      if (!robotId || !planNo) {
        return;
      }
      const matchedPlan = this.livePlanOptions.find(
        item => String(item.planNo || "") === String(planNo || "")
      );
      if (matchedPlan) {
        this.$set(this.selectedPlanNoMap, robotId, matchedPlan.planNo);
      }
    },
    formatDisplayTime(value) {
      if (!value) {
        return "刚刚";
      }

      const date = new Date(value);
      if (Number.isNaN(date.getTime())) {
        return String(value);
      }

       const diffMs = Date.now() - date.getTime();
       if (diffMs < 60000) {
         return "刚刚";
       }

       const diffMinutes = Math.floor(diffMs / 60000);
       if (diffMinutes < 60) {
         return `${diffMinutes}分钟前`;
       }

       const diffHours = Math.floor(diffMinutes / 60);
       if (diffHours < 24) {
         return `${diffHours}小时前`;
       }

       const diffDays = Math.floor(diffHours / 24);
       if (diffDays <= 7) {
         return `${diffDays}天前`;
       }

      const year = date.getFullYear();
      const month = `${date.getMonth() + 1}`.padStart(2, "0");
      const day = `${date.getDate()}`.padStart(2, "0");
      const hour = `${date.getHours()}`.padStart(2, "0");
      const minute = `${date.getMinutes()}`.padStart(2, "0");
      const second = `${date.getSeconds()}`.padStart(2, "0");
      return `${year}-${month}-${day} ${hour}:${minute}:${second}`;
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
    formatDurationSeconds(value) {
      const totalSeconds = Number(value);
      if (!Number.isFinite(totalSeconds) || totalSeconds <= 0) {
        return "0分";
      }

      const hours = Math.floor(totalSeconds / 3600);
      const minutes = Math.floor(totalSeconds / 60);
      if (hours > 0) {
        return `${hours}小时${minutes % 60}分`;
      }
      return `${minutes}分`;
    },
    startRobotNameEdit() {
      if (!this.selectedRobot) {
        return;
      }
      this.robotNameSnapshot = this.selectedRobot.name;
      this.editingRobotName = true;
      this.$nextTick(() => {
        const inputRef = this.$refs.robotNameInput;
        if (inputRef && inputRef.focus) {
          inputRef.focus();
        }
      });
    },
    finishRobotNameEdit() {
      this.editingRobotName = false;
      this.robotNameSnapshot = "";
    },
    cancelRobotNameEdit() {
      if (this.selectedRobot) {
        this.selectedRobot.name = this.robotNameSnapshot;
      }
      this.editingRobotName = false;
      this.robotNameSnapshot = "";
    },
    buildTimeText() {
      const now = new Date();
      const hour = `${now.getHours()}`.padStart(2, "0");
      const minute = `${now.getMinutes()}`.padStart(2, "0");
      const second = `${now.getSeconds()}`.padStart(2, "0");
      return `${hour}:${minute}:${second}`;
    },
    formatMessageTime(value) {
      if (!value) {
        return this.buildTimeText();
      }
      const date = new Date(value);
      if (Number.isNaN(date.getTime())) {
        return this.buildTimeText();
      }
      const hour = `${date.getHours()}`.padStart(2, "0");
      const minute = `${date.getMinutes()}`.padStart(2, "0");
      const second = `${date.getSeconds()}`.padStart(2, "0");
      return `${hour}:${minute}:${second}`;
    },
    addConsoleMessage(text) {
      const deviceId = this.selectedRobot ? this.selectedRobot.mac : "";
      this.consoleMessages.push({
        id: `${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
        text,
        time: this.buildTimeText(),
      });
      if (deviceId) {
        this.consoleMessageMap[deviceId] = this.consoleMessages.slice();
      }
      this.$nextTick(() => {
        this.scrollConsoleToBottom();
      });
    },
    setConsoleMessagesForDevice(deviceId) {
      const nextMessages = deviceId && this.consoleMessageMap[deviceId]
        ? this.consoleMessageMap[deviceId].slice()
        : [];
      this.consoleMessages = nextMessages;
      this.$nextTick(() => {
        this.scrollConsoleToBottom();
      });
    },
    appendSentMessages(messages, deviceId) {
      const currentDeviceId = deviceId || this.activeMonitorDeviceId;
      if (!currentDeviceId) {
        return;
      }

      const deviceMessages = this.consoleMessageMap[currentDeviceId]
        ? this.consoleMessageMap[currentDeviceId].slice()
        : [];

      messages.forEach((item) => {
        deviceMessages.push({
          id: `sent-${item.id}`,
          text: item.prompt || "",
          time: this.formatMessageTime(item.created_at),
        });
        this.sentMsgAfterIdMap[currentDeviceId] = Math.max(
          Number(this.sentMsgAfterIdMap[currentDeviceId]) || 0,
          Number(item.id) || 0
        );
      });

      this.consoleMessageMap[currentDeviceId] = deviceMessages;
      if (this.selectedRobot && this.selectedRobot.mac === currentDeviceId) {
        this.consoleMessages = deviceMessages.slice();
      }
      this.sentMsgAfterId = Number(this.sentMsgAfterIdMap[currentDeviceId]) || 0;
      this.$nextTick(() => {
        this.scrollConsoleToBottom();
      });
    },
    stopSentMsgPolling() {
      if (this.sentMsgTimer) {
        clearTimeout(this.sentMsgTimer);
        this.sentMsgTimer = null;
      }
    },
    scheduleSentMsgPolling() {
      this.stopSentMsgPolling();
      this.sentMsgTimer = setTimeout(() => {
        this.pollSentMessages();
      }, 3000);
    },
    pollSentMessages() {
      const deviceId = this.activeMonitorDeviceId;
      if (!deviceId) {
        return;
      }

      Api.liveStreaming.getSentMsg(
        deviceId,
        {
          afterId: this.sentMsgAfterId,
          limit: this.sentMsgLimit,
        },
        ({ data }) => {
          if (this.activeMonitorDeviceId !== deviceId) {
            return;
          }

          if (data && data.code === 0) {
            const messages = Array.isArray(data.data) ? data.data : [];
            if (messages.length) {
              this.appendSentMessages(messages, deviceId);
            }
            this.scheduleSentMsgPolling();
            return;
          }

          this.scheduleSentMsgPolling();
        }
      );
    },
    startSentMsgPolling(deviceId) {
      this.stopSentMsgPolling();
      this.activeMonitorDeviceId = deviceId;
      this.sentMsgAfterId = Number(this.sentMsgAfterIdMap[deviceId]) || 0;
      this.setConsoleMessagesForDevice(deviceId);
      this.pollSentMessages();
    },
    stopSentMsgTracking() {
      this.stopSentMsgPolling();
      this.activeMonitorDeviceId = "";
      this.sentMsgAfterId = 0;
    },
    stopLiveStatusPolling() {
      if (this.liveStatusTimer) {
        clearTimeout(this.liveStatusTimer);
        this.liveStatusTimer = null;
      }
    },
    scheduleLiveStatusPolling() {
      this.stopLiveStatusPolling();
      this.liveStatusTimer = setTimeout(() => {
        this.fetchLiveStatus();
      }, 5000);
    },
    resetLiveStatusDisplay() {
      this.isLiveRunning = false;
      this.liveStartedAtText = "";
      this.liveRuntimeText = "";
      this.roomStatusText = "-";
      this.roomStatusRaw = "";
      this.currentViewerCountText = "-";
      this.deviceOnline = null;
      this.deviceOfflineDurationSeconds = 0;
      this.roomEndedDurationSeconds = 0;
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
    fetchLiveStatus() {
      if (!this.selectedRobot) {
        this.stopSentMsgTracking();
        this.consoleMessages = [];
        this.resetLiveStatusDisplay();
        return;
      }

      const deviceId = this.selectedRobot.mac;
      Api.liveStreaming.getLiveStatus(deviceId, ({ data }) => {
        if (!this.selectedRobot || this.selectedRobot.mac !== deviceId) {
          return;
        }

        if (data && data.code === 0 && data.data) {
          const liveData = data.data || {};
          const target = this.robots.find((item) => item.id === this.selectedRobotId);
          const targetRobotId = target ? target.id : this.selectedRobotId;

          this.isLiveRunning = liveData.status === "running";
          this.liveStartedAtText = liveData.started_at
            ? this.formatAbsoluteTime(liveData.started_at)
            : "";
          this.liveRuntimeText = this.isLiveRunning
            ? this.formatRuntimeSeconds(liveData.runtime_seconds)
            : "";
          this.roomStatusRaw = liveData.room_status || "";
          this.roomStatusText =
            liveData.room_status === "running"
              ? "直播中"
              : liveData.room_status === "ended"
                ? "直播间关闭"
                : "-";
          this.currentViewerCountText =
            liveData.current_viewer_count !== undefined && liveData.current_viewer_count !== null
              ? String(liveData.current_viewer_count)
              : "-";
          if (!this.benefitMembershipActive && liveData.benefit_balance_seconds !== undefined) {
            const balanceSeconds = Number(liveData.benefit_balance_seconds) || 0;
            this.benefitLabel = "剩余可用时长";
            this.benefitValue = this.formatDurationSeconds(balanceSeconds);
            this.canStartLiveByBenefits = balanceSeconds > 0;
          } else if (this.benefitMembershipActive && this.benefitMembershipEndAt) {
            this.benefitLabel = "有效期";
            this.benefitValue = this.formatAbsoluteTime(this.benefitMembershipEndAt);
            this.canStartLiveByBenefits = true;
          }
          this.deviceOnline =
            typeof liveData.device_online === "boolean" ? liveData.device_online : null;
          this.deviceOfflineDurationSeconds =
            Number(liveData.device_offline_duration_seconds) || 0;
          this.roomEndedDurationSeconds =
            Number(liveData.room_ended_duration_seconds) || 0;

          if (target && this.deviceOnline !== null) {
            target.online = this.deviceOnline;
            target.deviceStatus = this.deviceOnline ? "online" : "offline";
          }

          if (this.isLiveRunning) {
            this.setRobotPlanSelectionByPlanNo(targetRobotId, liveData.plan_no);
          }

          if (this.isLiveRunning) {
            if (this.activeMonitorDeviceId !== deviceId) {
              this.startSentMsgPolling(deviceId);
            } else if (!this.sentMsgTimer) {
              this.pollSentMessages();
            }
            this.scheduleLiveStatusPolling();
            return;
          }

          if (this.activeMonitorDeviceId === deviceId) {
            this.stopSentMsgTracking();
          }
          this.setConsoleMessagesForDevice(deviceId);
          this.stopLiveStatusPolling();
          return;
        }

        if (this.activeMonitorDeviceId === deviceId) {
          this.stopSentMsgTracking();
        }
        this.setConsoleMessagesForDevice(deviceId);
        this.resetLiveStatusDisplay();
        this.stopLiveStatusPolling();
      });
    },
    handleSendMessage() {
      if (!this.pendingMessage) {
        return;
      }
      // this.addConsoleMessage(`🧭 导播指令: ${this.pendingMessage}`);
      Api.liveStreaming.sendManualMsg(this.selectedRobot.mac, { message: this.pendingMessage }, ({ data }) => {
        if (data && data.code === 0) {
          this.$message.success("消息已发送");
          return;
        }
        this.$message.error((data && data.msg) || "消息发送失败");
      });
      this.pendingMessage = "";
    },
    scrollConsoleToBottom() {
      const panel = this.$refs.consolePanel;
      if (!panel) {
        return;
      }
      panel.scrollTop = panel.scrollHeight;
    },
    startLive() {
        if (!this.selectedRobot) {
          this.$message.warning("请先选择机器人");
          return;
        }

        const targetRobot = {
          id: this.selectedRobot.id,
          mac: this.selectedRobot.mac,
          online: this.selectedRobot.online,
        };
        const targetPlanNo = this.currentSelectedPlanNo;
        const targetIsRunning = this.isLiveRunning;

        if (!targetRobot.online && !targetIsRunning) {
          this.$message.warning("机器人不在线");
          return;
        }

        if (!targetPlanNo) {
          this.$message.warning("请选择导播方案");
          return;
        }

        if (!targetIsRunning && !this.canStartLiveByBenefits) {
          this.$message.warning("当前账户没有可用权益，无法启动导播");
          return;
        }

        if (this.startLiveLoading) {
          return;
        }

        if (targetIsRunning) {
          this.startLiveLoading = true;
          Api.liveStreaming.stopLive(targetRobot.mac, ({ data }) => {
            this.startLiveLoading = false;
            if (data && data.code === 0) {
              this.$message.success("导播已停止");
              if (this.selectedRobot && this.selectedRobot.mac === targetRobot.mac) {
                this.stopSentMsgTracking();
                this.stopLiveStatusPolling();
                this.resetLiveStatusDisplay();
                this.setConsoleMessagesForDevice(targetRobot.mac);
              }
              return;
            }
            this.$message.error((data && data.msg) || "停止导播失败");
          });
          return;
        }

        this.startLiveLoading = true;

        const mcpExecuteString = {
          type: "mcp",
          payload: {
            jsonrpc: "2.0",
            id: 1,
            method: "tools/call",
            params: {
              name: "self.enter_chat_state",
              arguments: {},
            },
          },
        };
        Api.device.sendDeviceCommand(this.selectedRobotId, mcpExecuteString, ({ data }) => {
            if (data.code === 0) {
              console.log("成功发送进入对话状态命令到设备");
            } else {
              console.error("机器人进入对话状态失败:", data.msg);
            }

            setTimeout(() => {
              Api.livePlan.getLivePlanDetail(targetPlanNo, ({ data }) => {
                if (data && data.code === 0) {
                  const plan = data.data || {};
                  const params = {
                    plan_no: targetPlanNo,
                    platform: plan.platform,
                    live_id: plan.roomId,
                    device_id: targetRobot.mac,
                    config_json: plan.configJson,
                  };
                  Api.liveStreaming.startLive(params, ({ data }) => {
                    this.startLiveLoading = false;
                    if (data && data.code === 0) {
                      this.$message.success("导播已启动");
                      this.$set(this.selectedPlanNoMap, targetRobot.id, targetPlanNo);
                      if (this.selectedRobot && this.selectedRobot.mac === targetRobot.mac) {
                        this.startSentMsgPolling(targetRobot.mac);
                        this.fetchLiveStatus();
                      }
                    } else {
                      this.$message.error((data && data.msg) || "启动导播失败");
                    }
                  });
                } else {
                  this.startLiveLoading = false;
                  this.$message.error((data && data.msg) || "获取导播方案详情失败");
                }
              });
            }, 2000);
        });
    }
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

.desk-layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.robot-sidebar {
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

.panel-title--tight {
  margin-bottom: 0;
}

.robot-list {
  max-height: calc(100vh - 260px);
  overflow-y: auto;
  padding-right: 4px;
}

.robot-item {
  padding: 14px 16px;
  border: 1px solid transparent;
  border-radius: 16px;
  background: #ffffff;
  cursor: pointer;
  transition: all 0.2s ease;
}

.robot-item + .robot-item {
  margin-top: 14px;
}

.robot-item:hover,
.robot-item.active {
  background: #f3f6fb;
  border-color: #dfe8f5;
}

.robot-item__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.robot-item__id {
  font-size: 16px;
  font-weight: 700;
  color: #1f2937;
}

.robot-item__name {
  font-size: 14px;
  line-height: 1.6;
  min-height: 34px;
  margin-bottom: 10px;
  color: #667085;
  text-align: left;
}

.robot-item__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #98a2b3;
  font-size: 13px;
}

.desk-main {
  flex: 1;
  min-width: 0;
}

.director-summary-card,
.message-panel-card {
  border-radius: 20px;
  border: 1px solid #e8edf5;
}

.director-summary-card {
  margin-bottom: 20px;
}

.director-summary {
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: flex-start;
  position: relative;
}

.director-summary__top {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  flex-wrap: wrap;
  width: 100%;
}

.summary-top-main {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.summary-name-text {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
}

.auto-close-notice {
  position: absolute;
  top: 0;
  right: 0;
  width: 360px;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid #d8e4f4;
  background: linear-gradient(135deg, #f8fbff 0%, #eef5ff 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.auto-close-notice__title {
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 700;
  color: #26436a;
}

.auto-close-notice__item {
  position: relative;
  padding-left: 14px;
  font-size: 13px;
  line-height: 1.7;
  color: #5f6f86;
}

.auto-close-notice__item::before {
  content: "";
  position: absolute;
  left: 0;
  top: 8px;
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: #6d9ce8;
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

.name-editor {
  display: flex;
  align-items: center;
  gap: 8px;
}

.summary-edit /deep/ .el-input__inner {
  height: 40px;
  border-radius: 10px;
  border-color: #d0d5dd;
  font-size: 18px;
  font-weight: 700;
}

.summary-name-input {
  width: 260px;
}

.summary-action-btn {
  width: 40px;
  height: 40px;
  padding: 0;
  border-radius: 10px;
}

.device-meta-line,
.inline-info-row {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #667085;
  font-size: 14px;
  font-weight: 600;
}

.benefit-row {
  display: flex;
  align-items: center;
  /* justify-content: space-between; */
  gap: 16px;
  width: 100%;
  flex-wrap: wrap;
}

.redeem-box {
  display: flex;
  align-items: center;
  gap: 12px;
}

.redeem-input {
  width: 240px;
}

.redeem-input /deep/ .el-input__inner {
  height: 40px;
  border-radius: 10px;
}

.redeem-btn {
  height: 40px;
  padding: 0 16px;
  border-radius: 10px;
}

.plan-row {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.live-info-row {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
  color: #667085;
  font-size: 14px;
  font-weight: 600;
}

.abnormal-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.abnormal-item {
  color: #d9485f;
  font-size: 14px;
  font-weight: 600;
}

.plan-select-wrap {
  display: flex;
  align-items: center;
  width: 520px;
  min-height: 44px;
  border: 1px solid #d0d5dd;
  border-radius: 10px;
  background: #fff;
  overflow: hidden;
}

.plan-select-label {
  flex: none;
  padding: 0 16px;
  color: #1f2937;
  font-size: 14px;
  font-weight: 600;
  border-right: 1px solid #d0d5dd;
}

.plan-select {
  flex: 1;
}

.plan-select /deep/ .el-input__inner {
  border: none;
  height: 42px;
}

.plan-select /deep/ .el-input__suffix {
  right: 12px;
}

.start-btn {
  height: 44px;
  padding: 0 18px;
  border-radius: 10px;
}

.message-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.message-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
  justify-content: flex-end;
}

.message-input {
  width: 320px;
}

.message-input /deep/ .el-input__inner {
  height: 44px;
  border-radius: 10px;
}

.send-btn {
  height: 44px;
  padding: 0 18px;
  border-radius: 10px;
}

.online-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #667085;
  font-size: 14px;
  font-weight: 600;
}

.online-label {
  color: #344054;
}

.success-icon {
  color: #2f9f59;
  font-size: 22px;
}

.console-panel {
  max-height: calc(100vh - 330px);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-right: 4px;
}

.console-item {
  padding: 16px 18px;
  border: 1px solid #dfe6f1;
  border-radius: 10px;
  background: #fafcff;
  text-align: left;
}

.console-item__text {
  color: #344054;
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
  text-align: left;
}

.console-item__time {
  margin-top: 12px;
  color: #98a2b3;
  font-size: 13px;
  text-align: left;
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
  font-size: 18px;
  margin-bottom: 14px;
}

@media (max-width: 1280px) {
  .desk-layout {
    flex-direction: column;
  }

  .robot-sidebar {
    width: 100%;
  }

  .robot-list,
  .console-panel {
    max-height: none;
  }

  .message-panel-header,
  .message-toolbar,
  .plan-row {
    align-items: stretch;
    flex-direction: column;
  }

  .message-toolbar {
    justify-content: flex-start;
  }

  .message-input,
  .plan-select-wrap {
    width: 100%;
  }

  .auto-close-notice {
    position: static;
    width: 100%;
  }
}
</style>
