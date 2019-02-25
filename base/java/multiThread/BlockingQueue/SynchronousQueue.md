

## SynchronousQueue

Cached线程池的阻塞队列

```java
public class SynchronousQueue<E> extends AbstractQueue<E>
    implements BlockingQueue<E>, java.io.Serializable {
```

###  java doc

阻塞队列，其中每个插入操作必须由另一个线程等待一个相应的删除操作，并且反之亦然。同步队列没有任何内部容量，甚至没有容量。您不能在同步队列中使用`peek`，因为只有在您尝试删除元素时才会出现该元素; 你不能插入一个元素（使用任何方法），除非另一个线程试图删除它; 你不能迭代，因为没有什么可以迭代。队列的 头部是第一个排队插入线程试图添加到队列的元素; 如果没有这样的排队线程，则没有可用于删除的元素 poll()并将返回null。出于其他Collection方法的目的 （例如contains），a SynchronousQueue充当空集合。此队列不允许null元素。
同步队列类似于CSP和Ada中使用的集合点通道。它们非常适用于切换设计，其中在一个线程中运行的对象必须与在另一个线程中运行的对象同步，以便将其传递给某些信息，事件或任务。

此类支持用于排序等待生产者和消费者线程的可选公平策略。默认情况下，不保证此顺序。但是，使用公平性构造的队列true以FIFO顺序授予线程访问权限。



### 分析

该类实现了W.N.Scherer III和M.L.Scott的“Nonblocking Concurrent Objects with Condition Synchronization”中描述的双重栈和双重队列算法的扩展。 （Lifo）栈用于非公平模式，而（Fifo）队列用于公平模式。两者的表现大致相似。 Fifo通常支持争用下更高的吞吐量，但Lifo在常见应用程序中保持更高的线程局部性。
双队列（以及类似的堆栈）是在任何给定时间或者保持“数据” - 由`put`操作提供的项目，或“request” - 表示获取操作的时隙，或者是空的。 对“fulfill”的调用（即，从保持数据的队列请求项目的调用，或反之亦然）使互补节点出列（节点配对成功）。 这些队列最有趣的特性是任何操作都可以确定队列所处的模式，并在不需要锁定的情况下采取相应的行动。

队列和堆栈都扩展了抽象类Transferer，它定义了执行put或take的单个方法传输。这些统一为单一方法，因为在双数据结构中，put和take操作是对称的，因此几乎所有代码都可以组合。由此产生的转移方法是长期的，但是如果分解成几乎重复的部分，则更容易遵循。
队列和堆栈数据结构共享许多概念上的相似之处，但很少有具体细节。为简单起见，它们保持不同，以便以后可以单独进化。
这里的算法与上述论文中的版本的不同之处在于扩展它们以用于同步队列，以及处理取消。主要区别包括：
​	

1. 原始算法使用了位标记指针，但这里的节点使用节点中的模式位，从而导致许多进一步的调整。

2. SynchronousQueues必须阻止等待完成的线程。

3. 支持通过超时和中断取消，包括从列表中清除已取消的节点/线程，以避免垃圾保留和内存耗尽。


阻塞主要使用LockSupport park / unpark完成，除了看起来是下一个要完成的节点首先旋转一点（仅在多处理器上）。在非常繁忙的同步队列中，旋转可以显着提高吞吐量。在不太忙碌的人身上，旋转量足够小，不易引人注意。

在队列与堆栈中以不同方式进行清理。对于队列，我们几乎总能在取消时在O（1）时间内立即删除节点（模式重试以进行一致性检查）。但如果它可能被固定为当前尾部，它必须等到一些后续取消。对于堆栈，我们需要一个潜在的O（n）遍历来确保我们可以删除 node，但是这可以与访问堆栈的其他线程同时运行。
虽然垃圾收集处理大多数节点回收问题，否则会使非阻塞算法复杂化，但要注意“忘记”对数据，其他节点和线程的引用，这些引用可能会被阻塞的线程长期保留。如果设置为null否则会与主算法冲突，这可以通过将节点的链接更改为现在指向节点本身来完成。这对于堆栈节点来说并不是很多（因为被阻塞的线程不会挂起到旧的头指针），但是必须积极地忘记Queue节点中的引用，以避免任何节点自到达之后所引用的所有内容的可达性。

`Transferer`是实现SynchronousQueue的模板类。通过继承这个类，实现了公平版本的双队列，和非公平的双栈。

