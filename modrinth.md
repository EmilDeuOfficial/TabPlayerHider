<!-- MODRINTH SUMMARY (paste into Project Settings → Summary, max 256 chars) -->
<!-- Hides specific players from the tab list without removing them from the game world — ideal for hiding bots on your server. No dependencies. -->

## 👁️ TabPlayerHider

Hides specific players from the tab list without removing them from the game world.

Perfect for servers running bots — players in `hidden-players` are invisible in the tab list but fully present in-game, unaffected by lag, collision, or visibility.

### ✨ Features

- Removes configured players from every client's tab list on join (1-tick delayed packet)
- Immediate hide/show via commands — no relog needed
- Config-driven: add bot names once, persisted across restarts
- Tab-complete for `/tabh hide` shows online players; `/tabh show` shows currently hidden names

### ⚙️ Configuration

```yaml
# config.yml
hidden-players:
  - "BotName1"
  - "BotName2"
```

Names are **case-sensitive** and must match the exact Minecraft username.

### 🔑 Permissions

| Permission | Description | Default |
|---|---|---|
| `tabplayerhider.admin` | Access to all `/tabh` commands | op |

### 💬 Commands

Main command: `/tabplayerhide` · Alias: `/tph`

| Command | Description |
|---|---|
| `/tph on` | Enable hiding (hidden players disappear from tab list) |
| `/tph off` | Disable hiding (all players visible in tab list again) |
| `/tph status` | Show current state, hidden player count, and who is online |
| `/tph hide <player>` | Hide a player from the tab list (saves to config) |
| `/tph show <player>` | Show a player in the tab list again |
| `/tph list` | List all currently hidden player names |
| `/tph reload` | Reload config and re-apply hiding |

### 📋 Requirements

| | |
|---|---|
| Server | Paper 1.21.x |
| Java | 21+ |

### 🔧 How it works

On every player join, the plugin sends a `ClientboundPlayerInfoRemovePacket` (1 tick after join, after the server's own player-info broadcast) for each hidden player directly to the joining client. Because this only manipulates the tab list packet — and not the entity tracking or scoreboard — hidden players remain fully visible in the world.
