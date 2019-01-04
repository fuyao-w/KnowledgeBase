## ConcurrentHashMap

```java
public class ConcurrentHashMap<K,V> extends AbstractMap<K,V>
    implements ConcurrentMap<K,V>, Serializable 
```

### java doc

一个哈希表，支持完全并发检索的和高预期并发性的更新。该类遵循与Hashtable相同的功能规范，并包括与Hashtable的每个方法相对应的方法版本。但是，即使所有操作都是线程安全的，检索操作也不需要锁定，并且不支持以阻塞所有访问的方式锁定整个表。在依赖于线程安全但不依赖于其同步细节的程序中，此类可与Hashtable完全互操作。
检索操作（包括get）通常不会阻塞，因此可能与更新操作（包括put和remove）重叠。检索反映了最近完成的更新操作的结果。 （更正式地说，给定密钥的更新操作承担与报告更新值的该密钥的任何（非空）检索之前发生的关系。）对于诸如putAll和clear之类的聚合操作，并发检索可能反映插入或删除只有一些条目。类似地，Iterators，Spliterators和Enumerations在迭代器/枚举的创建时或之后的某个时刻返回反映哈希表状态的元素。它们不会抛出ConcurrentModificationException。但是，迭代器设计为一次只能由一个线程使用。请记住，聚合状态方法（包括size，isEmpty和containsValue）的结果通常仅在映射未在其他线程中进行并发更新时才有用。否则，这些方法的结果反映了可能足以用于监视或估计目的的瞬态，但不适用于程序控制。

当存在太多冲突时（即，具有不同散列码但是落入与表大小模数相同的槽的密钥），该表被动态扩展，具有每个映射大致保持两个bins 的预期平均效果（对应于0.75负载）调整大小的因子阈值）。随着映射的添加和删除，这个平均值可能会有很大的差异，但总的来说，这维持了哈希表的普遍接受的时间/空间权衡。但是，调整此大小或任何其他类型的散列表可能是一个相对较慢的操作。如果可能，最好将大小估计值作为可选的initialCapacity构造函数参数提供。另一个可选的loadFactor构造函数参数通过指定在计算给定数量的元素时要分配的空间量时使用的表密度，提供了另一种自定义初始表容量的方法。此外，为了与此类的先前版本兼容，构造函数可以选择将预期的concurrencyLevel指定为内部大小调整的附加提示。请注意，使用具有完全相同hashCode（）的许多键是减慢任何哈希表性能的可靠方法。为了改善影响，当键是可比较时，该类可以使用键之间的比较顺序来帮助打破关系。

可以创建ConcurrentHashMap的Set投影（使用newKeySet（）或newKeySet（int）），或者只查看键时使用keySet（Object），并且映射的值（可能是暂时的）不使用或者全部采用相同的映射值。

通过使用LongAdder值并通过computeIfAbsent初始化，ConcurrentHashMap可用作可伸缩频率映射（直方图或多集的形式）。例如，要向ConcurrentHashMap <String，LongAdder> freqs添加计数，可以使用freqs.computeIfAbsent（key，k  - > new LongAdder（））。increment（）;

此类及其视图和迭代器实现了Map和Iterator接口的所有可选方法。

与Hashtable类似，但与HashMap不同，此类不允许将null用作键或值。

ConcurrentHashMaps支持一组顺序和并行批量操作，与大多数Stream方法不同，它们被设计为安全且通常合理地应用，即使是由其他线程同时更新的映射;例如，在共享注册表中计算值的快照摘要时。有三种操作，每种操作有四种形式，接受带有键，值，条目和（键，值）对的函数作为参数和/或返回值。因为ConcurrentHashMap的元素没有以任何特定的方式排序，并且可以在不同的并行执行中以不同的顺序处理，所提供的函数的正确性不应该依赖于任何排序，或者可能依赖于任何其他可能瞬时变化的对象或值。计算正在进行中;除了forEach动作外，理想情况下应该是无副作用的。 Map.Entry对象上的批量操作不支持方法setValue。

- forEach：对每个元素执行给定的操作。 变量形式在执行操作之前对每个元素应用给定的变换。
- search：返回在每个元素上应用给定函数的第一个可用的非null结果; 在找到结果时跳过进一步搜索。
- reduce：累积每个元素。 提供的缩减功能不能依赖于排序（更正式地说，它应该是关联的和可交换的）。 有五种变体：
- 简单减少。 （对于（key，value）函数参数，没有这种方法的形式，因为没有相应的返回类型。）
- 映射缩减，累积应用于每个元素的给定函数的结果。
- 使用给定的基值减少标量的双精度，长数和整数。

