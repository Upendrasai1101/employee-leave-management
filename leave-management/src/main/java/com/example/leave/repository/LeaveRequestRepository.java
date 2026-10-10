package com.example.leave.repository;

import com.example.leave.entity.LeaveRequest;
import com.example.leave.entity.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByEmployeeIdOrderByAppliedOnDescIdDesc(Long employeeId);

    List<LeaveRequest> findByEmployeeIdInOrderByAppliedOnDescIdDesc(Collection<Long> employeeIds);

    /**
     * True if the employee already has a PENDING/APPROVED request overlapping [start, end].
     * Two ranges overlap when existing.start <= end AND existing.end >= start.
     */
    @Query("""
            SELECT COUNT(l) > 0 FROM LeaveRequest l
            WHERE l.employeeId = :employeeId
              AND l.status IN :statuses
              AND l.startDate <= :endDate
              AND l.endDate >= :startDate
            """)
    boolean existsOverlapping(@Param("employeeId") Long employeeId,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate,
                              @Param("statuses") Collection<LeaveStatus> statuses);
}
