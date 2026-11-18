package edu.bits.itsm.config;

import edu.bits.itsm.persistence.PersistentSlaPolicy;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Makes sure the SLA table has the standard targets on first start
@Component
public class DemoSlaLoader implements CommandLineRunner {

    private final PersistentSlaPolicy policy;

    public DemoSlaLoader(PersistentSlaPolicy policy) {
        this.policy = policy;
    }

    @Override
    public void run(String... args) {
        policy.seedDefaultsIfEmpty();
    }
}