这些批量操作接受parallelismThreshold参数。如果估计当前map大小小于给定阈值，则方法顺序进行。使用Long.MAX_VALUE值可以抑制所有并行性。使用值1可通过划分为足够的子任务来充分利用用于所有并行计算的ForkJoinPool.commonPool（），从而实现最大并行度。通常，您最初会选择其中一个极值，然后测量使用中间值的性能，这些值会影响开销与吞吐量之间的差异。

批量操作的并发属性遵循ConcurrentHashMap的并发属性：从get（key）返回的任何非null结果和相关的访问方法都与相关的插入或更新有关。任何批量操作的结果都反映了这些每元素关系的组成（但不一定是整个地图的原子，除非它以某种方式被称为静止）。相反，因为映射中的键和值永远不为null，所以null可以作为当前缺少任何结果的可靠原子指示符。为了维护此属性，null用作所有非标量缩减操作的隐式基础。对于double，long和int版本，基础应该是一个，当与任何其他值组合时，返回其他值（更正式地说，它应该是减少的标识元素）。最常见的减少具有这些特性;例如，用基数MAX_VALUE计算基数为0或最小值的和。

作为参数提供的搜索和转换函数应该类似地返回null以指示缺少任何结果（在这种情况下不使用它）。在映射缩减的情况下，这也使变换能够用作过滤器，如果不应该组合元素，则返回null（或者，如果是原始特化，则返回标识基础）。您可以在搜索或减少操作中使用它们之前，通过在“null表示现在没有任何内容”规则下自己编写复合变换和过滤来创建复合变换和过滤。

接受和/或返回Entry参数的方法维护键值关联。例如，当找到具有最大价值的密钥时，它们可能是有用的。请注意，可以使用新的AbstractMap.SimpleEntry（k，v）提供“plain”Entry参数。

批量操作可能会突然完成，抛出在应用函数中遇到的异常。在处理此类异常时请记住，其他并发执行的函数也可能抛出异常，或者如果没有发生第一个异常，则会这样做。

与顺序形式相比，并行加速是常见的，但不能保证。如果并行计算的基础工作比计算本身更昂贵，则涉及小map上的简短函数的并行操作可能比顺序形式执行得更慢。类似地，如果所有处理器忙于执行不相关的任务，则并行化可能不会导致太多实际的并行性。

所有任务方法的所有参数都必须为非null。

### 分析

​	此哈希表的主要设计目标是在最小化更新争用的同时保持并发可读性（通常是方法get（），但也包括迭代器和相关方法）。次要目标是使空间消耗与java.util.HashMap保持大致相同或更好，并支持许多线程在空表上的高初始插入速率。
 	此映射通常用作分箱（bin）（分区）哈希表。每个键值映射都保存在Node中。大多数Node是具有hash，key，value和next字段的基本Node类的实例。但是，存在各种子类：TreeNodes排列在平衡树中，而不是数组中。 TreeBins拥有TreeNodes集的根。在调整大小期间，ForwardingNodes被放置在bins的头部。在computeIfAbsent和相关方法中建立值时，ReservationNodes用作占位符。 TreeBin，ForwardingNode和ReservationNode类型不包含普通用户的 keys，values或hashes，并且在搜索等过程中很容易区分，因为它们具有负hash字段以及空键和值字段。 （这些特殊节点要么不常见，要么是瞬态的，因此携带一些未使用的字段的影响是微不足道的。）

​	数组被赖加载为一个二次幂的大小,第一次插入。表中的每个bin通常包含一个Node链表（最常见的是，链表只有零个或一个节点）。链表表访问需要 volatile/atomic 读取，writes和CASes。因为在没有进一步间接的情况下没有其他方安排这个，我们使用内置函数（jdk.internal.misc.Unsafe）操作。
​	我们使用节点哈希字段的高（符号）位用于控制目的 - 由于寻址约束，它仍然可用。具有负散列字段的节点在map方法中被特殊处理或忽略。
​	第一个节点在空箱中的插入（通过put或其变体）通过将其CAS到bin中来执行。到目前为止，这是大多数键/哈希分配下的put操作的最常见情况。其他更新操作（插入，删除和替换）需要锁定。我们不想浪费将不同的锁对象与每个bin关联所需的空间，因此使用bin列表本身的第一个节点作为锁。锁定对这些锁的支持依赖于内置的“synchronized”监视器。

