package edu.bits.itsm.search;

import edu.bits.itsm.domain.Incident;

import java.time.LocalDate;
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

        if (isSet(criteria.getIncidentId())) {
            predicates.add(inc -> inc.getId() != null &&
                    inc.getId().equalsIgnoreCase(criteria.getIncidentId().trim()));
        }

        if (isSet(criteria.getQuery())) {
            String q = criteria.getQuery().trim().toLowerCase();
            predicates.add(inc ->
                    (inc.getId() != null && inc.getId().toLowerCase().contains(q)) ||
                    (inc.getTitle() != null && inc.getTitle().toLowerCase().contains(q)) ||
                    (inc.getDescription() != null && inc.getDescription().toLowerCase().contains(q))
            );
        }

        if (isSet(criteria.getStatus())) {
            predicates.add(inc -> String.valueOf(inc.getStatus()).equalsIgnoreCase(criteria.getStatus().trim()));
        }

        if (isSet(criteria.getPriority())) {
            predicates.add(inc -> String.valueOf(inc.getPriority()).equalsIgnoreCase(criteria.getPriority().trim()));
        }

        if (isSet(criteria.getCategory())) {
            predicates.add(inc -> String.valueOf(inc.getCategory()).equalsIgnoreCase(criteria.getCategory().trim()));
        }

        if (isSet(criteria.getService())) {
            predicates.add(inc -> inc.getService() != null &&
                    inc.getService().getName() != null &&
                    inc.getService().getName().equalsIgnoreCase(criteria.getService().trim()));
        }

        if (isSet(criteria.getTechnicianId())) {
            predicates.add(inc -> inc.getAssignedTechnician() != null &&
                    inc.getAssignedTechnician().getId() != null &&
                    inc.getAssignedTechnician().getId().equalsIgnoreCase(criteria.getTechnicianId().trim()));
        }

        if (criteria.getFromDate() != null) {
            predicates.add(inc -> {
                if (inc.getCreatedAt() == null) return false;
                LocalDate createdDate = inc.getCreatedAt().toLocalDate();
                return !createdDate.isBefore(criteria.getFromDate());
            });
        }

        if (criteria.getToDate() != null) {
            predicates.add(inc -> {
                if (inc.getCreatedAt() == null) return false;
                LocalDate createdDate = inc.getCreatedAt().toLocalDate();
                return !createdDate.isAfter(criteria.getToDate());
            });
        }

        if (isSet(criteria.getSlaStatus()) && slaEvaluator != null) {
            predicates.add(inc -> {
                String evaluatedSla = slaEvaluator.apply(inc);
                return evaluatedSla != null && evaluatedSla.equalsIgnoreCase(criteria.getSlaStatus().trim());
            });
        }

        Predicate<Incident> compositePredicate = predicates.stream()
                .reduce(Predicate::and)
                .orElse(inc -> true);

        return incidents.stream()
                .filter(compositePredicate)
                .toList();
    }

    private boolean isSet(String value) {
        return value != null && !value.isBlank();
    }
}
