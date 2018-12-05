## PriorityQueue ##

    public class PriorityQueue<E> extends AbstractQueue<E> implements java.io.Serializable

### java doc ###
基于优先级堆的无界优先级队列。优先级队列的元素根据其自然顺序排序，或者由队列构造时提供的比较器排序，具体取决于使用的构造函数。优先级队列不允许null元素。依赖于自然排序的优先级队列也不允许插入不可比较的对象（这样做可能导致ClassCastException）。
此队列的头部是指定排序的最小元素。如果多个元素被绑定为最小值，则头部是这些元素之一 - 关系被任意打破。队列检索操作轮询，删除，查看和元素访问队列头部的元素。

优先级队列是无限制的，但具有内部容量，用于控制用于存储队列中元素的数组的大小。它始终至少与队列大小一样大。当元素添加到优先级队列时，其容量会自动增加。未指定增长政策的详细信息。

该类及其迭代器实现了Collection和Iterator接口的所有可选方法。方法iterator（）中提供的迭代器和方法spliterator（）中提供的Spliterator不保证以任何特定顺序遍历优先级队列的元素。如果需要有序遍历，请考虑使用Arrays.sort（pq.toArray（））。

请注意，此实现不同步。如果任何线程修改队列，则多个线程不应同时访问PriorityQueue实例。而是使用线程安全的PriorityBlockingQueue类。

实现说明：此实现为排队和出队方法提供O（log（n））时间（offer，poll，remove（）和add）; remove（Object）和contains（Object）方法的线性时间;和检索方法的持续时间（窥视，元素和大小）。

### 字段 ###


    private static final long serialVersionUID = -7720805057305804111L;
    
    private static final int DEFAULT_INITIAL_CAPACITY = 11;
    
    /**
     *  优先级队列表示为平衡二进制堆：两者
     *  queue [n]的子节点是队列[2 * n + 1]和队列[2 *（n + 1）]。
     *  如果比较器为空，则优先级队列由比较器或元素的自然顺序排序：对于
     *  堆中的每个节点n和n的每个后代d，n <= d。 假设队列是非空的，
     *  具有最低值的元素在队列[0]中。
     */
    transient Object[] queue; // 非私有，以简化嵌套类访问
    
    /**
     * 优先级队列中的元素数
     */
    int size;
    
    /**
     * 比较器，如果优先级队列使用元素的自然顺序，则为null
     */
    private final Comparator<? super E> comparator;
    
    /**
     * 此优先级队列已被结构修改的次数
     */
    transient int modCount;     // 非私有，以简化嵌套类访问


### 分析 ###

通过java doc和字段了解到，优先级队列通过维护一个队来，维护插入队列的顺序。而且他的初始容量为11。
还有一个比较器用于确定元素的顺序。

下面来看一个它的构造方法：

    public PriorityQueue(int initialCapacity) {
        this(initialCapacity, null);
    }
    
    public PriorityQueue(Comparator<? super E> comparator) {
        this(DEFAULT_INITIAL_CAPACITY, comparator);
    }
    
    public PriorityQueue(int initialCapacity,
                         Comparator<? super E> comparator) {
        // Note: This restriction of at least one is not actually needed,
        // but continues for 1.5 compatibility
        if (initialCapacity < 1)
            throw new IllegalArgumentException();
        this.queue = new Object[initialCapacity];
        this.comparator = comparator;
    }
    
    public PriorityQueue(Collection<? extends E> c) {
        if (c instanceof SortedSet<?>) {
            SortedSet<? extends E> ss = (SortedSet<? extends E>) c;
            this.comparator = (Comparator<? super E>) ss.comparator();
            initElementsFromCollection(ss);
        }
        else if (c instanceof PriorityQueue<?>) {
            PriorityQueue<? extends E> pq = (PriorityQueue<? extends E>) c;
            this.comparator = (Comparator<? super E>) pq.comparator();
            initFromPriorityQueue(pq);
        }
        else {
            this.comparator = null;
            initFromCollection(c);
        }
    }
    
    public PriorityQueue(PriorityQueue<? extends E> c) {
        this.comparator = (Comparator<? super E>) c.comparator();
        initFromPriorityQueue(c);
    }
    
    public PriorityQueue(SortedSet<? extends E> c) {
        this.comparator = (Comparator<? super E>) c.comparator();
        initElementsFromCollection(c);
    }

