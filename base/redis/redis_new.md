**redis** 

### 基础数据类型

string\ list\ set \zset\ hash\ stream\ hyperloglog 、geo、channel



### 底层数据结构



string: 1. int ( 1000 以内共享 ) ，2.embstr 精简字符串可以和 obj 拼装到一起，set key 只分配一次 内存就可以、3. sdsstr (数据，长度，剩余空闲长度) 二进制安全，兼容C 函数



Set : intset、 dict

Hash : dict、ziplist

Zset ：zip list、skip list

List ：quickList (zipList 为节点组成的linkList ，可以单个一个 zip list ,极端情况 zip list 只有一个值)



zip list 结构:是一块紧凑的内存，其中所有的kv 都挨在一起，每个节点都保存了前一个节点的长度，最开始有总长度等原信息，可以很容易的倒叙遍历

### **Skip list 的数据结构**

skip List 的基础是一个有序链表，在此基础上，加上一个层级结构，每层也是有序链表，但是越往上节点越少，当从上层查询的时候，可以根据二分法的性质不断向下查询，直到找到具体的节点。



(1) 由很多层结构组成



(2) 每一层都是一个有序的链表



(3) 最底层(Level 1)的链表包含所有元素



(4) 如果一个元素出现在 Level i 的链表中，则它在 Level i 之下的链表也都会出现。



(5) 每个节点包含三个指针，两个指向同一链表中的前后元素，一个指向下面一层的元素。



### **跳表的优缺点**



优点：

1.它们不是很占用内存。 基本上取决于你。 更改有关节点具有给定级别数的概率的参数将比 btree 占用更少的内存。

\2. 一个有序集合往往是很多 ZRANGE 或 ZREVRANGE 操作的目标，即作为链表遍历跳表。 通过此操作，跳过列表的缓存位置至少与其他类型的平衡树一样好。

3.它们更易于实现、调试等。 例如，由于跳过列表的简单性，我收到了一个补丁（已经在 Redis master 中），其中包含在 O(log(N)) 中实现 ZRANK 的增强跳过列表。 它几乎不需要对代码进行更改。



### skiplist与平衡树、哈希表的比较

skiplist和各种平衡树（如AVL、红黑树等）的元素是有序排列的，而哈希表不是有序的。因此，在哈希表上只能做单个key的查找，不适宜做范围查找。所谓范围查找，指的是查找那些大小在指定的两个值之间的所有节点。



在做范围查找的时候，平衡树比skiplist操作要复杂。在平衡树上，我们找到指定范围的小值之后，还需要以中序遍历的顺序继续寻找其它不超过大值的节点。如果不对平衡树进行一定的改造，这里的中序遍历并不容易实现。而在skiplist上进行范围查找就非常简单，只需要在找到小值之后，对第1层链表进行若干步的遍历就可以实现。



平衡树的插入和删除操作可能引发子树的调整，逻辑复杂，而skiplist的插入和删除只需要修改相邻节点的指针，操作简单又快速。



从内存占用上来说，skiplist比平衡树更灵活一些。一般来说，平衡树每个节点包含2个指针（分别指向左右子树），而skiplist每个节点包含的指针数目平均为1/(1-p)，具体取决于参数p的大小。如果像Redis里的实现一样，取p=1/4，那么平均每个节点包含1.33个指针，比平衡树更有优势。



查找单个key，skiplist和平衡树的时间复杂度都为O(log n)，大体相当；而哈希表在保持较低的哈希值冲突概率的前提下，查找时间复杂度接近O(1)，性能更高一些。所以我们平常使用的各种Map或dictionary结构，大都是基于哈希表实现的。



从算法实现难度上来比较，skiplist比平衡树要简单得多。





缺点：

1. 实现上跳表还是相对树结构占用空间更多的，空间换时间



### **redis的zset为什么用跳表而不用红黑树？**

1、跳表的实现更加简单，不用旋转节点，相对效率更高

2、跳表在范围查询的时候的效率是高于红黑树的，因为跳表是从上层往下层查找的，上层的区域范围更广，可以快速定位到查询的范围

3、平衡树的插入和删除操作可能引发子树的调整、逻辑复杂，而跳表只需要维护相邻节点即可

4、查找单个key，跳表和平衡树时间复杂度都是O(logN)













### **Rds 为什么这么快：**

1.基于内存操作

2.精心设计的高效内存结构，不仅节约内存，而且会尽量减少内存分配（例如 int 类型的缓存、embstr）

