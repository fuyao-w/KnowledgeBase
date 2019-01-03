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

此哈希表的主要设计目标是在最小化更新争用的同时保持并发可读性（通常是方法get（），但也包括迭代器和相关方法）。次要目标是使空间消耗与java.util.HashMap保持大致相同或更好，并支持许多线程在空表上的高初始插入速率。
 此映射通常用作分箱（bin）（分区）哈希表。每个键值映射都保存在Node中。大多数Node是具有散列，键，值和下一个字段的基本Node类的实例。但是，存在各种子类：TreeNodes排列在平衡树中，而不是数组中。 TreeBins拥有TreeNodes集的根。在调整大小期间，转发节点被放置在bins的头部。在computeIfAbsent和相关方法中建立值时，ReservationNodes用作占位符。 TreeBin，ForwardingNode和ReservationNode类型不包含普通用户键，值或哈希值，并且在搜索等过程中很容易区分，因为它们具有负哈希字段以及空键和值字段。 （这些特殊节点要么不常见，要么是瞬态的，因此携带一些未使用的字段的影响是微不足道的。）

数组被赖加载为一个二次幂的大小
第一次插入。表中的每个bin通常包含一个Node链表（最常见的是，链表只有零个或一个节点）。链表表访问需要 volatile/atomic 读取，writes和CASes。因为在没有进一步间接的情况下没有其他方安排这个，我们使用内置函数（jdk.internal.misc.Unsafe）操作。
我们使用节点哈希字段的顶部（符号）位用于控制目的 - 由于寻址约束，它仍然可用。具有负散列字段的节点在map方法中被特殊处理或忽略。
第一个节点在空箱中的插入（通过put或其变体）通过将其CAS化到箱中来执行。到目前为止，这是大多数键/哈希分配下的put操作的最常见情况。其他更新操作（插入，删除和替换）需要锁定。我们不想浪费将不同的锁对象与每个bin关联所需的空间，因此使用bin列表本身的第一个节点作为锁。锁定对这些锁的支持依赖于内置的“同步”监视器。

Using the first node of a list as a lock does not by itself suffice though:：当一个节点被锁定时，任何更新必须首先验证它在锁定后仍然是第一个节点，如果没有则重试。由于新节点始终附加到链表中，因此一旦节点首先位于bin中，它将一直保留，直到删除或bin变为无效（调整大小时）。
每个bin锁的主要缺点是，由同一个锁保护的bin列表中的其他节点上的其他更新操作可能会停止，例如当用户equals（）或映射函数需要很长时间时。但是，统计上，在随机哈希码下，这不是常见问题。理想情况下，箱中节点的频率遵循泊松分布（http://en.wikipedia.org/wiki/Poisson_distribution）
在给定调整阈值0.75的情况下平均约为0.5的参数，尽管由于调整粒度而具有较大的方差。忽略方差，列表大小k的预期出现是（exp（-0.5）pow（0.5，k）/ factorial（k））。第一个值是：
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
在随机哈希下，访问不同元素的两个线程的锁争用概率大约是1 /（8 #elements）。
 在实践中遇到的实际哈希码分布有时会明显偏离均匀随机性。这包括当N>（1 << 30）时的情况，因此一些键必须碰撞。类似地，对于dumb或hostile用法，不同的多个密钥被设计为具有相同的哈希码或仅在掩蔽的高位中不同的哈希码。因此，我们使用二进制策略，当bin中的节点数超过阈值时应用该策略。这些TreeBins使用平衡树来保存节点（一种特殊形式的红黑树），将搜索时间限制为O（log N）。 TreeBin中的每个搜索步骤至少是常规列表中的两倍，但是假设N不能超过（1 << 64）（在用完地址之前），则将搜索步骤，锁定保持时间等限制为合理常量（每个操作最坏情况下检查大约100个节点），只要键是可比较的（这是非常常见的 - 字符串，长整数等）。 TreeBin节点（TreeNodes）也保持与常规节点相同的“下一个”遍历指针，因此可以以相同的方式遍历迭代器。
当占用率超过百分比阈值（名义上为0.75，但见下文）时，表格会调整大小。在启动线程分配和设置替换数组之后，任何注意到需要扩容的线程都可以帮助调整大小。但是，这些其他线程可能会继续进行插入等操作，而不是拖延。使用TreeBins可以防止在调整大小过程中出现溢出的最坏情况。通过将bin一个接一个地从表格转移到下一个表格来调整收益大小。但是，线程要求在执行此操作之前传输小块索引（通过field transferIndex），从而减少争用。字段sizeCtl中的生成标记可确保不会重叠绑定。因为我们使用的是二次幂扩展，所以每个bin的元素必须保持相同的索引，或者以两个偏移的幂移动。我们通过捕获旧节点可以重用的情况来消除不必要的节点创建，因为它们的下一个字段不会改变。平均而言，当表翻倍时，只有大约六分之一的人需要克隆。一旦它们不再被可能同时遍历表中的任何读者线程引用，它们替换的节点将是垃圾收集的。在传输时，旧表bin仅包含特殊转发节点（具有散列字段“MOVED”），其包含下一个表作为其密钥。遇到转发节点时，使用新表重新启动访问和更新操作。

