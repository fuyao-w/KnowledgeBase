**GO**

Context 用法：

1.传递上下文数据 例子：trace 系统通过 traceID 获取goroutine

2.超时控制 or 主动取消 线程树

withValue

withCancel

withTimeOut

withDeadLine



**Select 用法**



1. select语句只能用于channel 的读写操作
2. select中的case条件(非阻塞)是并发执行的，select会选择先操作成功的那个case条件去执行，如果多个同时返回，则随机选择一个执行，此时将无法保证执行顺序。对于阻塞的case语句会直到其中有信道可以操作，如果有多个信道可操作，会随机选择其中一个 case 执行
3. 对于case条件语句中，如果存在信道值为nil的读写操作，则该分支将被忽略，可以理解为从select语句中删除了这个case语句
4. 对于空的select{}，会引起死锁
5. 对于for中的select{}, 也有可能会引起cpu占用过高的问题
6. Case 数量上限 65536

原理

首先确定两个顺序

1. channel 的遍历顺序，引入随机性
2. channel 的 lock 顺序(按地址排序)（底层直接引用 channel 结构，需要锁操作），防止出现死锁 ，其他的goroutine 也可以写相同的select 是不是



\1. 锁定scase语句中所有的channel

\2. 按照随机顺序检测scase中的channel是否ready

　　2.1 如果case可读，则读取channel中数据，解锁所有的channel，然后返回(case index)

　　2.2 如果case可写，则将数据写入channel，解锁所有的channel，然后返回(case index)

　　2.3 所有case都未ready 但是有 default ，则解锁所有的channel，然后返回（default index）

\3. 所有case都未ready，且没有default语句

　　 3.1 将当前协程加入到所有channel的等待队列

 　3.2 当将协程转入阻塞，等待被唤醒

\4. 唤醒后返回channel对应的case index
   解锁所有的channel，然后返回(case index)



我们简单总结一下 select 结构的执行过程与实现原理，首先在编译期间，Go 语言会对 select 语句进行优化，它会根据 select 中 case 的不同选择不同的优化路径：

