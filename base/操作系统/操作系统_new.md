---
typora-root-url: ../../picture
---

操作系统

### 进程：

运行中的程序，操作系统进行资源分配和调度的最小单位

进程有哪几种状态？

三态模型：就绪、运行和阻塞
五态模型：linux ：运行、可中断睡眠、不可中断睡眠、暂停or 跟踪 、退出 or 僵尸

### 线程：

线程就是一个实际的运行实体，是操作系统进行进行运算调度的最小单位。是进程中实际的运行单位，一个进程中可以运行多个线程，每条线程执行不同的任务。多个线程共享进程的内存空间，但是每个线程有自己的栈和程序计数器用于实现不同的任务。

### 运行状态：

新建、就绪、阻塞、运行、终止

### 内核线程和用户线程

内核线程是可调度实体，这意味着系统调度程序处理内核线程。（内核也必须是多线程的）
系统调度程序已知的这些线程与实现密切相关。为了便于编写可移植程序，库提供了用户线程。
一个内核线程是一个内核组件，如进程和中断处理程序; 它是系统调度程序处理的实体。内核线程在进程内运行，但可以被系统中的任何其他线程引用。除非您正在编写内核扩展或设备驱动程序，否则程序员无法直接控制这些线程。有关内核编程的更多信息，请参阅内核扩展和设备支持编程概念。
甲用户线程是使用程序员在程序中处理的控制的多个流的实体。用于处理用户线程的 API 由线程库提供。一个用户线程只存在于一个进程中；进程A中的用户线程不能引用进程B 中的用户线程。该库使用专有接口来处理内核线程以执行用户线程。与内核线程接口不同，用户线程 API 是符合 POSIX 标准的可移植编程模型的一部分。因此，在AIX®系统上开发的多线程程序可以轻松移植到其他系统。
在其他系统上，用户线程简称为线程，轻量级进程是指内核线程。

### 线程同步的方式：

信号量、信号、互斥锁、读写锁、条件变量

### 线程与进程的区别：

1.进程是资源分配的最小单位，线程是运算调度的最小单位
2.进程下的线程共享虚拟内存空间，文件描述符等资源，  进程则拥有独立的虚拟内存内存、等信息。
3.通信方式不同，线程可以通过，全局变量，互斥锁，条件变量来通信，进程一般通过管道or 共享内存来通信。
4.上下文切换速度不同，线程只需要切换程序计数器和栈。进程需要切换虚拟地址空间（页表）和寄存器、并且TLB 等硬件缓存也会失效，导致后续执行消耗的代价增大。
死锁：
必要条件：互斥、不可抢占、占有并等待、循环等待

### 死锁预防

破坏占有且等待：一次性获取全部资源
破坏循环等待：按照相同顺序操作，采用资源有序分配其基本思想是将系统中的所有资源顺序编号，将紧缺的，稀少的采用较大的编号，在申请资源时必须按照编号的顺序进行（从小往大申请），一个进程只有获得较小编号的进程才能申请较大编号的进程。
破快不可抢占： 新资源申请不下来赶紧释放所持有的资源

死锁避免：外部应用检测：银行家算法

### 如何避免死锁：按相同的顺序操作资源，放大lock 粒度

### 程间的通信的几种方式

信号、信号量  、匿名管道、有名管道、消息队列、共享内存、套接字

### 线程间通信方式：

1.锁（包括互斥锁、条件变量、读写锁），2临界区，3 信号量 4，信号

虚拟内存
虚拟内存系统负责为进程提供一个巨大的、稀疏的、私有的地址空间的假象，其中保存了程序的所有指令和数据。操作系统在专门硬件的帮助下，通过每一个虚拟内存的索引，将其转换为物理地址，物理内存根据获得的物理地址去获取所需的信息。操作系统会同时对许多进程执行此操作，并且确保程序之间互相不会受到影响，也不会影响操作系统。整个方法需要大量的机制（很多底层机制）和一些关键的策略。

### 操作系统中 段 的概念

