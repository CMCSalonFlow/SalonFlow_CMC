package com.example.salonflow.repository;

import com.example.salonflow.entity.UserRole;
import com.example.salonflow.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

List<UserRole> findByUser_Id(Long userId);

List<UserRole> findByRole_Id(Long roleId);

void deleteByUser_Id(Long userId);

void deleteByRole_Id(Long roleId);
}