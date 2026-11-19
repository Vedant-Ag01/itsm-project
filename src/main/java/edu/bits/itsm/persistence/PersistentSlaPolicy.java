package edu.bits.itsm.persistence;

import edu.bits.itsm.domain.Priority;
import edu.bits.itsm.domain.SlaConfigurationMissingException;
import edu.bits.itsm.sla.DefaultSlaPolicy;
import edu.bits.itsm.sla.SlaPolicy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// An SlaPolicy whose targets live in the database, so admins can change them.
// A small in-memory copy keeps lookups fast; it is refreshed on every update.
public class PersistentSlaPolicy implements SlaPolicy {

    public record Row(Priority priority, Duration response, Duration resolution) { }

    private final SlaPolicyTable table;
    private final Map<Priority, Row> cache = new ConcurrentHashMap<>();

    public PersistentSlaPolicy(SlaPolicyTable table) {
        this.table = table;
    }

    // On the very first start, fill the table with the standard targets
    public void seedDefaultsIfEmpty() {
        if (table.count() > 0) {
            return;
        }
        DefaultSlaPolicy defaults = new DefaultSlaPolicy();
        for (Priority priority : Priority.values()) {
            save(priority, defaults.responseTarget(priority), defaults.resolutionTarget(priority));
        }
    }

    public List<Row> all() {
        List<Row> rows = new ArrayList<>();
        for (SlaPolicyEntity entity : table.findAll()) {
            rows.add(toRow(entity));
        }
        rows.sort((a, b) -> a.priority().compareTo(b.priority()));
        return rows;
    }

    // Admin edit. Refuses nonsense before anything is saved.
    public Row update(Priority priority, long responseMinutes, long resolutionMinutes) {
        if (responseMinutes <= 0 || resolutionMinutes <= 0) {
            throw new IllegalArgumentException("SLA targets must be more than 0 minutes");
        }
        if (responseMinutes > resolutionMinutes) {
            throw new IllegalArgumentException(
                    "The response target cannot be longer than the resolution target");
        }
        return save(priority, Duration.ofMinutes(responseMinutes),
                Duration.ofMinutes(resolutionMinutes));
    }

    @Override
    public Duration responseTarget(Priority priority) {
        return find(priority).response();
    }

    @Override
    public Duration resolutionTarget(Priority priority) {
        return find(priority).resolution();
    }

    private Row find(Priority priority) {
        if (priority == null) {
            throw new IllegalArgumentException("priority must not be null");
        }
        Row cached = cache.get(priority);
        if (cached != null) {
            return cached;
        }
        Row row = table.findById(priority.name()).map(this::toRow)
                .orElseThrow(() -> new SlaConfigurationMissingException(priority.name()));
        cache.put(priority, row);
        return row;
    }

    private Row save(Priority priority, Duration response, Duration resolution) {
        SlaPolicyEntity entity = new SlaPolicyEntity();
        entity.priority = priority.name();
        entity.responseMinutes = response.toMinutes();
        entity.resolutionMinutes = resolution.toMinutes();
        table.save(entity);
        Row row = new Row(priority, response, resolution);
        cache.put(priority, row);
        return row;
    }

    private Row toRow(SlaPolicyEntity e) {
        return new Row(Priority.valueOf(e.priority),
                Duration.ofMinutes(e.responseMinutes),
                Duration.ofMinutes(e.resolutionMinutes));
    }
}
