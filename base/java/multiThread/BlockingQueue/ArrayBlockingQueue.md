## ArrayBlockingQueue

```java
public class ArrayBlockingQueue<E> extends AbstractQueue<E>
                            implements BlockingQueue<E>, Serializable
```

### java doc

由数组支持的有界阻塞队列。此队列命令元素FIFO（先进先出）。队列的头部是队列中最长时间的元素。队列的尾部是队列中最短时间的元素。在队列的尾部插入新元素，队列检索操作获取队列头部的元素。
这是一个经典的“有界缓冲区”，其中固定大小的数组包含由生产者插入并由消费者提取的元素。创建后，无法更改容量。尝试将元素放入完整队列将导致操作阻塞;尝试从空队列中获取元素同样会阻塞。

此类支持用于排序等待生产者和消费者线程的可选公平策略。默认情况下，不保证此顺序。但是，将fairness设置为true构造的队列以FIFO顺序授予线程访问权限。公平性通常会降低吞吐量，但会降低可变性并避免饥饿。

```java
/** The queued items */
final Object[] items;

/** items index for next take, poll, peek or remove */
int takeIndex;

/** items index for next put, offer, or add */
int putIndex;

/** Number of elements in the queue */
int count;

/*
 * Concurrency control uses the classic two-condition algorithm
 * found in any textbook.
 */

/** Main lock guarding all access */
final ReentrantLock lock;

/** Condition for waiting takes */
private final Condition notEmpty;

/** Condition for waiting puts */
private final Condition notFull;
```

### 构造方法

```java
public ArrayBlockingQueue(int capacity, boolean fair) {
    if (capacity <= 0)
        throw new IllegalArgumentException();
    this.items = new Object[capacity];
    lock = new ReentrantLock(fair);
    notEmpty = lock.newCondition();
    notFull =  lock.newCondition();
}
```

ArrayBlockingQueue是有界的阻塞队列，所以要由使用者确定初始值。并且也有公平和非公平两个版本。 通过重入锁和Condition来控制队列阻塞。

### offer

```java
public boolean offer(E e) {
    Objects.requireNonNull(e);
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        if (count == items.length)
            return false;
        else {
            enqueue(e);
            return true;
        }
    } finally {
        lock.unlock();
    }
}
```

通过重入锁实现线程安全，当队列容量满了的时候，直接返回false,否则调用enqueue进行入队操作。并不会将线程阻塞。

### put

```java
public void put(E e) throws InterruptedException {
    Objects.requireNonNull(e);
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    try {
        while (count == items.length)
            notFull.await();
        enqueue(e);
    } finally {
        lock.unlock();
    }
}
```

put操作当线程满了的时候通过notFull将线程阻塞，知道其他线程才从队列中取出元素然后将线程唤醒。

```java
private void enqueue(E e) {
    // assert lock.isHeldByCurrentThread();
    // assert lock.getHoldCount() == 1;
    // assert items[putIndex] == null;
    final Object[] items = this.items;
    items[putIndex] = e;
    if (++putIndex == items.length) putIndex = 0;
    count++;
    notEmpty.signal();
}
```

入队操作最后会将因为取空队列中的元素而被阻塞的线程唤醒。

### poll

```java
public E poll() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        return (count == 0) ? null : dequeue();
    } finally {
        lock.unlock();
    }
}
```

poll不会阻塞线程。

### take

```java
public E take() throws InterruptedException {
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    try {
        while (count == 0)
            notEmpty.await();
        return dequeue();
    } finally {
        lock.unlock();
    }
}
```

take在队列为空的时候调用notEmpty将当前线程阻塞，知道其他线程将元素入队并且唤醒当前线程。

```java
private E dequeue() {
    // assert lock.isHeldByCurrentThread();
    // assert lock.getHoldCount() == 1;
    // assert items[takeIndex] != null;
    final Object[] items = this.items;
    @SuppressWarnings("unchecked")
    E e = (E) items[takeIndex];
    items[takeIndex] = null;
    if (++takeIndex == items.length) takeIndex = 0;
    count--;
    if (itrs != null)
        itrs.elementDequeued();
    notFull.signal();
    return e;
}
```

出队操作在出队后将因为队列满而阻塞的线程唤醒。

### clear

```java
public void clear() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        int k;
        if ((k = count) > 0) {
            circularClear(items, takeIndex, putIndex);
            takeIndex = putIndex;
            count = 0;
            if (itrs != null)
                itrs.queueIsEmpty();
            for (; k > 0 && lock.hasWaiters(notFull); k--)
                notFull.signal();
        }
    } finally {
        lock.unlock();
    }
}
```

clear将队列元素清除后唤醒所有因为队列满而阻塞的线程。

