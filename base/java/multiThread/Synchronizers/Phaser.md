## Phaser

[讲解]: https://segmentfault.com/a/1190000015979879#articleHeader9



### java doc

可重用的同步屏障，功能类似于CyclicBarrier和CountDownLatch，但支持更灵活的使用。

**Registration**。 与其他障碍的情况不同，登记在phaser上同步的各方数量可能会随时间而变化。 任务可以随时注册（使用方法`register（）`，`bulkRegister（int）`或构建初始数量的构造者的形式），并且可选地在任何到达时注销（使用arrivalAndDeregister（）`）。 与大多数基本同步结构一样，注册和注销仅影响内部计数; 他们没有建立任何进一步的内部簿记，因此任务无法查询他们是否已注册。 （但是，你可以通过继承这个类来引入这样的簿记。）

**Synchronization**。 像CyclicBarrier一样，可以反复等待Phaser。 方法arrivalAndAwaitAdvance（）具有类似于`CyclicBarrier.await`的效果。 每一代phaser都有一个相关的相位数。 阶段编号从零开始，并在所有各方到达phaser时前进，在到达Integer.MAX_VALUE后回绕到零。 使用阶段编号可以在到达phaser时以及在等待其他人时通过可由任何注册方调用的两种方法独立控制操作：

- **Arrival**。方法`arrive（）`和`arrivalAndDeregister（）`记录到达。这些方法不会阻塞，但返回相关的到达阶段号;也就是说，到达所应用的phaser的相数。当给定阶段的最后一方到达时，执行可选动作并且阶段前进。这些动作由触发相位超前的一方执行，并通过覆盖方法onAdvance（int，int）来安排，该方法也控制终止。覆盖此方法与为CyclicBarrier提供屏障操作类似但更灵活。
- **Waiting**。方法awaitAdvance（int）需要一个指示到达阶段号的参数，并在phaser前进到（或已经处于）不同阶段时返回。与使用CyclicBarrier的类似结构不同，方法awaitAdvance继续等待，即使等待的线程被中断。可以使用可中断和超时版本，但在任务中断或超时等待时遇到的异常不会更改phaser的状态。如有必要，您可以在调用forceTermination之后，在这些异常的处理程序中执行任何相关的恢复。在ForkJoinPool中执行的任务也可以使用Phasers。如果池的parallelismLevel可以容纳最大数量的同时阻塞方，则可确保进度。

**Termination**。 phaser可以进入终止状态，可以使用方法isTerminated（）来检查。 在终止时，所有同步方法立即返回而不等待提前，如负返回值所示。 同样，终止时注册的尝试也没有效果。 当onAdvance的调用返回true时触发终止。 如果取消注册导致注册方的数量变为零，则默认实现返回true。 如下所示，当phaser控制具有固定迭代次数的动作时，通常很方便地覆盖该方法以在当前相位数达到阈值时引起终止。 方法forceTermination（）也可用于突然释放等待线程并允许它们终止。

**Tiering**。 可以对phaser进行分层（即，以树形结构构造）以减少争用。 然而，可以设置具有大量方的否则会经历大量同步争用成本的phaser，使得子组phaser共享共同的父级。 即使它产生更大的每操作开销，这也可以大大增加吞吐量。

在分层阶段的树中，自动管理子阶段与其父母的注册和注销。只要子phaser的注册方数量变为非零（如Phaser（Phaser，int）构造函数，register（）或bulkRegister（int）中所建立），子phaser就会向其父移植器注册。每当注册方的数量因调用arrivalAndDeregister（）而变为零时，子phaser就从其父phaser中注销。

**Monitoring**。虽然同步方法可以仅由注册方调用，但是phaser的当前状态可以由任何呼叫者监视。在任何给定时刻总共有getRegisteredParties（）派对，其中getArrivedParties（）已到达当前阶段（getPhase（））。当剩余的（getUnarrivedParties（））各方到达时，阶段会进展。这些方法返回的值可能反映瞬态，因此通常对同步控制无用。方法toString（）以便于非正式监视的形式返回这些状态查询的快照。

示例用法：

可以使用Phaser而不是CountDownLatch来控制为可变数量的聚会提供服务的一次性动作。 典型的习惯用法是将方法设置为首次注册，然后启动所有操作，然后取消注册，如下所示：

```java
 void runTasks(List<Runnable> tasks) {
   Phaser startingGate = new Phaser(1); // "1" to register self 
   // create and start threads
   for (Runnable task : tasks) {
     startingGate.register();
     new Thread(() -> {
       startingGate.arriveAndAwaitAdvance();
       task.run();
     }).start();
   }

   // deregister self to allow threads to proceed ，本阶段结束https://segmentfault.com/a/1190000015979879#articleHeader9
   startingGate.arriveAndDeregister();
 }
