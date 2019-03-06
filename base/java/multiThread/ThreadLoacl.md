```java
public class ThreadLocal<T> {
```

该类提供线程局部变量。 这些变量与它们的正常对应物的不同之处在于，访问一个变量的每个线程（通过其get或set方法）都有自己独立初始化的变量副本。 ThreadLocal实例通常是希望将状态与线程（例如，用户ID或事务ID）相关联的类中的私有静态字段。
例如，下面的类生成每个线程本地的唯一标识符。 线程的id在第一次调用ThreadId.get（）时分配，并在后续调用中保持不变。

```JAVA
import java.util.concurrent.atomic.AtomicInteger;

 public class ThreadId {
     // Atomic integer containing the next thread ID to be assigned
     private static final AtomicInteger nextId = new AtomicInteger(0);

     // Thread local variable containing each thread's ID
     private static final ThreadLocal<Integer> threadId =
         new ThreadLocal<Integer>() {
             @Override protected Integer initialValue() {
                 return nextId.getAndIncrement();
         }
     };

     // Returns the current thread's unique ID, assigning it if necessary
     public static int get() {
         return threadId.get();
     }
 }
```

只要线程处于活动状态且ThreadLocal实例可访问，每个线程都会保存对其线程局部变量副本的隐式引用; 在一个线程消失之后，它的所有线程局部实例副本都要进行垃圾收集（除非存在对这些副本的其他引用）。

```JAVA
public T get() {
    Thread t = Thread.currentThread();
    ThreadLocalMap map = getMap(t);
    if (map != null) {
        ThreadLocalMap.Entry e = map.getEntry(this);
        if (e != null) {
            @SuppressWarnings("unchecked")
            T result = (T)e.value;
            return result;
        }
    }
    return setInitialValue();
}
```

直接看一下 get 方法

首先获取了当前线程上下文的线程 t，然后调用`getMap` 获得了一个 ThreadLocalMap

```java
ThreadLocalMap getMap(Thread t) {
    return t.threadLocals;
}
```

通过线程 t 获得了一个，threadLocals。

```java
/* ThreadLocal values pertaining to this thread. This map is maintained
 * by the ThreadLocal class. */
ThreadLocal.ThreadLocalMap threadLocals = null;
```

这个 threadLocals 在 thread 类中。他是一个 ThreadLocal 的静态内部类 ThreadLocalMap ，

```java
static class ThreadLocalMap {
    static class Entry extends WeakReference<ThreadLocal<?>> {
        /** The value associated with this ThreadLocal. */
        Object value;

        Entry(ThreadLocal<?> k, Object v) {
            super(k);
            value = v;
        }
    }

    /**
         * The initial capacity -- MUST be a power of two.
         */
    private static final int INITIAL_CAPACITY = 16;

    /**
         * The table, resized as necessary.
         * table.length MUST always be a power of two.
         */
    private Entry[] table;

    /**
         * The number of entries in the table.
         */
    private int size = 0;

    /**
         * The next size value at which to resize.
         */
    private int threshold; // Default to 0

```

ThreadLocalMap是一个自定义的哈希映射，仅适用于维护线程本地值。 不会在ThreadLocal类之外导出任何操作。 该类是包私有的，允许在Thread类中声明字段。 为了帮助处理非常大且长期使用的用法，哈希表条目使用WeakReferences作为键。 但是，由于未使用引用队列，因此只有在表开始空间不足时才能保证删除过时条目。

ThreadLocalMap 中的条目为 Entry，继承自weakReference。存储在数组中，因为可以容纳不同的ThreadLocal 对象在，当前线程中存储数据，但是每个ThreadLocal  只可以存储一个实例数据。

```java
ThreadLocalMap(ThreadLocal<?> firstKey, Object firstValue) {
    table = new Entry[INITIAL_CAPACITY];
    int i = firstKey.threadLocalHashCode & (INITIAL_CAPACITY - 1);
    table[i] = new Entry(firstKey, firstValue);
    size = 1;
    setThreshold(INITIAL_CAPACITY);
}
```

构造逻辑很简单。

```java
private void set(ThreadLocal<?> key, Object value) {

    // We don't use a fast path as with get() because it is at
    // least as common to use set() to create new entries as
    // it is to replace existing ones, in which case, a fast
    // path would fail more often than not.

    Entry[] tab = table;
    int len = tab.length;
    int i = key.threadLocalHashCode & (len-1);

    for (Entry e = tab[i];
         e != null;
         e = tab[i = nextIndex(i, len)]) {
        ThreadLocal<?> k = e.get();

        if (k == key) {
            e.value = value;
            return;
        }

        if (k == null) {
            replaceStaleEntry(key, value, i);
            return;
        }
    }

    tab[i] = new Entry(key, value);
    int sz = ++size;
    if (!cleanSomeSlots(i, sz) && sz >= threshold)
        rehash();
}
```

在一个for 循环中，使用线性探查法处理冲突。如果key 也就是 ThreadLocal 相等，则直接替换val 。如果Thrad 因为内存不足被回收，则调用`replaceStaleEntry`，在当前槽位设置当前的ThreadLocal 了。旧的信息会被抹去。

最后 创建新的 Entry，然后调用 `cleanSomeSlots`清除已经被回收的槽位。

```java
public T get() {
    Thread t = Thread.currentThread();
    ThreadLocalMap map = getMap(t);
    if (map != null) {
        ThreadLocalMap.Entry e = map.getEntry(this);
        if (e != null) {
            @SuppressWarnings("unchecked")
            T result = (T)e.value;
            return result;
        }
    }
    return setInitialValue();
}
```

get 方法在 map为空的时候调用`setInitialValue`。

```java
private T setInitialValue() {
    T value = initialValue();
    Thread t = Thread.currentThread();
    ThreadLocalMap map = getMap(t);
    if (map != null) {
        map.set(this, value);
    } else {
        createMap(t, value);
    }
    if (this instanceof TerminatingThreadLocal) {
        TerminatingThreadLocal.register((TerminatingThreadLocal<?>) this);
    }
    return value;
}
```

将 `initialValue`方法的返回值最为value。创建新的map 或插入在原有Map 中。

` createMap(t, value)` 调用 ThreadLocalMap 的构造方法创建Entry 数组。



