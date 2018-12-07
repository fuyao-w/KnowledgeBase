## LinkedHashMap

```
public class LinkedHashMap<K,V> extends HashMap<K,V>
    implements Map<K,V>
```

### java doc

Map接口的哈希表和链表实现，具有可预测的迭代顺序。 此实现与HashMap的不同之处在于它维护了一个贯穿其所有条目的双向链表。 此链接列表定义迭代排序，通常是键插入映射的顺序（插入顺序）。 请注意，如果将键重新插入Map，则不会影响插入顺序。 （如果m.containsKey（k）在调用之前立即返回true，则调用m.put（k，v）时，将密钥k重新插入到映射m中。）

此实现使其客户端免受HashMap（和Hashtable）提供的未指定的，通常混乱的排序，而不会导致与TreeMap相关的成本增加。 无论原始Map的实现如何，它都可用于生成与原始Map具有相同顺序的Map副本：

```
   void foo(Map m) {
         Map copy = new LinkedHashMap(m);
         ...
     }
```

如果模块在输入上获取Map，复制它，然后返回其顺序由副本确定的结果，则此技术特别有用。 （客户通常会欣赏按照提交的顺序返回的内容。）
提供了一个特殊的构造函数来创建链接的哈希映射，其迭代顺序是其条目最后一次访问的顺序，从最近访问到最近（访问顺序）。这种Map非常适合构建LRU缓存。调用put，putIfAbsent，get，getOrDefault，compute，computeIfAbsent，computeIfPresent或merge方法会导致访问相应的条目（假设它在调用完成后存在）。如果替换值，则replace方法仅导致条目的访问。 putAll方法为指定映射中的每个映射生成一个条目访问，按照指定映射的条目集迭代器提供键 - 值映射的顺序。没有其他方法可以生成条目访问。特别是，对集合视图的操作不会影响后备映射的迭代顺序。

可以重写removeEldestEntry（Map.Entry）方法，以强制在将新映射添加到映射时自动删除过时映射的策略。

此类提供所有可选的Map操作，并允许null元素。与HashMap一样，它为基本操作（添加，包含和删除）提供了恒定时间性能，假设哈希函数在桶之间正确地分散元素。由于维护链表的额外费用，性能可能略低于HashMap的性能，但有一个例外：对LinkedHashMap的集合视图进行迭代需要与映射大小成比例的时间，无论其容量如何。对HashMap的迭代可能更昂贵，需要与其容量成比例的时间。

链接的哈希映射有两个影响其性能的参数：初始容量和负载因子。它们的定义与HashMap完全相同。但请注意，为此类选择过高的初始容量值的惩罚不如HashMap严重，因为此类的迭代时间不受容量影响。

请注意，此实现不同步。 如果多个线程同时访问链接的哈希映射，并且至少有一个线程在结构上修改了映射，则必须在外部进行同步。 这通常通过在自然封装Map的某个对象上进行同步来实现。 如果不存在此类对象，则应使用Collections.synchronizedMap方法“包装”该映射。 这最好在创建时完成，以防止意外地不同步访问Map：

`​	Map m = Collections.synchronizedMap(new LinkedHashMap(...));`	

结构修改是添加或删除一个或多个映射的任何操作，或者在访问顺序链接的哈希映射的情况下，影响迭代顺序。在插入有序链接散列映射中，仅更改与已包含在映射中的键相关联的值不是结构修改。在访问有序链接哈希映射中，仅使用get查询映射是结构修改。 ）
所有此类的集合视图方法返回的集合的迭代器方法返回的迭代器都是快速失败的：如果在创建迭代器之后的任何时候对映射进行结构修改，除非通过迭代器自己的remove方法，迭代器将抛出ConcurrentModificationException。因此，在并发修改的情况下，迭代器快速而干净地失败，而不是在未来的未确定时间冒任意，非确定性行为的风险。