```JAVA
/**
 * 双栈和队列的共享内部API。
 **/
abstract static class Transferer<E> {
    /**
     * 执行put或take。
     *
     * @param e 如果非空，则要交给消费者的item;
     *          如果为null，请求传输返回生产者提供的项目。
     * @param timed if this operation should timeout
     * @param nanos the timeout, in nanoseconds
     * @return 如果非空，则提供或收到的item; 如果为null,
     *         由于超时或中断，操作失败 --
     *         调用者可以通过检查Thread.interrupted来区分发生了哪些。
     */
    abstract E transfer(E e, boolean timed, long nanos);
}
```

`TransferStack`是非公平

```java
static final class TransferStack<E> extends Transferer<E> {
    /**
     * 这扩展了Scherer-Scott双栈算法，除了其他方式之外，通过使用“覆盖”节点而不是位标记指针进行区分：完成操作推送标记节点
     * （在模式中设置FULFILLING位）以保留一个点以匹配等待 节点。
     **/
        /** Node代表一个未实现的消费者 */
        static final int REQUEST    = 0;
        /** Node代表一个未实现的生产者 */
        static final int DATA       = 1;
        /** 节点正在履行另一个未实现的数据或请求 */
        static final int FULFILLING = 2;
```

```java
static final class SNode {
    volatile SNode next;        // 堆栈中的下一个节点
    volatile SNode match;       // 节点与此匹配
    volatile Thread waiter;     // to control park/unpark
    Object item;                // data; or null for REQUESTs
    int mode;
    // Note: item和mode字段不需要是volatile，因为它们总是先写入,后被读取,
    // 其他易变/原子操作。
```

### 非公平transfer

```java
E transfer(E e, boolean timed, long nanos) {
    /*
     * 基本算法是循环尝试以下三种操作之一：
     *
     * 1. 如果显然是空的或已经包含相同模式的节点,尝试在堆栈上推送节点并等待匹配, 返回它，如果取消则返回null。
     *
     * 2. 如果显然包含互补模式的节点，
     *    尝试将一个充实的节点推送到堆栈, 与相应的等待节点匹配, 
     *    从堆栈弹出，并返回匹配的项目. 由于执行操作3的其他线程，
     *    实际上可能不需要匹配或取消链接：
     *
     * 3. 如果堆栈顶部已经拥有另一个满足节点,
     *    通过执行匹配和/或弹出操作来帮助它，然后继续。 
     *    帮助的代码与实现的代码基本相同, 除了它不返回该项目。
     */

    SNode s = null; // constructed/reused as needed
    int mode = (e == null) ? REQUEST : DATA;

    for (;;) {
        SNode h = head;
        if (h == null || h.mode == mode) {  // empty or same-mode
            if (timed && nanos <= 0L) {     // can't wait
                if (h != null && h.isCancelled())
                    casHead(h, h.next);     // pop cancelled node
                else
                    return null;
            } else if (casHead(h, s = snode(s, e, h, mode))) {
                SNode m = awaitFulfill(s, timed, nanos);
                if (m == s) {               // wait was cancelled
                    clean(s);
                    return null;
                }
                if ((h = head) != null && h.next == s)
                    casHead(h, s.next);     // help s's fulfiller
                return (E) ((mode == REQUEST) ? m.item : s.item);
            }
        } else if (!isFulfilling(h.mode)) { // try to fulfill
            if (h.isCancelled())            // already cancelled
                casHead(h, h.next);         // pop and retry
            else if (casHead(h, s=snode(s, e, h, FULFILLING|mode))) {
                for (;;) { // loop until matched or waiters disappear
                    SNode m = s.next;       // m is s's match
                    if (m == null) {        // all waiters are gone
                        casHead(s, null);   // pop fulfill node
                        s = null;           // use new node next time
                        break;              // restart main loop
                    }
                    SNode mn = m.next;
                    if (m.tryMatch(s)) {
                        casHead(s, mn);     // pop both s and m
                        return (E) ((mode == REQUEST) ? m.item : s.item);
                    } else                  // lost match
                        s.casNext(m, mn);   // help unlink
                }
            }
        } else {                            // help a fulfiller
            SNode m = h.next;               // m is h's match
            if (m == null)                  // waiter is gone
                casHead(h, null);           // pop fulfilling node
            else {
                SNode mn = m.next;
                if (m.tryMatch(h))          // help match
                    casHead(h, mn);         // pop both h and m
                else                        // lost match
                    h.casNext(m, mn);       // help unlink
            }
        }
    }
}
```

