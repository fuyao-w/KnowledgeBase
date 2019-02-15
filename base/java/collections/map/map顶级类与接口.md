### Map

```java
public interface Map<K, V>
```

### java doc

将键映射到值的对象。map不能包含重复的键;每个键最多可以映射一个值。
这个接口取代了Dictionary类，它是一个完全抽象的类而不是接口。

Map接口提供三个集合视图，允许将map的内容视为一组键，值集或键值映射集。map的顺序定义为map集合视图上的迭代器返回其元素的顺序。一些map实现，比如TreeMap类，对它们的顺序做出了特定的保证;其他人，比如HashMap类，没有。

注意：如果将可变对象用作映射键，则必须非常小心。如果在对象是map中的键的同时以影响等于比较的方式更改对象的值，则不指定映射的行为。这种禁令的一个特例是，map不允许将自己作为一个关键词。虽然允许映射将自身包含为值，但建议极其谨慎：equals和hashCode方法不再在这样的映射上很好地定义。

所有通用映射实现类都应该提供两个“标准”构造函数：一个void（无参数）构造函数，它创建一个空映射，一个构造函数具有一个类型为Map的参数，它创建一个具有相同键值的新映射映射作为其论点。实际上，后一个构造函数允许用户复制任何map，生成所需类的等效map。没有办法强制执行此建议（因为接口不能包含构造函数），但JDK中的所有通用映射实现都符合。

如果此映射不支持该操作，则此接口中包含的“破坏性”方法（即修改它们操作的映射的方法）被指定为抛出UnsupportedOperationException。如果是这种情况，如果调用对map没有影响，这些方法可能（但不是必须）抛出UnsupportedOperationException。例如，如果要映射“映射”的映射为空，则在不可修改的映射上调用putAll（Map）方法可能（但不是必须）抛出异常。

某些map实现对它们可能包含的键和值有限制。例如，某些实现禁止空键和值，有些实现对其键的类型有限制。尝试插入不合格的键或值会引发未经检查的异常，通常为NullPointerException或ClassCastException。尝试查询不合格的键或值的存在可能会引发异常，或者它可能只是返回false;一些实现将展示前一种行为，一些将展示后者。更一般地，尝试对不合格的密钥或值进行操作，其完成不会导致将不合格的元素插入到映射中，可能会引发异常，或者可能在实现的选择中成功。此类异常在此接口的规范中标记为“可选”。

Collections Framework接口中的许多方法都是根据equals方法定义的。例如，containsKey（Object key）方法的规范说：“当且仅当此映射包含键k的映射时才返回true（key == null？k == null：key.equals（k） ）“。不应将此规范解释为暗示使用非空参数键调用Map.containsKey将导致为任何键k调用key.equals（k）。实现可以自由地实现优化，从而避免等于调用，例如，通过首先比较两个密钥的哈希码。 （Object.hashCode（）规范保证具有不等哈希码的两个对象不能相等。）更一般地，各种集合框架接口的实现可以自由地利用底层Object方法的指定行为，无论实现者认为它是否合适。

执行映射递归遍历的某些映射操作可能会失败，并且映射直接或间接包含自身的自引用实例例外。这包括clone（），equals（），hashCode（）和toString（）方法。实现可以可选地处理自引用场景，但是大多数当前实现不这样做。

#### Unmodifiable Maps

Map.of，Map.ofEntries和Map.copyOf静态工厂方法提供了一种创建不可修改映射的便捷方法。这些方法创建的Map实例具有以下特征：

它们是不可修改的。无法添加，删除或更新密钥和值。在Map上调用任何mutator方法将始终导致抛出UnsupportedOperationException。但是，如果包含的键或值本身是可变的，则可能导致Map的行为不一致或其内容似乎发生变化。