```

使一组线程重复执行给定迭代次数的操作的一种方法是覆盖onAdvance：

```java
 void startTasks(List<Runnable> tasks, int iterations) {
   Phaser phaser = new Phaser() {
     protected boolean onAdvance(int phase, int registeredParties) {
       return phase >= iterations - 1 || registeredParties == 0;
     }
   };
   phaser.register();
   for (Runnable task : tasks) {
     phaser.register();
     new Thread(() -> {
       do {
         task.run();
         phaser.arriveAndAwaitAdvance();
       } while (!phaser.isTerminated());
     }).start();
   }
   // allow threads to proceed; don't wait for them
   phaser.arriveAndDeregister();
 }
```

如果主要任务必须稍后等待终止，它可能会重新注册然后执行类似的循环：

```java
   // ...
   phaser.register();
   while (!phaser.isTerminated())
     phaser.arriveAndAwaitAdvance();
```

相关结构可用于等待上下文中的特定阶段编号，您确定该阶段永远不会包围Integer.MAX_VALUE。 例如：

```java
 void awaitPhase(Phaser phaser, int phase) {
   int p = phaser.register(); // assumes caller not already registered
   while (p < phase) {
     if (phaser.isTerminated())
       // ... deal with unexpected termination
     else
       p = phaser.arriveAndAwaitAdvance();
   }
   phaser.arriveAndDeregister();
 }
```

要使用阶段树创建一组n个任务，您可以使用以下形式的代码，假设一个Task类，其构造函数接受在构造时注册的Phaser。 在调用`build（new Task [n]，0，n，new Phaser（））`之后，可以启动这些任务，例如通过提交到池：

```java
 void build(Task[] tasks, int lo, int hi, Phaser ph) {
   if (hi - lo > TASKS_PER_PHASER) {
     for (int i = lo; i < hi; i += TASKS_PER_PHASER) {
       int j = Math.min(i + TASKS_PER_PHASER, hi);
       build(tasks, i, j, new Phaser(ph));
     }
   } else {
     for (int i = lo; i < hi; ++i)
       tasks[i] = new Task(ph);
       // assumes new Task(ph) performs ph.register()
   }
 }
```

TASKS_PER_PHASER的最佳值主要取决于预期的同步速率。 低至4的值可能适用于极小的每相任务机构（因此高速率），或者对于极大的任务机构而言可能高达数百。
实施说明：此实现将最大参与方数限制为65535.尝试注册其他参与方会导致IllegalStateException。 但是，您可以而且应该创建分层phaser以适应任意大量的参与者。

### 分析



```java
主状态表示，包含四个bit位域：
1.   unarrived  -- 尚未成为障碍的parties数量 (bits  0-15)
2.   parties    -- 等待的parties数            (bits 16-31)
3.   phase      -- 障碍的产生            (bits 32-62)
4.   terminated -- 如果障碍终止则设置            (bit  63  sign)

除了没有注册方的移相器之外，其他非法状态是零方和一个未得到支持的方（下面编码为EMPTY）。

为了有效地保持原子性，这些值被打包成单个（原子）长度。 良好的性能依赖于保持状态解码和编码简单，并保持竞赛窗口短。

除了初始注册子相位器（即，具有非空父母的子帧）之外，所有状态更新都通过CAS执行。 在这种（相对罕见的）情况下，我们使用内置同步锁定，同时首次注册其父节点。

允许subphaser的阶段滞后于其祖先的阶段，直到实际访问它为止 - 请参阅方法reconcileState。

private volatile long state;
```

### 分析

构造方法

```java
public Phaser(Phaser parent, int parties) {
    if (parties >>> PARTIES_SHIFT != 0)
        throw new IllegalArgumentException("Illegal number of parties");
    int phase = 0;
    this.parent = parent;
    if (parent != null) {
        final Phaser root = parent.root;
        this.root = root;
        this.evenQ = root.evenQ;
        this.oddQ = root.oddQ;
        if (parties != 0)
            phase = parent.doRegister(1);
    }
    else {
        this.root = this;
        this.evenQ = new AtomicReference<QNode>();
        this.oddQ = new AtomicReference<QNode>();
    }
    this.state = (parties == 0) ? (long)EMPTY :
        ((long)phase << PHASE_SHIFT) |
        ((long)parties << PARTIES_SHIFT) |
        ((long)parties);
}
```

