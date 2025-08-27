package org.linghu.mybackend.controller;

import java.util.List;

import org.linghu.mybackend.dto.AnnouncementDTO;
import org.linghu.mybackend.dto.AnnouncementRequestDTO;
import org.linghu.mybackend.dto.Result;
import org.linghu.mybackend.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 公告API控制器
 */
@RestController
@RequestMapping("/api/announcements")
@Tag(name = "公告管理", description = "公告管理相关API")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @Autowired
    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @PostMapping
    @io.swagger.v3.oas.annotations.Operation(summary = "创建公告", description = "创建一条新的公告，所有人可见")
    public Result<AnnouncementDTO> createAnnouncement(@RequestBody AnnouncementRequestDTO requestDTO) {
        AnnouncementDTO dto = AnnouncementDTO.builder()
                .title(requestDTO.getTitle())
                .content(requestDTO.getContent())
                .build();
        return Result.success(announcementService.createAnnouncement(dto));
    }

    @GetMapping("/{id}")
    @io.swagger.v3.oas.annotations.Operation(summary = "获取公告", description = "根据ID获取公告详情")
    public Result<AnnouncementDTO> getAnnouncementById(@PathVariable String id) {
        return Result.success(announcementService.getAnnouncementById(id));
    }

    @GetMapping
    @io.swagger.v3.oas.annotations.Operation(summary = "获取全部公告", description = "获取所有公告列表")
    public Result<List<AnnouncementDTO>> getAllAnnouncements() {
        return Result.success(announcementService.getAllAnnouncements());
    }

    @DeleteMapping("/{id}")
    @io.swagger.v3.oas.annotations.Operation(summary = "删除公告", description = "删除指定ID的公告")
    public Result<Void> deleteAnnouncement(@PathVariable String id) {
        announcementService.deleteAnnouncement(id);
        return Result.success();
    }

    @PutMapping("/{id}")
    @io.swagger.v3.oas.annotations.Operation(summary = "修改公告", description = "根据ID修改公告内容")
    public Result<AnnouncementDTO> updateAnnouncement(@PathVariable String id, @RequestBody AnnouncementRequestDTO requestDTO) {
        AnnouncementDTO dto = AnnouncementDTO.builder()
                .title(requestDTO.getTitle())
                .content(requestDTO.getContent())
                .build();
        AnnouncementDTO updated = announcementService.updateAnnouncement(id, dto);
        if (updated == null) {
            return Result.failure(404, "公告不存在");
        }
        return Result.success(updated);
    }
}
