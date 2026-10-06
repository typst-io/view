package io.typst.view;

import io.typst.inventory.ListInventoryAdapter;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ViewContentsTest {
    @Test
    void snapshotsEditableSlotsAndRemovesEmptyItemsWithoutChangingThePreviousContents() {
        Map<Integer, ViewControl<TestItem, String>> controls = Map.of(0, ViewControl.just(TestItem.diamond(1)));
        Map<Integer, TestItem> previousItems = Map.of(1, TestItem.diamond(8), 2, TestItem.diamond(2));
        ViewContents<TestItem, String> previous = ViewContents.of(controls, previousItems);
        TestItem deposited = TestItem.diamond(4);
        ListInventoryAdapter<TestItem> inventory = new ListInventoryAdapter<>(
                Arrays.asList(TestItem.diamond(1), null, deposited, TestItem.diamond(0)), null);

        ViewContents<TestItem, String> updated = previous.updated(TestItem.OPS, inventory);

        assertEquals(Map.of(2, deposited), updated.getItems());
        assertSame(controls, updated.getControls());
        assertEquals(previousItems, previous.getItems());
        assertEquals(8, previous.getItems().get(1).amount());
    }
}
