## Vector ##

    extends AbstractList<E> implements List<E>, RandomAccess, Cloneable, java.io.Serializable

### java doc ###

Vector类实现了一个可增长的对象数组。像数组一样，它包含可以使用整数索引访问的组件。但是，Vector的大小可以根据需要增大或缩小，以适应在创建Vector之后添加和删除项目。
每个Vector都试图通过维持容量和capacityIncrement来优化存储管理。容量始终至少与矢量大小一样大;它通常更大，因为随着组件被添加到Vector中，Vector的存储以块的大小增加capacityIncrement的大小。应用程序可以在插入大量组件之前增加Vector的容量;这减少了增量重新分配的数量。

这个类的iterator和listIterator方法返回的迭代器是快速失败的：如果在创建迭代器之后的任何时候对Vector进行结构修改，除了通过迭代器自己的remove或add方法之外，迭代器将抛出ConcurrentModificationException。因此，在并发修改的情况下，迭代器快速而干净地失败，而不是在未来的未确定时间冒任意，非确定性行为的风险。 elements方法返回的枚举不是快速失败的;如果在创建枚举后的任何时候对Vector进行结构修改，则枚举的结果是未定义的。

请注意，迭代器的快速失败行为无法得到保证，因为一般来说，在存在不同步的并发修改时，不可能做出任何硬性保证。失败快速迭代器会尽最大努力抛出ConcurrentModificationException。因此，编写依赖于此异常的程序以确保其正确性是错误的：迭代器的快速失败行为应该仅用于检测错误。

与新的集合实现不同，Vector是同步的。如果不需要线程安全实现，建议使用ArrayList代替Vector。


### 字段 ###

    //数组缓冲区，其中存储了向量的组件。
    protected Object[] elementData;

    //此Vector对象保存的有效元素数。
    protected int elementCount;

    //当矢量大小超过其容量时，矢量容量自动递增的量。
    protected int capacityIncrement;


    private static final long serialVersionUID = -2767605614048989439L;

### 扩容 ###

Vector的初始容量是通过构造方法传进去的 10。capacityIncrement代表着扩容时数组长度的曾量。默认是0。

    private int newCapacity(int minCapacity) {
            // overflow-conscious code
            int oldCapacity = elementData.length;
            int newCapacity = oldCapacity + ((capacityIncrement > 0) ?
                                             capacityIncrement : oldCapacity);
            if (newCapacity - minCapacity <= 0) {
                if (minCapacity < 0) // overflow
                    throw new OutOfMemoryError();
                return minCapacity;
            }
            return (newCapacity - MAX_ARRAY_SIZE <= 0)
                ? newCapacity
                : hugeCapacity(minCapacity);
        }

在执行到这个方法的时候会判断capacityIncrement如果为0的话就设置为初始数组的容量。那么默认的就是10。
所以它以2的次幂扩容。

通过java doc的提示，Vector类有控制数组有效元素大小的方法：

    public synchronized void setSize(int newSize) {
        modCount++;
        if (newSize > elementData.length)
            grow(newSize);
        final Object[] es = elementData;
        for (int to = elementCount, i = newSize; i < to; i++)
            es[i] = null;
        elementCount = newSize;
    }

setSize方法通过修好elementCount，来修改有效元素的个数。

除此之外Vector与ArrayList的区别就在于它的公共方法都是synchronized关键字修饰的，这代表他是同步的可以保证线程安全。但是它的性能并不好。
它很早就已经被不推荐使用了。

### 总结 ###

Vector是线程的容器。默认容量是10，默认以2次幂大小扩扩容。可以通过setSize()改变有效元素的大小。


## Stack ##

    class Stack<E> extends Vector<E> {

### java doc ###
Stack类表示对象的后进先出（LIFO）堆栈。 它使用五个操作扩展了Vector类，这些操作允许将向量视为堆栈。 提供了常见的推送和弹出操作，以及查看堆栈顶部项目的方法，测试堆栈是否为空的方法，以及搜索堆栈中的项目并发现其距离的方法 是从顶部。
首次创建堆栈时，它不包含任何项目。

Deque接口及其实现提供了更完整和一致的LIFO堆栈操作集，应优先使用此类。 例如：

    Deque<Integer> stack = new ArrayDeque<Integer>();

### 分析 ###

Stack是一个继承Vector来是实现的栈结构。方法很少不算构造方法只有5个，需要注意的是其中`pop`,
`peek`方法是由synchronized 修饰的。