<route lang="jsonc" type="page">
{
  "layout": "default",
  "style": {
    "navigationStyle": "custom",
    "navigationBarTitleText": "用户协议"
  }
}
</route>

<script lang="ts" setup>
import { t } from '@/i18n'

function goBack() {
  uni.navigateBack()
}
</script>

<template>
  <view class="h-screen flex flex-col bg-[#f5f8fd]">
    <wd-navbar
      :title="t('login.userAgreement')"
      safe-area-inset-top
      left-arrow
      :bordered="false"
      @click-left="goBack"
    >
      <template #left>
        <wd-icon name="arrow-left" size="18" />
      </template>
    </wd-navbar>
    <scroll-view scroll-y class="box-border flex-1 px-[30rpx] py-[30rpx]">
      <view class="rounded-[16rpx] bg-white px-[30rpx] py-[40rpx] shadow-[0_4rpx_20rpx_rgba(0,0,0,0.05)]">
        <view class="mb-[40rpx] text-center">
          <text class="text-[36rpx] text-[#1a1a1a] font-bold">
            {{ t('login.userAgreement') }}
          </text>
        </view>
        <view class="mb-[40rpx] text-center">
          <text class="text-[24rpx] text-[#666666]">
            更新日期：2026年5月8日
          </text>
        </view>
        <view class="mb-[32rpx]">
          <text class="text-[28rpx] text-[#333333] font-semibold">
            提示条款
          </text>
        </view>
        <view class="mb-[24rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            欢迎您使用小助物联助播语音助手（以下简称"本服务"）。本服务的运营方为本服务的实际部署者和管理者（以下简称"运营者"或"我们"）。
          </text>
        </view>
        <view class="mb-[24rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            在您注册、登录或使用本服务之前，请您务必审慎阅读、充分理解本协议各条款内容，特别是以加粗形式提示您注意的可能与您利益有重大关系的条款，包括但不限于免责声明、责任限制等条款。
          </text>
        </view>
        <view class="mb-[24rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            当您按照页面提示完成注册、登录或以其他方式使用本服务时，即表示您已充分阅读、理解并接受本协议的全部内容。如果您不同意本协议或其中任何条款，请立即停止使用本服务。
          </text>
        </view>
        <view class="mb-[40rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            本协议可能根据实际情况进行更新和修订，修订后的协议将通过本平台公告等方式予以公示。修订后的协议自公示之日起生效，如您在协议修订后继续使用本服务，则视为您已接受修订后的协议。
          </text>
        </view>
        <view class="mb-[32rpx]">
          <text class="text-[28rpx] text-[#333333] font-semibold">
            一、账号注册与使用
          </text>
        </view>
        <view class="mb-[24rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            1.1 用户资格：您确认，在使用本服务前，您应当具备中华人民共和国法律规定的与您行为相适应的民事行为能力。若您为未成年人，应在法定监护人的陪同和指导下使用本服务。
          </text>
        </view>
        <view class="mb-[24rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            1.2 账号注册：您可通过用户名密码或直接使用微信等方式注册和登录本服务。您应当提供真实、准确、完整的注册信息，并及时更新。
          </text>
        </view>
        <view class="mb-[24rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            1.3 账号安全：您应妥善保管您的账号和密码，因您的原因导致账号泄露或被他人冒用所产生的一切后果，由您自行承担。如发现账号存在安全隐患，请立即联系运营者。
          </text>
        </view>
        <view class="mb-[24rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            1.4 账号使用限制：您的账号仅限您本人使用，未经运营者同意，不得以任何方式将账号转让、出租或借给第三方使用。
          </text>
        </view>
        <view class="mb-[40rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            1.5 账号注销：如您需要注销账号，可通过本服务的账号设置功能进行操作，或联系运营者进行处理。账号注销后，相关数据将被删除且无法恢复，请谨慎操作。
          </text>
        </view>

        <view class="mb-[32rpx]">
          <text class="text-[28rpx] text-[#333333] font-semibold">
            二、服务内容
          </text>
        </view>
        <view class="mb-[24rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            2.1 服务概述：本服务依托人工智能技术，通过API接口调用第三方生成式人工智能模型，为连接的硬件设备提供智能交互服务，包括但不限于语音活动检测（VAD）、语音识别（ASR）、语义理解（LLM）、语音合成（TTS）、视觉理解（VLLM）、意图识别（Intent）、对话记忆（Memory）等。
          </text>
        </view>
        <view class="mb-[24rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            2.2 智能体管理：您可通过本服务创建和配置智能体，包括设定角色模板、选择语言模型、配置语音参数、设置意图识别规则、管理知识库、开启对话记忆、配置智能体插件等。智能体的设定内容不得违反国家法律法规。
          </text>
        </view>
        <view class="mb-[24rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            2.3 设备管理：您可通过本服务绑定和管理您的硬件设备，进行设备配置、固件更新、OTA远程升级等操作。您应确保所管理的设备为您合法拥有或已获得授权。
          </text>
        </view>
        <view class="mb-[24rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            2.4 视觉理解：本服务支持通过设备摄像头采集图像，调用第三方视觉模型进行图像识别和场景理解。您应确保采集图像的行为符合相关法律法规，不得侵犯他人隐私。
          </text>
        </view>
        <view class="mb-[24rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            2.5 意图识别：本服务可通过意图识别功能理解用户指令的意图，并据此触发相应的操作或服务，如控制智能设备、查询信息等。
          </text>
        </view>
        <view class="mb-[24rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            2.6 对话记忆：本服务可记录和存储您与智能体的交互历史摘要，以便在后续对话中提供更连贯、个性化的交互体验。您可通过本服务管理或清除记忆数据。
          </text>
        </view>
        <view class="mb-[24rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            2.7 知识库：本服务支持您上传文档、文本等内容构建知识库，智能体可在对话中检索知识库内容为您提供问答服务。您应确保上传的知识库内容不侵犯第三方知识产权，且不包含违反法律法规的内容。
          </text>
        </view>
        <view class="mb-[24rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            2.8 声纹识别：本服务支持声纹识别功能，您可注册声纹样本以实现说话人身份验证和个性化服务。声纹数据将用于身份验证目的。
          </text>
        </view>
        <view class="mb-[24rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            2.9 语音克隆：本服务支持语音克隆功能，您可通过提供音频样本克隆自定义音色。克隆音色仅供您本人使用，不得用于仿冒他人或从事违法违规活动。
          </text>
        </view>
        <view class="mb-[24rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            2.10 MCP接入点：本服务支持MCP（Model Context Protocol）协议，可作为MCP Server向外部提供工具调用能力，也可作为MCP Client调用外部MCP服务，实现与外部系统的标准化集成。
          </text>
        </view>
        <view class="mb-[24rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            2.11 上下文提供者：本服务支持上下文提供者（Context Provider）功能，可从外部数据源获取实时信息，如天气、新闻、股票等，为智能体提供更丰富的知识来源。
          </text>
        </view>
        <view class="mb-[24rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            2.12 MQTT协议：本服务支持MQTT（Message Queuing Telemetry Transport）协议，用于与物联网设备之间的消息通信。您可通过MQTT协议实现设备控制指令下发、设备状态监控等功能。
          </text>
        </view>
        <view class="mb-[24rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            2.13 服务说明：本服务所依赖的第三方AI模型生成的对话、答复内容仅供参考，不构成任何专业建议。您不得将输出内容作为医疗、法律、金融等领域的专业建议。您根据输出内容所作的任何判断或决策，由您自行承担全部责任。
          </text>
        </view>
        <view class="mb-[24rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            2.14 服务费用：本服务的具体收费规则以页面展示或运营者说明为准；如服务涉及第三方平台、模型服务、通信服务等费用，该等费用由相关服务提供方按照其规则收取。
          </text>
        </view>

        <view class="mb-[32rpx]">
          <text class="text-[28rpx] text-[#333333] font-semibold">
            三、免责声明
          </text>
        </view>
        <view class="mb-[24rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            3.1 AI生成内容：本服务依赖第三方AI模型提供智能交互能力，AI生成的内容可能存在不准确、不完整或不恰当之处。运营者不对AI生成内容的真实性、准确性、完整性作任何保证。
          </text>
        </view>
        <view class="mb-[24rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            3.2 服务中断：因以下原因导致服务中断或异常，运营者不承担责任：
          </text>
        </view>
        <view class="mb-[16rpx] ml-[40rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            （1）不可抗力因素，如自然灾害、疫情、战争等。
          </text>
        </view>
        <view class="mb-[16rpx] ml-[40rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            （2）电力供应故障、通信网络故障等公共服务因素。
          </text>
        </view>
        <view class="mb-[16rpx] ml-[40rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            （3）第三方AI模型服务商的服务故障或政策调整。
          </text>
        </view>
        <view class="mb-[16rpx] ml-[40rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            （4）系统维护、升级导致的临时性服务中断。
          </text>
        </view>
        <view class="mb-[16rpx] ml-[40rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            （5）黑客攻击、计算机病毒等网络安全事件。
          </text>
        </view>
        <view class="mb-[16rpx] ml-[40rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            （6）法律法规或政府管制导致的服务调整。
          </text>
        </view>
        <view class="mb-[40rpx] ml-[40rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            （7）其他非运营者过错导致的情形。
          </text>
        </view>
        <view class="mb-[24rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            3.3 第三方服务：本服务可能涉及第三方提供的语言模型、语音识别、语音合成、视觉模型、意图识别等服务。您在使用时应同时遵守第三方的服务条款。因第三方服务引发的问题，请直接与第三方联系。
          </text>
        </view>

        <view class="mb-[32rpx]">
          <text class="text-[28rpx] text-[#333333] font-semibold">
            四、未成年人保护
          </text>
        </view>
        <view class="mb-[24rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            4.1 若您是未满18周岁的未成年人，应在监护人的指导和同意下使用本服务。
          </text>
        </view>
        <view class="mb-[24rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            4.2 监护人应加强对未成年人使用本服务的监督，引导未成年人合理使用，避免过度依赖。
          </text>
        </view>
        <view class="mb-[40rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            4.3 未成年人在使用本服务时应注意个人信息保护，避免上传或透露个人敏感信息。
          </text>
        </view>

        <view class="mb-[32rpx]">
          <text class="text-[28rpx] text-[#333333] font-semibold">
            五、协议终止
          </text>
        </view>
        <view class="mb-[24rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            5.1 用户终止：您有权随时停止使用本服务并注销账号。
          </text>
        </view>
        <view class="mb-[24rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            5.2 运营者终止：出现以下情形时，运营者有权终止向您提供服务：
          </text>
        </view>
        <view class="mb-[16rpx] ml-[40rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            （1）您违反本协议的约定。
          </text>
        </view>
        <view class="mb-[16rpx] ml-[40rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            （2）您利用本服务从事违法违规活动。
          </text>
        </view>
        <view class="mb-[16rpx] ml-[40rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            （3）根据法律法规或政策要求。
          </text>
        </view>
        <view class="mb-[40rpx] ml-[40rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            （4）运营者决定停止运营本服务。
          </text>
        </view>
        <view class="mb-[24rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            5.3 终止后处理：协议终止后，运营者无义务保留您的账号信息及相关数据。运营者仍有权依据本协议追究您在协议有效期内的违约责任。
          </text>
        </view>

        <view class="mb-[32rpx]">
          <text class="text-[28rpx] text-[#333333] font-semibold">
            六、法律适用与争议解决
          </text>
        </view>
        <view class="mb-[24rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            6.1 本协议的订立、生效、解释、修订、终止及争议解决均适用中华人民共和国法律。
          </text>
        </view>
        <view class="mb-[24rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            6.2 因本协议或本服务引发的争议，双方应友好协商解决；协商不成的，任何一方有权向运营者所在地有管辖权的人民法院提起诉讼。
          </text>
        </view>
        <view class="mb-[40rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            6.3 本协议的任何条款被认定为无效或不可执行，不影响其余条款的效力。
          </text>
        </view>

        <view class="mb-[32rpx]">
          <text class="text-[28rpx] text-[#333333] font-semibold">
            七、其他
          </text>
        </view>
        <view class="mb-[24rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            7.1 本协议构成您与运营者之间关于使用本服务的完整协议。
          </text>
        </view>
        <view class="mb-[40rpx] ml-[20rpx]">
          <text class="text-[28rpx] text-[#333333] leading-[1.8]">
            7.2 运营者未行使或延迟行使本协议项下的任何权利，不构成对该权利的放弃。
          </text>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<style lang="scss" scoped>
</style>
