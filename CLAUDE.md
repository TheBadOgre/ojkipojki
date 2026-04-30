# ojkipojki

Simplified tabletop simulator. Lets you load sprite sheets, spawn tokens on a board, and move them. Think Tabletop Simulator but minimal.

## Current state

No CLI or dedicated entrypoint yet. `Main.kt` starts both server and client in the same JVM on port 12002 for development.

## Architecture

Client-server over TCP sockets. Server owns all mutable state. Client holds a read-only mirror of that state updated via events.

```
Client                          Server
  |                               |
  |-- Command (TCP) ------------> |
  |                         handle command
  |                         update ModelRepository
  |                               |
  | <-- Event (TCP, broadcast) -- |
  update StateRepository          |
```

### Server (`server/`)

- **`ModelRepository`** — in-memory store (`ConcurrentHashMap`) for mutable model objects: `SpriteBagModel`, `SpriteModel`, `TokenModel`.
- **`CommandDispatcher`** — routes incoming commands to their handler by type.
- **`EventBroadcastService`** — sends an event to all connected clients.
- **`ClientSessionManager`** / **`ConnectionManager`** — manage TCP connections and per-client sessions.

Models (`server/model/`) are mutable classes with `apply(domain)` to absorb domain state and `toState()` to produce domain objects for events.

### Client (`client/`)

- **`StateRepository`** — in-memory store (`ConcurrentHashMap`) for immutable domain objects: `SpriteBag`, `Sprite`, `Token`.
- **`EventDispatcher`** — routes incoming events to their handler by type.
- **`CommandTransmitter`** — sends commands to the server over the socket.
- **`SpriteLoader`** — loads sprite bags from a directory of PNGs (see below).

UI is not yet implemented.

### Shared (`shared/`)

Domain classes and protocol interfaces used by both sides.

**Domain objects** (immutable, used as-is on client):
- `SpriteBag(id, sprites)` — a named collection of sprites.
- `Sprite(id, frontImageBytes, backImageBytes)` — one game piece, front and back images as PNG bytes.
- `Token(id, spriteId, position, rotation, index, flipped)` — a placed instance of a sprite on the board.

**IDs:**
- `SpriteBagId(String)` — simple string name (e.g. `"dice"`).
- `SpriteId(SpriteBagId, red, green, blue)` — composite key; the RGB comes from the mask file (see SpriteLoader).
- `TokenId(UUID)` — random UUID assigned at spawn.

**Protocol abstractions:** `Handler<A>`, `Dispatcher<A>`, `Receiver`, `Transmitter` — generic interfaces for the command/event pipeline.

## Commands and Events

| Direction | Type | Effect |
|---|---|---|
| Client → Server | `UploadSpriteBagsCommand(spriteBags)` | Upsert sprite bags in `ModelRepository`, broadcast `SpriteBagsUpdatedEvent` |
| Client → Server | `SpawnTokensCommand(spriteBagId)` | Create one `TokenModel` per sprite in the bag (random x ∈ [-100,100], y increments by 20), broadcast `TokensUpdatedEvent` |
| Client → Server | `MoveTokensCommand(adjustments)` | Update position/rotation/index/flipped on existing tokens; null field = no change; broadcast `TokensUpdatedEvent` |
| Server → Clients | `SpriteBagsUpdatedEvent(spriteBags)` | Full list of all sprite bags — client saves each, no purge |
| Server → Clients | `TokensUpdatedEvent(tokens)` | Complete token list — client replaces all tokens, no leftovers |

Events always carry **complete state**, never deltas.

## Sprite loading (`SpriteLoader`)

Reads a directory of PNGs. Files are grouped by the prefix before the first `_`. Each group needs three files:

```
{id}_front.png   — front face of all sprites in this bag
{id}_back.png    — back face
{id}_mask.png    — color map: each unique non-black RGB color defines one sprite
```

`SpriteId` encodes the RGB from the mask. The loader finds the bounding box of each color region, crops it out of front/back images, and masks pixels belonging to other sprites transparent.

## Package structure

```
net.rafkos.ojkipojki
├── shared/
│   ├── domain/          # Domain classes + IDs (immutable, Serializable)
│   └── protocol/        # Handler, Dispatcher, Receiver, Transmitter + command/event types
├── server/
│   ├── model/           # Mutable model classes (apply/toState)
│   ├── application/     # ModelRepository
│   └── protocol/
│       ├── command/     # CommandDispatcher + handlers
│       └── event/       # EventBroadcastService, EventTransmitter
└── client/
    ├── application/     # StateRepository, SpriteLoader
    └── protocol/
        ├── command/     # CommandTransmitter
        └── event/       # EventDispatcher + handlers
```
