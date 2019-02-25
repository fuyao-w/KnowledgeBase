## StampedLock

```java
public class StampedLock implements Serializable
```

采用序列锁和有序读写锁的思想。

基于功能的锁，具有三种控制读/写访问的模式。 StampedLock的状态包括版本和模式。 锁定获取方法返回一个表示和控制锁定状态访问的标记; 这些方法的“try”版本可能会返回特殊值零以表示无法获取访问权限。 锁定释放和转换方法需要标记作为参数，如果它们与锁定状态不匹配则会失败。 这三种模式是：

- **Writing**。方法writeLock（）可能会阻塞等待独占访问，返回可以在方法unlockWrite（long）中使用的戳记以释放锁定。还提供了不定时和定时版本的tryWriteLock。当锁保持在写模式时，不能获得读锁，并且所有乐观读验证都将失败。
- **Reading**。方法readLock（）可能会阻塞等待非独占访问，返回可以在方法unlockRead（long）中使用的戳记以释放锁定。还提供了不定时和定时版本的tryReadLock。
- **Optimistic Reading**。方法 tryOptimisticRead（）仅在锁定当前未处于写入模式时才返回非零戳记。方法validate（long）如果在获取给定戳记后仍未在写入模式下获取锁定，则返回true。这种模式可以被认为是读锁的极弱版本，可以随时被写锁破坏。对短的只读代码段使用乐观模式通常可以减少争用并提高吞吐量。但是，它的使用本质上是脆弱的。乐观读取部分应该只读取字段并将它们保存在局部变量中，以便以后在验证后使用。在乐观模式下读取的字段可能非常不一致，因此仅当您熟悉数据表示以检查一致性和/或重复调用方法validate（）时才会使用。例如，在首次读取对象或数组引用，然后访问其中一个字段，元素或方法时，通常需要执行此类步骤。

此类还支持有条件地提供跨三种模式的转换的方法。例如，方法tryConvertToWriteLock（long）尝试“升级”一个模式，如果（1）已经处于读取模式的写入模式（2）并且没有其他读者或（3）处于乐观模式并且锁是可用的。这些方法的形式旨在帮助减少在基于重试的设计中出现的一些代码膨胀。

StampedLocks设计用作开发线程安全组件的内部实用程序。它们的使用依赖于对它们所保护的数据，对象和方法的内部属性的了解。它们不是可重入的，因此锁定的主体不应该调用可能尝试重新获取锁的其他未知方法（尽管您可以将戳记传递给可以使用或转换它的其他方法）。读锁定模式的使用依赖于相关的代码部分是无副作用的。未经验证的乐观读取部分无法调用未知容忍潜在不一致的方法。标记使用有限表示，并且不是加密安全的（即，可以猜测有效标记）。印花值可在连续操作一年（不迟于）后再循环。未经使用或验证超过此期限而持有的印章可能无法正确验证。 StampedLocks是可序列化的，但总是反序列化为初始解锁状态，因此它们对远程锁定没有用。
​
与Semaphore一样，但与大多数Lock实现不同，StampedLocks没有所有权概念。在一个线程中获取的锁可以在另一个线程中释放或转换。

StampedLock的调度策略并不总是比reader更喜欢writer，反之亦然。所有“try”方法都是尽力而为，并不一定符合任何计划或公平政策。从任何“try”方法获取或转换锁定的零返回不会携带有关锁定状态的任何信息;后续调用可能会成功。

因为它支持跨多种锁模式的协调使用，所以此类不直接实现Lock或ReadWriteLock接口。但是，StampedLock可以在仅需要相关功能集的应用程序中查看为ReadLock（），asWriteLock（）或asReadWriteLock（）。

**Sample Usage**。 以下说明了维护简单二维点的类中的一些用法习惯用法。 示例代码说明了一些try / catch约定，即使这里没有严格要求，因为它们的主体中不会发生异常。

