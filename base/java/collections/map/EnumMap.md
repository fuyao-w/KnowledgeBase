## EnumMap

```java
public class EnumMap<K extends Enum<K>, V> extends AbstractMap<K, V>
    implements java.io.Serializable, Cloneable
```

### java doc

用于枚举类型键的专用Map实现。 枚举映射中的所有键必须来自创建映射时显式或隐式指定的单个枚举类型。 枚举映射在内部表示为数组。 这种表现非常紧凑和高效。
枚举映射按其键的自然顺序（枚举常量的声明顺序）维护。 这反映在集合视图（keySet（），entrySet（）和values（））返回的迭代器中。

集合视图返回的迭代器非常一致：它们永远不会抛出ConcurrentModificationException，它们可能会也可能不会显示迭代进行过程中对映射所做的任何修改的影响。

不允许使用空密钥。 尝试插入null键将抛出NullPointerException。 但是，尝试测试是否存在空键或删除空键将正常运行。 允许空值。

```java
  Map<EnumKey, V> m
         = Collections.synchronizedMap(new EnumMap<EnumKey, V>(...));
```

​	实施说明：所有基本操作都在恒定时间内执行。 它们很可能（虽然不能保证）比它们的HashMap对应物更快。

### 字段

```java
/**
 * 此映射的所有键的枚举类型的Class对象。
 */
private final Class<K> keyType;

/**
 * 枚举对象元素组成的数组.  
 */
private transient K[] keyUniverse;

/**
 *此映射的数组表示形式。第i个元素是值
 *当前为哪个Universe [i]映射，如果不是，则为null
 *映射到任何东西，如果它映射到null，则为NULL.
 */
private transient Object[] vals;

/**
 * map的映射数量.
 */
private transient int size = 0;

//代表null
private static final Object NULL = new Object() {
        public int hashCode() {
            return 0;
        }

        public String toString() {
            return "java.util.EnumMap.NULL";
        }
    };  

```

keyUniverse缓存可了枚举里的所有元素，在迭代的时候可以提高性能，但`put`,`get`不会用到。
vals保存枚举映射的value值。

### 分析

```java
public EnumMap(Class<K> keyType) {
    this.keyType = keyType;
    keyUniverse = getKeyUniverse(keyType);
    vals = new Object[keyUniverse.length];
}
```

​	

```java
/**
 * 返回包含枚举K的所有值。
 * 结果是所有调用者都未克隆，缓存和共享。
 */
private static <K extends Enum<K>> K[] getKeyUniverse(Class<K> keyType) {
    return SharedSecrets.getJavaLangAccess()
                                    .getEnumConstantsShared(keyType);
}
```

EnumMap的长度就是枚举对象里元素的数量。不用扩容，在对象初始化的时候就已经确定。

```java
public V put(K key, V value) {
    typeCheck(key);

    int index = key.ordinal();
    Object oldValue = vals[index];
    vals[index] = maskNull(value);
    if (oldValue == null)
        size++;
    return unmaskNull(oldValue);
}
```

typeCheck验证key的合法性，必须是keyType类型或者是keyType的子类。通过`ordinal()`获取key的位置，`ordinal`代表枚举元素在枚举类中的顺序。是独一无二的值。



```java
public V get(Object key) {
    return (isValidKey(key) ?
            unmaskNull(vals[((Enum<?>)key).ordinal()]) : null);
}
```

`get()`在验证key合法性之后直接在vals获取到value值。



EnumMap的结构简单，官方文档也介绍性能甚至比HashMap更好。