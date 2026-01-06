# ZBenAdmintool

Admin- und Moderations-Tool für Paper 1.21 (Java 21) mit internem Rangsystem.

## Installation
1. `./gradlew build` ausführen.
2. Die erzeugte JAR aus `build/libs` in den `plugins/`-Ordner kopieren.
3. Server starten und bei Bedarf `config.yml`/`messages.yml` anpassen.

## Admin-Mode & Beobachtung
- `/adminmode` oder Button im Admin-GUI aktiviert den Admin-Mode, setzt dich in den Spectator, aktiviert (konfigurierbar) Nachtsicht und Unsichtbarkeit und speichert deinen vorherigen Zustand. Beim Beenden wird alles wiederhergestellt.
- Admin-Mode und Beobachtung benötigen die internen Rechte `ADMIN_MODE` sowie `OBSERVE/TELEPORT` und werden geloggt.
- Im Admin-GUI gibt es „Spieler beobachten“: Liste aller Online-Spieler, anklickbar zum sicheren Teleport (leicht erhöht, Richtung wird übernommen). Jeder Teleport wird in `teleport-logs.yml` gespeichert.

## Verdächtige Aktivitäten (Mining-Erkennung)
- Aktivierbar über `suspiciousMining.enabled` in der `config.yml`.
- Über `BlockBreakEvent` werden seltene Erze im Zeitfenster `windowSeconds` gesammelt. Werden die in `thresholds` definierten Werte überschritten (Y-Limits respektiert), entsteht ein Verdachtsfall.
- Creative/OP können optional ignoriert werden (`ignoreCreative`, `ignoreOperators`).
- Staff mit `admintool.alerts` wird benachrichtigt (Action-Text/Chat). Verdachtsfälle werden in `suspicious-mining.yml` gespeichert (Aufbewahrung `storeEventsDays`).
- Neues GUI „Verdächtige Aktivitäten“ listet die letzten Fälle (Spieler, Erz, Anzahl, Ort, Zeitpunkt). Klick-Aktionen: Teleport zum Spieler, als erledigt markieren.

## Erz-Sicht & Ore-Xray
- Abschnitt `oreVision` in der `config.yml` steuert Radius, Scan-Intervall, Partikel, Ores-Whitelist und Limits.
- „Erz-Highlight“ im Admin-GUI: markiert Ores in der Nähe mit Partikeln (nur für dich sichtbar).
- Optional „Ore-Xray View“: Wenn ProtocolLib installiert ist, werden für den Admin in der Nähe alle Nicht-Erze clientseitig als Luft gesendet. Ohne ProtocolLib bleibt der Button wirkungslos und bricht sicher ab.
- Keine serverseitigen Block-Änderungen, beim Deaktivieren/Logout werden die Fake-Änderungen zurückgesetzt.

## Admin-GUI
- Neue Buttons: Spieler beobachten, Verdächtige Aktivitäten, Erz-Highlight (Toggle), Ore-Xray (nur mit ProtocolLib).
- Inventory-Interaktionen sind geschützt; Items können nicht entnommen werden.

## Ränge & Rechte
Das Plugin verwaltet alle Berechtigungen intern (kein externes Permission-Plugin nötig). Standard-Ränge und Rechte:

- **Owner**: Alle Rechte (BAN, KICK, MUTE, WARN, INSPECT, RANK_MANAGE, ADMIN_MENU, ADMIN_MODE, VANISH, LOGS, OFFLINE_INVENTORY, OFFLINE_ENDERCHEST) und alle Bukkit-Permissions.
- **Admin**: BAN, KICK, MUTE, WARN, INSPECT, RANK_MANAGE, ADMIN_MENU, ADMIN_MODE, VANISH, LOGS, OFFLINE_INVENTORY, OFFLINE_ENDERCHEST.
- **Moderator**: KICK, MUTE, WARN, INSPECT.
- **Supporter**: INSPECT.
- **Spieler**: Keine Admin-Rechte.

Interne Rank-Permissions sind die Quelle der Wahrheit. Beim Laden werden fehlende Rechte für bekannte Ränge automatisch ergänzt. Bukkit-Permissions werden weiterhin über das Rangsystem vergeben, dienen aber nur als Ergänzung.