```java
 class Point {
   private double x, y;
   private final StampedLock sl = new StampedLock();

   // an exclusively locked method
   void move(double deltaX, double deltaY) {
     long stamp = sl.writeLock();
     try {
       x += deltaX;
       y += deltaY;
     } finally {
       sl.unlockWrite(stamp);
     }
   }

   // a read-only method
   // upgrade from optimistic read to read lock
   double distanceFromOrigin() {
     long stamp = sl.tryOptimisticRead(); 
     try {
       retryHoldingLock: for (;; stamp = sl.readLock()) {
         if (stamp == 0L)
           continue retryHoldingLock;
         // possibly racy reads
         double currentX = x;
         double currentY = y;
         if (!sl.validate(stamp))
           continue retryHoldingLock;
         return Math.hypot(currentX, currentY);
       }
     } finally {
       if (StampedLock.isReadLockStamp(stamp))
         sl.unlockRead(stamp);
     }
   }

   // upgrade from optimistic read to write lock
   void moveIfAtOrigin(double newX, double newY) {
     long stamp = sl.tryOptimisticRead();
     try {
       retryHoldingLock: for (;; stamp = sl.writeLock()) {
         if (stamp == 0L)
           continue retryHoldingLock;
         // possibly racy reads
         double currentX = x;
         double currentY = y;
         if (!sl.validate(stamp))
           continue retryHoldingLock;
         if (currentX != 0.0 || currentY != 0.0)
           break;
         stamp = sl.tryConvertToWriteLock(stamp);
         if (stamp == 0L)
           continue retryHoldingLock;
         // exclusive access
         x = newX;
         y = newY;
         return;
       }
     } finally {
       if (StampedLock.isWriteLockStamp(stamp))
         sl.unlockWrite(stamp);
     }
   }

   // Upgrade read lock to write lock
   void moveIfAtOrigin(double newX, double newY) {
     long stamp = sl.readLock();
     try {
       while (x == 0.0 && y == 0.0) {
         long ws = sl.tryConvertToWriteLock(stamp);
         if (ws != 0L) {
           stamp = ws;
           x = newX;
           y = newY;
           break;
         }
         else {
           sl.unlockRead(stamp);
           stamp = sl.writeLock();
         }
       }
     } finally {
       sl.unlock(stamp);
     }
   }
 }
```

### 分析

从概念上讲，锁的主要状态包括在写锁定时甚至是其他情况下增加的序列号。 但是，这被读取锁定时非读取的读取器计数所抵消。 验证“乐观”seqlock-reader-style标记时，将忽略读取计数。 因为我们必须为读者使用少量有限数量的位（当前为7位），所以当读取器数量超过计数字段时，将使用补充读取器溢出字。 我们通过将最大读取器计数值（RBITS）视为保护溢出更新的自旋锁来实现此目的。
等待者使用AbstractQueuedSynchronizer中使用的	改进的CLH锁，其中每个节点都标记为（field mode）作为读者或编写者。等待读者的集合在公共节点（字段`cowait`）下被分组（链接），因此就大多数CLH机制而言充当单个节点。凭借队列结构，等待节点实际上不需要携带序列号;我们知道每个都比它的前身更大。这将调度策略简化为主要是FIFO方案，其中包含Phase-Fair锁的元素。特别是，我们使用阶段公平反驳规则：如果传入的读取器在保持读取锁定但是有一个排队的写入器时到达，则此传入的读取器将排队。 （这条规则对方法acquireRead的一些复杂性负责，但没有它，锁变得非常不公平。）方法释放不会（有时不能）自己唤醒cowaiters。这是由主线程完成的，但是在方法acquireRead和acquireWrite中没有更好的事情可以帮助其他任何线程。

几乎所有这些机制都是在acquireWrite和acquireRead方法中执行的，这些代码是典型的扩展，因为操作和重试依赖于一致的本地缓存读取集。




### 字段

