# ZBenBackpack – Persistente Rucksäcke

## Features
- `/backpack` öffnet einen persönlichen Inventar-Rucksack (Bundle-Item + GUI).
- Inventargrößen: 9, 18, 27, 36, 45 oder 54 Slots.
  - Standard ohne Vorgabe: 9 Slots (oder via Permission `zbenbackpack.size.*`).
  - Per API (z. B. aus ZBenAdmintool) kann pro Spieler sofort eine Wunschgröße gesetzt werden.
- Inhalte werden in SQLite (`backpacks.db`) gespeichert und beim Schließen automatisch gesichert.
- Verhalten bei Tod konfigurierbar (z. B. Items behalten).

## Quickstart
1. Jar in `plugins/` legen und Server starten.
2. Mit `/backpack` den Rucksack öffnen; Item wird als Bundle mit Marker gespeichert.
3. Größenvorgaben:
   - Entweder Permissions verteilen (`zbenbackpack.size.27` etc.).
   - Oder ein externes Plugin wie **ZBenAdmintool** nutzt die API `applyBackpackSize(UUID, int)` zum Setzen.

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
- `zbenbackpack.size.18/27/36/45/54` (Default: op) – legt maximale Slots fest, falls keine Größe per API gespeichert ist.

## API / Integration
- Öffentliche Service-Methode: `applyBackpackSize(UUID playerId, int newSize)` (9, 18, 27, 36, 45 oder 54).
- Erreichbar über `ZBenBackpackPlugin#getBackpackService()` oder direkt `ZBenBackpackPlugin#applyBackpackSize(...)`.
- Wird die Größe verkleinert, landen überschüssige Items zuerst im Spielerinventar; was nicht passt, dropt vor dem Spieler. Für Offline-Spieler bleiben überschüssige Items in der Datenbank erhalten und werden beim nächsten Öffnen verteilt, bevor das neue Inventar angezeigt wird.

## Beispiele
- VIP-Perk: Spielern die Permission `zbenbackpack.size.36` geben für größere Taschen.
- Hardcore-Server: `behavior.on-death` auf `keep` lassen, damit Rucksack nicht dropt.

## Troubleshooting
- **Rucksack öffnet nicht:** Sicherstellen, dass Spieler ist; Konsole erhält Fehlermeldung „Only players can use this.“
- **Größe kleiner als erwartet:** Prüfen, welche `zbenbackpack.size.*`-Permission der Spieler besitzt.
