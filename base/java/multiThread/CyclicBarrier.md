## CyclicBarrier

```java
public class CyclicBarrier
```

### java doc

一种同步辅助工具，允许一组线程全部等待彼此到达公共障碍点。 CyclicBarriers在涉及固定大小的线程方的程序中很有用，这些线程必须偶尔等待彼此。 屏障称为循环，因为它可以在释放等待线程后重新使用。
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

