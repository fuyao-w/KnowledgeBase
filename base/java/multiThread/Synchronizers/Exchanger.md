## Exchanger 

https://segmentfault.com/a/1190000015963932

线程可以在对中交换元素的同步点。每个线程在exchange方法的入口处提交一些对象 ，与伙伴线程匹配，并在返回时接收其伙伴的对象。交换器可以被视为`SynchronousQueue`的双向形式。交换器可能在遗传算法和管道设计等应用中很有用。

**示例用法：** 下面是一个类的亮点，它使用一个`Exchanger` 在线程之间交换缓冲区，以便填充缓冲区的线程在需要时获得一个刚刚清空的线程，将填充的线程切换到清空缓冲区的线程。

```java
 class FillAndEmpty {
   Exchanger<DataBuffer> exchanger = new Exchanger<>();
   DataBuffer initialEmptyBuffer = ... a made-up type
   DataBuffer initialFullBuffer = ...

   class FillingLoop implements Runnable {
     public void run() {
       DataBuffer currentBuffer = initialEmptyBuffer;
       try {
         while (currentBuffer != null) {
           addToBuffer(currentBuffer);
           if (currentBuffer.isFull())
             currentBuffer = exchanger.exchange(currentBuffer);
         }
       } catch (InterruptedException ex) { ... handle ... }
     }
   }

   class EmptyingLoop implements Runnable {
     public void run() {
       DataBuffer currentBuffer = initialFullBuffer;
       try {
         while (currentBuffer != null) {
           takeFromBuffer(currentBuffer);
           if (currentBuffer.isEmpty())
             currentBuffer = exchanger.exchange(currentBuffer);
         }
       } catch (InterruptedException ex) { ... handle ...}
     }
   }

   void start() {
     new Thread(new FillingLoop()).start();
     new Thread(new EmptyingLoop()).start();
   }
 }
```

内存一致性效果：对于通过a成功交换对象的每对线程，在每个线程 发生之前的`Exchanger`操作发生在从另一个线程中的相应线程返回之后的操作之前。`exchange()``exchange()`

### 分析

概述：对于交换“槽”，核心算法是具有项目的参与者（调用者）：

```java
  for (;;) {
    if (slot is empty) {                       // offer
      place item in a Node;
      if (can CAS slot from empty to node) {
        wait for release;
        return matching item in node;
      }
    }
    else if (can CAS slot from node to empty) { // release
      get the item in node;
      set matching item in node;
      release waiting thread;
    }
    // else retry on CAS failure
  }
 
 
```

这是“双重数据结构”中最简单的形式 
​     这种方式在原则上是高效的，但实际上，像许多在单个位置上进行原子更新的算法一样，当许多参与者使用同一个Exchanger时，存在严重的伸缩性问题。所以我们的实现通过引入一个消去数组从而安排不同的线程使用不同的slot来降低竞争，并且保证最终会成对交换数据。这意味着我们不能完全地控制线程之间的划分方式，但是我们通过在竞争激烈时增加arena的范围，竞争变少时减少arena的范围来分配线程可用的下标。我们为了达到这个效果，通过ThreadLocals来定义Node，以及在Node中包含了线程下标index以及相应的跟踪状态。（对于每个线程，我们可以重用私有的Node而不是重新创建一个，因为slot只会通过CAS操作交替地变化(Node VS null)，从而不会遇到ABA问题。当然，我们在使用时需要重新设置item）。



为了实现一个高效的arena(场地)，我们仅在探测到竞争时才开辟空间(当然在单CPU时我们什么都不做)，首先我们通过单个slot的slotExchange方法来交换数据，探测到竞争时，我们会通过arena安排不同位置的slot，并且保证没有slot会在同一个缓存行上(cache line)。因为当前没有办法确定缓存行的尺寸，我们使用对任何平台来说都足够大的值。并且我们通过其他手段来避免错误/非意愿的共享来增加局部性，比如对Node使用边距（via sun.misc.Contended），"bound"作为Exchanger的属性，以及使用区别于LockSupport的重新安排park/unpark的版本来工作。

一开始我们只使用一个slot，然后通过记录冲突次数从而扩展arena的尺寸（冲突：CAS在交换数据时失败）。对于以上算法来说，能够表明竞争的碰撞类型是两个线程尝试释放Node的冲突----线程竞争提供Node的情况是可以合理地失败的。当一个线程在当前arena的每个位置上都失败时，会试着去递增arena的尺寸。我们记录冲突的过程中会跟踪"bound”的值，以及会重新计算冲突次数在bound的值被改变时（当然，在冲突次数足够时会改变bound）。

