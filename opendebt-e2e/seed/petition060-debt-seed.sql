-- Petition060 E2E seed — opendebt_debt database
-- Seeds section50 candidate items for the petition060 portal flows.

BEGIN;

DELETE FROM section50_decision_snapshot
WHERE worklist_id IN (
    SELECT id
    FROM section50_worklist
    WHERE debtor_person_id IN (
        '06000000-e2e0-0060-db00-000000000001',
        '06000000-e2e0-0060-db00-000000000002',
        '06000000-e2e0-0060-db00-000000000003'
    )
);

DELETE FROM section50_worklist_entry
WHERE worklist_id IN (
    SELECT id
    FROM section50_worklist
    WHERE debtor_person_id IN (
        '06000000-e2e0-0060-db00-000000000001',
        '06000000-e2e0-0060-db00-000000000002',
        '06000000-e2e0-0060-db00-000000000003'
    )
);

DELETE FROM section50_worklist
WHERE debtor_person_id IN (
    '06000000-e2e0-0060-db00-000000000001',
    '06000000-e2e0-0060-db00-000000000002',
    '06000000-e2e0-0060-db00-000000000003'
);

DELETE FROM section50_candidate_item
WHERE debtor_person_id IN (
    '06000000-e2e0-0060-db00-000000000001',
    '06000000-e2e0-0060-db00-000000000002',
    '06000000-e2e0-0060-db00-000000000003'
);

INSERT INTO section50_candidate_item (
    id,
    debtor_person_id,
    claim_id,
    item_type,
    claim_category,
    amount,
    suspected_data_error,
    confirmed_retskraft,
    accessory_of_claim_id,
    disproportionate_write_off,
    error_type,
    complexity,
    payment_opportunity,
    created_at,
    updated_at,
    created_by,
    updated_by,
    version
) VALUES
    (
        '06000000-e2e0-0060-ci00-000000000021',
        '06000000-e2e0-0060-db00-000000000001',
        'C-06021',
        'PRINCIPAL',
        'FINE',
        250.00,
        FALSE,
        FALSE,
        NULL,
        FALSE,
        NULL,
        'low',
        'medium',
        NOW(),
        NOW(),
        'e2e-seed-p060',
        'e2e-seed-p060',
        0
    ),
    (
        '06000000-e2e0-0060-ci00-000000000022',
        '06000000-e2e0-0060-db00-000000000001',
        'C-06022',
        'PRINCIPAL',
        'OTHER',
        100.00,
        FALSE,
        FALSE,
        NULL,
        FALSE,
        NULL,
        'low',
        'high',
        NOW(),
        NOW(),
        'e2e-seed-p060',
        'e2e-seed-p060',
        0
    ),
    (
        '06000000-e2e0-0060-ci00-000000000061',
        '06000000-e2e0-0060-db00-000000000002',
        'C-06061',
        'PRINCIPAL',
        'OTHER',
        300.00,
        FALSE,
        FALSE,
        NULL,
        FALSE,
        NULL,
        'medium',
        'low',
        NOW(),
        NOW(),
        'e2e-seed-p060',
        'e2e-seed-p060',
        0
    ),
    (
        '06000000-e2e0-0060-ci00-000000000062',
        '06000000-e2e0-0060-db00-000000000002',
        'C-06062',
        'PRINCIPAL',
        'OTHER',
        100.00,
        FALSE,
        FALSE,
        NULL,
        FALSE,
        NULL,
        'low',
        'high',
        NOW(),
        NOW(),
        'e2e-seed-p060',
        'e2e-seed-p060',
        0
    ),
    (
        '06000000-e2e0-0060-ci00-000000000071',
        '06000000-e2e0-0060-db00-000000000003',
        'C-06071',
        'PRINCIPAL',
        'OTHER',
        700.00,
        FALSE,
        TRUE,
        NULL,
        FALSE,
        NULL,
        'low',
        'medium',
        NOW(),
        NOW(),
        'e2e-seed-p060',
        'e2e-seed-p060',
        0
    ),
    (
        '06000000-e2e0-0060-ci00-000000000072',
        '06000000-e2e0-0060-db00-000000000003',
        'C-06072',
        'PRINCIPAL',
        'OTHER',
        300.00,
        FALSE,
        FALSE,
        NULL,
        FALSE,
        NULL,
        'medium',
        'medium',
        NOW(),
        NOW(),
        'e2e-seed-p060',
        'e2e-seed-p060',
        0
    ),
    (
        '06000000-e2e0-0060-ci00-000000000073',
        '06000000-e2e0-0060-db00-000000000003',
        'A-06072',
        'ACCESSORY',
        'OTHER',
        40.00,
        FALSE,
        FALSE,
        'C-06072',
        FALSE,
        NULL,
        'high',
        'low',
        NOW(),
        NOW(),
        'e2e-seed-p060',
        'e2e-seed-p060',
        0
    );

COMMIT;
