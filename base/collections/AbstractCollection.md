## AbstractCollection ##

该类提供了Collection接口的骨架实现，以尽量减少实现此接口所需的工作量。
该抽象类的方法都是基于迭代器实现的,为了实现一个不可修改的集合，程序员只需要扩展这个类并提供iterator和size方法的实现。 （ iterator方法返回的迭代器必须实现hasNext和next ） 
要实现可修改的集合，程序员必须另外覆盖此类的add方法（否则将抛出UnsupportedOperationException ），并且由iterator方法返回的迭代器必须另外实现其remove方法。

该类有一个变量 `static final int MAX_ARRAY_SIZE= Integer.MAX_VALUE - 8;`这个变量代表要分配的最大数组大小。 有些VM会在数组中保留一些header信息。 尝试分配更大的数组可能会导致OutOfMemoryError：请求的数组大小超过VM限制。

该类没有`get()`方法

该类也有两个抽象方法是子类必须实现的
1. `public abstract Iterator<E> iterator(); //返回此collection中包含的元素的迭代器。`
 
2. `public abstract int size();` 



