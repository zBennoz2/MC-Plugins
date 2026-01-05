# ZBenCoins – Markt & Jobs Übersicht

Dieses Plugin stellt den Marktplatz und das Job-System für den Server bereit. Die wichtigsten Infos:

## Schnellstart
- `/markt` öffnet das Hauptmenü. Coins werden direkt im GUI angezeigt, inkl. Währungsname und Hinweis auf `/pay`.
- Im Marktplatz kannst du Angebote durchsuchen, filtern und sortieren. Blättern per Pfeile, aktive Sortierung wird angezeigt.
- Jobs lassen sich ebenso durchsuchen und nach Typ/Status filtern sowie nach Belohnung oder Ablauf sortieren.

## Berechtigungen
- `zbencoins.market.search` – aktiviert die Suche im Marktplatz.
- `zbencoins.market.filter` – schaltet Filter und Sortierung im Marktplatz frei.
- `zbencoins.market.create` – erlaubt das Erstellen von Spieler-Angeboten (Standard: true).
- `zbencoins.serveroffers.use` – erlaubt den Kauf/Verkauf bei Server-Angeboten (Standard: true).
- `zbencoins.admin.serveroffers` – erlaubt das Erstellen, Bearbeiten und Löschen von Server-Angeboten.
- `zbencoins.serveroffers.bypasslimits` – ignoriert Mengenlimits in Server-Angeboten.
- `zbencoins.jobs.search` – erlaubt die Jobsuche.
- `zbencoins.jobs.filter` – erlaubt Job-Filter und Sortierung.

Ohne Berechtigung sind die Buttons im GUI ausgegraut und informieren den Spieler.

## Server-Angebote (Admin)
- Öffne `/markt` und wähle im Hauptmenü **Server-Shop** oder **Server-Ankauf**.
- Halte das gewünschte Item in der Hand und klicke auf **Server-Angebot erstellen**. Preis/Min/Max werden im Chat abgefragt.
- Mit Rechtsklick auf ein bestehendes Angebot öffnest du die Verwaltung (Preis/Limits anpassen, aktivieren/deaktivieren). Shift-Rechtsklick löscht ein Angebot.
- Alternativ: `/serveroffer create <buy|sell> <preis> [min] [max]` nutzt das Item aus der Hand.

### Wöchentliches Limit (7 Minecraft-Tage)
- Im Erstell-/Bearbeitungs-GUI kann mit dem **Wöchentliches Limit**-Button die Mengenbegrenzung aktiviert/deaktiviert werden.
- Über **Max pro 7 Tage setzen** wird die Gesamtmenge abgefragt (0 oder -1 bedeutet unbegrenzt). Die Eingabe erfolgt im Chat und wird direkt gespeichert.
- Das Limit zählt alle Käufe/Verkäufe über 7 Minecraft-Tage (Standard: 168000 Ticks ≈ 140 Minuten Realtime). Danach wird es automatisch zurückgesetzt.

## Hinweise
- Alle Texte im GUI sind auf Deutsch gehalten.
- Die Filter arbeiten clientfreundlich: Eingaben laufen über den Chat und laden das jeweilige GUI direkt neu.
- Pagination zeigt "Seite X/Y" an, sodass Spieler immer wissen, wo sie sich befinden.