​	Using the first node of a list as a lock does not by itself suffice though:：当一个节点被锁定时，任何更新必须首先验证它在锁定后仍然是第一个节点，如果没有则重试。由于新节点始终附加到链表中，因此一旦Node首先位于bin中，它将一直保留，直到删除或bin变为无效（调整大小时）。
​	每个bin锁的主要缺点是，由同一个锁保护的bin列表中的其他节点上的其他更新操作可能会被阻塞，例如当用户 `equals（）`或`mapping`函数需要很长时间时。但是，统计上，在随机哈希码下，这不是常见问题。理想情况下，箱中节点的频率遵循泊松分布
在给定调整阈值0.75的情况下平均约为0.5的参数，尽管由于调整粒度而具有较大的方差。忽略方差，列表大小k的预期出现是`（exp（-0.5）pow（0.5，k）/ factorial（k））`。第一个值是：
​     

      0:    0.60653066
      1:    0.30326533
      2:    0.07581633
      3:    0.01263606
      4:    0.00157952
      5:    0.00015795
      6:    0.00001316
      7:    0.00000094
      8:    0.00000006
      more: less than 1 in ten million
​	在随机哈希下，访问不同元素的两个线程的锁争用概率大约是1 /（8 #elements）。
 	在实践中遇到的实际哈希码分布有时会明显偏离均匀随机性。这包括当`N>（1 << 30）`时的情况，因此一些键一定会碰撞。类似地，对于dumb或hostile用法，不同的多个密钥被设计为具有相同的哈希码或仅在掩蔽的高位中不同的哈希码。因此，我们使用二进制策略，当bin中的节点数超过阈值时应用该策略。这些TreeBins使用平衡树来保存节点（一种特殊形式的红黑树），将搜索时间限制为O（log N）。 TreeBin中的每个搜索步骤至少是常规列表中的两倍，但是假设N不能超过（1 << 64）（在用完地址之前），则将搜索步骤，锁定保持时间等限制为合理常量（每个操作最坏情况下检查大约100个节点），只要键是可比较的（这是非常常见的 - 字符串，长整数等）。 TreeBin节点（TreeNodes）也保持与常规节点相同的“下一个”遍历指针，因此可以以相同的方式遍历迭代器。
​	当占用率超过百分比阈值（名义上为0.75，但见下文）时，表格会调整大小。 在启动线程分配和设置替换数组之后，任何注意到需要扩容的线程都可以帮助调整大小。但是，这些其他线程可能会继续进行插入等操作，而不是拖延。使用TreeBins可以防止在调整大小过程中出现溢出的最坏情况。通过将bin一个接一个地从原数组转移到下一个新数组来调整数组大小。但是，线程要求在执行此操作之前传输小块索引（通过字段transferIndex），从而减少争用。字段sizeCtl中的生成标记可确保不会重叠绑定。因为我们使用的是二次幂扩展，所以每个bin的元素必须保持相同的索引，或者以两个偏移的幂移动。我们通过捕获旧节点可以重用的情况来消除不必要的节点创建，因为它们的下一个字段不会改变。平均而言，当表翻倍时，只有大约六分之一的节点需要克隆。一旦它们不再被可能同时遍历表中的任何reader 线程引用，它们替换的节点将是垃圾收集的。在transfer时，旧表bin仅包含特殊转发节点（具有hash字段“MOVED”），其包含下一个table作为其密钥。遇到转发节点时，使用新表重新启动访问和更新操作。

​	每个bin的transfer都需要其bin锁定，这可能会在调整大小时停止等待锁定。但是，因为其他线程可以加入并帮助调整大小而不是争用锁，所以随着调整大小的进行，平均聚合等待会变短。transfer操作还必须确保旧表和新表中的所有可访问的bin都可以被任何遍历使用。这部分是通过从最后一个bin（table.length  -  1）向前到第一个bin来安排的。在看到转发节点时，遍历（请参阅类Traverser）安排移动到新表而不重新访问节点。为了确保即使在不按顺序移动时也不会跳过中间节点，在遍历期间首次遇到转发节点时会创建堆栈（请参阅类TableStack），以便在以后处理当前表时保持其位置。对这些保存/恢复机制的需求相对较少，但是当遇到一个转发节点时，通常会有更多转发节点。
 所以Traversers使用一个简单的缓存方案来避免创建这么多新的TableStack节点。
