## ArrayList

    public class ArrayList<E> extends AbstractList<E> implements List<E>, RandomAccess, Cloneable, Serializable

### java doc ###
实现了List接口的可调整大小的基于数组的实现。实现所有可选列表操作，并允许所有元素（基本类型或是对象），
包括null。除了实现List接口之外，此类还提供了一些方法来操作内部用于存储列表的数组的大小。 （这个类大致相当于Vector，除了它是不同步的。）
size，isEmpty，get，set，iterator和listIterator操作以恒定时间运行。添加操作以分摊的常量时间运行，即添加n个元素需要O（n）时间。
所有其他操作都以线性时间运行（粗略地说）。与LinkedList实现相比，常数因子较低。

每个ArrayList实例都有一个容量。容量是用于存储列表中元素的数组的大小。它始终**至少**与列表大小一样大。随着元素添加到ArrayList，其容量会自动增加。
除了添加元素具有恒定的摊销时间成本这一事实之外，未指定增长策略的详细信息。

在使用ensureCapacity操作添加大量元素之前，应用程序可以增加ArrayList实例的容量。这可能会减少增量重新分配的数量。

**请注意，此实现不同步**。如果多个线程同时访问ArrayList实例，并且至少有一个线程在结构上修改了列表，则必须在外部进行同步。
（结构上的修改是指添加或删除一个或多个元素，或明确地调整列表大小的操作;仅设置元素的值不是结构修改。）这是一个典型地通过在同步一些对象自然封装该完成名单。
如果不存在此类对象，则应使用Collections.synchronizedList方法“包装”该列表。同步列表最好在创建时完成，以防止意外地不同步访问列表：

    List list = Collections.synchronizedList（new ArrayList（...））;

此类的`iterator`和`listIterator`方法返回的迭代器是**快速失败**的：如果列表在任何时间从结构上修改创建迭代器之后，以任何方式，
除了通过迭代器自身的remove或add方法，迭代器都将抛出ConcurrentModificationException。
因此，**在并发修改的情况下，迭代器快速而干净地失败**，而不是在未来的未确定时间冒任意，非确定性行为的风险。

**请注意，迭代器的快速失败行为无法得到保证，因为一般来说，在存在不同步的并发修改时，不可能做出任何硬性保证。**
失败快速迭代器会尽最大努力抛出ConcurrentModificationException。因此，编写依赖于此异常的程序以确保其正确性是错误的：
迭代器的快速失败行为应该仅用于检测错误。（java doc）


### 行为分析 ###
首先需要分析一下，这个类有的字段

    //序列化版本号，这样我们反序列化的时候即使更改了类的结构，也能保证成功。
    private static final long serialVersionUID = 8683452581122892189L;

    //默认初始容量,当创建ArrayList是如果没有向构造函数传入想要的初始容量的话，那么这个ArrayList默认的初始容量就是10.
    private static final int DEFAULT_CAPACITY = 10;

    //数组缓冲区，其中存储了ArrayList的元素。 Array List的容量是此数组缓冲区的长度。
    //添加第一个元素时，任何带有elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA(在下面)的空数组列表将扩展为DEFAULT_CAPACITY
    //ArrayList存储元素用到的数组，注意它被transient修饰，这代表它不会被序列化。
    transient Object[] elementData;

    //用于空实例的共享空数组实例,它主要在两部分会用到。如果我们设置了初始化容量是0。会把它赋值给elementData，
    //另一个地方是反序列化的时候，反序列化的元素数量为0，也会把它赋值给elementData。
    private static final Object[] EMPTY_ELEMENTDATA = {};

    ////用于默认大小的空实例的共享空数组实例。 我们将它与EMPTY ELEMENTDATA区分开来，以便知道在添加第一个元素时要膨胀多少
    private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};

    //列表里包含的元素的的大小
    private int size;

    //容量最大值，在AbstractCollection中介绍过。
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    //继承自AbstractList
    protected transient int modCount = 0;


了解完字段后，首先看一下它的构造方法：

     public ArrayList() {
            this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
        }

    public ArrayList(int initialCapacity) {
        if (initialCapacity > 0) { //如果初始容量>0 那么就创建一个新的长度为initialCapacity的，Object数组
            this.elementData = new Object[initialCapacity];
        } else if (initialCapacity == 0) {
            this.elementData = EMPTY_ELEMENTDATA;
        } else {
            throw new IllegalArgumentException("Illegal Capacity: "+
                                               initialCapacity);
        }
    }

    public ArrayList(Collection<? extends E> c) {
        elementData = c.toArray();
        if ((size = elementData.length) != 0) {
            // 防止c.toArray() 返回的数组不是Object类型数组，这样会影响这个泛型类
            if (elementData.getClass() != Object[].class)
                elementData = Arrays.copyOf(elementData, size, Object[].class);
        } else {
            // 用一个空数组代替.
            this.elementData = EMPTY_ELEMENTDATA;
        }
    }

有三个版本，默认的无参构造函数，将 DEFAULTCAPACITY_EMPTY_ELEMENTDATA赋值给elementData，另一个可以传入一个int值作为List的初始容量。
最后一个通过其他集合创建新的集合。

