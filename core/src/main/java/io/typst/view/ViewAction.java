package io.typst.view;

import lombok.Value;
import lombok.With;

import java.util.concurrent.Future;

/**
 * The view actions will be handled by onClick, onClose in {@link ViewControl}
 */
@SuppressWarnings("unchecked")
public sealed interface ViewAction<I, P> {
    @Value
    @With
    class Close<I, P> implements ViewAction<I, P> {
        boolean giveBackItems;
    }

    Nothing<?, ?> NOTHING = new Nothing<>();

    static <I, P> Nothing<I, P> nothing() {
        return (Nothing<I, P>) NOTHING;
    }

    final class Nothing<I, P> implements ViewAction<I, P> {
        private Nothing() {
        }
    }

    @Value
    @With
    class Open<I, P> implements ViewAction<I, P> {
        ChestView<I, P> view;
    }

    @Value
    @With
    class OpenAsync<I, P> implements ViewAction<I, P> {
        Future<ChestView<I, P>> future;
    }

    Reopen<?, ?> REOPEN = new Reopen<>();

    static <I, P> Reopen<I, P> reopen() {
        return (Reopen<I, P>) REOPEN;
    }

    final class Reopen<I, P> implements ViewAction<I, P> {
        private Reopen() {
        }
    }

    @Value
    @With
    class Update<I, P> implements ViewAction<I, P> {
        ViewContents<I, P> contents;
    }

    @Value
    @With
    class UpdateAsync<I, P> implements ViewAction<I, P> {
        Future<ViewContents<I, P>> contentsFuture;
    }
}