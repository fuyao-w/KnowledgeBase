---
typora-root-url: ../../picture
---

**Go**

**TCmalloc**

是google 的内存分配库，性能很高， Mysql 5.7 就是用了该库分配内存。

设计原理为 将内存 根据不同大小分为三级 （线程缓存、中心列表，页堆）分别管理 小、大 对象。 小对象为  < 32K  。大对象为 32 K 以上。



TCMalloc 为每个线程分配一个本地缓存。线程本地缓存可以满足少量分配的需求。当本地缓存没有空闲内存的时候会向中心列表申请，中心列表内存不够或者申请了大对象则直接向页堆申请。



TCMalloc 处理大小 <= 32K 的对象（“小”对象）与较大的对象不同。大对象使用页面级分配器直接从中央堆分配（页面是 4K 对齐的内存区域）。即，一个大对象总是页面对齐的并且占据整数个页面。一连串的页面可以被分割成一系列小object，每个小object的大小都一样。例如，一页 (4K) 的运行可以分成 32 个大小为 128 字节的对象。

总结：TCmalloc 的内存单位以页来划分 4k ，多个页聚合 为 span 以提供更大的内存， 单个页拆分提供更小的内存。相同大小的页，通过链表串联起来。每个级别的内存池中都有这样改的不同内存大小的链表

小对象分配：

线程缓存包含大约 170 个不同大小类别的的空闲对象的单向链接列表。 按照 8 字节 16，32 最大 256 区分。分配器按照申请内存大小找到相应链表进行分配。线程缓存里的内存操作无锁，所以性能非常高。当线程缓存没有可用的内存分配的时候，会向中心列表申请，这个时候需要加锁 。 中心列表也没有就会向页堆申请。



大对象分配：

先解释span概念，多个连续页组成一个span

大对象 **>32 k** （向上舍入页面大小为**4k**），并由页堆处理。业堆也是一个空闲列表数组。 对于 **i< 256** ，第**k th** 个索引是由**K** 个页面组成的空闲链表。最第**256** 个条目是由长度 **>= 256** 个页面**span**的自由链表。 根据计算即可找到相应大小**span** 看有没有页面可以分配。如果找不到则从操作系统申请。



解除分配

当一个对象被释放时，我们计算它的页码并在中央数组中查找它以找到相应的跨度对象。跨度告诉我们物体是否小，如果小则它的大小等级。如果对象很小，我们将它插入到当前线程的线程缓存中适当的空闲列表中。如果线程缓存现在超过预定大小（默认为 2MB），我们将运行垃圾收集器，将未使用的对象从线程缓存移动到中央空闲列表中。

如果对象很大，跨度告诉我们对象覆盖的页面范围。假设这个范围是[p,q]。我们还查找页面p-1和的跨度q+1。如果这些相邻跨度中的任何一个是空闲的，我们将它们与[p,q]跨度合并 。生成的跨度被插入到页堆中适当的空闲列表中。





GO 的实现

Go 将对象分为 3 中 分别为 小（0-16 B）、中[16B-32KB]、大[32kb - ]

分配规则： 

微型对象（我们主要使用它来分配较小的字符串以及逃逸的临时变量）使用微型分配器，将零散对象分配到一个页里面， 再一次尝试线程缓存，中心缓存和页堆分配内存

小对象：依次尝试使用线程缓存、中心缓存、和堆

大对象：直接在堆上分配内存







**Go 调度器**

https://iswbm.com/537.html

Go 语言的调度器通过使用与 CPU 数量相等的线程减少线程频繁切换的内存开销，同时在每一个线程上执行额外开销更低的 Goroutine 来降低操作系统和硬件的负载。





历史：

0.x 最早只有一个线程 （G-M 模型）

1.0 多线程调度器 （允许运行多线程程序，全局锁导致竞争严重）

1.1 引入了处理器 P ,构成了 目前的 GMP 模型、在 P 的基础上实现了基于工作窃取的调度器，

1.2 -1.13 基于协作的抢占是调度器，在goroutine 函数调用时 插入抢占检查指令，在调用时检查当前 Goroutine 是否放弃了抢占请求，实现基于协作式的抢占调度

1.14 - 基于信号实现的抢占式调度器：

数据结构

![GOLANG SCHEDULER](/go/GOLANG SCHEDULER.png)







1.G - 表示 goroueine ，一个待执行的任务；

2.M - 表示操作系统线程，由操作系统的调度器调度和管理；

3.P - 表示处理器，它可以被看做运行在线程上的本地调度器；



G 里面存有 栈和寄存器等信息，用于实现上下文切换 还有 goid 

M 调度器最多可以创建10000 个线程，其中大多数线程都不会执行用户代码，最多只有 GOMAXPROCS 个活跃线程能够正常运行。

