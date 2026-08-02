package com.nexaerp.overdue;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class OverdueReminderSchedulerTest {

    @Test
    void disabledSchedulerPerformsNoDiscovery() {
        OverdueProperties properties = new OverdueProperties();
        properties.setEnabled(false);
        OverdueReminderService service = mock(OverdueReminderService.class);

        new OverdueReminderScheduler(properties, service).runDaily();

        verify(service, never()).processDueReminders();
    }

    @Test
    void enabledSchedulerDelegatesOnce() {
        OverdueProperties properties = new OverdueProperties();
        properties.setEnabled(true);
        OverdueReminderService service = mock(OverdueReminderService.class);

        new OverdueReminderScheduler(properties, service).runDaily();

        verify(service).processDueReminders();
    }
}
