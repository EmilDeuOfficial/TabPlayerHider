# TabPlayerHider

Hides specific players from the tab list without removing them from the game world. Useful for servers running bots: names listed in `hidden-players` disappear from the tab list but stay fully present in game, with normal collision, visibility and behaviour.

## Features

- Removes configured players from every client's tab list on join, using a 1 tick delayed packet
- Hide and show players instantly via command, no relog required
- Bot names are stored in the config and persist across restarts
- Tab completion for `/tabh hide` lists online players, `/tabh show` lists currently hidden names

## Requirements

| | |
|---|---|
| Server | Paper 1.21.x |
| Java | 21+ |

## Installation

1. Download the latest JAR from [Releases](../../releases)
2. Drop it into your server's `plugins/` folder
3. Restart the server
4. Add your bot names with `/tph hide <player>` or directly in `config.yml`

## Configuration

```yaml
# config.yml
hidden-players:
  - "BotName1"
  - "BotName2"
```

Names are case-sensitive and must match the exact Minecraft username.

## Permissions

| Permission | Description | Default |
|---|---|---|
| `tabplayerhider.admin` | Access to all `/tabh` commands | op |

## Commands

Main command: `/tabplayerhide`, alias: `/tph`

| Command | Description |
|---|---|
| `/tph on` | Enable hiding, hidden players disappear from the tab list |
| `/tph off` | Disable hiding, all players are visible in the tab list again |
| `/tph status` | Show current state, hidden player count and who is online |
| `/tph hide <player>` | Hide a player from the tab list and save it to the config |
| `/tph show <player>` | Show a player in the tab list again |
| `/tph list` | List all currently hidden player names |
| `/tph reload` | Reload the config and re-apply hiding |

## How it works

On every player join the plugin sends a `ClientboundPlayerInfoRemovePacket` for each hidden player directly to the joining client, one tick after join so it lands after the server's own player-info broadcast. Only the tab list packet is touched, not entity tracking or the scoreboard, so hidden players stay fully visible in the world.

### Build setup (NMS access)

The plugin sends raw packets, so it needs NMS. Put your Paper 1.21.4 server JAR in `server/` and install it into your local Maven repository:

```bash
mvn install:install-file \
  -Dfile=server/paper-1.21.4.jar \
  -DgroupId=io.papermc.paper \
  -DartifactId=paper-server \
  -Dversion=1.21.4-R0.1-SNAPSHOT \
  -Dpackaging=jar
```

Then build normally:

```bash
mvn package
```

Output: `target/TabPlayerHider-1.0.jar`, copy it to `plugins/`.

## Source

[github.com/EmilDeuOfficial/TabPlayerHider](https://github.com/EmilDeuOfficial/TabPlayerHider)
