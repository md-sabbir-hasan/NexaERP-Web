package com.nexaerp.approval;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "app.approval")
@Getter
@Setter
public class ApprovalProperties {
    private boolean enabled = false;
    private ManualJournal manualJournal = new ManualJournal();

    @Getter
    @Setter
    public static class ManualJournal {
        private boolean enabled = true;
    }
}
