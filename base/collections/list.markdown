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


##AbstractList ##

如果想要了解此类，有必要先了解一下[AbstractCollection][AbstractCollection]类，该抽象类此类提供的骨干实现的List接口以
最小化来实现该接口由一个“随机访问”数据存储备份所需的工作（如阵列）。 对于顺序存取的数据（如链接列表）， AbstractSequentialList应优先使用此类。 
要实现一个不可修改的列表，程序员只需要扩展这个类并提供get(int)和size()方法的实现。 


要实现可修改的列表，程序员必须另外覆盖set(int, E)方法（否则会抛出一个UnsupportedOperationException ）。 如果列表是可变大小，则程序员必须另外覆盖add(int, E)和remove(int)方法。 
根据Collection接口规范中的建议，程序员通常应该提供一个void（无参数）和集合构造函数。 


不像其他的抽象集合实现，程序员不必提供迭代器实现; 迭代器和列表迭代器由此类实现的，对的“随机访问”方法上： get(int) ， set(int, E) ， add(int, E)和remove(int) 。 
我们可以通过继承这个类实现自己需要的List
    
[AbstractCollection]:  https://github.com/TransientWang/KnowledgeBase/blob/master/base/collections/AbstractCollection.markdown "AbstractCollection抽象类"