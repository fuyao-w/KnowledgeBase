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

