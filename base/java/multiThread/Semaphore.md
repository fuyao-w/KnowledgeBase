## Semaphore

```java
public class Semaphore implements java.io.Serializable
```

### java doc

计数信号量。 从概念上讲，信号量保持一组许可。 如果需要，每个acquire（）都会阻塞，直到有许可证可用，然后接受它。 每个 `release()` 都会增加许可证，可能会释放阻塞的`acquire()`。 但是，没有使用实际的许可对象; 信号量只保留可用数量并相应地采取行动。
信号量通常用于限制线程数，而不是访问某些（物理或逻辑）资源。 例如，这是一个使用信号量来控制对池的访问的类：

```java
 class Pool {
   private static final int MAX_AVAILABLE = 100;
   private final Semaphore available = new Semaphore(MAX_AVAILABLE, true);

   public Object getItem() throws InterruptedException {
     available.acquire();
     return getNextAvailableItem();
   }

   public void putItem(Object x) {
     if (markAsUnused(x))
       available.release();
   }

   // Not a particularly efficient data structure; just for demo

   protected Object[] items = ... whatever kinds of items being managed
   protected boolean[] used = new boolean[MAX_AVAILABLE];

   protected synchronized Object getNextAvailableItem() {
     for (int i = 0; i < MAX_AVAILABLE; ++i) {
       if (!used[i]) {
         used[i] = true;
         return items[i];
       }
     }
     return null; // not reached
   }

   protected synchronized boolean markAsUnused(Object item) {
     for (int i = 0; i < MAX_AVAILABLE; ++i) {
       if (item == items[i]) {
         if (used[i]) {
           used[i] = false;
           return true;
         } else
           return false;
       }
     }
     return false;
   }
 }
```

在获取项目之前，每个线程必须从信号量获取许可证，以保证项目可供使用。当线程完成项目后，它将返回到池中，并且许可证将返回到信号量，允许另一个线程获取该项目。请注意，调用acquire（）时不会保持同步锁定，因为这会阻止item返回到池中。信号量封装了限制访问池所需的同步，与维护池本身一致性所需的任何同步分开。

信号量初始化为1，并且使用的信号量最多只有一个许可证可用作互斥锁。这通常被称为二进制信号量，因为它只有两种状态：一种是可用的，或者是零可用的。当以这种方式使用时，二进制信号量具有属性（与许多Lock实现不同），“锁”可以由除所有者之外的线程释放（因为信号量没有所有权的概念）。这在某些特定的上下文中很有用，例如死锁恢复。

此类的构造函数可选择接受公平参数。设置为false时，此类不保证线程获取许可的顺序。特别是，允许进行限制，也就是说，调用acquire（）的线程可以在一直等待的线程之前分配许可 - 逻辑上新线程将自己置于等待线程队列的头部。当公平性设置为true时，信号量保证选择调用任何获取方法的线程以按照它们对这些方法的调用的处理顺序（先进先出; FIFO）获得许可。请注意，FIFO排序必然适用于这些方法中的特定内部执行点。因此，一个线程可以在另一个线程之前调用获取，但是在另一个线程之后到达排序点，并且类似地从该方法返回时。另请注意，不定时的tryAcquire方法不遵守公平性设置，但会采用任何可用的许可证。

通常，用于控制资源访问的信号量应初始化为公平，以确保没有线程缺乏访问资源。当将信号量用于其他类型的同步控制时，非公平排序的吞吐量优势通常超过公平性考虑。

该类还提供了一次获取和释放多个许可的便捷方法。这些方法通常比循环更有效和有效。但是，他们没有建立任何优先顺序。例如，如果线程A调用s.acquire（3）并且线程B调用s.acquire（2），并且两个许可证变为可用，则无法保证线程B将获得它们，除非它获得第一个并且Semaphore是在公平模式。

内存一致性效果：在调用“release”方法（如release（））之前的线程中的操作发生在成功的“获取”方法（例如另一个线程中的acquire（））之后的操作之前。

