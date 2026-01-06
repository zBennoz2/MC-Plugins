# ZBenEnchants (Paper 1.21.x)

Deutschsprachiges Custom-Enchant-Plugin. Speichert Verzauberungen sauber im PersistentDataContainer, zeigt eigene Angebote am Verzauberungstisch, erlaubt Amboss-Integration mit Büchern und bietet optionale Dorfbewohner-Trades.

## Schnellstart
1. Jar in `plugins/` kopieren, Server mit Paper 1.21.x + Java 17 starten.
2. Standard-Config wird geschrieben: Tisch-, Amboss- und Villager-Integration sind aktiv.
3. Teste mit `/zbenenchants givebook <Spieler> <Enchant> <Level>` oder direkt am Verzauberungstisch.

## Wie erhalte ich die Verzauberungen?
- **Verzauberungstisch:** Mit Chance `enchantingTable.chanceToAddCustomEnchant` (Standard 25 %) wird ein Angebot durch ein ZBenEnchants-Enchant ersetzt. Mindest-Bücherregalstufe: `enchantingTable.minBookshelfPower`.
- **Amboss:** Custom-Buch rechts einlegen, kompatibles Werkzeug links. Kosten = `anvil.baseCost + anvil.costPerLevel * Level`. Bücher werden zuverlässig verbraucht; kein Duplizieren.
- **Dorfbewohner:** Librarians können Bücher anbieten (Chance `villagers.chancePerTradeRefresh`). Preis skaliert über `villagers.emeraldCost`.
- **Admin-Befehl:** `/zbenenchants givebook <spieler> <enchant> <level>`

## Verzauberungsliste
| Name | Beschreibung | Max-Level | Anwendbar auf | Erwerb |
| --- | --- | --- | --- | --- |
| Telekinese | Drops wandern direkt ins Inventar (Block-Blacklist & Sneak-Block möglich). | 3 | Äxte, Hacken, Spitzhacken, Schaufeln | Tisch, Amboss-Buch, Villager, Command |
| Schmelzen | Ores/Blöcke werden beim Abbauen automatisch geschmolzen (Whitelist/Blacklist). | 2 | Spitzhacken | Tisch, Amboss-Buch, Villager, Command |
| Bagger | Flächenabbau beim Sneaken, begrenzt über Radius/Max-Blöcke. | 1 | Spitzhacken | Tisch, Amboss-Buch, Villager, Command |
| Neuanpflanzen | Voll ausgewachsene Pflanzen werden nach dem Abbau neu gesetzt (zieht Saat aus Inventar). | 3 | Hacken | Tisch, Amboss-Buch, Villager, Command |
| Holzfäller | Entfernt zusammenhängende Stämme in Reichweite, optional nur natürliche Bäume. | 3 | Äxte | Tisch, Amboss-Buch, Villager, Command |
| Glücksfund | Chance auf einen zusätzlichen Drop des ersten Block-Drops. | 3 | Alle Werkzeuge | Tisch, Amboss-Buch, Villager, Command |
| Wächter | Chance auf Absorption nach Schaden, mit Cooldown. | 3 | Brustplatten | Tisch, Amboss-Buch, Villager, Command |
| Enterhaken | Rechtsklick-Sprung nach vorn; blockierbar im Kampf, Cooldown. | 3 | Stiefel | Tisch, Amboss-Buch, Villager, Command |
| Standfestigkeit | Reduziert Rückstoß/Velocity. | 3 | Stiefel | Tisch, Amboss-Buch, Villager, Command |
| Zweiter Atem | Bei niedrigem Leben Regeneration & Speed, Cooldown. | 1 | Brustplatten | Tisch, Amboss-Buch, Villager, Command |
| Vermesser | Kompass-Rechtsklick zeigt Welt-, Chunk- und Positionsinfos (mit ZBenClaims-Hook). | 1 | Kompass | Tisch, Amboss-Buch, Villager, Command |

## Konfiguration (Auszug)
- `enchantingTable.enabled`: Integration aktivieren/deaktivieren.
- `enchantingTable.chanceToAddCustomEnchant`: Wahrscheinlichkeit pro Vorbereitungs-Event, dass ein Angebot ersetzt wird.
- `enchantingTable.allowMultipleCustomEnchants`: ob mehrere Slots gleichzeitig Custom-Angebote sein dürfen.
- `enchantingTable.minBookshelfPower`: Mindest-Verzauberungsbonus des Tisches.
- `enchantingTable.restrictToSupportedItems`: nur passende Items zulassen.
- `anvil.enabled`: Ambossfunktion einschalten.
- `anvil.baseCost` / `anvil.costPerLevel`: Kostenformel pro Buch-Level.
- `anvil.allowStackedBooks`: ob gestapelte Bücher akzeptiert werden.
- `villagers.enabled`: Librarian-Trades für Bücher einschalten.
- `villagers.professions`: erlaubte Professionen (Standard nur LIBRARIAN).
- `villagers.chancePerTradeRefresh`: Chance pro generiertem Trade.
- `villagers.emeraldCost.base` / `perEnchantLevel`: Smaragd-Kosten.
- `messages.*`: Alle Chattexte (Deutsch).
- `general.combat-tag-ms`: Dauer des Combat-Tags für Grapple-Blocker.
- Weitere `enchants.*`-Sektionen regeln Reichweiten, Cooldowns, Black-/Whitelist.

## Fehlerquellen & Debugging
- **Amboss zeigt kein Ergebnis:**
  - Custom-Bücher müssen `zbenenchants:id` + `zbenenchants:level` im PDC besitzen oder eine erkennbare Lore („Sonderverzauberung: …“).
  - Jedes `PrepareAnvilEvent` wird in der Konsole geloggt (`[Amboss] ...`) – der Ablehnungsgrund steht dabei.
- **Kein Custom-Angebot am Zaubertisch:**
  - Mindest-Bücherregalstärke beachten (`enchantingTable.minBookshelfPower`, Standard 10).
  - Chance per Config (`chanceToAddCustomEnchant`, Standard 25 %). Erfolgreiche Slots werden im Log mit `[Zaubertisch]` angezeigt.
- **Villager handeln nichts:**
  - Nur konfigurierte Professionen/Level (Standard: Librarian, Level 1–5).
  - Chance pro Trade (`villagers.chancePerTradeRefresh`, Standard 15 %). Generierte Angebote erscheinen im Log (`[Villager]`).

## Permissions & Commands
- `zbenenchants.givebook` – erlaubt `/zbenenchants givebook ...` (Default: OP).
- `zbenenchants.reload` – erlaubt `/zbenenchants reload`.

## Stabilität & Hinweise
- Daten werden im PersistentDataContainer gespeichert – kein Itemverlust beim Neuladen.
- Custom-Enchants werden zusätzlich über PDC-Key `zbenenchants:id` + Level erkannt (Fallback Lore-Parser) und sauber auf Items übertragen.
- Amboss-, Tisch- und Villager-Handling laufen auf dem Main-Thread und vermeiden Duplikate.
- Alle Nachrichten/Lore sind deutschsprachig. Paper/Spigot 1.21.x wird unterstützt.
