package org.linghu.mybackend.service;

import java.util.List;

import org.linghu.mybackend.dto.AnnouncementDTO;

public interface AnnouncementService {
    AnnouncementDTO createAnnouncement(AnnouncementDTO announcementDTO);
    AnnouncementDTO getAnnouncementById(String id);
    List<AnnouncementDTO> getAllAnnouncements();
    void deleteAnnouncement(String id);
    AnnouncementDTO updateAnnouncement(String id, AnnouncementDTO announcementDTO);
}
