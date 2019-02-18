## LockSupport

```java
public class LockSupport
```

用于创建锁和其他同步类的基本线程阻塞原语。

该类与使用它的每个线程关联一个许可证（在`Semaphore`类的意义上）。`park`：如果许可证可用，调用将立即返回，并在此过程中消耗它; 否则*可能会*阻塞。`unpark`：如果尚未提供许可证，则请求许可证。（与Semaphores不同，许可证不会累积。最多只有一个。）可靠的使用需要使用volatile（或原子）变量来控制何时park或unpark。对于volatile变量访问保持对这些方法的调用的顺序，但不一定是非volatile变量访问。

方法`park`并`unpark`提供阻止和解除阻塞线程的有效方法，这些线程不会遇到导致弃用方法的问题`Thread.suspend` 并且`Thread.resume`不能用于此类目的：一个线程调用之间的竞争`park`和尝试`unpark`它的另一个线程将由于许可而保持活跃性。此外，`park`如果调用者的线程被中断，将返回，并且支持超时版本。 `park`方法也可以在任何其他时间返回，“无理由”，因此通常必须在返回时重新检查条件的循环内调用。从这个意义上讲`park`，这可以作为“忙碌等待”的优化，不会浪费太多时间旋转，但必须与a配对`unpark`才能有效。

`park`第三种形式也支持 `blocker`对象参数。在线程被阻塞时记录此对象，以允许监视和诊断工具识别线程被阻止的原因。（这些工具可以使用方法访问阻止程序`getBlocker(Thread)`）强烈建议使用这些表单而不是没有此参数的原始表单。`blocker`在锁实现中提供的正常参数是`this`。

这些方法旨在用作创建更高级别同步实用程序的工具，并且对于大多数并发控制应用程序本身并不有用。该`park` 方法仅用于以下形式的构造：

```java
 while (!canProceed()) {
   // ensure request to unpark is visible to other threads
   ...
   LockSupport.park(this);
 }
```

在调用park之前，线程发布请求unpark的动作不需要锁定或阻塞。 因为每个线程只有一个许可证，所以park的任何中间使用，包括隐式地通过类加载，都可能导致无响应的线程（“丢失unpark”）。

**样品使用。**以下是先进先出非重入锁定类的草图：

```java
 class FIFOMutex {
   private final AtomicBoolean locked = new AtomicBoolean(false);
   private final Queue<Thread> waiters
     = new ConcurrentLinkedQueue<>();

   public void lock() {
     boolean wasInterrupted = false;
     // publish current thread for unparkers
     waiters.add(Thread.currentThread());

     // Block while not first in queue or cannot acquire lock
     while (waiters.peek() != Thread.currentThread() ||
            !locked.compareAndSet(false, true)) {
       LockSupport.park(this);
       // ignore interrupts while waiting
       if (Thread.interrupted())
         wasInterrupted = true;
     }

     waiters.remove();
     // ensure correct interrupt status on return
     if (wasInterrupted)
       Thread.currentThread().interrupt();
   }

   public void unlock() {
     locked.set(false);
     LockSupport.unpark(waiters.peek());
   }

   static {
     // Reduce the risk of "lost unpark" due to classloading
     Class<?> ensureLoaded = LockSupport.class;
   }
 }
```

`arg`是此线程park的同步对象,通常是`this`

```java
private static void setBlocker(Thread t, Object arg) {
    // Even though volatile, hotspot doesn't need a write barrier here.
    U.putObject(t, PARKBLOCKER, arg);
}
```

`park`:

除非许可证可用，否则禁用当前线程以进行线程调度。
如果许可证可用，那么它被消耗并且调用立即返回; 否则当前线程因线程调度而被禁用，并且在发生以下三种情况之一之前处于休眠状态：

- 其他一些线程以当前线程作为目标调用unpark; 
- 要么其他一些线程会中断当前线程; 
- 要么虚假的呼叫（即无缘无故）返回。

此方法不会报告这些方法中的哪一个导致返回。 调用者应该首先重新检查导致线程停放的条件。 例如，调用者还可以确定返回时线程的中断状态。

```java
public static void park(Object blocker) {
    Thread t = Thread.currentThread();
    setBlocker(t, blocker);
    U.park(false, 0L);
    setBlocker(t, null);
}
```

`unpark`:

如果给定线程尚不可用，则为其提供许可。 如果线程在park时被阻塞，那么它将解锁。 否则，它的下一次park调用保证不会阻止。 如果尚未启动给定线程，则不保证此操作完全没有任何效果。

```java
public static void unpark(Thread thread) {
    if (thread != null)
        U.unpark(thread);
}
```

`getBlocker`:

返回提供给最近调用尚未解除阻塞的park方法的阻塞对象，如果未阻止，则返回null。 返回的值只是一个瞬间快照 - 该线程可能已经在不同的阻止对象上解除阻塞或阻塞。

```java
public static Object getBlocker(Thread t) {
    if (t == null)
        throw new NullPointerException();
    return U.getObjectVolatile(t, PARKBLOCKER);
}
```

`nextSecondarySeed`:由ThreadLocal使用。返回伪随机初始化或更新的辅助种子。 由于包访问限制而从ThreadLocalRandom复制。

```java
static final int nextSecondarySeed() {
    int r;
    Thread t = Thread.currentThread();
    if ((r = U.getInt(t, SECONDARY)) != 0) {
        r ^= r << 13;   // xorshift
        r ^= r >>> 17;
        r ^= r << 5;
    }
    else if ((r = java.util.concurrent.ThreadLocalRandom.current().nextInt()) == 0)
        r = 1; // avoid zero
    U.putInt(t, SECONDARY, r);
    return r;
}
```

### 总结

LockSpork可以被Thread.interrupt或者`unpark`直接中断放弃阻塞状态。