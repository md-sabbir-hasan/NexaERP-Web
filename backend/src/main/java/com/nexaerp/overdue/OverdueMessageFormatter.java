package com.nexaerp.overdue;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class OverdueMessageFormatter {

    public String notificationMessage(OverdueDocumentSnapshot document) {
        String prefix = document.documentType() == OverdueDocumentType.INVOICE
                ? "Invoice "
                : "Vendor bill ";
        return prefix + safe(document.documentNumber())
                + " is " + document.actualDaysOverdue()
                + " days overdue with " + safe(document.currencyCode())
                + " " + amount(document.dueAmount()) + " outstanding.";
    }

    public String amount(BigDecimal value) {
        return value == null
                ? "0.00"
                : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    public String safe(String value) {
        if (value == null) return "";
        return value.replaceAll("[\\p{Cntrl}]", " ").trim();
    }
}