进程内存结构一般分为 、堆、栈、代码块 如果将这三块分配在一个完整的内存地址中，则会出现比较打大的内存碎片，所以提出了一个泛化的概念 ，将堆、栈、代码块 以段为单位单独划分内存，当然 基址寄存器和界限寄存器也相当于变成了 3 个。 这样消除了内部碎片实现了更高效的虚拟内存，可以更好地支持稀疏的地址空间。另一个好处是可以支持代码共享（需要加保护位）

但是因为段 的大小不固定，所以会引入外部碎片，空闲内存被割裂成各种奇怪的大小，因为满足内存分配请求可能会很难。并且如果段很大则需要将其全部加载到内存中，所以为了解决这个问题。引入了页的概念。

页：上述段引起的外部碎片除了 针对内存进行定期整理外，还有一个更合适也是目前操作系统使用的办法，就是将空间分割成固定大小的分片。在虚拟内存中，将其成为 分页 。分页思想是将进程的地址值空间分割成固定大小的单元，每个单元称为 一页。相应地，我们把物理内存看成是定长槽块的阵列，要做页帧。每个页帧对应一个虚拟内存页。

操作系统通过页表的方式实现内存的虚拟化：页表是保存在内存中，主要作用是为地址空间每个虚拟页保存地址转换，从而可以找到物理内存的位置。 页表是每个进程的数据结构。 
操作系统在为进程分配资源的时候，初始化一个页表，对应用程序来说，他的地址空间就是连续而且从 0 开始，有效的实现了隔离。每个页表项保存这 pfn 物理内存帧号，和一些操作位 （如 存在、权限相关等）。在访问内存的时候 通过 TLB 硬件来加速地址转换。

页表也可以扩容到磁盘上，为进程提供逻辑上无限大的内存。 一般操作系统会将一块硬盘分配为 swap 空间，用于保存内存中存储不下的页。

### 缺页中断如何处理

在访问页的时候会 通过 TLB 找到具体的页表项，发现页表项里存在位 = 0 ，说明该页不在内存中。就会触发操作系统缺页中断。首先保存现场将寄存器和程序计数器相关信息推入内核栈，然后跳转到执行的处理程序然后，磁盘换回页面，更新页表 存在位 = 1，并更新PTE(页表项)。 中断执行完毕再恢复现场。重新执行TLB 访问，此时可以正确访问到内存。

频繁的页交换会发生操作系统抖动影响性能。

现在操作系统使用 ：段页混合 或者 多级页表（将线性页表变成树形结构，并且不需要指定未分配的页表项）



### 操作系统中进程调度策略有哪几种？

先来先服务、最短作业优先、最短剩余时间优先、轮转法、多级反馈队列、
MLFQ中有许多独立的队列（queue），每个队列有不同的优先级（prioritylevel）。任何时刻，一个工作只能存在于一个队列中。MLFQ总是优先执行较高优先级的工作（即在较高级队列中的工作）。当然，每个队列中可能会有多个工作，因此具有同样的优先级。在这种情况下，我们就对这些工作采用轮转调度。因此，MLFQ调度策略的关键在于如何设置优先级。MLFQ没有为每个工作指定不变的优先情绪而已，而是根据观察到的行为调整它的优先级。例如，如果一个工作不断放弃CPU去等待键盘输入，这是交互型进程的可能行为，MLFQ因此会让它保持高优先级。相反，如果一个工作长时间地占用CPU，MLFQ会降低其优先级。通过这种方式，MLFQ在进程运行过程中学习其行为，从而利用工作的历史来预测它未来的行为。

### 中断时会发生什么

中断可由操作系统时钟定时触发或者 IO 或者 内存缺页，或一些特殊情况触发（除以 0 ，访问非法内存）。中断触发后先保存现场(寄存器相关信息，程序计数器信息推进内核栈)，然后硬件会跳转到中断向量表（启动的时候，由操作系统指定内存位置），通过中断向量找到具体的处理程序（此时还可能会屏蔽中断）。开始处理程序，处理完后恢复现场， 继续执行或者执行退出。   协程是一种用户态的轻量级线程
协程完全实现在用户态，由处理器调度在挂在线程上执行。类似于可以被中断的函数。拥有自己的寄存器和、栈。 协程切换的时候需要保存现场。 直接操作栈也没有切换开销。不用切换到内核态 。 上下文切换速度快。

