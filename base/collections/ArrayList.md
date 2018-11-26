## ArrayList

    public class ArrayList<E> extends AbstractList<E>
    implements List<E>, RandomAccess, Cloneable, Serializable


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

此类的·iterator·和·listIterator·方法返回的迭代器是**快速失败**的：如果列表在任何时间从结构上修改创建迭代器之后，以任何方式，
除了通过迭代器自身的remove或add方法，迭代器都将抛出ConcurrentModificationException。
因此，**在并发修改的情况下，迭代器快速而干净地失败**，而不是在未来的未确定时间冒任意，非确定性行为的风险。

请注意，迭代器的快速失败行为无法得到保证，因为一般来说，在存在不同步的并发修改时，不可能做出任何硬性保证。
失败快速迭代器会尽最大努力抛出ConcurrentModificationException。因此，编写依赖于此异常的程序以确保其正确性是错误的：
迭代器的快速失败行为应该仅用于检测错误。（java doc）

下面来分析一下，这个List,首先看它继承的类 [AbstractList][AbsList] ，这个抽象类是对于实现`RandomAccess`集合类应该继承的类。
然后看一下它实现的接口，`List`, `RandomAccess`,`Cloneable`,`Serializable`
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

[AbsList]: https://github.com/TransientWang/KnowledgeBase/blob/master/base/collections/list.md "AbstractList抽象类"
[clonable]: https://github.com/TransientWang/KnowledgeBase/blob/master/base/lang/Cloneable.md "Cloneable标志接口