package xiaozhi.modules.voiceclone.service.impl;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.framework.AopContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.dashscope.audio.ttsv2.enrollment.Voice;
import com.alibaba.dashscope.audio.ttsv2.enrollment.VoiceEnrollmentParam;
import com.alibaba.dashscope.audio.ttsv2.enrollment.VoiceEnrollmentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xiaozhi.common.config.CosProperties;
import xiaozhi.common.constant.Constant;
import xiaozhi.common.exception.ErrorCode;
import xiaozhi.common.exception.RenException;
import xiaozhi.common.page.PageData;
import xiaozhi.common.service.impl.BaseServiceImpl;
import xiaozhi.common.utils.ConvertUtils;
import xiaozhi.common.utils.DateUtils;
import xiaozhi.common.utils.ToolUtil;
import xiaozhi.modules.model.entity.ModelConfigEntity;
import xiaozhi.modules.model.service.ModelConfigService;
import xiaozhi.modules.sys.dao.SysUserDao;
import xiaozhi.modules.sys.entity.SysUserEntity;
import xiaozhi.modules.voiceclone.dao.VoiceCloneDao;
import xiaozhi.modules.voiceclone.dto.VoiceCloneDTO;
import xiaozhi.modules.voiceclone.dto.VoiceCloneResponseDTO;
import xiaozhi.modules.voiceclone.entity.VoiceCloneEntity;
import xiaozhi.modules.voiceclone.service.VoiceCloneService;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceCloneServiceImpl extends BaseServiceImpl<VoiceCloneDao, VoiceCloneEntity>
        implements VoiceCloneService {
    private static final String COSYVOICE_CLONE_STREAM = "cosyvoice_clone_stream";
    private static final String COSYVOICE_REAL_VOICE_PREFIX = "cosyvoice-";
    private static final String COSYVOICE_CLONE_MODEL_NAME = "voice-enrollment";
    private static final String COSYVOICE_STATUS_OK = "OK";
    private static final String COSYVOICE_STATUS_UNDEPLOYED = "UNDEPLOYED";
    private static final int COSYVOICE_POLL_MAX_ATTEMPTS = 30;
    private static final long COSYVOICE_POLL_INTERVAL_MS = 2000L;

    private final ModelConfigService modelConfigService;
    private final SysUserDao sysUserDao;
    private final ObjectMapper objectMapper;
    private final COSClient cosClient;
    private final CosProperties cosProperties;

    @Override
    public PageData<VoiceCloneEntity> page(Map<String, Object> params) {
        IPage<VoiceCloneEntity> page = baseDao.selectPage(
                getPage(params, "create_date", true),
                getWrapper(params));

        return new PageData<>(page.getRecords(), page.getTotal());
    }

    private QueryWrapper<VoiceCloneEntity> getWrapper(Map<String, Object> params) {
        String name = (String) params.get("name");
        String userId = (String) params.get("userId");

        QueryWrapper<VoiceCloneEntity> wrapper = new QueryWrapper<>();
        wrapper.eq(StringUtils.isNotBlank(userId), "user_id", userId);
        if (StringUtils.isNotBlank(name)) {
            wrapper.and(w -> w.like("name", name)
                    .or().eq("voice_id", name));
        }
        return wrapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(VoiceCloneDTO dto) {
        ModelConfigEntity modelConfig = modelConfigService.getModelByIdFromCache(dto.getModelId());
        if (modelConfig == null || modelConfig.getConfigJson() == null) {
            throw new RenException(ErrorCode.VOICE_CLONE_MODEL_CONFIG_NOT_FOUND);
        }
        Map<String, Object> config = modelConfig.getConfigJson();
        String type = (String) config.get("type");
        if (StringUtils.isBlank(type)) {
            throw new RenException(ErrorCode.VOICE_CLONE_MODEL_TYPE_NOT_FOUND);
        }

        // 检查Voice ID是否已经被使用
        for (String voiceId : dto.getVoiceIds()) {
            if (StringUtils.isBlank(voiceId)) {
                continue;
            }
            if (Constant.VOICE_CLONE_HUOSHAN_DOUBLE_STREAM.equals(type)) {
                if (voiceId.indexOf("S_") == -1) {
                    throw new RenException(ErrorCode.VOICE_CLONE_HUOSHAN_VOICE_ID_ERROR);
                }
            }

            QueryWrapper<VoiceCloneEntity> wrapper = new QueryWrapper<>();
            wrapper.eq("voice_id", voiceId);
            wrapper.eq("model_id", dto.getModelId());
            Long count = baseDao.selectCount(wrapper);
            if (count > 0) {
                throw new RenException(ErrorCode.VOICE_ID_ALREADY_EXISTS);
            }
        }

        // 批量保存
        List<VoiceCloneEntity> batchInsertList = new ArrayList<>();
        // 遍历选择的音色ID，为每个音色ID创建一条记录
        int index = 0;
        String namePrefix = DateUtils.format(new Date(), "MMddHHmm");
        for (String voiceId : dto.getVoiceIds()) {
            index++;
            VoiceCloneEntity entity = new VoiceCloneEntity();
            entity.setModelId(dto.getModelId());
            entity.setVoiceId(voiceId);
            entity.setName(namePrefix + "_" + index);
            entity.setUserId(dto.getUserId());
            entity.setLanguages(dto.getLanguages());
            entity.setTrainStatus(0); // 默认训练中
            batchInsertList.add(entity);
        }
        if (ToolUtil.isNotEmpty(batchInsertList)) {
            insertBatch(batchInsertList);
        }
    }

    @Override
    public void delete(String[] ids) {
        if (ids == null || ids.length == 0) {
            return;
        }

        List<VoiceCloneEntity> entities = baseDao.selectBatchIds(Arrays.asList(ids));
        for (VoiceCloneEntity entity : entities) {
            deleteRemoteCloneVoice(entity);
            String objectKey = resolveCloneSampleObjectKey(entity.getVoiceSourceUrl());
            if (StringUtils.isNotBlank(objectKey)) {
                try {
                    cosClient.deleteObject(cosProperties.getBucket(), objectKey);
                } catch (Exception e) {
                    log.warn("删除音色样本 COS 文件失败, id={}, objectKey={}", entity.getId(), objectKey, e);
                }
            }
        }
        baseDao.deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    public List<VoiceCloneEntity> getByUserId(Long userId) {
        QueryWrapper<VoiceCloneEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.orderByDesc("created_at");
        return baseDao.selectList(wrapper);
    }

    @Override
    public PageData<VoiceCloneResponseDTO> pageWithNames(Map<String, Object> params) {
        // 先查询分页数据
        IPage<VoiceCloneEntity> page = baseDao.selectPage(
                getPage(params, "create_date", true),
                getWrapper(params));

        // 将实体列表转换为DTO列表
        List<VoiceCloneResponseDTO> dtoList = convertToResponseDTOList(page.getRecords());

        return new PageData<>(dtoList, page.getTotal());
    }

    @Override
    public VoiceCloneResponseDTO getByIdWithNames(String id) {
        VoiceCloneEntity entity = baseDao.selectById(id);
        if (entity == null) {
            return null;
        }

        VoiceCloneResponseDTO dto = ConvertUtils.sourceToTarget(entity, VoiceCloneResponseDTO.class);

        // 设置模型名称
        if (StringUtils.isNotBlank(entity.getModelId())) {
            dto.setModelName(modelConfigService.getModelNameById(entity.getModelId()));
        }

        // 设置用户名称
        if (entity.getUserId() != null) {
            SysUserEntity user = sysUserDao.selectById(entity.getUserId());
            if (user != null) {
                dto.setUserName(user.getUsername());
            }
        }
        
        // 确保trainStatus字段被正确设置，前端需要这个字段来判断是否为克隆音频
        dto.setTrainStatus(entity.getTrainStatus());

        return dto;
    }

    @Override
    public List<VoiceCloneResponseDTO> getByUserIdWithNames(Long userId) {
        List<VoiceCloneEntity> entityList = getByUserId(userId);
        return convertToResponseDTOList(entityList);
    }

    /**
     * 将VoiceCloneEntity列表转换为VoiceCloneResponseDTO列表
     */
    private List<VoiceCloneResponseDTO> convertToResponseDTOList(List<VoiceCloneEntity> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            return new ArrayList<>();
        }

        List<VoiceCloneResponseDTO> dtoList = new ArrayList<>(entityList.size());

        // 获取用户名称ID集合
        Set<Long> userIdList = entityList.stream().map(VoiceCloneEntity::getUserId).collect(Collectors.toSet());
        List<SysUserEntity> userList = sysUserDao.selectList(new QueryWrapper<SysUserEntity>().in("id", userIdList));
        Map<Long, String> userMap = userList.stream().collect(Collectors.toMap(SysUserEntity::getId, SysUserEntity::getUsername));

        // 转换每个实体为DTO
        for (VoiceCloneEntity entity : entityList) {
            VoiceCloneResponseDTO dto = ConvertUtils.sourceToTarget(entity, VoiceCloneResponseDTO.class);

            // 设置模型名称
            if (StringUtils.isNotBlank(entity.getModelId())) {
                dto.setModelName(modelConfigService.getModelNameById(entity.getModelId()));
            }

            // 设置用户名称
            if (entity.getUserId() != null) {
                dto.setUserName(userMap.get(entity.getUserId()));
            }
            
            // 确保trainStatus字段被正确设置，前端需要这个字段来判断是否为克隆音频
            dto.setTrainStatus(entity.getTrainStatus());

            // 设置是否有音频数据
            dto.setHasVoice(entity.getVoice() != null);

            dtoList.add(dto);
        }

        return dtoList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadVoice(String id, MultipartFile voiceFile) throws Exception {
        // 查询声音克隆记录
        VoiceCloneEntity entity = baseDao.selectById(id);
        if (entity == null) {
            throw new RenException(ErrorCode.VOICE_CLONE_RECORD_NOT_EXIST);
        }

        ModelConfigEntity modelConfig = modelConfigService.getModelByIdFromCache(entity.getModelId());
        if (modelConfig == null || modelConfig.getConfigJson() == null) {
            throw new RenException(ErrorCode.VOICE_CLONE_MODEL_CONFIG_NOT_FOUND);
        }
        String type = String.valueOf(modelConfig.getConfigJson().get("type"));

        // 读取音频文件并转为字节数组
        byte[] voiceData = voiceFile.getBytes();

        // 更新voice字段
        entity.setVoice(voiceData);
        if (COSYVOICE_CLONE_STREAM.equals(type)) {
            entity.setVoiceSourceUrl(uploadCloneSampleToCos(voiceData, voiceFile.getOriginalFilename()));
        }
        // 更新训练状态为待训练
        entity.setTrainStatus(0);
        entity.setTrainError("");

        // 保存到数据库
        baseDao.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateName(String id, String name) {
        // 查询声音克隆记录
        VoiceCloneEntity entity = baseDao.selectById(id);
        if (entity == null) {
            throw new RenException(ErrorCode.VOICE_CLONE_RECORD_NOT_EXIST);
        }

        // 更新名称
        entity.setName(name);
        baseDao.updateById(entity);
    }

    @Override
    public byte[] getVoiceData(String id) {
        VoiceCloneEntity entity = baseDao.selectById(id);
        if (entity == null) {
            return null;
        }
        return entity.getVoice();
    }

    @Override
    // @Transactional(rollbackFor = Exception.class)
    public void cloneAudio(String cloneId) {
        VoiceCloneEntity entity = baseDao.selectById(cloneId);
        if (entity == null) {
            throw new RenException(ErrorCode.VOICE_CLONE_RECORD_NOT_EXIST);
        }
        if (entity.getVoice() == null || entity.getVoice().length == 0) {
            throw new RenException(ErrorCode.VOICE_CLONE_AUDIO_NOT_UPLOADED);
        }
        if (Objects.equals(entity.getTrainStatus(), 1)) {
            throw new RenException("当前音色正在复刻中，请稍后查看结果");
        }

        entity.setTrainStatus(1);
        entity.setTrainError("");
        baseDao.updateById(entity);
        ((VoiceCloneServiceImpl) AopContext.currentProxy()).processCloneAudioAsync(cloneId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantInitialVoiceResource(Long userId) {
        if (userId == null) {
            return;
        }

        QueryWrapper<VoiceCloneEntity> existingWrapper = new QueryWrapper<>();
        existingWrapper.eq("user_id", userId);
        if (baseDao.selectCount(existingWrapper) > 0) {
            return;
        }

        ModelConfigEntity modelConfig = findInitialCosyVoiceModel();
        if (modelConfig == null) {
            log.warn("未找到可用的 cosyvoice_clone_stream TTS 模型，跳过发放初始音色资源, userId={}", userId);
            return;
        }

        VoiceCloneEntity entity = new VoiceCloneEntity();
        entity.setModelId(modelConfig.getId());
        entity.setVoiceId(buildInitialSlotVoiceId(userId));
        entity.setName(DateUtils.format(new Date(), "MMddHHmm") + "_1");
        entity.setUserId(userId);
        entity.setLanguages("全部");
        entity.setTrainStatus(0);
        entity.setTrainError("");
        insert(entity);
    }

    @Async("taskExecutor")
    public void processCloneAudioAsync(String cloneId) {
        try {
            processCloneAudio(cloneId);
        } catch (Exception e) {
            log.error("异步执行音色复刻失败, cloneId={}", cloneId, e);
        }
    }

    public void processCloneAudio(String cloneId) {
        VoiceCloneEntity entity = baseDao.selectById(cloneId);
        if (entity == null) {
            throw new RenException(ErrorCode.VOICE_CLONE_RECORD_NOT_EXIST);
        }
        if (entity.getVoice() == null || entity.getVoice().length == 0) {
            throw new RenException(ErrorCode.VOICE_CLONE_AUDIO_NOT_UPLOADED);
        }
        try {
            ModelConfigEntity modelConfig = modelConfigService.getModelByIdFromCache(entity.getModelId());
            if (modelConfig == null || modelConfig.getConfigJson() == null) {
                throw new RenException(ErrorCode.VOICE_CLONE_MODEL_CONFIG_NOT_FOUND);
            }
            Map<String, Object> config = modelConfig.getConfigJson();
            String type = (String) config.get("type");
            if (StringUtils.isBlank(type)) {
                throw new RenException(ErrorCode.VOICE_CLONE_MODEL_TYPE_NOT_FOUND);
            }
            if (Constant.VOICE_CLONE_HUOSHAN_DOUBLE_STREAM.equals(type)) {
                huoshanClone(config, entity);
            } else if (COSYVOICE_CLONE_STREAM.equals(type)) {
                cosyvoiceClone(config, entity);
            } else {
                throw new RenException("暂不支持该音色克隆类型: " + type);
            }
        } catch (RenException re) {
            entity.setTrainStatus(3);
            entity.setTrainError(re.getMsg());
            baseDao.updateById(entity);
            throw re;
        } catch (Exception e) {
            e.printStackTrace();
            entity.setTrainStatus(3);
            entity.setTrainError(e.getMessage());
            baseDao.updateById(entity);
            throw new RenException(ErrorCode.VOICE_CLONE_TRAINING_FAILED, e.getMessage());
        }
    }

    private void cosyvoiceClone(Map<String, Object> config, VoiceCloneEntity entity) throws Exception {
        String apiKey = getConfigString(config, "api_key");
        String targetModel = getConfigString(config, "model");
        String publicAudioUrl = entity.getVoiceSourceUrl();
        String cloneModelName = getConfigString(config, "clone_model_name");

        if (StringUtils.isAnyBlank(apiKey, targetModel, publicAudioUrl)) {
            throw new RenException("CosyVoice克隆配置不完整，请补充api_key、model");
        }

        entity.setTrainStatus(1);
        entity.setTrainError("");
        baseDao.updateById(entity);

        VoiceEnrollmentService service = StringUtils.isNotBlank(cloneModelName)
                ? new VoiceEnrollmentService(apiKey, cloneModelName)
                : new VoiceEnrollmentService(apiKey);

        VoiceEnrollmentParam customParam = buildCosyvoiceEnrollmentParam(config);
        String currentVoiceId = entity.getVoiceId();

        if (isCosyvoiceRealVoiceId(currentVoiceId)) {
            service.updateVoice(currentVoiceId, publicAudioUrl, customParam);
            pollCosyvoiceStatus(service, currentVoiceId, entity);
            return;
        }

        String prefix = buildCosyvoicePrefix(entity, config);
        Voice createdVoice = service.createVoice(targetModel, prefix, publicAudioUrl, customParam);
        if (createdVoice == null || StringUtils.isBlank(createdVoice.getVoiceId())) {
            throw new RenException("CosyVoice创建音色失败，未返回voiceId");
        }

        entity.setVoiceId(createdVoice.getVoiceId());
        entity.setTrainError("");
        baseDao.updateById(entity);
        pollCosyvoiceStatus(service, createdVoice.getVoiceId(), entity);
    }

    private VoiceEnrollmentParam buildCosyvoiceEnrollmentParam(Map<String, Object> config) {
        VoiceEnrollmentParam.VoiceEnrollmentParamBuilder<?, ?> builder = VoiceEnrollmentParam.builder();

        String cloneModelName = getConfigString(config, "clone_model_name");
        builder.model(StringUtils.defaultIfBlank(cloneModelName, COSYVOICE_CLONE_MODEL_NAME));

        List<String> languageHints = getLanguageHints(config.get("language_hints"));
        if (!languageHints.isEmpty()) {
            builder.languageHints(languageHints);
        }

        Number maxPromptAudioLength = getConfigNumber(config.get("max_prompt_audio_length"));
        if (maxPromptAudioLength != null) {
            builder.maxPromptAudioLength(maxPromptAudioLength.floatValue());
        }

        Map<String, Object> parameters = new HashMap<>();
        Object enablePreprocess = config.get("enable_preprocess");
        if (enablePreprocess != null) {
            parameters.put("enable_preprocess", enablePreprocess);
        }
        if (!parameters.isEmpty()) {
            builder.parameters(parameters);
        }

        return builder.build();
    }

    private void pollCosyvoiceStatus(VoiceEnrollmentService service, String voiceId, VoiceCloneEntity entity)
            throws Exception {
        for (int i = 0; i < COSYVOICE_POLL_MAX_ATTEMPTS; i++) {
            Voice voice = service.queryVoice(voiceId);
            if (voice == null || StringUtils.isBlank(voice.getStatus())) {
                throw new RenException("CosyVoice查询音色状态失败");
            }

            String status = voice.getStatus();
            if (COSYVOICE_STATUS_OK.equalsIgnoreCase(status)) {
                entity.setTrainStatus(2);
                entity.setTrainError("");
                baseDao.updateById(entity);
                return;
            }
            if (COSYVOICE_STATUS_UNDEPLOYED.equalsIgnoreCase(status)) {
                throw new RenException("CosyVoice音色审核未通过");
            }

            Thread.sleep(COSYVOICE_POLL_INTERVAL_MS);
        }

        throw new RenException("CosyVoice音色仍在部署中，请稍后重试");
    }

    private void deleteRemoteCloneVoice(VoiceCloneEntity entity) {
        if (entity == null || !isCosyvoiceRealVoiceId(entity.getVoiceId())) {
            return;
        }

        try {
            ModelConfigEntity modelConfig = modelConfigService.getModelByIdFromCache(entity.getModelId());
            if (modelConfig == null || modelConfig.getConfigJson() == null) {
                return;
            }
            Map<String, Object> config = modelConfig.getConfigJson();
            String type = String.valueOf(config.get("type"));
            if (!COSYVOICE_CLONE_STREAM.equals(type)) {
                return;
            }

            String apiKey = getConfigString(config, "api_key");
            if (StringUtils.isBlank(apiKey)) {
                return;
            }

            String cloneModelName = getConfigString(config, "clone_model_name");
            VoiceEnrollmentService service = StringUtils.isNotBlank(cloneModelName)
                    ? new VoiceEnrollmentService(apiKey, cloneModelName)
                    : new VoiceEnrollmentService(apiKey);
            service.deleteVoice(entity.getVoiceId());
        } catch (Exception e) {
            log.warn("删除远端 CosyVoice 音色失败, id={}, voiceId={}", entity.getId(), entity.getVoiceId(), e);
        }
    }

    private String uploadCloneSampleToCos(byte[] voiceData, String originalFilename) {
        String extension = getFileExtension(originalFilename);
        String fileKey = UUID.randomUUID().toString().replace("-", "") + extension;
        String objectKey = buildCloneSampleObjectKey(fileKey);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(voiceData.length);
        metadata.setContentType(resolveAudioContentType(extension));

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(voiceData)) {
            PutObjectRequest request = new PutObjectRequest(
                    cosProperties.getBucket(),
                    objectKey,
                    inputStream,
                    metadata);
            cosClient.putObject(request);
        } catch (Exception e) {
            throw new RenException("上传音频样本到 COS 失败: " + e.getMessage());
        }

        return buildPublicUrl(objectKey);
    }

    private String buildCloneSampleObjectKey(String fileKey) {
        return normalizePrefix(cosProperties.getCloneSamplePathPrefix()) + fileKey;
    }

    private String buildPublicUrl(String objectKey) {
        if (StringUtils.isNotBlank(cosProperties.getBaseUrl())) {
            String baseUrl = cosProperties.getBaseUrl().endsWith("/")
                    ? cosProperties.getBaseUrl()
                    : cosProperties.getBaseUrl() + "/";
            return baseUrl + objectKey;
        }
        return String.format("https://%s.cos.%s.myqcloud.com/%s",
                cosProperties.getBucket(),
                cosProperties.getRegion(),
                objectKey);
    }

    private String resolveCloneSampleObjectKey(String sourceUrl) {
        if (StringUtils.isBlank(sourceUrl)) {
            return "";
        }
        String key = sourceUrl.trim();
        String baseUrl = cosProperties.getBaseUrl();
        if (StringUtils.isNotBlank(baseUrl)) {
            String normalizedBase = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
            if (key.startsWith(normalizedBase)) {
                key = key.substring(normalizedBase.length());
            }
        }
        if (key.startsWith("http://") || key.startsWith("https://")) {
            int idx = key.indexOf("myqcloud.com/");
            if (idx >= 0) {
                key = key.substring(idx + "myqcloud.com/".length());
            }
        }
        if (key.startsWith("/")) {
            key = key.substring(1);
        }
        String expectedPrefix = normalizePrefix(cosProperties.getCloneSamplePathPrefix());
        if (StringUtils.isNotBlank(expectedPrefix) && !key.startsWith(expectedPrefix)) {
            key = expectedPrefix + key;
        }
        return key;
    }

    private String normalizePrefix(String prefix) {
        if (StringUtils.isBlank(prefix)) {
            return "";
        }
        String normalized = prefix.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (!normalized.endsWith("/")) {
            normalized += "/";
        }
        return normalized;
    }

    private String getFileExtension(String originalFilename) {
        if (StringUtils.isBlank(originalFilename) || !originalFilename.contains(".")) {
            return ".wav";
        }
        return originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
    }

    private String resolveAudioContentType(String extension) {
        return switch (extension) {
            case ".mp3" -> "audio/mpeg";
            case ".wav" -> "audio/wav";
            default -> "application/octet-stream";
        };
    }

    private boolean isCosyvoiceRealVoiceId(String voiceId) {
        return StringUtils.isNotBlank(voiceId) && voiceId.startsWith(COSYVOICE_REAL_VOICE_PREFIX);
    }

    private ModelConfigEntity findInitialCosyVoiceModel() {
        List<ModelConfigEntity> models = modelConfigService.getEnabledModelsByType("TTS");
        if (ToolUtil.isEmpty(models)) {
            return null;
        }

        for (ModelConfigEntity model : models) {
            if (model == null || model.getConfigJson() == null) {
                continue;
            }
            Object type = model.getConfigJson().get("type");
            if (COSYVOICE_CLONE_STREAM.equals(type)) {
                return model;
            }
        }
        return null;
    }

    private String buildInitialSlotVoiceId(Long userId) {
        return "CV_SLOT_" + userId + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String buildCosyvoicePrefix(VoiceCloneEntity entity, Map<String, Object> config) {
        String configuredPrefix = getConfigString(config, "prefix");
        if (StringUtils.isNotBlank(configuredPrefix)) {
            return sanitizeCosyvoicePrefix(configuredPrefix);
        }

        String suffix = entity.getId().replace("-", "").toLowerCase(Locale.ROOT);
        return sanitizeCosyvoicePrefix("cv" + suffix.substring(0, Math.min(8, suffix.length())));
    }

    private String sanitizeCosyvoicePrefix(String prefix) {
        String sanitized = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "");
        if (StringUtils.isBlank(sanitized)) {
            sanitized = "cvslot";
        }
        return sanitized.substring(0, Math.min(10, sanitized.length()));
    }

    private List<String> getLanguageHints(Object value) {
        if (value instanceof List<?> listValue) {
            return listValue.stream().filter(Objects::nonNull).map(String::valueOf).filter(StringUtils::isNotBlank)
                    .toList();
        }
        if (value instanceof String stringValue && StringUtils.isNotBlank(stringValue)) {
            return Arrays.stream(stringValue.split(",")).map(String::trim).filter(StringUtils::isNotBlank).toList();
        }
        return Collections.emptyList();
    }

    private Number getConfigNumber(Object value) {
        if (value instanceof Number number) {
            return number;
        }
        if (value instanceof String stringValue && StringUtils.isNotBlank(stringValue)) {
            try {
                return Double.parseDouble(stringValue);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String getConfigString(Map<String, Object> config, String... keys) {
        for (String key : keys) {
            Object value = config.get(key);
            if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    /**
     * 调用火山引擎进行语音复刻训练
     * 
     * @param config 模型配置
     * @param entity 语音克隆记录实体
     * @throws Exception
     */
    private void huoshanClone(Map<String, Object> config, VoiceCloneEntity entity) throws Exception {
        String appid = (String) config.get("appid");
        String accessToken = (String) config.get("access_token");

        if (StringUtils.isAnyBlank(appid, accessToken)) {
            throw new RenException(ErrorCode.VOICE_CLONE_HUOSHAN_CONFIG_MISSING);
        }

        String audioBase64 = Base64.getEncoder().encodeToString(entity.getVoice());
        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("appid", appid);
        List<Map<String, String>> audios = new ArrayList<>();
        Map<String, String> audioMap = new HashMap<>();
        audioMap.put("audio_bytes", audioBase64);
        audioMap.put("audio_format", "wav");
        audios.add(audioMap);
        reqBody.put("audios", audios);
        reqBody.put("source", 2);
        reqBody.put("language", 0);
        reqBody.put("model_type", 1);
        reqBody.put("speaker_id", entity.getVoiceId());

        String apiUrl = "https://openspeech.bytedance.com/api/v1/mega_tts/audio/upload";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer;" + accessToken)
                .header("Resource-Id", "seed-icl-1.0")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(reqBody)))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println(">>> HTTP status = " + response.statusCode());
        System.out.println(">>> response body = " + response.body());

        Map<String, Object> rsp = objectMapper.readValue(response.body(),
                new TypeReference<Map<String, Object>>() {
                });

        // 获取BaseResp对象
        Map<String, Object> baseResp = objectMapper.convertValue(rsp.get("BaseResp"),
                new TypeReference<Map<String, Object>>() {
                });
        if (baseResp != null) {
            Integer statusCode = objectMapper.convertValue(baseResp.get("StatusCode"), Integer.class);
            String statusMessage = objectMapper.convertValue(baseResp.getOrDefault("StatusMessage", ""),
                    String.class);

            // 获取speaker_id
            String speakerId = objectMapper.convertValue(rsp.get("speaker_id"), String.class);

            // StatusCode == 0 表示成功
            if (statusCode != null && statusCode == 0 && StringUtils.isNotBlank(speakerId)) {
                entity.setTrainStatus(2);
                entity.setVoiceId(speakerId);
                entity.setTrainError("");
                baseDao.updateById(entity);
            } else {
                // 失败时使用StatusMessage作为错误信息
                String errorMsg = StringUtils.isNotBlank(statusMessage) ? statusMessage : "训练失败";
                throw new RenException(errorMsg);
            }
        } else {
            String errorMsg = objectMapper.convertValue(rsp.get("message"),
                    new TypeReference<String>() {
                    });
            if (StringUtils.isNotBlank(errorMsg)) {
                throw new RenException(errorMsg);
            }
            throw new RenException(ErrorCode.VOICE_CLONE_RESPONSE_FORMAT_ERROR);
        }
    }
}
