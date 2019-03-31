## Thread

```java
class Thread implements Runnable
```

### java doc

线程是程序中执行的线程。 Java虚拟机允许应用程序同时运行多个执行线程。
每个线程都有优先权。 具有较高优先级的线程优先于具有较低优先级的线程执行。 每个线程可能也可能不会被标记为守护进程。 当在某个线程中运行的代码创建一个新的Thread对象时，新线程的优先级最初设置为等于创建线程的优先级，并且当且仅当创建线程是守护进程时才是守护进程线程。

当Java虚拟机启动时，通常会有一个非守护程序线程（通常调用某个指定类的名为main的方法）。 Java虚拟机继续执行线程，直到发生以下任一情况：

- 已调用类Runtime的exit方法，并且安全管理器已允许执行退出操作。

- 所有不是守护程序线程的线程都已经死亡，无论是通过从run方法调用返回还是抛出一个超出run方法传播的异常。

有两种方法可以创建新的执行线程。 一种是将类声明为Thread的子类。 该子类应该重写Thread类的run方法。 然后可以分配和启动子类的实例。 例如，计算大于规定值的素数的线程可以写成如下：

```java
    class PrimeThread extends Thread {
         long minPrime;
         PrimeThread(long minPrime) {
             this.minPrime = minPrime;
         }

         public void run() {
             // compute primes larger than minPrime
              . . .
         }
     }
```

然后，以下代码将创建一个线程并开始运行：

```java
   PrimeThread p = new PrimeThread(143);
     p.start();
 
```

创建线程的另一种方法是声明一个实现Runnable接口的类。 该类然后实现run方法。 然后可以分配类的实例，在创建Thread时作为参数传递，然后启动。 此其他样式中的相同示例如下所示：

```java
     class PrimeRun implements Runnable {
         long minPrime;
         PrimeRun(long minPrime) {
             this.minPrime = minPrime;
         }

         public void run() {
             // compute primes larger than minPrime
              . . .
         }
     }
 
```

然后，以下代码将创建一个线程并开始运行：

```java
     PrimeRun p = new PrimeRun(143);
     new Thread(p).start();
```

每个线程都有一个用于识别目的的名称。 多个线程可能具有相同的名称。 如果在创建线程时未指定名称，则会为其生成新名称。

除非另有说明，否则将null参数传递给此类中的构造函数或方法将导致抛出NullPointerException。

### 字段

```java
 private volatile String name;
 private int priority;

 /* 该线程是否是守护程序线程。 */
 private boolean daemon = false;

 /* 保留供JVM独占使用的字段*/
 private boolean stillborn = false;
 
 private long eetop;

 /* 那个Runnable会被运行. */
 private Runnable target;

 /* 这个线程的组 */
 private ThreadGroup group;

 /*此线程的上下文ClassLoader*/
 private ClassLoader contextClassLoader;

 /* 此线程的继承AccessControlContext*/
 private AccessControlContext inheritedAccessControlContext;

 /* 用于自动编写匿名线程。 */
 private static int threadInitNumber;

 /* 与此线程有关的ThreadLocal值。
  此映射由ThreadLocal类维护。*/
 ThreadLocal.ThreadLocalMap threadLocals = null;

 /*
  * 与此线程相关的InheritableThreadLocal值。 此映射由InheritableThreadLocal类维护。
  */
 ThreadLocal.ThreadLocalMap inheritableThreadLocals = null;

 /*
  * 此线程请求的堆栈大小，如果创建者未指定堆栈大小，则为0。
   * 虚拟机可以用这个数字做任何喜欢的事情;一些虚拟机会忽略它。
  */
 private final long stackSize;

 /*
  * 本机线程终止后持久存在的JVM私有状态。
  */
 private long nativeParkEventPointer;

 /*
  * Thread ID
  */
 private final long tid;

 /* 用于生成线程ID*/
 private static long threadSeqNumber;

 /*
  * 工具的Java线程状态，默认表示线程'尚未启动'
  */
 private volatile int threadStatus;

 /**
  * 提供给当前调用java.util.concurrent.locks.LockSupport.park的参数
  *设置方（私有）java.util.concurrent.locks.LockSupport.setBlocker使
  * 用java.util.concurrent.locks.LockSupport.getBlocker访问
  */
 volatile Object parkBlocker;

 /* 在可中断的I / O操作中阻塞此线程的对象（如果有）。
  *设置此线程的中断状态后，应调用阻塞程序的中断方法。
  */
 private volatile Interruptible blocker;
 private final Object blockerLock = new Object();



 /**
  * 线程可以拥有的最低优先级。
  */
 public static final int MIN_PRIORITY = 1;

/**
  * 分配给线程的默认优先级。
  */
 public static final int NORM_PRIORITY = 5;

 /**
  * 线程可以拥有的最大优先级。
  */
 public static final int MAX_PRIORITY = 10;
//保存栈踪迹的数组
  private static final StackTraceElement[] EMPTY_STACK_TRACE
        = new StackTraceElement[0];
```

