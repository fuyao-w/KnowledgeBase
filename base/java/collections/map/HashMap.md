## HashMap ##

```java
public class HashMap<K,V> extends AbstractMap<K,V>
    implements Map<K,V>, Cloneable, Serializable 
```

### java doc ###

基于哈希表的Map接口实现。此实现提供所有可选的映射操作，并允许空值和空键。 （HashMap类大致相当于Hashtable，除了它是不同步的并且允许空值。）这个类不保证Map的顺序;特别是，它不保证订单会随着时间的推移保持不变。
假设散列函数在桶之间正确地分散元素，该实现为基本操作（get和put）提供了恒定时间性能。对集合视图的迭代需要与HashMap实例的“容量”（桶的数量）加上其大小（键 - 值映射的数量）成比例的时间。因此，如果迭代性能很重要，则不要将初始容量设置得太高（或负载因子太低）非常重要。

HashMap的一个实例有两个影响其性能的参数：初始容量和负载因子。容量是哈希表中的桶数，初始容量只是创建哈希表时的容量。加载因子是在自动增加容量之前允许哈希表获取的完整程度的度量。当哈希表中的条目数超过加载因子和当前容量的乘积时，哈希表将被重新哈希（即，重建内部数据结构），以便哈希表具有大约两倍的桶数。

作为一般规则，默认加载因子（.75）在时间和空间成本之间提供了良好的折衷。较高的值会减少空间开销，但会增加查找成本（反映在HashMap类的大多数操作中，包括get和put）。在设置其初始容量时，应考虑映射中的预期条目数及其负载因子，以便最小化重新散列操作的数量。如果初始容量大于最大条目数除以加载因子，则不会发生重新加载操作。

如果要将多个映射存储在HashMap实例中，则使用足够大的容量创建映射将允许映射更有效地存储，而不是根据需要执行自动重新散列来扩展表。请注意，使用具有相同`hashCode()`的许多键可以使任何哈希表的性能下降。为了改善影响，当键是可比较时，该类可以使用键之间的比较顺序来帮助打破关系。

请注意，此实现不同步。如果多个线程同时访问哈希映射，并且至少有一个线程在结构上修改了映射，则必须在外部进行同步。 （结构修改是添加或删除一个或多个映射的任何操作;仅更改与实例已包含的键相关联的值不是结构修改。）这通常通过同步自然封装映射的某个对象来完成。 。如果不存在此类对象，则应使用Collections.synchronizedMap方法“包装”该映射。这最好在创建时完成，以防止意外地不同步访问Map：

```java
  Map m = Collections.synchronizedMap(new HashMap(...));
```

所有这个类的“集合视图方法”返回的迭代器都是快速失败的：如果在创建迭代器之后的任何时候对映射进行结构修改，除了通过迭代器自己的remove方法之外，迭代器将抛出ConcurrentModificationException。。 因此，在并发修改的情况下，迭代器快速而干净地失败，而不是在未来的未确定时间冒任意，非确定性行为的风险。

请注意，迭代器的快速失败行为无法得到保证，因为一般来说，在存在不同步的并发修改时，不可能做出任何硬性保证。 失败快速迭代器会尽最大努力抛出`ConcurrentModificationException`。 因此，编写依赖于此异常的程序以确保其正确性是错误的：迭代器的快速失败行为应该仅用于检测错误。

### 静态类

```JAVA
static class Node<K,V> implements Map.Entry<K,V> {

        final int hash;
        final K key;
        V value;
        Node<K,V> next;
}
```

该类是基本哈希bin节点，

### 字段 ### 

实施说明。

此map通常用作箱（bucket）哈希表，但是 当bins变得太大时，bins转换成 TreeNodes，与java.util.TreeMap中的结构类似 。 大多数方法都尝试使用bins，但是 适用时中继到TreeNode方法（只需检查 节点的实例）。 TreeNodes的Bin可以像其他任何一样遍历和使用，但是当元素过多时还支持更快的查找。 但是，由于绝大多数的bins 正常使用不会充满满容量，检查 tree bins的存在可能会在table方法中延迟。 树容器（其元素都是TreeNodes的bin） 主要由hashCode排序，但在ties的情况下，如果两个
元素是相同的“C类实现Comparable <C>”， 然后使用他们的`compareTo`方法进行排序。 （我们 通过反射保守地检查泛型类型以进行验证 。 请参阅compareClassFor方法。 增加的复杂性 在提供最坏情况O（log n）时，tree bin 的价值是值得的。 当key 钥具有不同的哈希值或有序时，树箱的额外复杂性值得提供最坏情况的O（log n）操作，因此，在hashCode（）方法返回分布不佳的值的意外或恶意用法中，性能会优雅地降级， 返回分布不均的值以及中的值。 以及许多键共享hashCode的那些，只要它们也是可比较的。
 （如果这些都不适用，与不采取预防措施相比，我们可能在时间和空间上浪费大约两倍。 但是，唯一已知的案例源于糟糕的用户编程实践，这些实践已经非常缓慢，这几乎没有什么区别。）

