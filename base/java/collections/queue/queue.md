## Queue

```java
public interface Queue<E> extends Collection<E>
```

### java doc

设计用于在处理之前保持元素的集合。 除了基本的Collection操作外，队列还提供额外的插入，
提取和检查操作。 这些方法中的每一种都以两种形式存在：一种在操作失败时抛出异常，
另一种返回特殊值（null或false，具体取决于操作）。
后一种形式的插入操作专门用于容量限制的队列实现; 在大多数实现中，插入操作不会失败。

|         | Throws exception | Returns special value |
| :-----: | :--------------: | :-------------------: |
| Insert  |      add(e)      |       offer(e)        |
| Remove  |     remove()     |        poll()         |
| Examine |    element()     |        peek()         |




队列通常（但不一定）以FIFO（先进先出）方式对元素进行排序。其中的例外是优先级队列，它根据提供的比较器对元素进行排序，或者元素的自然顺序，以及LIFO队列（或栈），它们对元素LIFO（后进先出）进行排序。无论使用什么顺序，队列的头部是通过调用remove（）或poll（）来移除的元素。在FIFO队列中，所有新元素都插入队列的尾部。其他类型的队列可能使用不同的放置规则。每个Queue实现都必须指定其排序属性。

如果可能，offer方法插入一个元素，否则返回false。这与Collection.add方法不同，后者只能通过抛出未经检查的异常来添加元素。 offer方法设计用于当故障是正常而非异常发生时，例如，在固定容量（或“有界”）队列中。

`remove()`和`poll()`方法删除并返回队列的头部。确切地说，从队列中删除哪个元素是队列排序策略的一个功能，该策略因实现而异。 remove（）和poll（）方法的不同之处仅在于队列为空时的行为：remove（）方法抛出异常，而poll（）方法返回null。

`element()`和`peek()`方法返回但不删除队列的头部。

Queue接口未定义阻塞队列方法，这在并发编程中很常见。这些等待元素出现或空间变得可用的方法在BlockingQueue接口中定义，该接口扩展了该接口。

队列实现通常 **不允许插入null元素** ，尽管某些实现（如LinkedList）不禁止插入null。即使在允许它的实现中，也不应将null插入到Queue中，因为null也被poll方法用作特殊返回值，以指示队列不包含任何元素。

队列实现通常不定义基于元素的方法equals和hashCode版本，而是从Object类继承基于身份的版本，因为基于元素的相等性并不总是为具有相同元素但具有不同排序属性的队列定义良好。


### 分析 ###

Queue接口是队列实现的顶级接口继承自Collection，提供了实现队列最基本的操作。需要知道的是，这个接口里定义的方法有两个版本，一种在队列为空的时候直接抛出异常，
一种允许队列为空。


## Deque ##

```java
public interface Deque<E> extends Queue<E>
```

线性集合，支持两端插入和移除元素。 名称deque是“双端队列”的缩写，通常发音为“deck”。 大多数Deque实现对它们可能包含的元素数量没有固定的限制，但是此接口支持容量限制的deques以及没有固定大小限制的deques。
此接口定义了访问双端队列两端元素的方法。 提供了插入，移除和检查元素的方法。 这些方法中的每一种都以两种形式存在：一种在操作失败时抛出异常，另一种返回特殊值（null或false，具体取决于操作）。 后一种形式的插入操作专门设计用于容量限制的Deque实现; 在大多数实现中，插入操作不会失败。

上述十二种方法总结在下表中：

