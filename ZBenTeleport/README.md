# ZBenTeleport – TPA, Homes, Warps & RTP

## Features
- TPA-System mit Anfragen, Akzeptieren/Ablehnen, Blocklisten und Toggle.
- Homes mit Limits (per Permission erweiterbar) und Schnellzugriff auf letzte Position (`/back`).
- Warps setzen/löschen/listen; optional zufälliger Teleport (RTP) mit Welt-Whitelist.
- Cooldown-Management pro Aktion und persistente Speicherung (SQLite).

## Quickstart
1. Jar in `plugins/` legen und Server starten.
2. Config (`config.yml`) für Cooldowns, Home-Limits und RTP-Welten prüfen.
3. Befehle nutzen: z. B. `/tpa Spieler`, `/tpaccept`, `/sethome`, `/home`, `/back`, `/warp Spawn`.

## Installation
- **Voraussetzungen:** Paper/Spigot 1.21, Java 17+.
- **Build:** `./gradlew build` erzeugt das Jar unter `build/libs`.
- **Installation:** Jar in `plugins/` kopieren, Server neu starten.

## Konfiguration (`config.yml`)
| Schlüssel | Bedeutung | Standard |
| --- | --- | --- |
| `cooldowns.tpa` | Cooldown für TPA (ms). | 5000 |
| `cooldowns.back` | Cooldown für `/back` (ms). | 3000 |
| `cooldowns.home` | Cooldown für Homes (ms). | 1000 |
| `cooldowns.rtp` | Cooldown für RTP (ms). | 30000 |
| `homes.defaultLimit` | Basis-Anzahl an Homes. | 1 |
| `homes.permissionLimits` | Zusätzliche Limits je Permission (z. B. `zbenteleport.homes.5`). | siehe Config |
| `back.prefer-death` | Bei Tod zuerst Todesposition nutzen. | true |
| `tpa.timeoutSeconds` | Ablaufzeit für Anfragen. | 60 |
| `tpa.replaceExisting` | Neue Anfragen ersetzen ältere. | true |
| `rtp.enabled` | RTP global aktivieren. | true |
| `rtp.minRadius`/`maxRadius` | Suchradius rund um Spieler. | 500 / 5000 |
| `rtp.maxAttempts` | Versuche für sichere Position. | 30 |
| `rtp.worldsAllowed` | Leere Liste = alle Welten erlaubt. | [] |

## Commands
| Command | Beschreibung | Permission | Aliases |
| --- | --- | --- | --- |
| `/tpa <spieler>` | Teleport-Anfrage senden. | `zbenteleport.tpa` | – |
| `/tphere <spieler>` | Spieler zu dir anfragen. | `zbenteleport.tphere` | – |
| `/tpaccept [id]` | Anfrage annehmen. | – | – |
| `/tpdeny [id]` | Anfrage ablehnen. | – | – |
| `/tpacancel` | Eigene Anfrage abbrechen. | – | – |
| `/tptoggle` | Anfragen erlauben/sperren. | `zbenteleport.tptoggle` | – |
| `/tpblock <spieler>` | Spieler blockieren. | `zbenteleport.tpblock` | – |
| `/tpunblock <spieler>` | Blockierung aufheben. | `zbenteleport.tpunblock` | – |
| `/tpblocklist` | Blockierte Spieler anzeigen. | `zbenteleport.tpblocklist` | – |
| `/rtp` | Zufälliger Teleport. | `zbenteleport.rtp` | – |
| `/setwarp <name>` | Warp setzen. | `zbenteleport.warp.set` | – |
| `/delwarp <name>` | Warp löschen. | `zbenteleport.warp.del` | – |
| `/warp <name>` | Warp nutzen. | `zbenteleport.warp.use` | – |
| `/warps` | Warps auflisten. | `zbenteleport.warp.list` | – |
| `/sethome <name>` | Home speichern. | `zbenteleport.sethome` | – |
| `/home <name>` | Zu Home teleportieren. | `zbenteleport.home` | – |
| `/delhome <name>` | Home entfernen. | `zbenteleport.delhome` | – |
| `/homes` | Homes anzeigen. | `zbenteleport.homes` | – |
| `/back` | Zurück zur letzten Position. | – | – |

## Permissions
- `zbenteleport.bypass.cooldown` – Cooldowns umgehen (Default: op).
- `zbenteleport.homes.*` – Unbegrenzte Homes (Default: op).
- Weitere Permissions gemäß Command-Tabelle (Default meist true, Warp-Set/Del op).

## Beispiele
- Spieler-Home-Limits staffeln: Permissions `zbenteleport.homes.5` oder `.10` vergeben.
- Event-Server: RTP-Welten in `rtp.worldsAllowed` einschränken.
- Moderation: Cooldown-Bypass für Admin-Team setzen.

## Troubleshooting
- **„Keine Berechtigung“:** Permissions laut Tabelle prüfen oder Gruppenplugin anpassen.
- **RTP schlägt fehl:** Welt ggf. nicht in `rtp.worldsAllowed` oder kein sicherer Ort nach `maxAttempts`.
- **Anfrage läuft ab:** `tpa.timeoutSeconds` erhöhen.
