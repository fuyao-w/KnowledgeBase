#  AbstractQueuedSynchronizer

AbstractQueuedSynchronizer是jdk实现锁的基础框架，对于juc包的理解非常重要。
同时AQS的实现为CLH锁的变体。了解CLH锁也至关重要。

## CLH

![CLH锁状态转换](https://github.com/TransientWang/KnowledgeBase/blob/master/picture/CLH锁.png.md)

```java

public abstract class CLHAbslock implements Lock {
    //保存尾部节点的引用，便于将新节点添加到节点尾部
    AtomicReference<Node> tail; 
    //当前线程所持有的节点
    ThreadLocal<Node> curNode;
    //帮助回收节点
    ThreadLocal<Node> preNode;


    public CLHAbslock() {
        tail = new AtomicReference<Node>(new Node());
        this.curNode = new ThreadLocal<Node>() {
            @Override
            protected Node initialValue() {
                return new Node();
            }
        };
        this.preNode = new ThreadLocal<>();

    }

    class Node {
        volatile AtomicBoolean locked = new AtomicBoolean();
    }
```

我们现在将注意力转向不同类型的队列锁定。上面的代码显示了CLHLock类的字段，构造函数和Node类。此类在Node对象中记录每个线程的状态，该对象具有布尔锁定字段。如果该字段是true，那么相应的线程已获得锁定，或者正在等待锁定。如果该字段为false，则该线程已释放锁定。锁本身表示为Node对象的虚拟链表。我们使用术语“虚拟”，因为列表是隐式的：每个线程通过线程局部pred变量引用其前任。公共尾部字段是最近添加到队列的节点的AtomicReference <Node>。如图7.10所示，为了获取锁，线程将其Node的锁定字段设置为true，表明该线程不是准备释放锁。该线程将getAndSet（）应用于尾部字段，以使其自己的节点成为队列的尾部，同时获取对其前任的Node的引用。然后该线程在前任的锁定字段上旋转，直到前任释放锁。要释放锁定，线程会将其节点的锁定字段设置为false。**然后它重新使用其前驱的Node作为未来锁访问的新节点。它可以这样做，因为此时线程的前驱Node不再被前驱使用，并且线程持有的Node可以由线程的后继者和尾部引用。虽然我们在示例中没有这样做，但它是可能的回收节点**，以便如果有Llocks，并且每个线程一次最多访问一个锁，那么CLHLock类只需要O（L + n）空间，而不是ALock类的O（Ln）。图7.11显示了一个典型的CLHLock执行。当一个线程释放它的锁时，它只使其后继的缓存无效。它提供先到先得的公平性。也许这种锁定算法的唯一缺点是它在无缓存的NUMA体系结构上表现不佳。每个线程都会自旋，等待其前置节点的锁定字段变为false。如果此内存位置是远程的，则性能会受到影响。但是，在缓存一致的体系结构中，这种方法应该可以正常工作。

```java
public class CLHLock extends CLHAbslock {
    @Override
    public void lock() {
        //获取当前线程所持有的节点
        Node node = curNode.get();
        node.locked.set(true);
        //将当前节点添加到tail
        Node pre = tail.getAndSet(node);
        //保存当前节点的前驱节点
        preNode.set(pre);
        while (pre.locked.get());

    }

   /**
     * @date 2018/12/17 21:30
     * @return void
     * @Description 当前线程解锁后，当前线程前驱节点就不会再被引用。而当前线程所持有的节点会被后继和尾节点引用。
     * 而在解锁后前驱节点和当前线程持有节点的状态是一样的，所以当前节点完全可以被前驱节点所替换，
     * 当前节点将被JVM回收，也可以不这么做。
     * @Param []
     **/
    @Override
    public void unlock() {
        Node node = curNode.get();
        node.locked.set(false);
        //将当前节点替换为前驱节点，帮助JVM回收当前线程持有的节点
        curNode.set(preNode.get());
    }
}
```

CLHLockclass：锁定获取和释放。 最初，尾部字段指的是其锁定字段为假的QNode。 ThreadAthen将getAndSet（）应用于尾部字段，以将其QNode插入队列的尾部，同时获取对其前任的QNode的引用。 接下来，Bdoes相同，将其QNode插入队列的尾部。然后通过将其节点的锁定字段设置为false来释放锁定。 然后它会回收pred引用的QNode，以便将来锁定访问。








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

 ### Node

等待队列节点类。
等待队列是“CLH”（Craig，Landin和Hagersten）锁定队列的变体。 CLH锁通常用于自旋锁。我们使用它们来阻止同步器，但是使用相同的基本策略来保存关于其节点的前驱中的线程的一些控制信息。每个节点中的“状态”字段跟踪线程是否阻塞。在其前驱发布时，将发出节点信号。否则队列的每个节点都用作持有单个等待线程的特定通知样式监视器。状态字段不控制线程是否是按钮锁等。线程可能会尝试获取它是否在队列中的第一个。但首先并不能保证成功;它只给予抗争的权利。因此，当前发布的竞争者线程可能需要重新审视。
要排入CLH锁定，您将其原子拼接为新的
 尾巴。要出列，您只需设置头部字段即可。

```
      +------+  prev +-----+       +-----+
 head |      | <---- |     | <---- |     |  tail
      +------+       +-----+       +-----+
```

插入CLH队列只需要对“尾部”进行单个原子操作，因此存在从未排队到排队的简单原子点划分。同样，出列只涉及更新“头部”。但是，节点需要更多的工作来确定他们的继任者是谁，部分是为了处理由于超时和中断而可能的取消。
“prev”链接（未在原始CLH锁中使用）主要用于处理取消。如果节点被取消，则其后继者（通常）重新链接到未取消的前驱。有关自旋锁的类似机制的解释，请参阅Scott和Scherer的论文
我们还使用“next”链接来实现阻塞机制。每个节点的线程ID保存在自己的节点中，因此前驱者通过遍历下一个链接来通知下一个节点，以确定它是哪个线程。后继者的确定必须避免使用新排队节点的比赛来设置其前驱的“下一个”字段。必要时，当节点的后继者看起来为空时，通过从原子更新的“尾部”向后检查来解决这个问题。 （或者，换句话说，下一个链接是一个优化，所以我们通常不需要后向扫描。）
 取消为基本算法引入了一些保守性。由于我们必须轮询取消其他节点，我们可能会忽略被取消的节点是在我们前面还是在我们后面。这是通过取消后始终取消停车的继承人来处理的，这使得他们能够稳定在新的前驱上，除非我们能够确定一位将承担此责任的未经撤销的前驱。
CLH队列需要一个虚拟标头节点才能启动。但是我们不会在构造上创建它们，因为如果没有争用就会浪费精力。相反，构造节点并在第一次争用时设置头尾指针。
 等待条件的线程使用相同的节点，但使用其他链接。条件只需要链接简单（非并发）链接队列中的节点，因为它们仅在完全保持时才被访问。等待时，将节点插入条件队列。根据信号，节点被转移到主队列。状态字段的特殊值用于标记节点所在的队列。
 感谢Dave Dice，Mark Moir，Victor Luchangco，Bill Scherer和Michael Scott以及JSR-166专家组成员对本课程设计的有益想法，讨论和批评。

```java
static final class Node {
    /** 标记表示节点正在共享模式中等待 */
    static final Node SHARED = new Node();
    /** 标记表示节点正在独占模式下等待 */
    static final Node EXCLUSIVE = null;

    /** waitStatus值表示线程已取消。 */
    static final int CANCELLED =  1;
    /** waitStatus值表示后继者的线程需要unparking。 */
    static final int SIGNAL    = -1;
    /**waitStatus值表示线程正在等待条件。 */
    static final int CONDITION = -2;
    /**
     * waitStatus值表示下一个acquireShared应无条件传播。
     */
    static final int PROPAGATE = -3;

    /**
     * 状态字段，仅接受值：
     *   SIGNAL:    此节点的后继是（或将很快）被阻塞（通过park），因此当前节点在释放或取消时必须取消其后继。
     *   为了避免竞争，获取方法必须首先指示它们需要信号，然后重试原子获取，然后在失败时阻止。
     *   CANCELLED:  由于超时或中断，此节点被取消。 节点永远不会离开这个状态。 
     *               特别是，具有已取消节点的线程永远不会再次阻塞。
     *   CONDITION:  此节点当前处于条件队列中。 在传输之前，它不会用作同步队列节点，此时状态将设置为0.
     *               （此处使用此值与字段的其他用法无关，但可简化机制。）
     *   PROPAGATE:  releaseShared应该传播到其他节点。 在doReleaseShared中设置（仅限头节点）
     *               以确保继续传播，即使其他操作已经介入。
     *   0:          以上都不是
     *
     * 数值以数字方式排列以简化使用。 非负值意味着节点不需要发信号。 
     * 因此，大多数代码不需要检查特定值，仅用于符号。
     *
     * 对于正常的同步节点，该字段初始化为0，对于条件节点，该字段初始化为CONDITION。
     * 它使用CAS（或可能的情况下，无条件的易失性写入）进行修改。
     */
    volatile int waitStatus;

    /**
     * 链接到当前节点/线程依赖的前导节点以检查waitStatus。 在排队期间分配，并且仅在出列时才被排除（为了GC）。 此外，在取消前驱时，我们在找到未取消的一个时短路，这将永远存在，因为头节点永远不会被取消：节点由于成功获取而变为仅头。
      * 取消的线程永远不会成功获取，并且线程仅取消自身，而不取消任何其他节点。
     */
    volatile Node prev;

    /**
     * 链接到当前节点/线程在释放时取消驻留的后继节点。 
     * 在排队期间分配，在绕过取消的前驱时进行调整，并在出列时排除（为了GC）。 
     * enq操作直到附加后才分配前驱的next字段，因此看到next字段为null并不一定意味着该节点位
     * 于队列的末尾。 但是，如果下一个字段看起来为空，我们可以从尾部扫描prev's进行仔细检查。 
     * 已取消节点的下一个字段设置为指向节点本身而不是null，以使isOnSyncQueue的生活更轻松。
     */
    volatile Node next;

    /**
     * 排队此节点的线程。 在构造时化并在使用后消失。
     */
    volatile Thread thread;

    /**
     * 链接到等待条件的下一个节点，或特殊值SHARED。
     * 因为条件队列只有在保持独占模式时才被访问，所以我们只需要一个简单的链接队列来在节点
     * 等待条件时保存节点。 然后将它们转移到队列中以重新获取。 
     * 并且因为条件只能是独占的，所以我们通过使用特殊值来指示共享模式来保存字段。
     */
    Node nextWaiter;

    /**
     * 如果节点在共享模式下等待，则返回true。
     */
    final boolean isShared() {
        return nextWaiter == SHARED;
    }

    /**
     * 返回上一个节点，如果为null则抛出NullPointerException。 
     * 在前驱不能为null时使用。 可以省略空检查，但检查其可以帮助VM。
     *
     * @return 此节点的前驱
     */
    final Node predecessor() {
        Node p = prev;
        if (p == null)
            throw new NullPointerException();
        else
            return p;
    }

    /** 建立初始头或SHARED标记。 */
    Node() {}

    /** addWaiter使用的构造函数。 */
    Node(Node nextWaiter) {
        this.nextWaiter = nextWaiter;
        THREAD.set(this, Thread.currentThread());
    }

    /** addConditionWaiter使用的构造方法。 */
    Node(int waitStatus) {
        WAITSTATUS.set(this, waitStatus);
        THREAD.set(this, Thread.currentThread());
    }

    /** CASes waitStatus field. */
    final boolean compareAndSetWaitStatus(int expect, int update) {
        return WAITSTATUS.compareAndSet(this, expect, update);
    }

    /** CASes next field. */
    final boolean compareAndSetNext(Node expect, Node update) {
        return NEXT.compareAndSet(this, expect, update);
    }

    final void setPrevRelaxed(Node p) {
        PREV.set(this, p);
    }

    // VarHandle mechanics
    private static final VarHandle NEXT;
    private static final VarHandle PREV;
    private static final VarHandle THREAD;
    private static final VarHandle WAITSTATUS;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            NEXT = l.findVarHandle(Node.class, "next", Node.class);
            PREV = l.findVarHandle(Node.class, "prev", Node.class);
            THREAD = l.findVarHandle(Node.class, "thread", Thread.class);
            WAITSTATUS = l.findVarHandle(Node.class, "waitStatus", int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
```

