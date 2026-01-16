# ZBenEssentials (Hytale)

ZBenEssentials ist ein eigenständiger Essentials-Mod für Hytale-Server ohne OP-Zwang. Er bringt ein integriertes Gruppen- und Permission-System, ein konfigurierbares Nachrichten- und Welcome-System sowie Basis-Commands. Das Projekt ist so aufgebaut, dass es keine globalen Overrides verwendet und dadurch kompatibel mit anderen Mods bleibt.

> Hinweis: Die Hytale-API/Loader-Struktur kann sich noch ändern. Alle SDK-spezifischen Stellen sind als **TODO** markiert und über Adapter-Methoden gekapselt.

## Installation

1. Ordner `ZBenEssentials_Hytale` in dein Mod-/Plugins-Verzeichnis kopieren.
2. Server starten, damit `config/config.json` und `config/users.json` erzeugt werden.
3. Konfiguration anpassen und bei Bedarf mit `/zben reload` neu laden.

## Commands

| Command | Beschreibung | Permission |
| --- | --- | --- |
| `/zben ping` | Test-Command | `zben.ping` |
| `/zben reload` | Konfiguration neu laden | `zben.admin` |
| `/zben whoami` | Zeigt Gruppe und Rechte | `zben.whoami` |
| `/zben setgroup <player> <group>` | Setzt Gruppe für Spieler | `zben.admin` |

## Permissions

- `zben.ping` – Zugriff auf `/zben ping`
- `zben.whoami` – Zugriff auf `/zben whoami`
- `zben.admin` – Zugriff auf Admin-Commands
- Wildcards werden unterstützt (z. B. `zben.*`, `zben.home.*`)

## Beispiel `config.json`

```json
{
  "language": "de_DE",
  "prefix": "[ZBen]",
  "defaultGroup": "default",
  "messageOverrides": {
    "command.ping": "pong!",
    "join.message": "{player} ist da!"
  },
  "welcome": {
    "enabled": true,
    "broadcastToAll": true,
    "messages": [
      "Willkommen {player}!",
      "Schau dir /zben whoami an."
    ]
  },
  "joinQuit": {
    "enabled": true,
    "joinMessage": "{player} hat den Server betreten.",
    "quitMessage": "{player} hat den Server verlassen."
  },
  "chatFormats": {
    "default": {
      "format": "{prefix} {player}: {message}",
      "priority": 1
    },
    "vip": {
      "format": "{prefix} §6VIP {player}§r: {message}",
      "priority": 10
    }
  },
  "groups": {
    "default": {
      "permissions": [
        "zben.ping",
        "zben.whoami"
      ]
    },
    "vip": {
      "permissions": [
        "zben.ping",
        "zben.whoami",
        "zben.home.*"
      ]
    },
    "admin": {
      "permissions": [
        "zben.*"
      ]
    }
  }
}
```

## Troubleshooting

- **Commands funktionieren nicht**: Prüfe, ob die TODO-Registrierung im Mod-Core bereits an die richtige Hytale-API angebunden ist.
- **Konfiguration lädt nicht**: Stelle sicher, dass `config/config.json` gültiges JSON enthält.
- **Permissions greifen nicht**: Prüfe die `groups`-Definition sowie die `users.json` Zuordnung.

## Projektstruktur

```
ZBenEssentials_Hytale/
├── README.md
├── config/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/zben/essentials/
│       │       ├── ZBenEssentialsMod.java
│       │       ├── commands/
│       │       └── services/
│       └── resources/
│           └── lang/
└── config/
```
