# ZBenCityJobs – Jobs, Marktplatz und Städte

## Features
- Auftragsbasierte Jobs mit Escrow-Sicherung, Annahme/Abgabe und Abbruch.
- Globaler Marktplatz mit GUI für Angebote und Käufe.
- Firmen- und Stadtverwaltung (Ränge, Steuer, Bürgermeister) inkl. Audit-Logs.
- Vault-Unterstützung für Wirtschaft; SQLite-Speicher (konfigurierbar) mit optional WAL.
- GUI-Steuerung für Job-Board und Markt, Wizard für Job-Erstellung.

## Quickstart
1. Vault + Economy-Plugin bereitstellen, Jar in `plugins/` legen und Server starten.
2. Mit `/jobs` das Job-Board öffnen oder neue Jobs erstellen.
3. `/market` für Marktplatz-Angebote nutzen; Firmen über `/company`, Städte über `/city` verwalten.

## Installation
- **Voraussetzungen:** Paper/Spigot 1.21, Java 17+, Vault + Economy für Zahlungsfunktionen.
- **Build:** `./gradlew build` → Jar unter `build/libs`.
- **Installation:** Jar in `plugins/` kopieren und Server neu starten.

## Konfiguration (`config.yml`)
| Schlüssel | Beschreibung | Standard |
| --- | --- | --- |
| `storage.database` | SQLite-Dateiname. | `cityjobs.db` |
| `storage.use-wal` | Write-Ahead-Logging aktivieren. | true |
| `storage.async-writes` | Asynchrone Schreibvorgänge. | true |
| `economy.escrow-required` | Lohn muss beim Erstellen hinterlegt werden. | true |
| `economy.escrow-account` | Konto für Escrow-Verwahrung. | `server` |
| `economy.tax-default-percent` | Standardsteuer für Städte. | 5.0 |
| `logging.debug` | Debug-Logs aktivieren. | false |
| `gui.job-board-size` | Inventargröße des Job-Boards. | 54 |
| `gui.market-size` | Inventargröße des Marktplatzes. | 54 |

## Commands
| Command | Beschreibung | Permission | Aliases |
| --- | --- | --- | --- |
| `/jobs` | Job-Board öffnen und Jobs verwalten. | `zbencityjobs.jobs` | – |
| `/market` | Marktplatz-GUI öffnen. | `zbencityjobs.market` | – |
| `/company` | Firmenbefehle (Gründung, Verwaltung). | `zbencityjobs.company` | – |
| `/city` | Städteverwaltung (Tax, Bürgermeister, Verwaltung). | `zbencityjobs.city` | – |

## Permissions
- `zbencityjobs.admin` – Bündelt alle Unterrechte (Default: op).
- Jobs: `zbencityjobs.jobs`, `.create`, `.list`, `.take`, `.submit`, `.cancel` (Default: true).
- Market: `zbencityjobs.market`, `.sell`, `.buy` (Default: true).
- Firmen: `zbencityjobs.company`, `.manage` (Manage Default: op).
- Städte: `zbencityjobs.city`, `.admin` (Default: op).

## Integrationen
- **Vault**: Economy-Transaktionen (Escrow, Käufe, Steuern) benötigen Vault + Economy-Provider.

## Beispiele
- Spielerjob erstellen: `/jobs` öffnen → Wizard-Schritte ausführen (Typ, Beschreibung, Lohn, Item angeben).
- Marktplatz: Item halten, `/market` öffnen und Angebot einstellen; Käufe direkt im GUI.
- Städteverwaltung: Mit `/city` Steuer über `economy.tax-default-percent` Vorgabe anpassen.

## Troubleshooting
- **„Vault missing“ Meldung:** Vault oder Economy-Plugin installieren/aktivieren.
- **DB-Fehler beim Start:** Schreibrechte im Plugin-Ordner und Datenbankdatei prüfen.
- **Job nicht annehmbar:** Job evtl. bereits vergeben/abgebrochen (siehe Fehlermeldung).

## Lizenz / Credits
- Autor: ZBenNoZ. Weitere Lizenzhinweise nicht angegeben.