- 它们不允许使用null键和值。尝试使用null键或值创建它们会导致NullPointerException。
- 如果所有键和值都是可序列化的，则它们是可序列化的。
- 他们在创建时拒绝重复密钥。传递给静态工厂方法的重复键导致IllegalArgumentException。
- 映射的迭代顺序未指定，可能会发生变化。
- 它们是基于价值的。调用者不应对返回实例的身份做出任何假设。工厂可以自由创建新实例或重用现有实例。因此，对这些实例的标识敏感操作（引用相等（==），标识哈希代码和同步）是不可靠的，应该避免。
- 它们按序列化表单页面上的指定进行序列化。

### Map.Entry

映射条目（键值对）。 `Map.entrySet`方法返回map的集合视图，其元素属于此类。 获取对映射条目的引用的唯一方法是来自此`collection-view`的迭代器。 这些`Map.Entry`对象仅在迭代期间有效; 更正式地说，如果在迭代器返回条目后修改了支持映射，则映射条目的行为是未定义的，除非通过映射条目上的`setValue`操作。

### 分析

Map接口是所有map的顶层接口，定义了访问map的一般方法，并且定义了一个内部接口Entry。还定义了一些静态方法，和default方法。default方法都是自1.8引入函数式接口相关的方法。例如：

```java
default V compute(K key,
        BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    Objects.requireNonNull(remappingFunction);
    V oldValue = get(key);

    V newValue = remappingFunction.apply(key, oldValue);
    if (newValue == null) {
        // delete mapping
        if (oldValue != null || containsKey(key)) {
            // something to remove
            remove(key);
            return null;
        } else {
            // nothing to do. Leave things as they were.
            return null;
        }
    } else {
        // add or replace old mapping
        put(key, newValue);
        return newValue;
    }
}
```



该方法将`value`重新映射成新值，但是不保证快速失败，也不保证原子性与同步性，需要由子类重写，实例：

        var map = new HashMap<String, String>();
        map.put("test", "testOne");
    
        map.computeIfAbsent("core", (v) -> {
    
            return v.concat("新值");
        });
        map.compute("扶摇", (k, v) -> {
            System.out.println(k.concat("被修改"));
            return "新值：".concat(v);
        });

## AbstractMap ##

    public abstract class AbstractMap<K,V> implements Map<K,V>

### java doc ###



此类提供Map接口的骨干实现，以最大限度地减少实现此接口所需的工作量。
要实现不可修改的映射，程序员只需要扩展此类并为·entrySet·方法提供实现，该方法返回映射映射的set-view。通常，返回的集合将依次在AbstractSet上实现。此set不应支持add或remove方法，并且其迭代器不应支持remove方法。

要实现可修改的映射，程序员必须另外覆盖此类的put方法（否则默认抛出UnsupportedOperationException），由迭代器返回的`entrySet().iterator()` 必须实现其`remove()`。

程序员通常应该根据Map接口规范中的建议提供void（无参数）和map构造函数。

此类中每个非抽象方法的文档详细描述了它的实现。如果正在实施的map允许更有效的实施，则可以覆盖这些方法中的每一个。

### 分析 ###

AbstractMap的方法实现都是基于迭代器，迭代器需要子类来实现。



## SortedMap ##

```java
public interface SortedMap<K,V> extends Map<K,V>
```

### java doc ###

一个map，进一步提供其键的总排序。map按照其键的自然顺序排序，或者通过在排序map创建时通常提供的比较器排序。迭代有序映射的集合视图（由entrySet，keySet和values方法返回）时会反映此顺序。提供了几个额外的操作以利用订购。 （此接口是SortedSet的map模拟。）
插入到有序映射中的所有键必须实现Comparable接口（或由指定的比较器接受）。此外，所有这些键必须是可相互比较的：k1.compareTo（k2）（或comparator.compare（k1，k2））不得对有序映射中的任何键k1和k2抛出ClassCastException。尝试违反此限制将导致违规方法或构造函数调用抛出ClassCastException。

请注意，如果有序映射要正确实现Map接口，则由有序映射维护的排序（无论是否提供显式比较器）必须与equals一致。 （有关与equals一致的精确定义，请参阅Comparable接口或Comparator接口。）这是因为Map接口是根据equals操作定义的，但是有序映射使用compareTo（或compare）方法执行所有键比较因此，从排序map的角度来看，这种方法被视为相等的两个键是相等的。树图的行为即使其排序与equals不一致也是明确定义的;它只是没有遵守Map接口的一般合同。