### 方法

```java
private static native void registerNatives();
static {
    registerNatives();
}
```

Thread类的第一个方法是static native方法，这个方法也存在于Object类中。

```java
//向调度程序提示当前线程是否愿意让出其当前使用的处理器。 调度程序可以忽略此提示。
//Yield是一种启发式尝试，用于改善线程之间的相对进展，否则会过度利用CPU。 它的使用应与详细的分析和基准测试相结合，以确保它实际上具有所需的效果。

//使用此方法很少合适。 它可能对调试或测试目的很有用，它可能有助于重现因竞争条件而产生的错误。 在设计并发控制结构（例如java.util.concurrent.locks包中的结构）时，它也可能很有用。
public static native void yield();
```

```java
//导致当前正在执行的线程休眠（暂时停止执行）指定的毫秒数，具体取决于系统计时器和调度程序的精度和准确性。 该线程不会失去任何监视器的所有权。
public static native void sleep(long millis) throws InterruptedException;
```

`sleep()`方法可以用于与线程间通信的wait()方法比较。sleep()方法不会退出监视器，wait()会退出监视器。

```java
//表示调用者暂时无法进展，直到其他活动发生一个或多个操作为止。 通过在自旋等待循环结构的每次迭代中调用此方法，调用线程向运行时指示它正在忙等待。 运行时可以采取措施来提高调用自旋等待循环结构的性能。
//API注意：
//作为一个例子，考虑一个类中的方法，该方法在循环中旋转，直到在该方法之外设置一些标志。 对onSpinWait方法的调用应该放在旋转循环中。：
—————————————————————————————————————————————————————————————————————
     class EventHandler {
         volatile boolean eventNotificationNotReceived;
         void waitForEventAndHandleIt() {
             while ( eventNotificationNotReceived ) {
                 java.lang.Thread.onSpinWait();
             }
             readAndProcessEvent();
         }

         void readAndProcessEvent() {
             // Read event from some source and process it
              . . .
         }
     }
—————————————————————————————————————————————————————————————————————     
//即使根本没有调用onSpinWait方法，上面的代码仍然是正确的。 然而，在一些体系结构上，Java虚拟机可以以更有益的方式发布处理器指令以解决这样的代码模式。
public static void onSpinWait() {}
```

onSpinWait()是在java9后添加的方法，用于帮助虚拟机提高性能，但它本身就是一个空实现方法。

```java
@Override
protected Object clone() throws CloneNotSupportedException {
    throw new CloneNotSupportedException();
}
```

Thread不支持克隆。

```java
public synchronized void start() {
    /**
     * This method is not invoked for the main method thread or "system"
     * group threads created/set up by the VM. Any new functionality added
     * to this method in the future may have to also be added to the VM.
     *
     * A zero status value corresponds to state "NEW".
     */
    if (threadStatus != 0)
        throw new IllegalThreadStateException();

    /* Notify the group that this thread is about to be started
     * so that it can be added to the group's list of threads
     * and the group's unstarted count can be decremented. */
    group.add(this);

    boolean started = false;
    try {
        start0();
        started = true;
    } finally {
        try {
            if (!started) {
                group.threadStartFailed(this);
            }
        } catch (Throwable ignore) {
            /* do nothing. If start0 threw a Throwable then
              it will be passed up the call stack */
        }
    }
}
```

启动线程的方法是一个同步方法。同一个线程不能被启动两次，然后调用native方法启动线程。

