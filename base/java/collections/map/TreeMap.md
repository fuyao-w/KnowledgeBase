## TreeMap

```java
public class TreeMap<K,V>
    extends AbstractMap<K,V>
    implements NavigableMap<K,V>, Cloneable, java.io.Serializable
```

### java doc

基于红黑树的NavigableMap实现。Map根据其键的自然顺序进行排序，或者通过在Map创建时提供的比较器进行排序，具体取决于使用的构造函数。
此实现为containsKey，get，put和remove操作提供了有保证的log（n）时间成本。算法是对Cormen，Leiserson和Rivest的算法导论中的算法的改编。

请注意，如果此有序映射要正确实现Map接口，则树映射维护的顺序（如任何有序映射，以及是否提供显式比较器）必须与equals一致。 （有关与equals一致的精确定义，请参见Comparable或Comparator。）这是因为Map接口是根据equals操作定义的，但是有序映射使用compareTo（或compare）方法执行所有键比较，因此从排序映射的角度来看，通过此方法被视为相等的键是相等的。即使排序与equals不一致，也可以很好地定义有序映射的行为。它只是没有遵守Map接口的一般合同。

请注意，此实现不同步。 如果多个线程同时访问映射，并且至少有一个线程在结构上修改了映射，则必须在外部进行同步。 （结构修改是添加或删除一个或多个映射的任何操作;仅仅更改与现有键关联的值不是结构修改。）这通常通过在自然封装映射的某个对象上进行同步来实现。 如果不存在此类对象，则应使用Collections.synchronizedSortedMap方法“包装”映射。 这最好在创建时完成，以防止意外地不同步访问Map：

```java
SortedMap m = Collections.synchronizedSortedMap(new TreeMap(...));
```

所有这个类的“集合视图方法”返回的集合的迭代器方法返回的迭代器是快速失败的：如果在创建迭代器之后的任何时候对映射进行结构修改，除非通过迭代器自己的删除方法，迭代器将抛出ConcurrentModificationException。因此，在并发修改的情况下，迭代器快速而干净地失败，而不是在未来的未确定时间冒任意，非确定性行为的风险。

请注意，迭代器的快速失败行为无法得到保证，因为一般来说，在存在不同步的并发修改时，不可能做出任何硬性保证。失败快速迭代器会尽最大努力抛出ConcurrentModificationException。因此，编写依赖于此异常的程序以确保其正确性是错误的：迭代器的快速失败行为应该仅用于检测错误。

此类中的方法返回的所有Map.Entry对及其视图表示生成时映射的快照。它们不支持Entry.setValue方法。 （但请注意，可以使用put更改关联映射中的映射。）

### 字段

```java
/**
 * 比较器用于维护此树形图中的顺序，或
 *如果它使用其键的自然顺序，则为null。
 */
private final Comparator<? super K> comparator;

//红黑树的根节点
private transient Entry<K,V> root;

```

### 分析

```java
public TreeMap(Comparator<? super K> comparator) {
    this.comparator = comparator;
}
```

```java
public TreeMap(SortedMap<K, ? extends V> m) {
    comparator = m.comparator();
    try {
        buildFromSorted(m.size(), m.entrySet().iterator(), null, null);
    } catch (java.io.IOException | ClassNotFoundException cannotHappen) {
    }
}
```

TreeMap是排序Map（不是有序，它的本质是红黑树，也就是二叉查找树，插入顺序并一定等于遍历顺序。），TreeSet也是对TreeSet的封装。实现了导航Map,可以`floor`,`ceil`等方法获取最接近的元素。

构造一个TreeMap可以传递进来一个`Comparator`接口。也可以通过另外一个TreeMap，或者其他map构造（需要自己实现）。或者什么也不传，通过让实现`comparable接口的`元素调用`compareTo`进行比较。

```java
static final class Entry<K,V> implements Map.Entry<K,V> {
    K key;
    V value;
    Entry<K,V> left;
    Entry<K,V> right;
    Entry<K,V> parent;
    boolean color = BLACK;
}
```

TreeMap将`Map.Entry`实现为红黑树的节点。



```java
public V put(K key, V value) {
    Entry<K,V> t = root;
    if (t == null) {
        compare(key, key); // type (and possibly null) check

        root = new Entry<>(key, value, null);
        size = 1;
        modCount++;
        return null;
    }
    int cmp;
    Entry<K,V> parent;
    // split comparator and comparable paths
    Comparator<? super K> cpr = comparator;
    if (cpr != null) {
        do {
            parent = t;
            cmp = cpr.compare(key, t.key);
            if (cmp < 0)
                t = t.left;
            else if (cmp > 0)
                t = t.right;
            else
                return t.setValue(value);
        } while (t != null);
    }
    else {
        if (key == null)
            throw new NullPointerException();
        @SuppressWarnings("unchecked")
            Comparable<? super K> k = (Comparable<? super K>) key;
        do {
            parent = t;
            cmp = k.compareTo(t.key);
            if (cmp < 0)
                t = t.left;
            else if (cmp > 0)
                t = t.right;
            else
                return t.setValue(value);
        } while (t != null);
    }
    Entry<K,V> e = new Entry<>(key, value, parent);
    if (cmp < 0)
        parent.left = e;
    else
        parent.right = e;
    fixAfterInsertion(e);
    size++;
    modCount++;
    return null;
}
```

put方法将节点插入树，然后进行调整。

remove也是将节点删除后调整红黑树。

```java
private void deleteEntry(Entry<K,V> p) {
    modCount++;
    size--;

    // If strictly internal, copy successor's element to p and then make p
    // point to successor.
    if (p.left != null && p.right != null) {
        Entry<K,V> s = successor(p);
        p.key = s.key;
        p.value = s.value;
        p = s;
    } // p has 2 children

    // Start fixup at replacement node, if it exists.
    Entry<K,V> replacement = (p.left != null ? p.left : p.right);

    if (replacement != null) {
        // Link replacement to parent
        replacement.parent = p.parent;
        if (p.parent == null)
            root = replacement;
        else if (p == p.parent.left)
            p.parent.left  = replacement;
        else
            p.parent.right = replacement;

        // Null out links so they are OK to use by fixAfterDeletion.
        p.left = p.right = p.parent = null;

        // Fix replacement
        if (p.color == BLACK)
            fixAfterDeletion(replacement);
    } else if (p.parent == null) { // return if we are the only node.
        root = null;
    } else { //  No children. Use self as phantom replacement and unlink.
        if (p.color == BLACK)
            fixAfterDeletion(p);

        if (p.parent != null) {
            if (p == p.parent.left)
                p.parent.left = null;
            else if (p == p.parent.right)
                p.parent.right = null;
            p.parent = null;
        }
    }
}
```

### 总结

TreeMap需要了解的是，如果元素没有实现`Comparable`接口，需要传递`Comparator`实现`compare`方法。还需要了解的就是红黑树的结构。至少了解红黑树的性质，和左右旋转。都是比较简单的。