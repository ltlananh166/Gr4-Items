package service;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import model.enums.CustTier;

/**
 * Điều phối các yêu cầu đặt hàng đồng bộ: yêu cầu VIP/PREMIUM được xử lý trước,
 * còn các yêu cầu trong cùng nhóm ưu tiên được xử lý theo thứ tự đến.
 */
public final class OrderRequestQueue {
    private static final int STANDARD_PRIORITY = 0;
    private static final int VIP_PREMIUM_PRIORITY = 1;

    private static final Comparator<Entry> ORDERING =
            Comparator.comparingInt(Entry::priority).reversed()
                    .thenComparingLong(Entry::sequence);

    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition stateChanged = lock.newCondition();
    private final PriorityQueue<Entry> waiting = new PriorityQueue<>(ORDERING);

    private long nextSequence;
    private Entry active;

    public Permit acquire(CustTier tier) throws InterruptedException {
        lock.lockInterruptibly();
        Entry entry = null;
        try {
            entry = new Entry(nextSequence++, priorityOf(tier));
            waiting.add(entry);
            stateChanged.signalAll();

            while (active != null || waiting.peek() != entry) {
                stateChanged.await();
            }
            waiting.remove();
            active = entry;
            return new Permit(this, entry);
        } catch (InterruptedException exception) {
            if (entry != null) {
                waiting.remove(entry);
                stateChanged.signalAll();
            }
            throw exception;
        } finally {
            lock.unlock();
        }
    }

    public int pendingCount() {
        lock.lock();
        try {
            return waiting.size();
        } finally {
            lock.unlock();
        }
    }

    private void release(Entry entry) {
        lock.lock();
        try {
            if (active == entry) {
                active = null;
                stateChanged.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    private static int priorityOf(CustTier tier) {
        // Trong mô hình miền hiện tại, VIP/PREMIUM là mọi hạng thành viên cao hơn STANDARD.
        return tier == null || tier == CustTier.STANDARD
                ? STANDARD_PRIORITY : VIP_PREMIUM_PRIORITY;
    }

    private record Entry(long sequence, int priority) {
    }

    public static final class Permit implements AutoCloseable {
        private final OrderRequestQueue owner;
        private final Entry entry;
        private final AtomicBoolean closed = new AtomicBoolean();

        private Permit(OrderRequestQueue owner, Entry entry) {
            this.owner = owner;
            this.entry = entry;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                owner.release(entry);
            }
        }
    }
}
