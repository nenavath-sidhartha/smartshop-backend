package com.smartshop.backend.repository;

import com.smartshop.backend.entity.DeliveryLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryLocationRepository extends JpaRepository<DeliveryLocation, Long> {

    Optional<DeliveryLocation> findFirstByDeliveryAssignmentIdOrderByRecordedAtDesc(Long assignmentId);

    List<DeliveryLocation> findTop20ByDeliveryAssignmentIdOrderByRecordedAtDesc(Long assignmentId);

    void deleteByDeliveryAssignmentId(Long assignmentId);
}
