package edu.bits.itsm.domain;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class Technician extends User {

    private final Set<IncidentCategory> skills = EnumSet.noneOf(IncidentCategory.class);
    private boolean available = true;

    public Technician(String id, String name, String email) {
        super(id, name, email);
    }

    @Override
    public String getRoleName() {
        return "TECHNICIAN";
    }

    public void addSkill(IncidentCategory category) {
        skills.add(category);
    }

    public boolean hasSkill(IncidentCategory category) {
        return skills.contains(category);
    }

    public Set<IncidentCategory> getSkills() {
        return Collections.unmodifiableSet(skills);
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}