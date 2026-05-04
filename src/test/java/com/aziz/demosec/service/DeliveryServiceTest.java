package com.aziz.demosec.service;

import com.aziz.demosec.Entities.*;
import com.aziz.demosec.repository.DeliveryAgencyRepository;
import com.aziz.demosec.repository.DeliveryAgentRepository;
import com.aziz.demosec.repository.DeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock private DeliveryRepository deliveryRepository;
    @Mock private PharmacyOrderRepository orderRepository;
    @Mock private DeliveryAgencyRepository agencyRepository;
    @Mock private DeliveryAgentRepository agentRepository;
    @Mock private NotificationService notificationService;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private DeliveryServiceImpl deliveryService;

    private PharmacyOrder order;
    private Delivery delivery;
    private DeliveryAgent agent;
    private DeliveryAgency agency;

    @BeforeEach
    void setUp() {
        order = new PharmacyOrder();
        order.setId(1L);
        order.setStatus(PharmacyOrderStatus.PAID);

        delivery = Delivery.builder()
                .id(10L)
                .order(order)
                .status(DeliveryStatus.PENDING)
                .trackingNumber("TRK-12345")
                .build();

        agency = new DeliveryAgency();
        agency.setId(5L);
        agency.setName("Fast-Delivery");

        agent = new DeliveryAgent();
        agent.setId(20L);
        agent.setName("John Doe");
        agent.setPhoneNumber("12345678");
        agent.setAgency(agency);
    }

    @Test
    void createDelivery_ShouldCreateNew_WhenNotExists() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrderId(1L)).thenReturn(Optional.empty());
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Delivery result = deliveryService.createDelivery(1L);

        // Assert
        assertNotNull(result);
        assertEquals(order, result.getOrder());
        assertEquals(DeliveryStatus.PENDING, result.getStatus());
        verify(notificationService).notifyDeliveryCreated(any(Delivery.class));
    }

    @Test
    void assignAgent_ShouldUpdateDeliveryAndOrderStatus() {
        // Arrange
        when(deliveryRepository.findById(10L)).thenReturn(Optional.of(delivery));
        when(agentRepository.findById(20L)).thenReturn(Optional.of(agent));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Delivery result = deliveryService.assignAgent(10L, 20L);

        // Assert
        assertEquals(agent, result.getAgent());
        assertEquals(DeliveryStatus.REQUESTED, result.getStatus());
        assertEquals(PharmacyOrderStatus.ASSIGNED, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void updateDeliveryStatus_ShouldChangeStatusAndNotify() {
        // Arrange
        when(deliveryRepository.findById(10L)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Delivery result = deliveryService.updateDeliveryStatus(10L, DeliveryStatus.PICKED_UP);

        // Assert
        assertEquals(DeliveryStatus.PICKED_UP, result.getStatus());
        verify(notificationService).notifyDeliveryStatusUpdate(any(Delivery.class));
    }
}
