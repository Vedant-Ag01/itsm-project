package edu.bits.itsm.search;

import edu.bits.itsm.domain.Incident;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class IncidentSearchService {

    public List<Incident> search(List<Incident> incidents, 
                                 IncidentSearchCriteria criteria, 
                                 Function<Incident, String> slaEvaluator) {
        if (incidents == null || incidents.isEmpty()) {
            return List.of();
        }
        if (criteria == null) {
            return new ArrayList<>(incidents);
        }

        List<Predicate<Incident>> predicates = new ArrayList<>();

        if (criteria.getIncidentId() != null && !criteria.getIncidentId().isBlank()) {
            predicates.add(inc -> inc.getId().equalsIgnoreCase(criteria.getIncidentId()));
        }

        if (criteria.getStatus() != null && !criteria.getStatus().isBlank()) {
            predicates.add(inc -> String.valueOf(inc.getStatus()).equalsIgnoreCase(criteria.getStatus()));
        }

        if (criteria.getCategory() != null && !criteria.getCategory().isBlank()) {
            predicates.add(inc -> inc.getCategory() != null && 
                    inc.getCategory().equalsIgnoreCase(criteria.getCategory()));
        }

        if (criteria.getSlaStatus() != null && !criteria.getSlaStatus().isBlank() && slaEvaluator != null) {
            predicates.add(inc -> {
                String evaluatedSla = slaEvaluator.apply(inc);
                return evaluatedSla != null && evaluatedSla.equalsIgnoreCase(criteria.getSlaStatus());
            });
        }

        // Combine all predicates into one using logical AND
        Predicate<Incident> compositePredicate = predicates.stream()
                .reduce(Predicate::and)
                .orElse(inc -> true); // If no criteria set, match all

        return incidents.stream()
                .filter(compositePredicate)
                .toList();
    }
}