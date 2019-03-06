## IdentityHashMap

```java
public class IdentityHashMap<K,V> extends AbstractMap<K,V>
    implements Map<K,V>, java.io.Serializable, Cloneable
```

### java doc 

此类使用哈希表实现Map接口，在比较键（和值）时使用引用相等性代替对象相等性。 换句话说，在IdentityHashMap中，当且仅当（k1 == k2）时，两个密钥k1和k2被认为是相等的。 （在正常的Map实现中（如HashMap），当且仅当（k1 == null？k2 == null：k1.equals（k2））时，两个键k1和k2被认为是相等的。）

**这个类不是通用的Map实现！ 虽然这个类实现了Map接口，但它故意违反了Map的一般契约，它要求在比较对象时使用equals方法。 此类仅用于需要引用相等语义的罕见情况。**

此类的典型用法是保留拓扑的对象图转换，例如序列化或深度复制。 要执行此类转换，程序必须维护一个“节点表”，以跟踪已处理的所有对象引用。 节点表必须不等同于不同的对象，即使它们碰巧相等。 此类的另一个典型用法是维护代理对象。 例如，调试工具可能希望为正在调试的程序中的每个对象维护一个代理对象。

此类提供所有可选的映射操作，并允许空值和空键。 这个类不保证地图的顺序; 特别是，它不保证订单会随着时间的推移保持不变。

假设系统标识哈希函数（System.identityHashCode（Object））在桶之间正确地分散元素，则此类为基本操作（get和put）提供常量性能。

此类有一个调整参数（影响性能但不影响语义）：预期的最大大小。此参数是地图预期保留的最大键值映射数。在内部，此参数用于确定最初包含哈希表的存储桶数。未指定预期最大大小和桶数之间的精确关系。

如果映射的大小（键值映射的数量）充分超过预期的最大大小，则桶的数量增加。增加桶的数量（“rehashing”）可能相当昂贵，因此创建具有足够大的预期最大大小的身份哈希映射是值得的。另一方面，对集合视图的迭代需要与哈希表中的桶数成比例的时间，因此如果您特别关注迭代性能或内存使用，则不应将预期的最大大小设置得太高。

请注意，此实现不同步。 如果多个线程同时访问标识哈希映射，并且至少有一个线程在结构上修改了映射，则必须在外部进行同步。 （结构修改是添加或删除一个或多个映射的任何操作;仅更改与实例已包含的键关联的值不是结构修改。）这通常通过同步自然封装映射的某个对象来完成。。 如果不存在此类对象，则应使用Collections.synchronizedMap方法“包装”该映射。 这最好在创建时完成，以防止意外地不同步访问地图：

```
   Map m = Collections.synchronizedMap(new IdentityHashMap(...));
```

所有这个类的“集合视图方法”返回的集合的迭代器方法返回的迭代器是快速失败的：如果在创建迭代器之后的任何时候对映射进行结构修改，除非通过迭代器自己的删除方法，迭代器将抛出ConcurrentModificationException。因此，在并发修改的情况下，迭代器快速而干净地失败，而不是在未来的未确定时间冒任意，非确定性行为的风险。

请注意，迭代器的快速失败行为无法得到保证，因为一般来说，在存在不同步的并发修改时，不可能做出任何硬性保证。失败快速迭代器会尽最大努力抛出ConcurrentModificationException。因此，编写依赖于此异常的程序以确保其正确性是错误的：故障快速迭代器应仅用于检测错误。

实现说明：这是一个简单的线性探测哈希表，如Sedgewick和Knuth的文本中所述。数组交替显示保持键和值。 （对于大型表，这比使用单独的数组具有更好的局部性。）对于许多JRE实现和操作混合，此类将比HashMap（使用链接而不是线性探测）产生更好的性能。

### 字段

```java
/**
 * 无参构造函数使用的初始容量.
 * 必须是二的次幂. 在给定载荷因子为2/3的情况下，值32对应于（指定的）预期最   *大元素数量21。
 */
private static final int DEFAULT_CAPACITY = 32;

/**
 * 最小容量，如果隐式指定较低值则使用
 * 由具有参数的任一构造函数. 在给定载荷因子为2/3的情况下，值4对应于预期的最   * 大尺寸2。 必须是二的次幂。
 */
private static final int MINIMUM_CAPACITY = 4;

/**
 * 最大容量，如果隐式指定更高的值，则使用该容量
 * 由具有参数的任一构造函数.
 * 必须是二的次幂 值 <= 1<<29.
 *
 * 实际上，map可以容纳不超过MAXIMUM_CAPACITY-1项，因为它必须至少有一个带   
 *  key == null的槽，以避免get（），put（），remove（）中的无限循环
 */
private static final int MAXIMUM_CAPACITY = 1 << 29;

/**
 * 表示表内的空键的值.
 */
static final Object NULL_KEY = new Object();

```

