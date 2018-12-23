## Exchanger 

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
 
  这是“双重数据结构”中最简单的形式 - 
```

       请参阅Scott和Scherer的DISC 04论文和
​      http://www.cs.rochester.edu/research/synchronization/pseudocode/duals.html
​     
​      这在原则上很有效。但实际上，像许多以单个位置的原子更新为中心的算法一样，当有多个参与者使用同一个Exchange时，它会出现可怕的扩展。因此，实现使用消除竞争的形式，通过安排一些线程通常使用不同的插槽来扩展这种争用，同时仍然确保最终，任何两方都能够交换项目。也就是说，我们不能完全跨线程进行分区，而是提供线程竞技场索引，这些索引平均会在争用下增长并在缺乏争用的情况下收缩。我们通过定义我们需要的节点作为ThreadLocals来处理这个问题，并在每个线程索引和相关的簿记状态中包含它们。 （我们可以安全地重复使用每个线程节点，而不是每次都创建它们，因为插槽在指向节点与null之间交替，因此不会遇到ABA问题。但是，我们需要在使用之间重新设置它们。）
​     

实现一个有效的舞台需要分配一堆空间，所以我们只有在检测到争用时才这样做（除了在单处理器上，它们没有帮助，所以不使用）。 否则，交换使用单槽slotExchange方法。 在争用时，不仅槽必须位于不同的位置，而且由于位于相同的高速缓存线（或更一般地，相同的相干单元），所以位置不得遇到存储器争用。 因为在撰写本文时，无法确定缓存行大小，因此我们定义了一个足以满足常见平台的值。 此外，在其他地方采取额外的谨慎措施以避免其他错误/无意的共享并增强局部性，包括向节点添加填充（通过@Contended），将“bound”嵌入为Exchanger字段。

​	竞技场开始只有一个用过的插槽。 我们通过跟踪碰撞来扩展有效的竞技场规模; 即尝试交换时失败的CAS。 根据上述算法的性质，可靠地指示争用的唯一类型的冲突是当两个尝试的释放冲突时 - 两个尝试的提议中的一个可以合法地失败到CAS而不指示多于一个其他线程的争用。 （注意：通过在CAS失败后读取插槽值来更准确地检测争用是可能但不值得的。）当线程在当前竞技场界限内的每个时隙发生冲突时，它会尝试将竞技场大小扩展一。 我们通过在“绑定”字段上使用版本（序列）编号来跟踪边界内的碰撞，并在参与者注意到绑定已更新（在任一方向上）时保守地重置碰撞计数。

​	有效的竞技场大小减少（当有超过一段时间后，放弃等待一段时间，然后尝试
在到期时递减竞技场大小。 “一阵子”的价值是一个经验问题。我们通过捎带来实施
使用spin-> yield-> block是合理的必要条件无论如何等待表现 - 在繁忙的交换机，优惠
通常几乎立即释放，在这种情况下上下文打开多处理器是非常缓慢/浪费的。竞技场
等待只是省略阻止部分，而是取消。旋转count根据经验选择为避免阻塞的值在最大持续汇率下的99％的时间各种试验机。旋转和产量需要一些限制随机性（使用便宜的xorshift）来避免规则的模式这会导致非生产性的生长/收缩周期。 （用一个伪随机也有助于规范旋转周期的持续时间使分支变得不可预测。）另外，在报价期间，a 服务员可以“知道”当它的插槽有时它会被释放已更改，但在设置匹配之前无法继续。在里面平均时间它不能取消报价，所以反而旋转/收益。注意：可以通过更改来避免这种二次检查线性化点是匹配字段的CAS（如已完成在Scott＆Scherer DISC论文中的一个案例中，也是如此稍微增加异步，以牺牲较差的碰撞为代价检测并且无法始终重用每线程节点。所以
目前的方案通常是一个更好的权衡。
      

​	在碰撞中，索引以相反的顺序循环地遍历竞技场，当边界改变时，在最大索引处重新开始（这将倾向于最稀疏）。 （在到期时，索引会减半，直到达到0.）有可能（并且已经尝试过）使用随机化，素数值步进或双哈希样式遍历而不是简单的循环遍历来减少聚束。 但凭经验，这些可能带来的任何好处都无法克服其增加的开销：除非存在持续争用，否则我们正在快速管理运行，因此更简单/更快速的控制策略比更准确但更慢的控制策略更好。
因为我们对竞技场大小控制使用到期，所以我们不能在公共交换方法的定时版本中抛出TimeoutExceptions，直到竞技场大小缩小为零（或竞技场未启用）。 这可能会延迟对超时的响应，但仍然在规范内。   
基本上所有的实现都在slotExchange和arenaExchange方法中。 它们具有相似的整体结构，但在太多细节上有所不同。 slotExchange方法使用单个Exchanger字段“slot”而不是arena数组元素。 然而，它仍然需要最小的碰撞检测来触发竞技场构造。 （最混乱的部分是确保中断状态和InterruptedExceptions在转换期间可以调用两种方法。这是通过使用null return作为重新检查中断状态的标记来完成的。）

​	在这种代码中太常见了，方法是单片的，因为大多数逻辑依赖于作为局部变量维护的字段的读取，因此不能很好地考虑因素 - 主要是，这里，庞大的spin-> yield-> block /取消代码。 请注意，即使通过释放线程读取字段Node.item也不会将其声明为volatile，因为它们仅在必须在访问之前的CAS操作之后执行此操作，并且拥有线程的所有使用在其他操作中以其他方式可接受地排序。 （因为实际的原子点是插槽CAS，在一个版本中写入Node.match比完全易失性写入更弱也是合法的。但是，这样做不成功，因为它可能允许进一步推迟写入 ，推迟进步。）

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

