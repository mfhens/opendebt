-- V3: Create notifications table for underretning, paakrav, rykker (petition004)
-- Begrebsmodel: Underretning=Notification, Påkrav=Demand for Payment, Rykker=Reminder Notice

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_type VARCHAR(30) NOT NULL,
    debt_id UUID NOT NULL REFERENCES debts(id),
    sender_creditor_org_id UUID NOT NULL,
    recipient_person_id UUID NOT NULL,
    channel VARCHAR(20) NOT NULL,
    sent_at TIMESTAMPTZ,
    delivery_state VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ocr_line VARCHAR(50),
    related_lifecycle_event_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_notification_type CHECK (notification_type IN (
        'PAAKRAV', 'RYKKER', 'AFREGNING', 'UDLIGNING',
        'ALLOKERING', 'RENTER', 'AFSKRIVNING', 'TILBAGESEND'
    )),
    CONSTRAINT chk_notification_channel CHECK (channel IN (
        'DIGITAL_POST', 'PHYSICAL_MAIL', 'PORTAL'
    )),
    CONSTRAINT chk_notification_delivery_state CHECK (delivery_state IN (
        'PENDING', 'SENT', 'DELIVERED', 'FAILED'
    ))
);

CREATE INDEX idx_notification_debt_id ON notifications(debt_id);
CREATE INDEX idx_notification_recipient ON notifications(recipient_person_id);
CREATE INDEX idx_notification_type ON notifications(notification_type);
CREATE INDEX idx_notification_delivery_state ON notifications(delivery_state);

COMMENT ON TABLE notifications IS 'Underretninger: paakrav, rykker, and other debt notifications';
COMMENT ON COLUMN notifications.notification_type IS 'Type of notification (begrebsmodel mapping)';
COMMENT ON COLUMN notifications.ocr_line IS 'OCR payment reference line for PAAKRAV notifications';
