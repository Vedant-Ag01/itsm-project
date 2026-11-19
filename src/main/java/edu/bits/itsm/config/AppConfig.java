package edu.bits.itsm.config;

import edu.bits.itsm.assignment.SkillBasedStrategy;
import edu.bits.itsm.audit.AuditLogObserver;
import edu.bits.itsm.audit.AuditRepository;
import edu.bits.itsm.domain.IncidentCategory;
import edu.bits.itsm.domain.Technician;
import edu.bits.itsm.duplicate.SimilarityDuplicateDetector;
import edu.bits.itsm.event.IncidentEventPublisher;
import edu.bits.itsm.event.IncidentObserver;
import edu.bits.itsm.notification.DeduplicatingChannel;
import edu.bits.itsm.notification.NotificationObserver;
import edu.bits.itsm.notification.SimulatedEmailChannel;
import edu.bits.itsm.notification.TelegramChannel;
import edu.bits.itsm.persistence.AuditTable;
import edu.bits.itsm.persistence.IncidentMapper;
import edu.bits.itsm.persistence.IncidentTable;
import edu.bits.itsm.persistence.JpaAuditRepository;
import edu.bits.itsm.persistence.JpaIncidentRepository;
import edu.bits.itsm.persistence.NotificationTable;
import edu.bits.itsm.persistence.PersistentInAppChannel;
import edu.bits.itsm.priority.WeightedPriorityStrategy;
import edu.bits.itsm.repository.IncidentRepository;
import edu.bits.itsm.service.IncidentFactory;
import edu.bits.itsm.service.IncidentRecipientResolver;
import edu.bits.itsm.service.IncidentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class AppConfig {

    @Bean
    public List<Technician> technicians() {
        List<Technician> list = new ArrayList<>();

        Technician ravi = new Technician("T1", "Ravi", "ravi@bits.edu");
        ravi.addSkill(IncidentCategory.NETWORK);
        ravi.addSkill(IncidentCategory.HARDWARE);
        ravi.addSkill(IncidentCategory.SECURITY);
        ravi.addSkill(IncidentCategory.STORAGE);
        ravi.addSkill(IncidentCategory.AUTHENTICATION);

        Technician meera = new Technician("T2", "Meera", "meera@bits.edu");
        meera.addSkill(IncidentCategory.SOFTWARE);
        meera.addSkill(IncidentCategory.DATABASE);
        meera.addSkill(IncidentCategory.CLOUD);
        meera.addSkill(IncidentCategory.APPLICATION);
        meera.addSkill(IncidentCategory.AUTHENTICATION);

        list.add(ravi);
        list.add(meera);
        return list;
    }

    @Bean
    public IncidentMapper incidentMapper(List<Technician> technicians) {
        return new IncidentMapper(technicians);
    }

    @Bean
    public IncidentRepository incidentRepository(IncidentTable jpa,
                                                 IncidentMapper mapper) {
        return new JpaIncidentRepository(jpa, mapper);
    }

    // ---------- audit and notifications, now saved in PostgreSQL ----------

    @Bean
    public AuditRepository auditRepository(AuditTable table) {
        return new JpaAuditRepository(table);
    }

    @Bean
    public PersistentInAppChannel inAppChannel(NotificationTable table) {
        return new PersistentInAppChannel(table);
    }

    @Bean
    public SimulatedEmailChannel emailChannel() {
        return new SimulatedEmailChannel();
    }

    @Bean
    public IncidentEventPublisher incidentEventPublisher(AuditRepository auditRepository,
                                                         PersistentInAppChannel inAppChannel,
                                                         SimulatedEmailChannel emailChannel,
                                                         IncidentRepository repository,
                                                         @Value("${TELEGRAM_BOT_TOKEN:}") String telegramToken,
                                                         @Value("${TELEGRAM_CHAT_ID:}") String telegramChatId) {
        IncidentEventPublisher publisher = new IncidentEventPublisher();

        publisher.subscribe(new AuditLogObserver(auditRepository));

        NotificationObserver notifications = new NotificationObserver(
                new IncidentRecipientResolver(repository, List.of("admin@bits.edu")));
        notifications.addChannel(inAppChannel);
        notifications.addChannel(emailChannel);

        // Telegram is optional: it is switched on only when both environment variables are set.
        // The wrapper makes sure one event is posted once, not once per recipient.
        if (!telegramToken.isBlank() && !telegramChatId.isBlank()) {
            try {
                notifications.addChannel(
                        new DeduplicatingChannel(new TelegramChannel(telegramToken, telegramChatId)));
                System.out.println("Telegram notifications are ON");
            } catch (IllegalArgumentException e) {
                System.err.println("Telegram notifications are OFF: " + e.getMessage());
            }
        } else {
            System.out.println("Telegram notifications are OFF "
                    + "(set TELEGRAM_BOT_TOKEN and TELEGRAM_CHAT_ID to turn them on)");
        }
        publisher.subscribe(notifications);

        return publisher;
    }

    // ---------- the service ----------

    @Bean
    public IncidentService incidentService(IncidentRepository repository,
                                           List<Technician> technicians,
                                           IncidentEventPublisher publisher) {
        // The service talks to one observer; the publisher fans out to the rest.
        List<IncidentObserver> observers = new ArrayList<>();
        observers.add(publisher::publish);

        return new IncidentService(
                repository,
                new IncidentFactory(repository),
                new WeightedPriorityStrategy(),
                new SkillBasedStrategy(),
                new SimilarityDuplicateDetector(),
                observers,
                technicians);
    }
}