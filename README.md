# bukkit-view

![Maven Central Version](https://img.shields.io/maven-central/v/io.typst/view-core)

A [pure](https://en.wikipedia.org/wiki/Purely_functional_programming) library to express minecraft chest view.

There is no side effect except `BukkitView.class`, all functions just pure, therefore this can be run in multithreads -- even Bukkit part can't -- and easy to write unit tests.

Also, this library is a good showcase how to do declarative programming in Java.

[Example is here!](https://github.com/typst-io/bukkit-view/blob/main/plugin/src/main/java/io/typst/view/bukkit/plugin/ViewPlugin.java)

## Import

Modules:
- view-core
- view-bukkit
- view-bukkit-kotlin

### Gradle

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.typst:view-core:${THE_LATEST}")
    // for bukkit
    // implementation('io.typst:view-bukkit:${THE_LATEST')
    // for bukkit kotlin
    // implementation('io.typst:view-bukkit-kotlin:${THE_LATEST}')
}
```

### Maven

```xml
<dependencies>
    <dependency>
        <groupId>io.typst</groupId>
        <artifactId>view-core</artifactId>
        <version>${THE_LATEST}</version>
    </dependency>
</dependencies>
```

### Quickstart

https://github.com/typst-io/bukkit-view-template

```shell
git clone https://github.com/typst-io/bukkit-view-template
```

## Initialize

```java
public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        BukkitView.register(this);
        // To specify the custom ItemStackOps (e.g. isSimilar for ItemsAdder)
        // BukkitView.register(new MyCustomStackOps(), this);
    }
}
```

## ChestView

```java
ChestView<ItemStack, Player> subView = ...;

String title = "title";
int row = 1;
Map<Integer, ViewControl<ItemStack, Player>> controls = new HashMap<>();

controls.put(3, ViewControl.<ItemStack, Player>of(
    new ItemStack(Material.DIAMOND),
    e -> {
        // this view item don't -- also shouldn't -- know how to open view,
        // just tell what view want to open.
        return new ViewAction.Open<>(subView);
    }
));

ChestView<ItemStack, Player> view = ChestView.builder()
    .title(title)
    .row(row)
    .contents(ViewContents.ofControls(controls))
    .build();
BukkitView.openView(view, player, plugin);
```

To open asynchronously, `ViewAction.OpenAsync<I, P>(Future<ChestView<I, P>>)`:

```java
ViewControl.<ItemStack, Player>of(
    bukkitItemStack,
    e -> {
        Future<ChestView> myChestViewFuture;
        return new ViewAction.OpenAsync(myChestViewFuture);
    }
)
```

To update just contents, `ViewAction.Update` also `ViewAction.UpdateAsync(Future<ViewContents<ItemStack, Player>>)`

```java
ViewControl.<ItemStack, Player>of(
    bukkitItemStack,
    e -> new ViewAction.Update<>(newContents)
    // UpdateAsync if needed
)
```

On close the view:

```java
ChestView.<ItemStack, Player>builder()
    // ... extra configures
    .onClose(e -> {
        // true: give back the `items` not `controls`
        // false: doesn't give back
        return new ViewAction.Close<>(true);
    })
    .build();
```

## PageView

Default construction `ofDefault()` for `PageViewLayout`:

```java
import io.typst.inventory.bukkit.BukkitItemStackOps;

// Lazy `Function<PageContext, ViewControl>` not just `ViewControl`
List<Function<PageContext<ItemStack, Player>, ViewControl<ItemStack, Player>>> items = ...;
PageViewLayout<ItemStack, Player> layout = PageViewLayout.ofDefault(
    BukkitItemStackOps.INSTANCE,
    "title", 
    6, 
    Material.STONE_BUTTON.getKey().toString(),
    items
);
```

Full construction for `PageViewLayout`:

```java
import io.typst.inventory.bukkit.BukkitItemStackOps;

// Paging elements
List<Function<PageContext, ViewControl>> items = ...;
// Paging elements will be put in this slots.
List<Integer> slots = ...;
// Control means fixed view-item, won't affected by view paging.
Map<Integer, Function<PageContext<ItemStack, Player>, ViewControl<ItemStack, Player>>> controls = ...;
String title = "title";
int row = 6;
PageViewLayout<ItemStack, Player> layout = PageViewLayout.of<ItemStack, Player>(title, row, items, slots, controls, BukkitItemStackOps.INSTANCE);
```

Evaluate a single page from the layout and open:

```java
int page = 1;
ChestView<ItemStack, Player> view = layout.toView(page);
BukkitView.openView(view, player, plugin);
```

## ViewControl

Constructions:

`ViewControl.of(ItemStack, Function<ClickEvent, ViewAction>)`

`ViewControl.just(ItemStack)`

`ViewControl.consumer(ItemStack, Consumer<ClickEvent>)`

## Tests

Use JDK 21 and the Gradle wrapper to run the library tests without the example plugin:

```shell
./gradlew :view-core:test :view-bukkit:test :view-bukkit-kotlin:test --continue
```

The tests cover core item snapshots and pagination, Bukkit inventory events and scheduled
actions, item metadata, and the Kotlin builder's Java interoperability. Bukkit tests use
MockBukkit with the same Paper API version as the library, without starting a Minecraft server.

Tests tagged `regression` assert the intended behavior and remain enabled in the default test
tasks. They currently expose existing library bugs: stale item refunds on close, item loss on
disconnect or update, stale asynchronous opens, unhandled `Reopen` actions, mutation of enchantment
maps, and division by zero in layouts without content slots. The test command will fail until
those bugs are fixed; the tests do not treat buggy behavior as a successful result.

To run one reproduction:

```shell
./gradlew :view-bukkit:test --tests '*BukkitViewTest.returnsAnItemDepositedImmediatelyBeforeClosing'
```

Each module writes its HTML test report to `build/reports/tests/test/index.html`.
