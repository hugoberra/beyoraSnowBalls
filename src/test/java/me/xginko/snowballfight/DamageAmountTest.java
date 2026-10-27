package me.xginko.snowballfight;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DamageAmountTest {
    @Test void keepsExistingConfiguredDamageIncludingFractionsAndZero() {
        assertEquals(3.0, DamageAmount.resolve(null, 3));
        assertEquals(7.5, DamageAmount.resolve(null, 7.5));
        assertEquals(0.0, DamageAmount.resolve(null, 0));
    }

    @Test void newKeyTakesPrecedenceWithoutAddingLegacyDamageAgain() {
        assertEquals(30.0, DamageAmount.resolve(30, 3));
        assertEquals(0.0, DamageAmount.resolve(0, 30));
    }

    @Test void freshConfigurationUsesRequestedDefault() {
        assertEquals(30.0, DamageAmount.resolve(null, null));
    }

    @Test void rejectsInvalidAmountsBeforeReplacingTheRunningConfiguration() {
        for (Object value : new Object[] { -1, Double.NaN, Double.POSITIVE_INFINITY, "30" }) {
            assertThrows(IllegalArgumentException.class, () -> DamageAmount.resolve(value, null));
            assertThrows(IllegalArgumentException.class, () -> DamageAmount.resolve(null, value));
        }
    }
}
