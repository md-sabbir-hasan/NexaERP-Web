package com.nexaerp.recurringexpense;

import com.nexaerp.recurringexpense.dto.RecurringExpenseTemplateRequestDto;
import com.nexaerp.recurringexpense.dto.RecurringExpenseTemplateResponseDto;

import java.util.List;

public interface RecurringExpenseTemplateService {
    RecurringExpenseTemplateResponseDto create(RecurringExpenseTemplateRequestDto request);
    RecurringExpenseTemplateResponseDto update(Long id, RecurringExpenseTemplateRequestDto request);
    RecurringExpenseTemplateResponseDto getById(Long id);
    List<RecurringExpenseTemplateResponseDto> getAll();
    void delete(Long id);

    RecurringExpenseTemplateResponseDto pause(Long id);
    RecurringExpenseTemplateResponseDto resume(Long id);

    // Manually force-generate this template's expense right now (ignores nextRunDate check)
    RecurringExpenseTemplateResponseDto runNow(Long id);

    // Used internally by runNow() and the scheduler — public + @Transactional so the
    // @Lazy self-injection proxy trick works correctly (see impl for details)
    RecurringExpenseTemplateResponseDto generateSingleTemplate(Long id);

    // Called by the scheduler daily — finds every ACTIVE template due today or earlier
    void generateDueExpenses();
}