### Backpack-Slots pro Rang
Jeder Rang hat eine validierte Backpack-Größe (9/18/27/36/45/54). Standardwerte:
- Owner: 54
- Admin: 45
- Moderator: 36
- Supporter: 27
- Spieler: 9

Änderungen greifen automatisch, sobald ein Spieler den Rang erhält. Wenn das Plugin **ZBenBackpack** installiert und aktiv ist, wird die Größe per Soft-Depend sofort gesetzt. Offline-Spieler behalten überschüssige Items in der Datenbank; sobald sie das Backpack öffnen, werden zu große Inventare reduziert und überschüssige Items ins Spieler-Inventar verschoben oder droppen vor dem Spieler. Ohne ZBenBackpack werden Ränge weiterhin gespeichert, die Backpack-Anpassung wird aber übersprungen.

## Befehle & Berechtigungen (über Ränge)
- `/admin` (Alias `/admintool`): Admin-GUI (ADMIN_MENU)
- `/adminmode`: Admin-Mode umschalten (ADMIN_MODE)
- `/vanish`: Vanish umschalten (VANISH)
- `/rank ...`: Ränge verwalten (`help`, `list`, `info`, `create`, `delete`, `set`, `remove`, `perm add/remove/list`, `backpack`) – (RANK_MANAGE)
- `/ban <spieler> [grund]`: Spieler bannen (BAN)
- `/kick <spieler> [grund]`: Spieler kicken (KICK)
- `/mute <spieler> [grund]`: Spieler stummschalten/entstummt bei erneutem Aufruf (MUTE)
- `/warn <spieler> <grund>`: Verwarnung speichern und ggf. melden (WARN)
- `/inspect`: Inspektor-Modus (INSPECT)
- `/logs <block|chest> <x> <y> <z> [world] [seite]`: Logs abrufen (LOGS)
- `/offinv <spieler>`: Offline-Inventar öffnen (OFFLINE_INVENTORY)
- `/offec <spieler>`: Offline-Enderchest öffnen (OFFLINE_ENDERCHEST)

Tab-Completion richtet sich nach der internen Rechteprüfung; ohne Rangberechtigung wird der Befehl abgelehnt („Dazu hast du keine Berechtigung.“).

## Offline-Inventar
- Öffnet Inventar oder Enderchest auch für Offline-Spieler.
- Während der Offline-Ansicht wird eine Sicherung (`plugins/ZBenAdmintool/offline-inventories/<uuid>.yml`) genutzt und Änderungen als „pending“ gespeichert.
- Join-Lock: Kommt der Spieler online, wird die Offline-Sitzung abgebrochen und gespeicherte Änderungen erst nach dem Login angewendet.
- Gespeicherte Änderungen werden beim nächsten Login des Spielers übernommen; überschüssige Items werden sicher ins Inventar gelegt oder droppen, damit nichts verloren geht.
- Aktionen werden im Server-Log protokolliert (wer welches Offline-Inventar geöffnet hat).

## Integration mit ZBenBackpack
- Softdepend (`ZBenBackpack`); keine harten Abhängigkeiten.
- Beim Setzen/Ändern eines Rangs wird automatisch die konfigurierte Backpack-Größe an ZBenBackpack weitergereicht.
- Ist ZBenBackpack nicht installiert oder deaktiviert, wird die Integration übersprungen (keine Fehlermeldung, optionaler Info-Log beim Erkennen).

## Häufige Fehler & Lösungen
- **„Dazu hast du keine Berechtigung.“ trotz Admin-Rang**: Prüfe, ob der Spieler wirklich einen Rang mit der passenden internen Berechtigung hat. Bestehende Ränge werden beim Laden automatisch mit Standardrechten ergänzt.
- **Befehle überschneiden sich mit anderen Plugins**: Bei Konflikten eigene Befehle (`/ban`, `/kick`, …) gezielt nutzen oder Aliase der Dritt-Plugins deaktivieren.
- **Backpack-Größe ändert sich nicht**: Stelle sicher, dass ZBenBackpack aktiv ist. Ohne dieses Plugin werden Änderungen am Rang zwar gespeichert, aber nicht angewendet.
- **Offline-Inventar speichert nicht**: Stelle sicher, dass die Sitzung nicht vom Join-Lock abgebrochen wurde. Die Datei `offline-inventories/<uuid>.yml` muss schreibbar sein.
