#  AbstractQueuedSynchronizer

AbstractQueuedSynchronizer是jdk实现锁的基础框架，对于锁的理解非常重要。

## AbstractOwnableSynchronizer

```java
public abstract class AbstractOwnableSynchronizer
    implements java.io.Serializable
```

### java doc

可由线程专有的同步器。 此类为创建可能需要所有权概念的锁和相关同步器提供了基础。 AbstractOwnableSynchronizer类本身不管理或使用此信息。 但是，子类和工具可以使用适当维护的值来帮助控制和监视访问并提供诊断。

```java
/**
 * 独占模式同步的当前所有者
 */
private transient Thread exclusiveOwnerThread;
```

此类只有一个字段，和相应get、set方法。构造函数权限为protected。

## AbstractQueuedSynchronizer

```java
public abstract class AbstractQueuedSynchronizer
    extends AbstractOwnableSynchronizer
    implements java.io.Serializable 
```

### java doc

提供一个框架，用于实现依赖于先进先出（FIFO）等待队列的阻塞锁和相关同步器（信号量，事件等）。此类旨在成为依赖单个原子int值来表示状态的大多数同步器的有用基础。子类必须定义更改此状态的受保护方法，并根据要获取或释放的此对象定义该状态的含义。鉴于这些，本类中的其他方法执行所有排队和阻塞机制。子类可以维护其他状态字段，但仅跟踪使用方法getState（），setState（int）和compareAndSetState（int，int）操作的原子更新的int值的同步。
子类应定义为非公共内部帮助程序类，用于实现其封闭类的同步属性。类AbstractQueuedSynchronizer不实现任何同步接口。相反，它定义了诸如acquireInterruptibly（int）之类的方法，可以通过具体的锁和相关的同步器来适当地调用它们来实现它们的公共方法。

此类支持默认独占模式和共享模式之一或两者。在独占模式下获取时，其他线程尝试获取不能成功。多个线程获取的共享模式可能（但不一定）成功。这个类不会“理解”这些差异，除非在机械意义上，当共享模式获取成功时，下一个等待线程（如果存在）还必须确定它是否也可以获取。在不同模式下等待的线程共享相同的FIFO队列。通常，实现子类仅支持这些模式中的一种，但两者都可以在ReadWriteLock中发挥作用。仅支持独占模式或仅支持共享模式的子类无需定义支持未使用模式的方法。

此类定义了一个嵌套的AbstractQueuedSynchronizer.ConditionObject类，可以通过支持独占模式的子类用作Condition实现，方法isHeldExclusively（）报告是否针对当前线程独占保持同步，使用当前线程调用方法release（int） getState（）值完全释放此对象，并获取（int），给定此保存的状态值，最终将此对象恢复到其先前获取的状态。否则，AbstractQueuedSynchronizer方法不会创建此类条件，因此如果无法满足此约束，请不要使用它。 AbstractQueuedSynchronizer.ConditionObject的行为当然取决于其同步器实现的语义。

此类为内部队列提供检查，检测和监视方法，以及条件对象的类似方法。可以使用AbstractQueuedSynchronizer将它们按需要导出到类中，以实现其同步机制。

此类的序列化仅存储基础原子整数维护状态，因此反序列化对象具有空线程队列。需要可串行化的典型子类将定义一个readObject方法，该方法在反序列化时将其恢复为已知的初始状态。

用法
要使用此类作为同步器的基础，请通过使用getState（），setState（int）和/或compareAndSetState（int，int）检查和/或修改同步状态来重新定义以下方法（如果适用）：

- tryAcquire(int)
- tryRelease(int)
- tryAcquireShared(int)
- tryReleaseShared(int)
- isHeldExclusively()

默认情况下，每个方法都会抛出UnsupportedOperationException。 这些方法的实现必须是内部线程安全的，并且通常应该是短的而不是阻塞的。 定义这些方法是使用此类的唯一受支持的方法。 所有其他方法都被宣布为final，因为它们不能独立变化。
您还可以从AbstractOwnableSynchronizer中找到继承的方法，以便跟踪拥有独占同步器的线程。 建议您使用它们 - 这使监视和诊断工具能够帮助用户确定哪些线程持有锁。

即使此类基于内部FIFO队列，它也不会自动执行FIFO获取策略。 独占同步的核心采用以下形式：

```java
 Acquire:
     while (!tryAcquire(arg)) {
        enqueue thread if it is not already queued;
        possibly block current thread;
     }

 Release:
     if (tryRelease(arg))
        unblock the first queued thread;
```

