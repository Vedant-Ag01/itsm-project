package edu.bits.itsm.sla;

import edu.bits.itsm.domain.Priority;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Expected targets are the team table, typed in by hand:
// P1 5m/30m, P2 15m/2h, P3 1h/8h, P4 4h/24h.
class DefaultSlaPolicyTest {

    // A complete valid custom table: P1 10m/1h, P2 20m/3h, P3 2h/10h, P4 5h/48h.
    private static Map<Priority, Duration> customResponse() {
        Map<Priority, Duration> map = new HashMap<>();
        map.put(Priority.P1, Duration.ofMinutes(10));
        map.put(Priority.P2, Duration.ofMinutes(20));
        map.put(Priority.P3, Duration.ofHours(2));
        map.put(Priority.P4, Duration.ofHours(5));
        return map;
    }

    private static Map<Priority, Duration> customResolution() {
        Map<Priority, Duration> map = new HashMap<>();
        map.put(Priority.P1, Duration.ofHours(1));
        map.put(Priority.P2, Duration.ofHours(3));
        map.put(Priority.P3, Duration.ofHours(10));
        map.put(Priority.P4, Duration.ofHours(48));
        return map;
    }

    // Same value for every priority (handy for tiny or huge targets).
    private static Map<Priority, Duration> allPriorities(Duration target) {
        return Map.of(Priority.P1, target, Priority.P2, target,
                Priority.P3, target, Priority.P4, target);
    }

    // ---- defaults ----

    @Test
    void defaultResponseTargets() {
        SlaPolicy policy = new DefaultSlaPolicy();
        assertEquals(Duration.ofMinutes(5), policy.responseTarget(Priority.P1));
        assertEquals(Duration.ofMinutes(15), policy.responseTarget(Priority.P2));
        assertEquals(Duration.ofHours(1), policy.responseTarget(Priority.P3));
        assertEquals(Duration.ofHours(4), policy.responseTarget(Priority.P4));
    }

    @Test
    void defaultResolutionTargets() {
        SlaPolicy policy = new DefaultSlaPolicy();
        assertEquals(Duration.ofMinutes(30), policy.resolutionTarget(Priority.P1));
        assertEquals(Duration.ofHours(2), policy.resolutionTarget(Priority.P2));
        assertEquals(Duration.ofHours(8), policy.resolutionTarget(Priority.P3));
        assertEquals(Duration.ofHours(24), policy.resolutionTarget(Priority.P4));
    }

    // ---- custom configuration ----

    @Test
    void customMapsChangeTheTargets() {
        SlaPolicy policy = new DefaultSlaPolicy(customResponse(), customResolution());
        assertEquals(Duration.ofMinutes(10), policy.responseTarget(Priority.P1));
        assertEquals(Duration.ofMinutes(20), policy.responseTarget(Priority.P2));
        assertEquals(Duration.ofHours(2), policy.responseTarget(Priority.P3));
        assertEquals(Duration.ofHours(5), policy.responseTarget(Priority.P4));
        assertEquals(Duration.ofHours(1), policy.resolutionTarget(Priority.P1));
        assertEquals(Duration.ofHours(3), policy.resolutionTarget(Priority.P2));
        assertEquals(Duration.ofHours(10), policy.resolutionTarget(Priority.P3));
        assertEquals(Duration.ofHours(48), policy.resolutionTarget(Priority.P4));
    }

    @Test
    void changingCallerMapsAfterConstructionHasNoEffect() {
        Map<Priority, Duration> response = customResponse();
        Map<Priority, Duration> resolution = customResolution();
        SlaPolicy policy = new DefaultSlaPolicy(response, resolution);

        response.put(Priority.P1, Duration.ofMinutes(1));
        resolution.clear();

        assertEquals(Duration.ofMinutes(10), policy.responseTarget(Priority.P1));
        assertEquals(Duration.ofHours(1), policy.resolutionTarget(Priority.P1));
        assertEquals(Duration.ofHours(48), policy.resolutionTarget(Priority.P4));
    }

    @Test
    void acceptsImmutableMapOf() {
        SlaPolicy policy = new DefaultSlaPolicy(Map.copyOf(customResponse()),
                Map.of(Priority.P1, Duration.ofHours(1), Priority.P2, Duration.ofHours(3),
                        Priority.P3, Duration.ofHours(10), Priority.P4, Duration.ofHours(48)));
        assertEquals(Duration.ofHours(3), policy.resolutionTarget(Priority.P2));
    }