​	遍历方案也适用于部分遍历bin的范围（通过备用的Traverser构造函数）来支持分区聚合操作。此外，如果转发到空表，则只读操作会放弃，这表示对当前未实现的关闭样式清除提供支持。
​	延迟表初始化在首次使用之前最小化占用空间，并且当第一个操作来自`putAll`，带有map参数的构造函数或反序列化时，也避免了重新绑定。这些情况试图覆盖初始容量设置，但在竞争的情况下无害地无法生效。

​	使用LongAdder的序列化来维护元素计数。我们需要结合专门化而不是仅使用LongAdder来访问隐式争用感知，从而导致创建多个CounterCell。计数器机制可以避免对更新进行争用，但如果在并发访问期间过于频繁地读取，则会遇到缓存抖动。为了避免经常阅读，仅在添加到已经存在两个或更多节点的bin时才尝试在争用下调整大小。在统一的哈希分布下，这种情况发生在阈值时大约为13％，这意味着只有大约1/8的检查阈值（并且在调整大小之后，很少会这样做）。
​	TreeBins对搜索和相关操作使用特殊形式的比较（这是我们不能使用现有集合（如TreeMaps）的主要原因）。 TreeBins包含Comparable元素，但可能包含其他元素，以及可比较的元素，但不一定对于相同的 `T` 可比较，因此我们不能在它们之间调用compareTo。要处理此问题，树主要按哈希值排序，然后按`Comparable.compareTo`顺序排序（如果适用）。在节点上查找时，如果元素不可比较或比较为0，则在绑定的哈希值的情况下可能需要搜索左和右子节点。 （这对应于完整列表搜索，如果所有元素都是不可比较的并且具有绑定的哈希值，则必须进行完整列表搜索。）在插入时，为了保持重新排序的总排序（或者在此处需要尽可能接近），我们比较类和identityHashCodes作为打破者。红黑平衡代码从pre-jdk-collections更新
 （http://gee.cs.oswego.edu/dl/classes/collections/RBCell.java）依次是Cormen，Leiserson和Rivest“算法导论”（CLR）。

​	TreeBins还需要额外的锁定机制。尽管在更新期间readers总是可以进行列表遍历，但是树遍历不是，主要是因为可以改变根节点和/或其链接的树旋转。 TreeBins包含一个寄生在主bin同步策略上的简单读写锁定机制：与插入或删除相关的结构调整已经被bin锁定（因此不能与其他writers冲突），但必须等待正在进行的reder完成。由于只有一个这样的waiter，我们使用一个简单的方案，使用一个“waiter”字段来阻塞writer。但是，reader永远不会阻塞。如果保持根锁，则它们沿慢速遍历路径（通过下一个指针）继续，直到锁变为可用或列表耗尽为止，以先到者为准。这些情况并不快，但最大化了预期的总吞吐量。

​	维护API和序列化与此类以前版本的兼容性引入了一些奇怪的问题。 主要是：我们保留未引用但未使用的构造函数参数，引用concurrencyLevel。 我们接受一个loadFactor构造函数参数，但只将其应用于初始表容量（这是我们唯一可以保证遵守它的时间。）我们还声明了一个未使用的“Segment”类，只有在序列化时才会以最小的形式实例化。
 	此外，仅为了与此类的先前版本兼容，它扩展了AbstractMap，即使它的所有方法都被覆盖，因此它只是无用的包袱。
​	这个文件被组织起来，使得阅读时的内容比其他方式更容易：首先是主要的静态声明和实用程序，然后是字段，然后是主要的公共方法（将多个公共方法的几个因子分解为内部方法），然后调整大小 方法，树，遍历和批量操作。