因为TreeNodes大约是常规节点大小的两倍，所以我们 仅当 bins 包含足够的节点以保证使用时才使用它们 （见TREEIFY_THRESHOLD）。 当它们变得太小时（由于 移除或调整大小）它们被转换回普通箱。在具有良好分布的`hashCodes`的用法中，tree是 很少用。 理想情况下，在随机hashCodes下，频率为 箱中的节点遵循泊松分布带有 默认大小调整的平均参数约为0.5 阈值为0.75，虽然因为有很大的差异 调整粒度。 忽略方差，预期
列表大小k的出现是

（exp（-0.5）pow（0.5，k）/ factorial（K））

 第一个值是：

0:    0.60653066<p>
1:    0.30326533<p>
2:    0.07581633<p>
3:    0.01263606<p>
4:    0.00157952<p>
5:    0.00015795<p>
6:    0.00001316<p>
7:    0.00000094<p>
8:    0.00000006<p>
更多情况：不到千万分之一

树bin的根通常是它的第一个节点。 然而， 有时（目前仅在Iterator.remove上），根可能
在其他地方，但可以在父链接后恢复 （方法`TreeNode.root()`）。

所有适用的内部方法都接受哈希码作为参数（通常由公共方法提供），允许他们互相调用而不重新计算`hashCodes`。
大多数内部方法也接受“tab”参数，这通常是当前table，但在调整大小或转换时可能是新的或旧的。

当bin列表被树化，拆分或未解析时，我们会保留 它们处于相同的相对访问/遍历顺序（即字段 Node.next）以更好地保留局部性，并略微保持局部性 简化对调用的拆分和遍历的处理 iterator.remove。 在插入时使用比较器时，要保持一个 总排序（或尽可能接近） 重新平衡，我们比较类和`identityHashCodes`为 搭配断路器。

他在普通与树模式之间使用和转换是 复杂的子类LinkedHashMap的存在。 看到 下面是定义为在插入时调用的钩子方法， 删除和访问允许LinkedHashMap内部 否则保持独立于这些机制。 （这还要求将映射实例传递给可能创建新节点的一些实用程序方法。）

类似于并发编程的基于SSA的编码风格有助于避免在所有扭曲指针操作中出现混叠错误。



```
/**
 * 默认初始容量 - 必须是2的幂。
 */
static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // 16
```

```java
/**
 * 如果具有参数的任一构造函数隐式指定较高值，则使用最大容量。
 * 2 的次幂 <= 1<<30.
 */
static final int MAXIMUM_CAPACITY = 1 << 30;
```



```java
/**
 * 在构造函数中未指定时使用的加载因子。
 */
static final float DEFAULT_LOAD_FACTOR = 0.75f;
```

```java
/**
  *bin计数阈值，用于使用树而不是bin的列表. 
  *将元素添加到具有至少这么多节点的bin时，bin被转换为树。
  *该值必须大于2并且应该至少为8以与树木移除中的假设相关联，以便在收缩时转换   *回普通bin。
  */
static final int TREEIFY_THRESHOLD = 8;
```

```java
/**
 * 用于在调整大小操作期间解除（拆分）bin的bin计数阈值。
 * 应小于TREEIFY_THRESHOLD，最多6个与去除时的收缩检测啮合。
 */
static final int UNTREEIFY_THRESHOLD = 6;
```

```java
/**
 * 容器可以树化的最小表容量。
 *（否则，如果bin中的节点太多，则会调整表的大小。）
 *应该至少4 * TREEIFY_THRESHOLD以避免
 *调整大小和树化阈值之间关系的冲突。
 */
static final int MIN_TREEIFY_CAPACITY = 64;
```

