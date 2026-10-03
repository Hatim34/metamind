# Jeu de démonstration Metamind — données réelles

Ce dossier remplit Metamind avec de **vraies publications en libre accès** d'universités belges.
Les métadonnées viennent d'OpenAlex et les PDF de leurs dépôts d'origine. Les fichiers sont importés
**via l'API de Metamind**, comme le ferait un bibliothécaire, puis l'historique est étalé sur plusieurs
mois pour que le site ait l'air en service depuis longtemps.

À placer à la racine du dépôt : `metamind/seed/`.

## Résultat attendu

- 6 institutions (ULB, UCLouvain, ULiège, KU Leuven, UGent, VUB), avec 2 à 3 bibliothécaires fictifs chacune.
- Environ 160 publications réelles (articles, thèses, rapports, preprints) en FR, NL et EN, avec :
  - auteurs, ORCID, DOI, résumé et mots-clés ;
  - la couverture du document, générée à partir de sa 1re page ;
  - la licence d'origine.
- Des visuels :
  - `covers/` : couverture de chaque publication (1re page), envoyée à Metamind comme image de couverture ;
  - `figures/` : la plus grande figure de chaque PDF (graphique, photo de terrain, schéma), pour des cartes plus parlantes sur l'accueil ;
  - `photos/<CODE>.jpg` : une photo de campus par institution ;
  - `photos/accueil-*.jpg` : 3 photos d'ambiance pour la page d'accueil.

  Les photos viennent de Wikimedia Commons, sous licence libre, avec l'auteur et la licence dans `manifest.json`. Relis-les une à une : la recherche peut remonter une image hors sujet.
- Des états réalistes : environ 70 % publiés, 15 % à valider, 10 % en attente, 5 % rejetés. Une partie des documents publiés est réservée à l'institution.
- Des crédits, mouvements, validations et journaux répartis sur 8 mois.
- `data/evaluation_seed.csv` : une première comparaison de la sortie du LLM avec les métadonnées réelles, par champ. Elle alimente directement le chapitre « Évaluation » du rapport.

## Étapes

```bash
cd seed
python -m venv .venv && source .venv/bin/activate      # Windows : .venv\Scripts\activate
pip install -r requirements.txt

# 1. Collecte (≈ 15-30 min selon les serveurs des éditeurs)
python collect.py --mailto ton.email@exemple.be --photos --figures --hero
#    test rapide : python collect.py --mailto ... --only ULB --per-institution 5

# 2. Chargement dans Metamind (backend démarré, base vide de préférence)
export METAMIND_ADMIN_EMAIL=admin@...
export METAMIND_ADMIN_PASSWORD=...
export METAMIND_DEMO_PASSWORD='Choisis-un-mot-de-passe-long'
python load.py --api http://localhost:8080/api/v1
#    test : python load.py --limit 5

# 3. Historique étalé dans le temps
python backdate.py --months 8 > data/backdate.sql
docker compose exec -T db psql -U metamind -d metamind < data/backdate.sql
```

Pour repartir de zéro : supprimer le volume de la base (`docker compose down -v`), relancer, puis
refaire les étapes 2 et 3. La collecte n'est pas à refaire, car `data/` est réutilisé.

## Visuels déjà fournis (`visuels/`)

Ce sont des illustrations originales, sans droits tiers, aux couleurs du livrable 10. Elles reprennent le motif de pages dont les champs sont surlignés, comme une extraction de métadonnées.

- `institutions/<CODE>.jpg` (1600×640) : un bandeau par institution, chacun avec sa propre composition. Il sert en en-tête de la page institution, ou de visuel de repli si aucune photo libre n'est trouvée.
- `accueil/hero-1..3.jpg` (2400×1000) : trois fonds d'en-tête pour l'accueil, en version sombre, claire et teal. Les documents sont groupés d'un côté pour laisser la place au titre et à la recherche.
- `couverture-defaut.jpg` (900×1200) : la couverture affichée quand un document n'a pas de vignette.

Pour les régénérer ou en créer pour une nouvelle institution : `python visuels.py` (Pillow requis).
Les photos réelles (campus, ambiance) restent récupérées par `collect.py --photos --hero`, sur ta machine.

## Coût LLM

Chaque document qui n'est pas « en attente » déclenche une extraction.
- Avec `METAMIND_LLM_PROVIDER=gemini`, environ 140 appels sont faits, et `evaluation_seed.csv` mesure de vrais résultats.
- Avec le provider `local`, le chargement fonctionne mais l'évaluation n'a aucune valeur.

Pour le rapport, utilise Gemini.

## Règles respectées (à citer dans le livrable 18)

- **Licences** : seuls les PDF sous CC BY, CC BY-SA, CC0 ou domaine public sont repris. Ces licences autorisent la rediffusion, y compris par un service payant. `--allow-nc` ajoute les licences NC ; à éviter pour un SaaS commercial. La licence et l'URL d'origine sont conservées dans `manifest.json` et doivent être affichées sur la fiche.
- **Métadonnées** : OpenAlex les diffuse sous CC0. Le citer comme source sur le site.
- **Photos** : uniquement des images Wikimedia Commons sous licence libre (CC BY, CC BY-SA, CC0, domaine public), avec l'auteur et la licence enregistrés pour le crédit obligatoire. Pas de logos : ce sont des marques, et les utiliser suggérerait un partenariat.
- **Pas de fausse affiliation** : les institutions sont réelles, mais le site doit afficher un bandeau du type « Démonstration — données publiques issues d'OpenAlex, sans lien avec les institutions citées ».
- **Personnes** : les auteurs sont réels, puisque ce sont des métadonnées publiques de leurs publications. Les bibliothécaires sont fictifs, et leurs adresses utilisent le domaine réservé `.test`. Aucun email de réinitialisation ne peut donc partir vers une vraie personne.
- **Mot de passe démo** : 12 caractères minimum, lu depuis une variable d'environnement, jamais dans le code ni dans les livrables.

## Si le backend a évolué (prompts 1, 2, 13, 14)

`load.py` vise l'API actuelle. Après le prompt 1 (consentements à l'inscription, MFA admin) ou le
prompt 2 (nouveaux champs), adapte :
- `create_librarians()` pour l'inscription ;
- `validation_body()`, à compléter avec langue, type, DOI et licence. Ces valeurs sont déjà dans `verite_terrain`.