当arena的可用尺寸大于1时，参与者会在等待一会儿之后在退出时递减arena的可用尺寸。这个“一会儿”的时间是经验决定的。我们通过借道使用spin->yield->block的方式来获得合理的等待性能--在一个繁忙的exchanger中，提供数据的线程经常是被立即释放的，相对应的多cpu环境中的上下文切换是非常缓慢/浪费的。arena上的等待去除了阻塞，取而代之的是取消。从经验上看所选择的自旋次数可以避免99%的阻塞时间，在一个极大的持久的交换速率以及一系列的计算机上。自旋以及退让产生了一些随机的特性（使用一种廉价的xorshift）从而避免了严格模式下会引起没必要的增长/收缩环。对于一个参与者来说，它会在slot已经改变的时候知道将要被唤醒，但是直到match被设置之后才能取得进展。在这段时间里它不能取消自己，所以只能通过自旋/退让来代替。注意：可以通过避免第二次检测，通过改变线性化点到对于match属性的CAS操作，当然这样做会增加一点异步性，以及牺牲了碰撞检测的效果和重用一线程一Node的能力。所以现在的方案更好。

检测到碰撞时，下标逆方向循环遍历arena，当bound改变时会以最大下标（这个位置Node是稀少的）重新开始遍历（当退出（取消）时，下标会收缩一半直到变0）。我们这里使用了单步来遍历，原因是我们的操作出现的极快在没有持续竞争的情况下，所以简单/快速的控制策略能够比精确但是缓慢的策略工作地更好。

因为我们使用了退出的方式来控制arena的可用尺寸，所以我们不能直接在限时版本中抛出TimeoutException异常直到arena的尺寸收缩到0（也就是说只要下标为0的位置可用）或者arena没有被激活。这可能对原定时间有延迟，但是还是可以接受的。

本质上来看所有的实现都存于方法slotExchange和arenaExchange中。这两个有相似的整体结构，但是在许多细节上不同。slotExchange方法使用单个"slot"属性相对于使用arena中的元素。同时它也需要极少的冲突检测去激活arena的构造（这里最糟糕的部分是确定中断状态以及InterruptedException异常在其他方法被调用时出现，这里是通过返回null来检查是否被中断.)

在这种类型的代码中，由于方法依赖的大部分逻辑所读取的变量是通过局部量来维持的，所以方法是大片的并且难以分解--这里主要是通过连在一起的 spin->yield->block/cancel 代码)。以及严重依赖于本身具有的Unsafe机制和内联的CAS操作和相关内存读取操作。注意Node.item不是volatile的，尽管它会被释放线程所读取，因为读取操作只会在CAS操作完成之后才发生，以及所有自己的变量都是以能被接受的次序被其他操作所使用。当然这里也可以使用CAS操作来用match，但是这样会减慢速度）。



### 字段

```java
/**
 *竞技场中任何两个使用过的插槽之间的索引距离（作为移位值），将它们隔开以避免错误共享。
 */
private static final int ASHIFT = 5;

/**
 * 支持的最大竞技场索引。 最大可分配竞技场大小为 MMASK + 1.必须是2的幂减1，小于（1 <<（31-ASHIFT））。
 * 255（0xff）的上限足以满足主算法的预期缩放限制。
 */
private static final int MMASK = 0xff;

/**
 * 绑定字段的序列/版本位的单位。对绑定的每次成功更改也会增加SEQ。
 */
private static final int SEQ = MMASK + 1;

/** 用于调整大小和旋转控制的CPU数量 */
private static final int NCPU = Runtime.getRuntime().availableProcessors();

/**
 * 竞技场的最大时隙索引：原则上可以保持所有线程没有争用的时隙数，
 * 或者最多可以保留最大可索引值。
 */
static final int FULL = (NCPU >= (MMASK << 1)) ? MMASK : NCPU >>> 1;

/**等待比赛时旋转的界限。 实际的迭代次数平均约为随机化的两倍。注意：当NCPU == 1时，禁用旋转。
 */
private static final int SPINS = 1 << 10;

/**
 * 表示公共方法的null参数/返回值。需要，因为API最初不会禁止空参数，它应该具有。
 */
private static final Object NULL_ITEM = new Object();

/**
 * 内部交换方法在超时时返回的Sentinel值，以避免需要这些方法的单独定时版本。
 */
private static final Object TIMED_OUT = new Object();
```