```java
/**
 * 该表在首次使用时初始化，并根据需要调整大小。 分配时，长度始终是2的幂。    	*（我们还在一些操作中容忍长度为零，以允许当前不需要的自举机制.)
 */
transient Node<K,V>[] table;

/**
*保持缓存的entrySet（）。 请注意，AbstractMap字段用于keySet（）和  	  *`values（）`。
*/
transient Set<Map.Entry<K,V>> entrySet;

/**
 * 此映射中包含的键 - 值映射的数量。
 */
transient int size;


transient int modCount;

/**
 * 要调整大小的下一个大小值（capacity * load factor）。
 * 扩容的阈值
 */

int threshold;

/**
 * hash表的加载因子
 */
final float loadFactor;
```





### 分析 ###

通过HashMap里的字段和内部类`Node`可以分析出，HashMap是一个Node数组+单向链表+树组成的结构。其中Node代表链表的的一个节点。几个常量DEFAULT_INITIAL_CAPACITY代表HashMap的默认数组长度16，DEFAULT_LOAD_FACTOR是加载因子，加载因子*数组长度代表HashMap扩容的阈值。MIN_TREEIFY_CAPACITY是HashMap里数组里链表转化成树的最小数组长度64。TREEIFY_THRESHOLD是在数组长度大于64后数组索引上的链表可以转化成树的最小链表长度。TREEIFY_THRESHOLD  当索引上的树重新转换成链表的最小元素数量。MAXIMUM_CAPACITY是数组最大长度。

### 构造方法

```java
public HashMap(int initialCapacity, float loadFactor) {
    if (initialCapacity < 0)
        throw new IllegalArgumentException("Illegal initial capacity: " +
                                           initialCapacity);
    if (initialCapacity > MAXIMUM_CAPACITY)
        initialCapacity = MAXIMUM_CAPACITY;
    if (loadFactor <= 0 || Float.isNaN(loadFactor))
        throw new IllegalArgumentException("Illegal load factor: " +
                                           loadFactor);
    this.loadFactor = loadFactor;
    this.threshold = tableSizeFor(initialCapacity);
}
```

```java
//返回与给定值最接近的数字。这个数字为2的整数次幂。
static final int tableSizeFor(int cap) {
    int n = -1 >>> Integer.numberOfLeadingZeros(cap - 1);
    return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
}
```

```java
//指定int值的二进制补码二进制表示中最高位（“最左侧”）一位之前的零位数，如果该值等于零，则为32。
public static int numberOfLeadingZeros(int i) {
    // HD, Count leading 0's
    if (i <= 0)
        return i == 0 ? 32 : 0;
    int n = 31;
    if (i >= 1 << 16) { n -= 16; i >>>= 16; }
    if (i >= 1 <<  8) { n -=  8; i >>>=  8; }
    if (i >= 1 <<  4) { n -=  4; i >>>=  4; }
    if (i >= 1 <<  2) { n -=  2; i >>>=  2; }
    return n - (i >>> 1);
}
```

构造方法与1.8有了变化。在初始化的时候前面的都好理解，初始化了加载因子。threshold值是通过`tableSizeFor`方法返回的，该方法会返回一个给定的初始值的最接近的数字，这个数字为2的整数次幂。

接下来分析`put()`:

### put ###

```java
public V put(K key, V value) {
    return putVal(hash(key), key, value, false, true);
}
计算key.hashCode（）并将散列（XOR）更高的散列位降低。 因为该表使用2次幂掩蔽，所以仅在当前掩码之上的位中变化的散列组将始终发生冲突。 （在已知的例子中是在小表中保存连续整数的浮点键集。）因此我们应用一个向下扩展高位比特影响的变换。 在速度，效用和比特扩展质量之间存在权衡。 因为许多常见的哈希集合已经合理分布（因此不会受益于传播），并且因为我们使用树来处理容器中的大量冲突，所以我们只是以最便宜的方式对一些移位的位进行异或，以减少系统损失， 以及由于表格边界而包含最高位的影响，否则这些位将永远不会用于索引计算。
通过位运算在hash值为2的16次幂的后会将hash值扩大。
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}

```

