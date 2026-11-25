-- V2: recipe OOP discriminator + attributes (flagged feature-required additions)
ALTER TABLE recipe ADD COLUMN recipe_type VARCHAR(10) NOT NULL DEFAULT 'VEG';
ALTER TABLE recipe ADD COLUMN cost INT;
ALTER TABLE recipe ADD COLUMN speed INT;       -- 1..5
ALTER TABLE recipe ADD COLUMN difficulty INT;  -- 1..5