下面来看一下`add()`方法：

        public boolean add(E e) {
            modCount++;
            add(e, elementData, size);
            return true;
        }

这里先把modCount的值加1，然后调用重载方法`add(E e, Object[] elementData, int s)`：

    private void add(E e, Object[] elementData, int s) {
        if (s == elementData.length)
            elementData = grow();
        elementData[s] = e;
        size = s + 1;
    }

这个重载方法有在elementData已经满了的情况下调用grow()进行扩容操作`grow()`又调用了重载方法，参数是`size+1`也就是List里`add()`方法进入的元素数量+1，
但是需要注意一下放扩容成功后才会size+1：

    private Object[] grow(int minCapacity) {
        return elementData = Arrays.copyOf(elementData,
                                           newCapacity(minCapacity));
    }

可以看到grow()方法，复制了一个新的数组给elementData，这个过程又调用了newCapacity(int minCapacity)：

    private int newCapacity(int minCapacity) {
        // overflow-conscious code
        int oldCapacity = elementData.length;
        int newCapacity = oldCapacity + (oldCapacity >> 1);
        if (newCapacity - minCapacity <= 0) {
            if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA)
                return Math.max(DEFAULT_CAPACITY, minCapacity);
            if (minCapacity < 0) // overflow
                throw new OutOfMemoryError();
            return minCapacity;
        }
        return (newCapacity - MAX_ARRAY_SIZE <= 0)
            ? newCapacity
            : hugeCapacity(minCapacity);
    }

这个方法是执行扩容操作的关键，首先通过位操作将原数组的容量增大了1.5倍。然后判断了扩容后的值是否小于最小容量（size+1）。
因为当我们初始化数组长度为0的时候，newCapacity的值还是0，而minCapacity为 0+1，或者初始化长度为1，2,3,的时候，minCapacity也为1，2,3。
还有一种情况是，扩容操作后newCapacity超过了Integer.MaxValue(2147483647)后就会变成负数。这个判断就是来处理这几种情况的。 如果进入了这个if里面则会判断elementData长度是否是0，
然后返回minCapacity和ArrayList的最大值，但是正常情况选肯定会返回DEFAULT_CAPACITY。除非通过反射更改了size，或是其他情况。
然后还要判断当minCapacity已经超过了Integer.MaxValue后变成负数的情况。会直接抛出内存不足的异常。如果没有进入到if里就会判断扩容后的容量是否小于Integer.MaxValue-8，
如果小于返回扩容后的值，如果大于会判断minCapacity是否大于MAX_ARRAY_SIZE ，小于返回Integer.MaxValue，大于返回MAX_ARRAY_SIZE。
不要被这些繁琐的判断绕晕，注意minCapacity是List里已有元素的数量+1，oldCapacity在扩容后超过了MAX_ARRAY_SIZE时，size还没有这么大，
知道数组里元素的容量已经大于MAX_ARRAY_SIZE时候，才会返回Integer.MaxValue，在这之前都不会进行扩容操作了。


### modCount行为 ###

剩下的方法，原理跟`add()`大同小异。下面通过观察迭代器来研究modCount的行为：

     private class Itr implements Iterator<E> {
            int cursor;       // index of next element to return
            int lastRet = -1; // index of last element returned; -1 if no such
            int expectedModCount = modCount;

            // prevent creating a synthetic constructor
            Itr() {}

            public boolean hasNext() {
                return cursor != size;
            }

            @SuppressWarnings("unchecked")
            public E next() {
                checkForComodification();
                int i = cursor;
                if (i >= size)
                    throw new NoSuchElementException();
                Object[] elementData = ArrayList.this.elementData;
                if (i >= elementData.length)
                    throw new ConcurrentModificationException();
                cursor = i + 1;
                return (E) elementData[lastRet = i];
            }

就截取到这里，需要注意expectedModCount变量他被赋值为modCount。然后在看一下`next()`

    final void checkForComodification() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }

这是next()的首先执行的方法，就是判断modCount与expectedModCount是否相等。那么什么情况下会不相等呢,还记得`add()`方法吗，
它首先会将modCount+1，在其他的方法里也一样，改变了elementData里的元素多少次modCount就加几次。但是除了迭代器之外的方法里都没有执行这个方法。
也就是说它的第一个功能就是记录修改数组元素的操作次数。但是并不会出现modCount与expectedModCount不相等的情况，那就只有在使用迭代器里会发生不相等的情况了。

    public void test() {
        ArrayList<Integer> list = new ArrayList<>(9);
        list.add(1);
        list.add(2);
        list.add(3);
        Iterator<Integer> integerIterator = list.listIterator(); //返回一个新的迭代器对象
        for (int i = 0; i < list.size(); i++) {
            list.add(i + 10);
            if(integerIterator.hasNext()){
                integerIterator.next();
            }
        }

    }