所有通用的有序映射实现类都应该提供四个“标准”构造函数。 虽然接口无法指定所需的构造函数，但无法强制执行此建议。 所有有序映射实现的预期“标准”构造函数是：

- 一个void（无参数）构造函数，它根据键的自然顺序创建一个空的有序映射。
- 具有Comparator类型的单个参数的构造函数，它创建根据指定的比较器排序的空的有序映射。
- 具有Map类型的单个参数的构造函数，它创建一个具有与其参数相同的键 - 值映射的新映射，并根据键的自然顺序进行排序。
- 具有SortedMap类型的单个参数的构造函数，它创建一个新的有序映射，其具有相同的键 - 值映射和与输入有序映射相同的顺序。

注意：有几种方法返回带有受限键范围的子图。 这样的范围是半开放的，即它们包括它们的低端点但不包括它们的高端点（如果适用）。 如果您需要一个封闭范围（包括两个端点），并且密钥类型允许计算给定密钥的后继，则只需从lowEndpoint请求子范围到后继（highEndpoint）。 例如，假设m是其键是字符串的映射。 下面的习惯用法获得一个视图，其中包含m中键的值介于低和高之间的所有键值映射，包括：

```java
SortedMap<String, V> sub = m.subMap(low, high+"\0");
```

可以使用类似的技术来生成开放范围（其中既不包含端点）。 下面的习惯用法获得一个视图，其中包含m中键值介于低位和高位之间的所有键值映射，不包括：

```java
SortedMap<String, V> sub = m.subMap(low+"\0", high);
```



## NavigableMap ##

```java
public interface NavigableMap<K,V> extends SortedMap<K,V>
```

### java doc ###

使用导航方法扩展的SortedMap返回给定搜索目标的最接近匹配。方法lowerEntry（K），floorEntry（K），ceilingEntry（K）和higherEntry（K）返回与分别小于，小于或等于，大于或等于，大于给定键的键相关联的Map.Entry对象，如果没有这样的键，则返回null。类似地，方法lowerKey（K），floorKey（K），ceilingKey（K）和higherKey（K）仅返回关联的键。所有这些方法都是为了定位而不是遍历条目而设计的。
可以按升序或降序键访问和遍历NavigableMap。 descendingMap（）方法返回地图视图，其中所有关系和方向方法的感知都被反转。升序操作和视图的性能可能比降序操作的速度快。方法subMap（K，boolean，K，boolean），headMap（K，boolean）和tailMap（K，boolean）与类似名称的SortedMap方法的不同之处在于接受描述下限和上限是包含还是排除的其他参数。任何NavigableMap的子图必须实现NavigableMap接口。

此接口还定义了返回和/或删除最小和最大映射（如果存在）的方法firstEntry（），pollFirstEntry（），lastEntry（）和pollLastEntry（），否则返回null。

入口返回方法的实现应该返回Map.Entry对，它们表示生成映射的快照，因此通常不支持可选的Entry.setValue方法。但请注意，可以使用put方法更改关联映射中的映射。

方法subMap（K，K），headMap（K）和tailMap（K）被指定返回SortedMap以允许对SortedMap的现有实现进行兼容改进以实现NavigableMap，但鼓励此接口的扩展和实现覆盖这些方法返回NavigableMap。类似地，可以重写SortedMap.keySet（）以返回NavigableSet。



## Dictionary ##

```java
public abstract
class Dictionary<K,V>
```

### java doc ###

Dictionary类是任何类的抽象父类，例如Hashtable，它将键映射到值。 每个键和每个值都是一个对象。 在任何一个Dictionary对象中，每个键最多与一个值相关联。 给定一个Dictionary和一个键，可以查找相关的元素。 任何非null对象都可以用作键和值。
通常，此类的实现应使用equals方法来确定两个键是否相同。

注意：此类已过时。 新实现应该实现Map接口，而不是扩展此类。

Dictionary是HashTable的父类。

