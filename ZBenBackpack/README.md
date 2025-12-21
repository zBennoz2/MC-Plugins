# ZBenBackpack – Persistente Rucksäcke

## Features
- `/backpack` öffnet einen persönlichen Inventar-Rucksack (Bundle-Item + GUI).
- Inventargrößen per Permission (9–54 Slots) skalierbar.
- Inhalte werden in SQLite (`backpacks.db`) gespeichert und beim Schließen automatisch gesichert.
- Verhalten bei Tod konfigurierbar (z. B. Items behalten).

## Quickstart
1. Jar in `plugins/` legen und Server starten.
2. Mit `/backpack` den Rucksack öffnen; Item wird als Bundle mit Marker gespeichert.
3. Permissions verteilen, um größere Größen freizuschalten (z. B. `zbenbackpack.size.27`).

## Installation
- **Voraussetzungen:** Paper/Spigot 1.21, Java 17+.
- **Build:** `./gradlew build` → Jar unter `build/libs`.
- **Installation:** Jar in `plugins/` kopieren und Server neu starten.

## Konfiguration (`config.yml`)
| Schlüssel | Beschreibung | Standard |
| --- | --- | --- |
| `behavior.on-death` | Umgang mit Rucksackinhalt bei Tod (z. B. `keep`). | `keep` |

## Commands
| Command | Beschreibung | Permission | Aliases |
| --- | --- | --- | --- |
| `/backpack` | Persönlichen Rucksack öffnen. | keine spezielle (Größe per Permission) | – |

## Permissions
- `zbenbackpack.size.9` (Default: true)
- `zbenbackpack.size.18/27/36/45/54` (Default: op) – legt maximale Slots fest.

## Beispiele
- VIP-Perk: Spielern die Permission `zbenbackpack.size.36` geben für größere Taschen.
- Hardcore-Server: `behavior.on-death` auf `keep` lassen, damit Rucksack nicht dropt.

## Troubleshooting
- **Rucksack öffnet nicht:** Sicherstellen, dass Spieler ist; Konsole erhält Fehlermeldung „Only players can use this.“
- **Größe kleiner als erwartet:** Prüfen, welche `zbenbackpack.size.*`-Permission der Spieler besitzt.
