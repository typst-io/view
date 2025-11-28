# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

bukkit-view is a pure functional library for creating declarative Minecraft chest GUIs in Bukkit/Spigot plugins. The library emphasizes immutability and functional programming principles, making it thread-safe and easy to test. All functions are pure except for `BukkitView.class`.

Published to Maven Central as `io.typst:bukkit-view-core`.

## Multi-Module Structure

This is a Gradle multi-module project with the following modules:

- **view-core**: Pure Java core library with no Bukkit dependencies. Contains the fundamental data structures (`ChestView`, `ViewControl`, `ViewAction`, `ViewContents`, `PageViewLayout`). This is the API that consumers interact with.
- **view-bukkit**: Bukkit integration layer. Depends on `view-core` and `inventory-bukkit`. Contains `BukkitView` which handles actual inventory operations and event listening.
- **view-bukkit-kotlin**: Kotlin extensions for the Bukkit module. Provides idiomatic Kotlin APIs.
- **view-plugin**: Example Bukkit plugin demonstrating library usage. Uses the shadow plugin to package dependencies.

Module naming convention: root project is `view`, but modules are named `view-{module}` (e.g., `view-core`, `view-bukkit`).

## Build Commands

```bash
# Build all modules
./gradlew build

# Build specific module
./gradlew :view-core:build
./gradlew :view-bukkit:build

# Run tests
./gradlew test
./gradlew :view-core:test

# Create plugin JAR (with dependencies shaded)
./gradlew :view-plugin:shadowJar

# Run debug server with plugin (from plugin module)
./gradlew :view-plugin:debugViewPlugin

# Clean debug server
./gradlew :view-plugin:cleanDebugViewPlugin

# Clean all builds
./gradlew clean
```

## Architecture Principles

### Pure Functional Core

The `view-core` module is designed to be purely functional:
- All view data structures are immutable (using Lombok `@Value` and `@With`)
- No side effects except in `BukkitView.class`
- State changes return new instances via `with*()` methods
- Thread-safe by design

### Sealed ViewAction Hierarchy

`ViewAction` is a sealed interface representing all possible view state transitions:
- `ViewAction.Open<I, P>`: Open a new view
- `ViewAction.OpenAsync<I, P>`: Open view from a Future
- `ViewAction.Update<I, P>`: Update current view contents
- `ViewAction.UpdateAsync<I, P>`: Update from a Future
- `ViewAction.Close<I, P>`: Close view (optionally give back items)
- `ViewAction.Reopen<I, P>`: Reopen current view
- `ViewAction.Nothing<I, P>`: No action

When modifying view actions, preserve the sealed interface structure and ensure all implementations are record-like with `@Value`.

### Generic Item and Player Types

Views are parameterized by `<I, P>` types:
- `I`: Item type (e.g., `ItemStack` for Bukkit)
- `P`: Player type (e.g., `Player` for Bukkit)

This allows the core to remain platform-agnostic. The `ItemStackOps<I>` interface abstracts item operations.

### ViewControl vs ViewContents

- **ViewControl**: Represents a single interactive slot with an item and click handler (`Function<ClickEvent, ViewAction>`)
- **ViewContents**: Container holding both fixed controls (readonly) and player-accessible items (mutable)
  - `controls`: Map of slot → ViewControl (fixed, not removable by players)
  - `items`: Map of slot → item (player can modify these)

### Event Flow

1. **Open**: `BukkitView.openView()` creates inventory, sets holder, calls `OpenEvent` callbacks
2. **Click**: `InventoryClickEvent` → validates against controls → calls `ViewControl.onClick()` → returns `ViewAction` → handled synchronously or asynchronously
3. **Update**: View contents can be updated via `ViewAction.Update` without closing/reopening
4. **Close**: `InventoryCloseEvent` → calls `onClose()` → handles parent views (modal behavior) → optionally gives back items

### PageViewLayout Pattern

`PageViewLayout` is a builder for paginated views:
- Separates page logic from view rendering
- `elements`: List of lazy `Function<PageContext, ViewControl>` for paged items
- `slots`: Which inventory slots should contain paged items
- `controls`: Fixed controls (like page navigation buttons)
- `toView(page)`: Evaluates a specific page into a `ChestView`

The `PageContext` provides access to current page, max page, and layout for navigation controls.

## Development Notes

### Java 21 Toolchain

All modules use Java 21. Ensure your JDK is configured correctly:
```bash
# Check Java version
java -version
```

### Dependencies

The project uses a custom Gradle plugin `io.typst:spigradle` for Spigot development:
- Automatically downloads Spigot APIs
- Provides `debugSpigot` task for testing plugins
- Uses `spigot('1.21.8')` dependency notation

Core dependencies:
- Lombok 1.18.36 (compile-only, annotation processor)
- JetBrains annotations 26.0.2-1
- `io.typst:inventory-core:2.4.0` (abstraction for inventory operations)
- JUnit 5.8.1 (testing)

### Working with ViewAction

After recent refactoring, `ViewAction` is now sealed. When adding new action types:
1. Add a new sealed implementation in `ViewAction.java`
2. Update `BukkitViewListener.handleAction()` to handle the new type
3. Ensure the action type uses `@Value` and `@With` for immutability
4. Add corresponding test cases

### Testing Pure Functions

Since most code is pure, tests can be written without mocking:
```java
// Good: Test pure view construction
ChestView<ItemStack, Player> view = ChestView.builder()
    .title("Test")
    .row(3)
    .contents(ViewContents.ofControls(controls))
    .build();

// Verify view properties directly
assertEquals("Test", view.getTitle());
```

### Publishing

Modules are published to Maven Central via Sonatype. The `registerPublish` function in root `build.gradle` configures:
- Maven coordinates
- POM metadata
- GPG signing
- Sources and Javadoc JARs

Publish with credentials:
```bash
./gradlew publish -PossrhUsername=... -PossrhPassword=...
```