### 线程与协程的区别：

​	0.	在IO 方面，线程运行是同步机制，协程是异步机制（直接让出就可以）
​	0.	协程能保留上一次调用时的状态，每次过程重入时，就相当于进入上一次调用的状态。
​	0.	线程是协程的资源。协程通过Interceptor来间接使用线程这个资源。
​	0.	协程并不是取代线程, 而且抽象于线程之上, 线程是被分割的CPU资源, 协程是组织好的代码流程, 协程需要线程来承载运行, 线程是协程的资源, 但协程不会直接使用线程, 协程直接利用的是执行器(Interceptor), 执行器可以关联任意线程或线程池, 可以使当前线程, UI线程, 或新建新程.。
​	0.	协程依赖于线程、 线程依赖于进程
​	0.	协程创建内存是 KB 级别，线程是 mb 级别



### 并发与并行的区别

并发是一段时间内，多个任务都会被处理，但是在某一时刻只有一个任务执行。
并行是真正的多处理器执行。

### 进程与线程的切换流程？

进程的切换，实质上就是被中断运行进程与待运行进程的上下文切换。从主观上来理解。只分为两步：
1.虚拟地址空间的切换，包含页表。
3.硬件上下文（寄存器，程序计数器）和内核栈的切换
另外还会涉及到调度信息的修改(PCB 内容修改，将PCB 放到其他的（阻塞）队列)，比如进程表的内容等，然后TLB 也会失效导致后续进程运行速度受影响
线程切换：
只保存寄存器、PC 上下文和线程栈，不涉及虚拟地址空间的切换。 

### IO 多路复用

目前支持I/O多路复用的系统调用有 select，pselect，poll，epoll，I/O多路复用就是通过一种机制，一个进程可以监视多个描述符，一旦某个描述符就绪（一般是读就绪或者写就绪），能够通知程序进行相应的读写操作。但select，pselect，poll，epoll本质上都是同步I/O，因为他们都需要在读写事件就绪后自己负责进行读写，也就是说这个读写过程是阻塞的，而异步I/O则无需自己负责进行读写，异步I/O的实现会负责把数据从内核拷贝到用户空间。

聊聊IO多路复用之select、poll、epoll详解
https://www.jianshu.com/p/dfd940e7fca2
epool 水平触发与边缘触发：
水平触发是只要读缓冲区有数据，就会一直触发可读信号，而边缘触发仅仅在空变为非空的时候通知一次，







### 简述 select, poll, epoll 的使用场景以及区别，epoll 中水平触发以及边缘触发有什么不同？

三者都是 IO 多路复用， 思想是同一个线程监听不同的fd ，有准备好的则内核通知线程，然后扫描链表 找到准备好的链接。
select 一般有 1024 连接的限制，poll 没有，但是他俩都要去扫描所有的链接，不管准备准备好。 epool 利用内核和io 直接沟通的优势，通过注册一个 callbcak ，io 准备好后，内核调用 callback 将准备好的链接单独塞到一个队列中， 这样只需要扫描这个已经准备好的队列即可。性能很高，并且没有监听数量限制。

水平触发是只要读缓冲区有数据，就会一直触发可读信号，而边缘触发仅仅在空变为非空的时候通知一次，

### 进程和线程之间有什么区别

进程是处于运行状态中的程序，是系统进行资源分配和调度的最小单位
线程是程序运行的实体，是系统运算调度的最小单位。

从内存上来说，进程内存结构一般包含（堆、栈、代码块）、进程启动需要初始化虚拟内存页表，并且要分配一块内存给内核空间。各个进程不共享内存空间
而线程启动不重新初始化内存，所有线程共享内存空间。每个线程有自己的栈和程序计数器。

