## Hashtable

```java
public class Hashtable<K,V> extends Dictionary<K,V>
    implements Map<K,V>, Cloneable, java.io.Serializable 
```

### java doc

该类实现了一个哈希表，它将键映射到值。任何非null对象都可以用作键或值。
要成功存储和检索哈希表中的对象，用作键的对象必须实现hashCode方法和equals方法。

Hashtable 的一个实例有两个影响其性能的参数：初始容量和负载因子。容量是哈希表中的桶数，初始容量只是创建哈希表时的容量。请注意，哈希表是打开的：在“哈希冲突”的情况下，单个存储桶存储多个条目，必须按顺序搜索。加载因子是在自动增加容量之前允许哈希表获取的完整程度的度量。初始容量和负载系数参数仅仅是实现的提示。关于何时以及是否调用rehash方法的确切细节是依赖于实现的。

通常，默认负载系数（.75）在时间和空间成本之间提供了良好的折衷。较高的值会减少空间开销，但会增加查找条目的时间成本（这反映在大多数Hashtable操作中，包括get和put）。

初始容量控制了浪费空间和重新运算操作的需要之间的权衡，这是非常耗时的。如果初始容量大于Hashtable将包含的最大条目数除以其加载因子，则不会发生重复操作。但是，将初始容量设置得太高会浪费空间。

如果要将多个条目设置为Hashtable，则以足够大的容量创建条目可以允许更有效地插入条目，而不是根据需要执行自动重新分组来扩展表。

此示例创建数字哈希表。它使用数字的名称作为键：

```java
   Hashtable<String, Integer> numbers
     = new Hashtable<String, Integer>();
   numbers.put("one", 1);
   numbers.put("two", 2);
   numbers.put("three", 3);
```

要检索数字，请使用以下代码：

```java
   Integer n = numbers.get("two");
   if (n != null) {
     System.out.println("two = " + n);
   }
```

由所有类的“集合视图方法”返回的集合的迭代器方法返回的迭代器是快速失败的：如果在创建迭代器之后的任何时候对Hashtable进行结构修改，除非通过迭代器自己的删除方法，迭代器将抛出ConcurrentModificationException。因此，在并发修改的情况下，迭代器快速而干净地失败，而不是在未来的未确定时间冒任意，非确定性行为的风险。 Hashtable的键和元素方法返回的枚举不是快速失败的;如果在创建枚举后的任何时候对Hashtable进行结构修改，则枚举的结果是未定义的。

请注意，迭代器的快速失败行为无法得到保证，因为一般来说，在存在不同步的并发修改时，不可能做出任何硬性保证。失败快速迭代器会尽最大努力抛出ConcurrentModificationException。因此，编写依赖于此异常的程序以确保其正确性是错误的：迭代器的快速失败行为应该仅用于检测错误。

从Java 2平台v1.2开始，这个类被改进以实现Map接口，使其成为Java Collections Framework的成员。与新的集合实现不同，Hashtable是同步的。如果不需要线程安全实现，建议使用HashMap代替Hashtable。如果需要线程安全的高并发实现，那么建议使用ConcurrentHashMap代替Hashtable。

### 分析

```java
public Hashtable() {
    this(11, 0.75f);
}
```

```java
public synchronized V put(K key, V value) {
    // Make sure the value is not null
    if (value == null) {
        throw new NullPointerException();
    }

    // Makes sure the key is not already in the hashtable.
    Entry<?,?> tab[] = table;
    int hash = key.hashCode();
    int index = (hash & 0x7FFFFFFF) % tab.length;
    @SuppressWarnings("unchecked")
    Entry<K,V> entry = (Entry<K,V>)tab[index];
    for(; entry != null ; entry = entry.next) {
        if ((entry.hash == hash) && entry.key.equals(key)) {
            V old = entry.value;
            entry.value = value;
            return old;
        }
    }

    addEntry(hash, key, value, index);
    return null;
}
```

使用线程探测再散列的方法解决Hash 冲突。

```java
protected void rehash() {
    int oldCapacity = table.length;
    Entry<?,?>[] oldMap = table;

    // overflow-conscious code
    int newCapacity = (oldCapacity << 1) + 1;
    if (newCapacity - MAX_ARRAY_SIZE > 0) {
        if (oldCapacity == MAX_ARRAY_SIZE)
            // Keep running with MAX_ARRAY_SIZE buckets
            return;
        newCapacity = MAX_ARRAY_SIZE;
            //省略
     
    }
}
```

HashTable是已经不被推荐使用的类，与HashMap类似。但是解决Hash 冲突的方法不同。继承`Dictionary`类，而不是`AbsTractMap`类。关键方法都用`Synchronized`关键字修饰。与`Vector`一样。默认初始容量为11,扩容方式为`旧值*2+1`。

