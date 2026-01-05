# ZBenAdmintool

Admin- und Moderations-Tool für Paper 1.21 (Java 21) mit internem Rangsystem.

## Installation
1. `./gradlew build` ausführen.
2. Die erzeugte JAR aus `build/libs` in den `plugins/`-Ordner kopieren.
3. Server starten und bei Bedarf `config.yml`/`messages.yml` anpassen.

## Ränge & Rechte
Das Plugin verwaltet alle Berechtigungen intern (kein externes Permission-Plugin nötig). Standard-Ränge und Rechte:

- **Owner**: Alle Rechte (BAN, KICK, MUTE, WARN, INSPECT, RANK_MANAGE, ADMIN_MENU, ADMIN_MODE, VANISH, LOGS, OFFLINE_INVENTORY, OFFLINE_ENDERCHEST) und alle Bukkit-Permissions.
- **Admin**: BAN, KICK, MUTE, WARN, INSPECT, RANK_MANAGE, ADMIN_MENU, ADMIN_MODE, VANISH, LOGS, OFFLINE_INVENTORY, OFFLINE_ENDERCHEST.
- **Moderator**: KICK, MUTE, WARN, INSPECT.
- **Supporter**: INSPECT.
- **Spieler**: Keine Admin-Rechte.

Interne Rank-Permissions sind die Quelle der Wahrheit. Beim Laden werden fehlende Rechte für bekannte Ränge automatisch ergänzt. Bukkit-Permissions werden weiterhin über das Rangsystem vergeben, dienen aber nur als Ergänzung.

### Backpack-Slots pro Rang
Jeder Rang hat eine konfigurierbare Backpack-Größe. Standardwerte:
- Owner: 54
- Admin: 45
- Moderator: 36
- Supporter: 30
- Spieler: 27

Änderungen greifen automatisch, sobald ein Spieler den Rang erhält. Wenn das Plugin **ZBenBackpack** installiert und aktiv ist, wird die Größe per Soft-Depend reflektiert; ohne ZBenBackpack werden stillschweigend keine Änderungen vorgenommen.

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

