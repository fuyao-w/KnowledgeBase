### java doc

一个队列，它还支持在检索元素时等待队列变为非空的操作，并在存储元素时等待队列中的空间可用。

`BlockingQueue`方法有四种形式，有不同的处理操作的方法，不能立即满足，但可能在将来的某个时候得到满足：一个抛出异常，第二个返回一个特殊值（ `null`或者`false`，取决于操作），第三个无限期地阻塞当前线程，直到操作成功，第四个在放弃之前只有给定的最大时间限制。这些方法总结在下表中：

|      | 引发异常                               | 特殊值                           | 阻塞                | 超时                                                         |
| ---- | -------------------------------------- | -------------------------------- | ------------------- | ------------------------------------------------------------ |
| 插入 | [`add(e)`](#add(E))                    | [`offer(e)`](#offer(E))          | [`put(e)`](#put(E)) | [`offer(e, time, unit)`](#offer(E,long,java.util.concurrent.TimeUnit)) |
| 去掉 | [`remove()`](../Queue.html#remove())   | [`poll()`](../Queue.html#poll()) | [`take()`](#take()) | [`poll(time, unit)`](#poll(long,java.util.concurrent.TimeUnit)) |
| 检查 | [`element()`](../Queue.html#element()) | [`peek()`](../Queue.html#peek()) | 不适用              | 不适用                                                       |

`BlockingQueue`不接受`null`元素。实现抛出`NullPointerException`尝试`add`，`put`或者`offer`a `null`。A `null`用作标记值以指示`poll`操作失败 。

`BlockingQueue`可能是有限的容量。在任何给定的时间，它可能具有`remainingCapacity`超过其没有额外元素可以`put`没有阻塞。`BlockingQueue`没有任何内在容量限制的总是报告剩余容量`Integer.MAX_VALUE`。

`BlockingQueue`实现主要用于生产者 - 消费者队列，但另外支持Collection接口。因此，例如，可以使用从队列中删除任意元素 `remove(x)`。然而，这些操作通常 *不是*非常有效地执行，并且仅用于偶尔使用，例如当排队的消息被取消时。

`BlockingQueue`实现是线程安全的。所有排队方法都使用内部锁或其他形式的并发控制以原子方式实现其效果。然而， *批量*收集操作`addAll`， `containsAll`，`retainAll`并且`removeAll`都 *没有*必要自动除非在实现中另有规定执行。因此，例如，`addAll(c)`在仅添加一些元素之后失败（抛出异常）是可能的 `c`。

本质`BlockingQueue`上*不*支持任何类型的“关闭”或“关闭”操作，以指示不再添加任何项目。这些功能的需求和使用倾向于依赖于实现。例如，一种常见的策略是生产者插入特殊 *的流末端*或*毒物*对象，这些对象在被消费者采用时会相应地进行解释。

用法示例，基于典型的生产者 - 消费者场景。请注意，a `BlockingQueue`可以安全地与多个生产者和多个消费者一起使用。

```
 class Producer implements Runnable {
   private final BlockingQueue queue;
   Producer(BlockingQueue q) { queue = q; }
   public void run() {
     try {
       while (true) { queue.put(produce()); }
     } catch (InterruptedException ex) { ... handle ...}
   }
   Object produce() { ... }
 }

 class Consumer implements Runnable {
   private final BlockingQueue queue;
   Consumer(BlockingQueue q) { queue = q; }
   public void run() {
     try {
       while (true) { consume(queue.take()); }
     } catch (InterruptedException ex) { ... handle ...}
   }
   void consume(Object x) { ... }
 }

 class Setup {
   void main() {
     BlockingQueue q = new SomeQueueImplementation();
     Producer p = new Producer(q);
     Consumer c1 = new Consumer(q);
     Consumer c2 = new Consumer(q);
     new Thread(p).start();
     new Thread(c1).start();
     new Thread(c2).start();
   }
 }
```

内存一致性效果：与其他并发集合一样，在将对象置于 从另一个线程中访问或删除该元素之后的 `BlockingQueue` 发生之前的操作之前`BlockingQueue`，线程中的操作。