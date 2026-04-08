-- P057 BLOCK-6: Add fordringshaver_id to daekning_fordring table
-- Required for GIL § 4 ordering display (AC-16: fordringshaverId column in portal)
ALTER TABLE daekning_fordring
    ADD COLUMN IF NOT EXISTS fordringshaver_id VARCHAR(100);
