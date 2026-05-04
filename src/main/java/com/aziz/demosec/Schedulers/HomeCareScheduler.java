package com.aziz.demosec.Schedulers;

import com.aziz.demosec.Entities.ServiceRequest;
import com.aziz.demosec.Entities.ServiceRequestStatus;
import com.aziz.demosec.repository.ServiceRequestRepository;
import com.aziz.demosec.service.HomeCareAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class HomeCareScheduler {

    private final ServiceRequestRepository requestRepository;
    private final HomeCareAssignmentService assignmentService;

    /**
     * Every 15 minutes, try to auto-assign PENDING requests that are still unassigned
     * or could not be assigned at creation time.
     */
    @Scheduled(fixedDelay = 900000) // 15 minutes
    public void processPendingRequests() {
        log.info("Running HomeCare Scheduler: checking pending requests...");
        
        List<ServiceRequest> pendingRequests = requestRepository.findByStatusOrderByCreatedAtDesc(ServiceRequestStatus.PENDING);
        
        for (ServiceRequest request : pendingRequests) {
            // If the requested date is in the past, cancel the request
            if (request.getRequestedDateTime().isBefore(LocalDateTime.now().minusHours(1))) {
                log.warn("Cancelling expired request #{}", request.getId());
                request.setStatus(ServiceRequestStatus.CANCELLED);
                request.setProviderNotes("Expired - No provider available in time");
                requestRepository.save(request);
                continue;
            }

            // Try to auto-assign if no provider is currently assigned
            if (request.getAssignedProvider() == null) {
                try {
                    assignmentService.autoAssign(request.getId());
                    log.info("Auto-assigned request #{} via scheduler", request.getId());
                } catch (Exception e) {
                    log.debug("Could not auto-assign request #{} yet: {}", request.getId(), e.getMessage());
                }
            }
        }
    }
}
