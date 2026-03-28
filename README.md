[![build](https://github.com/porkyoot/piggy-lib/actions/workflows/build.yml/badge.svg)](https://github.com/porkyoot/piggy-lib/actions/workflows/build.yml)
[![release](https://img.shields.io/github/v/release/porkyoot/piggy-lib)](https://github.com/porkyoot/piggy-lib/releases)

# Piggy Lib

# Piggy Lib

A shared library providing the core infrastructure and high-performance utilities for all Piggy mods. It centralizes common systems to ensure consistency, stability, and anti-cheat compliance.

---

## ⚡ Central Piggy Action Queue

The most critical component of the Piggy suite. All player interactions (clicking slots, placing blocks, using items) must be routed through this queue.

*   **Stateful Actions**: Interactions are wrapped in `IAction` objects (e.g., `InteractBlockAction`, `MoveItemAction`).
*   **Verification over Assumption**: Actions are not considered "done" when the packet is sent. They use an `onExecute` phase to trigger and a `verify` phase to poll for success (e.g., checking if an item actually moved to the destination slot).
*   **Rate Limiting & CPS**: A global Clicks-Per-Second (CPS) limiter follows configurable server rules. Routine actions (sorting) are throttled, while life-saving actions (MLG) use `ActionPriority.HIGHEST` to bypass the limiter.
*   **Packet Bundling**: Groups related interactions into single-tick executions when safe, minimizing network overhead.

## 📊 Performance & Telemetry

### PerfMonitor
A robust utility for tracking server-side health and network conditions.
*   **Dynamic TPS Tracking**: Calculates real-time Server Ticks Per Second.
*   **Latency Calculation**: Provides stable, smoothed ping data used by `piggy-build` for preemptive action triggering.

### Meta-Action Sessions
A sophisticated telemetry engine for diagnosing complex failures.
*   **Contextual Logging**: Captures inventory states, player positions, and world metadata before and after actions.
*   **Forensic Reports**: Generates detailed JSON/Markdown reports for failed operations (like a failed inventory sort or a missed MLG).
*   **Rich Formatting**: Uses `PiggyTelemetryFormatter` to provide interactive, clickable chat components for administrative review.

## 🎒 Inventory API

### InventorySearcher
A modular, high-performance engine for finding items across multiple containers.
*   **Condition-Based Search**: Filter items by Type, Tag, NBT, or custom predicates.
*   **Multi-Slot Resolution**: Handles complex searches involving split stacks or "mega-stacks" (>64 items).

## 🎨 UI & Configuration

*   **Unified Radial Menus**: Standardized `GenericRadialMenuScreen` for consistent selection interfaces.
*   **YACL Config Wrappers**: A simplified factory pattern for building beautiful, categorized configuration screens using **Yet Another Config Lib**.
*   **Anti-Cheat Feedback**: Hud overlays and floating notifications that inform players when features are restricted by the server.

---

## Dependencies

*   **[Fabric API](https://modrinth.com/mod/fabric-api)**
*   **[YACL](https://modrinth.com/mod/yacl)** (Required for config UI)

## License
**CC0-1.0**