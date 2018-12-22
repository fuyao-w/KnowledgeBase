## Exchanger 

线程可以在对中交换元素的同步点。每个线程在exchange方法的入口处呈现一些对象 ，与伙伴线程匹配，并在返回时接收其伙伴的对象。交换器可以被视为a的双向形式SynchronousQueue。交换器可能在遗传算法和管道设计等应用中很有用。

**示例用法：** 下面是一个类的亮点，它使用一个`Exchanger` 在线程之间交换缓冲区，以便填充缓冲区的线程在需要时获得一个刚刚清空的线程，将填充的线程切换到清空缓冲区的线程。

```java
 class FillAndEmpty {
   Exchanger<DataBuffer> exchanger = new Exchanger<>();
   DataBuffer initialEmptyBuffer = ... a made-up type
   DataBuffer initialFullBuffer = ...

   class FillingLoop implements Runnable {
     public void run() {
       DataBuffer currentBuffer = initialEmptyBuffer;
       try {
         while (currentBuffer != null) {
           addToBuffer(currentBuffer);
           if (currentBuffer.isFull())
             currentBuffer = exchanger.exchange(currentBuffer);
         }
       } catch (InterruptedException ex) { ... handle ... }
     }
   }

   class EmptyingLoop implements Runnable {
     public void run() {
       DataBuffer currentBuffer = initialFullBuffer;
       try {
         while (currentBuffer != null) {
           takeFromBuffer(currentBuffer);
           if (currentBuffer.isEmpty())
             currentBuffer = exchanger.exchange(currentBuffer);
         }
       } catch (InterruptedException ex) { ... handle ...}
     }
   }

   void start() {
     new Thread(new FillingLoop()).start();
     new Thread(new EmptyingLoop()).start();
   }
 }
```

内存一致性效果：对于通过a成功交换对象的每对线程，在每个线程 [*发生之前的*](package-summary.html#MemoryVisibility)`Exchanger`操作发生在从 另一个线程中的相应线程返回之后的操作之前。`exchange()``exchange()`