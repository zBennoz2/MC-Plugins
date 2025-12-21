# ZBenSkills – Fortschrittliches Skillsystem

## Features
- Skill-Trees für mehrere Disziplinen (Mining, Woodcutting, Farming, Combat, etc.) mit bis zu 200 Leveln und Prestige-Tokens.
- Anti-Exploit-Mechaniken (Cooldowns, Diminishing Returns, Spawner-Block, Welt-Blacklist).
- Challenges und Achievements (Definitionen in `achievements.yml`), GUI-Menü über `/skills`.
- Persistente Speicherung von Skill-XP/Leveln (Repository), automatisches Caching und Flush beim Stop.

## Quickstart
1. Jar in `plugins/` legen und Server starten (legt `config.yml`, `messages.yml`, `achievements.yml` an).
2. Mit `/skills` das Menü öffnen; Challenges/Achievements ansehen oder Prestige nutzen.
3. XP durch Spielaktivitäten sammeln (Listener für Abbau/Kampf etc.).

## Installation
- **Voraussetzungen:** Paper/Spigot 1.21, Java 17+.
- **Build:** `./gradlew build` → Jar unter `build/libs`.
- **Installation:** Jar in `plugins/` kopieren und Server neu starten.

## Konfiguration (`config.yml` – Auszug)
| Schlüssel | Beschreibung | Standard |
| --- | --- | --- |
| `skills.max-level` | Maximales Level pro Skill. | 200 |
| `skills.base-xp` | Basis-XP für Level 1. | 125 |
| `skills.exponential-base` | XP-Skalierung. | 1.18 |
| `skills.softcap-start` / `softcap-multiplier` | Erhöht Kosten ab Level-Schwelle. | 150 / 1.32 |
| `skills.prestige.tokens` | Tokens pro Prestige. | 1 |
| `anti-exploit.*` | Cooldowns, Diminishing Returns, Welt-Blacklist, Spawner-Mobs sperren. | siehe Config |
| `skill-tree.default-node-count` | Anzahl automatisch generierter Knoten pro Skill. | 25 |
| `skill-tree.nodes.*` | Optionale manuelle Knotendefinitionen je Skill. | {} |

## Commands
| Command | Beschreibung | Permission | Aliases |
| --- | --- | --- | --- |
| `/skills [challenges|achievements|prestige]` | Skill-Menü öffnen und Unterseiten wählen. | `zbenskills.use` | `skill`, `ability` |

## Permissions
- `zbenskills.use` – Zugriff auf das Skill-Menü (Default: true).

## Integrationen
- Speichert Daten lokal (Repository); keine externen Abhängigkeiten außer Server-API.

## Beispiele
- Progression anpassen: `skills.max-level` auf 100 senken und `exponential-base` reduzieren für schnelleren Fortschritt.
- Anti-Exploit verschärfen: `anti-exploit.disable-spawner-mobs: true` und `diminish-threshold` verringern.
- Prestige-Belohnungen: `skills.prestige.tokens` erhöhen, wenn Prestige attraktiver sein soll.

## Troubleshooting
- **XP steigt nicht:** Welt evtl. in `anti-exploit.disable-worlds` gelistet oder Aktionen unter Cooldown (`action-cooldown-ms`).
- **Leistungseinbrüche:** `anti-exploit.diminish-*` und `default-node-count` ggf. anpassen, um weniger Berechnungen zu erzwingen.