    @Test
    void acceptsEqualResponseAndResolution() {
        SlaPolicy policy = new DefaultSlaPolicy(allPriorities(Duration.ofMinutes(30)),
                allPriorities(Duration.ofMinutes(30)));
        assertEquals(Duration.ofMinutes(30), policy.responseTarget(Priority.P3));
        assertEquals(Duration.ofMinutes(30), policy.resolutionTarget(Priority.P3));
    }

    @Test
    void acceptsOneNanosecond() {
        SlaPolicy policy = new DefaultSlaPolicy(allPriorities(Duration.ofNanos(1)),
                allPriorities(Duration.ofNanos(1)));
        assertEquals(Duration.ofNanos(1), policy.responseTarget(Priority.P4));
    }

    @Test
    void acceptsHugeTarget() {
        Duration huge = Duration.ofSeconds(Long.MAX_VALUE, 999_999_999);
        SlaPolicy policy = new DefaultSlaPolicy(allPriorities(Duration.ofMinutes(5)),
                allPriorities(huge));
        assertEquals(huge, policy.resolutionTarget(Priority.P2));
    }

    // ---- rejected configuration (all IllegalArgumentException) ----

    @Test
    void rejectsNullResponseMap() {
        assertThrows(IllegalArgumentException.class,
                () -> new DefaultSlaPolicy(null, customResolution()));
    }

    @Test
    void rejectsNullResolutionMap() {
        assertThrows(IllegalArgumentException.class,
                () -> new DefaultSlaPolicy(customResponse(), null));
    }

    @Test
    void rejectsMissingPriorityInResponseMap() {
        Map<Priority, Duration> response = customResponse();
        response.remove(Priority.P3);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new DefaultSlaPolicy(response, customResolution()));
        assertTrue(e.getMessage().contains("P3"));
        assertTrue(e.getMessage().contains("response"));
    }

    @Test
    void rejectsMissingPriorityInResolutionMap() {
        Map<Priority, Duration> resolution = customResolution();
        resolution.remove(Priority.P4);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new DefaultSlaPolicy(customResponse(), resolution));
        assertTrue(e.getMessage().contains("P4"));
        assertTrue(e.getMessage().contains("resolution"));
    }

    @Test
    void rejectsNullKey() {
        Map<Priority, Duration> response = customResponse();
        response.put(null, Duration.ofMinutes(5));
        assertThrows(IllegalArgumentException.class,
                () -> new DefaultSlaPolicy(response, customResolution()));
    }

    @Test
    void rejectsNullValue() {
        Map<Priority, Duration> resolution = customResolution();
        resolution.put(Priority.P2, null);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new DefaultSlaPolicy(customResponse(), resolution));
        assertTrue(e.getMessage().contains("P2"));
    }

    @Test
    void rejectsZeroTarget() {
        Map<Priority, Duration> response = customResponse();
        response.put(Priority.P1, Duration.ZERO);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new DefaultSlaPolicy(response, customResolution()));
        assertTrue(e.getMessage().contains("P1"));
    }

    @Test
    void rejectsMinusOneNanosecond() {
        Map<Priority, Duration> resolution = customResolution();
        resolution.put(Priority.P3, Duration.ofNanos(-1));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new DefaultSlaPolicy(customResponse(), resolution));
        assertTrue(e.getMessage().contains("P3"));
    }

    @Test
    void rejectsNegativeMinutes() {
        Map<Priority, Duration> response = customResponse();
        response.put(Priority.P4, Duration.ofMinutes(-5));
        assertThrows(IllegalArgumentException.class,
                () -> new DefaultSlaPolicy(response, customResolution()));
    }

    @Test
    void rejectsResponseLongerThanResolutionByOneNanosecond() {
        Map<Priority, Duration> response = customResponse();
        response.put(Priority.P2, Duration.ofHours(3).plusNanos(1));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> new DefaultSlaPolicy(response, customResolution()));
        assertTrue(e.getMessage().contains("P2"));
    }

    @Test
    void gettersRejectNullPriority() {
        SlaPolicy policy = new DefaultSlaPolicy();
        assertThrows(IllegalArgumentException.class, () -> policy.responseTarget(null));
        assertThrows(IllegalArgumentException.class, () -> policy.resolutionTarget(null));
    }
}
