## LinkedList ##

```java
public class LinkedList<E> extends AbstractSequentialList<E> implements List<E>, Deque<E>, Cloneable, java.io.Serializable
```

### java doc ###

List和Deque接口的双链表实现。实现所有可选列表操作，并允许所有元素（包括null）。
对于双向链表，所有操作都可以预期。索引到列表中的操作将从开头或结尾遍历列表，以较接近指定索引为准。

请注意，此实现不同步。如果多个线程同时访问链表，并且至少有一个线程在结构上修改了列表，
则必须在外部进行同步。 （结构修改是添加或删除一个或多个元素的任何操作;仅设置元素的值不是结构修改。）
这通常通过同步自然封装列表的某个对象来完成。如果不存在此类对象，则应使用Collections.synchronizedList方法“包装”该列表。
这最好在创建时完成，以防止意外地不同步访问列表：

   List list = Collections.synchronizedList(new LinkedList(...));

此类的iterator和listIterator方法返回的迭代器是快速失败的：如果在创建迭代器之后的任何时候对列表进行结构修改，
除了通过Iterator自己的remove或add方法之外，迭代器将抛出ConcurrentModificationException。
因此，在并发修改的情况下，迭代器快速而干净地失败，而不是在未来的未确定时间冒任意，非确定性行为的风险。

请注意，迭代器的快速失败行为无法得到保证，因为一般来说，在存在不同步的并发修改时，不可能做出任何硬性保证。
失败快速迭代器会尽最大努力抛出ConcurrentModificationException。因此，编写依赖于此异常的程序以确保其正确性是错误的：迭代器的快速失败行为应该仅用于检测错误。（java doc）

LinkedList没有实现RandomAccess,所以它的不能以恒定时间获取元素，通过它的名字看，它是通过链表实现的。


### 字段 ###

```java
transient int size = 0;

//第一个Node
transient Node<E> first;

//最后一个Node
transient Node<E> last;
```


LinkedList的字段比较简单，只有三个。

### 行为分析 ###

由于LinkedList是通过链表实现的，增加元素的时候就是在当前链表最后面新加一个节点。并不会涉及到扩容的问题。

```java
    private static class Node<E> {
        E item;
        Node<E> next;
        Node<E> prev;

        Node(Node<E> prev, E element, Node<E> next) {
            this.item = element;
            this.next = next;
            this.prev = prev;
        }
    }
```


```java
void linkLast(E e) {
    final Node<E> l = last;
    final Node<E> newNode = new Node<>(l, e, null);
    last = newNode;
    if (l == null)
        first = newNode;
    else
        l.next = newNode;
    size++;
    modCount++;
}
```

第一个方法是节点的实现，有保存前一个和后一个的节点，后一个是`add()`方法的具体实现，可以看到行为就是新建一个节点放在最后面 ，并将新节点赋值给last。

LinedList的迭代器，迭代器有两个，一个是 正序迭代器，一个是降序迭代器。LinkdeList还提供了`pop()`,`poll()`这样的队列行为的方法，
所以可以通过LinkedList来创建一个链表实现的队列。

### 总结 ###

总结一下LinkedList个ArrayList的异同：

相同：
>他们两个都实现了List接口，基本的方法都相同
>他们两个都有fast-fail机制

不同：

>>他们两个的实现数据结构不同，一个是数组，另一个是双向链表。
>
>>ArrayList有初始容量，和扩容机制，LinkedList不需要。
>
>>LinkedList继承自AbstractSequentialList这是一个没有实现RandomAccess接口的抽象类，代表其不能以恒定时间获取元素。
>>遍历LindedList用迭代器速度较快<p>
>>ArrayList继承自AbstractList一个实现了RandomAccess的抽象类，代表可以以恒定的时间获取到元素，遍历它用for循环较快
>
>>LinkdeList实现了基于双端队列行为的方法，可以直接通过其创建一个链表队列<p>
>>ArrayList不能用于创建别的数据结构