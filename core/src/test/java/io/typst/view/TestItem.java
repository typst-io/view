package io.typst.view;

import io.typst.inventory.ItemKey;
import io.typst.inventory.ItemStackOps;

/** An immutable item model that keeps core tests independent of Bukkit. */
public record TestItem(String type, int amount, int maxStackSize) {
    public static final ItemStackOps<TestItem> OPS = new ItemStackOps<>() {
        @Override
        public boolean isEmpty(TestItem item) {
            return item == null || item.amount <= 0;
        }

        @Override
        public ItemKey getKeyFrom(TestItem item) {
            return new ItemKey(item.type, "");
        }

        @Override
        public int getAmount(TestItem item) {
            return item.amount;
        }

        @Override
        public void setAmount(TestItem item, int amount) {
            throw new UnsupportedOperationException("Core logic must not mutate test items");
        }

        @Override
        public int getMaxStackSize(TestItem item) {
            return item.maxStackSize;
        }

        @Override
        public TestItem copy(TestItem item) {
            return item;
        }

        @Override
        public TestItem create(ItemKey key) {
            return new TestItem(key.getId(), 1, 64);
        }

        @Override
        public TestItem empty() {
            return null;
        }

        @Override
        public boolean isSimilar(TestItem first, TestItem second) {
            return first != null && second != null && first.type.equals(second.type);
        }
    };

    public static TestItem diamond(int amount) {
        return new TestItem("minecraft:diamond", amount, 64);
    }
}
