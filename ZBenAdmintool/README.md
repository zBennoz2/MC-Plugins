# ZBenAdmintool

Admin-Werkzeug für Paper 1.21 (Java 21).

## Installation
1. Mit `./gradlew build` bauen.
2. Die erzeugte JAR aus `build/libs` nach `plugins/` kopieren.
3. Server starten, Config/Messages anpassen.

## Befehle
- `/admin` (Alias `/admintool`): Öffnet das Admin-GUI.
- `/adminmode`: Admin-Mode inkl. Vanish und Creative umschalten.
- `/vanish`: Vanish separat umschalten.
- `/rank ...`: Ränge verwalten (set, remove, create, delete, list, perm add/remove).
- `/inspect`: Inspektor-Modus zum Block/Container prüfen.
- `/logs <block|chest> <x> <y> <z> [world] [seite]`: Logs abrufen.
- `/offinv <spieler>`: Offline-Inventar anzeigen (offline nur Read-Only).
- `/offec <spieler>`: Offline-Enderchest anzeigen (offline nur Read-Only).

## Permissions
- `zbenadmintool.admin` – Admin-GUI
- `zbenadmintool.adminmode` – Admin-Mode
- `zbenadmintool.vanish` / `zbenadmintool.vanish.see`
- `zbenadmintool.rank`
- `zbenadmintool.inspect`
- `zbenadmintool.logs`
- `zbenadmintool.offinv`
- `zbenadmintool.offec`
