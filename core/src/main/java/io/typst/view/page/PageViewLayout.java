package io.typst.view.page;

import io.typst.inventory.ItemKey;
import io.typst.inventory.ItemStackOps;
import io.typst.view.ChestView;
import io.typst.view.CloseEvent;
import io.typst.view.ViewContents;
import io.typst.view.ViewControl;
import io.typst.view.ViewAction;
import lombok.Data;
import lombok.With;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Data(staticConstructor = "of")
@With
public class PageViewLayout<I, P> {
    private final String title;
    private final int row;
    private final List<Function<PageContext<I, P>, ViewControl<I, P>>> elements;
    private final List<Integer> slots;
    private final Map<Integer, Function<PageContext<I, P>, ViewControl<I, P>>> controls;
    private final Function<PageContext<I, P>, Function<CloseEvent<I, P>, ViewAction<I, P>>> onClose;
    private final ItemStackOps<I> itemOps;

    public static <I, P> PageViewLayout<I, P> ofDefault(ItemStackOps<I> itemOps, String title, int row, String buttonMaterial, List<Function<PageContext<I, P>, ViewControl<I, P>>> elements) {
        int cSize = (row - 1) * 9;
        List<Integer> slots = IntStream.range(0, cSize).boxed().collect(Collectors.toList());
        Map<Integer, Function<PageContext<I, P>, ViewControl<I, P>>> controls = new HashMap<>();
        controls.put(cSize + 3, ctx -> ViewControl.<I, P>of(
                e -> {
                    I item = itemOps.create(new ItemKey(buttonMaterial, String.format("<- %s/%s", ctx.getPage(), ctx.getMaxPage())));
                    itemOps.setAmount(item, Math.max(1, ctx.getPage()));
                    return item;
                },
                e -> new ViewAction.Update<I, P>(ctx.getLayout().toView(ctx.getPage() - 1).getContents())
        ));
        controls.put(cSize + 5, ctx -> ViewControl.<I, P>of(
                e -> {
                    I item = itemOps.create(new ItemKey(buttonMaterial, String.format("%s/%s ->", ctx.getPage(), ctx.getMaxPage())));
                    itemOps.setAmount(item, Math.max(1, ctx.getPage()));
                    return item;
                },
                e -> new ViewAction.Update<I, P>(ctx.getLayout().toView(ctx.getPage() + 1).getContents())
        ));
        return of(title, row, elements, slots, controls, ptx -> e -> new ViewAction.Close<>(true), itemOps);
    }

    public ChestView<I, P> toView(int page) {
        Map<Integer, ViewControl<I, P>> viewControls = new HashMap<>();
        int contentSize = getSlots().size();
        int count = getElements().size();
        int maxPage = Math.max(1, count / contentSize + Math.min(count % contentSize, 1));
        int coercedPage = Math.max(Math.min(page, maxPage), 1);
        PageContext<I, P> ctx = PageContext.of(this, maxPage, coercedPage);
        List<Function<PageContext<I, P>, ViewControl<I, P>>> subItemList = pagingList(contentSize, coercedPage, getElements());
        // Contents
        for (int i = 0; i < subItemList.size(); i++) {
            int slot = getSlots().get(i);
            viewControls.put(slot, subItemList.get(i).apply(ctx));
        }
        // Controls
        for (Map.Entry<Integer, Function<PageContext<I, P>, ViewControl<I, P>>> pair : getControls().entrySet()) {
            ViewControl<I, P> control = pair.getValue().apply(ctx);
            viewControls.put(pair.getKey(), control);
        }
        ViewContents<I, P> contents = ViewContents.ofControls(viewControls);
        return ChestView.<I, P>builder()
                .title(title)
                .row(row)
                .contents(contents)
                .onClose(onClose.apply(ctx))
                .itemOps(itemOps)
                .build();
    }

    private static <T> List<T> pagingList(int elementSize, int page, List<T> list) {
        int start = (page - 1) * elementSize;
        int end = page * elementSize;
        return list.isEmpty()
                ? Collections.emptyList()
                : list.subList(
                Math.min(Math.max(start, 0), list.size()),
                Math.min(Math.max(end, 0), list.size()));
    }
}