在默认情况下，运行时会将 GOMAXPROCS 设置成当前机器的核数，我们也可以在程序中使用 [runtime.GOMAXPROCS](https://draveness.me/golang/tree/runtime.GOMAXPROCS) 来改变最大的活跃线程数。

在大多数情况下，我们都会使用 Go 的默认设置，也就是线程数等于 CPU 数，默认的设置不会频繁触发操作系统的线程调度和上下文切换，所有的调度都会发生在用户态，由 Go 语言调度器触发，能够减少很多额外开销。



调度器中的处理器 P是 M 和 G 的中间层，它能提供线程需要的上下文环境，也会负责调度线程上的等待队列，通过处理器 P 的调度，每一个线程都能够执行多个 Goroutine，它能在 Goroutine 进行一些 I/O 操作时及时让出计算资源，提高线程的利用率。

因为调度器在启动时就会创建 GOMAXPROCS 个处理器，所以 Go 语言程序的处理器数量一定会等于 GOMAXPROCS，这些处理器会绑定到不同的内核线程上。



调度过程：

程序初始化的时候先 创建 M 和 P



简单总结一下，Go 语言有两个运行队列，其中一个是处理器本地的运行队列(最大 256 的环形链表)，另一个是调度器持有的全局运行队列，只有在本地运行队列没有剩余空间时才会使用全局队列。

调度循环开启：

[runtime.schedule](https://draveness.me/golang/tree/runtime.schedule) 函数会从下面几个地方查找待执行的 Goroutine：

1. 为了保证公平，当全局运行队列中有待执行的 Goroutine 时，通过 schedtick 保证有一定几率会从全局的运行队列中查找对应的 Goroutine；
2. 从处理器本地的运行队列中查找待执行的 Goroutine；
3. 如果前两种方法都没有找到 Goroutine，会通过 [runtime.findrunnable](https://draveness.me/golang/tree/runtime.findrunnable) 进行阻塞地查找 Goroutine；

[runtime.findrunnable](https://draveness.me/golang/tree/runtime.findrunnable) 的实现非常复杂，这个 300 多行的函数通过以下的过程获取可运行的 Goroutine：

1. 从本地运行队列、全局运行队列中查找；
2. 从网络轮询器中查找是否有 Goroutine 等待运行；
3. 通过 [runtime.runqsteal](https://draveness.me/golang/tree/runtime.runqsteal) 尝试从其他随机的处理器中窃取待运行的 Goroutine，该函数还可能窃取处理器的计时器；

因为函数的实现过于复杂，上述的执行过程是经过简化的，总而言之，当前函数一定会返回一个可执行的 Goroutine，如果当前不存在就会阻塞等待。





这里介绍的是 Goroutine 正常执行并退出的逻辑，实际情况会复杂得多，多数情况下 Goroutine 在执行的过程中都会经历协作式或者抢占式调度，它会让出线程的使用权等待调度器的唤醒。



除了上图中可能触发调度的时间点，运行时还会在线程启动 [runtime.mstart](https://draveness.me/golang/tree/runtime.mstart) 和 Goroutine 执行结束 [runtime.goexit0](https://draveness.me/golang/tree/runtime.goexit0) 触发调度。我们在这里会重点介绍运行时触发调度的几个路径：

- 主动挂起 — [runtime.gopark](https://draveness.me/golang/tree/runtime.gopark) -> [runtime.park_m](https://draveness.me/golang/tree/runtime.park_m)
- 系统调用 — [runtime.exitsyscall](https://draveness.me/golang/tree/runtime.exitsyscall) -> [runtime.exitsyscall0](https://draveness.me/golang/tree/runtime.exitsyscall0)
- 协作式调度 — [runtime.Gosched](https://draveness.me/golang/tree/runtime.Gosched) -> [runtime.gosched_m](https://draveness.me/golang/tree/runtime.gosched_m) -> [runtime.goschedImpl](https://draveness.me/golang/tree/runtime.goschedImpl)
- 系统监控 — [runtime.sysmon](https://draveness.me/golang/tree/runtime.sysmon) -> [runtime.retake](https://draveness.me/golang/tree/runtime.retake) -> [runtime.preemptone](https://draveness.me/golang/tree/runtime.preemptone)

我们在这里介绍的调度时间点不是将线程的运行权直接交给其他任务，而是通过调度器的 [runtime.schedule](https://draveness.me/golang/tree/runtime.schedule) 重新调度。





全局队列（Global Queue）：存放等待运行的 G。

P 的本地队列：同全局队列类似，存放的也是等待运行的 G，存的数量有限，不超过 256 个。新建 G’时，G’优先加入到 P 的本地队列，如果队列满了，则会把本地队列中一半的 G 移动到全局队列。

P 列表：所有的 P 都在程序启动时创建，并保存在数组中，最多有 GOMAXPROCS(可配置) 个。

M：线程想运行任务就得获取 P，从 P 的本地队列获取 G，P 队列为空时，M 也会尝试从全局队列拿一批 G 放到 P 的本地队列，或从其他 P 的本地队列偷一半放到自己 P 的本地队列。M 运行 G，G 执行之后，M 会从 P 获取下一个 G，不断重复下去。



有关 P 和 M 的个数问题

1、P 的数量：



由启动时环境变量 $GOMAXPROCS 或者是由 runtime 的方法 GOMAXPROCS() 决定。这意味着在程序执行的任意时刻都只有 $GOMAXPROCS 个 goroutine 在同时运行。

2、M 的数量:



go 语言本身的限制：go 程序启动时，会设置 M 的最大数量，默认 10000. 但是内核很难支持这么多的线程数，所以这个限制可以忽略。

runtime/debug 中的 SetMaxThreads 函数，设置 M 的最大数量

一个 M 阻塞了，会创建新的 M。

M 与 P 的数量没有绝对关系，一个 M 阻塞，P 就会去创建或者切换另一个 M，所以，即使 P 的默认数量是 1，也有可能会创建很多个 M 出来。





**为什么要有P**

1. 调度器和锁是全局资源，所有的调度状态都是中心化存储的，锁竞争问题严重；
2. 线程需要经常互相传递可运行的 Goroutine，引入了大量的延迟；
3. 每个线程都需要处理内存缓存，导致大量的内存占用并影响数据局部性；
4. 系统调用频繁阻塞和解除阻塞正在运行的线程，增加了额外开销；

**引入 P的好处**

- 每个 P 有自己的本地队列，大幅度的减轻了对全局队列的直接依赖，所带来的效果就是锁竞争的减少。而 GM 模型的性能开销大头就是锁竞争。
- 当一个 M 中 运行的 G 发生阻塞性操作时，P 会重新选择一个 M，若没有 M 就新创建一个 M 来继续从 P 本地队列中取 G 来执行，提高运行效率。
- 每个 P 相对的平衡上，在 GMP 模型中也实现了 Work Stealing 算法，如果 P 的本地队列为空，则会从全局队列或其他 P 的本地队列中窃取可运行的 G 来运行，减少空转，提高了资源利用率。

**
 
 defer 原理**

根据 defer 关键字后面定义的函数 *fn* 以及 参数的size，来创建一个延迟执行的 函数 ，将函数需要的参数直接复制一份，并将这个延迟函数，挂在到当前g的 *_defer* 的链表上.

当函数结束后，从 _defer 链表上从后往前执行。 需要注意的是，defer 声明的之后就直接将执行函数右边的参数进行复制。这个时候会获取不到后面执行的结果。正确 方法是使用 闭包，声明一个匿名函数。将指针指针复制过来。这也是 recover 必须要在匿名函数里的原因

**
 panic 原理**

程序在遇到panic的时候，就不再继续执行下去了，先把当前panic 挂载到 g._panic 链表上，开始遍历当前g的g._defer链表，然后执行_defer对象定义的函数等，如果 defer函数在调用过程中又发生了 panic，则又执行到了 gopanic函数，最后，循环打印所有panic的信息，并退出当前g。然而，如果调用defer的过程中，遇到了recover，则继续进行调度（mcall(recovery)）。





**make 和 new 的区别**

make 返回一个对象实例。 new 返回一个类型的指针并且为指针分配好内存









**GC**

算法：

标记-清除：从根变量开始遍历所有引用的对象，引用的对象标记为"被引用"，没有被标记的进行回收。

- 优点：解决了引用计数的缺点（不能很好的处理循环引用，而且实时维护引用计数，有也一定的代价。）
- 缺点：需要STW，即要暂时停掉程序运行。
- 代表语言：Golang(其采用三色标记法)

**stop the world**

是gc的最大性能问题，对于gc而言，需要停止所有的内存变化，即停止所有的goroutine，等待gc结束之后才恢复。

触发

阈值：默认内存扩大一倍，启动gc
 定期：默认2min触发一次gc，src/runtime/proc.go:forcegcperiod
 手动：runtime.gc()







**GO 内存飙升排查**

1.Go routine 泄露，例子：http body 读取完未关闭

2.底层数据很大的指针在goroutine 之间传递并且一直被引用无法回收，类似 slice map 等



**Go 优缺点**

优点：

语法简洁，开发速度快

运行速度快

并发功能强大

支持垃圾回收

标准库丰富而且易用

可直接编译成机器码，更可以跨平台编译

内嵌C 支持





缺点：

生态差点

错误处理繁琐

软件包工具不是特别好用

没有泛型，有时候会多写一些代码，但是还可以接受







**两次 GC 周期重叠会引发什么问题，GC 触发机制是什么样的？**



回答：首先go的gc没有这个问题，系统监控在执行前会首先判断GC 状态， runtime.GC() 会等待当前的标记过程结束。



GC 周期重叠的问题：Stop the world 的时间增加 ，标记过程由于写屏障会强行更改对象的颜色，（如果没有并发控制）是会出现mark冲突的 ，可能会在更新 GCmarkBits 的时候出错，导致有对象被误回收。