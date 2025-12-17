# ZBenCore (Paper 1.21.x)

Hardcoded, non-configurable branding (Tablist + /about) and small management commands without OP.

## Build
- Java 21
- `./gradlew build`
- Jar: `build/libs/ZBenCore-1.0.0.jar`

## Install
1. Put jar into `plugins/`
2. Start server
3. Configure toggles/messages in `plugins/ZBenCore/config.yml`
4. Give permissions (LuckPerms recommended)

## Permissions
- Owner rank (all permissions): `zben.owner`
- Single permissions:
  - `zben.about`
  - `zben.manage.kick`
  - `zben.manage.gamerule`
  - `zben.manage.time`
  - `zben.manage.weather`
  - `zben.manage.whitelist`

## Commands
- `/about`
- `/zben kick <player> <reason...>`
- `/zben gamerule <rule> <true|false|value>`
- `/zben time <day|night|set <value>>`
- `/zben weather <sun|rain|thunder>`
- `/zben whitelist <on|off|add|remove|list> [player]`