```java
/**
 * 节点保存部分交换的数据，以及其他每线程簿记。 通过@Contended填充以减少内存争用。
 */
@jdk.internal.vm.annotation.Contended static final class Node {
    int index;              // Arena index
    int bound;              // Exchanger.bound的最后记录值
    int collides;           // 当前界限的CAS故障数
    int hash;               // 旋转的伪随机
    Object item;            // This thread's current item
    volatile Object match;  // 通过发布线程提供的项目
    volatile Thread parked; // park时设置为此线程，否则为null
}
```

```java
/** 相应的线程本地类 */
static final class Participant extends ThreadLocal<Node> {
    public Node initialValue() { return new Node(); }
}

/**
 * Per-thread state.
 */
private final Participant participant;

/**
 * 消除阵列; null，直到启用（在slotExchange内）。
 * 元素访问使用volatile gets和CAS的模拟。
 */
private volatile Node[] arena;

/**
 *使用插槽直到检测到争用。
 */
private volatile Node slot;

/**
 * 最大有效竞技场位置的索引，与高位的SEQ号进行“或”运算，在每次更新时递增。
  * 从0到SEQ的初始更新用于确保竞技场阵列仅构造一次。
 */
private volatile int bound;
```

#### exchange

```java
public V exchange(V x) throws InterruptedException {
    Object v;
    Node[] a;
    Object item = (x == null) ? NULL_ITEM : x; // translate null args
    if (((a = arena) != null ||
         (v = slotExchange(item, false, 0L)) == null) &&
        ((Thread.interrupted() || // disambiguates null return
          (v = arenaExchange(item, false, 0L)) == null)))
        throw new InterruptedException();
    return (v == NULL_ITEM) ? null : (V)v;
}
```

`exchange`在线程比较少的情况下，调用`slotExchange`,在线程比较多的情况下调用`arenaExchange`。

```java
private final Object slotExchange(Object item, boolean timed, long ns) {
    Node p = participant.get();
    Thread t = Thread.currentThread();
    if (t.isInterrupted()) // preserve interrupt status so caller can recheck
        return null;

    for (Node q;;) {
        if ((q = slot) != null) {
            if (SLOT.compareAndSet(this, q, null)) {
                Object v = q.item;
                q.match = item;
                Thread w = q.parked;
                if (w != null)
                    LockSupport.unpark(w);
                return v;
            }
            // create arena on contention, but continue until slot null
            if (NCPU > 1 && bound == 0 &&
                BOUND.compareAndSet(this, 0, SEQ))
                arena = new Node[(FULL + 2) << ASHIFT];
        }
        else if (arena != null)
            return null; // caller must reroute to arenaExchange
        else {
            p.item = item;
            if (SLOT.compareAndSet(this, null, p))
                break;
            p.item = null;
        }
    }

    // await release
    int h = p.hash;
    long end = timed ? System.nanoTime() + ns : 0L;
    int spins = (NCPU > 1) ? SPINS : 1;
    Object v;
    while ((v = p.match) == null) {
        if (spins > 0) {
            h ^= h << 1; h ^= h >>> 3; h ^= h << 10;
            if (h == 0)
                h = SPINS | (int)t.getId();
            else if (h < 0 && (--spins & ((SPINS >>> 1) - 1)) == 0)
                Thread.yield();
        }
        else if (slot != p)
            spins = SPINS;
        else if (!t.isInterrupted() && arena == null &&
                 (!timed || (ns = end - System.nanoTime()) > 0L)) {
            p.parked = t;
            if (slot == p) {
                if (ns == 0L)
                    LockSupport.park(this);
                else
                    LockSupport.parkNanos(this, ns);
            }
            p.parked = null;
        }
        else if (SLOT.compareAndSet(this, p, null)) {
            v = timed && ns <= 0L && !t.isInterrupted() ? TIMED_OUT : null;
            break;
        }
    }
    MATCH.setRelease(p, null);
    p.item = null;
    p.hash = h;
    return v;
}
```

`participant`为每个线程提供唯一的`Node`，`Node`中保存了两个线程之间交换的item的相关信息，在开始检查当前线程是否被中断，如果被中断则返回null，由外层抛出异常。

交换元素的逻辑在一个自旋里，如果交换槽不为null，说明其他线程已经将要交换的信息准备好，需要现将自己的交换槽通过CAS清空。然后取出交换槽里其他线程交换的item，将自己需要交换的item放到自己交换槽的`match`字段中，并唤醒与其交换信息的被阻塞的线程。

