-- La classification ("Medicine", "Arts and Humanities"...) restait dans sa langue d'origine
-- alors que le reste de la notice etait traduit.
ALTER TABLE traductions_publications ADD COLUMN IF NOT EXISTS classification VARCHAR(255);