运行这个方法，会抛出`java.util.ConcurrentModificationException`，说明modCount与expectedModCount不相等了，
它是怎么造成的呢？在第一次执行add()方法之前，就已经创造好了一个新的迭代器对象，expectedModCount值就已经固定了。
这时候add()方法缺改变了modCount的值，造成了不相等，所以程序fast-fail，抛出异常。现在看**在for遍历与迭代器一起使用的时候，程序会fast-fail**。


下面再看一下另一个例子：


        public void test() {
            ArrayList<Integer> list = new ArrayList<>(9);
            list.add(1);
            list.add(2);
            list.add(3);
            Iterator<Integer> integerIterator = list.listIterator();
            for (int i = 0; i < 100; i++) {
                new Thread(() -> {
                    while (integerIterator.hasNext()) {
                        ((ListIterator<Integer>) integerIterator).add(1);
                        integerIterator.next();
                    }
                    System.out.println(list.size());
                }).start();
            }

        }

这段程序也会抛出异常，还不一定是一种异常。他抛出了`NoSuchElementException`,`ConcurrentModificationException`。但不是立即就抛出
如果只开启10个线程。有可能不会抛出异常。这是modeCount的第二个作用：**在多线程条件下可能的不确定行为时，保证迭代器的fast-fail。**


#### 注意 ####
看下面一段程序会发生什么：

    public void test() {
        ArrayList<Integer> list = new ArrayList<>(1000);
        for (int i = 0; i < 1000; i++) {
            list.add(i);
        }

        for (int i = 0; list.iterator().hasNext(); i++) {
            System.out.println(list.get(i));
        }

    }

这段程序会抛出异常，是什么异常？空指针异常吗？

    java.lang.IndexOutOfBoundsException: Index 1000 out of bounds for length 1000

它会抛出数组越界异常，来分析一下。首先创建了一个ArrayList并且为`add()`进去1000个数，下面的for循环是重点。
它的递增条件是 list调用iterator()方法,返回的迭代器对象再调用'hasNext()'

看一下iterator()的实现：

    public Iterator<E> iterator() {
        return new Itr();
    }

返回的迭代器是新创建出来的，也就是说每次在for中执行这里时都返回了一个新的对象，而且List里本来也有1000个元素。
所以调用`hasNext()`每次都会判定成功。然后在看一下get方法：

    public E get(int index) {
        Objects.checkIndex(index, size);
        return elementData(index);
    }

第一行，执行的方法会核对index是否越界。抛出`IndexOutOfBoundsException`。这段程序重点在是否了解获取迭代器的过程。


### 其他实现接口 ###
最后看一次下这个类实现的一些其他接口里有什么需要注意的：`List`, `RandomAccess`,`Cloneable`,`Serializable`
前两个之前都介绍过了，后面的 [`Cloneable`] [clonable]代表着这个List可以实现克隆的功能，看一下他是怎么实现的：

        public Object clone() {
            try {
                ArrayList<?> v = (ArrayList<?>) super.clone();
                v.elementData = Arrays.copyOf(elementData, size);
                v.modCount = 0;
                return v;
            } catch (CloneNotSupportedException e) {
                // this shouldn't happen, since we are Cloneable
                throw new InternalError(e);
            }
        }

它的实现就是克隆ArrayList，再将elementData复制一份过去。所以你克隆后的只相当于新创建了一个List
对象，但是如果List里面的元素不是基本类型而是对象的话，那么你就需要注意了，现在这两个List里面的对象
引用指向了相同的地址。这相当于一个浅克隆，所以使用它的克隆方法需要注意一下。

它还实现了序列化接口:


      private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {
            // Write out element count, and any hidden stuff
            int expectedModCount = modCount;
            s.defaultWriteObject();

            // Write out size as capacity for behavioral compatibility with clone()
            s.writeInt(size);

            // Write out all elements in the proper order.
            for (int i=0; i<size; i++) {
                s.writeObject(elementData[i]);
            }

            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }


这是ArrayList的序列化方法，是一个私有方法，ObjectOutputStream会通过反射调用这个类的writeObject方法进行序列化，ObjectInputStream会调用相应的readObject方法进行反序列化。
可以看到，这个方法通过modCount保证线程安全。而且ArrayList的序列化只是所保存元素的序列化。因为List只是一个存储数据的容器，把它序列化并无任何意义。

### 其他注意点 ###

ArrayList是一个泛型类，在编译期保证类型安全。但是不用泛型创建一个新的ArrayList可以吗？当然是可以的

    @Test
    public void test4() {
        ArrayList list = new ArrayList<>(9);
        list.add(1);
        list.add(1.1);
        list.add(1f);
        list.add(1L);
        list.add("test");
        list.forEach(System.out::println);
    }

    console output:

    1
    1.1
    1.0
    1
    test

后面的尖括号去掉也没有问题。

### 总结 ###

ArrayList结构比较简单，需要关注的地方。就是扩容机制，fast-fail。

[AbsList]: https://github.com/TransientWang/KnowledgeBase/blob/master/base/collections/list.md "AbstractList抽象类"


[clonable]: https://github.com/TransientWang/KnowledgeBase/blob/master/base/lang/Cloneable.md "Cloneable标志接口"