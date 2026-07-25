package com.nexaerp.recurringexpense;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecurringExpenseScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecurringExpenseScheduler.class);

    private final RecurringExpenseTemplateService recurringExpenseTemplateService;

    // Runs daily at 1:00 AM server time
    @Scheduled(cron = "0 0 1 * * *")
    public void generateDueRecurringExpenses() {
        log.info("Recurring expense scheduler started");
        recurringExpenseTemplateService.generateDueExpenses();
        log.info("Recurring expense scheduler finished");
    }
}