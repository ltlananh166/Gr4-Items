package service;

import exception.InvalidDiscountException;
import exception.InvalidEventException;
import exception.InvalidOrderException;
import model.FlashSaleItem;
import model.FlashSaleEvent;
import model.enums.CustTier;
import model.enums.SaleStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

public final class FlashSalePolicy {
    private FlashSalePolicy() {
    }

    public static void validateQuantity(int quantity) throws InvalidOrderException {
        if (quantity < 1 || quantity > FlashSaleConstants.MAX_UNITS_PER_CUSTOMER_PRODUCT_EVENT) {
            throw new InvalidOrderException(String.format(
                    "Each customer may purchase 1 to %d items per event.",
                    FlashSaleConstants.MAX_UNITS_PER_CUSTOMER_PRODUCT_EVENT));
        }
    }

    public static void validateEventSchedule(FlashSaleEvent event) throws InvalidEventException {
        if (event == null || event.getStartTime() == null || event.getEndTime() == null) {
            throw new InvalidEventException("Event start and end times are required.");
        }
        LocalDateTime start = event.getStartTime();
        LocalDateTime end = event.getEndTime();
        long durationMinutes = Duration.between(start, end).toMinutes();
        if (durationMinutes < FlashSaleConstants.MIN_EVENT_DURATION_MINUTES
                || durationMinutes > FlashSaleConstants.MAX_EVENT_DURATION_MINUTES) {
            throw new InvalidEventException("A Flash Sale event must last between 1 and 2 hours.");
        }
    }

    public static void requireBookable(FlashSaleEvent event, LocalDateTime now)
            throws InvalidOrderException {
        if (event == null) {
            throw new InvalidOrderException("Flash Sale event not found.");
        }
        Objects.requireNonNull(now, "now");
        SaleStatus status = event.getStatus();
        if (status == null) {
            throw new InvalidOrderException("Invalid Flash Sale event status.");
        }
        if (status == SaleStatus.LOCKED) {
            throw new InvalidOrderException(buildLockedMessage(event, now));
        }
        if (status.isEnded()) {
            throw new InvalidOrderException("The Flash Sale event has ended.");
        }
        if (status != SaleStatus.ACTIVE) {
            throw new InvalidOrderException("The Flash Sale event is not currently active.");
        }

        LocalDateTime start = event.getStartTime();
        LocalDateTime end = event.getEndTime();
        if (start == null || end == null) {
            throw new InvalidOrderException("Invalid Flash Sale event time.");
        }
        if (now.isBefore(start)) {
            throw new InvalidOrderException("The Flash Sale event has not started yet.");
        }
        if (!now.isBefore(end)) {
            throw new InvalidOrderException("The Flash Sale event has ended.");
        }
    }

    public static void validateDiscount(FlashSaleItem item) throws InvalidDiscountException {
        if (item == null || item.getOriginalPrice() == null || item.getSalePrice() == null
                || item.getOriginalPrice().signum() <= 0
                || item.getSalePrice().signum() < 0
                || item.getSalePrice().compareTo(item.getOriginalPrice()) >= 0) {
            throw new InvalidDiscountException("Invalid original or Flash Sale price.");
        }
        double discountPercent = item.getOriginalPrice()
                .subtract(item.getSalePrice())
                .multiply(java.math.BigDecimal.valueOf(100))
                .divide(item.getOriginalPrice(), 8, java.math.RoundingMode.HALF_UP)
                .doubleValue();
        if (discountPercent < FlashSaleConstants.MIN_DISCOUNT_PERCENT
                || discountPercent > FlashSaleConstants.MAX_DISCOUNT_PERCENT) {
            throw new InvalidDiscountException("Flash Sale discount must be between 30% and 70%.");
        }
    }

    public static double tierDiscountRate(CustTier tier) {
        if (tier == null) {
            return 0.0;
        }
        return switch (tier) {
            case SILVER -> FlashSaleConstants.SILVER_DISCOUNT_RATE;
            case GOLD -> FlashSaleConstants.GOLD_DISCOUNT_RATE;
            case DIAMOND -> FlashSaleConstants.DIAMOND_DISCOUNT_RATE;
            case STANDARD -> 0.0;
        };
    }

    private static String buildLockedMessage(FlashSaleEvent event, LocalDateTime now) {
        LocalDateTime unlockTime = event.getUnlockTime();
        if (unlockTime == null) {
            return "This Flash Sale event is temporarily locked.";
        }
        if (now.isBefore(unlockTime)) {
            Duration remaining = Duration.between(now, unlockTime);
            return String.format(
                    "This Flash Sale event is temporarily locked. Expected to reopen in %d minutes %d seconds (at %s).",
                    remaining.toMinutes(), remaining.minusMinutes(remaining.toMinutes()).getSeconds(),
                    unlockTime);
        }
        return "This Flash Sale event is temporarily locked.";
    }
}
