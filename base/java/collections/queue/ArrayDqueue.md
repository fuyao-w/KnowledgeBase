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
这是java11的源码，它和java8的源码有一些不同。


    public  void test(){
        ArrayDeque i = new ArrayDeque();
        i.addFirst(1);
        i.addLast(9);
    }

通过上面的这段程序来分析它的行为，第一行由于没有设置默认的初始值，那么就会执行上面的无参构造方法。默认构造方法里，创建了一个长度为16的数组，所以数组队列的默认容量是16。

    public ArrayDeque() {
        elements = new Object[16];
    }

然后执行`addFirst()`：

    public void addFirst(E e) {
        if (e == null)
            throw new NullPointerException();
        final Object[] es = elements;
        es[head = dec(head, es.length)] = e;
        if (head == tail)
            grow(1);
    }

首先会判断传进来的参数是否为空，是则抛出异常。然后将保存元素的elements数组，的引用赋值给一个叫es的final类型的数组。
然后下一行下先将head,和es数组的长度作为参数传递给了`dec()`：

    static final int dec(int i, int modulus) {
        if (--i < 0) i = modulus - 1;
        return i;
    }

此时head是默认的0，es.length()是默认的16。所以这个方法，判断头指针是否在0的位置，如果是则返回数组长度减1，不是则返回i--。
跳出这个方法后，返回值被赋值给了头指针，然后将e放到数组的相应位置中。那么因为head此时默认为0，所以dec方法返回了16-1=15 给head，
现在头指针移动到了，elements数组的末尾。而且elements[15]此时等于1。`dec()`的作用是做什么的呢?根据java doc和刚才的分析。
它在`addFirst()`方法中起到了控制head指针位置的作用，确保0 <= head < elements.length()。
但是通过源码可以看到`dec()`并没有针对小于0的情况做处理，那它是怎么保证 head >= 0呢？我们需要通过grow方法来分析，
但是我们现在添加的元素不足以进行扩容，head指针在15的位置，tail现在在0的位置。所以，先不看`grow()`，继续看`addLast()`:

        public void addLast(E e) {
            if (e == null)
                throw new NullPointerException();
            final Object[] es = elements;
            es[tail] = e;
            if (head == (tail = inc(tail, es.length)))
                grow(1);
        }

        static final int inc(int i, int modulus) {
            if (++i >= modulus) i = 0;
            return i;
        }

`addLast`因为是数组索引位置0开始计算的，索引直接将e放到tail的位置，然后会通过`inc()`将tail设置为下一个应该放置元素的位置，
方法的入参是tail和elements.length()，不难看出`inc()`保证了 0 <= tail < elements.length()。如果已经满了就返回0。
如果tail返回0，那么就说明之前head指针没变过，这时候tail==head就会进行扩容。

现在换一组测试：

    public void test(){
        ArrayDeque i = new ArrayDeque(2);
        i.addFirst(1);
        i.addLast(9);
        i.addLast(9);
    }

现在将队列的初始容量变成2，然后添加3个元素。这样就可以进性扩容。

    public ArrayDeque(int numElements) {
        elements =
            new Object[(numElements < 1) ? 1 :
                       (numElements == Integer.MAX_VALUE) ? Integer.MAX_VALUE :
                       numElements + 1];
    }


程序调用了参数为初始容量的构造方法，它允许穿进去负数，但最小值是1。还会判断容量是否是Integer.MAX_VALUE不是的话就在传进来参数的基础上加1。这代表着它的容量最小值是1。
现在初始的容量就会变成 2+1 = 3 然后程序会向下执行，知道执行到最后一行`addLast()`，这之前head指正在2的位置，tail指针在1的位置。
现在tail指针向后移动了一位，tail和head相遇了。

    private void grow(int needed) {
        // overflow-conscious code
        final int oldCapacity = elements.length;
        int newCapacity;
        // Double capacity if small; else grow by 50%
        int jump = (oldCapacity < 64) ? (oldCapacity + 2) : (oldCapacity >> 1);
        if (jump < needed
            || (newCapacity = (oldCapacity + jump)) - MAX_ARRAY_SIZE > 0)
            newCapacity = newCapacity(needed, jump);
        final Object[] es = elements = Arrays.copyOf(elements, newCapacity);
        // Exceptionally, here tail == head needs to be disambiguated
        if (tail < head || (tail == head && es[head] != null)) {
            // wrap around; slide first leg forward to end of array
            int newSpace = newCapacity - oldCapacity;
            System.arraycopy(es, head,
                             es, head + newSpace,
                             oldCapacity - head);
            for (int i = head, to = (head += newSpace); i < to; i++)
                es[i] = null;
        }
    }

`grow(1)`方法开始会将之前elements数组的容量保存下来，然后确定扩容容量jump。如果旧容量 < 64,新容量为2*oldCapacity+2，否则新容量就会在旧容量的基础上增大50%。
如果jump<need（这种情况在addLast方法里不会发生）或者在扩容到一定程度扩容后的值大于2147483647 - 8。会调用`newCapacity()`。

    /** Capacity calculation for edge conditions, especially overflow. */
    private int newCapacity(int needed, int jump) {
        final int oldCapacity = elements.length, minCapacity;
        if ((minCapacity = oldCapacity + needed) - MAX_ARRAY_SIZE > 0) {
            if (minCapacity < 0)
                throw new IllegalStateException("Sorry, deque too big");
            return Integer.MAX_VALUE;
        }
        if (needed > jump)
            return minCapacity;
        return (oldCapacity + jump - MAX_ARRAY_SIZE < 0)
            ? oldCapacity + jump
            : MAX_ARRAY_SIZE;
    }

`newCapacity()`方法作用就是是控制int溢出。接下来会将旧数组，上的数据复制到新的数组上去。
因为扩容之后head==tail如果就这样加入新的元素，那么肯定会出现错误。所以，还需要做的就是，将从head到最后得元素转移到正确的地方去。
然后将原来不应该存在元素的部分的元素赋空值，等待垃圾回收。


### 总结 ###

ArrayDeque没有实现`RandomAccess`接口，因为它只能从两端获取元素。它的迭代器也是快速失败的。

初始容量默认16，最小值1。扩容方式：元素个数小于64每次+2，大于64以1.5倍扩容。`addFirst()`方法操作数组的末尾，`addLast()`方法操作数组的起始位置。
tail在调用`addLast()`的时候回移动到添加所元素下一位，以通过tail==head来判断是否应该扩容。
有两个迭代器，分别遍历队列的两端。

`addFirst()`与`poolLast()`组合可以实现栈的功能，反之亦然。










