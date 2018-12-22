## CyclicBarrier

```java
public class CyclicBarrier
```

### java doc

一种同步辅助工具，允许一组线程全部等待彼此到达公共障碍点。 CyclicBarriers在涉及固定大小的线程方的程序中很有用，这些线程必须偶尔等待彼此。 称为屏障循环，因为它可以在释放等待线程后重新使用。
CyclicBarrier支持可选的Runnable命令，该命令在每个障碍点运行一次，在聚会中的最后一个线程到达之后，但在释放任何线程之前。 在任何一方继续之前，此屏障操作对于更新共享状态非常有用。

示例用法：以下是在并行分解设计中使用屏障的示例：

```java
 class Solver {
   final int N;
   final float[][] data;
   final CyclicBarrier barrier;

   class Worker implements Runnable {
     int myRow;
     Worker(int row) { myRow = row; }
     public void run() {
       while (!done()) {
         processRow(myRow);

         try {
           barrier.await();
         } catch (InterruptedException ex) {
           return;
         } catch (BrokenBarrierException ex) {
           return;
         }
       }
     }
   }

   public Solver(float[][] matrix) {
     data = matrix;
     N = matrix.length;
     Runnable barrierAction = () -> mergeRows(...);
     barrier = new CyclicBarrier(N, barrierAction);

     List<Thread> threads = new ArrayList<>(N);
     for (int i = 0; i < N; i++) {
       Thread thread = new Thread(new Worker(i));
       threads.add(thread);
       thread.start();
     }

     // wait until done
     for (Thread thread : threads)
       thread.join();
   }
 }
```

这里，每个工作线程处理一行矩阵，然后在屏障处等待，直到所有行都被处理完毕。 处理完所有行后，将执行提供的Runnable屏障操作并合并行。 如果合并确定已找到解决方案，则done（）将返回true并且每个工作程序将终止。
如果屏障操作不依赖于在执行时被暂停的各方，那么该方中的任何线程都可以在释放时执行该操作。 为了实现这一点，每次调用await（）都会返回该线程在屏障处的到达索引。 然后，您可以选择应执行屏障操作的线程，例如：

```java
 if (barrier.await() == 0) {
   // log the completion of this iteration
 }
```

CyclicBarrier使用全部或全部破坏模型进行失败的同步尝试：如果线程因中断，失败或超时而过早地离开障碍点，则在该障碍点等待的所有其他线程也将通过BrokenBarrierException（或InterruptedException）异常地离开 如果他们也在大约同一时间被打断了）。

内存一致性影响：调用await（）之前的线程中的操作发生在作为屏障操作一部分的操作之前，而操作又发生在从其他线程中相应的await（）成功返回之后的操作之前。

### 分析

```java
/**
 * 屏障的每次使用都表示为生成实例。
  * 每当屏障被触发或重置时，Generation都会改变。 
  * 可能有许多Generation与使用屏障的线程相关联 -
  * 由于锁定可能被分配给等待线程的非确定性方式 -
  * 但是其中只有一个可以一次处于活动状态（应用计数的线程）并且所有
  * 休息要么被打破要么跳闸。 
  * 如果有中断但没有后续重置，则不需要有活动的生成。
 **/
private static class Generation {
    Generation() {}                 // prevent access constructor creation
    boolean broken;                 // initially false
}
```

```java
/** 阻止其他线程执行的锁 */
private final ReentrantLock lock = new ReentrantLock();
/** 在达到跳闸条件时候等待*/
private final Condition trip = lock.newCondition();
/** 允许活动的线程数量 */
private final int parties;
/**跳闸时运行的命令 */
private final Runnable barrierCommand;
/** 每次栅栏循环锁代表的年代 */
private Generation generation = new Generation();
//允许活动线程的数量。 每一代都要从派对上计算到0。 它被重置为每个新一代或破坏时的各方。
private int count;
```

### dowait

