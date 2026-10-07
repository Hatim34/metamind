-- Une personne dont l'institution n'est pas encore inscrite peut en demander l'ajout.
-- L'institution reste inactive jusqu'a la decision de l'administrateur.
ALTER TABLE institutions ADD COLUMN IF NOT EXISTS en_attente BOOLEAN NOT NULL DEFAULT FALSE;

-- Les comptes de demonstration avaient ete rattaches a l'UCLouvain et a la KU Leuven
-- en gardant une adresse d'une autre institution : l'adresse suit maintenant le domaine.
UPDATE users SET email = 'sarah.lemaire@uclouvain.demo-metamind.test'
WHERE lower(email) = 'sarah@institution-a.example'
  AND institution_id = (SELECT id FROM institutions WHERE domaine_email = 'uclouvain.demo-metamind.test')
  AND NOT EXISTS (SELECT 1 FROM users WHERE lower(email) = 'sarah.lemaire@uclouvain.demo-metamind.test');

UPDATE users SET email = 'jan.peeters@kuleuven.demo-metamind.test'
WHERE lower(email) = 'jan@institution-b.example'
  AND institution_id = (SELECT id FROM institutions WHERE domaine_email = 'kuleuven.demo-metamind.test')
  AND NOT EXISTS (SELECT 1 FROM users WHERE lower(email) = 'jan.peeters@kuleuven.demo-metamind.test');

-- Adresses de demonstration sans accents (ines.wauters plutot que ines avec accent grave).
UPDATE users u SET email = translate(u.email, 'àâäéèêëîïôöùûüçñ', 'aaaeeeeiioouuucn')
WHERE u.email <> translate(u.email, 'àâäéèêëîïôöùûüçñ', 'aaaeeeeiioouuucn')
  AND NOT EXISTS (SELECT 1 FROM users o WHERE o.email = translate(u.email, 'àâäéèêëîïôöùûüçñ', 'aaaeeeeiioouuucn'));
