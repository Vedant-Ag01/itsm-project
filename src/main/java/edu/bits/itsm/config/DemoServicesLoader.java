package edu.bits.itsm.config;

import edu.bits.itsm.domain.BusinessCriticality;
import edu.bits.itsm.service.ServiceCatalog;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// On the very first start, fills the catalogue with the campus services
@Component
public class DemoServicesLoader implements CommandLineRunner {

    private final ServiceCatalog catalog;

    public DemoServicesLoader(ServiceCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public void run(String... args) {
        if (!catalog.allServices().isEmpty()) {
            return;
        }
        add("Campus WiFi", "Wireless network across the campus", BusinessCriticality.HIGH);
        add("VPN Gateway", "Remote access to campus systems", BusinessCriticality.HIGH);
        add("Payroll System", "Salary and payroll processing", BusinessCriticality.HIGH);
        add("Student Portal", "Student records, results and registration", BusinessCriticality.HIGH);
        add("Identity Provider", "Central account and sign-in service", BusinessCriticality.HIGH);
        add("SSO Login", "Single sign-on and password self-service", BusinessCriticality.MEDIUM);
        add("Staff Email", "Staff mailboxes and calendars", BusinessCriticality.MEDIUM);
        add("File Server", "Departmental file shares and backups", BusinessCriticality.MEDIUM);
        add("Cloud Storage", "Research and shared cloud storage", BusinessCriticality.MEDIUM);
        add("Lab Printing", "Shared printers in computer labs", BusinessCriticality.LOW);
        add("Classroom AV", "Projectors and audio-visual equipment", BusinessCriticality.LOW);
    }

    private void add(String name, String description, BusinessCriticality criticality) {
        catalog.create(name, description, criticality);
    }
}