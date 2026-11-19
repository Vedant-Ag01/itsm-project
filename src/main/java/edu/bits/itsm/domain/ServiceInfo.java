package edu.bits.itsm.domain;

// A service in the catalogue that employees can report problems against
public record ServiceInfo(Long id, String name, String description,
                          BusinessCriticality criticality, boolean active) {

    public ServiceInfo {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Service name cannot be empty");
        }
        if (criticality == null) {
            throw new IllegalArgumentException("Service must have a business criticality");
        }
        name = name.trim();
        if (description == null) {
            description = "";
        }
    }
}