package com.aziz.demosec.service;

import com.aziz.demosec.Entities.PharmacyOrder;
import com.aziz.demosec.Entities.PharmacyOrderStatus;
import com.aziz.demosec.repository.PharmacyOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PharmacyOrderScheduler {

    private final PharmacyOrderRepository orderRepository;
    private final PharmacyOrderService orderService;

    // Task 1 — Cancel expired PENDING orders every night at 2am
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cancelExpiredPendingOrders() {
        LocalDateTime expirationThreshold = LocalDateTime.now().minusHours(48);

        List<PharmacyOrder> expiredOrders = orderRepository
                .findByStatusAndCreatedAtBefore(PharmacyOrderStatus.PENDING, expirationThreshold);

        for (PharmacyOrder order : expiredOrders) {
            order.setStatus(PharmacyOrderStatus.CANCELLED);
            order.setPharmacistNote("Auto-cancelled: no patient action within 48h.");
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
            log.info("Order #{} auto-cancelled — pending since {}", order.getId(), order.getCreatedAt());
        }

        log.info("[Scheduler] cancelExpiredPendingOrders: {} order(s) cancelled.", expiredOrders.size());
    }

    // Advanced — Auto-escalate PENDING > 1h → REVIEWING every 2 minutes (demo interval)
    @Scheduled(cron = "0 */2 * * * ?")
    public void autoEscalateStalledOrders() {
        int count = orderService.escalateStalledOrders();
        log.info("[Scheduler] autoEscalateStalledOrders: {} order(s) escalated PENDING → REVIEWING.", count);
    }
}
