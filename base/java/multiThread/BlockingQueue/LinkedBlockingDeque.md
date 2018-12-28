## lockingDeque



### java doc

Deque还支持阻塞操作，这些操作在检索元素时等待deque变为非空，并在存储元素时等待deque中的空间可用。
BlockingDeque方法有四种形式，有不同的处理操作的方法，不能立即满足，但可能在将来的某个时候满足：一个抛出异常，第二个返回一个特殊值（null或false，取决于 操作），第三个无限期地阻塞当前线程直到操作成功，并且第四个块在放弃之前仅用于给定的最大时间限制。 这些方法总结在下表中：

| First Element (Head) |                      |                   |                |                           |
| -------------------- | -------------------- | ----------------- | -------------- | ------------------------- |
|                      | **Throws exception** | **Special value** | **Blocks**     | **Times out**             |
| Insert               | addFirst             | offerFirst(e)     | putFirst(e)    | offerFirst(e, time, unit) |
| Remove               | removeFirst()        | pollFirst()       | takeFirst()    | pollFirst(time, unit)     |
| Examine              | getFirst()           | peekFirst()       | not applicable | not applicable            |
| Last Element (Tail)  |                      |                   |                |                           |
|                      | **Throws exception** | **Special value** | **Blocks**     | **Times out**             |
| Insert               | addLast(e)           | offerLast(e)      | putLast(e)     | offerLast(e, time, unit)  |
| Remove               | removeLast()         | pollLast()        | takeLast()     | pollLast(time, unit)      |
| Examine              | getLast()            | peekLast()        | not applicable | not applicable            |

与任何BlockingQueue一样，BlockingDeque是线程安全的，不允许null元素，并且可能（或可能不）是容量约束的。

BlockingDeque实现可以直接用作FIFO BlockingQueue。 从BlockingQueue接口继承的方法与BlockingDeque方法完全等效，如下表所示：

|                      | `BlockingQueue` Method   | Equivalent `BlockingDeque` Method |
| -------------------- | ------------------------ | --------------------------------- |
| Insert               | add(e)                   | addLast(e)                        |
| offer(e)             | offerLast(e)             |                                   |
| put(e)               | putLast(e)               |                                   |
| offer(e, time, unit) | offerLast(e, time, unit) |                                   |
| Remove               | remove()                 | removeFirst()                     |
| poll()               | pollFirst()              |                                   |
| take()               | takeFirst()              |                                   |
| poll(time, unit)     | pollFirst(time, unit)x   |                                   |
| Examine              | element()                | getFirst()                        |
| peek()               | peekFirst()              |                                   |

内存一致性影响：与其他并发集合一样，在将对象放入BlockingDeque之前，线程中的操作发生在从另一个线程中的BlockingDeque访问或删除该元素之后的操作之前。

## LinkedBlockingDeque

```java
public class LinkedBlockingDeque<E>
    extends AbstractQueue<E>
    implements BlockingDeque<E>, java.io.Serializable
```

### java doc

基于链接节点的可选绑定阻塞双端队列。

可选的容量绑定构造函数参数用作防止过度扩展的方法。 如果未指定，则容量等于Integer.MAX_VALUE。 每次插入时都会动态创建链接节点，除非这会使deque超出容量。

大多数操作都是在恒定时间内运行（忽略阻塞时间）。 例外包括remove，removeFirstOccurrence，removeLastOccurrence，contains，iterator.remove（）和批量操作，所有这些都以线性时间运行。

该类及其迭代器实现了Collection和Iterator接口的所有可选方法。

### 分析

阻塞双端队列，原理与普通的队列差不多，就不在分析。