transfer是SynchronousQueue阻塞队列的（非公平）核心方法，通过参数`e`决定当前应该`put`还是`take`。如果参数e为null，则将mode字段设置为REQUEST，代表当前是put方法，如果不为null，则将mode字段设置为DATA,代表当前是`take`。

如果头结点为null或者头结点的模式与当前模式相同，当前线程应该做的就是创建一个节点入栈并等待其他线程与当前节点匹配，需要判断节点是否应该超时和是否已经超时，如果该节点已经超时了，并且节点已经被取消了，则将节点后移。如果节点没有取消，则直接返回null。

如果当前没超时的要求，则将当前头结点添加到栈顶，并且调用`awaitFulfill`等待，向节点中添加数据，或者取走数据。

```java
        /**
         * 旋转/阻塞直到节点s与执行操作匹配。
         *
         * @param s the waiting node
         * @param timed true if timed wait
         * @param nanos timeout value
         * @return matched node, or s if cancelled
         */
SNode awaitFulfill(SNode s, boolean timed, long nanos) {
    /*
     *当节点/线程即将阻塞时，它会设置其waiter字段，然后在实际park之前至少再重新检查一次状态，从而覆盖竞赛与履行者，注意到waiter非空，因此应该被唤醒。
     *
     * 当出现在呼叫点的节点调用位于堆栈头部时，调用park之前会有旋转，以避免生产者和消费者在非常接近的时间到达时阻塞。 这可能发生在足以打扰多处理器。
     *
     * 返回主循环的检查顺序反映了中断优先于正常返回的事实，正常返回优先于超时。 （因此，在超时时，最后一次检查匹配是在放弃之前完成的。）除了来自不定时的SynchronousQueue的调用。{poll / offer}不检查中断并且根本不等待，因此被困在传输方法中 比调用awaitFulfill。
     */
    final long deadline = timed ? System.nanoTime() + nanos : 0L;
    Thread w = Thread.currentThread();
    int spins = shouldSpin(s)
        ? (timed ? MAX_TIMED_SPINS : MAX_UNTIMED_SPINS)
        : 0;
    for (;;) {
        if (w.isInterrupted())
            s.tryCancel();
        SNode m = s.match;
        if (m != null)
            return m;
        if (timed) {
            nanos = deadline - System.nanoTime();
            if (nanos <= 0L) {
                s.tryCancel();
                continue;
            }
        }
        if (spins > 0) {
            Thread.onSpinWait();
            spins = shouldSpin(s) ? (spins - 1) : 0;
        }
        else if (s.waiter == null)
            s.waiter = w; // establish waiter so can park next iter
        else if (!timed)
            LockSupport.park(this);
        else if (nanos > SPIN_FOR_TIMEOUT_THRESHOLD)
            LockSupport.parkNanos(this, nanos);
    }
}
```

`awaitFulfill`通过spins字段确定当前线程应该自旋的次数，然后进入到自旋中，等待节点的match被填充。每次将spins减1。如果已经超时，则将当前节点取消。如果spins减少到0，则将当前线程赋值给节点的waiter字段，然后重新进行一次自旋，然后将当前线程阻塞。

重新调到外层方法，`awaitFulfill`返回了match字段。如果发现当前节点被取消，则清除当前节点，然后返回null。如果当前节点在等待期间有有节点插入了栈，则将头结点后移到当前节点后面。也就是栈顶两个节点进行了模式匹配，将元素传递出去。最后返回，如果当前是REQUEST模式。则返回awaitFulfill方法返回的match.item，否则返回当前节点的item(为null)。

如果头结点不为null,并且头结点的模式与当前模式不同，则可以尝试与头结点进行匹配。同样会判断节点被取消的情况,然后将节点入栈并将模式标记为正在匹配,如果当前栈中节点没有后继节点，则跳出内层自旋，重新进行外层自旋。否则尝试匹配，匹配成功后根据模式返回item。将栈顶两个已经匹配的节点出栈。（出栈操作可能被第三步辅助完成）

```java
boolean tryMatch(SNode s) {
    if (match == null &&
        SMATCH.compareAndSet(this, null, s)) {
        Thread w = waiter;
        if (w != null) {    // waiters need at most one unpark
            waiter = null;
            LockSupport.unpark(w);
        }
        return true;
    }
    return match == s;
}
```

