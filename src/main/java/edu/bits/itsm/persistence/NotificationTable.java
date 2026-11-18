package edu.bits.itsm.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationTable extends JpaRepository<NotificationEntity, Long> {

    List<NotificationEntity> findByRecipientOrderByIdDesc(String recipient);

    List<NotificationEntity> findByRecipientAndReadFalse(String recipient);

    long countByRecipientAndReadFalse(String recipient);

    Optional<NotificationEntity> findByIdAndRecipient(Long id, String recipient);
}