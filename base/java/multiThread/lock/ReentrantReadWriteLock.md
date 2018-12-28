## ReadWriteLock

```java
public interface ReadWriteLock
```

### java doc

ReadWriteLock维护一对关联的锁，一个用于只读操作，另一个用于写入。 只要没有写入器，读锁定可以由多个读取器线程同时保持。 写锁是独占的。
所有ReadWriteLock实现都必须保证writeLock操作的内存同步效果（如Lock接口中所指定）对相关的readLock也有效。 也就是说，成功获取读锁定的线程将看到在先前释放写锁定时所做的所有更新。

读写锁允许访问共享数据的并发性高于互斥锁允许的并发性。它利用了这样一个事实：虽然一次只有一个线程（一个编写器线程）可以修改共享数据，但在许多情况下，任何数量的线程都可以同时读取数据（因此读取器线程）。理论上，使用读写锁所允许的并发性的增加将导致相互使用互斥锁的性能提高。实际上，这种并发性的增加只能在多处理器上完全实现，并且只有在共享数据的访问模式合适时才能实现。

读写锁是否会提高使用互斥锁的性能取决于与被修改相比读取数据的频率，读写操作的持续时间以及数据的争用 - 是，将尝试同时读取或写入数据的线程数。例如，最初填充数据并且之后不经常修改但经常搜索的集合（例如某种目录）是使用读写锁的理想候选者。但是，如果更新变得频繁，那么数据的大部分时间都会被完全锁定，并且并发性几乎没有增加。此外，如果读取操作太短，则读写锁定实现的开销（其本质上比互斥锁定更复杂）可以支配执行成本，特别是因为许多读写锁定实现仍然通过序列化所有线程。小部分代码。最终，只有分析和测量才能确定使用读写锁是否适合您的应用。

尽管读写锁的基本操作是直截了当的，但实现必须做出许多策略决策，这可能会影响给定应用程序中读写锁的有效性。 这些政策的例子包括：

- 在写入器释放写锁定时，当读取器和写入器都在等待时，确定是否授予读锁定或写锁定。写偏好很常见，因为写作预计很短且不常见。读者偏好不太常见，因为如果读者频繁且长期按预期，它可能导致写入的长时间延迟。公平或“有序”实施也是可能的。
- 确定在读取器处于活动状态且写入器正在等待时请求读锁定的读取器是否被授予读锁定。读者的偏好可以无限期地延迟写入者，而对作者的偏好可以减少并发的可能性。
- 确定锁是否可重入：具有写锁的线程是否可以重新获取它？它可以在保持写锁定的同时获得读锁定吗？读锁本身是否可重入？
- 写锁是否可以降级为读锁而不允许介入的写入器？读锁可以升级到写锁，优先于其他等待的读者或编写者吗？

在评估给定实现对应用程序的适用性时，您应该考虑所有这些事情。

## ReentrantReadWriteLock

```java
public class ReentrantReadWriteLock
        implements ReadWriteLock, java.io.Serializable 
```

### java doc

ReadWriteLock的实现，支持与ReentrantLock类似的语义。
该类具有以下属性：

- **Acquisition order**


此类不会强制执行锁定访问的reader 或writer 首选项顺序。但是，它确实支持可选的公平政策。

***Non-fair mode (default)***
当构造为非公平（默认）时，读取和写入锁定的输入顺序是未指定的，受重入约束的限制。持续争用的非公平锁定可能无限期地推迟一个或多个读取器或写入器线程，但通常具有比公平锁定更高的吞吐量。

***Fair mode***
当构造为公平时，线程使用近似到达顺序策略争用进入。当释放当前保持的锁定时，将为最长等待的单个写入器线程分配写入锁定，或者如果有一组读取器线程等待的时间长于所有等待的写入器线程，则将为该组分配读取锁定。
尝试获取公平读锁定（非重复）的线程将阻止是否保持写锁定，或者存在等待写入器线程。在最旧的当前等待的写入器线程获取并释放写锁定之前，线程将不会获取读锁定。当然，如果等待的写入者放弃其等待，将一个或多个读取器线程作为队列中最长的服务器并且写锁定空闲，那么将为这些读取器分配读锁定。