如果发现有其他节点正在匹配头结点，则帮助匹配。步骤与第二步骤差不多，除了没有返回值。

### transferQueue

```java
static final class TransferQueue<E> extends Transferer<E> {
    /*
     * 这扩展了Scherer-Scott双队列算法，除了其他方式之外，通过在节点内使用模式而不是标记指针而不同。 该算法比堆栈更简单，
     * 因为履行者不需要显式节点，并且匹配是通过CAS将QNode.item字段从非null变为null
     * （对于put）或反之亦然（for take）来完成的。
     */
    
        /** Head of queue */
        transient volatile QNode head;
        /** Tail of queue */
        transient volatile QNode tail;
        /**
         * 引用可能尚未从队列中取消链接的已取消节点，因为它是取消时最后插入的节点。
         */
        transient volatile QNode cleanMe;
```

```java
 /** Node class for TransferQueue. */
static final class QNode {
    volatile QNode next;          // next node in queue
    volatile Object item;         // CAS'ed to or from null
    volatile Thread waiter;       // to control park/unpark
    final boolean isData;
```

### transfer

```java
E transfer(E e, boolean timed, long nanos) {
    /* 基本算法是循环尝试采取以下两种操作之一：
     *
     * 1. 如果队列显然为空或持有同模节点，
     *    尝试将节点添加到waiters队列，等待完成（或取消）并返回匹配项。
     *
     * 2.如果队列显然包含等待项，并且此调用是互补模式, 尝试通过CAS'ing等待节点的item字段并将其出列，
     * 然后返回匹配项。
     *
     * 在每种情况下，一路上，检查并尝试代表其他停滞/缓慢的线程帮助推进头部和尾部。
     *
     * 循环以空检查开始，防止看到未初始化的头部或尾部值。 
     * 这在当前的SynchronousQueue中永远不会发生, 但如果来电者持有非易失性/最终参考资料给转介者。
      *无论如何，检查在这里是因为它在循环顶部放置了空检查, 
      *这通常比隐含穿插它们更快。
     */

        QNode s = null; // constructed/reused as needed
        boolean isData = (e != null);

        for (;;) {
            QNode t = tail;
            QNode h = head;
            // 根据源码注释，在SynchronousQueue中不会出现这两种情况，因为在构造函数中对head和tail初始化了
            if (t == null || h == null)         // saw uninitialized value 根据源码注释，在SynchronousQueue中不会出现这两种情况，因为在
                continue;                       // spin

            // 队列为空 或者 队列中线程与当前线程模式相同（队列中只存在一种模式的线程）
            if (h == t || t.isData == isData) { // empty or same-mode
                QNode tn = t.next;
                if (t != tail)                  // inconsistent read 被其他线程修改了tail指针，循环重新开始
                    continue;
                if (tn != null) {               // lagging tail 如果这个过程中又有新的线程插入队列，tail节点后又有新的节点，则修改tail指向新的节点
                    advanceTail(t, tn);
                    continue;
                }
                if (timed && nanos <= 0)        // can't wait 表示非阻塞操作，不等待，模式相同说明没有可匹配的线程，直接返回null
                    return null;
                if (s == null)
                    s = new QNode(e, isData);
                // CAS修改tail.next=s，即将节点s加入队列中;如果操作失败说明t.next!=null,有其他线程加入了节点，循环重新开始
                if (!t.casNext(null, s))        // failed to link in
                    continue;

                // 修改队列尾指针tail指向节点s;这里没有用CAS操作，因为即使设置失败，其他线程会帮忙修改tail指针
                advanceTail(t, s);              // swing tail and wait
                Object x = awaitFulfill(s, e, timed, nanos);    //线程挂起等待匹配
                if (x == s) {                   // wait was cancelled 如果线程被取消则从队列中清除，并且返回null
                    clean(t, s);
                    return null;
                }

                if (!s.isOffList()) {           // not already unlinked
                    // 注意这里传给advanceHead的参数是t，为什么？
                    // 因为节点t是节点s的前驱节点，执行到这里说明节点s代表的线程被唤醒得到匹配，所以唤醒的时候，节点s肯定是队列中第一个节点，前驱节点t是head指针
                    // 这里也并没有用for循环保证CAS操作成功，是因为唤醒线程s的线程也会执行advanceHead操作
                    advanceHead(t, s);          // unlink if head
                    if (x != null)              // and forget fields
                        s.item = s;
                    s.waiter = null;
                }
                return (x != null) ? (E)x : e;

            } else {                            // complementary-mode 如果为互补模式，则进行匹配
                QNode m = h.next;               // node to fulfill 公平模式，从队列头部开始匹配，由于head指向一个dummy节点，所以待匹配的节点为head.next
                // 如果出现以下三种情况之一，重新循环匹配：
                // (1)t != tail; tail指针被修改
                // (2)m == null; 待匹配的线程节点不存在
                // (3)h != head; head指针被修改,m节点被其他线程匹配成功了
                if (t != tail || m == null || h != head)
                    continue;                   // inconsistent read

                Object x = m.item;
                // 如果出现以下几种情况之一，则重新循环匹配：
                // (1)isData == (x != null); 线程的模式相同（都为生产者或者都为消费者），无法匹配
                // (2)x == m; 节点m代表的线程在等待被匹配过程中被取消了
                // (3)!m.casItem(x, e); CAS操作修改item失败
                if (isData == (x != null) ||    // m already fulfilled
                    x == m ||                   // m cancelled
                    !m.casItem(x, e)) {         // lost CAS
                    advanceHead(h, m);          // dequeue and retry
                    continue;
                }

                // 匹配成功，修改head指针指向下一节点;同时唤醒匹配成功的线程
                advanceHead(h, m);              // successfully fulfilled
                LockSupport.unpark(m.waiter);
                // x != null; 表示等待匹配的线程为生产者，则当前线程为消费者，返回生产者的数据x
                // x == null; 表示等待匹配的线程为消费者，则当前线程为生产者，当前线程返回自身的数据
                return (x != null) ? (E)x : e;
            }
        }
    }
```

