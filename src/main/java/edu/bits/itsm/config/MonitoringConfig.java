package edu.bits.itsm.config;

import edu.bits.itsm.escalation.EscalationService;
import edu.bits.itsm.escalation.TimeBasedEscalationPolicy;
import edu.bits.itsm.event.IncidentEventPublisher;
import edu.bits.itsm.service.IncidentService;
import edu.bits.itsm.service.SlaEscalationJob;
import edu.bits.itsm.sla.SlaCalculator;
import edu.bits.itsm.sla.SlaMonitor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class MonitoringConfig {

    @Bean
    public SlaMonitor slaMonitor(SlaCalculator calculator) {
        return new SlaMonitor(calculator);
    }

    @Bean
    public EscalationService escalationService() {
        return new EscalationService(new TimeBasedEscalationPolicy());
    }

    @Bean
    public SlaEscalationJob slaEscalationJob(IncidentService incidentService,
                                             SlaMonitor slaMonitor,
                                             EscalationService escalationService,
                                             IncidentEventPublisher publisher) {
        return new SlaEscalationJob(incidentService, slaMonitor, escalationService, publisher);
    }
}