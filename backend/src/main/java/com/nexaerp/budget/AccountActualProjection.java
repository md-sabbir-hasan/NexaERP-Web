package com.nexaerp.budget;

import java.math.BigDecimal;

public interface AccountActualProjection {
    Long getAccountId();
    BigDecimal getTotalDebit();
    BigDecimal getTotalCredit();
}