## 创建线程的几种方式

1. 继承Thread类，实现run()方法并调用Thread类的start()方法。
2. 实现Runnable接口，实现run()方法，将Runnable接口传递给Thread类，然后调用start()。
3. 通过`Executors`创建线程池。但是线程池也要在ThreadFactory里面通过前两种方式创建线程。

真正创建线程行为的只有继承Thread类的方式，实现Runnable也只是在run()里完成新线程的行为。

## 线程的几种状态

通过`Thread.State`枚举类可以查看线程有**六种状态**。

线程状态。 线程可以处于以下状态之一：

- `NEW`
  尚未启动的线程处于此状态。
- RUNNABLE
  在Java虚拟机中执行的线程处于此状态。
- BLOCKED
  被阻塞等待监视器锁定的线程处于此状态。
- WAITING
  无限期等待另一个线程执行特定操作的线程处于此状态。
- TIMED_WAITING
  正在等待另一个线程执行最多指定等待时间的操作的线程处于此状态。
- TERMINATED
  已退出的线程处于此状态。

线程在给定时间点只能处于一种状态。 这些状态是虚拟机状态，不反映任何操作系统线程状态。

NEW和RUNNABLE状态很好理解，那BLOCKED状态实在什么情况下产生的呢？

按照官方文档的解释，阻塞状态是在一种行为向另一种行为变化的过程中产生的。另一种行为比较清楚，就是该线程进入监视器，一种行为肯定就是没有获取监视器是的一些动作。当没获取监视器的线程希望也能进入监视器，但被其他线程抢先占领时。 该线程只能等待，这种等待的行为就叫阻塞。

什么情况下线程获取不到监视器，只能等待呢？

1. 在多个线程执行同一个对象的同步方法的时候。哪些没有获取到监视器，希望进入方法执行的线程，就会处于BLOCKED状态。synchronized 代码块对一个lock对象上锁，也会造成阻塞。或者ReenTrantLock。

2. 上面一种情况是线程本身一直没获取过锁，还有一种情况是。线程获取了锁，但是暂时放弃了。比如一个线程在调用`wait()`方法放弃了监视器，在它被唤醒后还是会再次进入该监视器。但是如果别的线程在这期间进入了监视器，该线程就会因为等待监视器而进入阻塞状态。

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