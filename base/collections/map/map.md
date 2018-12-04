### Map

```java
public interface Map<K, V>
```

### java doc

将键映射到值的对象。地图不能包含重复的键;每个键最多可以映射一个值。
这个接口取代了Dictionary类，它是一个完全抽象的类而不是接口。

Map接口提供三个集合视图，允许将地图的内容视为一组键，值集或键值映射集。地图的顺序定义为地图集合视图上的迭代器返回其元素的顺序。一些地图实现，比如TreeMap类，对它们的顺序做出了特定的保证;其他人，比如HashMap类，没有。

注意：如果将可变对象用作映射键，则必须非常小心。如果在对象是地图中的键的同时以影响等于比较的方式更改对象的值，则不指定映射的行为。这种禁令的一个特例是，地图不允许将自己作为一个关键词。虽然允许映射将自身包含为值，但建议极其谨慎：equals和hashCode方法不再在这样的映射上很好地定义。

所有通用映射实现类都应该提供两个“标准”构造函数：一个void（无参数）构造函数，它创建一个空映射，一个构造函数具有一个类型为Map的参数，它创建一个具有相同键值的新映射映射作为其论点。实际上，后一个构造函数允许用户复制任何地图，生成所需类的等效地图。没有办法强制执行此建议（因为接口不能包含构造函数），但JDK中的所有通用映射实现都符合。

如果此映射不支持该操作，则此接口中包含的“破坏性”方法（即修改它们操作的映射的方法）被指定为抛出UnsupportedOperationException。如果是这种情况，如果调用对地图没有影响，这些方法可能（但不是必须）抛出UnsupportedOperationException。例如，如果要映射“映射”的映射为空，则在不可修改的映射上调用putAll（Map）方法可能（但不是必须）抛出异常。

某些地图实现对它们可能包含的键和值有限制。例如，某些实现禁止空键和值，有些实现对其键的类型有限制。尝试插入不合格的键或值会引发未经检查的异常，通常为NullPointerException或ClassCastException。尝试查询不合格的键或值的存在可能会引发异常，或者它可能只是返回false;一些实现将展示前一种行为，一些将展示后者。更一般地，尝试对不合格的密钥或值进行操作，其完成不会导致将不合格的元素插入到映射中，可能会引发异常，或者可能在实现的选择中成功。此类异常在此接口的规范中标记为“可选”。

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

映射条目（键值对）。 `Map.entrySet`方法返回地图的集合视图，其元素属于此类。 获取对映射条目的引用的唯一方法是来自此`collection-view`的迭代器。 这些`Map.Entry`对象仅在迭代期间有效; 更正式地说，如果在迭代器返回条目后修改了支持映射，则映射条目的行为是未定义的，除非通过映射条目上的`setValue`操作。

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



该方法将key重新映射成新值，但是不保证快速失败，也不保证原子性与同步性，需要由子类重写。