## ArrayDeque ##

    public class ArrayDeque<E> extends AbstractCollection<E> implements Deque<E>, Cloneable, Serializable

### java doc ###

Deque接口的可调整大小的数组实现。阵列deques没有容量限制;他们根据需要增长以支持使用。它们不是线程安全的;在没有外部同步的情况下，它们不支持多线程的并发访问。禁止使用空元素。当用作堆栈时，此类可能比Stack快，并且在用作队列时比LinkedList更快。
大多数ArrayDeque操作以分摊的常量时间运行。例外包括remove，removeFirstOccurrence，removeLastOccurrence，contains，iterator.remove（）和批量操作，所有这些都以线性时间运行。

这个类的迭代器方法返回的迭代器是快速失败的：如果在创建迭代器之后的任何时候修改了deque，除了通过迭代器自己的remove方法之外，迭代器通常会抛出ConcurrentModificationException。因此，在并发修改的情况下，迭代器快速而干净地失败，而不是在未来的未确定时间冒任意，非确定性行为的风险。

请注意，迭代器的快速失败行为无法得到保证，因为一般来说，在存在不同步的并发修改时，不可能做出任何硬性保证。失败快速迭代器会尽最大努力抛出ConcurrentModificationException。因此，编写依赖于此异常的程序以确保其正确性是错误的：迭代器的快速失败行为应该仅用于检测错误。

该类及其迭代器实现了Collection和Iterator接口的所有可选方法。


### 字段 ###
     //存储双端队列元素的数组。不包含deque元素的所有数组单元格始终为null。
     //该数组总是至少有一个空槽（在尾部）
     transient Object[] elements;

     //deque头部元素的索引（即remove()或pop()将删除的元素）; 或者任意数字0 <= head <elements.length如果deque为空则等于tail。
     transient int head;
     //将下一个元素添加到双端队列尾部的索引（通过addLast（E），add（E）或push（E））; elements [tail]始终为null。
     transient int tail;
     //最大容量
     private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

### 行为分析 ###



    public ArrayDeque() {
        elements = new Object[16];
    }

    public ArrayDeque(int numElements) {
        elements =
            new Object[(numElements < 1) ? 1 :
                       (numElements == Integer.MAX_VALUE) ? Integer.MAX_VALUE :
                       numElements + 1];
    }

默认构造方法里，创建了一个长度为16的数组，所以数组队列的默认容量是16，但是可以自定义初始容量的构造方法很有趣，
它允许穿进去负数，但最小值是1。还会判断容量是否是Integer.MAX_VALUE不是的话就在传进来参数的基础上加1。这代表着它的容量最小值是1。
