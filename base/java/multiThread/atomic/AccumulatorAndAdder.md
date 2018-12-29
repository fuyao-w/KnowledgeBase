## Striped64

```java
abstract class Striped64 extends Number
```

一个包本地类，包含支持64位值动态条带化的类的通用表示和机制。 该类扩展了Number，因此具体的子类必须公开这样做

分析

这个类维护一个原子更新变量的延迟初始化表，以及一个额外的“base”字段。数组大小是2的幂。 索引使用屏蔽的每个线程的哈希码。 此类中的几乎所有声明都是package-private，由子类直接访问。

列表的item属于Cell类; AtomicLong的变体填充（通过@Contended）以减少缓存争用。 对于大多数原子来说，填充是过度的，因为它们通常不规则地分散在存储器中，因此不会相互干扰太多。 但是，驻留在数组中的Atomic对象往往会彼此相邻放置，因此在没有这种预防措施的情况下，通常会共享缓存行（对性能产生巨大的负面影响）。

部分原因是cell相对较大，我们避免在需要它们之前创建它们。 如果没有争用，则对基本字段进行所有更新。 在第一次争用时（基础更新上的CAS失败），该表被初始化为大小2.在进一步争用时，表大小加倍，直到达到大于或等于CPUS数的最接近2的幂。 表槽在需要时保持为空（空）。

单个螺旋锁（“cellsBusy”）用于初始化和调整表的大小，以及使用新单元填充插槽。 不需要阻塞锁; 当锁不可用时，线程会尝试其他插槽（或基座）。 在这些重试期间，争用增加并且局部性降低，这仍然比替代方案更好。

通过ThreadLocalRandom维护的Thread探测字段用作每线程哈希码。 我们让它们保持未初始化为零（如果它们以这种方式出现），直到它们在时隙0处竞争。然后将它们初始化为通常不会与其他值冲突的值。 执行更新操作时，失败的CAS会指示争用和/或表冲突。 碰撞时，如果表大小小于容量，则它的大小加倍，除非某些其他线程持有锁。 如果散列插槽为空，并且锁定可用，则会创建新的单元。 否则，如果插槽存在，则尝试CAS。 重试通过“双重散列”继续，使用辅助散列（Marsaglia XorShift）尝试查找空闲插槽。

表大小是有限的，因为当线程多于CPU时，假设每个线程都绑定到CPU，就会存在一个完美的哈希函数，将线程映射到槽以消除冲突。 当我们达到容量时，我们通过随机改变冲突线程的哈希码来搜索这个映射。 因为搜索是随机的，并且冲突仅通过CAS失败而变得已知，所以收敛可能很慢，并且因为线程通常不会永远地绑定到CPUS，所以可能根本不会发生。 然而，尽管存在这些限制，但在这些情况下观察到的争用率通常较低。

当曾经散列到它的线程终止时，以及在表格加倍导致没有线程在扩展掩码下散列到它的情况下，Cell可能会被释放。 我们不会尝试检测或删除此类细胞，假设对于长期运行的实例，观察到的争用水平将会重现，因此最终将再次需要cell; 对于短命的，没关系。

```java
static final class Cell {
    volatile long value;
    Cell(long x) { value = x; }
    final boolean cas(long cmp, long val) {
        return VALUE.compareAndSet(this, cmp, val);
    }
    final void reset() {
        VALUE.setVolatile(this, 0L);
    }
    final void reset(long identity) {
        VALUE.setVolatile(this, identity);
    }
    final long getAndSet(long val) {
        return (long)VALUE.getAndSet(this, val);
    }

    // VarHandle mechanics
    private static final VarHandle VALUE;
    static {
        try {
            MethodHandles.Lookup l = MethodHandles.lookup();
            VALUE = l.findVarHandle(Cell.class, "value", long.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
```

```java
/** Number of CPUS, to place bound on table size */
static final int NCPU = Runtime.getRuntime().availableProcessors();

/**
 * Table of cells. When non-null, size is a power of 2.
 */
transient volatile Cell[] cells;

/**
 * 基值，主要在没有争用时使用，但也作为表初始化比赛期间的后备。通过CAS更新。
 */
transient volatile long base;

/**
 * 调整大小和/或创建单元格时使用的Spinlock（通过CAS锁定）。
 */
transient volatile int cellsBusy;
```

