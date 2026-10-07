-- Aligne les documents existants sur le cycle de vie du livrable 07.

-- Un document dont la notice a ete rejetee restait "a valider" indefiniment.
UPDATE documents SET statut = 'REJETE'
WHERE statut = 'A_VALIDER'
  AND id IN (SELECT document_id FROM metadonnees WHERE statut = 'REJETE');

-- Texte lu mais jamais analyse par l'IA : la file de validation affichait une notice vide.
UPDATE documents SET statut = 'EN_ATTENTE'
WHERE statut = 'A_VALIDER'
  AND NOT EXISTS (
      SELECT 1 FROM enrichissements e
      WHERE e.document_id = documents.id AND e.statut = 'TERMINE'
  );

-- Analyse par l'IA en echec : le texte est disponible, l'analyse peut etre relancee.
UPDATE documents SET statut = 'EN_ATTENTE'
WHERE statut = 'ECHEC' AND texte_extrait IS NOT NULL AND texte_extrait <> '';

-- EN_FILE n'est plus un statut de document.
UPDATE documents SET statut = 'EN_ATTENTE' WHERE statut = 'EN_FILE';
