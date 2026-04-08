-- P058 fix: add tier_level discriminator column to collection_measures so that
-- applyTier2Waiver can query only tier-2 SET_OFF rows without polluting tier-1 / tier-3.
-- Nullable to preserve backward compatibility with rows created before this migration.
ALTER TABLE collection_measures ADD COLUMN tier_level INTEGER;