```java
/** 旋转控制的处理器数量 */
private static final int NCPU = Runtime.getRuntime().availableProcessors();

/** Maximum number of retries before enqueuing on acquisition; at least 1 */
private static final int SPINS = (NCPU > 1) ? 1 << 6 : 1;

/** Maximum number of tries before blocking at head on acquisition */
private static final int HEAD_SPINS = (NCPU > 1) ? 1 << 10 : 1;

/** Maximum number of retries before re-blocking */
private static final int MAX_HEAD_SPINS = (NCPU > 1) ? 1 << 16 : 1;

/** The period for yielding when waiting for overflow spinlock */
private static final int OVERFLOW_YIELD_RATE = 7; // must be power 2 - 1

/** The number of bits to use for reader count before overflowing */
private static final int LG_READERS = 7;

// Values for lock state and stamp operations
private static final long RUNIT = 1L;
private static final long WBIT  = 1L << LG_READERS; //128
private static final long RBITS = WBIT - 1L;  
private static final long RFULL = RBITS - 1L;
private static final long ABITS = RBITS | WBIT;
private static final long SBITS = ~RBITS; // note overlap with ABITS

/*
 * 通过检查可以区分3种印章模式(m = stamp & ABITS):
 * write mode: m == WBIT (128)
 * optimistic read mode: m == 0L (even when read lock is held)
 * read mode: m > 0L && m <= RFULL (标记是状态的副本，但标记中的读取保持计数除了用于确定模式
 *之外是未使用的)
 *
 * This differs slightly from the encoding of state:
 * (state & ABITS) == 0L 表示锁当前已解锁.
 * (state & ABITS) == RBITS是一个特殊的瞬态值，表示自旋锁定以操纵数据溢出。
 */

/** 锁定状态的初始值;避免故障值为零。 */
private static final long ORIGIN = WBIT << 1;

// 取消获取方法的特殊值，因此调用者可以抛出IE
private static final long INTERRUPTED = 1L;

// Values for node status; order matters
private static final int WAITING   = -1;
private static final int CANCELLED =  1;

// Modes for nodes (int not boolean to allow arithmetic)
private static final int RMODE = 0;
private static final int WMODE = 1;
```
### 内部类

```java
static final class WNode {
    volatile WNode prev;
    volatile WNode next;
    volatile WNode cowait;    // list of linked readers
    volatile Thread thread;   // non-null while possibly parked
    volatile int status;      // 0, WAITING, or CANCELLED
    final int mode;           // RMODE or WMODE
    WNode(int m, WNode p) { mode = m; prev = p; }
}
```

```java
/** Head of CLH queue */
private transient volatile WNode whead;
/** Tail (last) of CLH queue */
private transient volatile WNode wtail;

// views
transient ReadLockView readLockView;
transient WriteLockView writeLockView;
transient ReadWriteLockView readWriteLockView;

/** Lock sequence/state */
private transient volatile long state;
/** extra reader count when state read count saturated */
private transient int readerOverflow;
```

### 构造方法

```java
public StampedLock() {
    state = ORIGIN; //256
}
```

### readLock

如果 只有一个节点在等待或者没有CLH队列并且锁处于读状态则直接尝试修改state+1L。成功直接返回，失败调用acquireRead

```java
public long readLock() {
    long s, next;
    // 绕过acquireRead关于常见的无竞争案例
    return (whead == wtail //如果CLH队列头等于队列尾
            && ((s = state) & ABITS) < RFULL //读状态
            && casState(s, next = s + RUNIT)) //s+1L
        ? next//获取成功返回next
        : acquireRead(false, 0L); //获取失败
}
```

acquireRead会尝试获取锁，如果失败则会进行自旋。在自旋结束后也不能获取锁的话则会创建CLH队列，新建头结点==尾节点，并将当前线程节点设置为tail.next，然后阻塞。直到被唤醒

