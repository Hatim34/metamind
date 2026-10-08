import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '../core/i18n';
import { BackService } from '../core/back.service';

/** Pages légales (N5). Texte juridique en français ; titres traduits. */
@Component({
  standalone: true,
  imports: [RouterLink, TranslatePipe],
  template: `
    <div class="m-wrap m-wrap--narrow m-page m-prose-page">
      <h1 class="m-title">{{ 'Informations légales' | t }}</h1>

      <section id="confidentialite">
        <h2 class="m-h3">{{ 'Confidentialité' | t }}</h2>
        <p>Metamind est soumise au Règlement général sur la protection des données (Règlement UE 2016/679, RGPD) et à la loi belge du 30 juillet 2018. L'autorité de contrôle est l'Autorité de protection des données (APD).</p>
        <h3 class="m-h4">Données traitées et finalités</h3>
        <ul>
          <li>Email, nom, prénom, institution : création et gestion des comptes.</li>
          <li>Mot de passe (haché avec bcrypt) : authentification.</li>
          <li>Journaux d'audit (actions, horodatage) : traçabilité et sécurité.</li>
          <li>Noms d'auteurs et métadonnées : description des publications.</li>
        </ul>
        <p>La base légale est l'exécution du contrat de service et l'intérêt légitime (sécurité). Le traitement respecte les principes de licéité, finalité, minimisation, exactitude et limitation de conservation.</p>
        <h3 class="m-h4">Vos droits</h3>
        <p>Vous disposez des droits d'accès, de rectification, d'effacement, de limitation, d'opposition et de portabilité. La rectification se fait depuis votre profil ; l'export de vos données et la demande de suppression sont accessibles depuis la page Profil. Une demande est traitée dans un délai d'un mois. En cas de désaccord, vous pouvez saisir l'APD (autoriteprotectiondonnees.be).</p>
        <h3 class="m-h4">Sécurité et conservation</h3>
        <p>Les communications sont chiffrées (HTTPS/TLS), les mots de passe hachés (bcrypt), l'accès contrôlé par rôles. La suppression courante est logique afin de préserver la traçabilité ; sur exercice fondé du droit à l'oubli, les données personnelles sont anonymisées ou supprimées. Une violation de données est notifiée à l'APD dans les 72 heures.</p>
      </section>

      <section id="mentions-legales">
        <h2 class="m-h3">{{ 'Mentions légales' | t }}</h2>
        <p>Metamind est une plateforme développée dans le cadre de l'épreuve intégrée du Bachelier en Informatique de l'Institut des Carrières Commerciales (ICC), Bruxelles, par ASSAL Hatim (2025-2026).</p>
        <p>Pour les comptes utilisateurs, Metamind agit comme responsable de traitement. Pour les documents déposés par les institutions, Metamind agit comme sous-traitant de l'institution, qui reste responsable de ses données. En tant qu'hébergeur, Metamind n'exerce pas de surveillance générale des contenus mais retire promptement tout contenu manifestement illicite qui lui est signalé.</p>
      </section>

      <section id="cgv">
        <h2 class="m-h3">{{ 'Conditions générales de vente' | t }}</h2>
        <ul>
          <li>Le prix total, tous frais compris, est affiché avant la commande.</li>
          <li>Le paiement est délégué à Stripe, prestataire certifié PCI-DSS ; les données bancaires ne sont jamais stockées par Metamind.</li>
          <li>Les crédits achetés servent à l'extraction de métadonnées ; une extraction échouée ne consomme aucun crédit.</li>
          <li>Le service numérique étant exécuté immédiatement, l'acheteur donne son accord exprès à l'exécution immédiate et reconnaît perdre son droit de rétractation de 14 jours.</li>
        </ul>
      </section>

      <section id="ia">
        <h2 class="m-h3">{{ 'Comment nous utilisons l\\'IA' | t }}</h2>
        <p>Un modèle de langage propose les métadonnées (titre, auteurs, résumé, mots-clés, date, langue, type, classification) à partir du texte du document. Aucune notice n'est publiée sans la relecture et la validation d'un bibliothécaire de l'institution, qui peut tout corriger. Les métadonnées publiées portent la mention de cette assistance.</p>
      </section>

      <button type="button" class="m-btn m-btn--ghost" (click)="back.back('/catalogue')">← {{ 'Retour' | t }}</button>
    </div>
  `
})
export class LegalPage {
  readonly back = inject(BackService);
}

@Component({
  standalone: true,
  imports: [RouterLink, TranslatePipe],
  template: `
    <div class="m-wrap m-page m-empty">
      <h1 class="m-title">{{ 'Page introuvable' | t }}</h1>
      <a class="m-btn m-btn--ghost" routerLink="/">{{ 'Retour à l\\'accueil' | t }}</a>
    </div>
  `
})
export class NotFoundPage {}
