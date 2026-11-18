package edu.bits.itsm.api;

import edu.bits.itsm.auth.ForbiddenException;
import edu.bits.itsm.auth.Role;
import edu.bits.itsm.auth.UserAccount;
import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.Technician;

// The rule for who may see an incident: admins all, employees their own,
// technicians the ones assigned to them.
public class IncidentAccess {

    public static boolean canSee(UserAccount user, Incident incident) {
        if (user.role == Role.ADMIN) {
            return true;
        }
        if (user.role == Role.EMPLOYEE) {
            return incident.getReporter().getId().equals(user.id);
        }
        Technician assigned = incident.getAssignedTechnician();
        return assigned != null && assigned.getId().equals(user.id);
    }

    public static void requireCanSee(UserAccount user, Incident incident) {
        if (!canSee(user, incident)) {
            throw new ForbiddenException("You are not allowed to see this incident");
        }
    }
}