```java
    /**
     * The largest possible table capacity.  This value must be
     * exactly 1<<30 to stay within Java array allocation and indexing
     * bounds for power of two table sizes, and is further required
     * because the top two bits of 32bit hash fields are used for
     * control purposes.
     */
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * 默认的初始表容量。 必须是2的幂（即，至少1）并且最多为MAXIMUM_CAPACITY。
     */
    private static final int DEFAULT_CAPACITY = 16;

    /**
     * 最大可能（非幂2）阵列大小。 需要使用Array和相关方法。
     */
    static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /**
     * 此表的默认并发级别。 未使用但定义为与此类的先前版本兼容。
     */
    private static final int DEFAULT_CONCURRENCY_LEVEL = 16;

    /**
     *此表的加载因子。 构造函数中此值的覆盖仅影响初始表容量。 通常不使用实际浮点值
      *  - 使用诸如{@code n  - （n >>> 2）}之类的表达式来简化相关的大小调整阈值。
     */
    private static final float LOAD_FACTOR = 0.75f;

    /**
     * bin计数阈值，用于使用树而不是bin的列表。 将元素添加到具有至少这么多节点的bin时，bin被转换为
     * 树。 该值必须大于2，并且应该至少为8以与树木移除中的假设相关联，以便在收缩时转换回普通bin。
     */
    static final int TREEIFY_THRESHOLD = 8;

    /**
     * 用于在调整大小操作期间解除（拆分）bin的bin计数阈值。 应小于TREEIFY_THRESHOLD，
     * 最多6个与去除时的收缩检测网格。
     */
    static final int UNTREEIFY_THRESHOLD = 6;

    /**
     *容器可以树化的最小表容量。 （否则，如果bin中的节点太多，则会调整表的大小。）该值应至少为4 * TREEIFY_THRESHOLD，以避免调整大小和树化阈值之间的冲突。
     */
    static final int MIN_TREEIFY_CAPACITY = 64;

    /**
     *每次转移步骤的最小rebinnings次数。 范围细分为允许多个resizer 线程。
     *此值用作下限以避免resizer遇到过多的内存争用。 该值至少应为DEFAULT_CAPACITY。
     */
    private static final int MIN_TRANSFER_STRIDE = 16;

    /**
     * sizeCtl中用于生成戳记的位数。 32位阵列必须至少为6。
     */
    private static final int RESIZE_STAMP_BITS = 16;

    /**
     * 可以帮助调整大小的最大线程数。 必须适合32  -  RESIZE_STAMP_BITS位。
     */
    private static final int MAX_RESIZERS = (1 << (32 - RESIZE_STAMP_BITS)) - 1;

    /**
     * 记录大小标记的位移在sizeCtl中。
     */
    private static final int RESIZE_STAMP_SHIFT = 32 - RESIZE_STAMP_BITS;

    /*
     * 节点哈希字段的编码。见上文的解释。
     */
    static final int MOVED     = -1; // hash for forwarding nodes
    static final int TREEBIN   = -2; // hash for roots of trees
    static final int RESERVED  = -3; // hash for transient reservations
    static final int HASH_BITS = 0x7fffffff; // usable bits of normal node hash

    /** Number of CPUS, to place bounds on some sizings */
    static final int NCPU = Runtime.getRuntime().availableProcessors();
    
    /**
     * 箱子阵列。懒惰在第一次插入时初始化。
     *尺寸始终是2的力量。由迭代器直接访问。
     */
    transient volatile Node<K,V>[] table;
    
    /**
     * The next table to use; non-null only while resizing.
     */
    private transient volatile Node<K,V>[] nextTable;
    
    /**
     * 基本计数器值，主要在没有争用时使用，但也作为表初始化竞争期间的后备。 通过CAS更新。
     */
    private transient volatile long baseCount;
    
    /**
     * 表初始化和调整大小控制。 当为负数时，表正在初始化或调整大小：-1表示初始化，
     * 否则 - （1 +活动的调整线程数）。 否则，当table为null时，
     * 保留要在创建时使用的初始表大小，或者默认为0。 初始化之后，
     * 保存下一个元素计数值，在该值上调整表的大小。
     */
    private transient volatile int sizeCtl;
    
    /**
     * 调整大小时要分割的下一个表索引（加一）。
     */
    private transient volatile int transferIndex;
    
    /**
     *调整大小和/或创建CounterCell时使用的Spinlock（通过CAS锁定）。
     */
    private transient volatile int cellsBusy;
    
    /**
     * 计数器表。当非null时，size是2的幂。
     */
    private transient volatile CounterCell[] counterCells;
```

