## AtomicInteger

```java
public class AtomicInteger extends Number implements java.io.Serializable 
```

### java doc

可以原子方式更新的int值。 有关原子访问属性的描述，请参阅VarHandle规范。 AtomicInteger用于诸如原子递增计数器的应用程序中，不能用作Integer的替代。 但是，此类确实扩展了Number，以允许通过处理基于数字的类的工具和实用程序进行统一访问。

### 分析

原子类的实现是通过volatile变量+Unsafe.CAS实现的原子性操作。

```java
private volatile int value;
```

```java
public AtomicInteger(int initialValue) {
    value = initialValue;
}
```

在创建类的时候初始化value或默认为0。

```java
public final boolean compareAndSet(int expectedValue, int newValue) {
    return U.compareAndSetInt(this, VALUE, expectedValue, newValue);
}
```

通过Unsafe类提供的compareAndSetInt方法原子的更新value。如果value和期望的值一样则将value更新成newValue

```java
public final int getAndSet(int newValue) {
    return U.getAndSetInt(this, VALUE, newValue);
}
```



## AtomicReference

```java
public class AtomicReference<V> implements java.io.Serializable 
```

### java doc

可以原子方式更新的对象引用。 有关原子访问属性的描述，请参阅VarHandle规范。`V` -此引用引用的对象类型

```java
private static final VarHandle VALUE;
static {
    try {
        MethodHandles.Lookup l = MethodHandles.lookup();
        VALUE = l.findVarHandle(AtomicReference.class, "value", Object.class);
    } catch (ReflectiveOperationException e) {
        throw new ExceptionInInitializerError(e);
    }
}
```

```java
private volatile V value;
```

AtomicReference 通过VarHandle实现原子操作对象。

```java
public final boolean compareAndSet(V expectedValue, V newValue) {
    return VALUE.compareAndSet(this, expectedValue, newValue);
}
```

## AtomicReferenceArray

### java doc

一组对象引用，其中元素可以原子方式更新。 有关原子访问属性的描述，请参阅VarHandle规范。

```java
private static final VarHandle AA
    = MethodHandles.arrayElementVarHandle(Object[].class);
private final Object[] array; // must have exact type Object[]
```

```java
public final boolean compareAndSet(int i, E expectedValue, E newValue) {
    return AA.compareAndSet(array, i, expectedValue, newValue);
}
```

## AtomicReferenceFieldUpdater

基于反射的实用程序，可以对指定类的指定volatile参考字段进行原子更新。 此类设计用于原子数据结构，其中同一节点的多个引用字段独立地受原子更新的影响。 例如，树节点可能被声明为

```java
 class Node {
   private volatile Node left, right;

   private static final AtomicReferenceFieldUpdater<Node, Node> leftUpdater = AtomicReferenceFieldUpdater.newUpdater(Node.class, Node.class, "left");
   private static AtomicReferenceFieldUpdater<Node, Node> rightUpdater =  AtomicReferenceFieldUpdater.newUpdater(Node.class, Node.class, "right");

   Node getLeft() { return left; }
   boolean compareAndSetLeft(Node expect, Node update) {
     return leftUpdater.compareAndSet(this, expect, update);
   }
   // ... and so on
 }
```

请注意，此类中compareAndSet方法的保证比其他原子类弱。 因为此类无法确保该字段的所有使用都适用于原子访问的目的，所以它只能保证原子性与compareAndSet的其他调用相关并在同一更新程序上设置。

类型为T的参数的对象参数不是传递给newUpdater的类的实例（java.lang.Class <U>，java.lang.Class <W>，java.lang.String）将导致抛出ClassCastException。

```java
public static <U,W> AtomicReferenceFieldUpdater<U,W> newUpdater(Class<U> tclass, Class<W> vclass, String fieldName) {
        return new AtomicReferenceFieldUpdaterImpl<U,W>
            (tclass, vclass, fieldName, Reflection.getCallerClass());
    }
```

通过工厂模式创建`AtomicReferenceFieldUpdater`