启动和切换效率上来说  进程启动慢，cpu 进行进程调度的开销也大 （涉及虚拟内存转换，相关联的硬件比如 tlb 缓存全报废）。线程启动较快耗费资源小，线程调度开销也小。

其他：线程间和进程间通信方式不同
进程：匿名管道、有名管道、消息队列、共享内存、socket、信号、信号量
线程：锁（互斥量、读写锁） 、条件变量、临界区（java）、信号、信号量

### 操作系统如何进行内存管理

一般有分页 和 分段 两种

分段解决的问题：进程内存空间分为 堆、栈、代码块， 这三块之间有很大的空闲内存无法使用，造成较大的内部碎片。所以提出段这样一个泛化的概念，将堆、栈、代码块 以段为单位单独管理和分配（肯定有段表了），每个段有自己的基址寄存器和界限寄存器。

在分段后，虽然解决了内部碎片的问题，但是因为各个段之间的内存空闲大小不固定，无法高效分配内存。所以又提出分页的概念解决外部碎片的问题。
分页的思想是将内存 以 页为单位（8k），每个页映射到一个物理内存相应大小的页帧中。内存页通过页表的方式管理，每个页表项保存页到物理帧的映射管理（pfn 帧号），通过TLB 硬件加速转换。

同时以分页为基础，操作系统实现了内存的虚拟化。 核心的原理就是为进程分配逻辑上连续的内存页，内存页通过保存在每个进程空间中的页表进行管理。即达到了进程间内存隔离的效果又达到了进程内内存管理的简化（每个进程都认为自己有连续的内存空间）。  同时，操作系统通过将虚拟的内存页扩容到磁盘上又为进程提供了无限大的内存，一般操作系统可以分配 swap 空间用于物理内存满了的时候将进程暂时不需要的数据移动到磁盘上，这块是通过页表上的标志位实现判断和控制的。但是频繁的进行内存和磁盘进行交换会引起系统颠簸，严重影响性能。 而且发现内存不在物理内存上还会引发缺页异常（引起中断处理逻辑）降低性能。

另外内存页概念的出现还未 不同的进程共享相同的内存（比如运行相同的代码）提供了可能。 通过页表上增加标志位就可以了。。。



### 操作系统中，虚拟地址与物理地址之间如何映射？

通过页表，每个页表项保存了内存页到物理页帧的映射（保存了 pfn 物理的内存帧号），然后通过 硬件 tlb 实现加速计算和缓存。当然，进程切换会导致 tlb 失效，拉低性能

简述操作系统中的缺页中断
先访问 tlb 发现未命中，然后访问页表，系统根据页表项访问内存页，发现其中的 存在 标致位 为 0 ，表示数据在磁盘上。然后引发缺页中断，首先保存现场。然后操作系统的通过中断处理向量表找到处理程序，将磁盘上的内存页换回物理内存中（这其中可能还要涉及将其他的页换出）。更新页表，然后重试指令，重新查询TLB 然后从页表加载到TLB ，最后就就正常运行了

### 系统出现大量缺页中断的可能原因

1.　malloc和mmap等内存分配函数只是建立进程的虚拟地址空间，并没有分配实际的物理内存。
当进程访问没有建立映射关系的虚拟内存时会自动的触发一个缺页中断。
2.系统内存不够了被内存页被频繁的换进换出



### 什么时候会由用户态陷入内核态

系统调用、中断（时钟、IO、异常、内存缺页，除 0 ，非法指针访问）

### 简述自旋锁与互斥锁的使用场景

自旋锁是一种乐观锁一般用在读多写少场景，原理是 cas + 重试 ，比如java 里的并发map 就会用到。
互斥锁比较重，是完全互斥的，使用场景： 自己封装的并发数据结构（并发map）

### MMAP原理

mmap用于把文件映射到内存空间中，简单说mmap就是把一个文件的内容在用户内存里面做一个映像。映射成功后，用户对这段内存区域的修改可以直接反映到内核空间，同样，内核空间对这段区域的修改也直接反映用户空间。那么对于内核空间<---->用户空间两者之间需要的数据传输就不再需要了。

