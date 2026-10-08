-- Comptes de demonstration : un mot de passe commun, celui des comptes de seed,
-- pour pouvoir se connecter a chacun pendant la defense.
-- Seuls les comptes en @*.demo-metamind.test sont concernes ; aucun compte reel n'est touche.
-- L'empreinte est un BCrypt (cout 10), verifiee avec le BCryptPasswordEncoder de l'application.
UPDATE users
SET mot_de_passe_hash = '$2a$10$jTGUH9aQ9tAQ09.EKszpR.dVxPi2TPryjlXMlRGLG0sCVPEZrA3we'
WHERE email LIKE '%.demo-metamind.test'
  AND status = 'ACTIF';
