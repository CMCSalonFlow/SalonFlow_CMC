package com.example.salonflow.repository;

import com.example.salonflow.entity.RolePermission;
import com.example.salonflow.entity.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolePermissionRepository
        extends JpaRepository<RolePermission, RolePermissionId> {

    List<RolePermission> findByRole_Id(Long roleId);

    List<RolePermission> findByPermission_Id(Long permissionId);
}