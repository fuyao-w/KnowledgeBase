## PriorityBlockingQueue

```java
public class PriorityBlockingQueue<E> extends AbstractQueue<E>
    implements BlockingQueue<E>, java.io.Serializable
```

### java doc

一个无限制的阻塞队列，它使用与类PriorityQueue相同的排序规则，并提供阻塞检索操作。虽然此队列在逻辑上是无限制的，但由于资源耗尽（导致OutOfMemoryError），尝试添加可能会失败。此类不允许null元素。依赖于自然排序的优先级队列也不允许插入不可比较的对象（这样做会导致ClassCastException）。
该类及其迭代器实现了Collection和Iterator接口的所有可选方法。方法iterator（）中提供的迭代器和方法spliterator（）中提供的Spliterator不保证以任何特定顺序遍历PriorityBlockingQueue的元素。如果需要有序遍历，请考虑使用Arrays.sort（pq.toArray（））。此外，方法drainTo可用于按优先级顺序删除部分或全部元素，并将它们放在另一个集合中。

此类的操作不保证具有相同优先级的元素的排序。如果需要强制执行排序，则可以定义使用辅助键来断开主要优先级值中的关系的自定义类或比较器。例如，这是一个将先进先出的打破平局应用于可比元素的类。要使用它，您将插入一个新的FIFOEntry（anEntry）而不是普通的条目对象。

### 分析

PriorityQueue，是一个容量无限的队列，是会在取元素时候阻塞。

实现使用基于数组的二进制堆，公共操作受单个锁保护。 但是，调整大小期间的分配使用简单的自旋锁（仅在不保持主锁的情况下使用），以便允许在分配时同时操作。 这避免了等待消费者的反复推迟以及随后的元素积累。 在分配期间退出锁定的需要使得无法简单地将委托的java.util.PriorityQueue操作包装在锁中，就像在此类的先前版本中所做的那样。 为了保持互操作性，在序列化期间仍然使用普通的PriorityQueue，它以牺牲瞬时加倍开销为代价来维护兼容性。

```java
/**
 * 默认数组容量。
 */
private static final int DEFAULT_INITIAL_CAPACITY = 11;

/**
 * 要分配的最大数组大小。
 * Some VMs reserve some header words in an array.
 * Attempts to allocate larger arrays may result in
 * OutOfMemoryError: Requested array size exceeds VM limit
 */
private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

/**
 * 优先级队列表示为平衡二进制堆:
  * queue [n]的两个子节点是队列[2 * n + 1]和队列[2 *（n + 1）]。 
  * 优先级队列由比较器或元素的自然顺序排序，
  * 如果比较器为空:对于堆中的每个节点n和n的每个后代d, n <= d.
  * 假设队列是非空的，具有最低值的元素在队列[0]中。
 */
private transient Object[] queue;

/**
 * 优先级队列中的元素数。
 */
private transient int size;

/**
 * The comparator, or null if priority queue uses elements'
 * natural ordering.
 */
private transient Comparator<? super E> comparator;

/**
 * Lock used for all public operations.
 */
private final ReentrantLock lock = new ReentrantLock();

/**
 * Condition for blocking when empty.
 */
private final Condition notEmpty = lock.newCondition();

/**
 * Spinlock for allocation, acquired via CAS.
 */
private transient volatile int allocationSpinLock;
```

### 构造方法

```java
public PriorityBlockingQueue(int initialCapacity,
                             Comparator<? super E> comparator) {
    if (initialCapacity < 1)
        throw new IllegalArgumentException();
    this.comparator = comparator;
    this.queue = new Object[Math.max(1, initialCapacity)];
}
```

PriorityBlockingQueue默认的容量是11，最小为1，最大2147483647。

```java
public boolean offer(E e) {
    if (e == null)
        throw new NullPointerException();
    final ReentrantLock lock = this.lock;
    lock.lock();
    int n, cap;
    Object[] es;
    while ((n = size) >= (cap = (es = queue).length))
        tryGrow(es, cap);
    try {
        final Comparator<? super E> cmp;
        if ((cmp = comparator) == null)
            siftUpComparable(n, e, es);
        else
            siftUpUsingComparator(n, e, es, cmp);
        size = n + 1;
        notEmpty.signal();
    } finally {
        lock.unlock();
    }
    return true;
}
```

 offer先判断是否应该扩容，如果size大于等于数组的容量，则调用`tryGrow`进行扩容，

