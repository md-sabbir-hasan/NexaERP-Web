package com.nexaerp.settings;

import com.nexaerp.account.Account;

public interface SystemSettingsService {
    // Get account by setting key
    Account getAccount(SettingKey key);

    // Get account ID by setting key
    Long getAccountId(SettingKey key);

    // Get raw string value
    String getValue(SettingKey key);

    // Update a setting
    void updateSetting(SettingKey key, String value);
}
