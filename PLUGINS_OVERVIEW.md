# Plugin-Übersicht – MC-Plugins Sammlung

Diese Sammlung enthält mehrere eigenständige Paper/Spigot-Plugins von ZBenNoZ. Jedes Plugin kann einzeln genutzt werden, viele ergänzen sich jedoch im Gameplay (Teleport, Claims, Skills, Enchants, Economy, Loot). Alle Plugins sind für Minecraft 1.21 und Java 17+ ausgelegt.

## Plugin-Liste
| Plugin | Kurzbeschreibung | Hauptfeatures | Wichtige Commands |
| --- | --- | --- | --- |
| ZBenClaims | Chunk-basierte Claims mit Rang-Limits und Trust-System. | Claims, Ränge/Prefixes, Flags, Admin-Bypass | `/claim`, `/unclaim`, `/trust`, `/claims`, `/zbenclaims`, `/rank` |
| ZBenAmbient | Ambient-Partikel um Spieler. | Sporenblüten-Partikel, Nebel in Sümpfen, konfigurierbare Intensität | – |
| ZBenTeleport | TPA/Homes/Warps/RTP mit Cooldowns. | TPA & Blocklisten, Homes mit Limits, Warps, RTP, Back | `/tpa`, `/tpaccept`, `/sethome`, `/home`, `/warp`, `/rtp`, `/back` |
| ZBenTimber | Vein/Tree-Mining. | Baum-/Erz-/Stein-Adern auf einmal abbauen, Sneak-Option, Limits | – |
| ZBenCityJobs | Jobs- & Marktplatz-System mit Firmen/Städten. | Job-Board mit Escrow, Market GUI, Companies/Cities, Vault-Economy | `/jobs`, `/market`, `/company`, `/city` |
| ZBenBackpack | Persistente Rucksäcke. | `/backpack` GUI, Größen per Permission, SQLite-Speicher | `/backpack` |
| ZBenLootr | Spielerbezogener Kistenloot. | Per-Player-Loot für Container, SQLite/MySQL, Cache, Loot-Tables | `/zbenlootr` |
| ZBenEnchants | Custom-Verzauberungen. | Telekinesis/Smelt, Excavator, Lumberjack, Replant, Kampf- und Utility-Enchants, Cooldowns | `/zbenenchants`, Subcommands `givebook`, `reload` |
| ZBenSkills | Skill- & Prestige-System. | Multi-Skill-Trees, Challenges/Achievements, Anti-Exploit, Prestige | `/skills` |

## Kombinierte Nutzung / Integrationen
- **ZBenClaims + ZBenEnchants:** Enchants erkennen optional das Claims-Plugin (Softdepend). Claims-Ränge können in Tab/Chat/Nametags als Prefixe angezeigt werden und so auch für andere Plugins (z. B. Teams, Ränge) genutzt werden.
- **ZBenTeleport + ZBenClaims:** Empfohlen, um Homes und Warps nur in eigenen Claims zu setzen (via Permissions/Serverregeln). Teleport-Back kann nach Claim-Toden hilfreich sein.
- **ZBenBackpack + ZBenSkills:** Größere Rucksack-Permissions können als Skill- oder Rang-Belohnung vergeben werden.
- **ZBenCityJobs + ZBenLootr/ZBenEnchants:** Wirtschaftliche Progression (Jobs/Market) lässt sich mit besseren Loots und Enchants koppeln; Escrow verhindert Abuse.

### Abhängigkeiten und Reihenfolge
- Softdepend: ZBenEnchants → ZBenClaims (Claims sollten vor Enchants geladen sein, wenn genutzt).
- Externe Abhängigkeit: ZBenCityJobs benötigt Vault + Economy-Provider für Zahlungen.
- Startreihenfolge: Economy/Permissions vor CityJobs laden; Claims vor Enchants; übrige Plugins unabhängig.

### Empfohlenes Setup & Beispiele
- **Survival-Server:** Claims aktivieren, ZBenTeleport für Komfort, ZBenBackpack für Inventarqualität, ZBenEnchants & ZBenSkills für Progression, ZBenLootr für personalisierte Dungeon-Loots, ZBenCityJobs für Wirtschaft.
- **Rollen/Permissions:** Ränge aus ZBenClaims (Beginner/Spieler/VIP/Admin) als Basis nehmen und weitere Rechte ergänzen (z. B. größere Backpacks, mehr Homes, CityJobs-Admin nur für Staff).
- **Beispiel-Permissions pro Rolle:**
  - User: `zbenclaims.use`, `zbenteleport.tpa/home/warp.use`, `zbenbackpack.size.9`, `zbenskills.use`.
  - VIP: zusätzlich `zbenteleport.homes.5`, `zbenbackpack.size.36`, mehr Claim-Limit über `rank`.
  - Admin: alle `zbenclaims.admin*`, `zbenteleport.bypass.cooldown`, `zbenlootr.admin`, `zbenenchants.*` laut Bedarf.

## Server-Setup Checkliste
- Java 17+ und Paper/Spigot 1.21.
- Vault + Economy-Plugin installieren (für ZBenCityJobs).
- Datenbankpfade/Logins prüfen (Backpack/CityJobs/Lootr nutzen SQLite oder MySQL).
- Permission-Plugin konfigurieren (LuckPerms o. ä.).
- Konfigurationsdateien pro Plugin anpassen (Cooldowns, Limits, Loot-Table, Claim-Ränge, Skill-Schwellen).

## FAQ
- **„Ich sehe kein Claim-Prefix in der Tablist“:** Prefixe pro Rang in `ZBenClaims/config.yml` prüfen; Tablist-/Chat-Plugin muss Colorcodes respektieren.
- **„Backpack ist zu klein“:** Höhere `zbenbackpack.size.*`-Permission vergeben.
- **„TPA-Anfragen kommen nicht an“:** Spieler hat evtl. `/tptoggle` aktiviert oder Absender ist geblockt.
- **„Loot wird geteilt“:** Sicherstellen, dass Container-Typ in `ZBenLootr` freigegeben ist und Plugin aktiv.
- **„Jobs bezahlen nicht“:** Vault/Economy prüfen und `economy.escrow-required` korrekt setzen.

## Changelog / Updates
- Beim Aktualisieren: Server stoppen, Jars austauschen, ggf. Config-Änderungen übernehmen (Backup der YAMLs und Datenbanken anlegen). Prüfen, ob neue Permissions oder Config-Keys hinzugekommen sind.
