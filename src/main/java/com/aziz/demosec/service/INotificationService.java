package com.aziz.demosec.service;

import com.aziz.demosec.dto.NotificationDTO;
import com.aziz.demosec.Entities.Notification;
import java.util.List;

public interface INotificationService {
    List<NotificationDTO> getUnreadForAdmin(String email);
    void markAsRead(Long id);
    void markAllAsRead(String email);
    void deleteNotification(Long id);
    void clearAll(String email);
    void sendNotification(Notification notification);
}
