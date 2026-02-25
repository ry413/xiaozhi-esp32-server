package xiaozhi.modules.wallpaper.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xiaozhi.common.service.impl.BaseServiceImpl;
import xiaozhi.common.config.CosProperties;
import xiaozhi.modules.device.dao.DeviceDao;
import xiaozhi.modules.device.entity.DeviceEntity;
import xiaozhi.modules.sys.dao.SysUserDao;
import xiaozhi.modules.sys.entity.SysUserEntity;
import xiaozhi.modules.wallpaper.dao.WallpaperDao;
import xiaozhi.modules.wallpaper.dto.WallpaperDTO;
import xiaozhi.modules.wallpaper.entity.WallpaperEntity;
import xiaozhi.modules.wallpaper.service.WallpaperService;

@Slf4j
@Service
@AllArgsConstructor
public class WallpaperServiceImpl extends BaseServiceImpl<WallpaperDao, WallpaperEntity> implements WallpaperService {
    
    private final WallpaperDao wallpaperDao;
    private final COSClient cosClient;
    private final CosProperties cosProperties;
    private final SysUserDao sysUserDao;
    private final DeviceDao deviceDao;

    @Override
    public List<WallpaperDTO> getWallpapersForUser(Long userId) {
        SysUserEntity user = sysUserDao.selectById(userId);
        Set<Integer> hiddenBuiltinIds = toHiddenSet(user);

        QueryWrapper<WallpaperEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .or()
               .eq("is_builtin", 1);

        List<WallpaperEntity> entities = baseDao.selectList(wrapper);
        if (!hiddenBuiltinIds.isEmpty()) {
            entities = entities.stream()
                    .filter(e -> !isBuiltin(e) || !hiddenBuiltinIds.contains(e.getId()))
                    .toList();
        }

        return entities.stream().map(e -> {
            return new WallpaperDTO(e.getId(), e.getFileKey(), e.getIsBuiltin());
        }).toList();
    }

    
    @Override
    public List<WallpaperDTO> getWallpapersByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<WallpaperEntity> entities = wallpaperDao.selectBatchIds(ids);

        // 映射成 DTO，只暴露 id + fileKey
        return entities.stream().map(e -> {
            return new WallpaperDTO(e.getId(), e.getFileKey(), e.getIsBuiltin());
        }).toList();
    }

    @Override
    public void deleteWallpaper(Integer id, Long currentUserId) {
        WallpaperEntity entity = wallpaperDao.selectById(id);
        if (entity == null) {
            return;
        }
        if (!isBuiltin(entity)) {
            String objectKey = resolveObjectKey(entity.getFileKey());
            if (StringUtils.hasText(objectKey)) {
                cosClient.deleteObject(cosProperties.getBucket(), objectKey);
            }
            wallpaperDao.deleteById(id);
            removeWallpaperFromDevices(currentUserId, id);
            return;
        }
        hideBuiltinWallpaperForUser(entity.getId(), currentUserId);
        removeWallpaperFromDevices(currentUserId, id);
    }

    @Override
    public Integer uploadWallpaper(MultipartFile file, Long currentUserId) {
        // 1. 校验文件类型/大小
        // 2. 存储到对象存储/本地磁盘，得到 fileKey
        String fileKey = uploadToStorage(file);

        WallpaperEntity entity = new WallpaperEntity();
        entity.setFileKey(fileKey);
        entity.setUserId(currentUserId);
        wallpaperDao.insert(entity);

        return entity.getId();
    }

    @Override
    public void clearHiddenBuiltinWallpapers(Long currentUserId) {
        SysUserEntity user = sysUserDao.selectById(currentUserId);
        if (user == null) {
            return;
        }
        user.setHiddenBuiltinWallpaperIds(Collections.emptyList());
        sysUserDao.updateById(user);
    }
    
    private String uploadToStorage(MultipartFile file) {
        String fileKey = buildFileKey(file);
        String objectKey = buildObjectKey(fileKey);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        if (StringUtils.hasText(file.getContentType())) {
            metadata.setContentType(file.getContentType());
        }

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest request = new PutObjectRequest(cosProperties.getBucket(), objectKey, inputStream, metadata);
            cosClient.putObject(request);
        } catch (IOException e) {
            throw new RuntimeException("上传壁纸到 COS 失败", e);
        }

        return fileKey;
    }

    private String buildFileKey(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null) {
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex >= 0) {
                ext = originalFilename.substring(dotIndex);
            }
        }
        return UUID.randomUUID().toString().replace("-", "") + ext;
    }

    private String buildObjectKey(String fileKey) {
        String prefix = normalizePrefix(cosProperties.getPathPrefix());
        return prefix + fileKey;
    }

    private String resolveObjectKey(String fileKey) {
        if (!StringUtils.hasText(fileKey)) {
            return "";
        }
        String key = fileKey.trim();
        String baseUrl = cosProperties.getBaseUrl();
        if (StringUtils.hasText(baseUrl)) {
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
        if (StringUtils.hasText(cosProperties.getPathPrefix()) && !key.startsWith(normalizePrefix(cosProperties.getPathPrefix()))) {
            String prefix = normalizePrefix(cosProperties.getPathPrefix());
            key = prefix + key;
        }
        return key;
    }

    private String normalizePrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return "";
        }
        String trimmed = prefix.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        if (!trimmed.endsWith("/")) {
            trimmed = trimmed + "/";
        }
        return trimmed;
    }

    private boolean isBuiltin(WallpaperEntity entity) {
        return entity.getIsBuiltin() != null && entity.getIsBuiltin() == 1;
    }

    private Set<Integer> toHiddenSet(SysUserEntity user) {
        if (user == null || user.getHiddenBuiltinWallpaperIds() == null || user.getHiddenBuiltinWallpaperIds().isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(user.getHiddenBuiltinWallpaperIds());
    }

    private void hideBuiltinWallpaperForUser(Integer wallpaperId, Long userId) {
        if (wallpaperId == null || userId == null) {
            return;
        }
        SysUserEntity user = sysUserDao.selectById(userId);
        if (user == null) {
            return;
        }
        List<Integer> hiddenIds = user.getHiddenBuiltinWallpaperIds();
        if (hiddenIds == null) {
            hiddenIds = new java.util.ArrayList<>();
        }
        if (!hiddenIds.contains(wallpaperId)) {
            hiddenIds.add(wallpaperId);
            user.setHiddenBuiltinWallpaperIds(hiddenIds);
            sysUserDao.updateById(user);
        }
    }

    private void removeWallpaperFromDevices(Long userId, Integer wallpaperId) {
        if (userId == null || wallpaperId == null) {
            return;
        }
        List<DeviceEntity> devices = deviceDao.selectList(new QueryWrapper<DeviceEntity>().eq("user_id", userId));
        if (devices == null || devices.isEmpty()) {
            return;
        }
        for (DeviceEntity device : devices) {
            List<Integer> wallpaperIds = device.getWallpaperIds();
            if (wallpaperIds == null || wallpaperIds.isEmpty()) {
                continue;
            }
            if (wallpaperIds.removeIf(id -> id != null && id.equals(wallpaperId))) {
                device.setWallpaperIds(wallpaperIds);
                deviceDao.updateById(device);
            }
        }
    }

}