### 构造方法

```java
public ConcurrentHashMap(int initialCapacity,
                         float loadFactor, int concurrencyLevel) {
    if (!(loadFactor > 0.0f) || initialCapacity < 0 || concurrencyLevel <= 0)
        throw new IllegalArgumentException();
    if (initialCapacity < concurrencyLevel)   // Use at least as many bins
        initialCapacity = concurrencyLevel;   // as estimated threads
    
    long size = (long)(1.0 + (long)initialCapacity / loadFactor);
    
    int cap = (size >= (long)MAXIMUM_CAPACITY) ?
        MAXIMUM_CAPACITY : tableSizeFor((int)size);
    
    this.sizeCtl = cap;
}
```

初始化过程一共处理了两件事。判断inititalCapacity的值是否合法和确定sizeCtl，concurrencyLevel引入但是只在初始化initialCapacity的时候被使用。现在相当于它的最小值。但是正常的情况下初始化只是计算了sizeCtl的值，sizeCtl的值会被`tableSizeFor`设置为一个2的次幂的数字（必须为2的次幂目也是像HashMap一样通过位运算得到的索引值与使用`%`的结果相同），如果通过构造方法传入initialCapacity为16的情况下sizeCtl为32。但是ConcurrentHashMap有一个无参的构造方法，这时候初始化bins数组长度为`DEFAULT_CAPACITY`也就是16。

### putVal

```java
final V putVal(K key, V value, boolean onlyIfAbsent) {
    if (key == null || value == null) throw new NullPointerException();
    int hash = spread(key.hashCode());
    int binCount = 0;
    for (Node<K,V>[] tab = table;;) {
        Node<K,V> f; int n, i, fh; K fk; V fv;
        if (tab == null || (n = tab.length) == 0)
            tab = initTable();
        else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
            if (casTabAt(tab, i, null, new Node<K,V>(hash, key, value)))
                break;                   // no lock when adding to empty bin
        }
        else if ((fh = f.hash) == MOVED)
            tab = helpTransfer(tab, f);
        else if (onlyIfAbsent // check first node without acquiring lock
                 && fh == hash
                 && ((fk = f.key) == key || (fk != null && key.equals(fk)))
                 && (fv = f.val) != null)
            return fv;
        else {
            V oldVal = null;
            synchronized (f) {
                if (tabAt(tab, i) == f) {
                    if (fh >= 0) {
                        binCount = 1;
                        for (Node<K,V> e = f;; ++binCount) {
                            K ek;
                            if (e.hash == hash &&
                                ((ek = e.key) == key ||
                                 (ek != null && key.equals(ek)))) {
                                oldVal = e.val;
                                if (!onlyIfAbsent)
                                    e.val = value;
                                break;
                            }
                            Node<K,V> pred = e;
                            if ((e = e.next) == null) {
                                pred.next = new Node<K,V>(hash, key, value);
                                break;
                            }
                        }
                    }
                    else if (f instanceof TreeBin) {
                        Node<K,V> p;
                        binCount = 2;
                        if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key,
                                                       value)) != null) {
                            oldVal = p.val;
                            if (!onlyIfAbsent)
                                p.val = value;
                        }
                    }
                    else if (f instanceof ReservationNode)
                        throw new IllegalStateException("Recursive update");
                }
            }
            if (binCount != 0) {
                if (binCount >= TREEIFY_THRESHOLD)
                    treeifyBin(tab, i);
                if (oldVal != null)
                    return oldVal;
                break;
            }
        }
    }
    addCount(1L, binCount);
    return null;
}
```

putVal方法首先通过`spread`计算出key的hash值。`spread`通过将key的hash值的地位16位无符号右移到高位，并且与原hash值做异或，最后与0x7fffffff 按位与，来获得一个相对均匀的Hash值。

然后进入到了一个自旋中，最小化因同步阻塞带来的性能消耗。在自旋中首先判断bins数组是否为空或者长度为0，判断成功后会调用`initTable`方法对bins数组进行初始化。

```java
private final Node<K,V>[] initTable() {
    Node<K,V>[] tab; int sc;
    while ((tab = table) == null || tab.length == 0) {
        if ((sc = sizeCtl) < 0)
            Thread.yield(); // lost initialization race; just spin
        else if (U.compareAndSetInt(this, SIZECTL, sc, -1)) {
            try {
                if ((tab = table) == null || tab.length == 0) {
                    int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
                    @SuppressWarnings("unchecked")
                    Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                    table = tab = nt;
                    sc = n - (n >>> 2);
                }
            } finally {
                sizeCtl = sc;
            }
            break;
        }
    }
    return tab;
}
```

