-- V5: Fix demo caseworker IDs to match caseworker-portal demo-login identities
-- Portal offers: anna-jensen, erik-sorensen, mette-larsen
-- Seed data used: caseworker-demo (does not exist in portal)

ALTER TABLE cases DISABLE TRIGGER cases_versioning_trigger;
ALTER TABLE cases DISABLE TRIGGER cases_audit_trigger;

-- Assign the existing crossing-transaction demo case to anna-jensen
UPDATE cases SET
    primary_caseworker_id = 'anna-jensen'
WHERE id = '00000000-0000-0000-0000-00000000C001';

-- Also fix the legacy column (V2 schema) if still present
UPDATE cases SET
    assigned_caseworker_id = 'anna-jensen'
WHERE id = '00000000-0000-0000-0000-00000000C001'
  AND assigned_caseworker_id IS NOT NULL;

ALTER TABLE cases ENABLE TRIGGER cases_versioning_trigger;
ALTER TABLE cases ENABLE TRIGGER cases_audit_trigger;
