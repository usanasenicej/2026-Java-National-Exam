package com.wasac.ne.scheduler;

import com.wasac.ne.service.BillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled jobs for automatic bill lifecycle management.
 *
 * Runs every night at midnight (server local time) to:
 * 1. Mark APPROVED bills whose due date has passed as OVERDUE
 * 2. Apply late payment penalties to those overdue bills
 * 3. Disconnect meters for bills overdue more than 90 days
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueBillScheduler {

    private final BillService billService;

    /**
     * Trigger: daily at midnight — "0 0 0 * * *"
     *   Seconds  Minutes  Hours  Day-of-month  Month  Day-of-week
     *      0        0       0         *           *         *
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void processOverdueBillsNightly() {
        log.info("=== [SCHEDULER] Nightly overdue bill processing started ===");
        try {
            int updated = billService.processOverdueBills();
            log.info("=== [SCHEDULER] Nightly overdue bill processing complete — {} bills updated ===", updated);
        } catch (Exception e) {
            log.error("=== [SCHEDULER] Overdue bill processing failed: {} ===", e.getMessage(), e);
        }
    }
}
