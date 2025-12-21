# ZBenTimber – Baum- und Ader-Abbau

## Features
- Fällt ganze Baumstämme (inkl. Blätter) mit einem Blockbruch.
- Erz-Adern sowie optionale Steinadern können in einem Rutsch abgebaut werden.
- Sneak-Anforderung (konfigurierbar) und Block-Limit pro Aktion zur Performancekontrolle.
- Separate Permissions für Holz, Erze und Steinadern.

## Quickstart
1. Jar in `plugins/` legen und Server starten.
2. In-game beim Sneaken einen Log/Erz/Stein abbauen.
3. Werte in `config.yml` anpassen, falls mehr/weniger Blöcke verarbeitet werden sollen.

## Installation
- **Voraussetzungen:** Paper/Spigot 1.21, Java 17+.
- **Build:** `./gradlew build` → Jar unter `build/libs`.
- **Installation:** Jar in `plugins/` kopieren, Server neu starten.

## Konfiguration (`config.yml`)
| Schlüssel | Beschreibung | Standard |
| --- | --- | --- |
| `limits.max` | Maximal abbaubare Blöcke pro Aktion. | 96 |
| `stone-vein` | Steinadern (Cobblestone/Deepslate) ebenfalls abbauen. | false |
| `requireSneak` | Nur bei gedrücktem Sneak aktiv. | true |

## Commands
Keine Befehle vorhanden.

## Permissions
- `zbentimber.tree` – Baum- und Blätterabbau erlauben (Default: true).
- `zbentimber.ore` – Erzadern abbauen (Default: true).
- `zbentimber.stone` – Steinadern, falls `stone-vein` aktiv (Default: true).

## Beispiele
- Schnellholz: `requireSneak` auf `false` setzen, um ohne Sneak zu arbeiten.
- Farmwelt-Erze: `limits.max` erhöhen, um größere Adern mit einem Schlag abzubauen.

## Troubleshooting
- **Nichts passiert beim Abbau:** Sneak-Anforderung prüfen oder fehlende Permission.
- **Werkzeug bricht schneller:** Haltbarkeit sinkt pro abgebautem Block; hochwertiges Werkzeug empfohlen.
