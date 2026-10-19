package edu.bits.itsm;

import edu.bits.itsm.domain.Administrator;
import edu.bits.itsm.domain.Employee;
import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.IncidentCategory;
import edu.bits.itsm.domain.Priority;
import edu.bits.itsm.domain.Technician;
import edu.bits.itsm.domain.User;

import java.util.List;

public class DomainDemo {

    public static void main(String[] args) {
        Employee employee = new Employee("E102", "Asha", "asha@company.com", "Finance");
        Technician technician = new Technician("T205", "Ravi", "ravi@company.com");
        Administrator admin = new Administrator("A001", "Meera", "meera@company.com");
        technician.addSkill(IncidentCategory.APPLICATION);

        // Polymorphism: one list of User, each object answers in its own way
        List<User> users = List.of(employee, technician, admin);
        for (User user : users) {
            System.out.println(user.getName() + " -> " + user.getRoleName());
        }

        // Create an incident and move it through classification, priority and assignment
        Incident incident = new Incident("INC1024", employee, "Payment application unavailable");
        System.out.println("Status at creation: " + incident.getStatus());

        incident.setCategory(IncidentCategory.APPLICATION);
        incident.setPriority(Priority.P1);
        incident.setAssignedTechnician(technician);

        System.out.println("Priority: " + incident.getPriority() + " (" + incident.getPriority().getLabel() + ")");
        System.out.println("Reported by: " + incident.getReporter().getName());
        System.out.println("Assigned to: " + incident.getAssignedTechnician().getName());
        System.out.println("Technician has APPLICATION skill: "
                + technician.hasSkill(IncidentCategory.APPLICATION));

        // Validation: an invalid incident must be rejected
        try {
            new Incident("INC2000", employee, "   ");
        } catch (IllegalArgumentException e) {
            System.out.println("Rejected: " + e.getMessage());
        }
    }
}