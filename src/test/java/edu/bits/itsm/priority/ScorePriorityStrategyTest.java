package edu.bits.itsm.priority;

import edu.bits.itsm.domain.BusinessCriticality;
import edu.bits.itsm.domain.Impact;
import edu.bits.itsm.domain.Priority;
import edu.bits.itsm.domain.Urgency;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Expected priorities are worked out by hand from the approved bands:
// 1-3 P4, 4-8 P3, 9-17 P2, 18-27 P1 (score = impact x urgency x criticality).
class ScorePriorityStrategyTest {

    private final PriorityStrategy strategy = new ScorePriorityStrategy();

    private Priority calc(Impact i, Urgency u, BusinessCriticality c) {
        return strategy.calculate(i, u, c, 1);
    }

    // ---- lowest and highest score ----

    @Test
    void lowestScoreOneIsP4() {
        assertEquals(Priority.P4, calc(Impact.LOW, Urgency.LOW, BusinessCriticality.LOW));
    }

    @Test
    void highestScoreTwentySevenIsP1() {
        assertEquals(Priority.P1, calc(Impact.HIGH, Urgency.HIGH, BusinessCriticality.HIGH));
    }

    // ---- P4 / P3 boundary: 3 -> P4, 4 -> P3 ----

    @Test
    void scoreThreeIsTopOfP4() {
        assertEquals(Priority.P4, calc(Impact.LOW, Urgency.LOW, BusinessCriticality.HIGH));
    }

    @Test
    void scoreFourIsBottomOfP3() {
        assertEquals(Priority.P3, calc(Impact.LOW, Urgency.MEDIUM, BusinessCriticality.MEDIUM));
    }

    // ---- P3 / P2 boundary: 8 is the highest P3, 9 the lowest P2 ----

    @Test
    void scoreEightIsP3() {
        assertEquals(Priority.P3, calc(Impact.MEDIUM, Urgency.MEDIUM, BusinessCriticality.MEDIUM));
    }

    @Test
    void scoreNineIsBottomOfP2() {
        assertEquals(Priority.P2, calc(Impact.HIGH, Urgency.HIGH, BusinessCriticality.LOW));
    }

    // ---- P2 / P1 boundary: 12 is the highest reachable P2, 18 the lowest P1 ----

    @Test
    void scoreTwelveIsHighestReachableBaseScoreInP2() {
        assertEquals(Priority.P2, calc(Impact.HIGH, Urgency.MEDIUM, BusinessCriticality.MEDIUM));
    }

    @Test
    void scoreEighteenIsBottomOfP1() {
        assertEquals(Priority.P1, calc(Impact.HIGH, Urgency.HIGH, BusinessCriticality.MEDIUM));
    }

    // ---- multiplication, not addition ----

    @Test
    void usesProductNotSum() {
        // HIGH, MEDIUM, MEDIUM: product 3*2*2 = 12 -> P2. A sum (3+2+2 = 7) would give P3.
        assertEquals(Priority.P2, calc(Impact.HIGH, Urgency.MEDIUM, BusinessCriticality.MEDIUM));
    }

    @Test
    void orderOfRatingsDoesNotMatter() {
        // Each of these is two HIGHs and one MEDIUM = 18 -> P1.
        assertEquals(Priority.P1, calc(Impact.MEDIUM, Urgency.HIGH, BusinessCriticality.HIGH));
        assertEquals(Priority.P1, calc(Impact.HIGH, Urgency.MEDIUM, BusinessCriticality.HIGH));
        assertEquals(Priority.P1, calc(Impact.HIGH, Urgency.HIGH, BusinessCriticality.MEDIUM));
    }

    // ---- affected users do not change the base result ----

    @Test
    void affectedUserCountDoesNotChangeBasePriority() {
        Impact i = Impact.LOW;
        Urgency u = Urgency.LOW;
        BusinessCriticality c = BusinessCriticality.HIGH; // score 3 -> P4
        assertEquals(Priority.P4, strategy.calculate(i, u, c, 1));
        assertEquals(Priority.P4, strategy.calculate(i, u, c, 50));
        assertEquals(Priority.P4, strategy.calculate(i, u, c, 1000));
        assertEquals(Priority.P4, strategy.calculate(i, u, c, Integer.MAX_VALUE));
    }

    @Test
    void sameInputsAlwaysGiveSameResult() {
        Priority first = calc(Impact.HIGH, Urgency.MEDIUM, BusinessCriticality.MEDIUM);
        Priority second = calc(Impact.HIGH, Urgency.MEDIUM, BusinessCriticality.MEDIUM);
        assertEquals(first, second);
    }

    // ---- affected-user count: 0 is allowed, negative is not ----

    @Test
    void zeroAffectedUsersIsAccepted() {
        // 0 users is valid; the count is not scored, so HIGH/HIGH/MEDIUM = 18 -> P1
        assertEquals(Priority.P1, strategy.calculate(Impact.HIGH, Urgency.HIGH, BusinessCriticality.MEDIUM, 0));
    }

    @Test
    void negativeAffectedUsersIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> strategy.calculate(Impact.LOW, Urgency.LOW, BusinessCriticality.LOW, -1));
        assertTrue(e.getMessage().contains("affectedUsers"));
        assertThrows(IllegalArgumentException.class,
                () -> strategy.calculate(Impact.LOW, Urgency.LOW, BusinessCriticality.LOW, -5));
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

    // ---- name ----

    @Test
    void hasMeaningfulName() {
        assertNotNull(strategy.getName());
        assertEquals("ScorePriorityStrategy", strategy.getName());
    }
}