```java
/**
 * 尝试增加数组以容纳至少一个元素（但通常扩展约50％），放弃（允许重试）争用（我们期望很少）。 
 * 只有在握住锁定时才调用
 **/
private void tryGrow(Object[] array, int oldCap) {
    lock.unlock(); // must release and then re-acquire main lock
    Object[] newArray = null;
    if (allocationSpinLock == 0 &&
        ALLOCATIONSPINLOCK.compareAndSet(this, 0, 1)) {
        try {
            int newCap = oldCap + ((oldCap < 64) ?
                                   (oldCap + 2) : // grow faster if small
                                   (oldCap >> 1));
            if (newCap - MAX_ARRAY_SIZE > 0) {    // possible overflow
                int minCap = oldCap + 1;
                if (minCap < 0 || minCap > MAX_ARRAY_SIZE)
                    throw new OutOfMemoryError();
                newCap = MAX_ARRAY_SIZE;
            }
            if (newCap > oldCap && queue == array)
                newArray = new Object[newCap];
        } finally {
            allocationSpinLock = 0;
        }
    }
    if (newArray == null) // back off if another thread is allocating
        Thread.yield();
    lock.lock();
    if (newArray != null && queue == array) {
        queue = newArray;
        System.arraycopy(array, 0, newArray, 0, oldCap);
    }
}
```

在进行扩容操作时，要释放锁允许其他线程进行扩容操作，与当前线程竞争。首先判断allocationSpinLock字段是否为0，是的话修改为1。只有成功修改的线程才能进入扩容逻辑中。扩容逻辑是容量小于64加2，大于64扩大1.5倍。如果容量溢出整形最大值则抛出是内存溢出异常。

扩容结束后刚才进入扩容逻辑失败的线程则会让出CPU，让扩容成功的线程重新获取锁，将旧数组转移到新数组上。扩容成功后将新元素插入数组中，如果没有传入comparator，则调用siftUpComparable使用实现Comparable接口元素的compareTo方法进行比较。否则调用siftUpUsingComparator方法插入。

```java
private static <T> void siftUpComparable(int k, T x, Object[] es) {
    Comparable<? super T> key = (Comparable<? super T>) x;
    while (k > 0) {
        int parent = (k - 1) >>> 1;
        Object e = es[parent];
        if (key.compareTo((T) e) >= 0)
            break;
        es[k] = e;
        k = parent;
    }
    es[k] = key;
}
```

如果新插入元素大于它此时的父节点，则满足小顶堆，将新元素放入k位置。否则将它与父节点交换，知道满足小顶堆。

最后offer唤醒因为元素数量为0而阻塞的元素。

### take

```java
public E take() throws InterruptedException {
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    E result;
    try {
        while ( (result = dequeue()) == null)
            notEmpty.await();
    } finally {
        lock.unlock();
    }
    return result;
}
```

take的出队列操作调用dequeue，如果队列元素为0，则阻塞。

```java
private E dequeue() {
    // assert lock.isHeldByCurrentThread();
    final Object[] es;
    final E result;

    if ((result = (E) ((es = queue)[0])) != null) {
        final int n;
        final E x = (E) es[(n = --size)];
        es[n] = null;
        if (n > 0) {
            final Comparator<? super E> cmp;
            if ((cmp = comparator) == null)
                siftDownComparable(0, x, es, n);
            else
                siftDownUsingComparator(0, x, es, n, cmp);
        }
    }
    return result;
}
```

从堆顶取元素，取出元素后调整堆。

```java
private static <T> void siftDownComparable(int k, T x, Object[] es, int n) {
    // assert n > 0;
    Comparable<? super T> key = (Comparable<? super T>)x;
    int half = n >>> 1;           // loop while a non-leaf
    while (k < half) {
        int child = (k << 1) + 1; // 假设左孩子是最少的
        Object c = es[child];
        int right = child + 1; //右孩子
        if (right < n &&    
            ((Comparable<? super T>) c).compareTo((T) es[right]) > 0)
            c = es[child = right];
        if (key.compareTo((T) c) <= 0) //如果 key小于左右孩子中最小的，则满足小顶堆
            break;
        es[k] = c;  //不满足则与堆顶交换
        k = child;  //重新调整孩子节点
    }
    es[k] = key;//将key放到合适的位置。
}
```

将最后一个节点，重新插入堆中保持小顶堆。