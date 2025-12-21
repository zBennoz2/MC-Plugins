# ZBenLootr – Spielerbezogener Kisten-Loot

## Features
- Persönlicher Loot pro Spieler für unterstützte Container (Kisten, Fässer; doppelte Kisten optional erkannt).
- Loot wird pro Container-ID und Spieler gespeichert (SQLite oder MySQL), inkl. Cache.
- Loot-Generierung über Vanilla-Loot-Table oder konfigurierbaren Modus; Seed-Modus pro Spieler.
- Admin-Befehl zum Reload/Info.

## Quickstart
1. Config anpassen (`storage` für SQLite/MySQL, Loot-Table wählen).
2. Jar in `plugins/` legen und Server starten.
3. Container öffnen – jeder Spieler erhält eigenes Inventar, das dauerhaft gespeichert wird.

## Installation
- **Voraussetzungen:** Paper/Spigot 1.21, Java 17+.
- **Build:** `./gradlew build` → Jar unter `build/libs`.
- **Installation:** Jar in `plugins/` kopieren und Server neu starten.

## Konfiguration (`config.yml`)
| Schlüssel | Beschreibung | Standard |
| --- | --- | --- |
| `storage` | `SQLITE` oder `MYSQL`. | SQLITE |
| `mysql.*` | Host/Port/DB/User/Pass, wenn MySQL genutzt wird. | siehe Config |
| `cache.maxEntries` | Max. Cache-Einträge. | 10000 |
| `cache.expireSeconds` | Cache-Ablauf in Sekunden. | 600 |
| `containers.enabledTypes` | Liste unterstützter Container (z. B. CHEST, BARREL). | CHEST, BARREL |
| `containers.detectDoubleChest` | Doppelkisten automatisch erkennen. | true |
| `loot.mode` | Loot-Modus (z. B. `VANILLA_LOOTTABLE`). | VANILLA_LOOTTABLE |
| `loot.vanillaLootTable` | Genutzte Vanilla-Loot-Table. | `minecraft:chests/simple_dungeon` |
| `loot.seedMode` | Seed je Spieler oder global (`PER_PLAYER`). | PER_PLAYER |

## Commands
| Command | Beschreibung | Permission | Aliases |
| --- | --- | --- | --- |
| `/zbenlootr <reload|info>` | Config neu laden oder Info anzeigen. | `zbenlootr.admin` | – |

## Permissions
- `zbenlootr.admin` – Verwaltung des Plugins (Default: op).

## Beispiele
- Dungeon-Loot personalisieren: Loot-Table auf `minecraft:chests/ancient_city` setzen.
- MySQL nutzen: `storage: MYSQL` wählen und Zugangsdaten unter `mysql` hinterlegen.

## Troubleshooting
- **Loot wird geteilt:** Prüfen, ob Container-Typ in `containers.enabledTypes` enthalten ist und Double-Chest-Erkennung aktiv ist.
- **DB-Fehler:** MySQL-Zugangsdaten prüfen oder auf SQLite zurückfallen.
