package com.bank.prescreener.web;

import com.bank.prescreener.model.Screening;
import com.bank.prescreener.repo.ScreeningRepository;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in agent's screening history + summary stats. */
@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final ScreeningRepository repo;

    public HistoryController(ScreeningRepository repo) {
        this.repo = repo;
    }

    public record Stats(long total, long eligible, long borderline, long notEligible,
                        int approvalRate, double avgFoir) {
    }

    public record HistoryResponse(Stats stats, List<Screening> screenings) {
    }

    @GetMapping
    public HistoryResponse history(Principal principal) {
        String agent = principal.getName();
        List<Screening> recent = repo.findTop100ByAgentEmailOrderByCreatedAtDesc(agent);

        long total = repo.countByAgentEmail(agent);
        long eligible = repo.countByAgentEmailAndVerdict(agent, "ELIGIBLE");
        long borderline = repo.countByAgentEmailAndVerdict(agent, "BORDERLINE");
        long notEligible = repo.countByAgentEmailAndVerdict(agent, "NOT_ELIGIBLE");
        int approvalRate = total == 0 ? 0 : (int) Math.round(100.0 * eligible / total);
        Double avg = repo.avgFoirByAgent(agent);
        double avgFoir = avg == null ? 0.0 : Math.round(avg * 10.0) / 10.0;

        return new HistoryResponse(
                new Stats(total, eligible, borderline, notEligible, approvalRate, avgFoir), recent);
    }
}