```java
/**
 * Main barrier code, covering the various policies.
 */
private int dowait(boolean timed, long nanos)
    throws InterruptedException, BrokenBarrierException,
           TimeoutException {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        final Generation g = generation;

        if (g.broken)
            throw new BrokenBarrierException();

        if (Thread.interrupted()) {
            breakBarrier();
            throw new InterruptedException();
        }

        int index = --count;
        if (index == 0) {  // tripped
            boolean ranAction = false;
            try {
                final Runnable command = barrierCommand;
                if (command != null)
                    command.run();
                ranAction = true;
                nextGeneration();
                return 0;
            } finally {
                if (!ranAction)
                    breakBarrier();
            }
        }

        // loop until tripped, broken, interrupted, or timed out
        for (;;) {
            try {
                if (!timed)
                    trip.await();
                else if (nanos > 0L)
                    nanos = trip.awaitNanos(nanos);
            } catch (InterruptedException ie) {
                if (g == generation && ! g.broken) {
                    breakBarrier();
                    throw ie;
                } else {
                    // We're about to finish waiting even if we had not
                    // been interrupted, so this interrupt is deemed to
                    // "belong" to subsequent execution.
                    Thread.currentThread().interrupt();
                }
            }

            if (g.broken)
                throw new BrokenBarrierException();

            if (g != generation)
                return index;

            if (timed && nanos <= 0L) {
                breakBarrier();
                throw new TimeoutException();
            }
        }
    } finally {
        lock.unlock();
    }
}
```

doWait方法比较长，但是比较好理解，首先通过ReentrantLock获取当前线程的锁，然后进入到了try/catch代码块中。在代码块里面获取次循环相关的Generation对象g，如果g被break就抛出`BrokenBarrierException`。然后判断当前线程是否被中断过，是的话调用`breakBarrier`并抛出异常。

```java
private void breakBarrier() {
    generation.broken = true;
    count = parties;
    trip.signalAll();
}
```

`breakBarrier`将generation的broken字段设置为True,将代表可获取的活动线程数重置为parties,最后唤醒左右调用await方法并且被锁住await的其他线程。

如果没有发生异常，则查看剩余可获取线程数是否为0，为0的话就运行barrierCommand，也就是达到了允许活动的线程数量后执行由构造函数传递进来的Runnable。如果运行Runnable的时候发生了异常，则通过runAction字段。在fianally块里，调用`breakBarrier`通知其他线程并抛出异常。如果没有发生异常执行完毕后会调用`nextGeneration`唤醒其他线程并且，将count恢复为parties，并且创建一个新的`Generation`对象，代表栅栏循环进入了下一个循环。最后返回。

```java
/**
  * 更新障碍之旅状态并唤醒所有人。 仅在握住锁定时调用。
  */
  private void nextGeneration() {
  // 信号完成上一代
  trip.signalAll();
  // 建立下一代
  count = parties;
  generation = new Generation();
  }
```



如果当前的线程数量没有满足要求的话,则会进入到一个自旋过程中。首先通过形参`timed`判断是否有期限要求，没有的话直接通过`trip.await` 让当前的线程等待。如果有就调用`trip.awaitNanos(nanos)`让当前线程等待相应的时间。

在catch代码块里，会处理当前线程被中断的情况，如果generation还是当前的年代，并且generation没有被break，就调用`breakBarrier`将栅栏循环break，然后抛出`InterruptedException`交由调用者处理。如果generation不是当前的年代，或者generation被break。那么就交由后面的代码处理。

最后会判断generation是否被break,和generation是否属于当前年代或者等待是否超时。如果有以上情况则抛出异常或者返回。最后在finally中休息解锁当前的线程。

### reset

```java
/**
 *将屏障重置为其初始状态。 如果任何一方当前正在屏障等待，
 * 他们将返回BrokenBarrierException。 注意，由于其他原因发生破损后的重置可能很复杂;
  *  线程需要以其他方式重新同步，并选择一个来执行重置。 
  *  相反，可能优选地为随后的使用创建新的屏障。
 **/
public void reset() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        breakBarrier();   // break the current generation
        nextGeneration(); // start a new generation
    } finally {
        lock.unlock();
    }
}
```

`reset`用于将当前栅栏循环重置，但是会让等待的线程抛出异常，所以应该谨慎使用。