```java
private long acquireRead(boolean interruptible, long deadline) {
    boolean wasInterrupted = false;
    WNode node = null, p;
    for (int spins = -1;;) {
        WNode h;
        if ((h = whead) == (p = wtail)) {  //如果CLH头尾节点相同
            for (long m, s, ns;;) {
                if ((m = (s = state) & ABITS) < RFULL ? //状态处于读节点
                    casState(s, ns = s + RUNIT) : //尝试获取锁
                    (m < WBIT && (ns = tryIncReaderOverflow(s)) != 0L)) { 
                    if (wasInterrupted)
                        Thread.currentThread().interrupt();
                    return ns;
                }
                else if (m >= WBIT) { //写锁
                    if (spins > 0) {
                        --spins;
                        Thread.onSpinWait(); //自旋
                    }
                    else {
                        if (spins == 0) {
                            WNode nh = whead, np = wtail; 
                            if ((nh == h && np == p) || (h = nh) != (p = np))//CLH锁状态改变，有其他线程入队，退出自旋
                                break;
                        }
                        spins = SPINS;
                    }
                }
            }
        }
        if (p == null) { // CLH队尾为null则初始化队列
            WNode hd = new WNode(WMODE, null);//写模式
            if (WHEAD.weakCompareAndSet(this, null, hd))
                wtail = hd;
        }
        else if (node == null)
            node = new WNode(RMODE, p); //读模式，尾节点当做前驱节点
        else if (h == p || p.mode != RMODE) {//如果队列为空或者尾节点不是写模式
            if (node.prev != p)//如果node的前驱不是尾节点
                node.prev = p; //如果node不为null，将node的上一个节点设置为CLH tail
            else if (WTAIL.weakCompareAndSet(this, p, node)) { //将CLH 尾节点后移
                p.next = node; //入队成功，退出自旋
                break;
            }
        }
        else if (!WCOWAIT.compareAndSet(p, node.cowait = p.cowait, node)) //将node添加到尾节点的cowait链表
            node.cowait = null;//失败职位null
        else {
            for (;;) {
                WNode pp, c; Thread w;
                if ((h = whead) != null && (c = h.cowait) != null &&
                    WCOWAIT.compareAndSet(h, c, c.cowait) && 
                    (w = c.thread) != null) // help release
                    LockSupport.unpark(w);  //唤醒等待节点队列头，如果是读锁则释放后续cowait链表
                if (Thread.interrupted()) {
                    if (interruptible)
                        return cancelWaiter(node, p, true);
                    wasInterrupted = true;
                }
                if (h == (pp = p.prev) || h == p || pp == null) {//node前驱为head，或者node就是head或者node为空
                    long m, s, ns;
                    do {//读模式的时候再次尝试获取锁
                        if ((m = (s = state) & ABITS) < RFULL ? //如果是读模式
                            casState(s, ns = s + RUNIT) :
                            (m < WBIT &&
                             (ns = tryIncReaderOverflow(s)) != 0L)) {//溢出
                            if (wasInterrupted)
                                Thread.currentThread().interrupt();
                            return ns;
                        }
                    } while (m < WBIT);
                }
                if (whead == h && p.prev == pp) {
                    long time;
                    if (pp == null || h == p || p.status > 0) {
                        node = null; // throw away  锁已经被其他线程获取，或者尾节点被取消	
                        break;
                    }
                    if (deadline == 0L)
                        time = 0L;
                    else if ((time = deadline - System.nanoTime()) <= 0L) {
                        if (wasInterrupted)
                            Thread.currentThread().interrupt();
                        return cancelWaiter(node, p, false);
                    }
                    Thread wt = Thread.currentThread();
                    node.thread = wt;
                    if ((h != pp || (state & ABITS) == WBIT) && //其他线程获取了写锁
                        whead == h && p.prev == pp) {
                        if (time == 0L)
                            LockSupport.park(this); 
                        else
                            LockSupport.parkNanos(this, time);
                    }
                    node.thread = null;
                }
            }
        }
    }

    for (int spins = -1;;) {
        WNode h, np, pp; int ps;
        if ((h = whead) == p) {//只有一个节点
            if (spins < 0)
                spins = HEAD_SPINS;
            else if (spins < MAX_HEAD_SPINS)
                spins <<= 1;
            for (int k = spins;;) { // spin at head
                long m, s, ns;
                if ((m = (s = state) & ABITS) < RFULL ? //尝试获取锁
                    casState(s, ns = s + RUNIT) :
                    (m < WBIT && (ns = tryIncReaderOverflow(s)) != 0L)) {
                    WNode c; Thread w;
                    whead = node;
                    node.prev = null;
                    while ((c = node.cowait) != null) {
                        if (WCOWAIT.compareAndSet(node, c, c.cowait) &&
                            (w = c.thread) != null)
                            LockSupport.unpark(w);//唤醒cowait链表中的节点
                    }
                    if (wasInterrupted)
                        Thread.currentThread().interrupt();
                    return ns;
                }
                else if (m >= WBIT && --k <= 0)
                    break;
                else
                    Thread.onSpinWait();
            }
        }
        else if (h != null) {//如果不止有一个节点
            WNode c; Thread w;
            while ((c = h.cowait) != null) {
                if (WCOWAIT.compareAndSet(h, c, c.cowait) &&
                    (w = c.thread) != null)
                    LockSupport.unpark(w);
            }
        }
        if (whead == h) {
            if ((np = node.prev) != p) {
                if (np != null)
                    (p = np).next = node;   // stale
            }
            else if ((ps = p.status) == 0) 
                WSTATUS.compareAndSet(p, 0, WAITING);
            else if (ps == CANCELLED) { //取消
                if ((pp = p.prev) != null) {
                    node.prev = pp;
                    pp.next = node;
                }
            }
            else {
                long time;
                if (deadline == 0L)
                    time = 0L;
                else if ((time = deadline - System.nanoTime()) <= 0L)
                    return cancelWaiter(node, node, false);
                Thread wt = Thread.currentThread();
                node.thread = wt;
                if (p.status < 0 &&
                    (p != h || (state & ABITS) == WBIT) &&//写模式
                    whead == h && node.prev == p) {
                        if (time == 0L)
                            LockSupport.park(this);
                        else
                            LockSupport.parkNanos(this, time);
                }
                node.thread = null;
                if (Thread.interrupted()) {
                    if (interruptible)
                        return cancelWaiter(node, node, true);
                    wasInterrupted = true;
                }
            }
        }
    }
}
```