```java
final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
               boolean evict) {
    Node<K,V>[] tab; Node<K,V> p; int n, i;
    if ((tab = table) == null || (n = tab.length) == 0)
        n = (tab = resize()).length;
    if ((p = tab[i = (n - 1) & hash]) == null)
        tab[i] = newNode(hash, key, value, null);
    else {
        Node<K,V> e; K k;
        if (p.hash == hash &&
            ((k = p.key) == key || (key != null && key.equals(k))))
            e = p;
        else if (p instanceof TreeNode)
            e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
        else {
            for (int binCount = 0; ; ++binCount) {
                if ((e = p.next) == null) {
                    p.next = newNode(hash, key, value, null);
                    if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                        treeifyBin(tab, hash);
                    break;
                }
                if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k))))
                    break;
                p = e;
            }
        }
        if (e != null) { // existing mapping for key
            V oldValue = e.value;
            if (!onlyIfAbsent || oldValue == null)
                e.value = value;
            afterNodeAccess(e);
            return oldValue;
        }
    }
    ++modCount;
    if (++size > threshold)
        resize();
    afterNodeInsertion(evict);
    return null;
}
```

`put()`首先会重新计算key的hash值，目的是为了将key更加均匀的散列在数组中，防止有些位置永远不会被用于索引计算。在`putVal()`开始，先判断table数组是否为空或者长度为0。因为在之前的构造方法中，没有对table数组进行操作，所以此时table数组为空。会调用`resize()`方法：

#### resize ####

```java
final Node<K,V>[] resize() {
    Node<K,V>[] oldTab = table;
    int oldCap = (oldTab == null) ? 0 : oldTab.length;
    int oldThr = threshold;
    int newCap, newThr = 0;
    if (oldCap > 0) {
        if (oldCap >= MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return oldTab;
        }
        else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                 oldCap >= DEFAULT_INITIAL_CAPACITY)
            newThr = oldThr << 1; // double threshold
    }
    else if (oldThr > 0) // initial capacity was placed in threshold
        newCap = oldThr;
    else {               // zero initial threshold signifies using defaults
        newCap = DEFAULT_INITIAL_CAPACITY;
        newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
    }
    if (newThr == 0) {
        float ft = (float)newCap * loadFactor;
        newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                  (int)ft : Integer.MAX_VALUE);
    }
    threshold = newThr;
    @SuppressWarnings({"rawtypes","unchecked"})
    Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
    table = newTab;
    if (oldTab != null) {
      // 省略
                }
            }
        }
    }
    return newTab;
}
```

`resize()`方法的作用是：初始化或加倍表格大小。

初始化：table数组为空，oldThr如果不为0则为设置的初始容量，所以将oldThr赋值给newCap，也就是将table的数组长度确定为我们设置的大小。然后用设置的加载因子计算newThr并赋值给threshold，如果没设置初始容量，那么就会用默认的值去初始化数组和threshold。，在这里看出，table数组的初始化是延迟到第一次`put()`的时候进行的。**并且threshold刚开始保存的是初始数组容量**。

接下来会创建一个新节点，并且根据新的容量创建新的数组。如果是第一次创建，则不需要将旧数组上的数据中心hash到新的数组上的过程直接返回。

扩容:扩容的时候table长度大于0，数组容量和threshold都会进行二倍扩容。然后将旧数组上元素迁移到新数组上。分为三种情况：

```  java
//resize方法省略部分  
for (int j = 0; j < oldCap; ++j) {
            Node<K,V> e;
            if ((e = oldTab[j]) != null) {
                oldTab[j] = null;
                if (e.next == null)
                    newTab[e.hash & (newCap - 1)] = e;
                else if (e instanceof TreeNode)
                    ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                else { // preserve order
                    Node<K,V> loHead = null, loTail = null;
                    Node<K,V> hiHead = null, hiTail = null;
                    Node<K,V> next;
                    do {
                        next = e.next;
                        if ((e.hash & oldCap) == 0) {
                            if (loTail == null)
                                loHead = e;
                            else
                                loTail.next = e;
                            loTail = e;
                        }
                        else {
                            if (hiTail == null)
                                hiHead = e;
                            else
                                hiTail.next = e;
                            hiTail = e;
                        }
                    } while ((e = next) != null);
                    if (loTail != null) {
                        loTail.next = null;
                        newTab[j] = loHead;
                    }
                    if (hiTail != null) {
                        hiTail.next = null;
                        newTab[j + oldCap] = hiHead;
                    }
```

​	

1. 只有一个节点,会被重新hash（e.hash & (newCap - 1)）,新位置有两种情况，hash到原位置，2次幂的偏移量hash到新的位置。

