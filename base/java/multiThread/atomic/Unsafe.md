## Unsafe

用于执行低级，不安全操作的方法的集合。 虽然类和所有方法都是公共的，但是这个类的使用是有限的，因为只有受信任的代码才能获取它的实例。
注意：调用者的责任是确保在调用此类的方法之前检查参数。 虽然对输入执行了一些基本检查，但检查是最好的，当性能是一个重要的优先级时，就像运行时编译器优化此类的方法一样，可以省略一些或所有检查（如果有的话）。 因此，调用者不能依赖检查和相应的例外！

不能直接`new Unsafe()`，原因是`Unsafe`被设计成单例模式，构造方法是私有的；

不能通过调用`Unsafe.getUnsafe()获取，因为getUnsafe在java 8之前被设计成只能从引导类加载器（bootstrap class loader）加载。在11中jdk被拆分在模块中，用户不能使用jdk.internal模块下的usafe类即使是通过反射，调用会产生以下错误：

java.lang.IllegalAccessError: class xxx(in unnamed module @0x5b464ce8) cannot access class jdk.internal.misc.Unsafe (in module java.base) because module java.base does not export jdk.internal.misc to unnamed module @0x5b464ce8 at xxx.<clinit>

提供了compare and set 操作、getAndSet 、getAndAdd、volatile变量操作、和直接操作内存数据。阻塞，不经过VM安全检查创建Class。

```JAVA
public native int getInt(Object o, long offset);
```



```java
public native void putInt(Object o, long offset, int x);
```

通过字段所在对象的内存地址相对偏移量来，直接获取变量，设置变量。

```java
public long getAddress(Object o, long offset) {
    if (ADDRESS_SIZE == 4) {
        return Integer.toUnsignedLong(getInt(o, offset));
    } else {
        return getLong(o, offset);
    }
}
```

从给定的内存地址获取本机指针。 如果地址为零，或者没有指向从allocateMemory获得的块，则结果是未定义的。
如果本机指针的宽度小于64位，则将其作为无符号数扩展为Java long。 指针可以由任何给定的字节偏移量索引，简单地通过将该偏移量（作为简单整数）添加到表示指针的长度。 实际从目标地址读取的字节数可以通过查询addressSize来确定

```java
public void putAddress(Object o, long offset, long x) {
    if (ADDRESS_SIZE == 4) {
        putInt(o, offset, (int)x);
    } else {
        putLong(o, offset, x);
    }
}
```

将本机指针存储到给定的内存地址中。 如果地址为零，或者没有指向从allocateMemory获得的块，则结果是未定义的。



```java
public long allocateMemory(long bytes) {
    allocateMemoryChecks(bytes);

    if (bytes == 0) {
        return 0;
    }

    long p = allocateMemory0(bytes);
    if (p == 0) {
        throw new OutOfMemoryError();
    }

    return p;
}
```

分配给定大小的新的本机内存块（以字节为单位）。



```java
public long objectFieldOffset(Field f) {
    if (f == null) {
        throw new NullPointerException();
    }

    return objectFieldOffset0(f);
}
```

返回给定字段在其存储分配中的位置

```java
public Class<?> defineClass(String name, byte[] b, int off, int len,
                            ClassLoader loader,
                            ProtectionDomain protectionDomain) {
    if (b == null) {
        throw new NullPointerException();
    }
    if (len < 0) {
        throw new ArrayIndexOutOfBoundsException();
    }

    return defineClass0(name, b, off, len, loader, protectionDomain);
}
```

告诉VM定义一个类，没有安全检查。 默认情况下，类加载器和保护域来自调用者的类。



```java
public final native boolean compareAndSetInt(Object o, long offset,
                                             int expected,
                                             int x);
```

通过CAS算法原子更新变量

```java
public native void park(boolean isAbsolute, long time);
```

阻塞当前线程

```java
public final int getAndSetInt(Object o, long offset, int newValue) {
    int v;
    do {
        v = getIntVolatile(o, offset);
    } while (!weakCompareAndSetInt(o, offset, v, newValue));
    return v;
}
```

```java
public native int     getIntVolatile(Object o, long offset);
```

`getInt(Object, long)`的volatile版本