### unlockRead

```java
public void unlockRead(long stamp) {
    long s, m; WNode h;
    while (((s = state) & SBITS) == (stamp & SBITS) //没有其他线程修改锁的状态
           && (stamp & RBITS) > 0L  //印章处于锁定状态
           && ((m = s & RBITS) > 0L)) { //state处于锁定状态
        if (m < RFULL) {//读模式
            if (casState(s, s - RUNIT)) { //解锁成功后如果 锁状态等于1L，并且CLH链表不为null，则释放链表中的节点
                if (m == RUNIT && (h = whead) != null && h.status != 0)
                    release(h);
                return;
            }
        }
        else if (tryDecReaderOverflow(s) != 0L)//读锁数量溢出处理
            return;
    }
    throw new IllegalMonitorStateException();
}
```



### tryOptimisticRead

```java
public long tryOptimisticRead() {
    long s;
    return (((s = state) & WBIT) == 0L) ? (s & SBITS) : 0L;
}
```

如果没有写锁则返回非0，再通过`validate` 判断这期间是否有其他线程持有了写锁。而且不用释放锁

### tryConvertToWriteLock

```java
public long tryConvertToWriteLock(long stamp) {
    long a = stamp & ABITS, m, s, next;
    while (((s = state) & SBITS) == (stamp & SBITS)) {
        if ((m = s & ABITS) == 0L) {//没有被任何锁锁定
            if (a != 0L)
                break;
            if ((next = tryWriteLock(s)) != 0L)
                return next;
        }
        else if (m == WBIT) {//已经持有写锁
            if (a != m)
                break;
            return stamp;
        }
        else if (m == RUNIT && a != 0L) {//有一个读锁
            if (casState(s, next = s - RUNIT + WBIT)) {
                VarHandle.storeStoreFence();
                return next;
            }
        }
        else
            break;
    }
    return 0L;
}
```

如果锁定状态与给定标记匹配，则原子地执行以下操作之一。 如果标记表示持有写锁定，则返回它。 或者，如果读锁定，如果写锁定可用，则释放读锁定并返回写入标记。 或者，如果乐观读取，则仅在立即可用时才返回写入戳记。 在所有其他情况下，此方法返回零。