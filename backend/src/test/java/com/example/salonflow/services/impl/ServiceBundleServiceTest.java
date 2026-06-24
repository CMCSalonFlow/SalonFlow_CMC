package com.example.salonflow.services.impl;

import com.example.salonflow.dto.bundle.*;
import com.example.salonflow.entity.Salon;
import com.example.salonflow.entity.Service;
import com.example.salonflow.entity.ServiceBundle;
import com.example.salonflow.entity.ServiceBundleItemId;
import com.example.salonflow.exception.BusinessException;
import com.example.salonflow.repository.SalonRepository;
import com.example.salonflow.repository.ServiceBundleRepository;
import com.example.salonflow.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceBundleServiceTest {

    @Mock
    private ServiceBundleRepository serviceBundleRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private SalonRepository salonRepository;

    @InjectMocks
    private ServiceBundleServiceImpl serviceBundleService;

    private Salon salon;
    private Service service1;
    private Service service2;
    private Service serviceOtherSalon;
    private Service serviceInactive;

    @BeforeEach
    void setUp() {
        salon = Salon.builder().id(1L).name("Salon Test").build();

        service1 = Service.builder()
                .id(11L)
                .salon(salon)
                .name("Haircut")
                .price(BigDecimal.valueOf(100.00))
                .durationMinutes(30)
                .isActive(true)
                .images(new ArrayList<>())
                .build();

        service2 = Service.builder()
                .id(12L)
                .salon(salon)
                .name("Shaving")
                .price(BigDecimal.valueOf(50.00))
                .durationMinutes(15)
                .isActive(true)
                .images(new ArrayList<>())
                .build();

        Salon salonOther = Salon.builder().id(2L).name("Other Salon").build();
        serviceOtherSalon = Service.builder()
                .id(13L)
                .salon(salonOther)
                .name("Other Haircut")
                .price(BigDecimal.valueOf(100.00))
                .durationMinutes(30)
                .isActive(true)
                .images(new ArrayList<>())
                .build();

        serviceInactive = Service.builder()
                .id(14L)
                .salon(salon)
                .name("Inactive Massage")
                .price(BigDecimal.valueOf(200.00))
                .durationMinutes(60)
                .isActive(false)
                .images(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("🚫 Create combo with < 2 services -> throw BusinessException")
    void create_withLessThanTwoServices_shouldThrowBusinessException() {
        CreateBundleRequest request = CreateBundleRequest.builder()
                .name("Short Combo")
                .price(BigDecimal.valueOf(120.00))
                .items(List.of(BundleItemRequest.builder().serviceId(11L).displayOrder(1).build()))
                .build();

        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));

        assertThatThrownBy(() -> serviceBundleService.create(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Combo/gói dịch vụ phải có ít nhất 2 dịch vụ khác nhau");
    }

    @Test
    @DisplayName("🚫 Create combo containing service of another salon -> throw BusinessException")
    void create_withServiceBelongsToAnotherSalon_shouldThrowBusinessException() {
        CreateBundleRequest request = CreateBundleRequest.builder()
                .name("Mixed Combo")
                .price(BigDecimal.valueOf(120.00))
                .items(List.of(
                        BundleItemRequest.builder().serviceId(11L).build(),
                        BundleItemRequest.builder().serviceId(13L).build()
                ))
                .build();

        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));
        when(serviceRepository.findById(11L)).thenReturn(Optional.of(service1));
        when(serviceRepository.findById(13L)).thenReturn(Optional.of(serviceOtherSalon));

        ServiceBundle mockBundle = ServiceBundle.builder().id(100L).salon(salon).name("Mixed Combo").price(BigDecimal.valueOf(120.00)).build();
        when(serviceBundleRepository.saveAndFlush(any(ServiceBundle.class))).thenReturn(mockBundle);

        assertThatThrownBy(() -> serviceBundleService.create(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("does not belong to salon 1");
    }

    @Test
    @DisplayName("🚫 Create combo containing inactive service -> throw BusinessException")
    void create_withInactiveService_shouldThrowBusinessException() {
        CreateBundleRequest request = CreateBundleRequest.builder()
                .name("Inactive Combo")
                .price(BigDecimal.valueOf(120.00))
                .items(List.of(
                        BundleItemRequest.builder().serviceId(11L).build(),
                        BundleItemRequest.builder().serviceId(14L).build()
                ))
                .build();

        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));
        when(serviceRepository.findById(11L)).thenReturn(Optional.of(service1));
        when(serviceRepository.findById(14L)).thenReturn(Optional.of(serviceInactive));

        ServiceBundle mockBundle = ServiceBundle.builder().id(100L).salon(salon).name("Inactive Combo").price(BigDecimal.valueOf(120.00)).build();
        when(serviceBundleRepository.saveAndFlush(any(ServiceBundle.class))).thenReturn(mockBundle);

        assertThatThrownBy(() -> serviceBundleService.create(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("is inactive");
    }

    @Test
    @DisplayName("✅ Create combo successfully with valid inputs and reload trigger fields")
    void create_withValidRequest_shouldSaveAndReturnResponse() {
        CreateBundleRequest request = CreateBundleRequest.builder()
                .name("Super Combo")
                .description("Super combo description")
                .price(BigDecimal.valueOf(120.00))
                .items(List.of(
                        BundleItemRequest.builder().serviceId(11L).displayOrder(1).build(),
                        BundleItemRequest.builder().serviceId(12L).displayOrder(2).build()
                ))
                .build();

        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));
        when(serviceRepository.findById(11L)).thenReturn(Optional.of(service1));
        when(serviceRepository.findById(12L)).thenReturn(Optional.of(service2));

        when(serviceBundleRepository.saveAndFlush(any(ServiceBundle.class))).thenAnswer(invocation -> {
            ServiceBundle b = invocation.getArgument(0);
            b.setId(100L);
            return b;
        });

        ServiceBundle reloadedBundle = ServiceBundle.builder()
                .id(100L)
                .salon(salon)
                .name("Super Combo")
                .description("Super combo description")
                .price(BigDecimal.valueOf(120.00))
                .originalPrice(BigDecimal.valueOf(150.00))
                .totalDurationMinutes(45)
                .isActive(true)
                .items(new ArrayList<>())
                .build();

        reloadedBundle.getItems().add(com.example.salonflow.entity.ServiceBundleItem.builder()
                .id(new ServiceBundleItemId(100L, 11L))
                .bundle(reloadedBundle)
                .service(service1)
                .displayOrder(1)
                .build());
        reloadedBundle.getItems().add(com.example.salonflow.entity.ServiceBundleItem.builder()
                .id(new ServiceBundleItemId(100L, 12L))
                .bundle(reloadedBundle)
                .service(service2)
                .displayOrder(2)
                .build());

        when(serviceBundleRepository.findByIdAndSalonId(100L, 1L)).thenReturn(Optional.of(reloadedBundle));

        BundleResponse response = serviceBundleService.create(1L, request);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getName()).isEqualTo("Super Combo");
        assertThat(response.getPrice()).isEqualByComparingTo("120.00");
        assertThat(response.getOriginalPrice()).isEqualByComparingTo("150.00");
        assertThat(response.getTotalDurationMinutes()).isEqualTo(45);
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems().get(0).getServiceId()).isEqualTo(11L);
        assertThat(response.getItems().get(1).getServiceId()).isEqualTo(12L);
    }
}