<table class="striped">
 <caption>Deque方法摘要</caption>
  <thead>
  <tr>
    <td rowspan="2"></td>
    <th scope="col" colspan="2"> First Element (Head)</th>
    <th scope="col" colspan="2"> Last Element (Tail)</th>
  </tr>
  <tr>
    <th scope="col" style="font-weight:normal; font-style:italic">Throws exception</th>
    <th scope="col" style="font-weight:normal; font-style:italic">Special value</th>
    <th scope="col" style="font-weight:normal; font-style:italic">Throws exception</th>
    <th scope="col" style="font-weight:normal; font-style:italic">Special value</th>
  </tr>
  </thead>
  <tbody>
  <tr>
    <th scope="row">Insert</th>
    <td><code>addFirst(e)</code></td>
    <td><code>offerFirst(e)</code></td>
    <td><code>addLast(e)</code></td>
    <td><code>offerLast(e)</code></td>
  </tr>
  <tr>
    <th scope="row">Remove</th>
    <td><code>removeFirst()</code></td>
    <td><code>pollFirst()</code></td>
    <td><code>removeLast()</code></td>
    <td><code>pollLast()</code></td>
  </tr>
  <tr>
    <th scope="row">例子</th>
    <td><code>getFirst()</code></td>
    <td><code>peekFirst()</code></td>
    <td><code>getLast()</code></td>
    <td><code>peekLast()</code></td>
  </tr>
  </tbody>
 </table>

 此接口扩展了Queue接口。 当deque用作队列时，会产生FIFO（先进先出）行为。 元素在双端队列的末尾添加并从头开始删除。 从Queue接口继承的方法与Deque方法完全等效，如下表所示：

 <table class="striped">
  <caption>Comparison of Queue and Deque methods</caption>
   <thead>
   <tr>
     <th scope="col"> <code>Queue</code> Method</th>
     <th scope="col"> Equivalent <code>Deque</code> Method</th>
   </tr>
   </thead>
   <tbody>
   <tr>
     <th scope="row"><code>add(e)</code></th>
     <td><code>addLast(e)</code></td>
   </tr>
   <tr>
     <th scope="row"><code>offer(e)</code></th>
     <td><code>offerLast(e)</code></td>
   </tr>
   <tr>
     <th scope="row"><code>remove()</code></th>
     <td><code>removeFirst()</code></td>
   </tr>
   <tr>
     <th scope="row"><code>poll()</code></th>
     <td><code>pollFirst()</code></td>
   </tr>
   <tr>
     <th scope="row"><code>element()</code></th>
     <td><code>getFirst()</code></td>
   </tr>
   <tr>
     <th scope="row"><code>peek()</code></th>
     <td><code>peekFirst()</code></td>
   </tr>
   </tbody>
  </table>
**Deques也可以用作LIFO（后进先出）的栈**。 应优先使用此接口，而不是传统的Stack类。 当deque用作堆栈时，元素将从双端队列的开头推出并弹出。 堆栈方法等同于Deque方法，如下表所示：

  <table class="striped">
   <caption>Comparison of Stack and Deque methods</caption>
    <thead>
    <tr>
      <th scope="col"> Stack Method</th>
      <th scope="col"> Equivalent <code>Deque</code> Method</th>
    </tr>
    </thead>
    <tbody>
    <tr>
      <th scope="row"><code>push(e)</code></th>
      <td><code>addFirst(e)</code></td>
    </tr>
    <tr>
      <th scope="row"><code>pop()</code></th>
      <td><code>removeFirst()</code></td>
    </tr>
    <tr>
      <th scope="row"><code>peek()</code></th>
      <td><code>getFirst()</code></a></td>
    </tr>
    </tbody>
   </table>


请注意，当deque用作队列或堆栈时，peek方法同样有效; 在任何一种情况下，元素都是从双端队列的开头绘制的。

此接口提供了两种方法来删除内部元素，`removeFirstOccurrence()`和`removeLastOccurrence()`。

与List接口不同，此接口不支持对元素的索引访问。

严格要求Deque实现禁止插入null元素, 强烈建议任何允许null元素的Deque实现的用户不要利用插入空值的能力。 这是因为null被各种方法用作特殊返回值，以指示deque为空。

Deque实现通常不定义equals和hashCode方|法的基于元素的版本，而是从Object类继承基于身份的版本。

### 行为分析 ###

双端队列接口继承了Queue接口，扩展了它的行为。两端都可以进行入队出队的操作。


## AbstractQueue ##

```java
public abstract class AbstractQueue<E> extends AbstractCollection<E> implements Queue<E>
```

### java doc
此类提供某些 Queue 操作的骨干实现。 当基本实现不允许null元素时，此类中的实现是适当的。 方法add，remove和element分别基于offer，poll和peek，但抛出异常而不是通过false或null返回指示失败。
扩展此类的Queue实现必须最低限度地定义一个方法Queue.offer(E)，该方法不允许插入null元素，以及Queue.peek()，Queue.poll()，Collection.size()和Collection方法.iterator()。 通常，还会覆盖其他方法。 如果无法满足这些要求，请考虑继承AbstractCollection。

### 分析 ###

这个抽象队列提供了队列的基本实现，可以通过继承这个队列来实现特定需求的队列。此类时优先级队列的父类。