```java
private void exit() {
    if (threadLocals != null && TerminatingThreadLocal.REGISTRY.isPresent()) {
        TerminatingThreadLocal.threadTerminated();
    }
    if (group != null) {
        group.threadTerminated(this);
        group = null;
    }
    /* Aggressively null out all reference fields: see bug 4006245 */
    target = null;
    /* Speed the release of some of these resources */
    threadLocals = null;
    inheritableThreadLocals = null;
    inheritedAccessControlContext = null;
    blocker = null;
    uncaughtExceptionHandler = null;
}
```

用于线程结束后帮助GC

```java
public void interrupt() {
    if (this != Thread.currentThread()) {
        checkAccess();

        // thread may be blocked in an I/O operation
        synchronized (blockerLock) {
            Interruptible b = blocker;
            if (b != null) {
                interrupt0();  // set interrupt status
                b.interrupt(this);
                return;
            }
        }
    }

    //设置中断状态
    interrupt0();
}
```



interrupt()只是设置中断状态并没有中断线程的执行。



```java
public boolean isInterrupted() {
    return isInterrupted(false);
}
```

而isInterrupted()则是测试该线程是否已被中断，并且清除楚线程的中断状态。也就是说如果连续两次调用次方法，第二次会返回false。

```java
public static int enumerate(Thread tarray[]) {
    return currentThread().getThreadGroup().enumerate(tarray);
}
```

enumerate将线程组中的线程复制到一个数组中。

#### join()原理

```java
//此线程最多等待毫秒毫秒。 超时为0意味着永远等待。
//此实现使用this.wait调用this.isAlive的循环。 当一个线程终止时，将调用this.notifyAll方法。 建议应用程序不要在Thread实例上使用wait，notify或notifyAll。
public final synchronized void join(long millis)
throws InterruptedException {
    long base = System.currentTimeMillis();
    long now = 0;

    if (millis < 0) {
        throw new IllegalArgumentException("timeout value is negative");
    }

    if (millis == 0) {
        while (isAlive()) {
            wait(0);
        }
    } else {
        while (isAlive()) {
            long delay = millis - now;
            if (delay <= 0) {
                break;
            }
            wait(delay);
            now = System.currentTimeMillis() - base;
        }
    }
}
```

join用于让在上下文中的线程等待调用join子线程执行完毕后再继续。原理就是在上下文线程环境中调用子线程wait()。

```java

    public static void main(String[] args) {
        TestThread testThread = new TestThread(Thread.currentThread(), new test(0));

        testThread.joins(0);


        System.out.println("主线程结束");

    }

    static class test {
        long time = 0;

        public test(int time) {
            this.time = time;
        }

        public synchronized void say() {
            for (int i = 0; i < 100000; i++) {
                System.out.println("1");
            }
        }

    }

    static class TestThread extends Thread {
        Thread main;

        public TestThread(Thread main, ThreadWait.test test) {
            this.main = main;
            this.test = test;
            this.start();
        }

        public synchronized void joins(long time) {
            try {
                if (time == 0)
                    this.wait();
                else
                    this.wait(time);
                System.out.println(Thread.currentThread().getName());

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public TestThread(test test) {
            this.test = test;
            this.start();
        }

        test test;

        @Override
        public void run() {
            test.say();
            System.out.println(main.getState());
            System.out.println("子线程结束");
        }
    }
```

上面这段程序运行结果是，先输出1000个1然后是“WAITING、子线程结束、main、主线程结束”

joins()方法达到了和Thread.join()一样的效果。

WAITING是主线程的JVM状态，main也是主线程的名字。joins()方法是一个同步方法，当在主线程的上下文中调用this.wait()的时候,this处在主线程上下文,导致了主线程放弃监视器进入WAITING状态。在子线程执行结束的时候，调用this.notifyAll。this所在的上下文线程也是主线程。所以主线程又被唤醒，继续执行。所以官方文档中不建议在Thread实例上使用wait。



```java
当且仅当当前线程在指定对象上保存监视器锁时返回true。
此方法旨在允许程序断言当前线程已经拥有指定的锁：
	assert Thread.holdsLock(obj);
___________________________________________
public static native boolean holdsLock(Object obj);
```



getStackTrace() 和getAllStackTrace()用于获取线程相关的栈踪迹。

