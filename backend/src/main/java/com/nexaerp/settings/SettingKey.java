package com.nexaerp.settings;

import java.util.Set;

public enum SettingKey {
    // Default Accounts
    DEFAULT_RECEIVABLE_ACCOUNT,
    DEFAULT_PAYABLE_ACCOUNT,
    DEFAULT_SALES_REVENUE,
    DEFAULT_VAT_PAYABLE,
    DEFAULT_INPUT_VAT,
    DEFAULT_TDS_PAYABLE,
    DEFAULT_OPENING_EQUITY,
    DEFAULT_SALES_RETURN_ACCOUNT,
    DEFAULT_PURCHASE_RETURN_ACCOUNT,

    // Company Settings (future)
    COMPANY_NAME,
    DEFAULT_CURRENCY,
    FINANCIAL_YEAR,
    DECIMAL_PRECISION,
    TIMEZONE,
    DATE_FORMAT,

    // Feature Flags (future)
    AUTO_POST_INVOICE,
    ALLOW_NEGATIVE_STOCK,
    DEFAULT_WAREHOUSE;

    // Keys whose value must be a valid, active Account ID.
    private static final Set<SettingKey> ACCOUNT_REFERENCE_KEYS = Set.of(
            DEFAULT_RECEIVABLE_ACCOUNT,
            DEFAULT_PAYABLE_ACCOUNT,
            DEFAULT_SALES_REVENUE,
            DEFAULT_VAT_PAYABLE,
            DEFAULT_INPUT_VAT,
            DEFAULT_TDS_PAYABLE,
            DEFAULT_OPENING_EQUITY,
            DEFAULT_SALES_RETURN_ACCOUNT,
            DEFAULT_PURCHASE_RETURN_ACCOUNT
    );

    public boolean isAccountReference() {
        return ACCOUNT_REFERENCE_KEYS.contains(this);
    }
}