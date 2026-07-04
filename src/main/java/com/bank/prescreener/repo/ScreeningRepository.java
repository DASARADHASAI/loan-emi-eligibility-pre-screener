package com.bank.prescreener.repo;

import com.bank.prescreener.model.Screening;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ScreeningRepository extends JpaRepository<Screening, UUID> {

    List<Screening> findTop100ByAgentEmailOrderByCreatedAtDesc(String agentEmail);

    long countByAgentEmail(String agentEmail);

    long countByAgentEmailAndVerdict(String agentEmail, String verdict);

    @Query("select avg(s.foir) from Screening s where s.agentEmail = ?1")
    Double avgFoirByAgent(String agentEmail);
}
