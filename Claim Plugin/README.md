# ZBenClaims – Chunk-Ansprüche mit Rängen

## Features
- Chunk-basierte Claims mit Limit pro Rang und Trust-Liste.
- Ränge mit eigenen Prefixen (Tab, Chat, Nametag) und Claim-Limits.
- Rank-Editor direkt im Chat über `/ranks` (erstellen, umbenennen, Eigenschaften & Flags).
- Inventar-/Kisten-Autosortierung per Mausrad (konfigurierbar, optional mit Permission).
- Feste TAB-Branding-Texte (Header/Footer) beim Join und nach Reload.
- Schutz-Flags (z. B. `interact_protected`) pro Claim mit Standardwerten aus der Config.
- Admin- und Bypass-Unterstützung für Moderation.

## Quickstart
1. Plugin in den `plugins/`-Ordner legen und Server starten.
2. Stehe im gewünschten Chunk und führe `/claim` aus.
3. Vertraue Mitspieler mit `/trust <name>` oder entferne sie mit `/untrust <name>`.
4. Claims mit `/claims` einsehen und mit `/unclaim` freigeben.

## Installation
- **Voraussetzungen:** Paper/Spigot 1.21, Java 17 oder neuer.
- **Build:** Gradle-Projekt, `./gradlew build` erzeugt das Jar unter `build/libs`.
- **Installation:** Jar in den `plugins/`-Ordner kopieren, Server neu starten.

## Konfiguration (Auszug)
- **database.file:** Dateiname der SQLite-Datenbank (`config.yml`).
- **cache.preloadAllClaims:** Claims beim Start vorladen (`config.yml`).
- **claims.disabledWorlds:** Welten, in denen Claims deaktiviert sind (`config.yml`).
- **flags.default.interact_protected:** Standard-Flag pro Claim (`config.yml`).
- **ranks.list:** Verfügbare Ränge mit `priority`, Claim-`limit`, Prefixen, Kosten & Flags (`config.yml`).
- **ranks.mode:** Rangbestimmung (z. B. `auto`) (`config.yml`).
- **autosort.*:** Steuerung der Mausrad-Sortierung (Inventar/Kisten, Hotbar, Permission) (`config.yml`).
- **tablist.branding.*:** Header/Footer für die TAB-Liste (`config.yml`).
- **messages.yml:** Alle deutschen Meldungen, inkl. Rank- und Autosort-Texte.

## Commands
| Command | Beschreibung | Permission | Aliases |
| --- | --- | --- | --- |
| `/claim` | Aktuellen Chunk claimen. | `zbenclaims.use` | – |
| `/unclaim` | Eigenen Claim im Chunk entfernen. | `zbenclaims.use` | – |
| `/trust <spieler>` | Spieler im aktuellen Claim vertrauen. | `zbenclaims.use` | – |
| `/untrust <spieler>` | Vertrauen entziehen. | `zbenclaims.use` | – |
| `/claims [seite]` | Liste eigener Claims. | `zbenclaims.use` | – |
| `/zbenclaims reload` | Config neu laden. | `claim.admin` | – |
| `/rank <set|get|list> ...` | Spieler-Ränge setzen/abfragen. | `claim.admin` | – |
| `/ranks ...` | Rank-Definitionen listen/bearbeiten. | `claim.admin` (Lesen ohne Rechte) | – |

## Permissions
- `zbenclaims.use` – Allgemeine Nutzung der Spielerbefehle (Default: true).
- `zbenclaims.admin` – Admin-Aktionen inkl. Reload (Default: op).
- `zbenclaims.admin.bypass` – Schutzmechaniken ignorieren (Default: op).
- `zbenclaims.admin.rank` – Rangverwaltung (Default: op).

## Integrationen
- Andere Plugins können anhand der Ränge Prefixe anzeigen (Tab/Chat/Nametag-Konfiguration in `config.yml`).
- ZBenEnchants erkennt optional die Anwesenheit von ZBenClaims (softdepend) und kann Claims berücksichtigen.

## Beispiele
- Stadtviertel schützen: Spieler rangbasiert auf `VIP` setzen, damit mehr Claims verfügbar sind.
- Community-Plot: Owner claimt Chunk, vertraut Bauteam mit `/trust` und nutzt Prefixe für Rollenkennzeichnung.
- Cleanup: Admin prüft Claims via `/claims` und gibt ungenutzte Flächen mit `/unclaim` frei.

## Troubleshooting
- **Meldung „Claim-Limit erreicht“:** Rang-Limit in `config.yml` erhöhen oder Rang des Spielers anpassen.
- **Claimen nicht möglich:** Welt ggf. in `claims.disabledWorlds` deaktiviert oder Chunk nicht geladen.
- **Kein Präfix sichtbar:** Tab/Chat/Nametag-Prefixe pro Rang in `config.yml` prüfen.