### Linux 下如何查看端口被哪个进程占用？  

lsof -i 

### 简述 Linux 进程调度的算法

先来先服务，最短进程优先，最短剩余时间优先，轮转法、多级反馈队列法

### 0 拷贝原理

减少CPU 拷贝和上下文切换
传统方式 读写 需要 涉及到 内容在 用户态 、内核态、再到dma 、io 设备的复制
0 拷贝是取消 用户态到内核态的复制，避免 用户态和内核态的切换。或者减少必要的赋值从而提高性能。
一般的实现有 三种思路
用户态直接 io ，  mmap +write、send file (socket 到 io 设备),splice(可以不是 socket 其他IO 也行)
写时复制技术 

无论是传统的 I/O 方式，还是引入了零拷贝之后，2 次 DMA copy是都少不了的。因为两次 DMA 都是依赖硬件完成的。所以，所谓的零拷贝，都是为了减少 CPU copy 及减少了上下文的切换。

### 什么是DMA

DMA，全称Direct Memory Access，即直接存储器访问。
DMA 就是一个小小的带有自己的CPU 的内存控制器，可以独立将内存复制到IO 设备中，降低CPU 的负担。
DMA传输将数据从一个地址空间复制到另一个地址空间，提供在外设和存储器之间或者存储器和存储器之间的高速数据传输。当CPU初始化这个传输动作，传输动作本身是由DMA控制器来实现和完成的。DMA传输方式无需CPU直接控制传输，也没有中断处理方式那样保留现场和恢复现场过程，通过硬件为RAM和IO设备开辟一条直接传输数据的通道，使得CPU的效率大大提高。

### 什么情况下，进程会进行切换

进程切换可以在操作系统从当前正在运行的进程中获得控制权的任何时刻发生
时钟中断（正常的进程调度）、I\O 中断、内存缺页异常、陷阱指令、或者进程崩溃  ，或者正在执行的程序使用系统调用激活新的程序

### Linux 系统怎么看缺页中断？

ps -o majflt,minflt -c 程序名 或者pidstat

### KILL 命令

kill命令的工作原理是向Linux系统的内核发送一个系统操作信号和某个程序的进程标识号，然后系统内核就可以对进程标识号指定的进程进行操作。
执行kill -9 <PID>，进程是怎么知道自己被发送了一个信号的？首先要产生信号，执行kill程序需要一个pid，根据这个pid找到这个进程的task_struct（这个是Linux下表示进程/线程的结构），然后在这个结构体的特定的成员变量里记下这个信号。 这时候信号产生了但还没有被特定的进程处理，叫做Pending signal。 等到下一次CPU调度到这个进程的时候，内核会保证先执行do\_signal这个函数看看有没有需要被处理的信号，若有，则处理；若没有，那么就直接继续执行该进程。所以我们看到，在Linux下，信号并不像中断那样有异步行为，而是每次调度到这个进程都是检查一下有没有未处理的信号。
当然信号的产生不仅仅在终端kill的时候才产生的。总结起来，大概有如下三种产生方式：
	•	硬件异常：比如除0
	•	软件通知：比如当你往一个已经被对方关闭的管道中写数据的时候，会发生SIGPIPE
	•	终端信号：你输入kill -9 <PID>，或者control+c就是这种类型

### inode是什么？

理解inode，要从文件储存说起。
文件储存在硬盘上，硬盘的最小存储单位叫做"扇区"（Sector）。每个扇区储存512字节（相当于0.5KB）。
操作系统读取硬盘的时候，不会一个个扇区地读取，这样效率太低，而是一次性连续读取多个扇区，即一次性读取一个"块"（block）。这种由多个扇区组成的"块"，是文件存取的最小单位。"块"的大小，最常见的是4KB，即连续八个 sector组成一个 block。
文件数据都储存在"块"中，那么很显然，我们还必须找到一个地方储存文件的元信息，比如文件的创建者、文件的创建日期、文件的大小等等。这种储存文件元信息的区域就叫做inode，中文译名为"索引节点"。
每一个文件都有对应的inode，里面包含了与该文件有关的一些信息。

