## RandomAccess（标志接口） ##

List实现使用的标记界面，表明它们支持快速（通常为恒定时间）随机访问。 此接口的主要目的是允许通用算法更改其行为，以便在应用于随机访问列表或顺序访问列表时提供良好的性能。


一般实现了这个接口的list用for循环比用迭代器循环快,比如ArrayList，没有实现这个接口的list用迭代器遍历比用for循环更快比如LinkedList。
        
        
            public static <T> int binarySearch(List<? extends Comparable<? super T>> list, T key) {
                if (list instanceof RandomAccess || list.size()<BINARYSEARCH_THRESHOLD)
                    return Collections.indexedBinarySearch(list, key);
                else
                    return Collections.iteratorBinarySearch(list, key);
            }


这是Collections工具类下的一个二分法搜索方法，在这里面首先用 instanceif 判断该List是否实现了RandomAccess接口，如果实现了RandomAccess
接口则调用indexedBinarySearch方法来处理：

        int indexedBinarySearch(List<? extends Comparable<? super T>> list, T key) {
            int low = 0;
            int high = list.size()-1;
    
            while (low <= high) {
                int mid = (low + high) >>> 1;
                Comparable<? super T> midVal = list.get(mid);
                int cmp = midVal.compareTo(key);
    
                if (cmp < 0)
                    low = mid + 1;
                else if (cmp > 0)
                    high = mid - 1;
                else
                    return mid; // key found
            }
            return -(low + 1);  // key not found
        }
        
它获取到minVal值是调用传入的List所实现的get方法。比如ArrayList.get()：

            public E get(int index) {
                Objects.checkIndex(index, size);
                return elementData(index);
            }
            E elementData(int index) {
                return (E) elementData[index];
            }
        
而没有实现RandomAccess接口的List则用iteratorBinarySearch处理：
        
        
            private static <T> int iteratorBinarySearch(List<? extends T> l, T key, Comparator<? super T> c) {
                int low = 0;
                int high = l.size()-1;
                ListIterator<? extends T> i = l.listIterator();
        
                while (low <= high) {
                    int mid = (low + high) >>> 1;
                    T midVal = get(i, mid);
                    int cmp = c.compare(midVal, key);
        
                    if (cmp < 0)
                        low = mid + 1;
                    else if (cmp > 0)
                        high = mid - 1;
                    else
                        return mid; // key found
                }
                return -(low + 1);  // key not found
            }
            
这个方法在获取midVal值的时候则是用了Colltctions工具类下get方法是实现的:
   
         private static <T> T get(ListIterator<? extends T> i, int index) {
                T obj = null;
                int pos = i.nextIndex();
                if (pos <= index) {
                    do {
                        obj = i.next();
                    } while (pos++ < index);
                } else {
                    do {
                        obj = i.previous();
                    } while (--pos > index);
                }
                return obj;
            }

可以看到它只能通过迭代器，逐个的遍历直到中间索引的位置。

所以如果对LinkedList这样没有实现随机访问接口的List用for循环迭代的话，每次获取index位置的时候都会从0一直迭代到需要的位置，
下面是LinkedList.get()的关键代码：
    
      Node<E> node(int index) {
            // assert isElementIndex(index);
    
            if (index < (size >> 1)) {
                Node<E> x = first;
                for (int i = 0; i < index; i++)
                    x = x.next;
                return x;
            } else {
                Node<E> x = last;
                for (int i = size - 1; i > index; i--)
                    x = x.prev;
                return x;
            }
        }

而实现了随机访问接口的List比如ArrayList.get()(上面已经展示过)则会以恒定的时间获取到索引位置的值，所以更适合用for来迭代。


## AbstractList ##

如果想要了解此类，有必要先了解一下[AbstractCollection][AbstractCollection]类，该抽象类此类提供的骨干实现的List接口以
最小化来实现该接口由一个“随机访问”数据存储备份所需的工作（如阵列）。 对于顺序存取的数据（如链接列表）， AbstractSequentialList应优先使用此类。 
要实现一个不可修改的列表，程序员只需要扩展这个类并提供get(int)和size()方法的实现。 


要实现可修改的列表，程序员必须另外覆盖set(int, E)方法（否则会抛出一个UnsupportedOperationException ）。 如果列表是可变大小，则程序员必须另外覆盖add(int, E)和remove(int)方法。 
根据Collection接口规范中的建议，程序员通常应该提供一个void（无参数）和集合构造函数。 


