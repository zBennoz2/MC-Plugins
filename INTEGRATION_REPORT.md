# Integration Report

## Erkannte Plugins
- Claim Plugin (ZBenClaims) – Gradle, Rank/Chunk-Claim-System
- Server-Plugin (ZBenCityJobs) – Gradle, Jobs/Markt/Company/City Economy
- ZBenAmbient – Gradle, Ambient Task Effects
- ZBenBackpack – Gradle, persistente Backpacks
- ZBenEnchants – Gradle, Enchant-Verwaltung
- ZBenLootr – Gradle, Loot-Tables/Chest-Instanzen
- ZBenSkills – Gradle, Skill-System
- ZBenTeleport – Gradle, Teleport/Home/Warp
- ZBenTimber – Gradle, Schnelles Abbauen (Timber)

## Display Owner
- Vorgeschlagen: ZBenClaims als Display Owner, da bereits Rank-Visuals/Scoreboard nutzt.

## Tablist Format
- Geplant: Prefix = Rank tabPrefix + [Team]; Suffix optional Job «Job»; Spielername dazwischen.

## Services/Interfaces
- Neu erstellt in ZBenClaims: RankProvider, RankView, TeamProvider, JobProvider (Bukkit Services).

## Backpack-Rank-Config
- Noch offen: Erweiterung in ZBenBackpack zur Rank-basierten Größe geplant.

## Commands/Permissions neu
- Keine neuen Befehle/Permissions hinzugefügt (nur Service-API vorbereitet).
