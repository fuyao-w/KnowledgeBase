##  z·ReenTrantLock

```java
public class ReentrantLock implements Lock, java.io.Serializable
```

### java doc

可重入互斥锁具有与使用同步方法和语句访问的隐式监视器锁相同的基本行为和语义，但具有扩展功能。
ReentrantLock由最后成功锁定的线程拥有，但尚未解锁。当锁不是由另一个线程拥有时，线程调用锁将返回，成功获取锁。如果当前线程已拥有锁，则该方法将立即返回。这可以使用方法`isHeldByCurrentThread（）`和`getHoldCount（）`来检查。

此类的构造函数接受可选的`fairness`参数。当设置为true时，在争用下，锁定有利于授予对等待时间最长的线程的访问权限。否则，此锁定不保证任何特定的访问顺序。使用由许多线程访问的公平锁的程序可以显示比使用默认设置的程序更低的总吞吐量（即，更慢;通常慢得多），但是获得锁的时间变化较小并且保证缺乏饥饿。但请注意，锁的公平性并不能保证线程调度的公平性。因此，使用公平锁的许多线程中的一个可以连续多次获得它，而其他活动线程没有进展并且当前没有持有锁。另请注意，不定时的tryLock（）方法不遵循公平性设置。即使其他线程正在等待，如果锁可用，它也会成功。

建议练习总是立即跟随调用锁定try块，最常见的是在构造之前/之后，例如：

```java
 class X {
   private final ReentrantLock lock = new ReentrantLock();
   // ...

   public void m() {
     lock.lock();  // block until condition holds
     try {
       // ... method body
     } finally {
       lock.unlock()
     }
   }
 }
```

除了实现Lock接口之外，此类还定义了许多用于检查锁定状态的公共和受保护方法。 其中一些方法仅适用于仪器和监测。

此类的序列化与内置锁的行为方式相同：反序列化锁处于解锁状态，无论序列化时的状态如何。

此锁通过同一线程最多支持2147483647个递归锁。 尝试超过此限制会导致锁定方法的错误抛出。

### 分析

重入锁作为并发包里面的常用对象，也是依赖于AQS实现的。并且通过实现AQS的钩子方法`tryAcquire`提供了同一线程多次获取锁的功能。还提供了公平获取和非公平（默认）获取锁的实现。

```java
abstract static class Sync extends AbstractQueuedSynchronizer
```

Sync是此锁的同步控制基础。 在下面转换为公平和非公平版本。 使用AQS状态表示锁定的保持数。

Sync主要添加了一个非公平状态下获取锁的方法`nonfairTryAcquire`用于`tryLock`的实现实现了AQS的钩子方法`tryRelease`：

```java
final boolean nonfairTryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    int c = getState();
    if (c == 0) {
        if (compareAndSetState(0, acquires)) {
            setExclusiveOwnerThread(current);
            return true;
        }
    }
    else if (current == getExclusiveOwnerThread()) {
        int nextc = c + acquires;
        if (nextc < 0) // overflow
            throw new Error("Maximum lock count exceeded");
        setState(nextc);
        return true;
    }
    return false;
}
```

nonfairTryAcquire的实现很好理解，通过CAS将state设置为1，成功就获取了锁，如果占用锁的线程是当前线程，那么可以通过增加AQS.state的值来，让本线程再次获取锁，这就是重入锁的实现原理。

```java
protected final boolean tryRelease(int releases) {
    int c = getState() - releases;
    if (Thread.currentThread() != getExclusiveOwnerThread())
        throw new IllegalMonitorStateException();
    boolean free = false;
    if (c == 0) {
        free = true;
        setExclusiveOwnerThread(null);
    }
    setState(c);
    return free;
}
```

tryRelease释放当前线程占用的锁，将AQS.state减少，如果state为0就将当前占用锁线程设置为null。所以当前线程获取多少次锁，就必须释放相应的次数来释放锁。

针对公平和非公平两个版本，有两个Sync的实现类`NonfairSync`和`FairSync`，两个都实现了钩子方法`tryAcquire`：

