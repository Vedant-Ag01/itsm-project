package edu.bits.itsm.priority;

import edu.bits.itsm.domain.BusinessCriticality;
import edu.bits.itsm.domain.Impact;
import edu.bits.itsm.domain.Priority;
import edu.bits.itsm.domain.Urgency;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Expected priorities are worked out by hand from the approved weighted rule:
// adjusted = (impact x urgency x criticality) + affectedUsers / 20 (integer division),
// then 18+ P1, 9+ P2, 4+ P3, else P4.
class WeightedPriorityStrategyTest {

    private final PriorityStrategy strategy = new WeightedPriorityStrategy();

    // ---- first bonus point at 20 users: base 3 (LOW/LOW/HIGH) ----

    @Test
    void under20UsersAddsNothing() {
        // 3 + 0 = 3 -> P4
        assertEquals(Priority.P4, strategy.calculate(Impact.LOW, Urgency.LOW, BusinessCriticality.HIGH, 1));
        assertEquals(Priority.P4, strategy.calculate(Impact.LOW, Urgency.LOW, BusinessCriticality.HIGH, 19));
    }

    @Test
    void twentyUsersAddsOnePoint() {
        // 3 + 1 = 4 -> P3
        assertEquals(Priority.P3, strategy.calculate(Impact.LOW, Urgency.LOW, BusinessCriticality.HIGH, 20));
        assertEquals(Priority.P3, strategy.calculate(Impact.LOW, Urgency.LOW, BusinessCriticality.HIGH, 21));
    }

    // ---- second bonus point at 40 users: base 2 (LOW/LOW/MEDIUM) ----

    @Test
    void thirtyNineUsersAddsOnePoint() {
        // 2 + 1 = 3 -> P4
        assertEquals(Priority.P4, strategy.calculate(Impact.LOW, Urgency.LOW, BusinessCriticality.MEDIUM, 39));
    }

    @Test
    void fortyUsersAddsTwoPoints() {
        // 2 + 2 = 4 -> P3
        assertEquals(Priority.P3, strategy.calculate(Impact.LOW, Urgency.LOW, BusinessCriticality.MEDIUM, 40));
        assertEquals(Priority.P3, strategy.calculate(Impact.LOW, Urgency.LOW, BusinessCriticality.MEDIUM, 41));
    }

    // ---- third bonus point at 60 users: base 1 (LOW/LOW/LOW) ----

    @Test
    void fiftyNineUsersAddsTwoPoints() {
        // 1 + 2 = 3 -> P4
        assertEquals(Priority.P4, strategy.calculate(Impact.LOW, Urgency.LOW, BusinessCriticality.LOW, 59));
    }

    @Test
    void sixtyUsersAddsThreePoints() {
        // 1 + 3 = 4 -> P3
        assertEquals(Priority.P3, strategy.calculate(Impact.LOW, Urgency.LOW, BusinessCriticality.LOW, 60));
        assertEquals(Priority.P3, strategy.calculate(Impact.LOW, Urgency.LOW, BusinessCriticality.LOW, 61));
    }

    // ---- base 8 crosses from P3 to P2 with one bonus point ----

    @Test
    void mediumIncidentWith19UsersStaysP3() {
        // 8 + 0 = 8 -> P3
        assertEquals(Priority.P3, strategy.calculate(Impact.MEDIUM, Urgency.MEDIUM, BusinessCriticality.MEDIUM, 19));
    }

    @Test
    void mediumIncidentWith20UsersBecomesP2() {
        // 8 + 1 = 9 -> P2
        assertEquals(Priority.P2, strategy.calculate(Impact.MEDIUM, Urgency.MEDIUM, BusinessCriticality.MEDIUM, 20));
    }

    // ---- P3 -> P2 crossing: base 6 (HIGH/MEDIUM/LOW) ----

    @Test
    void adjustedEightStaysP3() {
        // 6 + 59/20 = 6 + 2 = 8 -> P3
        assertEquals(Priority.P3, strategy.calculate(Impact.HIGH, Urgency.MEDIUM, BusinessCriticality.LOW, 59));
    }

    @Test
    void adjustedNineBecomesP2() {
        // 6 + 3 = 9 -> P2
        assertEquals(Priority.P2, strategy.calculate(Impact.HIGH, Urgency.MEDIUM, BusinessCriticality.LOW, 60));
        assertEquals(Priority.P2, strategy.calculate(Impact.HIGH, Urgency.MEDIUM, BusinessCriticality.LOW, 61));
    }

    // ---- P2 -> P1 crossing: base 12 (HIGH/MEDIUM/MEDIUM) ----

    @Test
    void adjustedSeventeenStaysP2() {
        // 12 + 5 = 17 -> P2
        assertEquals(Priority.P2, strategy.calculate(Impact.HIGH, Urgency.MEDIUM, BusinessCriticality.MEDIUM, 100));
        assertEquals(Priority.P2, strategy.calculate(Impact.HIGH, Urgency.MEDIUM, BusinessCriticality.MEDIUM, 119));
    }

