BlockingQueue接口定义了一种阻塞的FIFO queue，每一个BlockingQueue都有一个容量，让容量满时往BlockingQueue中添加数据时会造成阻塞，当容量为空时取元素操作会阻塞。

ArrayBlockingQueue是一个由数组支持的有界阻塞队列。在读写操作上都需要锁住整个容器，因此吞吐量与一般的实现是相似的，适合于实现“生产者消费者”模式。

LinkedBlockingQueue是基于链表的阻塞队列，同ArrayListBlockingQueue类似，其内部也维持着一个数据缓冲队列（该队列由一个链表构成），当生产者往队列中放入一个数据时，队列会从生产者手中获取数据，并缓存在队列内部，而生产者立即返回；只有当队列缓冲区达到最大值缓存容量时（LinkedBlockingQueue可以通过构造函数指定该值），才会

阻塞生产者队列，直到消费者从队列中消费掉一份数据，生产者线程会被唤醒，反之对于消费者这端的处理也基于同样的原理。而LinkedBlockingQueue之所以能够高效的处理并发数据，还因为其对于生产者端和消费者端分别采用了独立的锁来控制数据同步，这也意味着在高并发的情况下生产者和消费者可以并行地操作队列中的数据，

以此来提高整个队列的并发性能。


ArrayBlockingQueue和LinkedBlockingQueue的区别：

1. 队列中锁的实现不同

    ArrayBlockingQueue实现的队列中的锁是没有分离的，即生产和消费用的是同一个锁；

    LinkedBlockingQueue实现的队列中的锁是分离的，即生产用的是putLock，消费是takeLock

2. 在生产或消费时操作不同

    ArrayBlockingQueue实现的队列中在生产和消费的时候，是直接将枚举对象插入或移除的；

    LinkedBlockingQueue实现的队列中在生产和消费的时候，需要把枚举对象转换为Node<E>进行插入或移除，会影响性能

3. 队列大小初始化方式不同

    ArrayBlockingQueue实现的队列中必须指定队列的大小；

    LinkedBlockingQueue实现的队列中可以不指定队列的大小，但是默认是Integer.MAX_VALUE