package xiaozhi.modules.wallpaper.controller;

import java.util.List;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import xiaozhi.common.user.UserDetail;
import xiaozhi.common.utils.Result;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;
import xiaozhi.modules.device.service.DeviceService;
import xiaozhi.modules.security.user.SecurityUser;
import xiaozhi.modules.wallpaper.dto.WallpaperDTO;
import xiaozhi.modules.wallpaper.service.WallpaperAuthService;
import xiaozhi.modules.wallpaper.service.WallpaperService;

@Slf4j
@Tag(name = "壁纸管理")
@RestController
@RequestMapping("/wallpaper")
public class WallpaperController {
    private final WallpaperService wallpaperService;
    private final WallpaperAuthService wallpaperAuthService;
    private final DeviceService deviceService;

    public WallpaperController(WallpaperService wallpaperService, WallpaperAuthService wallpaperAuthService, DeviceService deviceService) {
        this.wallpaperService = wallpaperService;
        this.wallpaperAuthService = wallpaperAuthService;
        this.deviceService = deviceService;
    }

    @GetMapping("/list")
    @Operation(summary = "获取当前用户所有壁纸")
    @RequiresPermissions("sys:role:normal")
    public Result<List<WallpaperDTO>> getWallpapersList() {
        UserDetail user = SecurityUser.getUser();
        List<WallpaperDTO> list = wallpaperService.getWallpapersForUser(user.getId());
        return new Result<List<WallpaperDTO>>().ok(list);
    }

    @PostMapping("/batch")
    @Operation(summary = "按id批量获取壁纸信息")
    @RequiresPermissions("sys:role:normal")
    public Result<List<WallpaperDTO>> getWallpapersByIds(@RequestBody List<Integer> ids) {
        List<WallpaperDTO> list = wallpaperService.getWallpapersByIds(ids);
        return new Result<List<WallpaperDTO>>().ok(list);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除壁纸")
    @RequiresPermissions("sys:role:normal")
    public Result<Void> deleteWallpaper(@PathVariable Integer id) {
        UserDetail user = SecurityUser.getUser();
        wallpaperService.deleteWallpaper(id, user.getId());
        return new Result<>();
    }

    @PostMapping("/upload")
    @Operation(summary = "上传壁纸")
    @RequiresPermissions("sys:role:normal")
    public Result<Integer> uploadWallpaper(@RequestParam("file") MultipartFile file) {
        log.info("uploadWallpaper called");
        UserDetail user = SecurityUser.getUser();
        Integer id = wallpaperService.uploadWallpaper(file, user.getId());
        return new Result<Integer>().ok(id);
    }

    @GetMapping("/device-batch")
    @Operation(summary = "给esp32直接使用, 按id批量获取壁纸信息")
    public Result<List<WallpaperDTO>> getWallpapersByIdsForDevice(
            @RequestHeader("X-Device-Id") String deviceMac,
            @RequestHeader("X-Device-Ts") long ts,
            @RequestHeader("X-Device-Sign") String sign) {
        if (!wallpaperAuthService.verify(deviceMac, ts, sign)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        List<Integer> wallpaper_ids = deviceService.getDeviceWallpaperIds(deviceMac);
        List<WallpaperDTO> list = wallpaperService.getWallpapersByIds(wallpaper_ids);
        return new Result<List<WallpaperDTO>>().ok(list);
    }
}