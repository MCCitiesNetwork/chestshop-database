# ChestShop Database

A Paper plugin for Minecraft 1.21.8 that persists [ChestShop](https://www.spigotmc.org/resources/chestshop.51856/) data to a MariaDB database, enabling server-wide shop search, price browsing, and hologram previews.

Built for [DemocracyCraft](https://democracycraft.net/).

## Features

- **Shop tracking** — Automatically records shops as they are created, destroyed, and restocked in real time.
- **Search GUI** — `/csdb find` Opens a paginated chest GUI; filter by shop type (buy/sell), hide empty or full shops, and sort by price, stock, quantity, or distance.
- **Hologram previews** — Floating item displays above each shop sign, loaded per-chunk; togglable per player.
- **WorldEdit / FAWE integration** — Shops removed by a WorldEdit operation are automatically cleaned up from the database.
- **WorldGuard integration** — Exposes a `%region-name%` placeholder usable in shop display templates.

## Requirements

| Dependency | Required |
|---|---|
| Paper 1.21.8 | Yes |
| ChestShop | Yes |
| MariaDB | Yes |
| WorldEdit or FAWE | No |
| WorldGuard | No |

## Installation

1. Drop the plugin JAR into your `plugins/` folder.
2. Start the server once to generate config files, then stop it.
3. Edit `plugins/ChestShopDatabase/database-settings.yml` with your MariaDB connection details.
4. Run the SQL schema in `sql/create_tables_maria.sql` against your database.
5. Start the server.

## Commands

| Command | Permission | Description |
|---|---|---|
| `/csdb find` | `csdb.find` | Open the shop search GUI |
| `/csdb find toggle preview <true\|false>` | `csdb.preview.toggle` | Toggle hologram previews for yourself |
| `/csdb find toggle visibility <true\|false>` | `csdb.visibility.toggle` | Show or hide a shop in search results |
| `/csdb resync <chunksPerTick>` | `csdb.resync` | Full database resync by scanning all loaded chunks |
| `/csdb reload` | op | Reload config, messages, and item code groupings |

**Note:** In DemocracyCraft server, these commands are just "/find" or "/find toggle preview", with no "csdb" required. 

## Configuration

| File | Purpose |
|---|---|
| `database-settings.yml` | MariaDB URL, username, password |
| `settings.yml` | Shop icon templates, lore, click command, preview scale |
| `item-code-groupings.yml` | Item code aliases mapped to canonical item codes |
| `messages.yml` | Player-facing messages |

## Building

```bash
./gradlew build
```

Output JARs are placed in each module's `build/libs/` directory.

## Project Structure

```
chestshop-database/
├── core/                    # Shared models, DB interfaces, utilities
├── chestshop-database-bukkit/  # Main plugin (commands, GUI, listeners)
├── adapters/
│   ├── worldedit/           # WorldEdit integration
│   ├── fawe/                # FastAsyncWorldEdit integration
│   └── worldguard/          # WorldGuard region placeholder
└── sql/
    └── create_tables_maria.sql
```
