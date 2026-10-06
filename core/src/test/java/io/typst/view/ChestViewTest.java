package io.typst.view;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ChestViewTest {
    @Test
    void findsSpaceWithoutUsingControlsFullStacksOrDifferentItems() {
        TestItem partial = TestItem.diamond(62);
        ChestView<TestItem, String> view = ChestView.<TestItem, String>builder(TestItem.OPS)
                .contents(ViewContents.of(
                        Map.of(0, ViewControl.just(TestItem.diamond(1))),
                        Map.of(1, TestItem.diamond(64), 2, new TestItem("minecraft:gold_ingot", 1, 64), 3, partial)))
                .build();

        assertEquals(List.of(3, 4), view.findSpaces(List.of(0, 1, 2, 3, 4, 5), TestItem.diamond(5)));
        assertEquals(62, partial.amount());
    }

    @Test
    void stopsAfterEnoughCapacityHasBeenFound() {
        ChestView<TestItem, String> view = ChestView.<TestItem, String>builder(TestItem.OPS).build();

        assertEquals(List.of(0, 1), view.findSpaces(List.of(0, 1, 2), TestItem.diamond(65)));
        assertEquals(List.of(), view.findSpaces(List.of(0), TestItem.diamond(0)));
    }

    @Test
    void replacingContentsLeavesTheOriginalViewIntact() {
        ChestView<TestItem, String> original = ChestView.<TestItem, String>builder(TestItem.OPS).build();
        ViewContents<TestItem, String> contents = ViewContents.ofControls(
                Map.of(0, ViewControl.just(TestItem.diamond(1))));

        ChestView<TestItem, String> updated = original.withContents(contents);

        assertEquals(Map.of(), original.getContents().getControls());
        assertSame(contents, updated.getContents());
        assertSame(TestItem.OPS, updated.getItemOps());
    }
}
