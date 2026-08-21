# Installation

## Download from Releases

1. Go to [GitHub Releases](https://github.com/HeCUana/CRPI-FakePlayer/releases) and download the latest version
2. Place `crpi-fakeplayer-x.x.x.jar` in your server's `mods/` directory
3. Ensure these dependencies are also in `mods/`:
   - [Fabric API](https://modrinth.com/mod/fabric-api)
   - [Carpet Mod](https://modrinth.com/mod/carpet)

## Build from Source

```bash
# Clone the repository
git clone https://github.com/HeCUana/CRPI-FakePlayer.git
cd CRPI-FakePlayer

# Build (requires JDK 21)
gradlew.bat build

# Build artifact located at
# build/libs/crpi-fakeplayer-x.x.x.jar
```

## Server Deployment

1. Install [Fabric Loader 0.19.3+](https://fabricmc.net/use/installer/) on your server
2. Place all jar files in the `mods/` directory
3. Start the server once to generate config files
4. Edit `config/carpet.conf` to adjust rules (optional)

## Verify Installation

After starting the server, run:

```
/crpi fp list
```

If it returns a fake player list (or empty list), installation is successful.

## Directory Structure

```
server/
├── mods/
│   ├── crpi-fakeplayer-x.x.x.jar    # This mod
│   ├── fabric-api-x.x.x.jar         # Fabric API
│   └── carpet-x.x.x.jar             # Carpet
├── config/
│   └── carpet.conf                   # Carpet rule config
└── world/
    └── ...
```

## Troubleshooting

### Q: `Missing mod dependencies` error on startup

Ensure Fabric Loader, Fabric API, and Carpet versions meet requirements. Check `logs/latest.log` for specific error messages.

### Q: `/crpi fp` command doesn't exist

Confirm Carpet mod is loaded correctly. CRPI-FakePlayer's command registration depends on Carpet's extension mechanism.

### Q: Fake player spawned but can't execute behaviors

Check that the Carpet rule `fakePlayerActions` is `true` (default). Run:

```
/crpi-fakeplayer fakePlayerActions true
```
