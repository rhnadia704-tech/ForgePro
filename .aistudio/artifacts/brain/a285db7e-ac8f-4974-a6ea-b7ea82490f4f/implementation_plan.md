# ROM Forge Studio — Universal Dependency-Graph, Trust-Chain Signer, ART/DEX Regenerator & AI ROM Porter

Une station d'ingénierie système Android **100 % modulaire, agnostique et sans aucun traitement figé**, conçue pour extraire sans le moindre oubli l'intégralité du matériel (`/sdcard/PORTER/EXTRACT`), modéliser le graphe de dépendances complet (DAG), orchestrer le portage (avec maîtrise prioritaire **FOD / UDFPS**) via **Gemini 3.1 Pro (`ThinkingLevel.HIGH`)**, et garantir **zéro bootloop / zéro crash au boot** grâce à un moteur complet de **Chaîne de Confiance Cryptographique, de Régénération ART/DEX (`oat`, `odex`, `vdex`, `art`, `fsv_meta`) et de Réconciliation des XML de Signatures**.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Nouveaux Modules Critiques Intégrés (Zéro Oubli, Chaîne de Confiance & Régénération Post-Signature) :**
> 1. **Extraction & Portage "Zéro Fichier Oublié" (Recursive Transitive Closure)** : Le scanner parcourt récursivement chaque binaire, bibliothèque `.so` (`DT_NEEDED` multi-niveaux), firmware (`/vendor/firmware_mnt`, `/vendor/firmware`), fichier de configuration (`.xml`, `.conf`, `.kl`, `.idc`, `.json`), script `.rc`, apex, jar et APK jusqu'à atteindre un point fixe mathématique (0 dépendance externe non résolue) avant d'écrire dans `/sdcard/PORTER/EXTRACT` et d'injecter dans l'image cible.
> 2. **Moteur Intelligent de Signature & Chaîne de Confiance (`TrustChain & APK/APEX Signer`)** :
>    - **Préservation anti-corruption des APKs/JARs** : Alignement strict `zipalign` (4 octets, et **4096 octets / 4K page-aligned** pour `resources.arsc` non compressé et les bibliothèques natives `.so`), préservation ou re-signature cohérente par groupe de `sharedUserId` (`android.uid.system`, `android.uid.phone`, `android.uid.nfc`, `android.uid.bluetooth`) avec les paires de clés AOSP/Release (`platform`, `shared`, `media`, `networkstack`, `testkey`) pour éviter tout `INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES` ou crash de `PackageManagerService`.
>    - **Mise à jour synchronisée des XML et Fichiers de Signatures Système** : Analyse et mise à jour automatique des certificats X.509 hexadécimaux/SHA-256 dans `etc/selinux/plat_mac_permissions.xml`, `vendor/etc/selinux/vendor_mac_permissions.xml`, `etc/permissions/privapp-permissions-*.xml`, `etc/sysconfig/*.xml`, `etc/security/mac_permissions.xml` et tables `seapp_contexts` pour empêcher tout rejet SELinux au démarrage.
> 3. **Module de Régénération Post-Modification (`oat`, `odex`, `vdex`, `art`, `fsv_meta`, `vbmeta`)** :
>    - **Synchronisation ART / Dalvik (`dex2oat` & Checksums DEX)** : Dès qu'un APK ou JAR (`services.jar`, `framework.jar`, `SystemUI.apk`) est modifié ou re-signé, ses anciens fichiers pré-optimisés `.oat`, `.odex`, `.vdex` et `.art` (dans `oat/arm64/` et `/system/framework/arm64/boot-*.art`) deviennent invalides (mismatch CRC32/Adler32 DEX) et causent un crash fatal de `Zygote` / `SystemServer`. Le module **régénère les en-têtes/conteneurs `.vdex`, `.odex`, `.oat`, `.art`** avec les nouveaux checksums DEX ou nettoie proprement les artéfacts obsolètes pour forcer une recompilation `dex2oat` propre au premier boot sans corruption.
>    - **Régénération `fsv_meta` (FS-Verity), `fs_config`, `file_contexts` & `AVB 2.0`** : Recalcul intégral des arbres de hachage Merkle (`fsv_meta` / `.fsv_meta` pour les APKs système protégés par fs-verity), des pieds de page `avb_footer` (`add_hashtree_footer` dm-verity) et de l'image `vbmeta.img` avec désactivation/mise à jour des flags de vérification si nécessaire.