3.命令处理由单线程模型，没有锁冲突降低性能

4.高效的 IO 模型，统一封装的不同平台的 IO 处理函数，由事件循环执行

5.一切逻辑以命令处理为主、过期、rehash 等操作都设计成渐进式的不会阻塞住逻辑，(fork 进程后不允许 rehash

6.自己实现 vm 机制，使用虚拟内存更高效 （在开启 VM 功能后，Rds 可以以非阻塞的方式向磁盘交换对象，保障执行速度）（https://www.cnblogs.com/xuegang/archive/2011/11/16/2250920.html	）



### **驱逐策略(rds 内存满了之后会发生什么？)：**

直接拒绝、all 随机 、expire 随机、all lru 、all lfu  expire lru 、expire lfu



### **持久化策略：**

rdb \aof \混合 aof

rdb: 可以通过 save bgsave 、或者 配置文件 100 s 内更新100 次这样触发

通过 fork 命令，创建新进程（保存了快照）,生成 rdb 文件，（ 此时不可以进行 后台 rehash 操作。）

重启时，读取 该文件 恢复数据 



aof : 开启后（配置文件 appendonly yes ）记录每个执行的命令 到 aof buffer 定时 sync 刷新到 磁盘， 刷新策略（每次 sync、every sec、由操作系统决定）

aof 重写：有效的缩小 aof 文件大小，原理：fork 进程（主进程 同时开启3对管道，

），直接将每个key 通过写命令的方式写到 file 里 。同时主进程将即时的命令 追加到 重写 buf 里，然后定时通过管道同步到子进程，最后 子进程结束后通过管道向主进程发送 停止命令， 结束进程。主进程在事件循环将 buf 里剩下一点内容写到文件，完成切换



混合 aof :（配置文件 aof-use-rdb-preamble ） rdb 的内存紧凑优势+ aof 即时写入的优势。 重写的时候文件先写入 rdb格式，再追加 aof 格式。启动时，按格式读取即可。





### 命令过期策略：

1.惰性过期(访问的时候再过期)

2.时间循环事件 beforeSleep() 会执行总共 1ms 的扫描 （有一个最大的扫多少 index 的数量）批量过期



rehash 策略：



负载因子 大于 1 。 扩容 2 倍扩容：  缩容：90 %空间空闲，缩小到大于当前key 数量的最小 2 的n 次幂

步骤：

redis .server struct 下 ht 列表，分别是 当前的 库 和扩容的库、然后 rehashidx 由 -1 -> 0



扩容后 的 dict 保存在 ht[1] 

1. 处理命令 (增删查改)： 此时正在 rehash 将该节点直接 rehash 到 新 table 上，查询的时候是两个都会查找
2. 渐进式 rehash datebaseCron 里面 每次根据 rehashidx++ 最多执行 100 个节点总计 1ms 的 rehash ，直到结束，切换 ht[0] 和 ht[1]



1. 最后 rehashidx= -1

结束





### **事务：**



Rds multi \exec 只能保证原子性、 通过 watch 命令监控要修改的键



server.watch dict key: 键 value 	监听 该 key 的client 结构体



每次执行逻辑的时候，会遍历 watch key 列表，通知其他 client 失败。







Client 里保存一个mstate 队列 缓存 mutli 后命令，然后 执行 exec 的是时候，一次全部遍历执行，如果执行的 waitch key 。则如果在其 cient 设置 flag |=dirty



其他client 执行的时候就知道了。 



放弃就是 discard 、 unwatch 在exec 自动执行





lua 脚本：

Eval \eval sha 一般通过 script load 生成sha



正常执行的命令不能带有随机性（保证复制的一致性），要想使用必须 在随机命令前执行 redis.replicatecommands() 命令（内部替换成 multi exec ）spop 就会变成 srem 了，（复制和 aof 处理是一样的，有特殊的转化）



redis.call（返回错误） redis.pcall（不返回错误，而是返回一个 error table 可以更灵活的修改）



sccpit kill ， lua 脚本每行都有一个 hook ，所以可以在执行的时候停止，但是前提是不能处理过写命令，否则只能执行 shutdown nosve 了



使用注意：别写死循环，别写长时间执行命令



### **主从复制原理：**

2.8 之前： slave 发送 sync 命令， master fork 进程生成 rdb 文件，同时将即时命令存储到缓冲区。 然后先发送 rdb 文件，然后发送缓冲区内容。如果中断则重新执行



2.8 之后： slave 发送psync 命令，master 第一次连接还是 生成rdb 文件，存复制缓冲区 ，然后同步。同时维护一个当前生成命令的序号。 出现网络故障中断后，master 会将即时命令先写入缓冲区，然后等到slave 来请求的时候会携带一个序号，master 判断序号是否在缓冲区里，如果在就将序号后面的数据同步给 slave 。节省大量的链接消耗。



### **Slave 如何处理过期数据**

实现复制功能不能依靠主从节点的时钟，Redis使用了以下三个方式来处理：

1. 副本不会主动删除过期键，而是等主节点过期时生成 DEL 命令同步至从节点进行删除。
2. 但主节点过期删除不及时，可能会使从节点上存在逻辑上已经过期的键。为了处理该问题，副本采用它自己的 逻辑时钟 来判断读取时键是否应当过期，过期则返回不存在（即使数据仍然在内存中，等着主节点的 DEL 命令）
3. 在Lua脚本运行时，服务器中的时间是 冻结 的，防止键在脚本运行的过程中过期。这是为了保持副本上执行的脚本能具有相同的效果。（注：不同机器，性能不一样，脚本执行时长也不同）



### **主从不一致原因**

1.复制延迟

2.过期延迟

3.配置不同





### **哨兵：**

一般是 三个哨兵带 几个主从节点，

哨兵启动之后会先与配置文件中监控的Master建立两条连接，一条称为命令连接，另一条称为消息连接。哨兵就是通过如上两条连接发现其他哨兵和Redis Slave服务器，并且与每个Redis Slave也建立同样的两条连接。

命令链接用于监控健康状态。



哨兵通过配置文件 和主节点建立连接发现主节点， 通过直接和 主节点建立链接 发现 slave 和 其他哨兵，哨兵间的信息发送通过  sentinel:__hello 频道发布。

通过心跳监控 节点，先单个 哨兵判断 主观下线，然后同步进群内其他哨兵，超过半数就可以判定客观下线，发起投票半数选举出 leader 执行 multi , slave no one ,rewrite config clear clients , exec 然后，同步其他 salve 节点， slaveof 新 主节点，中间肯定是有损的





1）主从切换完成之后，客户端和其他哨兵如何知道现在提供服务的Redis Master是哪一个呢？

回答：可以通过subscribe __sentinel__:hello频道，知道当前提供服务的Master的IP和Port。



2）执行切换的哨兵发生了故障，切换操作是否会由其他哨兵继续完成呢？

回答：执行切换的哨兵发生故障后，剩余哨兵会重新选主，并且重新开始执行切换流程。



3）故障Master恢复之后，会继续作为Master提供服务还是会作为Slave提供服务？

回答：Redis中主从切换完成之后，当故障Master恢复之后，会作为新Master的一个Slave来提供服务。



### **集群:** 

https://cloud.tencent.com/document/product/239/18336（tengx）

slots 概念 16384



规格，一般 每个主 + 一个slave ， 访问集群命令通过 move 重定向 将其指定到 正确的 slot 机器上， 一般情况下 slave 不会执行读命令， 可以通过配置打开。



分槽（slot）：即如何决定某条数据应该由哪个节点提供服务；

通过hash 算法确定属于哪个slot ,



2）端如何向集群发起请求（客户端并不知道某个数据应该由哪个节点提供服务，并且如果扩容或者节点发生故障后，不应该影响客户端的访问）？

客户端可以连接任意接节点， 向集群中发起请求 。接受到的节点计算是不是本节点的slot 不是本节点 通过 -move 命令 重定向

3）某个节点发生故障之后，该节点服务的数据该如何处理？

集群之间通过心跳判定 客观下线。然后执行 slave of 主从切换 ，也可以 手动执行 fial over 命令

4）扩容，即向集群中添加新节点该如何操作？

增量迁移 ：本节点 没有 +ask 新节点 ，全量迁移后在同步集群里其他的节点，刷新纪录

5）同一条命令需要处理的key分布在不同的节点中（如Redis中集合取并集、交集的相关命令），如何操作？

正常会报错，需要通过 hash {tag} 将需要操作的命令set 到一个slot 里面



### **集群高可用**

**1.主从切换： master 通过心跳包（包含该master 负责的slot 和其他集群内master 的信息）判活。判定定一个master 已经死亡的条件为：1.首先要其他master 判定主观下线，2.然后通过心跳判定客观下线。** 

**真正的主从切换只能由slave 发起，并且只能由master 投票（半数选举），通过后slave 先将自己声明为master, 将主从关系清除。将M 负责的slot 声明到 slave 中，发送pong 包同步其他节点更新状态。**

**2.还有一种手动切换 faliover 命令，集群有停顿，但是数据不丢失。**



**3.集群副本漂移 ： 一个主节点挂 多个从节点，其他主节点 挂了，可以从本主节点 的从节点迁移到其他主节点下，然后通过心跳包同步集群里的其他节点。（步骤，先和原master 断开，然后将主节点改为新的master ，然后让主节点添加自己为从节点，自己的主节点同部位新的主节点）**



**数据迁移： 增量迁移 ：本节点 没有 ask 新节点** 


 

###  unlink 命令

unlink命令 是4.0 后提供的，目的是为了解决 del 大key同步阻塞的问题， 原理是 del 的时候会将对象提交给异步线程去删除， 但是会评估对象的大小，像 string 或者size < 64 的其他数据结构 还是同步删除，因为异步也需要成本的 有锁+wait notify 。 所以unlink 不是万能的，工作中还需要自己注意 string 类型的大key 问题。

（可以开启 lazy-free 配置项）

rename 命令 原理是先删除 在set ，也可能造成删大key 阻塞 ，需要注意





### **Redis 4.0 新特性**

Lazyfree



配置：lazyfree-lazy-server-del yes/no



新增命令 swapdb memory



LFU机制与hotkey



Redis 4.0新增了allkey-lfu和volatile-lfu两种数据逐出策略，同时还可以通过object命令来获取某个key的访问频度。



### **Redis 5.0 新特性**

- 新的数据类型：流数据（Stream）。详细说明请参见[Redis Streams](https://redis.io/topics/streams-intro)。



- RDB中增加LFU和LRU信息。



- 新增有序集合（Sorted Set）命令[ZPOPMIN](https://redis.io/commands/zpopmin)、[ZPOPMAX](https://redis.io/commands/zpopmax)、[BZPOPMIN](https://redis.io/commands/bzpopmin)和[BZPOPMAX](https://redis.io/commands/bzpopmax)。



- 弃用slave术语（需要API向后兼容的情况例外）。



- 新增动态HZ（Dynamic HZ）以平衡空闲CPU使用率和响应性。











### **redis 6.0 新特性**

**1.**多线程**IO**

Redis 6引入多线程IO，但多线程部分只是用来处理网络数据的读写和协议解析，执行命令仍然是单线程。之所以这么设计是不想因为多线程而变得复杂，需要去控制 key、lua、事务，LPUSH/LPOP 等等的并发问题。

**RESP3**协议

RESP（Redis Serialization Protocol）是 Redis 服务端与客户端之间通信的协议。Redis 5 使用的是 RESP2，而 Redis 6 开始在兼容 RESP2 的基础上，开始支持 RESP3。

推出RESP3的目的：一是因为希望能为客户端提供更多的语义化响应，以开发使用旧协议难以实现的功能；另一个原因是实现 Client-side-caching（客户端缓存）功能。

**2.**重新设计了客户端缓存功能

实现了Client-side-caching（客户端缓存）功能。放弃了caching slot，而只使用key names。

[*Redis server-assisted client side caching*](https://link.zhihu.com/?target=https%3A//redis.io/topics/client-side-caching)

**4.**支持**SSL**

连接支持SSL，更加安全。

**5.ACL**权限控制

\1. 支持对客户端的权限控制，实现对不同的key授予不同的操作权限。

\2. 有一个新的ACL日志命令，允许查看所有违反ACL的客户机、访问不应该访问的命令、访问不应该访问的密钥，或者验证尝试失败。这对于调试ACL问题非常有用。

**6.**提升了**RDB**日志加载速度

根据文件的实际组成（较大或较小的值），可以预期20/30%的改进。当有很多客户机连接时，信息也更快了，这是一个老问题，现在终于解决了。



### **redis 序列化方式：**

一般 protobuf or json ，其他就是简单字符串



### **一致性HASH**

https://www.jianshu.com/p/528ce5cd7e8f



### **Rds 数据倾斜的原因以及解决办法**

https://blog.csdn.net/weixin_45701550/article/details/115832552



### **如何为rds 新增数据类型**

通过module 模块实现钩子方法，然后通过C 的链接将其加载

https://www.modb.pro/db/52884
 

**腾讯云****RDS** **单机版本架构****
 https://cloud.tencent.com/document/product/239/36151**