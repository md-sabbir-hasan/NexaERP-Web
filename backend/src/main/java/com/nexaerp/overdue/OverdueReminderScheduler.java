package com.nexaerp.overdue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OverdueReminderScheduler {

    private final OverdueProperties properties;
    private final OverdueReminderService reminderService;

    @Scheduled(
            cron = "${app.overdue.scheduler.cron:0 15 6 * * *}",
            zone = "${app.overdue.time-zone:Asia/Dhaka}"
    )
    public void runDaily() {
        if (!properties.isEnabled()) return;
        log.info("Overdue reminder scheduler started");
        reminderService.processDueReminders();
        log.info("Overdue reminder scheduler finished");
    }
}