### 函数式接口

```java
@FunctionalInterface
public interface UncaughtExceptionHandler {
    /**
     * Method invoked when the given thread terminates due to the
     * given uncaught exception.
     * <p>Any exception thrown by this method will be ignored by the
     * Java Virtual Machine.
     * @param t the thread
     * @param e the exception
     */
    void uncaughtException(Thread t, Throwable e);
}
```

可以通过此接口指定当线程发生未捕获异常的时候的的处理办法。例如：

```java
thread.setUncaughtExceptionHandler((t, e) -> System.out.println(t.getName() + "发生异常" + e.getLocalizedMessage()));
```





### Thread类过期方法说明

stop()、suspend()、resume()方法在1.2的时候，就已经不推荐被使用。官方文档的解释：

### Why is `Thread.stop` deprecated?

因为它本质上是不安全的。 停止线程会导致它解锁已锁定的所有监视器。 （当ThreadDeath异常向上传播时，监视器将被解锁。）如果先前受这些监视器保护的任何对象处于不一致状态，则其他线程现在可以以不一致的状态查看这些对象。 据说这些物体已被损坏。 当线程对受损对象进行操作时，可能会导致任意行为。 这种行为可能很微妙并且难以检测，或者可能是明显的。 与其他未经检查的异常不同，ThreadDeath会以静默方式杀死线程; 因此，用户没有警告他的程序可能被破坏。 腐败可以在实际损害发生后的任何时间显现，甚至在未来几小时或几天。

下面是推荐的代替方法：

stop的大多数用法应该由代码修改，该代码只是修改某个变量以指示目标线程应该停止运行。 目标线程应定期检查此变量，如果变量指示它将停止运行，则以有序的方式从其run方法返回。 为了确保快速通信停止请求，变量必须是易失性的（或者必须同步对变量的访问）。

例如，假设您的applet包含以下start，stop和run方法：

```java
    private Thread blinker;

    public void start() {
        blinker = new Thread(this);
        blinker.start();
    }

    public void stop() {
        blinker.stop();  // UNSAFE!
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e){
            }
            repaint();
        }
    }
```

您可以通过将applet的stop和run方法替换为以下内容来避免使用Thread.stop：

```java
    private volatile Thread blinker;

    public void stop() {
        blinker = null;
    }

    public void run() {
        Thread thisThread = Thread.currentThread();
        while (blinker == thisThread) {
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e){
            }
            repaint();
        }
    }
```

#### 我不能只捕获ThreadDeath异常并修复损坏的对象吗？

理论上，也许，但它会使编写正确的多线程代码的任务大大复杂化。 由于两个原因，这项任务几乎无法克服：

- 一个线程几乎可以在任何地方抛出一个ThreadDeath异常。 考虑到这一点，必须非常详细地研究所有同步的方法和块。
- 从第一个（在catch或finally子句中）清理时，线程可以抛出第二个ThreadDeath异常。 清理必须重复，直到成功为止。 这一点的代码非常复杂。

总而言之，这是不切实际的。

#### 如何停止等待很长时间的线程（例如，输入）？

这就是Thread.interrupt方法的用途。 可以使用上面显示的相同的“0state based”的信号机制，但是状态更改（blinker = null，在前面的示例中）之后可以调用Thread.interrupt来中断等待：

```java
    public void stop() {
        Thread moribund = waiter;
        waiter = null;
        moribund.interrupt();
    }
```

要使此技术起作用，任何捕获中断异常并且不准备立即处理它的方法都必须重新设置异常。 我们说**reasserts**而不是**rethrows**，因为并不总是可以重新抛出异常。 如果没有声明捕获InterruptedException的方法抛出此（已检查）异常，那么它应该使用以下语法“reinterrupt itself”：

```java
Thread.currentThread().interrupt();
```

这可确保Thread尽快重新加入InterruptedException。

#### 如果一个线程没有响应Thread.interrupt怎么办？

在某些情况下，您可以使用特定于应用程序的技巧。 例如，如果线程正在等待已知套接字，则可以关闭套接字以使线程立即返回。 不幸的是，确实没有任何技术可以发挥作用。 应该注意的是，在等待线程不响应Thread.interrupt的所有情况下，它也不会响应Thread.stop。 此类情况包括故意拒绝服务攻击，以及thread.stop和thread.interrupt无法正常工作的I / O操作。

