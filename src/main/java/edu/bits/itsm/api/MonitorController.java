package edu.bits.itsm.api;

import edu.bits.itsm.auth.AllowedRoles;
import edu.bits.itsm.auth.Role;
import edu.bits.itsm.service.SlaEscalationJob;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MonitorController {

    private final SlaEscalationJob job;

    public MonitorController(SlaEscalationJob job) {
        this.job = job;
    }

    // Runs the SLA and escalation check right now (handy for the demo)
    @PostMapping("/api/admin/monitor/run")
    @AllowedRoles({Role.ADMIN})
    public SlaEscalationJob.RunResult runNow() {
        return job.runOnce();
    }
}