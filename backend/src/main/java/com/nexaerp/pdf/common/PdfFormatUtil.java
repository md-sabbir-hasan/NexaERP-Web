package com.nexaerp.pdf.common;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class PdfFormatUtil {

    private PdfFormatUtil() {}

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    public static String date(LocalDate date) {
        return date == null ? "-" : date.format(DATE_FORMAT);
    }

    public static String money(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;

        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);

        return "BDT " + formatter.format(value);
    }

    public static String text(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}