package com.smartshop.backend.repository;

import com.smartshop.backend.entity.DeliveryAssignment;
import com.smartshop.backend.entity.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, Long> {

    Optional<DeliveryAssignment> findByOrderId(Long orderId);

    List<DeliveryAssignment> findByDeliveryAgentIdOrderByAssignedAtDesc(Long agentId);

    List<DeliveryAssignment> findByDeliveryAgentIdAndDeliveryStatusNotIn(Long agentId, List<DeliveryStatus> statuses);

    List<DeliveryAssignment> findAllByOrderByAssignedAtDesc();

    boolean existsByOrderId(Long orderId);
}
