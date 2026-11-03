# Journal des versions

Tout ce qui change dans **Fold Launcher Pro**, build par build. La version la plus récente est en haut.
Chaque build se télécharge dans les [releases](https://github.com/yuksel3803/DuoLauncherv2/releases), et le launcher propose lui-même d'installer les nouvelles (à partir de la build 17).

## Build 39

- **Page bloquée à moitié corrigée** : survoler une pastille de page lançait le changement de page, mais un petit mouvement du doigt l’annulait en cours de route. Les changements de page vont maintenant toujours jusqu’au bout.
- **Plus d’icône perdue** : pendant qu’une page tourne, il n’y a plus de case cible ; on lâche une fois la page arrivée (la cible se met à jour même si le doigt ne bouge plus). Si on lâche quand même en route, la page se recale.
- **Pastilles de pages mieux placées** : sur l’écran externe, elles apparaissent dans la zone de l’horloge au lieu de cacher la dernière rangée d’icônes.

## Build 38

- **Écran Récents de Fold Launcher Pro** : l’écran des applis récentes de Samsung n’est disponible qu’avec One UI Home ; avec un autre launcher, One UI affiche une version de secours (vignettes écrasées). Fold Launcher Pro a maintenant son propre écran Récents : grandes cartes des dernières applis (heure d’utilisation), toucher pour ouvrir, « À côté de… » pour l’écran partagé avec l’appli précédente, × pour en retirer une, « Tout fermer » pour libérer la mémoire. Il s’ouvre par un geste (Réglages → Gestes → « Applis récentes ») ou par le bouton « Récents » du dock dans les applis. Demande l’accès aux données d’utilisation.
- **Changer de page en déplaçant une icône, encore plus simple** : de grosses pastilles numérotées apparaissent en bas pendant le déplacement (1, 2, 3… et « + » pour une nouvelle page) ; survoler une pastille tourne directement sur cette page.

## Build 37

- **Bouton « Modifier » réparé** : le fondu du haut de la page Aujourd’hui rognait le bouton. Le titre et le bouton sont maintenant fixes au-dessus des cartes, qui défilent seules en dessous.

## Build 36

- **Bloc noir sous la note corrigé** : la page Aujourd’hui était réduite de toute la hauteur du clavier, alors que le dock en cache déjà une partie ; elle est maintenant réduite seulement de ce que le clavier recouvre vraiment.
- **Changer de page en déplaçant une icône** : bande de bord deux fois plus large (56 dp), première page tournée après 0,4 s au lieu de 0,6 s, bords lumineux pendant le déplacement et petite vibration à chaque page.
- **Dock de l’écran externe** : son cadre épouse ses icônes au lieu de prendre toute la largeur.
- **Page Aujourd’hui sur l’écran externe** (au choix, Réglages → Accueil → Pages) : tes widgets sur une page à gauche de l’accueil.

## Build 35

- **Page Aujourd’hui sans coupure** : le bas de la colonne s’estompe en douceur au lieu d’être coupé net au-dessus du dock, avec un peu de marge pour remonter entièrement la dernière carte.
- **Pastilles des applis récentes du dock** : elles ne sont plus rognées (les icônes récentes étaient semi-transparentes, ce qui coupait tout ce qui dépassait).

## Build 34

- **Écrire dans la note sans être caché par le clavier** : la page Aujourd’hui se raccourcit au-dessus du clavier et fait remonter la carte en cours (Note rapide, Rappels). La grille d’applis, elle, ne bouge pas.

## Build 33

- **Poignée du dock à gauche ou à droite** : Réglages → Accueil → Dock → « Côté de la poignée ». Le dock s’ouvre du même côté.

## Build 32

- Le texte « Appui long : à côté » est retiré du dock dans les applis (l’appui long ouvre toujours l’appli à côté).

## Build 31

- **Écran partagé, nouvelle méthode** : la commande d’écran partagé d’Android utilisée par la build 30 n’est plus prise en charge par les versions récentes d’Android. Maintenant, une courte fenêtre de Fold Launcher Pro ouvre l’appli choisie « à côté » (ce qu’Android accepte depuis une vraie fenêtre), puis rouvre l’appli en cours dans sa moitié et se ferme. Utilisée par le dock dans les applis (appui long) et par la zone « Écran partagé » de l’accueil.
- Le service d’accessibilité retient seulement le nom de l’appli au premier plan, pour savoir laquelle garder.

## Build 30

- **Écran partagé qui marche depuis le dock dans les applis** : Android refusait d’ouvrir une appli « à côté » depuis la poignée (elle s’ouvrait en plein écran). Maintenant, l’appli en cours est d’abord mise en écran partagé par l’action d’Android, puis l’appli choisie s’ouvre dans l’autre moitié.
- Demande le service d’accessibilité, renommé « Fold Launcher Pro – gestes et écran partagé » (il ne lit pas l’écran). La zone « Écran partagé » de l’accueil utilise la même méthode quand il est activé.

## Build 29

- **Dock dans les applis** (Réglages → Accueil → Dock) : une fine poignée irisée au bord droit de l’écran, visible seulement dans les applis. Touche-la : le dock s’ouvre par-dessus l’appli. Touche une appli pour l’ouvrir, maintiens-la pour l’ouvrir à côté en écran partagé. La poignée se déplace verticalement en la faisant glisser. Demande l’autorisation « Afficher par-dessus les autres applis ».
- **Widgets qui défilent** : les listes des widgets Android (Google Tasks, Gmail, Agenda…) défilent dans la page Aujourd’hui ; la colonne ne leur vole plus le geste.

## Build 28

- **Option « appli d’accueil » retirée** : plus de bandeau « Faire de Fold Launcher Pro l’appli d’accueil » en bas de l’accueil, ni de ligne dans Réglages → Paramètres. (L’accueil guidé du tout premier lancement la propose encore une fois.)

## Build 27

- **Retirer une pastille à la main** : appui long sur une appli → « Retirer la pastille ». Le chiffre ou les bulles en orbite disparaissent jusqu’au prochain nouveau message ; les notifications restent dans le volet.

## Build 26

- **Créer un dossier plus facilement** : lâcher une appli n’importe où sur l’icône d’une autre (avec une marge) crée le dossier ; avant, il fallait viser le centre exact de l’icône, sinon l’appli était seulement déplacée à côté.
- Une petite vibration accompagne l’anneau irisé quand lâcher créera un dossier.

## Build 25

- **Bandeau « Déplacé » retiré** : déplacer une appli ne fait plus apparaître « Annuler » en bas de l’écran. Il reste seulement quand on retire une appli de l’accueil.

## Build 24 — Style et raccourcis

- **Styles d’horloge** (Accueil → Horloge) : Fine, Grasse, Aiguilles (cadran holographique) ou En lettres (« quatorze heures douze », « midi vingt »).
- **Météo vivante** (Thèmes → Fond) : pluie, neige, orage avec un éclair discret, ou brouillard, dessinés sur le fond selon la météo ; s’arrête hors de l’accueil et avec l’économie de batterie.
- **Fond d’écran animé** (Thèmes → Fond → Appliquer) : les halos holographiques sur l’écran de verrouillage, dans la palette choisie, qui dérivent lentement et s’arrêtent quand l’écran est éteint.
- **Coffre secret** : appui long sur une appli → « Mettre au coffre secret ». Les applis disparaissent de l’accueil, du dock, de la liste, de la recherche et des suggestions ; on ouvre le coffre avec le geste « Coffre secret » (Réglages → Gestes) puis l’empreinte.
- **Ranger une page** : appui long sur une case vide → « Ranger cette page » par couleur, catégorie, utilisation ou nom. Les grands dossiers restent en place ; « Annuler » remet l’ordre d’avant.
- **Réponse rapide** : appui long sur une appli qui a un message non lu → champ « Répondre » sous l’aperçu, envoyé par l’appli elle-même (WhatsApp, Messages, Telegram…).
- **Dock qui se cache** (Accueil → Dock, écran interne) : il glisse vers le bas pendant que tu changes de page et revient aussitôt après.
- **Mises à jour toutes les 6 heures** par défaut (Build 23), réglable.

## Build 23 — Mises à jour économes

- **Vérification toutes les 6 heures** par défaut, à l’accueil comme en arrière-plan, pour épargner la batterie (la build 21 vérifiait toutes les 5 à 15 minutes).
- **Fréquence réglable** : toutes les heures, toutes les 6 h ou une fois par jour (Réglages → Paramètres → Mises à jour). « Vérifier » reste disponible à tout moment.

## Build 22

- **Rangée « Ce matin / Ce midi… » retirée** : les applis habituelles de l’heure ne s’affichent plus sous l’horloge. Il ne reste que les suggestions de situation (voiture, casque, rendez-vous proche, avant de dormir), désactivables dans Réglages → Accueil → Suggestions.

## Build 21 — Mises à jour immédiates

- **Fenêtre de mise à jour sans attendre** : le launcher regarde sur GitHub à chaque retour sur l’accueil, puis toutes les 5 minutes tant qu’il est affiché (avant : une fois toutes les 6 heures, d’où l’absence de fenêtre).
- **Notification en arrière-plan** : toutes les 15 minutes, même dans une autre appli, une notification « Fold Launcher Pro — build N disponible » ; la toucher ouvre directement la fenêtre de mise à jour. L’autorisation des notifications est demandée une fois.
- Vérifications plus légères : GitHub répond « rien de nouveau » sans renvoyer la liste quand elle n’a pas changé.

## Build 20 — Aperçu fidèle

- L’aperçu des réglages reproduit exactement la première page de ton accueil : même grille, cases vides conservées, dossiers et grands dossiers à leur place et à leur taille, aux vraies proportions de chaque écran.

## Build 19 — Réglages repensés

- **Fond opaque** : l’écran d’accueil ne transparaît plus derrière les réglages (ni derrière la recherche et les autres panneaux).
- **Recherche dans les réglages** : « dock », « couleurs », « batterie », « miroir »… mène directement à la bonne section.
- **Une page par catégorie** : toutes les sections d’une catégorie se suivent dans un seul défilement ; les pastilles en haut sautent à une section et suivent le défilement.
- **Aperçu plus compact** et vivant : la vraie heure, la palette et la grille actuelles, avec le choix « Externe » ou « Interne » (page Aujourd’hui, grille et dock de l’écran déplié).
- **Écran externe** : les catégories passent en pastilles en haut pour laisser toute la hauteur aux réglages.

## Build 18 — Un launcher qui s’adapte

**Pliage**
- **Continuité** : en dépliant ou en repliant, l’autre écran s’ouvre sur la page qui contient les mêmes applis (et la page Actus si tu y étais).
- **Écran externe miroir** : replié depuis une page de l’écran interne que l’écran externe n’a pas, ses applis s’affichent dans une fenêtre sur l’écran externe, le temps d’en ouvrir une.
- **Mode chevalet** : à moitié plié et tourné comme un petit ordinateur (capteur de charnière), l’écran du haut montre l’heure et la musique, celui du bas de grosses commandes, le prochain rendez-vous et le dock.

**Plus intelligent**
- **Suggestions sous l’horloge** : en voiture (Bluetooth de la voiture) Maps et musique ; casque branché, musique et vidéo ; rendez-vous avec une adresse dans l’heure, Maps ; le soir en charge, le réveil ; sinon les applis habituelles de ce moment. Mode « Toujours », « Seulement en situation » ou « Jamais ».
- **Actions dans la recherche** : minuteur, alarme, rappel (ajouté à la carte Rappels), conversions d’unités (distance, poids, volume, vitesse, température, données, durée), devises (taux de la BCE), appeler ou écrire à un contact. Entrée lance la première action.
- **Pastille « Nouveau »** : une perle verte sur les applis installées dans la semaine, jusqu’à leur première ouverture.

**Ton style**
- **Palettes de couleurs** : Holographique, Fond d’écran (couleurs tirées de ton fond d’écran, mises à jour quand il change), Aurore, Océan, Braise, Menthe.
- **Ambiance jour et nuit** : plus lumineux le jour, noir profond la nuit, selon le lever et le coucher du soleil de ta ville.
- **Halos vivants** : aube rose, midi bleu, crépuscule orangé, nuit violette ; ils pâlissent quand la batterie baisse.
- **Effets de changement de page** : Classique, Profondeur, Cube, Fondu, Carrousel.

**Nouveaux gestes et pastilles**
- **Dessiner pour lancer** : enregistre une forme par appli (un W pour WhatsApp…), puis trace-la sur le tableau ouvert par un geste (deux doigts vers le haut par défaut s’il est libre).
- **Notifications en orbite** : les messages non lus deviennent des bulles qui tournent autour de l’icône, plus vite quand il y en a beaucoup.

**Page Aujourd’hui**
- **Prochain trajet** : le prochain rendez-vous avec une adresse et le bouton « Itinéraire ».
- **Compte à rebours** : J-12 avant les vacances, un anniversaire…
- **Souvenir du jour** : une photo prise à cette date les années précédentes.

Tout se règle dans Réglages : Thèmes → Couleurs et Animations, Accueil → Suggestions et Pliage, Gestes → Dessins, Widgets → Notifications.

## Build 17 — Optimisation et mises à jour intégrées

- **Mises à jour intégrées** : une fenêtre s'affiche quand une nouvelle version sort, avec les nouveautés et un bouton « Mettre à jour ». Réglages → Paramètres → Mises à jour pour vérifier à la main, désactiver la vérification automatique ou ouvrir ce journal.
- **Démarrage plus rapide** : les icônes se chargent en parallèle sur tous les cœurs, et le code est précompilé à l'installation (profil de démarrage).
- **Moins de mémoire** : les icônes vivent dans la mémoire graphique (environ 25 Mo de moins pour 150 applis), défilement plus fluide.
- **Batterie** : météo, agenda, interrupteurs, horloge et capteur de parallaxe s'arrêtent quand une autre appli est au premier plan, et se rafraîchissent dès le retour sur l'accueil.
- **Fond holographique** : les halos bougent sans recalculer l'écran ; les dégradés des bulles sont mis en cache.
- **Historique des lancements** gardé en mémoire au lieu d'être relu à chaque fois (dock des récents, suggestions).
- **Geste retiré** : glisser vers le haut en partant d'une icône n'ouvre plus l'appli ; ça ouvre la liste des applis comme partout ailleurs.
- Le menu de l'accueil affiche « Réglages » au lieu de « Réglages de Pli ».

## Build 16 — Fold Launcher Pro

- L'application s'appelle désormais **Fold Launcher Pro** (icône, accueil guidé, réglages, sauvegardes).
- Page de présentation du projet sur GitHub.

## Build 15

- Accueil guidé au premier lancement et guide des gestes.
- La recherche apprend les applis que tu ouvres le plus.
- Espace de stockage de chaque appli dans son menu.
- Widgets empilés (plusieurs au même endroit).
- Économie de batterie automatique sous 20 %.

## Build 14

- Animation d'ouverture holographique des applis.
- Zone « Écran partagé » sur l'écran interne.
- Widgets Rappels et Temps d'écran.
- Interrupteur des vibrations.

## Build 13

- Réglages repensés : catégories, sous-sections et aperçu en direct de l'accueil.

## Build 12

- Widget d'interrupteurs rapides.
- Dossiers grand format (2×2, toute la largeur…) qui ouvrent l'appli en un appui.
- Formes d'icônes.
- Recherche Google / Finder à droite de l'horloge.

## Build 11

- Copier la disposition d'un écran à l'autre, renommer les applis, icônes personnalisées.
- Recherche élargie, applis récentes dans le dock interne.
- Verrouillage de la disposition, applis protégées par empreinte, bouton Annuler.

## Builds 6 à 10

- Placement libre dans n'importe quelle case, ajout et suppression de pages.
- Widgets réservés à l'écran interne.
- Corbeille pendant le déplacement, sélection multiple.
- Barres de corbeille et de sélection en haut de l'écran.
- Ajout de plusieurs applis d'un coup.

## Builds 1 à 5

- Nouveau launcher holographique pour Galaxy Z Fold, séparé de Duo Launcher.
- Grille réglable par écran, dock interne jusqu'à 10 applis.
- Page d'actualités tout à gauche (à la place de Google Discover).
- Packs d'icônes, icônes teintées, gestes personnalisables, sauvegarde de la disposition.
- Glisser vers le bas pour fermer la liste des applis et la recherche.