优先级队列的构造方法蛮多的，可以传入初始容量和比较器，也可以通过sortSet和其他的Collection来创建。

看一下`offer()`方法

    public boolean offer(E e) {
        if (e == null)
            throw new NullPointerException();
        modCount++;
        int i = size;
        if (i >= queue.length)
            grow(i + 1);
        siftUp(i, e);
        size = i + 1;
        return true;
    }

`offer()`方法会先判断并调用`grow()`扩容，然后调用`siftUp()`方法进行。

    private void grow(int minCapacity) {
        int oldCapacity = queue.length;
        // Double size if small; else grow by 50%
        int newCapacity = oldCapacity + ((oldCapacity < 64) ?
                                         (oldCapacity + 2) :
                                         (oldCapacity >> 1));
        // overflow-conscious code
        if (newCapacity - MAX_ARRAY_SIZE > 0)
            newCapacity = hugeCapacity(minCapacity);
        queue = Arrays.copyOf(queue, newCapacity);
    }

优先级队列的扩容方式与数组双端队列一样，64以下+2,以上扩大50%。通过`hugeCapacity()`控制int值溢出。
然后复制到新数组就可以了。

    private void siftUp(int k, E x) {
        if (comparator != null)
            siftUpUsingComparator(k, x, queue, comparator);
        else
            siftUpComparable(k, x, queue);
    }
    
    private static <T> void siftUpComparable(int k, T x, Object[] es) {
        Comparable<? super T> key = (Comparable<? super T>) x;
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            Object e = es[parent];
            if (key.compareTo((T) e) >= 0)
                break;
            es[k] = e;
            k = parent;
        }
        es[k] = key;
    }


优先级队列通过`siftUp()`方法使用元素默认的比较器添加新元素。如参是数组有效元素的长度，通过迭代方式，先找到父节点，
如果新元素比父节点大直接跳出循环添加新元素，否则将父节点元素放到最后一个元素的位置，然后将父节点的索引作为k，继续比较。
所以优先级队列维护的是一个最小堆。

    public boolean remove(Object o) {
        int i = indexOf(o);
        if (i == -1)
            return false;
        else {
            removeAt(i);
            return true;
        }
    }


`remove()`方法先确定要删除元素的位置：

    private int indexOf(Object o) {
        if (o != null) {
            final Object[] es = queue;
            for (int i = 0, n = size; i < n; i++)
                if (o.equals(es[i]))
                    return i;
        }
        return -1;
    }

就是通过按顺序比较，找到的。

    E removeAt(int i) {
        // assert i >= 0 && i < size;
        final Object[] es = queue;
        modCount++;
        int s = --size;
        if (s == i) // removed last element
            es[i] = null;
        else {
            E moved = (E) es[s];
            es[s] = null;
            siftDown(i, moved);
            if (es[i] == moved) {
                siftUp(i, moved);
                if (es[i] != moved)
                    return moved;
            }
        }
        return null;
    }

如果要删除的元素在最后一位，则直接删除。否则就需要调用`siftDown()`

    private void siftDown(int k, E x) {
        if (comparator != null)
            siftDownUsingComparator(k, x, queue, size, comparator);
        else
            siftDownComparable(k, x, queue, size);
    }
    
    private static <T> void siftDownComparable(int k, T x, Object[] es, int n) {
        // assert n > 0;
        Comparable<? super T> key = (Comparable<? super T>)x;
        int half = n >>> 1;           // loop while a non-leaf
        while (k < half) {
            int child = (k << 1) + 1; // assume left child is least
            Object c = es[child];
            int right = child + 1;
            if (right < n &&
                ((Comparable<? super T>) c).compareTo((T) es[right]) > 0)
                c = es[child = right];
            if (key.compareTo((T) c) <= 0)
                break;
            es[k] = c;
            k = child;
        }
        es[k] = key;
    }


### 总结 ###

优先级队列通过一个维护一个小顶堆，来实现优先级插入。初始容量11，扩容方式与ArrayDeque相同。
不能以恒定时间随机获取元素，故没有实现RandomAccess接口。