请注意，迭代器的快速失败行为无法得到保证，因为一般来说，在存在不同步的并发修改时，不可能做出任何硬性保证。失败快速迭代器会尽最大努力抛出ConcurrentModificationException。因此，编写依赖于此异常的程序以确保其正确性是错误的：迭代器的快速失败行为应该仅用于检测错误。

由此类的所有集合视图方法返回的集合的spliterator方法返回的分裂器是后期绑定，失败快速，另外还报告Spliterator.ORDERED。

### 内部类

```
static class Entry<K,V> extends HashMap.Node<K,V> {
    Entry<K,V> before, after;
    Entry(int hash, K key, V value, Node<K,V> next) {
        super(hash, key, value, next);
    }
}
```

LinkedHashMap的在Node的基础上增加了before和after两个变量，变量来确定元素的顺序。

###  字段

```
/**
 * 双向链表的头（最年长）。
 */
transient LinkedHashMap.Entry<K,V> head;

/**
 * 双向链表的尾部（最年轻）。
 */
transient LinkedHashMap.Entry<K,V> tail;

/**
 * 此链接哈希映射的迭代排序方法: 为`true`是按访问顺序，`false`时插入顺序
 */
final boolean accessOrder;
```

### 分析

```
private void linkNodeLast(LinkedHashMap.Entry<K,V> p) {
    LinkedHashMap.Entry<K,V> last = tail;
    tail = p;
    if (last == null)
        head = p;
    else {
        p.before = last;
        last.after = p;
    }
}
```

```
Node<K,V> newNode(int hash, K key, V value, Node<K,V> e) {
    LinkedHashMap.Entry<K,V> p =
        new LinkedHashMap.Entry<>(hash, key, value, e);
    linkNodeLast(p);
    return p;
}
```

LinkedHashMap继承自HashMap，添加了一些处理before,after指针的方法，`linkNodeLast`新创建Node的时候，将最后一个节点赋值给新建节点的`before`变量。

```
void afterNodeRemoval(Node<K,V> e) { // unlink
    LinkedHashMap.Entry<K,V> p =
        (LinkedHashMap.Entry<K,V>)e, b = p.before, a = p.after;
    p.before = p.after = null;
    if (b == null)
        head = a;
    else
        b.after = a;
    if (a == null)
        tail = b;
    else
        a.before = b;
}
```

`afterNodeRemoval`方法在删除节点后，将原节点的前后节点通过after,before变量，连接起来。

```
void afterNodeInsertion(boolean evict) { // possibly remove eldest
    LinkedHashMap.Entry<K,V> first;
    if (evict && (first = head) != null && removeEldestEntry(first)) {
        K key = first.key;
        removeNode(hash(key), key, null, false, true);
    }
}
```

`afterNodeInsertion`是留给开发者使用的方法，`removeEldestEntry`方法默认返回false，如果想实现一个保留固定元素数量的缓存Map，可以继承LinkedHashMap实现`removeEldestEntry`，并在`put`方法中调用`afterNodeInsertion`。

```
void afterNodeAccess(Node<K,V> e) { // move node to last
    LinkedHashMap.Entry<K,V> last;
    if (accessOrder && (last = tail) != e) {
        LinkedHashMap.Entry<K,V> p =
            (LinkedHashMap.Entry<K,V>)e, b = p.before, a = p.after;
        p.after = null;
        if (b == null)
            head = a;
        else
            b.after = a;
        if (a != null)
            a.before = b;
        else
            last = b;
        if (last == null)
            head = p;
        else {
            p.before = last;
            last.after = p;
        }
        tail = p;
        ++modCount;
    }
}
```

`afterNodeAccess`在访问后会将元素放到最后顺序。

## 总结

LinkedHashMap是有序的Map，可以通过`accessOrder`字段，控制其按访问顺序迭代还是按插入顺序迭代。可以通过继承它实现一个最近最少使用的缓存（LRU）。