---

## 1. Overview & Core Concept

- **Pourquoi un GSI modifié ou mal re-signé plante au démarrage (et comment nous l'éliminons à 100 %)** :
  1. **Crash `Zygote` / `SystemServer` (Mismatch OAT/VDEX/ART)** : Si `services.jar` ou `SystemUI.apk` est patché (par ex. pour le FOD) ou re-signé sans mettre à jour `/system/framework/oat/arm64/services.odex` / `.vdex` ou `boot-framework.oat`, Android détecte une divergence entre le CRC du `classes.dex` dans l'archive et celui inscrit dans le `.vdex`/`.oat` et provoque un `RuntimeException` fatal. Notre **Module ART/OAT/VDEX Regenerator** recalcule et synchronise systématiquement ces fichiers.
  2. **Crash `PackageManagerService` (Rupture de la Chaîne de Confiance `sharedUserId` & `mac_permissions.xml`)** : Si on re-signe `SystemUI.apk` avec une clé différente de `framework-res.apk` alors qu'ils partagent `android.uid.system`, ou si la nouvelle clé publique n'est pas répertoriée dans `plat_mac_permissions.xml`, SELinux bloque l'application (`SELinuxMMAC` failure) et le téléphone reste bloqué sur le logo de démarrage. Notre **Trust-Chain Engine** cartographie tous les certificats du GSI unpacké avant toute opération et maintient une cohérence cryptographique absolue sur toute la chaîne.
  3. **Corruption `resources.arsc` / Page Alignment (Android 11–16)** : Android exige que `resources.arsc` et les `.so` non compressés soient alignés sur 4 octets (ou 4 Ko sur les kernels récents). Toute modification d'APK dans l'application applique un réalignement binaire strict.
  4. **Incohérence `fsv_meta` / FS-Verity & DM-Verity** : Les systèmes modernes vérifient les métadonnées fs-verity (`.fsv_meta`) et l'arbre de hachage AVB. Notre module régénère les descripteurs `fsv_meta` et le hashtree AVB 2.0 complet.

---

## 2. User Experience & Visual Design

### Flux Utilisateur & Commandes Principales

L'interface s'articule autour du **Workbench d'Ingénierie Système** et de ses modules spécialisés, sans aucun comportement figé :

1. **Sélecteur de Fichier & Bouton `EXTRACT IMG` (Unpack, Topology DAG & Trust-Chain Audit)** :
   - Ouvre n'importe quelle image (`.img` EXT4/EROFS, Sparse, `super.img`, `payload.bin`, `.xz`, `.lz4`, `.dat.br`).
   - Extrait l'arborescence complète, capture `fs_config` + `file_contexts`, construit le **Graphe de Dépendances Cible (`TargetSystemGraph`)** ET effectue l'**Audit de la Chaîne de Confiance du GSI** :
     - Inventorie les certificats de signature de chaque APK/APEX (`framework-res.apk`, `SystemUI.apk`, `Settings.apk`, overlays), les groupes `sharedUserId`, les entrées `plat_mac_permissions.xml`, l'état des fichiers `.oat`/`.odex`/`.vdex`/`.art` et les métadonnées `fsv_meta` / `avb_footer`.
2. **Bouton `EXTRACT DEVICE (ROOT)` (`Zero-Omission Recursive Extractor -> /sdcard/PORTER/EXTRACT`)** :
   - Exécute un scan récursif à **Fermeture Transitive Totale (Zéro Fichier Oublié)** sur toutes les partitions du téléphone (`/system`, `/system_ext`, `/product`, `/vendor`, `/odm`, `/sys`, `/dev`) et écrit tout dans **`/sdcard/PORTER/EXTRACT/`** :
     - Pour chaque sous-système découvert dynamiquement (en commençant par la chaîne complète **FOD / UDFPS**, Display/HBM, Biometrics, Audio, Camera, RIL/IMS, Sensors, Keymaster/Gatekeeper, Power, Thermal) :
       - Suit récursivement chaque bibliothèque ELF (`DT_NEEDED` niveau 1, 2, 3... jusqu'à `libc.so`), chaque firmware chargé par les blobs, chaque fichier XML/JSON/CONF référencé dans le binaire (extraction des chaînes de chemins internes), chaque script `.rc`, chaque fragment VINTF et chaque règle SELinux `.cil`.
     - Génère dans `/sdcard/PORTER/EXTRACT/` :
       - `proprietary-files.txt`, `Android.bp`, `BoardConfig.mk`, `device.mk` (sans aucun fichier manquant).
       - `topology_dag.json` (le graphe complet de l'arborescence et du mécanisme interne du téléphone).
       - `zero_omission_manifest.json` (rapport de vérification prouvant que 100 % des dépendances transitives ont été extraites).
       - `disassembly/` (code désassemblé : Smali/DEX, tables de symboles ELF, XML RRO décompilés, politiques SELinux `.cil`, scripts `.rc`, certificats et XML de signatures).
3. **Liste Déroulante des IMG Unpackés & Bouton `REPACK`** :
   - Permet de sélectionner une image unpackée, lance la vérification finale du graphe, régénère les tables `fs_config` / `file_contexts` et reconstruit l'image système finale (`.img` Raw/Sparse/EROFS/EXT4) sans aucune corruption d'inode.
4. **Bouton `PORTAGE` (Exécution du Plan IA + Régénération OAT/VDEX/FSV_META)** :
   - Transmet le graphe complet du téléphone (`/sdcard/PORTER/EXTRACT/topology_dag.json`) et celui de l'image unpackée à **Gemini 3.1 Pro (`ThinkingLevel.HIGH`)**.
   - Exécute le plan de restructuration dynamique (adaptation FOD/UDFPS, HALs, Overlays RRO, SELinux `.cil`, Init `.rc`, patchs Smali/DEX) tout en déclenchant automatiquement le **Regenerator Engine** sur chaque fichier touché (alignement APK 4K, synchronisation `.oat`/`.odex`/`.vdex`/`.art`, recalcul `fsv_meta` et mise à jour des XML de permissions/signatures).
5. **Bouton `SIGN` (Chaîne de Confiance Complète, XML Signatures, OAT/VDEX & AVB 2.0)** :
   - Exécute le pipeline de signature intelligente à 4 étages :
     1. **Signature & Alignement des APKs/JARs/Overlays** : Signe en v1/v2/v3/v4 avec préservation stricte de la clé `platform` / `shared` du GSI (ou rotation complète et coordonnée de toute la chaîne `sharedUserId`), compression `STORE` + alignement 4096 octets pour `resources.arsc` et `.so`.
     2. **Synchronisation des XML de Signatures & Permissions** : Met à jour automatiquement les empreintes `<signer signature="...">` dans `plat_mac_permissions.xml`, `keys.conf`, `privapp-permissions-*.xml` et `default-permissions/*.xml`.
     3. **Régénération ART / DEX (`oat`, `odex`, `vdex`, `art`) & `fsv_meta`** : Met à jour les sommes de contrôle DEX dans les fichiers `.vdex`/`.oat`/`.odex` (ou reconstruit les conteneurs d'optimisation ART) et régénère les descripteurs `.fsv_meta` (FS-Verity).
     4. **Signature de Partition AVB 2.0 (`avbtool` & `vbmeta`)** : Recalcule l'arbre de hachage dm-verity (`add_hashtree_footer`) et génère l'image `vbmeta_patched.img` correspondante.
6. **Bouton Maître `MAKE` (Pipeline Automatique Intégral A-to-Z — Zéro Bootloop)** :
   - Enchaîne de manière 100 % autonome :
     1. **Unpack & Désassemblage Extrême** du GSI + construction du graphe cible et audit de sa chaîne de confiance.
     2. **Extraction Zéro-Oubli du Téléphone** dans `/sdcard/PORTER/EXTRACT` par fermeture transitive complète + désassemblage extrême.
     3. **Planification Architecturale par Gemini 3.1 Pro (`ThinkingLevel.HIGH`)** : Analyse de l'arborescence et du mécanisme interne complet (focus FOD + sous-systèmes matériels + chaîne de signatures), génération du plan de restructuration respectant 100 % du graphe de dépendances.
     4. **Portage & Restructuration Modulaire** dans le GSI unpacké.
     5. **Régénération `oat` / `odex` / `vdex` / `art` / `fsv_meta` + Synchronisation des XML de Signatures (`mac_permissions.xml`, `privapp-permissions`)**.
     6. **Re-signature Chaîne de Confiance Complète + Repack + Signature AVB 2.0**, produisant une ROM spécifique prête à démarrer sans aucun bootloop ni crash d'application système.

---

## 3. Key Product Decisions & Trade-Offs

- **Décision 1 : Fermeture Transitive Récursive pour "Zéro Fichier Oublié"**
  - *Approche choisie* : Lors de l'extraction dans `/sdcard/PORTER/EXTRACT`, l'application ne s'arrête pas aux fichiers de premier niveau. Elle ouvre chaque binaire `.so` / exécutable, lit ses entrées `DT_NEEDED` ET scanne sa section `.rodata` pour détecter tous les chemins de fichiers codés en dur (`/vendor/etc/...`, `/vendor/firmware/...`, `/sys/class/...`, `/dev/...`), puis ajoute récursivement ces fichiers à la file d'extraction jusqu'à ce qu'aucun fichier dépendant ne manque.
- **Décision 2 : Gestion Non-Destructive de la Chaîne de Confiance & Régénération ART (`oat`/`vdex`/`fsv_meta`)**
  - *Approche choisie* : Pour éviter de briser la chaîne de confiance interne du GSI unpacké, le moteur analyse d'abord les certificats existants de `framework-res.apk` et `SystemUI.apk`. Lorsqu'un APK système est modifié ou qu'un nouvel Overlay RRO (comme l'overlay FOD) est créé, le moteur harmonise les signatures sur l'ensemble du groupe `sharedUserId`, met à jour `plat_mac_permissions.xml` avec le certificat exact, régénère les en-têtes/fichiers `.vdex`, `.odex`, `.oat`, `.art` et `.fsv_meta` pour éviter tout rejet par `Zygote` ou `fs-verity`, puis signe la partition avec AVB 2.0.

---

## 4. Technical Architecture & Data Strategy

### Architecture & Component Diagram

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                         ROM FORGE STUDIO UI (Jetpack Compose)                        │
│ ┌────────────────────────┐ ┌──────────────────────────┐ ┌──────────────────────────┐ │
│ │   Workbench & MAKE     │ │  Dependency DAG & Zero-  │ │  Trust-Chain, ART/VDEX   │ │
│ │   Control Center       │ │  Omission /PORTER/EXTRACT│ │  & AI Restructuring Lab  │ │
│ │ • File Picker +        │ │ • Recursive ELF/Firmware/│ │ • Gemini 3.1 Pro (HIGH)  │ │
│ │   [EXTRACT IMG]        │ │   Config/HAL Closure     │ │   System Mechanism Plan  │ │
│ │ • Dropdown + [REPACK]  │ │ • Dynamic AOSP Tree in   │ │ • APK/SharedUserId/XML   │ │
│ │ • [EXTRACT DEVICE]     │ │   /sdcard/PORTER/EXTRACT │ │   Signature Synchronizer │ │
│ │ • [PORTAGE] & [SIGN]   │ │ • Interactive Mechanism  │ │ • OAT/ODEX/VDEX/ART &    │ │
│ │ • [MAKE (Full A-to-Z)] │ │   & Disassembly Browser  │ │   fsv_meta/AVB Generator │ │
│ └───────────┬────────────┘ └────────────┬─────────────┘ └────────────┬─────────────┘ │
└─────────────┼───────────────────────────┼────────────────────────────┼───────────────┘
              │                           │                            │
              ▼                           ▼                            ▼
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                           RomForgeViewModel (StateFlow / MVVM)                       │
└─────────┬───────────────────────┬──────────────────────────┬─────────────────────────┘
          │                       │                          │
          ▼                       ▼                          ▼
┌─────────────────────┐ ┌────────────────────────┐ ┌───────────────────────────────────┐
│ UniversalImageEngine│ │ ZeroOmissionExtractor  │ │ TrustChainAndArtRegenerator       │
│ & Disassembler      │ │ & SystemTopologyDAG    │ │ & AiRestructuringOrchestrator     │
│ • EXT4/EROFS/Sparse/│ │ (/sdcard/PORTER/EXTRACT│ │ • Gemini 3.1 Pro (Thinking HIGH)  │
│   Payload/Super/XZ  │ │ • Recursive DT_NEEDED  │ │ • OAT/ODEX/VDEX/ART & boot.art    │
│ • Preserves Exact   │ │   & .rodata Path Scan  │ │   Header/Container Regenerator    │
│   fs_config &       │ │ • Full Disassembly of  │ │ • fsv_meta (FS-Verity) & AVB 2.0  │
│   file_contexts     │ │   FOD & All Subsystems │ │ • plat_mac_permissions.xml &      │
│                     │ │                        │ │   privapp-permissions XML Sync    │
└─────────┬───────────┘ └───────────┬────────────┘ └──────────────┬────────────────────┘
          │                         │                             │
          └─────────────────────────┼─────────────────────────────┘
                                    ▼
┌──────────────────────────────────────────────────────────────────────────────────────┐
│   Room Database (UnpackedImages, DAGNodes, TrustCertificates, ArtArtifacts, AIPlans) │
│   + Physical Storage (/sdcard/PORTER/EXTRACT & /sdcard/PORTER/MAKE_OUTPUT)           │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

### Détail du Pipeline Anti-Bootloop, Signature & Régénération (`SIGN` & `MAKE`)

1. **Audit & Résolution du Graphe de Dépendances (0 Fichier Oublié)** :
   - Vérifie que chaque bibliothèque `.so` injectée possède 100 % de sa chaîne `DT_NEEDED` dans le namespace linker (`ld.config.<vndk>.txt`), que chaque firmware référencé dans `.rodata` est présent, et que chaque service `.rc` a son domaine SELinux et son interface VINTF déclarés.
2. **Synchronisation des Certificats & XML de Signatures (`mac_permissions.xml` & `privapp-permissions`)** :
   - Parse `/system/etc/selinux/plat_mac_permissions.xml` (et `/system_ext`, `/product`), extrait les balises `<signer signature="..."><seinfo value="platform"/></signer>`, vérifie la correspondance avec les certificats X.509 des APKs modifiés ou injectés, et met à jour les entrées XML ainsi que `/system/etc/permissions/privapp-permissions-platform.xml` pour empêcher tout arrêt de `SystemServer` pour permission privilégiée manquante (`Signature|Privileged` enforcement).
3. **Régénération des Artéfacts d'Exécution (`oat`, `odex`, `vdex`, `art`, `fsv_meta`)** :
   - Pour chaque archive bytecode modifiée (`services.jar`, `framework.jar`, `SystemUI.apk`, etc.), met à jour les structures `.vdex` (en-tête VerifierDeps & checksums DEX), `.odex` / `.oat` (en-tête ELF OAT et tables de classes) et `.art` (`boot.art`), et régénère les fichiers `.fsv_meta` (descripteurs Merkle tree FS-Verity) afin que le noyau et le runtime ART acceptent les fichiers dès la première milliseconde du boot.
4. **Alignement Binaire & Signature AVB 2.0** :
   - Réaligne les archives APK sur frontière de page 4 Ko (`4096 bytes`), applique la table `fs_config` (UID/GID/Capabilities) et `file_contexts` SELinux sur chaque nouveau fichier créé (`.oat`, `.vdex`, `.fsv_meta`, `.apk`, `.so`, `.rc`, `.cil`), reconstruit l'image système et applique la signature `avbtool` (`vbmeta` + hashtree footer).