initTable方法也有一个自旋，首先判断sizeCtl的值，如果sizeCtl的值小于0，则说明bins数组正在被别的线程初始化或者调整大小，此时当前线程应该做的就是让出CPU，让其他线程完成初始化操作。如果sizeCtl大于0，说明目前还没有别的线程，真正进入到初始化的真正步骤当中，此时应该要做的就是将sizeCtl的值先保存到变量`sr` 然后将其CAS为`-1`，`-1`代表bins数组正在被初始化或者被调整大小。sr则被用于初始化bins数组`table`的大小，在这里ConcurrentHashMap与HashMap的第一点不同：**他们两个的初始化都是懒加载，但是HashMap的table初始值保存在ThresHold中，而ConcurrnetHashMap则被保存在sizeCtl中。**

接着执行初始化table的逻辑，如果 sc > 0 则将sc设置为初始的数组长度，如果使用的是无参的构造方法，那么sizeCtl就是默认的0值，使用`DEFAULT_CAPACITY`也就是16作为table的初始长度。在初始化完成后还要讲sizeCtl的值设置为 `n - (n >>> 2)`（n代表新数组长度,`>>>`代表无符号右移，n >>> m 相当于 n / pow(2,m)），默认情况下sizeCtl的新值为12。最后返回新table。

在初始话完成后进行一次自旋，下一次就会进入其他判断逻辑，首先通过Unsafe类获在table上`(n - 1) & hash`（除留取余法，与`%`相同）索引处为null的时候设置新的bin，如果如果成功,则将put成功，跳出自旋。注意在table上bin为null的时候put，是不需要锁的。

如果此时table上该索引不为null,则CAS会失败，说明该索引处的bin不为null。这时候如果想插入节点，应该将新节点加入bin后面形成连表，而此时首先应该判断table是否正在被移动，需要判断bin的hash值此时是否等于MOVED( -1 )，在前面的doc注释中已经提到过，当hash值为负数的时候表达一些特殊的含义。此时就代表该bin正处于从旧table transfer 到新的table的过程中。此时并不会将当前线程阻塞，而是执行`helpTranfer`

```java
final Node<K,V>[] helpTransfer(Node<K,V>[] tab, Node<K,V> f) {
    Node<K,V>[] nextTab; int sc;
    if (tab != null && (f instanceof ForwardingNode) &&
        (nextTab = ((ForwardingNode<K,V>)f).nextTable) != null) {
        int rs = resizeStamp(tab.length);
        while (nextTab == nextTable && table == tab &&
               (sc = sizeCtl) < 0) {
            if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                sc == rs + MAX_RESIZERS || transferIndex <= 0)
                break;
            if (U.compareAndSetInt(this, SIZECTL, sc, sc + 1)) {
                transfer(tab, nextTab);
                break;
            }
        }
        return nextTab;
    }
    return table;
}
```

helpTransfer参数是table与正在转移的bin节点。



如果当前节点没有在transfer状态，则会继续判断`onlyIfAbsent`是否为ture。这个参数在putIfAbsent方法中才会为true，如果在Map中已经存在key并且value不为null，则会直接返回value值，而不执行插入操作。

如果onlyIfAbsent为fasle，则证明当前table上的bin不为null,此时插入操作，应该向以bin为头结点的链表上执行插入操作。首先使用Synchronized代码块锁住table上的bin，也就是锁住链表的头结点。在锁住bin后还需要重新判断当前位置的bin有没有变化而且bin的hash值 > 0（bin不是特殊节点）。真正的插入操作还是在一个自旋中进行的并且使用binCount字段计数当亲链表的长度，当超过一定的阈值后会在插入成功后将链表转化成树。

插入操作一共有三种情况，向链表中插入、向树中插入、bin节点是ReservationNode（computeIfAbsent和compute中使用的占位符节点）