### 硬链接 和 软链接

硬链接： 与普通文件没什么不同，inode 都指向同一个文件在硬盘中的区块。 删除任意一个硬链接或者删除原文件都不影响，还是可以通过其他硬链接读取到文件内容
软连接：保存了其代表的文件的绝对路径，是另外一种文件，在硬盘上有独立的区块，访问时替换自身路径。删除源文件后不能通过软连接再访问

### Malloc 原理

Malloc函数用于动态分配内存。为了减少内存碎片和系统调用的开销，malloc其采用内存池的方式，先申请大块内存作为堆区，然后将堆区分为多个内存块，以块作为内存管理的基本单位。当用户申请内存时，直接从堆区分配一块合适的空闲块。Malloc采用隐式链表结构将堆区分成连续的、大小不一的块，包含已分配块和未分配块；同时malloc采用显示链表结构来管理所有的空闲块，即使用一个双向链表将空闲块连接起来，每一个空闲块记录了一个连续的、未分配的地址。
当进行内存分配时，Malloc会通过隐式链表遍历所有的空闲块，选择满足要求的块进行分配；当进行内存合并时，malloc采用边界标记法，根据每个块的前后块是否已经分配来决定是否进行块合并。

所以malloc采用的是内存池的管理方式（ptmalloc），Ptmalloc 采用边界标记法将内存划分成很多块，从而对内存的分配与回收进行管理。为了内存分配函数malloc的高效性，ptmalloc会预先向操作系统申请一块内存供用户使用，当我们申请和释放内存的时候，ptmalloc会将这些内存管理起来，并通过一些策略来判断是否将其回收给操作系统。这样做的最大好处就是，使用户申请和释放内存的时候更加高效，避免产生过多的内存碎片。

### 进程空间从高位到低位都有些什么？