#### 为什么不推荐使用Thread.suspend和Thread.resume？

Thread.suspend本身就容易出现死锁。 如果目标线程在监视器上保持锁定，以在挂起时保护关键系统资源，则在恢复目标线程之前，任何线程都无法访问此资源。 如果恢复目标线程的线程在调用resume之前尝试锁定此监视器，则会导致死锁。 这种死锁通常表现为“frozen”。

#### 我应该使用什么代替Thread.suspend和Thread.resume？

与Thread.stop一样，谨慎的方法是让“目标线程”轮询一个变量，指示线程的所需状态（活动或挂起）。 当挂起所需的状态时，线程使用Object.wait等待。 当线程恢复时，使用Object.notify通知目标线程。

例如，假设您的applet包含以下mousePressed事件处理程序，该处理程序切换名为blinker的线程的状态：

```java
    private boolean threadSuspended;

    Public void mousePressed(MouseEvent e) {
        e.consume();

        if (threadSuspended)
            blinker.resume();
        else
            blinker.suspend();  // DEADLOCK-PRONE!

        threadSuspended = !threadSuspended;
    }
```

并将以下代码添加到“run loop”：

```java
                synchronized(this) {
                    while (threadSuspended)
                        wait();
                }
```

wait方法抛出InterruptedException，因此它必须在try ... catch子句中。 把它放在与睡眠相同的条款中是很好的。 检查应该跟随（而不是先于）睡眠，以便在线程“恢复”时立即重新绘制窗口。 生成的run方法如下：

```java
    public void run() {
        while (true) {
            try {
                Thread.sleep(interval);

                synchronized(this) {
                    while (threadSuspended)
                        wait();
                }
            } catch (InterruptedException e){
            }
            repaint();
        }
    }
```

请注意，mousePressed方法中的notify和run方法中的wait是在synchronized块内。 这是语言所必需的，并确保正确序列化wait和notify。 实际上，这消除了可能导致“挂起”线程错过通知并且无限期保持暂停的竞争条件。

虽然随着平台的成熟，Java中的同步成本正在下降，但它永远不会是免费的。可以使用一个简单的技巧来删除我们在“运行循环”的每次迭代中添加的同步。 添加的synchronized块被稍微复杂的代码块替换，只有在线程实际被挂起时才会进入同步块：

```java
                if (threadSuspended) {
                    synchronized(this) {
                        while (threadSuspended)
                            wait();
                    }
                }
```

在没有显式同步的情况下，必须使threadSuspended成为volatile，以确保suspend-request的快速通信。

生成的run方法是：

```java
    private volatile boolean threadSuspended;

    public void run() {
        while (true) {
            try {
                Thread.sleep(interval);

                if (threadSuspended) {
                    synchronized(this) {
                        while (threadSuspended)
                            wait();
                    }
                }
            } catch (InterruptedException e){
            }
            repaint();
        }
    }
```

#### 我可以结合使用这两种技术来生成可以安全“stopped”或“suspended”的线程吗？

是的，这是相当简单的。 一个微妙之处是目标线程可能已经在另一个线程试图阻止它时被挂起。 如果stop方法仅将状态变量（blinker）设置为null，则目标线程将保持挂起（在监视器上等待），而不是按原样正常退出。 如果重新启动applet，多个线程可能会同时在监视器上等待，从而导致行为不稳定。

要纠正这种情况，stop方法必须确保目标线程在挂起时立即恢复。 一旦目标线程恢复，它必须立即识别它已被停止，并正常退出。 以下是生成的run和stop方法的外观：

```java
    public void run() {
        Thread thisThread = Thread.currentThread();
        while (blinker == thisThread) {
            try {
                Thread.sleep(interval);

                synchronized(this) {
                    while (threadSuspended && blinker==thisThread)
                        wait();
                }
            } catch (InterruptedException e){
            }
            repaint();
        }
    }

    public synchronized void stop() {
        blinker = null;
        notify();
    }
```

如果stop方法调用Thread.interrupt，如上所述，它也不需要调用notify，但它仍然必须同步。 这可确保目标线程不会因竞争条件而错过中断。