（共享模式类似，但可能涉及级联信号。）
因为在入队之前调用了获取中的检查，所以新获取的线程可能会先阻塞其他被阻塞和排队的线程。但是，如果需要，您可以通过内部调用一个或多个检查方法来定义tryAcquire和/或tryAcquireShared来禁用插入，从而提供公平的FIFO采集顺序。特别是，如果hasQueuedPredecessors（）（专门设计为由公平同步器使用的方法）返回true，则大多数公平同步器可以定义tryAcquire返回false。其他变化是可能的。

对于默认的驳船（也称为贪婪，放弃和车队避让）策略，吞吐量和可扩展性通常最高。虽然这不能保证公平或无饥饿，但允许先前排队的线程在稍后排队的线程之前重新进行，并且每次重新保留都有一个无偏见的机会成功对抗传入的线程。此外，虽然获取不是通常意义上的“旋转”，但它们可能会在阻塞之前执行tryAcquire的多次调用以及其他计算。当仅短暂地保持独占同步时，这给旋转带来了大部分好处，而没有大部分责任。如果需要，可以通过先前调用获取具有“快速路径”检查的方法来增强此功能，可能预先检查hasContended（）和/或hasQueuedThreads（）仅在同步器可能不会被争用的情况下执行此操作。

该类通过将其使用范围专门化为可依赖于int状态，获取和释放参数的同步器以及内部FIFO等待队列，为同步提供了有效且可扩展的基础。如果这还不够，可以使用原子类，自己的自定义Queue类和LockSupport阻塞支持从较低级别构建同步器。

用法示例
这是一个不可重入的互斥锁类，它使用零值表示解锁状态，一个表示锁定状态。 虽然非重入锁并不严格要求记录当前所有者线程，但此类仍然这样做，以便更容易监视使用情况。 它还支持条件并公开一些检测方法：

```java
 class Mutex implements Lock, java.io.Serializable {

   // Our internal helper class
   private static class Sync extends AbstractQueuedSynchronizer {
     // Acquires the lock if state is zero
     public boolean tryAcquire(int acquires) {
       assert acquires == 1; // Otherwise unused
       if (compareAndSetState(0, 1)) {
         setExclusiveOwnerThread(Thread.currentThread());
         return true;
       }
       return false;
     }

     // Releases the lock by setting state to zero
     protected boolean tryRelease(int releases) {
       assert releases == 1; // Otherwise unused
       if (!isHeldExclusively())
         throw new IllegalMonitorStateException();
       setExclusiveOwnerThread(null);
       setState(0);
       return true;
     }

     // Reports whether in locked state
     public boolean isLocked() {
       return getState() != 0;
     }

     public boolean isHeldExclusively() {
       // a data race, but safe due to out-of-thin-air guarantees
       return getExclusiveOwnerThread() == Thread.currentThread();
     }

     // Provides a Condition
     public Condition newCondition() {
       return new ConditionObject();
     }

     // Deserializes properly
     private void readObject(ObjectInputStream s)
         throws IOException, ClassNotFoundException {
       s.defaultReadObject();
       setState(0); // reset to unlocked state
     }
   }

   // The sync object does all the hard work. We just forward to it.
   private final Sync sync = new Sync();

   public void lock()              { sync.acquire(1); }
   public boolean tryLock()        { return sync.tryAcquire(1); }
   public void unlock()            { sync.release(1); }
   public Condition newCondition() { return sync.newCondition(); }
   public boolean isLocked()       { return sync.isLocked(); }
   public boolean isHeldByCurrentThread() {
     return sync.isHeldExclusively();
   }
   public boolean hasQueuedThreads() {
     return sync.hasQueuedThreads();
   }
   public void lockInterruptibly() throws InterruptedException {
     sync.acquireInterruptibly(1);
   }
   public boolean tryLock(long timeout, TimeUnit unit)
       throws InterruptedException {
     return sync.tryAcquireNanos(1, unit.toNanos(timeout));
   }
 }
```

这是一个类似于CountDownLatch的latch类，只是它只需要一个信号来触发。 由于锁存器是非独占的，因此它使用共享的获取和释放方法。

```java
 class BooleanLatch {

   private static class Sync extends AbstractQueuedSynchronizer {
     boolean isSignalled() { return getState() != 0; }

     protected int tryAcquireShared(int ignore) {
       return isSignalled() ? 1 : -1;
     }

     protected boolean tryReleaseShared(int ignore) {
       setState(1);
       return true;
     }
   }

   private final Sync sync = new Sync();
   public boolean isSignalled() { return sync.isSignalled(); }
   public void signal()         { sync.releaseShared(1); }
   public void await() throws InterruptedException {
     sync.acquireSharedInterruptibly(1);
   }
 }
```

 