尝试获取公平写锁定（非重复）的线程将阻塞，除非读取锁定和写入锁定都是空闲的（这意味着没有等待的线程）。 （请注意，非阻塞的ReentrantReadWriteLock.ReadLock.tryLock（）和ReentrantReadWriteLock.WriteLock.tryLock（）方法不遵循此公平设置，并且如果可能，将立即获取锁定，无论等待线程如何。）

- **Reentrancy**

当构造为公平时，线程使用近似到达顺序策略争用进入。当释放当前保持的锁定时，将为最长等待的单个写入器线程分配写入锁定，或者如果有一组读取器线程等待的时间长于所有等待的写入器线程，则将为该组分配读取锁定。
尝试获取公平读锁定（非重复）的线程将阻止是否保持写锁定，或者存在等待写入器线程。在最旧的当前等待的写入器线程获取并释放写锁定之前，线程将不会获取读锁定。当然，如果等待的写入者放弃其等待，将一个或多个读取器线程作为队列中最长的服务器并且写锁定空闲，那么将为这些读取器分配读锁定。

尝试获取公平写锁定（非重复）的线程将阻塞，除非读取锁定和写入锁定都是空闲的（这意味着没有等待的线程）。 （请注意，非阻塞的ReentrantReadWriteLock.ReadLock.tryLock（）和ReentrantReadWriteLock.WriteLock.tryLock（）方法不遵循此公平设置，并且如果可能，将立即获取锁定，无论等待线程如何。）

- **Lock downgrading**

Reentrancy还允许通过获取写锁定，然后读取锁定然后释放写入锁定，从写入锁定降级到读取锁定。 但是，无法从读锁定升级到写锁定。

- **Interruption of lock acquisition**

读取锁定和写入锁定都支持锁定获取期间的中断。

- **Condition support**

写入锁提供了一个Condition实现，其行为方式与写入锁相同，因为ReentrantLock.newCondition（）为ReentrantLock提供了Condition实现。 当然，此Condition只能与写锁一起使用。

读锁不支持Condition和readLock（）。newCondition（）抛出UnsupportedOperationException。

- **Instrumentation**

此类支持确定锁是保持还是争用的方法。 这些方法用于监视系统状态，而不是用于同步控制。

此类的序列化与内置锁的行为方式相同：反序列化锁处于解锁状态，无论序列化时的状态如何。

- **Sample usages**. 下面是一个代码草图，展示了如何在更新缓存后执行锁定降级（在以非嵌套方式处理多个锁时，异常处理尤其棘手）：

```java
 class CachedData {
   Object data;
   boolean cacheValid;
   final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

   void processCachedData() {
     rwl.readLock().lock();
     if (!cacheValid) {
       // 在获取写锁定之前必须释放读锁定
       rwl.readLock().unlock();
       rwl.writeLock().lock();
       try {
         // 重新检查状态，因为在我们执行之前，另一个线程可能已获取写锁和更改状态。
         if (!cacheValid) {
           data = ...
           cacheValid = true;
         }
         //在释放写锁定之前通过获取读锁来降级
         rwl.readLock().lock();
       } finally {
         rwl.writeLock().unlock(); // 解锁写入，仍然保持读取
       }
     }

     try {
       use(data);
     } finally {
       rwl.readLock().unlock();
     }
   }
 }
```

ReentrantReadWriteLocks可用于在某些类型的集合的某些用途中提高并发性。 这通常是值得的，只有当预期集合很大时，由读取器线程比读写器线程访问更多，并且需要具有超过同步开销的开销的操作。 例如，这是一个使用TreeMap的类，该类预计很大并且可以同时访问。

```java
 class RWDictionary {
   private final Map<String, Data> m = new TreeMap<>();
   private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
   private final Lock r = rwl.readLock();
   private final Lock w = rwl.writeLock();

   public Data get(String key) {
     r.lock();
     try { return m.get(key); }
     finally { r.unlock(); }
   }
   public List<String> allKeys() {
     r.lock();
     try { return new ArrayList<>(m.keySet()); }
     finally { r.unlock(); }
   }
   public Data put(String key, Data value) {
     w.lock();
     try { return m.put(key, value); }
     finally { w.unlock(); }
   }
   public void clear() {
     w.lock();
     try { m.clear(); }
     finally { w.unlock(); }
   }
 }
```