IdentityHashMap的初始容量为32，默认最小期望值为4。NULL_KEY帮住区分null带不代表key。

## 分析 

```java
public IdentityHashMap() {
    init(DEFAULT_CAPACITY);
}
```

无参构造方法将 默认值扩大二倍，也就是 64。

```java
public IdentityHashMap(int expectedMaxSize) {
    if (expectedMaxSize < 0)
        throw new IllegalArgumentException("expectedMaxSize is negative: "
                                           + expectedMaxSize);
    init(capacity(expectedMaxSize));
}
```

```java
private static int capacity(int expectedMaxSize) {
    // assert expectedMaxSize >= 0;
    return
        (expectedMaxSize > MAXIMUM_CAPACITY / 3) ? MAXIMUM_CAPACITY :
        (expectedMaxSize <= 2 * MINIMUM_CAPACITY / 3) ? MINIMUM_CAPACITY :
        Integer.highestOneBit(expectedMaxSize + (expectedMaxSize << 1));
}
```

```java
private void init(int initCapacity) {
    table = new Object[2 * initCapacity];
}
```

初始化过程将数组长度初始化为，最接近expectedMaxSize二倍的2次幂数*2。如果是8的话就返回32，7也返回32。

```java
public V put(K key, V value) {
    final Object k = maskNull(key);

    retryAfterResize: for (;;) {
        final Object[] tab = table;
        final int len = tab.length;
        int i = hash(k, len);

        for (Object item; (item = tab[i]) != null;
             i = nextKeyIndex(i, len)) {
            if (item == k) {
                @SuppressWarnings("unchecked")
                    V oldValue = (V) tab[i + 1];
                tab[i + 1] = value;
                return oldValue;
            }
        }

        final int s = size + 1;
        // Use optimized form of 3 * s.
        // Next capacity is len, 2 * current capacity.
        if (s + (s << 1) > len && resize(len))
            continue retryAfterResize;

        modCount++;
        tab[i] = k;
        tab[i + 1] = value;
        size = s;
        return null;
    }
}
```

从 put 方法可以看出，key 和 value 全部存储在数组上，而且相邻

```java
private boolean resize(int newCapacity) {
    int newLength = newCapacity * 2;
    Object[] oldTable = table;
    int oldLength = oldTable.length;
    if (oldLength == 2 * MAXIMUM_CAPACITY) { // can't expand any further
        if (size == MAXIMUM_CAPACITY - 1)
            throw new IllegalStateException("Capacity exhausted.");
        return false;
    }
    if (oldLength >= newLength)
        return false;

    Object[] newTable = new Object[newLength];

    for (int j = 0; j < oldLength; j += 2) {
        Object key = oldTable[j];
        if (key != null) {
            Object value = oldTable[j+1];
            oldTable[j] = null;
            oldTable[j+1] = null;
            int i = hash(key, newLength);
            while (newTable[i] != null)
                i = nextKeyIndex(i, newLength);
            newTable[i] = key;
            newTable[i + 1] = value;
        }
    }
    table = newTable;
    return true;
}
```

IdentityHashMap 只用数组存储元素。所以扩容阈值较小。put方法的扩容判断是`s + (s << 1) > len`这句话，如果元素数量达到容量的三分之二就要进行扩容操作，新数组长度是旧数组长度的二倍。`retryAfterResize`是java里的`goto`在扩容完成前进行自旋。

```java
private static int nextKeyIndex(int i, int len) {
    return (i + 2 < len ? i + 2 : 0);
}
```

在插入元素时每次跳跃2个索引位置。

判断key相等的时候直接用 `==`判断。如果希望获取到字段值全部相等的两个对象的时候， 就可以使用此类。以为该类只判断地址是否相等。



### 总结

IdentityHashMap比较特殊，通过`hash`确定位置，`==`来获取key。所有元素只存储在数组中，没有HashMap中bucket的概念。并且构造方法里面的期望值参数，也不是数组的初始值。而是扩大的四倍的2次幂数。