```java
final void longAccumulate(long x, LongBinaryOperator fn,
                          boolean wasUncontended) {
    int h;
    if ((h = getProbe()) == 0) {//返回当前线程的探测值。由于包限制，从ThreadLocalRandom复制。
        ThreadLocalRandom.current(); // 强制初始化
        h = getProbe();
        wasUncontended = true;  //false if CAS failed before call
    }
    boolean collide = false;                // 如果最后一个插槽非空，则为真
    done: for (;;) {
        Cell[] cs; Cell c; int n; long v;
        if ((cs = cells) != null && (n = cs.length) > 0) {
            if ((c = cs[(n - 1) & h]) == null) {//取余
                if (cellsBusy == 0) {       // 尝试附加新的Cell
                    Cell r = new Cell(x);   // 乐观创造
                    if (cellsBusy == 0 && casCellsBusy()) {
                        try {               //在锁定下重新检查
                            Cell[] rs; int m, j;
                            if ((rs = cells) != null &&
                                (m = rs.length) > 0 &&
                                rs[j = (m - 1) & h] == null) {
                                rs[j] = r; //赋值
                                break done;
                            }
                        } finally {
                            cellsBusy = 0;
                        }
                        continue;           //插槽现在非空
                    }
                }
                collide = false;
            }
            else if (!wasUncontended)       // CAS已知失败
                wasUncontended = true;      // 重拍后继续
            else if (c.cas(v = c.value,
                           (fn == null) ? v + x : fn.applyAsLong(v, x)))
                break;
            else if (n >= NCPU || cells != cs)
                collide = false;            // At max size or stale
            else if (!collide)
                collide = true;
            else if (cellsBusy == 0 && casCellsBusy()) {
                try {
                    if (cells == cs)        // 扩容
                        cells = Arrays.copyOf(cs, n << 1);
                } finally {
                    cellsBusy = 0;
                }
                collide = false;
                continue;                   // Retry with expanded table
            }
            h = advanceProbe(h);
        }
        else if (cellsBusy == 0 && cells == cs && casCellsBusy()) {
            try {                           // Initialize table
                if (cells == cs) {
                    Cell[] rs = new Cell[2];
                    rs[h & 1] = new Cell(x);
                    cells = rs;
                    break done;
                }
            } finally {
                cellsBusy = 0;
            }
        }
        // Fall back on using base
        else if (casBase(v = base,
                         (fn == null) ? v + x : fn.applyAsLong(v, x)))
            break done;
    }
}
```

## LongAdder

```java
public class LongAdder extends Striped64 implements Serializable 
```

一个或多个变量共同维持数组最初的和为0。当跨线程争用更新（方法`add（long）`）时，变量集可能会动态增长以减少争用。方法`sum（）`（或等效地，`longValue（）`）返回保持总和的变量的当前总和。
当多个线程更新用于收集统计信息但不用于细粒度同步控制的目的的公共和时，此类通常优于AtomicLong。在低更新争用下，这两个类具有相似的特征。但在高争用的情况下，这一类的预期吞吐量明显更高，但代价是空间消耗更高。

LongAdders可以与ConcurrentHashMap一起使用，以维护可伸缩的频率映射（直方图或多集的形式）。例如，要将计数添加到`ConcurrentHashMap <String，LongAdder> freqs`，初始化（如果尚未存在），则可以使用`freqs.computeIfAbsent（key，k  - > new LongAdder（））`。`increment（）;`

此类扩展了Number，但没有定义equals，hashCode和compareTo等方法，因为实例应该是变异的，因此不能用作集合键。

LongAdder类似线程私有的原子变量，并且可以将所有线程的值累加起来。

```java
public void add(long x) {
    Cell[] cs; long b, v; int m; Cell c;
    if ((cs = cells) != null || !casBase(b = base, b + x)) {
        boolean uncontended = true;
        if (cs == null || (m = cs.length - 1) < 0 ||
            (c = cs[getProbe() & m]) == null ||
            !(uncontended = c.cas(v = c.value, v + x)))
            longAccumulate(x, null, uncontended);
    }
}
```

如果没有线程征用，那么直接使用base字段作为基础值，当有线程竞争的时候调用`longAccumulate`

## LongAccumulator

```java
public class LongAccumulator extends Striped64 implements Serializable
```

### java doc

使用提供的函数一起维护运行长值的一个或多个变量。当跨线程争用更新（方法累积（长））时，变量集可以动态增长以减少争用。方法get（）（或等效地，longValue（））返回维护更新的变量的当前值。
当多个线程更新用于收集统计信息等目的的公共值时，此类通常优于AtomicLong，而不是用于细粒度同步控制。在低更新争用下，这两个类具有相似的特征。但在高争用的情况下，这一类的预期吞吐量明显更高，但代价是空间消耗更高。

线程内或线程之间的累积顺序无法保证且不能依赖，因此该类仅适用于累积顺序无关紧要的函数。提供的累加器功能应该是无副作用的，因为当尝试的更新由于线程之间的争用而失败时可以重新应用它。对于可预测的结果，累加器函数应该是关联的和可交换的。该函数应用现有值（或标识）作为一个参数，给定更新作为另一个参数。例如，要保持运行的最大值，可以提供Long :: max以及Long.MIN_VALUE作为标识。

LongAdder类提供了此类功能的类比，用于维护计数和总和的常见特殊情况。调用new LongAdder（）相当于新的LongAccumulator（（x，y） - > x + y，0L）。

此类扩展了Number，但没有定义equals，hashCode和compareTo等方法，因为实例应该是变异的，因此不能用作集合键。

```java
private final LongBinaryOperator function;
private final long identity;

/**
 * Creates a new instance using the given accumulator function
 * and identity element.
 * @param accumulatorFunction a side-effect-free function of two arguments
 * @param identity identity (initial value) for the accumulator function
 */
public LongAccumulator(LongBinaryOperator accumulatorFunction,
                       long identity) {
    this.function = accumulatorFunction;
    base = this.identity = identity;
}
```

LongAccumulator与longAdder通过一个函数式接口定义数值的累积行为

```java
public void accumulate(long x) {
    Cell[] cs; long b, v, r; int m; Cell c;
    if ((cs = cells) != null
        || ((r = function.applyAsLong(b = base, x)) != b
            && !casBase(b, r))) { //无竞争使用base字段存储
        boolean uncontended = true;
        if (cs == null
            || (m = cs.length - 1) < 0
            || (c = cs[getProbe() & m]) == null
            || !(uncontended =
                 (r = function.applyAsLong(v = c.value, x)) == v
                 || c.cas(v, r)))
            longAccumulate(x, function, uncontended); 
    }
}
```