1. ```java
   for (Node<K,V> e = f;; ++binCount) {
       K ek;
       if (e.hash == hash &&
           ((ek = e.key) == key ||
            (ek != null && key.equals(ek)))) {
           oldVal = e.val;
           if (!onlyIfAbsent)
               e.val = value;
           break;
       }
       Node<K,V> pred = e;
       if ((e = e.next) == null) {
           pred.next = new Node<K,V>(hash, key, value);
           break;
       }
   }
   ```

   当节点的hash值相等并且key值也相等的时候，会判断是否是`putIfAbsent`方法，如果onlyIfAbsent为false，则直接替换节点value值并跳出返回。如果遍历到链表末尾，e为null的时候说明bin的链表中没有插入的key值，应该新建一个节点并且插入到链表末尾。

2.  第二种遇到树的情况在插入后树化的时候再具体分析，在调用`TreeBin.putTreeVal`进行树的插入操作后如果新插入节点则返回空值，如果树种已经有与key相同的节点，则返回该节点并在onlyIfAbsent为false的时候进行替换。

   ```java
   else if (f instanceof TreeBin) {
       Node<K,V> p;
       binCount = 2;
       if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key,
                                      value)) != null) {
           oldVal = p.val;
           if (!onlyIfAbsent)
               p.val = value;
       }
   }
   ```

3. 最后一种情况时遇到`ReservationNode`节点的时候直接抛出异常。

在插入成功后判断binCount是否超过了`TREEIFY_THRESHOLD`，则会进行树化，在树化结束后，如果oldVal不为null则证明是替换了旧值，不用跳出自旋执行addCount直接返回，否则跳出自旋执行addCount。

```java
private final void addCount(long x, int check) {
    CounterCell[] cs; long b, s;
    if ((cs = counterCells) != null ||
        !U.compareAndSetLong(this, BASECOUNT, b = baseCount, s = b + x)) {
        CounterCell c; long v; int m;
        boolean uncontended = true;
        if (cs == null || (m = cs.length - 1) < 0 ||
            (c = cs[ThreadLocalRandom.getProbe() & m]) == null ||
            !(uncontended =
              U.compareAndSetLong(c, CELLVALUE, v = c.value, v + x))) {
            fullAddCount(x, uncontended);
            return;
        }
        if (check <= 1)
            return;
        s = sumCount();
    }
    if (check >= 0) {
        Node<K,V>[] tab, nt; int n, sc;
        while (s >= (long)(sc = sizeCtl) && (tab = table) != null &&
               (n = tab.length) < MAXIMUM_CAPACITY) {
            int rs = resizeStamp(n);
            if (sc < 0) {
                if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                    sc == rs + MAX_RESIZERS || (nt = nextTable) == null ||
                    transferIndex <= 0)
                    break;
                if (U.compareAndSetInt(this, SIZECTL, sc, sc + 1))
                    transfer(tab, nt);
            }
            else if (U.compareAndSetInt(this, SIZECTL, sc,
                                         (rs << RESIZE_STAMP_SHIFT) + 2))
                transfer(tab, null);
            s = sumCount();
        }
    }
}
```

在没有竞争的情况下，baseCount字段被CAS设置加一。然后进入下一个判断，当baseCount+x（putVal里等于1L的时候）> sizeCtl 并且table不为null、table.length < MAXIMUM_CAPACITY的时候进入自旋。

首先调用resizeStamp获取用于调整大小为n的表的大小的标记戳用于修改sizeCtl。在第一次进行扩容的时候返回值为32795。而且此时的sizeCtl值为12，所以会进入else if块里面将sizeCtl设置为一个负数，表示进入了resize阶段。







### 树操作

当bin是TreeBin的实例代表table上该索引处是红黑树，TreeBin只在table索引上使用，值持有树的根节点和相应的锁信息。

```java
  /**
   * 在bins头部使用的TreeNodes。 TreeBins不包含用户键或值，而是指向TreeNodes列表及其根目录。
   * 它们还保持寄生的读写锁，迫使writers（持有bin锁定的线程）等待readers（没有持有bin锁定的线程）在树重组操作之前完成。  
   **/
    static final class TreeBin<K,V> extends Node<K,V> {
        TreeNode<K,V> root;
        volatile TreeNode<K,V> first;
        volatile Thread waiter;
        volatile int lockState;
        // values for lockState
        static final int WRITER = 1; // set while holding write lock
        static final int WAITER = 2; // set when waiting for write lock
        static final int READER = 4; // increment value for setting read lock
        
```


