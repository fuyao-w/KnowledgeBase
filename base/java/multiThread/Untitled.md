## 创建线程的几种方式

1. 继承[Thread类]( https://github.com/TransientWang/KnowledgeBase/blob/master/base/java/multiThread/Thread.md )，实现run()方法并调用Thread类的start()方法。

2. 实现[Runnable接口][https://github.com/TransientWang/KnowledgeBase/blob/master/base/java/multiThread/Runnable.md]，实现run()方法，将Runnable接口传递给Thread类，然后调用start()。

3. 通过`Executors`创建[线程池][]。但是线程池也要在ThreadFactory里面通过前两种方式创建线程。

真正创建线程行为的只有继承Thread类的方式，实现Runnable也只是在run()里完成新线程的行为。

## 线程的几种虚拟机状态

通过`Thread.State`枚举类可以查看线程有**六种虚拟机状态**。

线程状态。 线程可以处于以下状态之一：

- `NEW`
  尚未启动的线程处于此状态。
- `RUNNABLE`
  在Java虚拟机中执行的线程处于此状态。
- `BLOCKED`
  被阻塞等待监视器锁定的线程处于此状态。
- `WAITING`
  无限期等待另一个线程执行特定操作的线程处于此状态。
- `TIMED_WAITING`
  正在等待另一个线程执行最多指定等待时间的操作的线程处于此状态。
- `TERMINATED`
  已退出的线程处于此状态。

线程在给定时间点只能处于一种状态。 这些状态是虚拟机状态，不反映任何操作系统线程状态。

NEW和RUNNABLE状态很好理解，那BLOCKED状态实在什么情况下产生的呢？

按照官方文档的解释，阻塞状态是在一种行为向另一种行为变化的过程中产生的。另一种行为比较清楚，就是该线程进入监视器，一种行为肯定就是没有获取监视器是的一些动作。当没获取监视器的线程希望也能进入监视器，但被其他线程抢先占领时。 该线程只能等待，这种等待的行为就叫阻塞。

什么情况下线程获取不到监视器，只能阻塞呢？

1. 在多个线程执行同一个对象的同步方法的时候。哪些没有获取到监视器，希望进入方法执行的线程，就会处于BLOCKED状态。synchronized 代码块对一个lock对象上锁，也会造成阻塞。或者ReenTrantLock。

2. 上面一种情况是线程本身一直没获取过锁，还有一种情况是。线程获取了锁，但是又放弃了。比如一个线程在调用`wait()`方法放弃了监视器，在它被唤醒后还是会再次进入该监视器。但是如果别的线程在这期间进入了监视器，该线程就会因为等待监视器而进入阻塞状态。

   ![](https://github.com/TransientWang/KnowledgeBase/blob/master/picture/线程阻塞状态.png)

```java
static Q q = new Q();

public static void main(String[] args) throws Exception {

    Thread t = new mythread();
    t.start();

    System.out.println("");
    Thread.currentThread().sleep(100);
    synchronized (q) {
        q.notify();
    }
    Thread m = new mythread();
    m.start();
    Thread.currentThread().sleep(100);

    System.out.println("m:" + m.getState().name());
    System.out.println("t:" + t.getState().name());
}

static class mythread extends Thread {

    @Override
    public void run() {

        synchronized (q) {

            q.get();

        }
        System.out.println(Thread.currentThread().getName());
    }
}

static class Q {
    public synchronized void get() {

        try {
            wait();
            try {
                Thread.currentThread().sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}
```



WAITING状态，一般发生在调用wait()或者join()、LockSupport.park()，TIMED_WAITING状态就发生在带有限时的wait()方法。

当线程调用wait()放弃监视器的时候，就会处于WAITING状态，对于join()的情况，上下文中的线程，在等待调用join()方法的线程死亡的时候，也会处于WAITING状态。LockSupport.park()与wait()类似。

```java
public static void main(String[] args) {
    Observer Observer = new Observer(Thread.currentThread());
    try {
        Observer.join();
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
}
static class Observer extends Thread {
    Thread main;

    public Observer(Thread main) {
        this.main = main;
        this.start();
    }

    @Override
    public void run() {
        try {
            Thread.currentThread().sleep(1000);
            System.out.println(main.getState());
            Thread.currentThread().sleep(100000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

## 消息传递

java里消息传递的几种方式

1. wait() / notify() / notifyAll()

wait():导致当前线程等待它被唤醒，通常是通知或中断，或者经过了指定的时间。
当前线程必须拥有此对象的监视器锁。 有关线程可以成为监视器锁的所有者的方式的说明，请参阅notify方法。

此方法使当前线程（此处称为T）将自身置于此对象的等待集中，**然后放弃此对象上的任何和所有同步声明。** 请注意，只放弃此对象上的锁定; 当线程可以同步的任何其他对象在线程等待时保持锁定状态。

然后线程T因线程调度而被禁用，并且在发生以下任何一种情况之前处于休眠状态：

* 其他一些线程调用此对象的notify方法，并且线程T恰好被任意选为要被唤醒的线程。
* 其他一些线程为此对象调用notifyAll方法。
* 一些其他线程中断线程T.
* 指定的实时数量已经或多或少地过去了。 以纳秒为单位的实时量由表达式1000000 * timeoutMillis + nanos给出。 如果timeoutMillis和nanos都为零，则不考虑实时，并且线程等待直到被其他原因之一唤醒。
* 线程T被虚假唤醒。 

​	然后从该对象的等待集中删除线程T并重新启用线程调度。 **它以正常的方式与其他线程竞争**，以便在对象上进行同步; 一旦它重新获得对象的控制权，对象的所有同步声明都将恢复到原状 - 即，调用wait方法时的情况。 线程T然后从wait方法的调用返回。 因此，从wait方法返回时，对象和线程T的同步状态与调用wait方法时的状态完全相同。

线程可以在没有被通知，中断或超时的情况下唤醒，即所谓的虚假唤醒。 虽然这在实践中很少发生，但应用程序可以通过循环测试应该导致线程被唤醒的条件来防范它，并且如果条件不满足则继续等待。 


如果当前线程在等待之前或期间被任何线程中断，则抛出InterruptedException。 抛出此异常时，将清除当前线程的中断状态。 在如上所述恢复此对象的锁定状态之前，不会抛出此异常。

notify():唤醒正在此对象监视器上等待的单个线程。 如果任何线程正在等待此对象，则选择其中一个线程被唤醒。 选择是任意的，由实施决定。 线程通过调用其中一个wait方法等待对象的监视器。
在当前线程放弃对该对象的锁定之前，唤醒的线程将无法继续。 唤醒的线程将以通常的方式与可能正在竞争同步此对象的任何其他线程竞争; 例如，唤醒线程在成为锁定此对象的下一个线程时没有可靠的特权或劣势。

此方法只应由作为此对象监视器所有者的线程调用。 线程以三种方式之一成为对象监视器的所有者：

* 通过执行该对象的同步实例方法。
* 通过执行在对象上同步的同步语句的主体。
* 对于Class类型的对象，通过执行该类的同步静态方法。

2. 管道 PipedOutputStream/PipedWriter/PipedInputStream/PipedReader

```java
public static void main(String[] args) {
    PipeOutputThread pipeOutputThread = new PipeOutputThread();
    PipeIntputThread pipeIntputThread = new PipeIntputThread();
    pipeOutputThread.start();

    pipeIntputThread.start();


}

static class PipeOutputThread extends Thread {
    static PipedOutputStream pipedOutputStream;

    public PipeOutputThread() {
        this.pipedOutputStream = new PipedOutputStream();

    }

    public static PipedOutputStream getPipeOutPutStream() {
        return pipedOutputStream;
    }

    @Override
    public void run() {
        try {
            pipedOutputStream.connect(PipeIntputThread.getPipeIntPutStream());
            pipedOutputStream.write(Thread.currentThread().getName().getBytes());
            pipedOutputStream.flush();
            Thread.currentThread().sleep(100000);
            pipedOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

static class PipeIntputThread extends Thread {
    static PipedInputStream pipedInputStream;

    public PipeIntputThread() {
        this.pipedInputStream = new PipedInputStream();
    }

    public static PipedInputStream getPipeIntPutStream() {
        return pipedInputStream;
    }

    @Override
    public void run() {
        try {

            while (true) {
                if (pipedInputStream.available() > 0) {
                    byte[] bytes = new byte[100];
                    pipedInputStream.read(bytes);
                    for (int i = 0; i < bytes.length; i++) {
                        byte aByte = bytes[i];
                        if (aByte != 0)
                            System.out.print((char) aByte);

                    }
                    pipedInputStream.close();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
```

3. 通过ReentrantLock.newCondition()获得Condition对象

   ```java
   Condition condition = reentrantLock.newCondition();
   
   condition.await();
   ```

####  生产者消费者例子

下面是通过线程间消息传递实现的生产者、消费者的例子：

```java
public static void main(String[] args) throws Exception {
    Q q = new Q();
    Producer poducer = new Producer("生产者", q);
    Coumoster comsumer = new Coumoster("消费者", q);
    poducer.t.join();
    comsumer.t.join();
    System.out.println("主线程结束");
}

static class Q {
    Boolean condition = true;
    int i = 0;

    public synchronized void put() {
        while (!condition) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        i += 1;
        System.out.println("生产：" + i);
        notify();
        condition = false;
    }

    public synchronized void get() {
        while (condition) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        i -= 1;
        System.out.println("消费：" + i);
        notify();
        condition = true;
    }
}

static class Producer implements Runnable {
    String name;
    Q q;
    Thread t;

    public Producer(String name, Q q) {
        this.name = name;
        this.q = q;
        t = new Thread(this, name);
        t.start();
    }

    @Override
    public void run() {
        while (true) {
            q.put();
        }

    }


}


static class Coumoster implements Runnable {

    String name;
    Q q;
    Thread t;

    public Coumoster(String name, Q q) {
        this.name = name;
        this.q = q;
        t = new Thread(this, name);
        t.start();
    }

    @Override
    public void run() {
        while (true) {
            q.get();
        }

    }

}
```

## 同步

线程同步的几种方式

1. synchronized

   synchronized修饰方法、synchronized代码块

   同步方法在执行之前获取监视器。

   对于类（静态）方法，使用与方法类的Class对象关联的监视器。

   对于实例方法，使用与此关联的监视器（调用该方法的对象）。

2. volatile

   Java Memory Model确保所有线程看到的变量是一致的。

3. 原子变量

   java.util.concurrent.atomic包下的原子变量通过无锁的方式，保证变量与字段的原子性。

4. java.util.concurrent.locks包下的LockSupport、ReenTrantLock、ReenTrantReadWriteLock、StampedLock

5. java.util.concurrent包下面的Semaphore、CountDownLatch、CyclicBarrier、Exchanger

6. ThreadLocal无同步

7. 阻塞队列

## 并发容器，并发框架
ConcurrentHashMap、ConcurrentLinkedDeque、ConcurrentLinkedQueue、
ConcurrentSkipListMap、ConcurrentSkipListSet、CopyOnWriteArrayList、
CopyOnWriteArraySet、CountedCompleter、SynchronousQueue
