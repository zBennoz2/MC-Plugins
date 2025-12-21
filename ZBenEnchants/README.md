# ZBenEnchants – Zusätzliche Verzauberungen

## Features
- Zusätzliche Werkzeugeffekte wie Telekinesis (Drops ins Inventar), Smelt (Auto-Schmelzen), Excavator/Lumberjack (Flächenabbau), Replant, LuckyFind u. a.
- Kampforientierte Effekte (Guardian, SecondWind, Stability) und Utility (Grapple).
- Combat-Tagging und pro-Enchant-Cooldowns; Enchant-Level per Persistent Data gespeichert.
- Verwaltungsbefehl zum Geben von Büchern und Reload der Config.

## Quickstart
1. Jar in `plugins/` legen und Server starten (optional mit ZBenClaims als Softdepend).
2. Mit `/zbenenchants givebook <spieler> <enchant> <level>` Bücher vergeben.
3. Config-Werte für Reichweiten, Cooldowns und Blacklists in `config.yml` anpassen.

## Installation
- **Voraussetzungen:** Paper/Spigot 1.21, Java 17+.
- **Build:** `./gradlew build` → Jar unter `build/libs`.
- **Installation:** Jar in `plugins/` kopieren und Server neu starten.

## Konfiguration (`config.yml` – Auszug)
- `general.combat-tag-ms`: Dauer für Combat-Tagging.
- `messages.*`: Präfix, Nutzungs-/Fehlermeldungen für Commands und Enchant-Anwendung.
- `enchants.telekinesis.disable-while-sneaking`: Telekinesis bei Sneak deaktivieren; `block-blacklist` für Ausnahmen.
- `enchants.smelt.whitelist-blocks` / `blacklist-blocks`: Blocktypen für Auto-Schmelzen.
- `enchants.excavator`: `radius`, `max-blocks`, `cooldown`, `durability-multiplier`, `blacklist` (Flächenabbau beim Sneaken).
- `enchants.replant`: `include-nether-wart` für automatische Neubepflanzung.
- `enchants.lumberjack`: `max-blocks`, `radius`, `durability-multiplier`, `natural-only`, `cooldown`.
- `enchants.luckyfind`: Basischance und Blacklist für seltene Funde.
- `enchants.guardian`: Chance, Dauer, Stärke, Cooldown für Schutz-Effekt.
- `enchants.grapple`: Wurfhakenstärke, Max-Vertikal, Sneak-Pflicht, Combat-Block, Cooldown.
- `enchants.stability`: Fallschadenreduktion pro Level.
- `enchants.secondwind`: Gesundheits-Schwelle, Regeneration/Speed-Dauer, Cooldown.

## Commands
| Command | Beschreibung | Permission | Aliases |
| --- | --- | --- | --- |
| `/zbenenchants` | Hauptbefehl (Book-Gabe, Reload laut Messages). | – | – |
| `/zbenenchants givebook <spieler> <enchant> <level>` | Enchant-Buch vergeben. | `zbenenchants.givebook` | – |
| `/zbenenchants reload` | Config neu laden. | `zbenenchants.reload` | – |

## Permissions
- `zbenenchants.givebook` – Enchant-Bücher verteilen (Default: op).
- `zbenenchants.reload` – Config neu laden (Default: op).

## Integrationen
- Softdepend auf **ZBenClaims**: Prüft optional, ob Claims-Plugin aktiv ist.

## Beispiele
- Ressourcenfarm: Excavator + Telekinesis auf Spitzhacke, `excavator.max-blocks` erhöhen, um Flächen abzubauen.
- Holzfäller-Rang: Lumberjack mit höherem Radius und `natural-only: true` für natürliche Bäume.
- PvE: Guardian/SecondWind einsetzen, um kurzzeitige Resistenzen/Heilung bei niedrigem Leben zu erhalten.

## Troubleshooting
- **Effekt tritt nicht ein:** Cooldowns pro Enchant prüfen (`general.combat-tag-ms` oder spezifische Cooldowns).
- **Auto-Schmelzen greift nicht:** Block ggf. nicht auf Whitelist oder auf Blacklist in `enchants.smelt`.
- **Replant funktioniert nicht:** Sicherstellen, dass Saatgut im Inventar vorhanden ist und Feld ausgewachsen war.
