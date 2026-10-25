package repository;

import model.FlashItem;

/** Repository boundary validation for Flash Sale inventory invariants. */
final class FlashItemValidator {
    private FlashItemValidator() {
    }

    static void validate(FlashItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Flash Sale item must not be null");
        }
    }
}