2. 链表：当索引位置是链表的时候。分为三种情况：
   1. 元素在与 oldCapacity 进行 hash后的索引位置为0，这种情况出现于 `（key.hashCode=oldCap）*n+n & oldCap`这种情况，会被重新分配在新数组相对于旧数组相同的位置上。
   2. 元素在旧数组上索引位置不为0，会被重新分配在旧数组位置+oldCap的新位置。
      1. 原来在hash在0索引位置的Node的key值，被修改后重新hash不在0索引位置，和原来hash不在0索引位置的Node重新hash后在0索引位置。

   ```java
   Node<K,V> loHead = null, loTail = null;
   Node<K,V> hiHead = null, hiTail = null;
   ```

   为了将链表中心分配在新数组，声明了四个变量。loHead,loTail对应第一种情								  况，hiHead，hiTail对应第二种情况。

   前两种情况，lo/hiTail节点只是向后移动,第三种情况以图片为例：

   ![](https://github.com/TransientWang/KnowledgeBase/blob/master/picture/HashMap_resize().png)

- `e节点`是链表的首节点，也是当前节点。它的hash在索引0的位置。这时候`loHead`,`loTail`都被赋值`e`。
- 然后`e节点`后面`f节点`成为新的当前节点。此时`loHead.next`、`loTail.next`都指向g。
- `f节点`重新hash在索引1的位置。hiHead,hiTail被赋值为f，成为一条新链表的首节点。此时`hiHead.next`、`hiTail.next`都指向`g`。
- 接下来`g`又成为了新的当前节点。
- `g`重新hash到0的位置，此时`loTail`=`e`，`loTail.next`指向当前节点`g`。并将`loTaol`移动到当前节点`g`。这样就略过了`f节点`。

- 当循环结束时候进行两次判断，第一次将索引在0位置的新链表的loTail置空，放置在新数组相对于旧数组的相同位置j,第二次将索引在1位置的新链表Tail置空，注意，之前`hiHead`,`hiTail`的下一节点都指向`f`。如果不将尾部断开会形成二叉树，这是错误的。

3. 索引位置是树：对于树的操作与链表基本是一样的，还会判断当数元素数量小于6的时候将树还原成链表。

#### resize()结束 

重新回到`putVal()`方法当中，`resize()`方法之后判断，新元素的hash计算的索引位置是否为空，为空直接包装一个Node节点。赋值给索引上就可以了。

如果该索引位置不为空，则会判断该Node节点是否与新加入的key相同，相同就可以替换返回旧值。如果不相同，则会判断该节点是否是树，是的话调用`putTreeVal()`替换或者直接插入，不是的话会遍历当前链表，找到key相同的node节点或者或者将新节点放到链表末尾。新插入的好话就要判断该索引也就是bucket上的元素数量是否达到了扩容标准，元素数量是否大于8，和table数组长度是否大于64，如果一个标准没达到，则扩容。如果可以扩容，则将Node链表替换成TreeNode链表再进行转换数操作。

如果是新插入节点，则会最后进行一次是否应该扩容的判断。

源码很简单，但是需要注意的是在遇到需要遍历链表才能插入的情况时，新节点的插入方式是***尾部插入***，
而在1.8之前是头部插入

#### putVal()结束 ####

#### get() ###

```java
final Node<K,V> getNode(int hash, Object key) {
    Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (first = tab[(n - 1) & hash]) != null) {
        if (first.hash == hash && // always check first node
            ((k = first.key) == key || (key != null && key.equals(k))))
            return first;
        if ((e = first.next) != null) {
            if (first instanceof TreeNode)
                return ((TreeNode<K,V>)first).getTreeNode(hash, key);
            do {
                if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k))))
                    return e;
            } while ((e = e.next) != null);
        }
    }
    return null;
}
```

对于`get()`需要了解的是，必须是key的hash值相等，并且调用`equals`方法也返回true的时候才能证明这就是需要的key。



## 总结 ## 

HashMap的实现逻辑较为简单，但是有一个问题需要考虑，**这里的jdk源码都是 `java11`的，结构较8又有了变化。在`java8`之前HashMap在transfer()函数将旧数组元素迁移到新数组的时候，链表的顺序会被颠倒。而HashMap是非线程安全的，在多线程的情况下可能会出现链表成环的现象，从`get`时造成死循环。11里面链表的顺序没有被改变，不会出现链表成环的操作**
还有一个问题，是为什么数组的容量必须为2的次幂呢？回顾`putVal()`在定位新元素位置是怎么做的，
`(n- 1) & hash`它就相当于取模操作，但是当n为奇数的时候不能正确的获得与取模操作一样的结果。
所以，要让数组的容量为2的次幂。最后留意一下现在插入链表的方式是尾部插入。