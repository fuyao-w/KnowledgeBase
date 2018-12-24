## LinkedBlockingQueue

```java
public class LinkedBlockingQueue<E> extends AbstractQueue<E>
        implements BlockingQueue<E>, java.io.Serializable 
```

### java doc

基于链表的可选绑定阻塞队列。 此队列命令元素FIFO（先进先出）。 队列的头部是队列中最长时间的元素。 队列的尾部是队列中最短时间的元素。 在队列的尾部插入新元素，队列检索操作获取队列头部的元素。 链接队列通常具有比基于阵列的队列更高的吞吐量，但在大多数并发应用程序中具有较低的可预测性能。
可选的容量绑定构造函数参数用作防止过多队列扩展的方法。 如果未指定，则容量等于Integer.MAX_VALUE。 每次插入时都会动态创建链接节点，除非这会使队列超出容量。

### 分析

“two lock queue”算法的变体。 putLock门 输入（和提供），并具有相关的条件等待放。同样适用于takeLock。 “count”字段他们都依赖的是作为一个原子来维持以避免在大多数情况下需要获得两个锁。另外，尽量减少需求对于put来获取takeLock，反之亦然，级联通知是用过的。当一个put通知它至少启用了一次， 它标志着接受者。那个接受者反过来向其他人发出信号自信号以来已输入项目。和对称的采取信号放置。删除（对象）和迭代器获得两个锁。


writers和readers之间的可见性如下：
每当一个元素入队时，就会获得putLock计数更新。随后的reader保证了可见性通过获取putLock（通过fullyLock）排队节点或者通过获取takeLock，然后读取n = count.get（）;
这样可以看到前n个项目。

要实现弱一致的迭代器，我们需要这样做保持所有节点GC都可以从前任出列的节点中到达。
这会导致两个问题：

   - 允许恶意迭代器导致无限制的内存保留
   - 如果，将旧节点跨代链接到新节点

一个节点在现场投入使用，这个世代的GCs有一个处理困难，导致重复的主要收集。 但是，只需要可以访问未删除的节点 出列节点，并且可达性不一定非必要是GC所理解的那种。我们使用的技巧将刚刚出列的节点链接到自身。这样的隐式的自我链接意味着前进到head.next。

### 链表节点

```java
static class Node<E> {
    E item;

    /**
     * One of:
     * - 真正的后继节点
     * -这个节点，意思是后继者是head.next
     * - null，表示没有后继者（这是最后一个节点）
     */
    Node<E> next;

    Node(E x) { item = x; }
}
```

```java
/** The capacity bound, or Integer.MAX_VALUE if none */
private final int capacity;

/** Current number of elements */
private final AtomicInteger count = new AtomicInteger();

/**
 * Head of linked list.
 * Invariant: head.item == null
 */
transient Node<E> head;

/**
 * Tail of linked list.
 * Invariant: last.next == null
 */
private transient Node<E> last;

/** Lock held by take, poll, etc */
private final ReentrantLock takeLock = new ReentrantLock();

/** Wait queue for waiting takes */
private final Condition notEmpty = takeLock.newCondition();

/** Lock held by put, offer, etc */
private final ReentrantLock putLock = new ReentrantLock();

/** Wait queue for waiting puts */
private final Condition notFull = putLock.newCondition();
```

### 构造方法

```java
public LinkedBlockingQueue(int capacity) {
    if (capacity <= 0) throw new IllegalArgumentException();
    this.capacity = capacity;
    last = head = new Node<E>(null);
}
```

容量默认是2147483647，将链表的head、last初始化。

### put

```java
public void put(E e) throws InterruptedException {
    if (e == null) throw new NullPointerException();
    final int c;
    final Node<E> node = new Node<E>(e);
    final ReentrantLock putLock = this.putLock;
    final AtomicInteger count = this.count;
    putLock.lockInterruptibly();
    try {
        /*
         *请注意，即使没有锁定保护，count也会用于等待保护。 
         * 这是有效的，因为count只能在这一点上减少（所有其他put被锁定关闭），
         * 并且如果它从容量发生变化，我们（或其他一些等待put）会发出信号。 
         * 同样，对于其他等待守卫的计数的所有其他用途。
         */
        while (count.get() == capacity) {
            notFull.await();
        }
        enqueue(node);
        c = count.getAndIncrement();
        if (c + 1 < capacity)
            notFull.signal();
    } finally {
        putLock.unlock();
    }
    if (c == 0)
        signalNotEmpty();
}
```

读和写操作使用了两个不同的重入锁，当容量已满的时候会将当前线程阻塞，当入队成功后如果当前容量还没有满则会唤醒其他因为容量满而阻塞的线程。如果之前队列容量为0，则唤醒因为容量为0而阻塞的线程。

```java
private void enqueue(Node<E> node) {
    // assert putLock.isHeldByCurrentThread();
    // assert last.next == null;
    last = last.next = node;
}
```

入队操作将新节点加入链表末尾。

### take

```java
public E take() throws InterruptedException {
    final E x;
    final int c;
    final AtomicInteger count = this.count;
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lockInterruptibly();
    try {
        while (count.get() == 0) {
            notEmpty.await();
        }
        x = dequeue();
        c = count.getAndDecrement();
        if (c > 1)
            notEmpty.signal();
    } finally {
        takeLock.unlock();
    }
    if (c == capacity)
        signalNotFull();
    return x;
}
```

take操作使用takeLock,如果队列长度为0，则将当前线程阻塞，出队后如果容量还是大于0，则会唤醒因为容量为0,而阻塞的对列。如果出队后容量小于最大容量，则唤醒因为容量满而阻塞的线程。

```java
private E dequeue() {
    // assert takeLock.isHeldByCurrentThread();
    // assert head.item == null;
    Node<E> h = head;
    Node<E> first = h.next;
    h.next = h; // help GC
    head = first;
    E x = first.item;
    first.item = null;
    return x;
}
```

出队操作将head清除