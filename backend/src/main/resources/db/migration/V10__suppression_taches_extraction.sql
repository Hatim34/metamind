-- La table taches_extraction (V5) preparait une file d'extraction asynchrone qui n'a
-- jamais ete branchee : aucun code n'y lit ni n'y ecrit. L'extraction est synchrone,
-- et seul le traitement du fichier apres l'import est asynchrone (@Async). La table,
-- toujours vide, est retiree pour que le schema decrive ce que fait l'application.
DROP TABLE IF EXISTS taches_extraction;
