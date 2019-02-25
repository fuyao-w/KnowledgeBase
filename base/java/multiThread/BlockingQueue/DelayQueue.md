## Delayed

```java
public interface Delayed extends Comparable<Delayed> 
```

混合样式界面，用于标记在给定延迟后应该对其执行操作的对象。
此接口的实现必须定义compareTo方法，该方法提供与其getDelay方法一致的排序。

```java
//以给定的时间单位返回与此对象关联的剩余延迟。
long getDelay(TimeUnit unit);
```

## DelayQueue

```java
public class DelayQueue<E extends Delayed> extends AbstractQueue<E>
    implements BlockingQueue<E> 
```

### java doc

Delayed元素的无界阻塞队列，其中只有在延迟过期时才能获取元素。 队列的头部是延迟元素，其延迟在过去最远。 如果没有延迟到期，则没有头，并且poll将返回null。 当元素的getDelay（TimeUnit.NANOSECONDS）方法返回小于或等于零的值时，会发生到期。 即使使用take或poll无法删除未过期的元素，它们也会被视为普通元素。 例如，size方法返回已过期和未过期元素的计数。 此队列不允许null元素。
该类及其迭代器实现了Collection和Iterator接口的所有可选方法。 方法iterator（）中提供的迭代器不保证以任何特定顺序遍历DelayQueue的元素。

### 示例

```java
public static void main(String[] args) {
    DelayQueue<DelayEmement> queue = new DelayQueue<>();
    DelayEmement delayEmement = new DelayEmement(10000, "测试");
    try {
        queue.put(delayEmement);
        System.out.println(queue.take().getMsg());
    } catch (Exception e) {
        e.printStackTrace();
    }
}

@Getter
@Setter
static class DelayEmement implements Delayed {
    private long delay;
    private long expire;
    private String msg;


    public DelayEmement(long delay, String msg) {
        this.delay = delay;
        this.expire = System.currentTimeMillis() + delay;
        this.msg = msg;

    }

    @Override
    public long getDelay(TimeUnit unit) {

        return unit.convert(this.expire - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {

        return (int) (this.getDelay(TimeUnit.MICROSECONDS) - o.getDelay(TimeUnit.MICROSECONDS));
    }

}
```

### 分析

```java
private final transient ReentrantLock lock = new ReentrantLock();
private final PriorityQueue<E> q = new PriorityQueue<E>();

/**
 *指定的线程等待队列头部的元素。 Leader-Follower模式的这种变体（http://www.cs.wustl.edu/~schmidt/POSA/POSA2/）
 * 用于最小化不必要的定时等待。 当一个线程成为领导者时，它只等待下一个延迟过去，
 * 但其他线程无限期地等待。 在从take（）或poll（...）返回之前，
 * 领导者线程必须发出一些其他线程的信号，除非其他一些线程成为临时的领导者。
 * 每当队列的头部被具有较早到期时间的元素替换时，领导者字段通过被重置为空而无效，
 * 并且用信号通知一些等待线程，但不一定是当前领导者。
 * 所以等待线程必须准备好在等待时获得并失去领导力。
 */
private Thread leader;

/**
 * 当一个较新的元素在队列的头部可用或新线程可能需要成为领导者时发出条件。
 */
private final Condition available = lock.newCondition();
```

### offer

```java
public boolean offer(E e) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        q.offer(e);
        if (q.peek() == e) {
            leader = null;
            available.signal();
        }
        return true;
    } finally {
        lock.unlock();
    }
}
```

offer逻辑比较简单，延迟队列的功能是在take方法上实现的。

```java
public E take() throws InterruptedException {
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    try {
        for (;;) {
            E first = q.peek();
            if (first == null)
                available.await();
            else {
                long delay = first.getDelay(NANOSECONDS);
                if (delay <= 0L)
                    return q.poll();
                first = null; // don't retain ref while waiting
                if (leader != null)
                    available.await();
                else {
                    Thread thisThread = Thread.currentThread();
                    leader = thisThread;
                    try {
                        available.awaitNanos(delay);
                    } finally {
                        if (leader == thisThread)
                            leader = null;
                    }
                }
            }
        }
    } finally {
        if (leader == null && q.peek() != null)
            available.signal();
        lock.unlock();
    }
}
```

`take`如果队列里没有元素，则阻塞直到其他线程将元素添加进入队列将当前线程唤醒。

如果队列中有元素则查看所要弹出的元素的过期时间，如果返回值大于0，则说明该元素还没有到过期时间不应该被获取,如果发现leader线程不为null。则说明已经有其他线程成为leader线程，则调用condition让当前线程等待，让其他优先线程获取。如果leader线程为null，则自己占领leader线程，并且阻塞一段时间。如果唤醒后leader线程是自己，则将leader线程置为null。这时其take线程应该被阻塞，当前线程应该去获取元素并返回。如果不是则进行下次自旋。下虽然take操作已经被重入锁锁住，但依然保守的设计假定有其他线程与当前线程都尝试成为leader线程的情况。如果这种情况真的出现，也不会对程序造成什么影响。