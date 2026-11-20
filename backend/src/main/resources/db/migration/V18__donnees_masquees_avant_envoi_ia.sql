-- Trace, pour chaque analyse, les donnees personnelles masquees avant l'envoi au modele (par categorie, jamais les valeurs).
ALTER TABLE enrichissements ADD COLUMN IF NOT EXISTS donnees_masquees VARCHAR(255);
