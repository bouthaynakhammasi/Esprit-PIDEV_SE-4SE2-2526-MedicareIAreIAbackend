package com.aziz.demosec.service;

import com.aziz.demosec.Entities.HomeCareService;
import com.aziz.demosec.Entities.ServiceProvider;
import com.aziz.demosec.Entities.ServiceRequest;
import com.aziz.demosec.Entities.ServiceRequestStatus;
import com.aziz.demosec.repository.ServiceProviderRepository;
import com.aziz.demosec.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class HomeCareRequestService {

    private final ServiceRequestRepository requestRepo;
    private final ServiceProviderRepository userRepo;
    private final SimpMessagingTemplate messagingTemplate;

    private ServiceProvider getCurrentProvider() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userRepo.findByUser_Email(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalArgumentException("Provider not found"));
        }
        throw new IllegalStateException("Authentication required");
    }

    @Transactional
    public ServiceRequest acceptRequest(Long requestId) {
        ServiceProvider provider = getCurrentProvider();
        ServiceRequest request = requestRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        if (!request.getStatus().equals(ServiceRequestStatus.PENDING)) {
            throw new IllegalStateException("Only PENDING requests can be accepted");
        }
        request.setAssignedProvider(provider);
        request.setStatus(ServiceRequestStatus.ACCEPTED);
        request.setAssignedDateTime(LocalDateTime.now());
        requestRepo.save(request);
        // notify patient
        messagingTemplate.convertAndSend("/queue/homecare", new HomeCareNotification(request.getId(), "ACCEPTED"));
        return request;
    }

    @Transactional
    public ServiceRequest declineRequest(Long requestId) {
        ServiceProvider provider = getCurrentProvider();
        ServiceRequest request = requestRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        if (!request.getStatus().equals(ServiceRequestStatus.PENDING)) {
            throw new IllegalStateException("Only PENDING requests can be declined");
        }
        request.setStatus(ServiceRequestStatus.CANCELLED);
        requestRepo.save(request);
        // notify patient
        messagingTemplate.convertAndSend("/queue/homecare", new HomeCareNotification(request.getId(), "DECLINED"));
        return request;
    }

    @Transactional
    public ServiceRequest startRequest(Long requestId) {
        ServiceProvider provider = getCurrentProvider();
        ServiceRequest request = requestRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        if (!request.getStatus().equals(ServiceRequestStatus.ACCEPTED)) {
            throw new IllegalStateException("Only ACCEPTED requests can be started");
        }
        request.setStatus(ServiceRequestStatus.IN_PROGRESS);
        requestRepo.save(request);
        messagingTemplate.convertAndSend("/queue/homecare", new HomeCareNotification(request.getId(), "IN_PROGRESS"));
        return request;
    }

    @Transactional
    public ServiceRequest completeRequest(Long requestId) {
        ServiceProvider provider = getCurrentProvider();
        ServiceRequest request = requestRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        if (!request.getStatus().equals(ServiceRequestStatus.IN_PROGRESS)) {
            throw new IllegalStateException("Only IN_PROGRESS requests can be completed");
        }
        request.setStatus(ServiceRequestStatus.COMPLETED);
        request.setCompletedAt(LocalDateTime.now());
        requestRepo.save(request);
        messagingTemplate.convertAndSend("/queue/homecare", new HomeCareNotification(request.getId(), "COMPLETED"));
        return request;
    }
}

// Simple DTO for WebSocket notifications
class HomeCareNotification {
    private Long requestId;
    private String event;
    public HomeCareNotification(Long requestId, String event) {
        this.requestId = requestId;
        this.event = event;
    }
    public Long getRequestId() { return requestId; }
    public String getEvent() { return event; }
}