1. 空的 select 语句会被转换成调用 [runtime.block](https://draveness.me/golang/tree/runtime.block) 直接挂起当前 Goroutine；

2. 如果 select 语句中只包含一个 case，编译器会将其转换成 if ch == nil { block }; n; 表达式；

3. - 首先判断操作的 Channel 是不是空的；
   - 然后执行 case 结构中的内容；

4. 如果 select 语句中只包含两个 case 并且其中一个是 default，那么会使用 [runtime.selectnbrecv](https://draveness.me/golang/tree/runtime.selectnbrecv) 和 [runtime.selectnbsend](https://draveness.me/golang/tree/runtime.selectnbsend) 非阻塞地执行收发操作；

5. 在默认情况下会通过 [runtime.selectgo](https://draveness.me/golang/tree/runtime.selectgo) 获取执行 case 的索引，并通过多个 if 语句执行对应 case 中的代码；

在编译器已经对 select 语句进行优化之后，Go 语言会在运行时执行编译期间展开的 [runtime.selectgo](https://draveness.me/golang/tree/runtime.selectgo) 函数，该函数会按照以下的流程执行：

1. 随机生成一个遍历的轮询顺序 pollOrder 并根据 Channel 地址生成锁定顺序 lockOrder；

2. 根据 pollOrder 遍历所有的 case 查看是否有可以立刻处理的 Channel；

3. 1. 如果存在，直接获取 case 对应的索引并返回；
   2. 如果不存在，创建 [runtime.sudog](https://draveness.me/golang/tree/runtime.sudog) 结构体，将当前 Goroutine 加入到所有相关 Channel 的收发队列，并调用 [runtime.gopark](https://draveness.me/golang/tree/runtime.gopark) 挂起当前 Goroutine 等待调度器的唤醒；

4. 当调度器唤醒当前 Goroutine 时，会再次按照 lockOrder 遍历所有的 case，从中查找需要被处理的 [runtime.sudog](https://draveness.me/golang/tree/runtime.sudog) 对应的索引；



**select 语句会对所有的 chan 上锁，这个上锁是提前按地址排好顺序的，请问为什么**

goroutine 也可以写相同的select ，如果乱序上锁，有非常大的记录会造成死锁



**channel 用法**



把channel用在数据流动的地方：

消息传递 

并发控制 

同步控制



通过 make 函数初始化 可以指定缓冲区长度（0 和不指定缓冲区长度相同）



channel可进行3种操作：

- 读
- 写
- 关闭



channel存在3种状态：

- nil，未初始化的状态，只进行了声明，或者手动赋值为nil
- active，正常的channel，可读或者可写
- closed，已关闭，千万不要误认为关闭**channel**后，**channel**的值是**nil**



注意：

- 发送到 nil 通道永远阻塞 
- 来自 nil 通道的接收永远阻塞
- 发送到关闭的通道发生 panic
- 来自关闭通道的接收立即返回零值



**用法：**

使用for range读channel  场景 ：发 push 异步起一个goroutine 消费 ，数据库里扫描出来的用户

使用**_,ok**判断**channel**是否关闭 ：

使用**select**处理多个**channel**    ： **consul** 实现的锁 ，使用 **select** 监听 关机**channel** 与 **leader** 切换**channel**

使用**channel**的声明控制读写权限 **:** 多个**gooutine** 中传递的时候使用

使用缓冲**channel**增强并发和异步 ：使用多个**goroutine** 消费 **kafka**

为操作加上超时、使用**time**实现**channel**无阻塞读写 **: time.after**

使用**close(ch)**关闭所有下游协程 **:**平滑关机，接受信号后关闭 **channel**

使用**chan struct{}**作为信号**channel**  

使用**channel**传递结构体的指针而非结构体 



**如何控制线程数**

声明 长度为 **n** 的**channel** 

然后**for** 循环开启 **goroutine** 启动前必须要投递成功， **goroutine** 执行完毕后必须再接收。这样就可以控制固定数量的**goroutine** 在执行了



a := **make\*(\*chan** bool, 3***)\***

**for** i := 0; i < 100; i++ ***{\***

​	a <- ***true\***

​	**go func\*() {\***

​		t.**Log\*(\***"--"***)\***

​		time.**Sleep\*(\***time.***Second)\***

​		<-a

​	***}()\***

***}\***







**原理：**

整体数据结构基础是一个环形buffer 、 和sendq \recvq （双向G链表）  还有表示缓冲区容量位置的两个index(sendx \ recvx )，还有一个状态字段表示 是否处于 close 状态

一般有缓冲区的情况是 

发送者发现缓冲区有空间则直接放到缓冲区，如果缓冲区已满则将自己加入 sendq 然后挂起自己

接受者如果发现缓冲区有待取出的内容，则直接取走，否则将自己加入 recvq 然后挂起自己



无缓冲区和特殊情况

如果消费者取数据发现 sendq 里不为空，则直接从 sendq 复制好了，性能优化

生产者也是同样的道理



**channel close 发生了什么**

更改 close 字段为 1 ，然后唤醒所有的 sendq \recvq 的G ，接受者后续直接返回零值，发送者直接 panic





**slice** 



slice（切片）是一种数组结构，相当于是一个动态的数组，可以按需自动增长和缩小。



使用切片的好处：

1. slice 参数传递相当于引用传递，而数组相当于值传递
2. 动态扩容很方便



Go 语言中包含三种初始化切片的方式：

1. 通过下标的方式获得数组或者切片的一部分； 
2. 使用字面量初始化新的切片；
3. 使用关键字 make 创建切片：



底层结构：

一个struct 有一个 data 指针，指向底层数组，还有长度和容量两个整形值

![GOLANG SLICE STRUCT](/Users/wfy/Documents/KnowledgeBase/picture/go/GOLANG SLICE STRUCT.png)





扩容：

1. 如果期望容量大于当前容量的两倍就会使用期望容量；
2. 如果当前切片的长度小于 1024 就会将容量翻倍；
3. 如果当前切片的长度大于 1024 就会每次增加 25% 的容量，直到新容量大于期望容量；



注意：

1、多个slice指向相同的底层数组时，修改其中一个slice，可能会影响其他slice的值；

2、slice作为参数传递时，比数组更为高效，因为slice的结构比较小；

3、slice在扩张时，可能会发生底层数组的变更及内存拷贝；

4、因为slice引用了数组，这可能导致数组空间不会被gc，当数组空间很大，而slice引用内容很少时尤为严重；



**空slice和nil的slice区别？能直接append吗？**



1. 首先JSON 标准库对 nil slice 和 空 slice 的处理是不一致. nil 值会报错，空 值正常处理
2. 空 slice 是底层结构初始化好，但是底层数组长度为 0 ， nil slice 是底层结构还未初始化，只声明的变量



都可以直接 append 安全的



**slice 和数组的区别**

数组是内置(build-in)类型,是一组同类型数据的集合，它是值类型，通过从0开始的下标索引访问元素值。在初始化后长度是固定的，无法修改其长度。当作为方法的参数传入时将复制一份数组而不是引用同一指针。数组的长度也是其类型的一部分，通过内置函数len(array)获取其长度。
 注意：和C中的数组相比，又是有一些不同的
 
 \1. Go中的数组是值类型，换句话说，如果你将一个数组赋值给另外一个数组，那么，实际上就是将整个数组拷贝一份
 \2. 如果Go中的数组作为函数的参数，那么实际传递的参数是一份数组的拷贝，而不是数组的指针。这个和C要区分开。因此，在Go中如果将数组作为函数的参数传递的话，那效率就肯定没有传递指针高了。
 \3. array的长度也是Type的一部分，这样就说明[10]int和[20]int是不一样的。array的结构用图示表示是这样的：



 ![len](/Users/wfy/Documents/KnowledgeBase/picture/go/len.png)

len表示数组的长度，后面的int储存的是实际数据 







**string 底层原理**

源码包src/runtime/string.go:stringStruct定义了string的数据结构：



type stringStruct struct {

 str unsafe.Pointer

 len int

}



所以string是byte的列表，通常但并不一定是UTF-8编码的文本。

另外，还提到了两点，非常重要：

- string可以为空（长度为0），但不会是nil； （var a string = nil 报错,基础类型都是这样的）
- string对象不可以修改。









**HashMap**

Go 语言使用拉链法来解决哈希碰撞的问题实现了哈希表，它的访问、写入和删除等操作都在编译期间转换成了运行时的函数或者方法。哈希在每一个桶中存储键对应哈希的前 8 位，当对哈希进行操作时，这些 tophash 就成为可以帮助哈希快速遍历桶中元素的缓存。



1. 装载因子已经超过 6.5；
2. 哈希使用了太多溢出桶 (count >= (1<<B & 15))；

Golang中map的底层实现是一个散列表，因此实现map的过程实际上就是实现散表的过程。在这个散列表中，主要出现的结构体有两个，一个叫hmap(a header for a go map)，一个叫bmap(a bucket for a Go map，通常叫其bucket)。hmap如下所示：

bmap 的结构分为两部分，第一个部分是一个长度为 8 类型为 uint8 的数组，每个索引存储该桶里每个key 的前8 位，第二部分是 key 和value 的紧凑内存



哈希表的每个桶都只能存储 8 个键值对，一旦当前哈希的某个桶超出 8 个，新的键值对就会存储到哈希的溢出桶中。随着键值对数量的增加，溢出桶的数量和哈希的装载因子也会逐渐升高，超过一定范围就会触发扩容，扩容会将桶的数量翻倍，元素再分配的过程也是在调用写操作时增量进行的，不会造成性能的瞬时巨大抖动。

![HASHMAP MAPACCESS](/Users/wfy/Documents/KnowledgeBase/picture/go/HASHMAP MAPACCESS.png)



**在 for range map 的过程中给 map 添加键值会发生什么？**

结果未知，一般情况下都不会全部成功添加的，只能添加一部分



**for k := range map {delete(map,k)} 会发生什么，原因是啥？**

正常执行，map 里的key 全部被删除，底层语法优化，直接执行 mapClear 函数将 map 的内容清空，实际上根本没执行循环逻辑,如果循环体内有其他语句也是可以正常全部删除的，不会漏删



**GO 协作式调度（伪抢占式调度）**

1. 用户主动让权、 调用 runtime.Gosched() 主动放弃，发起调度。（需要用户手动在代码里写，比较麻烦）
2. 调度主动让权、另一种主动放弃的方式是通过抢占标记的方式实现的。基本想法是在每个函数调用的序言 （函数调用的最前方）插入抢占检测指令，当检测到当前 Goroutine 被标记为被应该被抢占时， 则主动中断执行，让出执行权利。并将G 放入全局队列

具体执行流程：

1. 编译器会在调用函数前插入 [runtime.morestack](https://draveness.me/golang/tree/runtime.morestack)；
2. Go 语言运行时会在垃圾回收暂停程序、系统监控发现 Goroutine 运行超过 10ms 时发出抢占请求 StackPreempt；
3. 当发生函数调用时，会执行编译器插入的 [runtime.morestack](https://draveness.me/golang/tree/runtime.morestack)，它调用的 [runtime.newstack](https://draveness.me/golang/tree/runtime.newstack) 会检查 Goroutine 的 stackguard0 字段是否为 StackPreempt；
4. 如果 stackguard0 是 StackPreempt，就会触发抢占让出当前线程；



可以抢占的条件：

1. 运行时没有禁止抢占（m.locks == 0）
2. 运行时没有在执行内存分配（m.mallocing == 0）
3. 运行时没有关闭抢占机制（m.preemptoff == ""）
4. M 与 P 绑定且没有进入系统调用（p.status == _Prunning） 



什么时候会会给 stackguard0 设置抢占标记 stackPreempt 呢？ 一共有以下几种情况：

**注意： 真正的抢占是异步的通过系统监控进行的**

1.进入系统调用时（runtime.reentersyscall，注意这种情况是为了保证不会发生栈分裂， 真正的抢占是异步的通过系统监控进行的）

任何运行时不再持有锁的时候（m.locks == 0）并且

3.当垃圾回收器需要停止所有用户 Goroutine 时

4.Sysmon 线程监控到有 M 执行时间超过 10ms





**6.8.2** 抢占式调度

从上面提到的两种协作式调度逻辑我们可以看出，这种需要用户代码来主动配合的调度方式存在 一些致命的缺陷：一个没有主动放弃执行权、且不参与任何函数调用的函数，直到执行完毕之前， 是不会被抢占的。那么这种不会被抢占的函数会导致什么严重的问题呢？回答是，由于运行时无法 停止该用户代码，则当需要进行垃圾回收时，无法及时进行；



// 此程序在 Go 1.14 之前的版本不会输出 OK package main import ( "runtime" "time" ) func main() { runtime.GOMAXPROCS(1) go func() { 	for { 	} }() time.Sleep(time.Millisecond) println("OK") }



终于在 Go 1.10 后 [Clements, 2019]，Austin 进一步提出的解决方案，希望使用每个指令 与执行栈和寄存器的映射关系，通过记录足够多的信息，并通过异步线程来发送抢占信号的方式 来支持异步抢占式调度。



我们知道现代操作系统的调度器多为抢占式调度，其实现方式通过硬件中断来支持线程的切换， 进而能安全的保存运行上下文。在 Go 运行时实现抢占式调度同样也可以使用类似的方式，通过 向线程发送系统信号的方式来中断 M 的执行，进而达到抢占的目的。 但与操作系统的不同之处在于，由于运行时诸多机制的存在（例如垃圾回收器），还必须能够在 Goroutine 被停止时，保存充足的上下文信息。 这就给中断信号带来了麻烦，如果中断信号恰好发生在一些关键阶段（例如写屏障期间）， 则无法保证程序的正确性。这也就要求我们需要严格考虑触发异步抢占的时机。

异步抢占式调度的一种方式就与运行时系统监控有关，监控循环会将发生阻塞的 Goroutine 抢占， 解绑 P 与 M，从而让其他的线程能够获得 P 继续执行其他的 Goroutine。 这得益于 sysmon 中调用的 retake 方法。这个方法处理了两种抢占情况， 一是抢占阻塞在系统调用上的 P，二是抢占运行时间过长的 G。 其中抢占运行时间过长的 G 这一方式还会出现在垃圾回收需要进入 STW 时。



信号：抢占调用的整体逻辑：

1. M1 发送中断信号（signalM(mp, sigPreempt)）
2. M2 收到信号，操作系统中断其执行代码，并切换到信号处理函数（sighandler(signum, info, ctxt, gp)）
3. M2 修改执行的上下文，并恢复到修改后的位置（asyncPreempt）
4. 重新进入调度循环进而调度其他 Goroutine（preemptPark 和 gopreempt_m）

版本二：

1.M 注册一个 SIGURG 信号的处理函数：sighandler。

2.sysmon 线程检测到执行时间过长的 goroutine、GC stw 时，会向相应的 M（或者说线程，每个线程对应一个 M）发送 SIGURG 信号。

3.收到信号后，内核执行 sighandler 函数，通过 pushCall 插入 asyncPreempt 汇编函数调用。

4.回到当前 goroutine 执行 asyncPreempt 函数，通过 mcall 切到 g0(G0 是每次启动一个 M 都会第一个创建的 gourtine，G0 仅用于负责调度的 G，G0 不指向任何可执行的函数，每个 M 都会有一个自己的 G0。在调度或系统调用时会使用 G0 的栈空间，全局变量的 G0 是 M0 的 G0。) 栈执行 gopreempt_m。

5.将当前 goroutine 插入到全局可运行队列，M 则继续寻找其他 goroutine 来运行。

6.被抢占的 goroutine 再次调度过来执行时，会继续原来的执行流。





上述的异步抢占流程我们是通过系统监控来说明的，正如前面所提及的，异步抢占的本质是在为垃圾回收器服务， 由于我们还没有讨论过 Go 语言垃圾回收的具体细节，这里便不做过多展开，读者只需理解，在垃圾回收周期开始时， 垃圾回收器将通过上述异步抢占的逻辑，停止所有用户 Goroutine，进而转去执行垃圾回收。





**GO 里面那些行为会出现panic**

1.数组下标越界

2.访问为初始化的指针或空指针

3.往已经close 的 chan 发送数据

4.并发map

5.类型断言，没用ok 字符



**什么样的panic 不能 recover?**

底层通过 throw(“”) 语句抛出来的异常不能被recover, 举例：并发map 



**Recover 原理**

分析程序的崩溃和恢复过程比较棘手，代码不是特别容易理解。我们在本节的最后还是简单总结一下程序崩溃和恢复的过程：

1. 编译器会负责做转换关键字的工作；

2. 1. 将 panic 和 recover 分别转换成 [runtime.gopanic](https://draveness.me/golang/tree/runtime.gopanic) 和 [runtime.gorecover](https://draveness.me/golang/tree/runtime.gorecover)；
   2. 将 defer 转换成 [runtime.deferproc](https://draveness.me/golang/tree/runtime.deferproc) 函数；
   3. 在调用 defer 的函数末尾调用 [runtime.deferreturn](https://draveness.me/golang/tree/runtime.deferreturn) 函数；

3. 在运行过程中遇到 [runtime.gopanic](https://draveness.me/golang/tree/runtime.gopanic) 方法时，会从 Goroutine 的链表依次取出 [runtime._defer](https://draveness.me/golang/tree/runtime._defer) 结构体并执行；

4. 如果调用延迟执行函数时遇到了 [runtime.gorecover](https://draveness.me/golang/tree/runtime.gorecover) 就会将 _panic.recovered 标记成 true 并返回 panic 的参数；

5. 1. 在这次调用结束之后，[runtime.gopanic](https://draveness.me/golang/tree/runtime.gopanic) 会从 [runtime._defer](https://draveness.me/golang/tree/runtime._defer) 结构体中取出程序计数器 pc 和栈指针 sp 并调用 [runtime.recovery](https://draveness.me/golang/tree/runtime.recovery) 函数进行恢复程序；
   2. [runtime.recovery](https://draveness.me/golang/tree/runtime.recovery) 会根据传入的 pc 和 sp 跳转回 [runtime.deferproc](https://draveness.me/golang/tree/runtime.deferproc)；
   3. 编译器自动生成的代码会发现 [runtime.deferproc](https://draveness.me/golang/tree/runtime.deferproc) 的返回值不为 0，这时会跳回 [runtime.deferreturn](https://draveness.me/golang/tree/runtime.deferreturn) 并恢复到正常的执行流程；

6. 如果没有遇到 [runtime.gorecover](https://draveness.me/golang/tree/runtime.gorecover) 就会依次遍历所有的 [runtime._defer](https://draveness.me/golang/tree/runtime._defer)，并在最后调用 [runtime.fatalpanic](https://draveness.me/golang/tree/runtime.fatalpanic) 中止程序、打印 panic 的参数并返回错误码 2；



**栈内存管理**

v1.3 之前分段栈（最小栈为 8KB ），之后使用连续栈 (最小栈为 2k)



分段栈：在栈空间不足的时候会开辟一块新的栈空间，所有的栈用指针连接

问题：

1. 如果当前 Goroutine 的栈几乎充满，那么任意的函数调用都会触发栈扩容，当函数返回后又会触发栈的收缩，如果在一个循环中调用函数，栈的分配和释放就会造成巨大的额外开销，这被称为热分裂问题（Hot split）；
2. 一旦 Goroutine 使用的内存越过了分段栈的扩缩容阈值，运行时会触发栈的扩容和缩容，带来额外的工作量；



连续栈：栈空间不足的时候，分配一块更大的内存，将原来的栈上的内容直接复制过去

​	因为需要拷贝变量和调整指针，连续栈增加了栈扩容时的额外开销，但是通过合理栈缩容机制就能避免热分裂带来的性能问题[10](https://draveness.me/golang/docs/part3-runtime/ch07-memory/golang-stack-management/#fn:10)，在 GC 期间如果 Goroutine 使用了栈内存的四分之一，那就将其内存减少一半，这样在栈内存几乎充满时也只会扩容一次，不会因为函数调用频繁扩缩容。