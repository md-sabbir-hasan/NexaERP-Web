package com.nexaerp.overdue;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverduePropertiesTest {

    @Test
    void defaultConfigurationIsSafeAndValid() {
        OverdueProperties properties = new OverdueProperties();

        assertFalse(properties.isEnabled());
        assertFalse(properties.getEmail().isEnabled());
        assertTrue(properties.isInternalEmailOnly());
        assertTrue(properties.isMilestonesValid());
        assertTrue(properties.isTimeZoneValid());
        assertTrue(properties.isProcessingTimeoutValid());
        assertTrue(properties.getEmail().isRetryDelayValid());
    }

    @Test
    void milestonesZoneAndDurationsRejectUnsafeValues() {
        OverdueProperties properties = new OverdueProperties();
        properties.setMilestones(List.of(1, 7, 7, 30));
        assertFalse(properties.isMilestonesValid());
        properties.setMilestones(List.of(7, 1));
        assertFalse(properties.isMilestonesValid());
        properties.setMilestones(List.of(0, 1));
        assertFalse(properties.isMilestonesValid());

        properties.setTimeZone("Not/A_Zone");
        assertFalse(properties.isTimeZoneValid());
        properties.setProcessingTimeout(Duration.ZERO);
        assertFalse(properties.isProcessingTimeoutValid());
        properties.getEmail().setRetryDelay(Duration.ofSeconds(-1));
        assertFalse(properties.getEmail().isRetryDelayValid());
    }
}