### 分析

信号量也有两个版本公平和非公平，运行原理在AQS中已经介绍过了。这里主要介绍钩子方法，和AQS中没有提到过得方法。



```java
protected final boolean tryReleaseShared(int releases) {
    for (;;) {
        int current = getState();
        int next = current + releases;
        if (next < current) // overflow
            throw new Error("Maximum permit count exceeded");
        if (compareAndSetState(current, next))
            return true;
    }
}
```

释放信号量`tryReleaseShared`钩子方法,原理也很简单。

```java
public void acquire() throws InterruptedException {
    sync.acquireSharedInterruptibly(1);
}
```

```java
public final void acquireSharedInterruptibly(int arg)
        throws InterruptedException {
    if (Thread.interrupted())
        throw new InterruptedException();
    if (tryAcquireShared(arg) < 0)
        doAcquireSharedInterruptibly(arg);
}
```

`tryAcquireShared`在尝试获取信号量失败后，调用AQS的`doAcquireSharedInterruptibly`

```java
private void doAcquireSharedInterruptibly(int arg)
    throws InterruptedException {
    final Node node = addWaiter(Node.SHARED);
    try {
        for (;;) {
            final Node p = node.predecessor();
            if (p == head) {
                int r = tryAcquireShared(arg);
                if (r >= 0) {
                    setHeadAndPropagate(node, r);
                    p.next = null; // help GC
                    return;
                }
            }
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                throw new InterruptedException();
        }
    } catch (Throwable t) {
        cancelAcquire(node);
        throw t;
    }
}
```

`doAcquireSharedInterruptibly`与普通doAcquireInterruptily区别在于，在尝试`tryAcquireShared`成功后调用了AQS的`setHeadAndPropagate`

```java
private void setHeadAndPropagate(Node node, int propagate) {
    Node h = head; // Record old head for check below
    setHead(node);
    /*
     * 如果以下情况尝试发信号通知下一个排队节点：
     * 传播由调用者指示，或者由前一个操作记录（在setHead之前或之后为h.waitStatus）
     * （注意：这使用waitStatus的符号检查，因为PROPAGATE状态可能转换为SIGNAL。 
     * 并且下一个节点正在共享模式中等待，或者我们不知道，
     * 因为它看起来是空的
     * 这两个检查中的保守性可能会导致不必要的唤醒，
     * 但只在当有多个竞争获取/释放时，所以大多数需要 无论如何现在或很快发出信号。
     */
    if (propagate > 0 || h == null || h.waitStatus < 0 ||
        (h = head) == null || h.waitStatus < 0) {
        Node s = node.next;
        if (s == null || s.isShared())
            doReleaseShared();
    }
}
```

setHeadAndPropagate设置队列头，并检查后继者是否可能在共享模式下等待，如果是传播，则设置传播  > 0或PROPAGATE状态。

```java
private void doReleaseShared() {
    /*
     *即使存在其他正在进行的获取/释放，也要确保发布传播。 
     * 如果它需要信号，这通常会尝试取消停止头部的接入者。 但如果没有，
     * 则将状态设置为PROPAGATE以确保在释放后继续传播。 此外，我们必须循环，
     * 以防在我们这样做时添加新节点。 此外，与unparkSuccessor的其他用法不同，
     * 我们需要知道CAS重置状态是否失败，如果是这样，则重新检查。
     */
    for (;;) {
        Node h = head;
        if (h != null && h != tail) {
            int ws = h.waitStatus;
            if (ws == Node.SIGNAL) {
                if (!h.compareAndSetWaitStatus(Node.SIGNAL, 0))
                    continue;            // loop to recheck cases
                unparkSuccessor(h);
            }
            else if (ws == 0 &&
                     !h.compareAndSetWaitStatus(0, Node.PROPAGATE))
                continue;                // loop on failed CAS
        }
        if (h == head)                   // loop if head changed
            break;
    }
}
```

