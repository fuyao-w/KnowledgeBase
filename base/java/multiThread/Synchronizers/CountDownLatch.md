## CountDownLatch

```java
public class CountDownLatch
```

### java doc

允许一个或多个线程等待直到在其他线程中执行的一组操作完成的同步辅助。
使用给定计数初始化CountDownLatch。由于countDown（）方法的调用，await方法阻塞直到当前计数达到零，之后释放所有等待的线程，并且任何后续的await调用立即返回。这是一次性现象 - 计数无法重置。如果您需要重置计数的版本，请考虑使用CyclicBarrier。

CountDownLatch是一种多功能同步工具，可用于多种用途。初始化为count的CountDownLatch用作简单的开/关锁存器或门：所有线程调用等待在门处等待，直到由调用countDown（）的线程打开它。初始化为N的CountDownLatch可用于使一个线程等待，直到N个线程完成某个操作，或者某个操作已完成N次。

CountDownLatch的一个有用属性是它不要求调用countDown的线程在继续之前等待计数达到零，它只是阻止任何线程继续等待直到所有线程都可以通过。

示例用法：这是一对类，其中一组工作线程使用两个倒计时锁存器：

- 第一个是启动信号，阻止任何工人继续工作，直到驱动程序准备好继续进行;
- 第二个是完成信号，允许驾驶员等到所有工人完成。

```java
 class Driver { // ...
   void main() throws InterruptedException {
     CountDownLatch startSignal = new CountDownLatch(1);
     CountDownLatch doneSignal = new CountDownLatch(N);

     for (int i = 0; i < N; ++i) // create and start threads
       new Thread(new Worker(startSignal, doneSignal)).start();

     doSomethingElse();            // don't let run yet
     startSignal.countDown();      // let all threads proceed
     doSomethingElse();
     doneSignal.await();           // wait for all to finish
   }
 }

 class Worker implements Runnable {
   private final CountDownLatch startSignal;
   private final CountDownLatch doneSignal;
   Worker(CountDownLatch startSignal, CountDownLatch doneSignal) {
     this.startSignal = startSignal;
     this.doneSignal = doneSignal;
   }
   public void run() {
     try {
       startSignal.await();
       doWork();
       doneSignal.countDown();
     } catch (InterruptedException ex) {} // return;
   }

   void doWork() { ... }
 }
```

另一个典型的用法是将问题分成N个部分，用执行该部分的Runnable描述每个部分并对锁存器进行倒计时，并将所有Runnables排队到Executor。 当所有子部件都完成后，协调线程将能够通过等待。 （当线程必须以这种方式重复倒计时时，而是使用CyclicBarrier。）

```java
 class Driver2 { // ...
   void main() throws InterruptedException {
     CountDownLatch doneSignal = new CountDownLatch(N);
     Executor e = ...

     for (int i = 0; i < N; ++i) // create and start threads
       e.execute(new WorkerRunnable(doneSignal, i));

     doneSignal.await();           // wait for all to finish
   }
 }

 class WorkerRunnable implements Runnable {
   private final CountDownLatch doneSignal;
   private final int i;
   WorkerRunnable(CountDownLatch doneSignal, int i) {
     this.doneSignal = doneSignal;
     this.i = i;
   }
   public void run() {
     try {
       doWork(i);
       doneSignal.countDown();
     } catch (InterruptedException ex) {} // return;
   }

   void doWork() { ... }
 }
```

内存一致性影响：在计数达到零之前，调用countDown（）之前的线程中的操作发生在从另一个线程中的相应await（）成功返回之后的操作之前。

### 分析

`CountDownLatch`类实现比较简单，关键点在钩子方法上。

### `await()`

```java
public void await() throws InterruptedException {
    sync.acquireSharedInterruptibly(1);
}
```

这个`await`与Condition类中的`await`作用类似，阻塞调用线程：

```java
public final void acquireSharedInterruptibly(int arg)
        throws InterruptedException {
    if (Thread.interrupted())
        throw new InterruptedException();
    if (tryAcquireShared(arg) < 0)
        doAcquireSharedInterruptibly(arg);
}
```

```java
protected int tryAcquireShared(int acquires) {
    return (getState() == 0) ? 1 : -1;
}
```

在`tryAcquireShared`里的实现与正常情况相反，只要state不为0，就会返回-1。将当前的线程阻塞。直到其他线程将state降为0。

### `countDown`:

```java
public void countDown() {
    sync.releaseShared(1);
}
```

```java
protected boolean tryReleaseShared(int releases) {
    // Decrement count; signal when transition to zero
    for (;;) {
        int c = getState();
        if (c == 0)
            return false;
        int nextc = c - 1;
        if (compareAndSetState(c, nextc))
            return nextc == 0;
    }
}
```

每次调用`countDown`会将state减去1。