实施说明
此锁最多支持65535个递归写锁和65535个读锁。 尝试超过这些限制会导致锁定方法的错误抛出。



### 分析

```java
abstract static class Sync extends AbstractQueuedSynchronizer {
       

        /*
         * 读取与写入次数提取常量和函数。 锁定状态在逻辑上划分为两个*unsigned shorts：
         * 较低的一个表示独占（写入）锁定保持计数，而较*高的一个表示共享（读取器）保持计数。
         */

        static final int SHARED_SHIFT   = 16;
        static final int SHARED_UNIT    = (1 << SHARED_SHIFT);
        static final int MAX_COUNT      = (1 << SHARED_SHIFT) - 1;
        static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;
```

```java
protected final boolean tryAcquire(int acquires) {
    /*
     * Walkthrough:
     * 1. 如果读取计数非零或写入计数非零且所有者是不同的线程，则失败。
     * 2. 如果计数会饱和，那就失败了。 （只有在计数已经非零时才会发生这种情况。）
     * 3. 否则，如果此线程是可重入获取或队列策略允许，则该线程有资格获得锁定。 
     * 如果是，请更新状态并设置所有者。
     */
    Thread current = Thread.currentThread();
    int c = getState();
    int w = exclusiveCount(c);
    if (c != 0) {
        // (Note: if c != 0 and w == 0 then shared count != 0)
        if (w == 0 || current != getExclusiveOwnerThread())
            return false;
        if (w + exclusiveCount(acquires) > MAX_COUNT)
            throw new Error("Maximum lock count exceeded");
        // Reentrant acquire
        setState(c + acquires);
        return true;
    }
    if (writerShouldBlock() || //非公平锁返回false,公平锁返回hasQueuedPredecessors方法
        !compareAndSetState(c, c + acquires))
        return false;
    setExclusiveOwnerThread(current);
    return true;
}
```



```java
/**
*请注意，条件可以调用tryRelease和tryAcquire。 因此，它们的参数可能包含在条件等待期间释放的
* 读取和写入保持，并在tryAcquire中重新建立。 
*/

 **/
protected final boolean tryRelease(int releases) {
    if (!isHeldExclusively())
        throw new IllegalMonitorStateException();
    int nextc = getState() - releases;
    boolean free = exclusiveCount(nextc) == 0;
    if (free)
        setExclusiveOwnerThread(null);
    setState(nextc);
    return free;
}
```



```java
protected final int tryAcquireShared(int unused) {
    /*
     * Walkthrough:
     * 1. 如果另一个线程持有写锁定，则失败。
     * 2. 否则，此线程符合锁定wrt状态，因此请询问是否应该因为队列策略而阻塞。 
     * 如果没有，请尝试通过CASing状态授予并更新计数。 请注意，步骤不会检查可重入获取，
     * 这会被推迟到完整版本，以避免在更典型的非重入情况下检查保留计数。
     * 3. 如果步骤2因为线程显然不合格或CAS失败或计数饱和而失败，则链接到具有完全重试循环的版本。
     */
    Thread current = Thread.currentThread();
    int c = getState();
    if (exclusiveCount(c) != 0 &&
        getExclusiveOwnerThread() != current)
        return -1;
    int r = sharedCount(c);
    if (!readerShouldBlock() &&
        r < MAX_COUNT &&
        compareAndSetState(c, c + SHARED_UNIT)) {
        if (r == 0) {
            firstReader = current;
            firstReaderHoldCount = 1;
        } else if (firstReader == current) {
            firstReaderHoldCount++;
        } else {
            HoldCounter rh = cachedHoldCounter;
            if (rh == null ||
                rh.tid != LockSupport.getThreadId(current))
                cachedHoldCounter = rh = readHolds.get();
            else if (rh.count == 0)
                readHolds.set(rh);
            rh.count++;
        }
        return 1;
    }
    return fullTryAcquireShared(current);
}
```

