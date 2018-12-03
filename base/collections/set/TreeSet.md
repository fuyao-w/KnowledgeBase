## TreeSet ##

    public class TreeSet<E> extends AbstractSet<E>
                 implements NavigableSet<E>, Cloneable, java.io.Serializable

### java doc ###

基于TreeMap的NavigableSet实现。元素按照它们的自然顺序排序，或者通过设定时间提供的比较器排序，具体取决于使用的构造函数。
此实现为基本操作（添加，删除和包含）提供了有保证的log（n）时间成本。

请注意由集合维护的集合必须与equals一致。(是否提供明确的比较器。有关与equals一致的精确定义，请参阅`Comparable`或`Comparator `。）从集合的角度来看，这种方法被认为是相等的。集合的行为即使其排序与equals不一致也是明确定义的;它只是不遵守set接口的一般契约。

请注意，此实现不同步。如果多个线程同时访问TreeSet，并且至少有一个线程修改了该集，则必须在外部进行同步。这是通过同步封装集合的某个对象来实现的。如果不存在搜索对象，则应使用Collections.synchronizedSortedSet方法“包装”该集合。这最好在创建时完成，以防止对集合的意外不同步访问：

    SortedSet s = Collections.synchronizedSortedSet（new TreeSet（...））;

这个类的迭代器方法返回的迭代器是快速失败的：迭代器是以任何方式创建的，除了通过迭代器自己的remove方法之外，迭代器想要抛出一个ConcurrentModificationException。因此，在并发修改的情况下，迭代器快速而干净地失败，而不是在未来的未确定时间冒任意，非确定性行为的风险。


### 字段 ###

    //存储元素的map
    private transient NavigableMap<E,Object> m;

    //与支持Map中的Object关联的虚拟值(value)
    private static final Object PRESENT = new Object();



### 构造方法 ###

    TreeSet(NavigableMap<E,Object> m) {
        this.m = m;
    }

    public TreeSet() {
        this(new TreeMap<>());
    }

    public TreeSet(Comparator<? super E> comparator) {
        this(new TreeMap<>(comparator));
    }

    public TreeSet(Collection<? extends E> c) {
        this();
        addAll(c);
    }


    public TreeSet(SortedSet<E> s) {
        this(s.comparator());
        addAll(s);
    }

### 分析 ###

TreeSet继承了AbstarctSet实现NavigableSet，通过构造方法可以看出，它是对TreeMap的封装。利用了Map中key的唯一性。
来实现有序的set。它的一些特性，就到TreeMap中去介绍。

在这里说一下其他的东西：

    floor​(E e) //返回此set中小于或等于给定元素的最大元素，如果没有这样的元素，则返回null。(<=)
    ceiling​(E e)//返回此set中大于或等于给定元素的最小元素，如果没有这样的元素，则返回null。(>=)
    lower​(E e)//返回此set中的最大元素严格小于给定元素，如果没有这样的元素，则返回null。(<)
    higher​(E e)//返回此set中严格大于给定元素的最小元素，如果没有这样的元素，则返回null。(>)

这几个方法是NavigableSet定义的方法，通过这几个方法可以，通过最接近匹配的原则获取元素。