```java
protected final boolean tryAcquire(int acquires) {
    return nonfairTryAcquire(acquires);
}
```

`NonfairSync`就是调用Sync的`nonfairTryAcquire`。

```java
protected final boolean tryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    int c = getState();
    if (c == 0) {
        if (!hasQueuedPredecessors() &&
            compareAndSetState(0, acquires)) {
            setExclusiveOwnerThread(current);
            return true;
        }
    }
    else if (current == getExclusiveOwnerThread()) {
        int nextc = c + acquires;
        if (nextc < 0)
            throw new Error("Maximum lock count exceeded");
        setState(nextc);
        return true;
    }
    return false;
}
```

`FairSync`通过`hasQueuedPredecessors`实现只有在当前线程之前没有排队的情况下才可以尝试获取锁。换句话说公平锁不同的线程尝试修改state获取锁时候前面不能有线程在排队，所以大家占用锁的几率是一样的。

```java
public final boolean hasQueuedPredecessors() {
    Node h, s;
    if ((h = head) != null) {
        if ((s = h.next) == null || s.waitStatus > 0) {
            s = null; // traverse in case of concurrent cancellation
            for (Node p = tail; p != h && p != null; p = p.prev) {
                if (p.waitStatus <= 0)
                    s = p;
            }
        }
        if (s != null && s.thread != Thread.currentThread())
            return true;
    }
    return false;
}
```

hasQueuedPredecessors在state==0后重新判断，是否此时没有线程占用锁，或者在这个间隙内有其他线程又成功占用锁的情况下有没有其他线程进入等待队列。如果没有返回false,此时当前线程会尝试修改state，如果失败就直接将当先线程持有节点入队。这样不会发生后来的线程插队的情况。

其他实现

`lockInterruptibly`:

```java
public void lockInterruptibly() throws InterruptedException {
    sync.acquireInterruptibly(1); 
}
```

```java
public final void acquireInterruptibly(int arg)
        throws InterruptedException {
    if (Thread.interrupted())
        throw new InterruptedException();
    if (!tryAcquire(arg))
        doAcquireInterruptibly(arg);
}
```

```java
private void doAcquireInterruptibly(int arg)
    throws InterruptedException {
    final Node node = addWaiter(Node.EXCLUSIVE);
    try {
        for (;;) {
            final Node p = node.predecessor();
            if (p == head && tryAcquire(arg)) {
                setHead(node);
                p.next = null; // help GC
                return;
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

在尝试获取锁期间如果被中断，则会直接抛出`InterruptedException`。

`tryLock(long timeout, TimeUnit unit)`：

```
public boolean tryLock(long timeout, TimeUnit unit)
        throws InterruptedException {
    return sync.tryAcquireNanos(1, unit.toNanos(timeout));
}
```

```java
public final boolean tryAcquireNanos(int arg, long nanosTimeout)
        throws InterruptedException {
    if (Thread.interrupted())
        throw new InterruptedException();
    return tryAcquire(arg) ||
        doAcquireNanos(arg, nanosTimeout);
}
```

```java
private boolean doAcquireNanos(int arg, long nanosTimeout)
        throws InterruptedException {
    if (nanosTimeout <= 0L)
        return false;
    final long deadline = System.nanoTime() + nanosTimeout;
    final Node node = addWaiter(Node.EXCLUSIVE);
    try {
        for (;;) {
            final Node p = node.predecessor();
            if (p == head && tryAcquire(arg)) {
                setHead(node);
                p.next = null; // help GC
                return true;
            }
            nanosTimeout = deadline - System.nanoTime();
            if (nanosTimeout <= 0L) {
                cancelAcquire(node);
                return false;
            }
            if (shouldParkAfterFailedAcquire(p, node) &&
                nanosTimeout > SPIN_FOR_TIMEOUT_THRESHOLD)
                LockSupport.parkNanos(this, nanosTimeout);
            if (Thread.interrupted())
                throw new InterruptedException();
        }
    } catch (Throwable t) {
        cancelAcquire(node);
        throw t;
    }
}
```

如果在规定时间内不能获取锁，就取消申请锁。