```java
/**
 * Gets rid of cancelled node s with original predecessor pred.
 */
void clean(QNode pred, QNode s) {
    s.waiter = null; // forget thread
    /*
     * 在任何给定时间，列表中的一个节点都不能被删除 - 最后插入的节点。 为了适应这种情况，
     * 如果我们无法删除s，我们将其前身保存为“cleanMe”，
     * 首先删除先前保存的版本。 可以始终删除节点s或先前保存的节点中的至少一个，因此总是终止。
     */
       while (pred.next == s) { // Return early if already unlinked
             QNode h = head;
             QNode hn = h.next;   // Absorb cancelled first node as head
             // 从队列头部开始遍历，遇到被取消的节点则将其出队
             if (hn != null && hn.isCancelled()) {
                 advanceHead(h, hn);
                 continue;
             }
             QNode t = tail;      // Ensure consistent read for tail
             if (t == h)     // 表示队列为空
                 return;
             QNode tn = t.next;
             if (t != tail)
                 continue;
             if (tn != null) {     // 有新的节点加入，帮助其加入队列尾
                 advanceTail(t, tn);
                 continue;
             }
             // 如果被取消的节点s不是队尾节点，则修改其前驱节点的next指针，将节点s从队列中删除
             if (s != t) {        // If not tail, try to unsplice
                 QNode sn = s.next;
                 if (sn == s || pred.casNext(s, sn))
                     return;
             }
             // 如果s是队尾节点，则用cleanMe节点指向其前驱节点，等待以后s不是队尾时再从队列中清除
             // 如果 cleanMe == null，则直接将pred赋值给cleanMe即可
             // 否则，说明之前有一个节点等待被清除，并且用cleanMe指向了其前驱节点，所以现在需要将其从队列中清除
             QNode dp = cleanMe;
             if (dp != null) {    // Try unlinking previous cancelled node
                 QNode d = dp.next;
                 QNode dn;
                 // 以下四种情况下将cleanMe置为null，前面三种是特殊情况，最后一个是利用CAS操作正确的将待清楚的节点从队列中清除
                 // d == null; 表示待清除的节点不存在
                 // d == dp; dp被清除
                 // !d.isCancelled(); d没有被取消，说明哪里出错了
                 if (d == null ||               // d is gone or
                     d == dp ||                 // d is off list or
                     !d.isCancelled() ||        // d not cancelled or
                     (d != t &&                 // d not tail and
                      (dn = d.next) != null &&  //   has successor
                      dn != d &&                //   that is on list
                      dp.casNext(d, dn)))       // d unspliced
                     casCleanMe(dp, null);
                 if (dp == pred)     // 说明已经被其他线程更新了
                     return;      // s is already saved node
             } else if (casCleanMe(null, pred))
                 return;          // Postpone cleaning s
         }
     }
```

