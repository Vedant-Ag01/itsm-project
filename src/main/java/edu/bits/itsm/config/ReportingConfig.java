package edu.bits.itsm.config;

import edu.bits.itsm.reporting.ReportService;
import edu.bits.itsm.search.IncidentSearchService;
import edu.bits.itsm.sla.DefaultSlaCalculator;
import edu.bits.itsm.sla.DefaultSlaPolicy;
import edu.bits.itsm.sla.SlaCalculator;
import edu.bits.itsm.sla.SlaPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// B's SLA pieces and C's reporting and search pieces, made available to the controllers
@Configuration
public class ReportingConfig {

    @Bean
    public SlaPolicy slaPolicy() {
        return new DefaultSlaPolicy();
    }

    @Bean
    public SlaCalculator slaCalculator(SlaPolicy policy) {
        return new DefaultSlaCalculator(policy);
    }

    @Bean
    public ReportService reportService() {
        return new ReportService();
    }

    @Bean
    public IncidentSearchService incidentSearchService() {
        return new IncidentSearchService();
    }
}