package edu.bits.itsm.notification;

import edu.bits.itsm.event.IncidentEvent;
import java.util.List;

public interface RecipientResolver {
    List<String> recipientsFor(IncidentEvent event);
}