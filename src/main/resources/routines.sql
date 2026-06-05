-- WASAC Utility Billing System - Database Routines
-- Triggers for automatic notifications on bill generation and full payment

DROP TRIGGER IF EXISTS trg_after_bill_insert//
CREATE TRIGGER trg_after_bill_insert
AFTER INSERT ON bills
FOR EACH ROW
BEGIN
    INSERT INTO notifications (customer_id, message, type, read_flag, bill_id, created_at, updated_at, created_by, updated_by)
    SELECT
        NEW.customer_id,
        CONCAT('Dear ', c.full_names, ',\nYour ', NEW.billing_month, '/', NEW.billing_year,
               ' utility bill of ', NEW.total_amount, ' FRW has been successfully processed.'),
        'BILL_GENERATED',
        FALSE,
        NEW.id,
        NOW(),
        NOW(),
        'system',
        'system'
    FROM customers c
    WHERE c.id = NEW.customer_id;
END//

DROP TRIGGER IF EXISTS trg_after_payment_insert//
CREATE TRIGGER trg_after_payment_insert
AFTER INSERT ON payments
FOR EACH ROW
BEGIN
    DECLARE v_total_paid DECIMAL(14,2);
    DECLARE v_total_amount DECIMAL(14,2);
    DECLARE v_customer_id BIGINT;
    DECLARE v_customer_name VARCHAR(255);
    DECLARE v_billing_month INT;
    DECLARE v_billing_year INT;
    DECLARE v_bill_id BIGINT;

    SELECT b.total_amount, b.customer_id, b.billing_month, b.billing_year, b.id
    INTO v_total_amount, v_customer_id, v_billing_month, v_billing_year, v_bill_id
    FROM bills b WHERE b.id = NEW.bill_id;

    SELECT COALESCE(SUM(p.amount_paid), 0) INTO v_total_paid
    FROM payments p WHERE p.bill_id = NEW.bill_id;

    IF v_total_paid >= v_total_amount THEN
        UPDATE bills SET status = 'PAID', amount_paid = v_total_paid,
            outstanding_balance = 0, updated_at = NOW()
        WHERE id = NEW.bill_id AND status != 'PAID';

        SELECT full_names INTO v_customer_name FROM customers WHERE id = v_customer_id;

        INSERT INTO notifications (customer_id, message, type, read_flag, bill_id, created_at, updated_at, created_by, updated_by)
        VALUES (
            v_customer_id,
            CONCAT('Dear ', v_customer_name, ',\nYour ', v_billing_month, '/', v_billing_year,
                   ' utility bill of ', v_total_amount, ' FRW has been successfully processed.'),
            'PAYMENT_RECEIVED',
            FALSE,
            v_bill_id,
            NOW(),
            NOW(),
            'system',
            'system'
        );
    END IF;
END//

DROP PROCEDURE IF EXISTS sp_get_customer_outstanding//
CREATE PROCEDURE sp_get_customer_outstanding(IN p_customer_id BIGINT)
BEGIN
    SELECT
        c.full_names AS customer_name,
        COUNT(b.id) AS total_bills,
        COALESCE(SUM(b.outstanding_balance), 0) AS total_outstanding
    FROM customers c
    LEFT JOIN bills b ON b.customer_id = c.id AND b.status IN ('APPROVED', 'OVERDUE')
    WHERE c.id = p_customer_id
    GROUP BY c.id, c.full_names;
END//
