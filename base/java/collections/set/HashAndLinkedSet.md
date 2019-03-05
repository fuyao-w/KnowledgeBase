## HashSet ##

```java
public class HashSet<E> extends AbstractSet<E>
       implements Set<E>, Cloneable, java.io.Serializable
```

### java doc ###

此类实现Set接口，由哈希表（实际上是HashMap实例）支持。它不保证集合的迭代顺序;特别是，它不保证顺序会随着时间的推移保持不变。该类允许null元素。
该类为基本操作（添加，删除，包含和大小）提供恒定的时间性能，假设散列函数在桶之间正确地分散元素。迭代此集合需要的时间与HashSet实例的大小（元素数量）加上后备HashMap实例的“容量”（桶数）之和成比例。因此，如果迭代性能很重要，则不要将初始容量设置得太高（或负载因子太低）非常重要。

请注意，此实现不同步。如果多个线程同时访问哈希集，并且至少有一个线程修改了该集，则必须在外部进行同步。这通常通过在自然封装集合的某个对象上进行同步来实现。如果不存在此类对象，则应使用Collections.synchronizedSet方法“包装”该集合。这最好在创建时完成，以防止对集合的意外不同步访问：

    Set s = Collections.synchronizedSet（new HashSet（...））;

这个类的迭代器方法返回的迭代器是快速失败的：如果在创建迭代器后的任何时候修改了集合，除了通过迭代器自己的remove方法之外，迭代器抛出一个ConcurrentModificationException。因此，在并发修改的情况下，迭代器快速而干净地失败，而不是在未来的未确定时间冒任意，非确定性行为的风险。

请注意，迭代器的快速失败行为无法得到保证，因为一般来说，在存在不同步的并发修改时，不可能做出任何硬性保证。失败快速迭代器会尽最大努力抛出ConcurrentModificationException。因此，编写依赖于此异常的程序以确保其正确性是错误的：迭代器的快速失败行为应该仅用于检测错误。

### 字段 ###


```java
private transient HashMap<E,Object> map;

// Dummy value to associate with an Object in the backing Map
private static final Object PRESENT = new Object();
```

### 构造方法 ###

```java
public HashSet() {
    map = new HashMap<>();
}

public HashSet(Collection<? extends E> c) {
    map = new HashMap<>(Math.max((int) (c.size()/.75f) + 1, 16));
    addAll(c);
}
//构造一个新的空链接哈希集。 （此包私有构造函数仅由LinkedHashSet使用。）
//后备HashMap实例是具有指定初始容量和指定加载因子的LinkedHashMap。
HashSet(int initialCapacity, float loadFactor, boolean dummy) {
    map = new LinkedHashMap<>(initialCapacity, loadFactor);
}
```

### 分析 ###



从构造方法看出，HashSet是hashMap的封装，而且他还有包私有的构造方法将LinkedHashMap作为存储元素的容器，注释说明这是为了构造LinkedHashSet准备的。
如果根据其他的集合获取HashSet，那么他的初始化长度为，集合的长度/0.75 +1 与16的最大值。

另外构造方法里有一个布尔类型的dummy参数。被注释为忽略（将此构造函数与其他int，float构造函数区分开来）它在目前的版本中没有用处。



## LinkedHashSet ##

Set接口的哈希表和链表实现，具有可预测的迭代顺序。 此实现与HashSet的不同之处在于它维护了一个贯穿其所有条目的双向链表。 此链接列表定义迭代排序，即元素插入集合（插入顺序）的顺序。 请注意，如果将元素重新插入到集合中，则不会影响插入顺序。 （如果s.contains（e）在调用之前立即返回true，则调用s.add（e）时，将元素e重新插入到集合中。）

此实现使其客户端免受HashSet提供的未指定的，通常是混乱的排序，而不会导致与TreeSet相关的增加的成本。 无论原始集合的实现如何，它都可用于生成与原始集合具有相同顺序的集合的副本：

```java
 void foo(Set s) {
     Set copy = new LinkedHashSet(s);
     ...
 }
```

如果模块在输入上获取集合，复制它，然后返回其顺序由副本确定的结果，则此技术特别有用。 （客户通常会欣赏按照提交的顺序返回的内容。）
此类提供所有可选的Set操作，并允许null元素。与HashSet一样，它为基本操作（添加，包含和删除）提供了恒定时间性能，假设散列函数在桶之间正确地分散元素。由于维护链表的额外费用，性能可能略低于HashSet的性能，但有一个例外：对LinkedHashSet的迭代需要与集合大小成比例的时间，无论其容量如何。对HashSet的迭代可能更昂贵，需要与其容量成比例的时间。

链接的哈希集有两个影响其性能的参数：初始容量和加载因子。它们的定义与HashSet完全相同。但请注意，为此类选择过高的初始容量值的代价不如HashSet严重，因为此类的迭代次数不受容量影响。

请注意，此实现不同步。如果多个线程同时访问链接的哈希集，并且至少有一个线程修改了该集，则必须在外部进行同步。这通常通过在自然封装集合的某个对象上进行同步来实现。如果不存在此类对象，则应使用Collections.synchronizedSet方法“包装”该集合。这最好在创建时完成，以防止对集合的意外不同步访问：

    Set s = Collections.synchronizedSet(new LinkedHashSet(...));

这个类的迭代器方法返回的迭代器是快速失败的：如果在创建迭代器之后的任何时候修改了set，除了通过迭代器自己的remove方法之外，迭代器将抛出ConcurrentModificationException。 因此，在并发修改的情况下，迭代器快速而干净地失败，而不是在未来的未确定时间冒任意，非确定性行为的风险。

请注意，迭代器的快速失败行为无法得到保证，因为一般来说，在存在不同步的并发修改时，不可能做出任何硬性保证。 失败快速迭代器会尽最大努力抛出ConcurrentModificationException。 因此，编写依赖于此异常的程序以确保其正确性是错误的：迭代器的快速失败行为应该仅用于检测错误。

### 构造方法 ###

```java
public class LinkedHashSet<E>
    extends HashSet<E>
    implements Set<E>, Cloneable, java.io.Serializable {
    
public LinkedHashSet(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor, true);
    }

public LinkedHashSet(int initialCapacity) {
    super(initialCapacity, .75f, true);
}

public LinkedHashSet() {
    super(16, .75f, true);
}

public LinkedHashSet(Collection<? extends E> c) {
    super(Math.max(2*c.size(), 11), .75f, true);
    addAll(c);
}
```



```java
HashSet(int initialCapacity, float loadFactor, boolean dummy) {
    map = new LinkedHashMap<>(initialCapacity, loadFactor);
}
```

LinkedHashSet直接继承了HashSet，通过HashSet 的构造方法通过 LinkedHashMap 创建内部容器。可以看出他默认的容量是16，如果通过其他集合构造的话，最小是11和二倍数组容量的最大值。加载因子都是0.75。

