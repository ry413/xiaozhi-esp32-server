package xiaozhi.modules.wallpaper.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xiaozhi.common.service.impl.BaseServiceImpl;
import xiaozhi.modules.wallpaper.dao.WallpaperDao;
import xiaozhi.modules.wallpaper.dto.WallpaperDTO;
import xiaozhi.modules.wallpaper.entity.WallpaperEntity;
import xiaozhi.modules.wallpaper.service.WallpaperService;

@Slf4j
@Service
@AllArgsConstructor
public class WallpaperServiceImpl extends BaseServiceImpl<WallpaperDao, WallpaperEntity> implements WallpaperService {
    
    private WallpaperDao wallpaperDao;
    private static final String WALLPAPER_DIR = "/Users/ry79/xzwp";
    private static final String WALLPAPER_BASE_URL = "http://192.168.2.27:8536/";

    @Override
    public List<WallpaperDTO> getWallpapersForUser(Long userId) {
        QueryWrapper<WallpaperEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .or()
               .eq("is_builtin", 1);

        List<WallpaperEntity> entities = baseDao.selectList(wrapper);

        return entities.stream().map(e -> {
            return new WallpaperDTO(e.getId(), e.getUrl(), e.getIsBuiltin());
        }).toList();
    }

    
    @Override
    public List<WallpaperDTO> getWallpapersByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<WallpaperEntity> entities = wallpaperDao.selectBatchIds(ids);

        // 映射成 DTO，只暴露 id + url
        return entities.stream().map(e -> {
            return new WallpaperDTO(e.getId(), e.getUrl(), e.getIsBuiltin());
        }).toList();
    }

    @Override
    public void deleteWallpaper(Integer id, Long currentUserId) {
        // 这里后面可以加权限校验：只能删自己的，不能删内置的之类
        wallpaperDao.deleteById(id);
    }

    @Override
    public Integer uploadWallpaper(MultipartFile file, Long currentUserId) {
        // 1. 校验文件类型/大小
        // 2. 存储到对象存储/本地磁盘，得到 url
        String url = uploadToStorage(file);

        WallpaperEntity entity = new WallpaperEntity();
        entity.setUrl(url);
        entity.setUserId(currentUserId);
        wallpaperDao.insert(entity);

        return entity.getId();
    }
    
    private String uploadToStorage(MultipartFile file) {
        try {
            // 1. 确保目录存在
            Path dir = Paths.get(WALLPAPER_DIR);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            // 2. 取原始文件名，拿到扩展名
            String originalFilename = file.getOriginalFilename(); // 例如 "xxx.png"
            String ext = "";

            if (originalFilename != null) {
                int dotIndex = originalFilename.lastIndexOf('.');
                if (dotIndex >= 0) {
                    ext = originalFilename.substring(dotIndex); // 包含点，比如 ".png"
                }
            }

            // 3. 生成一个不重复的新文件名
            String newFileName = UUID.randomUUID().toString().replace("-", "") + ext;

            // 4. 目标路径
            Path target = dir.resolve(newFileName);

            // 5. 把上传内容写到目标文件
            file.transferTo(target.toFile());

            // 6. 返回可访问的 URL
            return WALLPAPER_BASE_URL + newFileName;
        } catch (IOException e) {
            throw new RuntimeException("保存壁纸失败", e);
        }
    }

}
