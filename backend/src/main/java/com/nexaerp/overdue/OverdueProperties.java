package com.nexaerp.overdue;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Component
@Validated
@ConfigurationProperties(prefix = "app.overdue")
@Getter
@Setter
public class OverdueProperties {

    private boolean enabled = false;

    @Valid
    private Document invoice = new Document();

    @Valid
    private Document vendorBill = new Document();

    @Valid
    private Channel inApp = new Channel(true);

    @Valid
    private Email email = new Email();

    @Valid
    private Scheduler scheduler = new Scheduler();

    @NotBlank
    private String timeZone = "Asia/Dhaka";

    @NotNull
    private List<Integer> milestones = new ArrayList<>(List.of(1, 7, 15, 30));

    @AssertTrue(message = "app.overdue.internal-email-only must remain true")
    private boolean internalEmailOnly = true;

    @Min(1)
    @Max(1000)
    private int batchSize = 100;

    @NotNull
    private Duration processingTimeout = Duration.ofMinutes(30);

    @AssertTrue(message = "app.overdue.milestones must be positive, unique and sorted")
    public boolean isMilestonesValid() {
        if (milestones == null || milestones.isEmpty()) return false;
        if (milestones.stream().anyMatch(value -> value == null || value <= 0)) return false;
        if (new HashSet<>(milestones).size() != milestones.size()) return false;
        for (int index = 1; index < milestones.size(); index++) {
            if (milestones.get(index - 1) >= milestones.get(index)) return false;
        }
        return true;
    }

    @AssertTrue(message = "app.overdue.time-zone must be a valid ZoneId")
    public boolean isTimeZoneValid() {
        try {
            ZoneId.of(timeZone);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @AssertTrue(message = "app.overdue.processing-timeout must be positive")
    public boolean isProcessingTimeoutValid() {
        return processingTimeout != null && !processingTimeout.isZero() && !processingTimeout.isNegative();
    }

    @Getter
    @Setter
    public static class Document {
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class Channel {
        private boolean enabled;

        public Channel() {
        }

        public Channel(boolean enabled) {
            this.enabled = enabled;
        }
    }

    @Getter
    @Setter
    public static class Email extends Channel {
        @Min(1)
        private int maxAttempts = 3;

        @NotNull
        private Duration retryDelay = Duration.ofHours(24);

        @AssertTrue(message = "app.overdue.email.retry-delay must be positive")
        public boolean isRetryDelayValid() {
            return retryDelay != null && !retryDelay.isZero() && !retryDelay.isNegative();
        }
    }

    @Getter
    @Setter
    public static class Scheduler {
        @NotBlank
        private String cron = "0 15 6 * * *";
    }
}
