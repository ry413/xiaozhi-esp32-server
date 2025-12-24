package xiaozhi.modules.wallpaper.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import xiaozhi.common.service.BaseService;
import xiaozhi.modules.wallpaper.dto.WallpaperDTO;
import xiaozhi.modules.wallpaper.entity.WallpaperEntity;

public interface WallpaperService extends BaseService<WallpaperEntity> {
    List<WallpaperDTO> getWallpapersForUser(Long userId);

    List<WallpaperDTO> getWallpapersByIds(List<Integer> ids);

    void deleteWallpaper(Integer id, Long currentUserId);

    Integer uploadWallpaper(MultipartFile file, Long currentUserId);
}