如果CAS将交换槽清空失败，则说明当前有其他线程参与竞争，需要创建一个竞技场也就是Node数组（数组的长度会根据CPU的数量进行优化）。但是依然会尝试从原来的槽中取item,知道交换槽被其他线程取走置null。

如果交换操为null，但是竞技场（Node数组）不为null，则直接返回null，交给`arenaExchange`从竞技场中取出。

如果交换槽和竞技场都为null，则说明还没有其他线程发起交换，那么自己可以将交换信息先放到槽中。

首先将当前线程的交换item赋值给当前线程专属的Node节点然后通过CAS赋值将Node给交换槽，然后退出自旋，进行后续操作。

接下来通过spin->yield->block策略（先自旋一定时间，如果自旋到期限交换槽变了，就重新自旋，否则阻塞）等待`match`被其他线程填充,进行等待以减少线程切换带来的开销。如果超时或者获取到`match`最后会调用`varHandle`的`setRelease`将match清空(保证在次之后其他线程看到的都是更新后的值)，最后返回item。

### arenaExchange

```java
private final Object arenaExchange(Object item, boolean timed, long ns) {
    Node[] a = arena;
    int alen = a.length;
    Node p = participant.get();
    for (int i = p.index;;) {                      // access slot at i
        int b, m, c;
        int j = (i << ASHIFT) + ((1 << ASHIFT) - 1);
        if (j < 0 || j >= alen)
            j = alen - 1;
        Node q = (Node)AA.getAcquire(a, j);
        if (q != null && AA.compareAndSet(a, j, q, null)) {
            Object v = q.item;                     // release
            q.match = item;
            Thread w = q.parked;
            if (w != null)
                LockSupport.unpark(w);
            return v;
        }
        else if (i <= (m = (b = bound) & MMASK) && q == null) {
            p.item = item;                         // offer
            if (AA.compareAndSet(a, j, null, p)) {
                long end = (timed && m == 0) ? System.nanoTime() + ns : 0L;
                Thread t = Thread.currentThread(); // wait
                for (int h = p.hash, spins = SPINS;;) {
                    Object v = p.match;
                    if (v != null) {
                        MATCH.setRelease(p, null);
                        p.item = null;             // clear for next use
                        p.hash = h;
                        return v;
                    }
                    else if (spins > 0) {
                        h ^= h << 1; h ^= h >>> 3; h ^= h << 10; // xorshift
                        if (h == 0)                // initialize hash
                            h = SPINS | (int)t.getId();
                        else if (h < 0 &&          // approx 50% true
                                 (--spins & ((SPINS >>> 1) - 1)) == 0)
                            Thread.yield();        // two yields per wait
                    }
                    else if (AA.getAcquire(a, j) != p)
                        spins = SPINS;       // releaser hasn't set match yet
                    else if (!t.isInterrupted() && m == 0 &&
                             (!timed ||
                              (ns = end - System.nanoTime()) > 0L)) {
                        p.parked = t;              // minimize window
                        if (AA.getAcquire(a, j) == p) {
                            if (ns == 0L)
                                LockSupport.park(this);
                            else
                                LockSupport.parkNanos(this, ns);
                        }
                        p.parked = null;
                    }
                    else if (AA.getAcquire(a, j) == p &&
                             AA.compareAndSet(a, j, p, null)) {
                        if (m != 0)                // try to shrink
                            BOUND.compareAndSet(this, b, b + SEQ - 1);
                        p.item = null;
                        p.hash = h;
                        i = p.index >>>= 1;        // descend
                        if (Thread.interrupted())
                            return null;
                        if (timed && m == 0 && ns <= 0L)
                            return TIMED_OUT;
                        break;                     // expired; restart
                    }
                }
            }
            else
                p.item = null;                     // clear offer
        }
        else {
            if (p.bound != b) {                    // stale; reset
                p.bound = b;
                p.collides = 0;
                i = (i != m || m == 0) ? m : m - 1;
            }
            else if ((c = p.collides) < m || m == FULL ||
                     !BOUND.compareAndSet(this, b, b + SEQ + 1)) {
                p.collides = c + 1;
                i = (i == 0) ? m : i - 1;          // cyclically traverse
            }
            else
                i = m + 1;                         // grow
            p.index = i;
        }
    }
}
```

`arenaExchange`将交换槽变成了一个数组，初始默认在0索引位置尝试交换，交换成功 直接返回，否则在index位置上尝试，并且根据collides（竞争失败次数）进行扩容。还有相应的收缩策略，知道超时或占领成功。

