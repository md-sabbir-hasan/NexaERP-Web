package com.nexaerp.settings;


import com.nexaerp.account.Account;
import com.nexaerp.account.AccountRepository;
import com.nexaerp.audit.AuditAction;
import com.nexaerp.audit.AuditLogService;
import com.nexaerp.common.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SystemSettingsServiceImpl implements SystemSettingsService{

    private final SystemSettingRepository systemSettingRepository;
    private final AccountRepository accountRepository;
    private final AuditLogService auditLogService;

    // Spring's @Cacheable works through a proxy - calling this.someCachedMethod()
    // from inside another method of the same class bypasses that proxy entirely,
    // so the cache never triggers. Self-injecting the proxy and calling through
    // it (self.getAccountId(...), self.getValue(...)) fixes that.
    @Lazy
    @Autowired
    private SystemSettingsService self;

    @Override
    @Cacheable(value = "systemSettings", key = "#key")
    public Account getAccount(SettingKey key) {
        Long accountId = self.getAccountId(key);
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found for setting: " + key));
    }

    @Override
    @Cacheable(value = "systemSettings", key = "#key + '_id'")
    public Long getAccountId(SettingKey key) {
        String value = self.getValue(key);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new ResourceNotFoundException(
                    "Invalid account ID in settings for key: " + key);
        }
    }

    @Override
    @Cacheable(value = "systemSettings", key = "#key + '_value'")
    public String getValue(SettingKey key) {
        return systemSettingRepository.findByKey(key)
                .map(SystemSetting::getValue)
                .orElseThrow(() -> new ResourceNotFoundException(
                        key + " is not configured yet. Please set it from Settings > Default Accounts first."));
    }

    @Override
    @CacheEvict(value = "systemSettings", allEntries = true)
    public void updateSetting(SettingKey key, String value) {
        SystemSetting setting = systemSettingRepository.findByKey(key)
                .orElseGet(() -> SystemSetting.builder().key(key).build());

        String oldValue = setting.getValue();
        setting.setValue(value);
        systemSettingRepository.save(setting);

        auditLogService.log(AuditAction.UPDATED, "SYSTEM_SETTING", setting.getId(),
                oldValue, value);
    }
}