package edu.bits.itsm.domain;

// An IT service that can fail, such as "Payment application" or "VPN".
// Its business criticality is what feeds the priority calculation later.
public class ItService {

    private final String name;
    private final BusinessCriticality criticality;

    public ItService(String name, BusinessCriticality criticality) {
        if (name == null || name.isBlank()) {
            throw new InvalidIncidentException("Service name cannot be empty");
        }
        if (criticality == null) {
            throw new InvalidIncidentException("Service must have a business criticality");
        }
        this.name = name;
        this.criticality = criticality;
    }

    public String getName() {
        return name;
    }

    public BusinessCriticality getCriticality() {
        return criticality;
    }
}