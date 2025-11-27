package io.typst.view;

import lombok.Data;
import lombok.With;

import java.util.function.Consumer;
import java.util.function.Function;

@Data(staticConstructor = "of")
@With
public class ViewControl<I, P> {
    private final Function<OpenEvent<I, P>, I> item;
    private final Function<ClickEvent<I, P>, ViewAction<I, P>> onClick;

    public static <I, P> ViewControl<I, P> just(I item) {
        return of(item, ignore -> ViewAction.nothing());
    }

    public static <I, P> ViewControl<I, P> consumer(I item, Consumer<ClickEvent<I, P>> onClick) {
        return of(item, e -> {
            onClick.accept(e);
            return ViewAction.nothing();
        });
    }

    public static <I, P> ViewControl<I, P> of(I item, Function<ClickEvent<I, P>, ViewAction<I, P>> onClick) {
        return ViewControl.<I, P>of(e -> item, onClick);
    }

    public I getItem(OpenEvent<I, P> event) {
        return item.apply(event);
    }
}