1.上面是内核信息，下面是用户信息
从上到下分别为 页表，和与进程相关的数据结构 和 内核栈 ，后面是内核的代码和数据 。再往下是用户态的数据，分别为 用户栈 ，堆，代码段。
Linux用户空间(https://juejin.cn/post/6844903684514545677)
Linux用户地址空间从低位到高位的顺序可以分为：文本段(Text Segment)、初始化数据段(Data Segment)、未初始化数据段(Bss Segment)、堆(Heap)、栈(Stack)和环境变量区(Environment variables)

#### 文本段

用户空间的最低位是文本段，包含了程序运行的机器码。文本段具有只读属性，防止进程意外修改了指令导致程序出错。而且对于多进程的程序，可以共享同一份程序代码，这样减少了对物理内存的占用。
但是文本段并不是从0x0000 0000开始的，而是从0x0804 8000开始。0x0804 8000以下的地址是保留区，进程是不能去访问该地址段的数据，因此C语言中将为空的指针指向0。

#### 初始化数据段

文本段上面就是初始化的数据段，数据段包含显示初始化的全局变量和静态变量。当程序被加载到内存中时，从可执行文件中读取这些数据的值，并加载到内存。因此，可执行文件中需要保存这些变量的值。

#### Bss

Bss段包含未初始化的全局变量和静态变量，还包含显示初始化为0的全局变量(根据编译器的实现)。当程序被加载到内存中时，这一段内存就会被初始化为0。可执行文件中只需要保存这一段内存的起始地址就行，因此减小了可执行文件的大小。

#### 堆

堆从下自上增长(根据实现)，用于动态分配内存。堆的顶端成为program break，可以通过brk和sbrk函数调整堆顶的位置。c语言通过malloc函数实现动态内存分配，通过free释放分配的内存，后面会详细描述这两个函数的实现。堆上的内存通过一个双向链表进行维护，链表的每个节点保存这块内存的大小是否可用等信息。在堆上分配内存可能会导致以下问题： （1）分配的内存，没有释放，就会导致内存泄漏； （2）频繁的分配小块的内存有可能导致堆上都是剩余的小块的内存，这称为内存碎片；

#### 栈

栈是一个动态增长和收缩的段，栈是自顶向下增长。栈由栈帧组成，每调用一个函数，系统会为每个当前调用的函数分配一个栈帧，栈帧从存储了参数的实参，以及函数中使用的局部变量，当函数返回时，该函数的栈帧就会弹出，函数中的局部变量因此也就被销毁了。

![对每个进程，](/system/对每个进程，.png)

不需要将每一块的意义都讲出来，我们今天在这里简述。这个图是进程的虚拟空间的分配模型图，可以看到其分为用户空间和内核空间，用户空间从低位到高位发展，存放的是这个进程的代码段和数据段，以及运行时候的堆和用户栈。内核空间从高位到低位，存放着内核的代码和数据，以及内核为这个进程创建的相关数据结构，比如页表等。

除了内存之外，还有文件操作符的保存。Linux之中“一切皆文件”。使用open系统调用，可以返回一个整数作为文件描述符file descriptor，那么进程就可以使用file descriptor来作为参数在任何系统调用之中标识那个打开的文件，内核为进程维护了一个文件描述符表来保持进程所有获得的file descriptor。

### 五种IO 模型 

https://www.jianshu.com/p/486b0965c296
同步阻塞、同步非阻塞、多路复用IO（事件驱动IO） 、异步IO 、信号驱动IO
同步：发生 IO 操作的时候阻塞process
异步：发生 IO 操作的时候不阻塞process
阻塞：一个具体调用是必须等到数据准备好才可以返回
非阻塞：一个具体调用如果没准备好就返回一个空值或错误

同步阻塞 就代表 IO 操作和查询的调用都是阻塞的
同步非阻塞 代表 查询不阻塞，但是进行 IO 操作的时候阻塞
多路复用 也是同步阻塞类型的
异步 IO 是在内核将数据准备好后直接通知 process 没有任何的阻塞	

### 怎样理解阻塞非阻塞与同步异步的区别？

https://www.zhihu.com/question/19732473

	0.	阻塞/非阻塞， 同步/异步的概念要注意讨论的上下文：
	•	在进程通信层面， 阻塞/非阻塞， 同步/异步基本是同义词， 但是需要注意区分讨论的对象是发送方还是接收方。
	•	发送方阻塞/非阻塞（同步/异步）和接收方的阻塞/非阻塞（同步/异步） 是互不影响的。
	•	在 IO 系统调用层面（ IO system call ）层面， 非阻塞 IO 系统调用 和 异步 IO 系统调用存在着一定的差别， 它们都不会阻塞进程， 但是返回结果的方式和内容有所差别， 但是都属于非阻塞系统调用（ non-blocing system call ）
2. 非阻塞系统调用（non-blocking I/O system call 与 asynchronous I/O system call） 的存在可以用来实现线程级别的 I/O 并发， 与通过多进程实现的 I/O 并发相比可以减少内存消耗以及进程切换的开销。

一个非阻塞I/O 系统调用 read() 操作立即返回的是任何可以立即拿到的数据， 可以是完整的结果， 也可以是不完整的结果， 还可以是一个空值。
而异步I/O系统调用 read（）结果必须是完整的， 但是这个操作完成的通知可以延迟到将来的一个时间点。

#### 管道是怎么通信的呢？

首先管道是内核的一个缓冲区，而且是在内存中。管道一头连接着一个进程的输出，另一头连接着另一个进程的输入。一个缓冲区不需要很大，它被设计成为环形的数据结构，以便管道可以被循环利用。当管道中没有信息的话，从管道中读取的进程会等待，直到另一端的进程放入信息。当管道被放满信息的时候，尝试放入信息的进程会等待，直到另一端的进程取出信息。当两个进程都终结的时候，管道也自动消失。看下图： 



https://bbs.huaweicloud.com/blogs/detail/249699