package io.typst.view;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ViewControlTest {
    @Test
    void evaluatesDisplayItemsForEachPlayerWhenRequested() {
        ChestView<TestItem, String> view = ChestView.<TestItem, String>builder(TestItem.OPS).build();
        ViewControl<TestItem, String> control = ViewControl.<TestItem, String>of(
                event -> new TestItem(event.getPlayer(), 1, 64), event -> ViewAction.nothing());

        assertEquals("Alice", control.getItem(new OpenEvent<>("Alice", view)).type());
        assertEquals("Bob", control.getItem(new OpenEvent<>("Bob", view)).type());
    }

    @Test
    void forwardsTheClickToAConsumerAndReturnsNothing() {
        AtomicReference<ClickEvent<TestItem, String>> received = new AtomicReference<>();
        ViewControl<TestItem, String> control = ViewControl.consumer(TestItem.diamond(1), received::set);
        ChestView<TestItem, String> view = ChestView.<TestItem, String>builder(TestItem.OPS).build();
        ClickEvent<TestItem, String> click = new ClickEvent<>(view, "Alice", "LEFT", "PICKUP_ALL", -1);

        ViewAction<TestItem, String> action = control.getOnClick().apply(click);

        assertSame(click, received.get());
        assertSame(ViewAction.nothing(), action);
    }
}
