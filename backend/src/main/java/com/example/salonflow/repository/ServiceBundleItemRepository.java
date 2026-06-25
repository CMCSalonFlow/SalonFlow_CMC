package com.example.salonflow.repository;

import com.example.salonflow.entity.ServiceBundleItem;
import com.example.salonflow.entity.ServiceBundleItemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceBundleItemRepository extends JpaRepository<ServiceBundleItem, ServiceBundleItemId> {

    List<ServiceBundleItem> findByBundleId(Long bundleId);
}