不像其他的抽象集合实现，程序员不必提供迭代器实现; 迭代器和列表迭代器由此类实现的，对的“随机访问”方法上： get(int) ， set(int, E) ， add(int, E)和remove(int) 。 
我们可以通过继承这个类实现自己需要的List（源自官方文档）
    
该类有一个字段`protected transient int modCount = 0;` []()

>代表这个list的结构已经被修改过的次数，结构修改是那些改变list的size属性，或者其他方式如迭代进度可能会产生不正确的结果
>这个字段用在迭代器和列表迭代器实现中，如果此字段的值意外更改，则迭代器(list迭代器)，将在响应迭代器的next方法，remove方法，previous(),set(),add()等方法
>抛出concurrentmodificationexception 异常，这提供了fast-fail 的行为，而不是在面对迭代过程中，并发修改的非确定性行为。<p>
>子类们使用这个字段是可选的，如果一个子类希望提供快速失败的迭代器(lsit迭代器),那么 它仅仅需要增加这个字段在他的 add，remove，方法
(或者其他任何修改了list结构的方法)。在一次调用add(int)或者remove(int)中，modeCount的值只能增加1。否则，迭代器或者list迭代器将抛出
ConcurrentModificationExceptions 异常。如果一个实现不希望提供快速失败迭代器，这个字段可以忽视。简单的说就是使用该字段让List确保只有被单一线程修改.
将在分析ArrayList时具体分析modCount的行为，同时会举一个例子，


该类实现了两个私有化子类` class Itr implements Iterator<E>`和`class ListItr extends Itr implements ListIterator<E>`
>Iterator 集合类的迭代器 Enumeration的替代者允许调用者在迭代期间使用明确定义的语义从底层集合中删除元素,并改进了方法名<p>
>ListIterator 用于允许程序员沿任一方向遍历列表的列表的迭代器，在迭代期间修改列表，并获取列表中迭代器的当前位置。

该类实现了AbstractCollection里的抽象方法`iterator()`返回 `new Itr()`,又添加了一个 `listIterator()`方法,返回 `new ListItr()`,
还有一个抽象`get()`方法，没有实现AbstractCollection类的`size()`方法。所以子类必须要实现`get()`和`size()`方法。
另外，如果子类想要能够修改元素，还需要重写 add(), set(), remove() 方法，否则直接抛出UnsupportedOperationException异常。

 
该类添加了两个静态内部类`RandomAccessSubList`和`SubList` ，这两类是`subList()`方法根据实现的List是否实现了RandomAccess接口而返回的类,
通过这两个类操作外部类的数据。


## AbstractSequentialList ##

该类不与之前介绍的两个类在同一层，但是它也是一个抽象类，并且是LinkedList的父类，所以在本页来分析这个类

此类提供List接口的骨干实现，以最大限度地减少实现由“顺序访问”的List（例如链表）支持的此接口所需的工作量。
对于随机访问数据（例如数组），应优先使用AbstractList而不是此类。这个类与AbstractList类相反，
它在List的listIterator上实现了可以“随机访问”的方法（get（int index），set（int index，E element），add（int index，E element）
和remove（int index） ））要实现列表，程序员只需要扩展此类并提供listIterator和
size方法的实现。对于不可修改的列表，程序员只需要实现列表迭代器的hasNext，next，hasPrevious，previous和index方法。
对于可修改的列表，程序员还应该实现list迭代器的set方法。对于可变大小的列表，程序员还应该实现列表迭代器的remove和add方法。
程序员通常应该根据Collection接口规范中的建议提供void（无参数）和集合构造函数。

以上是`java doc`原话，下面分析一下这些话的意义，以`get()`方法为例子：

     public E get(int index) {
            try {
                return listIterator(index).next();
            } catch (NoSuchElementException exc) {
                throw new IndexOutOfBoundsException("Index: "+index);
            }
        }

`AbstractSequentialList`并没有实现`RandomaAccess`接口，设置数据的的实现是通过`listIterator`的迭代进行的，上面提到的其他三个方法也都是通过`listIterator`实现的。
如果我们实现自己的List而且是通过链表这类的数据结构存储的话，我们应该去继承这个类实现保存数据的数据结构和`listIterator`的相应方法，来实现自己的List。
当然上面已经提供好的方法也可以重写，就像`LinkedList`一样。




[AbstractCollection]:  https://github.com/TransientWang/KnowledgeBase/blob/master/base/collections/AbstractCollection.markdown "AbstractCollection抽象类"