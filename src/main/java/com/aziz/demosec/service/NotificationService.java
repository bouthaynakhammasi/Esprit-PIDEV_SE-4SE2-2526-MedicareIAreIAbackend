package com.aziz.demosec.service;

import com.aziz.demosec.dto.pharmacy.NotificationResponseDTO;
import java.util.List;

public interface NotificationService {
    List<NotificationResponseDTO> getForUser(Long userId);
    List<NotificationResponseDTO> getUnread(Long userId);
    long countUnread(Long userId);
    public void  markAsRead(Long id);
    void markAllAsRead(Long userId);
    void notifyAdminsOfNewPharmacist(com.aziz.demosec.Entities.Pharmacist pharmacist);
    void notifyAdminsOfNewProvider(com.aziz.demosec.Entities.ServiceProvider provider);
    void notifyAccountActivated(com.aziz.demosec.domain.User user);
    void notifyDeliveryCreated(com.aziz.demosec.Entities.Delivery delivery);
    void notifyDeliveryStatusUpdate(com.aziz.demosec.Entities.Delivery delivery);
    public NotificationResponseDTO markAsReadById(Long id);
}
