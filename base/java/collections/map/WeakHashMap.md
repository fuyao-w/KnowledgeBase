## WeakHashMap

```java
public class WeakHashMap<K,V> extends AbstractMap<K,V>
    implements Map<K,V>
```

### java doc 

基于哈希表的Map接口实现，带弱键。当WeakHashMap的密钥不再正常使用时，它将自动被删除。更确切地说，给定密钥的映射的存在不会阻止密钥被垃圾收集器丢弃，即，gc开始，判定回收，最终回收。当一个键被丢弃时，它的条目将被有效地从地图中删除，因此该类的行为与其他Map实现略有不同。
支持空值和空值。该类具有与HashMap类相似的性能特征，并具有与初始容量和负载因子相同的效率参数。

与大多数集合类一样，此类不同步。可以使用Collections.synchronizedMap方法构造同步的WeakHashMap。

此类主要用于使用等于方法的关键对象，使用==运算符测试对象标识。一旦这样的密钥被丢弃，它就永远不会被重新创建，因此以后不可能在WeakHashMap中查找该密钥，并且对其条目已被删除感到惊讶。这个类可以很好地处理其equals方法不基于对象标识的关键对象，例如String实例。但是，使用这种可重新调用的密钥对象，自动删除其密钥已被丢弃的WeakHashMap条目可能会令人困惑。

WeakHashMap类的行为部分取决于垃圾收集器的操作，因此几个熟悉的（但不是必需的）Map不变量不适用于此类。由于垃圾收集器可能随时丢弃密钥，因此WeakHashMap的行为可能就像未知线程正在静默删除条目一样。特别是，即使您在WeakHashMap实例上进行同步并且不调用其mutator方法，size方法也可能会随着时间的推移返回较小的值，因为isEmpty方法返回false然后返回true，以便containsKey方法返回对于给定键，true和更高版本为false，因为get方法返回给定键的值但后来返回null，put方法返回null，remove方法返回false，前面看来是在映射，以及对密钥集，值集合和条目集的连续检查，以连续产生较少数量的元素。

WeakHashMap中的每个关键对象都间接存储为弱引用的引用对象。因此，只有在垃圾收集器清除了对映射内部和外部的弱引用之后，才会自动删除密钥。

实现说明：WeakHashMap中的值对象由普通的强引用保存。因此，应该注意确保值对象不直接或间接地强烈引用它们自己的密钥，因为这将防止密钥被丢弃。请注意，值对象可以通过WeakHashMap本身间接引用其键;也就是说，值对象可以强烈地引用一些其他关键对象，其关联的值对象又强烈地引用第一值对象的关键字。如果映射中的值不依赖于持有对它们的强引用的映射，则处理此问题的一种方法是在插入之前将值本身包装在WeakReferences中，如：m.put（key，new WeakReference（value）），然后解开每一个。

所有这个类的“集合视图方法”返回的集合的迭代器方法返回的迭代器是快速失败的：如果在创建迭代器之后的任何时候对映射进行结构修改，除非通过迭代器自己的删除方法，迭代器将抛出ConcurrentModificationException。因此，在并发修改的情况下，迭代器快速而干净地失败，而不是在未来的未确定时间冒任意，非确定性行为的风险。

请注意，迭代器的快速失败行为无法得到保证，因为一般来说，在存在不同步的并发修改时，不可能做出任何硬性保证。失败快速迭代器会尽最大努力抛出ConcurrentModificationException。因此，编写依赖于此异常的程序以确保其正确性是错误的：迭代器的快速失败行为应该仅用于检测错误。

### 字段

```java
/**
 * 已被回收虚引用对象的WeakEntries的引用队列
 */
private final ReferenceQueue<Object> queue = new ReferenceQueue<>();
```

```java
/**
 * 表示表内的空键的值.
 */
private static final Object NULL_KEY = new Object();
```

其他的字段跟HashMap相同，queue对象是被回收的虚引用对象的WeekReference队列,需要通过此队列将，没有用的WeekReference对象清除。

NULL_KEY代表key为null。用于区分key为null是，没有这个键，开始null被作为了键。
### 分析

```java
 private static Object maskNull(Object key) {
        return (key == null) ? NULL_KEY : key;
    }
```
通过maskNull方法，可以区分出
```java
private static class Entry<K,V> extends WeakReference<Object> implements Map.Entry<K,V> {
    V value;
    final int hash;
    Entry<K,V> next;

    /**
     * Creates new entry.
     */
    Entry(Object key, V value,
          ReferenceQueue<Object> queue,
          int hash, Entry<K,V> next) {
        super(key, queue);
        this.value = value;
        this.hash  = hash;
        this.next  = next;
    }

    @SuppressWarnings("unchecked")
    public K getKey() {
        return (K) WeakHashMap.unmaskNull(get());
    }

    public V getValue() {
        return value;
    }

    public V setValue(V newValue) {
        V oldValue = value;
        value = newValue;
        return oldValue;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Map.Entry))
            return false;
        Map.Entry<?,?> e = (Map.Entry<?,?>)o;
        K k1 = getKey();
        Object k2 = e.getKey();
        if (k1 == k2 || (k1 != null && k1.equals(k2))) {
            V v1 = getValue();
            Object v2 = e.getValue();
            if (v1 == v2 || (v1 != null && v1.equals(v2)))
                return true;
        }
        return false;
    }

    public int hashCode() {
        K k = getKey();
        V v = getValue();
        return Objects.hashCode(k) ^ Objects.hashCode(v);
    }

    public String toString() {
        return getKey() + "=" + getValue();
    }
}
```

WeakHashMap的结构与HashMap基本相同，不同的是WeakHashMap的Entry数组继承自WeakReference，在Entry的构造函数中`super(key, queue);`是调用WeakReference的构造方法。将key包装成虚引用对象。并且通过`expungeStaleEntries`清除弱引用对象已经被回收的WeekedReference对象。

 ```java
    private void expungeStaleEntries() {
        for (Object x; (x = queue.poll()) != null; ) {
            synchronized (queue) {
                @SuppressWarnings("unchecked")
                    Entry<K,V> e = (Entry<K,V>) x;
                int i = indexFor(e.hash, table.length);
    
                Entry<K,V> prev = table[i];
                Entry<K,V> p = prev;
                while (p != null) {
                    Entry<K,V> next = p.next;
                    if (p == e) {
                        if (prev == e)
                            table[i] = next;
                        else
                            prev.next = next;
                        // Must not null out e.next;
                        // stale entries may be in use by a HashIterator
                        e.value = null; // Help GC
                        size--;
                        break;
                    }
                    prev = p;
                    p = next;
                }
            }
        }
    }
```