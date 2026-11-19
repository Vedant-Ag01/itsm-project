package edu.bits.itsm.api;

import edu.bits.itsm.auth.AllowedRoles;
import edu.bits.itsm.auth.Role;
import edu.bits.itsm.domain.Priority;
import edu.bits.itsm.persistence.PersistentSlaPolicy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class SlaPolicyController {

    public record SlaPolicyResponse(String priority, long responseMinutes, long resolutionMinutes) { }

    public record UpdateSlaPolicyRequest(long responseMinutes, long resolutionMinutes) { }

    private final PersistentSlaPolicy policy;

    public SlaPolicyController(PersistentSlaPolicy policy) {
        this.policy = policy;
    }

    // Any logged-in user may see the targets
    @GetMapping("/api/sla-policies")
    public List<SlaPolicyResponse> list() {
        List<SlaPolicyResponse> result = new ArrayList<>();
        for (PersistentSlaPolicy.Row row : policy.all()) {
            result.add(toResponse(row));
        }
        return result;
    }

    @PutMapping("/api/admin/sla-policies/{priority}")
    @AllowedRoles({Role.ADMIN})
    public SlaPolicyResponse update(@PathVariable String priority,
                                    @RequestBody UpdateSlaPolicyRequest request) {
        Priority parsed;
        try {
            parsed = Priority.valueOf(priority.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown priority '" + priority
                    + "'. Use P1, P2, P3 or P4");
        }
        return toResponse(policy.update(parsed, request.responseMinutes(),
                request.resolutionMinutes()));
    }

    private SlaPolicyResponse toResponse(PersistentSlaPolicy.Row row) {
        return new SlaPolicyResponse(row.priority().name(),
                row.response().toMinutes(), row.resolution().toMinutes());
    }
}
