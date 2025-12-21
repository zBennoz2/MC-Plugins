# ZBenAmbient – Ambient-Partikel für Spieler

## Features
- Periodische Partikeleffekte um Online-Spieler (Sporenblüten-Regenschauer).
- Zusätzlicher Nebel in Sumpf-Biomen basierend auf Spielerbiom.
- Konfigurierbare Intensität und Aktivierung pro Effekt.

## Quickstart
1. Jar in `plugins/` legen und Server starten.
2. Config bei Bedarf anpassen (`plugins/ZBenAmbient/config.yml`).
3. Spieler erhalten automatisch Ambient-Partikel im Spiel.

## Installation
- **Voraussetzungen:** Paper/Spigot 1.21, Java 17+.
- **Build:** `./gradlew build` erzeugt das Jar unter `build/libs`.
- **Installation:** Jar in `plugins/` kopieren, Server neu starten.

## Konfiguration (`config.yml`)
| Schlüssel | Beschreibung | Standard |
| --- | --- | --- |
| `intensity.leaves` | Partikelanzahl der fallenden Sporenblüten pro Tick. | 2 |
| `intensity.fog` | Partikelanzahl für Nebel (nur in Sumpf-Biomen). | 1 |
| `enabled.leaves` | Blätterpartikel aktivieren/deaktivieren. | true |
| `enabled.fog` | Nebeleffekt aktivieren/deaktivieren. | true |
| `enabled.fireflies` | Platzhalter-Option, aktuell im Code nicht genutzt. | true |

## Commands
Keine Befehle verfügbar.

## Permissions
Keine eigenen Permissions erforderlich.

## Beispiele
- Stimmung in Sumpfgebieten erhöhen: `enabled.fog: true`, `intensity.fog: 3` für dichteren Nebel.
- Dezente Effekte: `intensity.leaves: 1` setzen.

## Troubleshooting
- **Keine Partikel sichtbar:** Grafikeinstellungen der Spieler prüfen und sicherstellen, dass `enabled.*` nicht deaktiviert ist.
- **Hohe Partikeldichte:** `intensity`-Werte reduzieren.
