package io.typst.view.page;

import io.typst.view.ChestView;
import io.typst.view.ClickEvent;
import io.typst.view.OpenEvent;
import io.typst.view.TestItem;
import io.typst.view.ViewAction;
import io.typst.view.ViewControl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PageViewLayoutTest {
    @ParameterizedTest
    @CsvSource({"-1, 0, 3", "0, 0, 3", "1, 0, 3", "2, 3, 3", "3, 6, 1", "4, 6, 1"})
    void clampsPagesAndPlacesOnlyTheirElementsInConfiguredSlots(int page, int firstElement, int count) {
        List<Integer> slots = List.of(0, 2, 4);
        PageViewLayout<TestItem, String> layout = PageViewLayout.of(
                "Pages", 1, elements(7), slots,
                Map.of(8, context -> ViewControl.just(new TestItem("page-" + context.getPage(), 1, 64))),
                context -> event -> ViewAction.nothing(), TestItem.OPS);

        ChestView<TestItem, String> view = layout.toView(page);

        assertEquals(count + 1, view.getContents().getControls().size());
        for (int index = 0; index < count; index++) {
            TestItem item = view.getContents().getControls().get(slots.get(index)).getItem(
                    new OpenEvent<>("Alice", view));
            assertEquals("element-" + (firstElement + index), item.type());
        }
        assertEquals(Map.of(), view.getContents().getItems());
    }

    @Test
    void emptyElementsStillProduceTheFirstPageAndNavigationControls() {
        PageViewLayout<TestItem, String> layout = PageViewLayout.ofDefault(
                TestItem.OPS, "Empty", 2, "minecraft:stone_button", elements(0));

        ChestView<TestItem, String> view = layout.toView(10);

        assertEquals(Set.of(12, 14), view.getContents().getControls().keySet());
        assertEquals(Set.of(12, 14), navigate(view, 12).getContents().getControls().keySet());
        assertEquals(Set.of(12, 14), navigate(view, 14).getContents().getControls().keySet());
    }

    @Test
    void nextAndPreviousControlsSelectTheAdjacentPage() {
        PageViewLayout<TestItem, String> layout = PageViewLayout.ofDefault(
                TestItem.OPS, "Pages", 2, "minecraft:stone_button", elements(19));
        ChestView<TestItem, String> middle = layout.toView(2);

        assertEquals("element-0", navigate(middle, 12).getContents().getControls().get(0)
                .getItem(new OpenEvent<>("Alice", middle)).type());
        assertEquals(3, navigate(middle, 14).getContents().getControls().size());
        assertEquals("element-18", navigate(middle, 14).getContents().getControls().get(0)
                .getItem(new OpenEvent<>("Alice", middle)).type());
    }

    @Test
    @Tag("regression")
    void rejectsDefaultLayoutsWithoutAnyContentRowInsteadOfDividingByZero() {
        assertThrows(IllegalArgumentException.class, () -> PageViewLayout.ofDefault(
                TestItem.OPS, "Pages", 1, "minecraft:stone_button", elements(0)).toView(1));
    }

    private static ViewAction.Update<TestItem, String> navigate(ChestView<TestItem, String> view, int slot) {
        ViewAction<TestItem, String> action = view.getContents().getControls().get(slot).getOnClick()
                .apply(new ClickEvent<>(view, "Alice", "LEFT", "PICKUP_ALL", -1));
        @SuppressWarnings("unchecked")
        ViewAction.Update<TestItem, String> update = (ViewAction.Update<TestItem, String>)
                assertInstanceOf(ViewAction.Update.class, action);
        return update;
    }

    private static List<Function<PageContext<TestItem, String>, ViewControl<TestItem, String>>> elements(int count) {
        return IntStream.range(0, count)
                .<Function<PageContext<TestItem, String>, ViewControl<TestItem, String>>>mapToObj(
                        index -> context -> ViewControl.just(new TestItem("element-" + index, 1, 64)))
                .toList();
    }
}