每个bin的transfer都需要其bin锁定，这可能会在调整大小时停止等待锁定。但是，因为其他线程可以加入并帮助调整大小而不是争用锁，所以随着调整大小的进行，平均聚合等待会变短。传输操作还必须确保旧表和新表中的所有可访问的bin都可以被任何遍历使用。这部分是通过从最后一个bin（table.length  -  1）向前到第一个bin来安排的。在看到转发节点时，遍历（请参阅类Traverser）安排移动到新表而不重新访问节点。为了确保即使在不按顺序移动时也不会跳过中间节点，在遍历期间首次遇到转发节点时会创建堆栈（请参阅类TableStack），以便在以后处理当前表时保持其位置。对这些保存/恢复机制的需求相对较少，但是当遇到一个转发节点时，通常会有更多转发节点。
 所以Traversers使用一个简单的缓存方案来避免创建这么多新的TableStack节点。 （感谢Peter Levart 建议在这里使用堆栈。）
遍历方案也适用于部分遍历箱的范围（通过备用的Traverser构造函数）来支持分区聚合操作。此外，如果转发到空表，则只读操作会放弃，这表示对当前未实现的关闭样式清除提供支持。
延迟表初始化在首次使用之前最小化占用空间，并且当第一个操作来自putAll，带有map参数的构造函数或反序列化时，也避免了重新绑定。这些情况试图覆盖初始容量设置，但在比赛的情况下无害地无法生效。

使用LongAdder的特化来维护元素计数。我们需要结合专门化而不是仅使用LongAdder来访问隐式争用感知，从而导致创建多个CounterCell。计数器机制可以避免对更新进行争用，但如果在并发访问期间过于频繁地读取，则会遇到缓存抖动。为了避免经常阅读，仅在添加到已经存在两个或更多节点的bin时才尝试在争用下调整大小。在统一的哈希分布下，这种情况发生在阈值时大约为13％，这意味着只有大约1/8的检查阈值（并且在调整大小之后，很少会这样做）。
TreeBins对搜索和相关操作使用特殊形式的比较（这是我们不能使用现有集合（如TreeMaps）的主要原因）。 TreeBins包含Comparable元素，但可能包含其他元素，以及可比较的元素，但不一定对于相同的T可比较，因此我们不能在它们之间调用compareTo。要处理此问题，树主要按哈希值排序，然后按Comparable.compareTo顺序排序（如果适用）。在节点上查找时，如果元素不可比较或比较为0，则在绑定的哈希值的情况下可能需要搜索左和右子节点。 （这对应于完整列表搜索，如果所有元素都是不可比较的并且具有绑定的哈希值，则必须进行完整列表搜索。）在插入时，为了保持重新排序的总排序（或者在此处需要尽可能接近），我们比较类和identityHashCodes作为打破者。红黑平衡代码从pre-jdk-collections更新
 （http://gee.cs.oswego.edu/dl/classes/collections/RBCell.java）依次是Cormen，Leiserson和Rivest“算法导论”（CLR）。

​	TreeBins还需要额外的锁定机制。尽管在更新期间读者总是可以进行列表遍历，但是树遍历不是，主要是因为可以改变根节点和/或其链接的树旋转。 TreeBins包含一个寄生在主bin同步策略上的简单读写锁定机制：与插入或删除相关的结构调整已经被bin锁定（因此不能与其他编写者冲突），但必须等待正在进行的读者完成。由于只有一个这样的服务员，我们使用一个简单的方案，使用一个“服务员”字段来阻止编写者。但是，读者永远不会阻塞。如果保持根锁，则它们沿慢速遍历路径（通过下一个指针）继续，直到锁变为可用或列表耗尽为止，以先到者为准。这些情况并不快，但最大化了预期的总吞吐量。
 维护API和序列化与以前的兼容性
​	维护API和序列化与此类以前版本的兼容性引入了一些奇怪的问题。 主要是：我们保留未引用但未使用的构造函数参数，引用concurrencyLevel。 我们接受一个loadFactor构造函数参数，但只将其应用于初始表容量（这是我们唯一可以保证遵守它的时间。）我们还声明了一个未使用的“Segment”类，只有在序列化时才会以最小的形式实例化。
 此外，仅为了与此类的先前版本兼容，它扩展了AbstractMap，即使它的所有方法都被覆盖，因此它只是无用的包袱。
 这个文件被组织起来，使得阅读时的内容比其他方式更容易：首先是主要的静态声明和实用程序，然后是字段，然后是主要的公共方法（将多个公共方法的几个因子分解为内部方法），然后调整大小方法，树，遍历和批量操作。



```java
    /**
     * 我们保持原状，但不是最大的桌面容量。 该值必须正好为1 << 30才能保持Java数组分配和两个表大小的
     * 幂的索引边界，并且还需要，因为32位散列字段的前两位用于控制目的。使用的构造函数参数指向
     * concurrencyLevel。
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
     * 树。 该值必须大于2，并且应该至少为8以与树木移除中的假设相关联，以便在收缩时转换回普通箱。
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
     *每次转移步骤的最小重组次数。 范围细分为允许多个缩放器线程。
      *  此值用作下限以避免resizer遇到过多的内存争用。 该值至少应为DEFAULT_CAPACITY。
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
     * 基本计数器值，主要在没有争用时使用，但也作为表初始化比赛期间的后备。 通过CAS更新。
     */
    private transient volatile long baseCount;
    
    /**
     * 表初始化和调整大小控制。 当为负数时，表正在初始化或调整大小：-1表示初始化，
     * 否则 - （1 +活动大小调整线程数）。 否则，当table为null时，
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