    @Test
    void adjustedEighteenBecomesP1() {
        // 12 + 6 = 18 -> P1
        assertEquals(Priority.P1, strategy.calculate(Impact.HIGH, Urgency.MEDIUM, BusinessCriticality.MEDIUM, 120));
        assertEquals(Priority.P1, strategy.calculate(Impact.HIGH, Urgency.MEDIUM, BusinessCriticality.MEDIUM, 121));
    }

    @Test
    void hundredUsersIsNotAnAutomaticOneBandPromotion() {
        // 12 + 5 = 17 -> P2
        assertEquals(Priority.P2, strategy.calculate(Impact.HIGH, Urgency.MEDIUM, BusinessCriticality.MEDIUM, 100));
    }

    // ---- several bands can be crossed ----

    @Test
    void largeOutageCanLiftAllLowIncidentToP1() {
        // 1 + 340/20 = 1 + 17 = 18 -> P1 (crosses P4 -> P3 -> P2 -> P1)
        assertEquals(Priority.P1, strategy.calculate(Impact.LOW, Urgency.LOW, BusinessCriticality.LOW, 340));
    }

    // ---- P1 saturation and very large counts ----

    @Test
    void scoreAbove27StaysP1() {
        // 27 + 1 = 28 -> P1
        assertEquals(Priority.P1, strategy.calculate(Impact.HIGH, Urgency.HIGH, BusinessCriticality.HIGH, 20));
    }

    @Test
    void maximumUserCountWithHighestBaseIsP1() {
        // 27 + 107,374,182 = 107,374,209 (fits in int) -> P1
        assertEquals(Priority.P1,
                strategy.calculate(Impact.HIGH, Urgency.HIGH, BusinessCriticality.HIGH, Integer.MAX_VALUE));
    }

    @Test
    void maximumUserCountWithLowestBaseIsP1() {
        // 1 + 107,374,182 -> P1
        assertEquals(Priority.P1,
                strategy.calculate(Impact.LOW, Urgency.LOW, BusinessCriticality.LOW, Integer.MAX_VALUE));
    }

    @Test
    void baseP1StaysP1WithOneUser() {
        // 18 + 0 = 18 -> P1
        assertEquals(Priority.P1, strategy.calculate(Impact.HIGH, Urgency.HIGH, BusinessCriticality.MEDIUM, 1));
    }

    // ---- affected-user count: 0 is allowed, negative is not ----

    @Test
    void zeroAffectedUsersIsAcceptedWithNoBonus() {
        // 0 / 20 = 0 bonus: HIGH/HIGH/MEDIUM = 18 + 0 -> P1, LOW/LOW/HIGH = 3 + 0 -> P4
        assertEquals(Priority.P1, strategy.calculate(Impact.HIGH, Urgency.HIGH, BusinessCriticality.MEDIUM, 0));
        assertEquals(Priority.P4, strategy.calculate(Impact.LOW, Urgency.LOW, BusinessCriticality.HIGH, 0));
    }

    @Test
    void negativeAffectedUsersIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> strategy.calculate(Impact.LOW, Urgency.LOW, BusinessCriticality.LOW, -1));
        assertTrue(e.getMessage().contains("affectedUsers"));
        assertThrows(IllegalArgumentException.class,
                () -> strategy.calculate(Impact.LOW, Urgency.LOW, BusinessCriticality.LOW, -100));
    }

    // ---- invalid ratings ----

    @Test
    void nullImpactIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> strategy.calculate(null, Urgency.LOW, BusinessCriticality.LOW, 1));
        assertTrue(e.getMessage().contains("impact"));
    }

    @Test
    void nullUrgencyIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> strategy.calculate(Impact.LOW, null, BusinessCriticality.LOW, 1));
        assertTrue(e.getMessage().contains("urgency"));
    }

    @Test
    void nullCriticalityIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> strategy.calculate(Impact.LOW, Urgency.LOW, null, 1));
        assertTrue(e.getMessage().contains("criticality"));
    }

    // ---- names and interchangeability ----

    @Test
    void namesAreMeaningfulAndDistinct() {
        PriorityStrategy base = new ScorePriorityStrategy();
        assertEquals("WeightedPriorityStrategy", strategy.getName());
        assertNotEquals(base.getName(), strategy.getName());
    }

    @Test
    void eitherStrategyCanBeUsedThroughTheInterface() {
        // Same call, same variable type; only the object behind it changes.
        // LOW/LOW/HIGH = 3, with 20 users: base stays 3 -> P4, weighted 3 + 1 = 4 -> P3.
        PriorityStrategy chosen = new ScorePriorityStrategy();
        assertEquals(Priority.P4, chosen.calculate(Impact.LOW, Urgency.LOW, BusinessCriticality.HIGH, 20));

        chosen = new WeightedPriorityStrategy();
        assertEquals(Priority.P3, chosen.calculate(Impact.LOW, Urgency.LOW, BusinessCriticality.HIGH, 20));
    }
}
