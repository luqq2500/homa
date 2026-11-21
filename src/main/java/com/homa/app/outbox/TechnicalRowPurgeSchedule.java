package com.homa.app.outbox;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs {@link TechnicalRowPurge} once a day, at {@code homa.technical-rows.purge-cron} in Kuala Lumpur time, for rows
 * older than {@code homa.technical-rows.retention} (30 days, plan "Data retention"). Every instance may run it; the
 * deletes are idempotent. Turned off with {@code homa.technical-rows.purge.enabled=false}.
 */
@Component
@ConditionalOnProperty(name = "homa.technical-rows.purge.enabled", havingValue = "true", matchIfMissing = true)
public class TechnicalRowPurgeSchedule {

    private final TechnicalRowPurge purge;
    private final Duration retention;

    public TechnicalRowPurgeSchedule(TechnicalRowPurge purge,
            @Value("${homa.technical-rows.retention:P30D}") Duration retention) {
        this.purge = purge;
        this.retention = retention;
    }

    @Scheduled(cron = "${homa.technical-rows.purge-cron:0 17 3 * * *}", zone = "Asia/Kuala_Lumpur")
    public void purgeExpiredTechnicalRows() {
        purge.purgeOlderThan(retention);
    }
}