```java
AtomicReferenceFieldUpdaterImpl(final Class<T> tclass,
                                final Class<V> vclass,
                                final String fieldName,
                                final Class<?> caller) {
    final Field field;
    final Class<?> fieldClass;
    final int modifiers;
    try {
        field = AccessController.doPrivileged(
            new PrivilegedExceptionAction<Field>() {
                public Field run() throws NoSuchFieldException {
                    return tclass.getDeclaredField(fieldName);
                }
            });
        modifiers = field.getModifiers();
        sun.reflectx.misc.ReflectUtil.ensureMemberAccess(
            caller, tclass, null, modifiers);
        ClassLoader cl = tclass.getClassLoader();
        ClassLoader ccl = caller.getClassLoader();
        if ((ccl != null) && (ccl != cl) &&
            ((cl == null) || !isAncestor(cl, ccl))) {
            sun.reflectx.misc.ReflectUtil.checkPackageAccess(tclass);
        }
        fieldClass = field.getType();
    } catch (PrivilegedActionException pae) {
        throw new RuntimeException(pae.getException());
    } catch (Exception ex) {
        throw new RuntimeException(ex);
    }

    if (vclass != fieldClass)
        throw new ClassCastException();
    if (vclass.isPrimitive())
        throw new IllegalArgumentException("Must be reference type");

    if (!Modifier.isVolatile(modifiers))
        throw new IllegalArgumentException("Must be volatile type");

    // 对受保护字段成员的访问仅限于访问类或其子类之一的接收者,
    // 并且访问类必须又是受保护成员的定义类的子类（或包兄弟）。
    // 如果更新程序引用当前程序包之外的声明类的受保护字段, receiver参数将缩小为访问类的类型。
    this.cclass = (Modifier.isProtected(modifiers) &&
                   tclass.isAssignableFrom(caller) &&
                   !isSamePackage(tclass, caller))
                  ? caller : tclass;
    this.tclass = tclass;
    this.vclass = vclass;
    this.offset = U.objectFieldOffset(field);
}
```

```java
public final boolean compareAndSet(T obj, V expect, V update) {
    accessCheck(obj);
    valueCheck(update);
    return U.compareAndSetObject(obj, offset, expect, update);
}
```

## ABA问题

ABA现象经常出现，特别是在使用类似compareAndSet()这种条件同步操作的动态内存算法中。典型的情形是，一个将要被compareAndSet()从a变为b的引用又被变回为a。这样一来，即使对数据结构的影响已经产生，compareAndSet( )调用也将成功返回，但已不再是想要的结果。

解决这个问题的一种直接办法就是对每个原子引用附上一个唯一一的时间戳。

## AtomicMarkableReferences

```java
public class AtomicMarkableReference<V>
```

### java doc

AtomicMarkableReference维护一个对象引用以及一个可以原子方式更新的标记位。
实现说明：此实现通过创建表示 "boxed" [reference, boolean] 对的内部对象来维护可标记引用。

### 分析

AtomicMarkableReference通过将一个布尔值绑定到想要原子操作的数据实现了，版本的功能。

```java
private static class Pair<T> {
    final T reference;
    final boolean mark;
    private Pair(T reference, boolean mark) {
        this.reference = reference;
        this.mark = mark;
    }
    static <T> Pair<T> of(T reference, boolean mark) {
        return new Pair<T>(reference, mark);
    }
}
```

通过VarHandle更新pair

```java
// VarHandle mechanics
private static final VarHandle PAIR;
static {
    try {
        MethodHandles.Lookup l = MethodHandles.lookup();
        PAIR = l.findVarHandle(AtomicMarkableReference.class, "pair",
                               Pair.class);
    } catch (ReflectiveOperationException e) {
        throw new ExceptionInInitializerError(e);
    }
}
```

初始化

```java
public AtomicMarkableReference(V initialRef, boolean initialMark) {
    pair = Pair.of(initialRef, initialMark);
}
```

CAS

```JAVA
public boolean compareAndSet(V       expectedReference,
                             V       newReference,
                             boolean expectedMark,
                             boolean newMark) {
    Pair<V> current = pair;
    return
        expectedReference == current.reference &&
        expectedMark == current.mark &&
        ((newReference == current.reference &&
          newMark == current.mark) ||
         casPair(current, Pair.of(newReference, newMark)));
}
```

```JAVA
private boolean casPair(Pair<V> cmp, Pair<V> val) {
    return PAIR.compareAndSet(this, cmp, val);
}
```

## AtomicStampedReference

```java
AtomicStampedReference
```

### java doc

AtomicStampedReference维护一个对象引用以及一个整数“标记”，可以原子方式更新。
实现说明：此实现通过创建表示"boxed" [reference, integer] 对的内部对象来维护标记引用。

### 分析

AtomicStampedReference通过引入时间戳解决ABA的问题

```java
private static class Pair<T> {
    final T reference;
    final int stamp;
    private Pair(T reference, int stamp) {
        this.reference = reference;
        this.stamp = stamp;
    }
    static <T> Pair<T> of(T reference, int stamp) {
        return new Pair<T>(reference, stamp);
    }
}
```

将AtomicMarkableReference的布尔值，替换成了整形时间戳。

```java
public boolean compareAndSet(V   expectedReference,
                             V   newReference,
                             int expectedStamp,
                             int newStamp) {
    Pair<V> current = pair;
    return
        expectedReference == current.reference &&
        expectedStamp == current.stamp &&
        ((newReference == current.reference &&
          newStamp == current.stamp) ||
         casPair(current, Pair.of(newReference, newStamp)));
}
```

```java
private boolean casPair(Pair<V> cmp, Pair<V> val) {
    return PAIR.compareAndSet(this, cmp, val);
}
```

当时间戳不变的时候才可以更新变量值。

