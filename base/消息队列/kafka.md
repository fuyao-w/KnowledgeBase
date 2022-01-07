---
typora-root-url: ../../picture
---

**kafka**



**什么是消息队列：**

消息队列通俗解释就是在不同进程之间传递消息的中间件，最基础逻辑来讲一个消息队列里有两个角色 。

1.生产者：负责在上游根据事件投递消息

2.消费者：负责在下游消费特定的消息



在当前的消息队列实现里面，其实还有一个 broker 角色，他是生产消费的代理。负责接受生产者的信息进行持久化，并且将消息推送到消费端。



消息队列在业务中一般有三种应用场景  

1.服务间进行消息传递，松耦合化。让服务的职责更单一

2.异步通知：我们的真人认证会接收AI 部门的的异步kafka 处理认证结果

3.在大流量场景下抵御流量洪峰 ：直播间场景送礼小礼物量非常大，用异步话来处理。



目前市面上的消息队列有 ，kafka （高吞吐量） 、rocket MQ （也挺高、还有定时任务）、rabbitMq 低吞吐量， 去哪网的QMQ 等等 ，plusor 存算分离，扩展性最好。

![总结一下](/queue/总结一下.jpg)

![kafka的缺点](/queue/kafka的优缺点.png)

![Kafka高吞吐的原因](/queue/Kafka高吞吐的原因.png)![kafka的缺点](/queue/kafka的缺点.png)**kafka isr 、hight wartermark 是啥**

ISR（in-sync replica） 就是 Kafka 为某个分区维护的一组同步集合，即每个分区都有自己的一个 ISR 集合，处于 ISR 集合中的副本，意味着 follower 副本与 leader 副本保持一定的同步状态，只有处于 ISR 集合中的副本才有资格被选举为 leader。一条 Kafka 消息，只有被 ISR 中的副本都接收到，才被视为“已同步”状态。这跟 zk 的同步机制不一样，zk 只需要超过半数节点写入，就可被视为已写入成功。



1、AR（Assigned Repllicas）一个partition的所有副本（就是replica，不区分leader或follower）



2、ISR（In-Sync Replicas）能够和 leader 保持同步的 follower + leader本身 组成的集合。



3、OSR（Out-Sync Relipcas）不能和 leader 保持同步的 follower 集合



需要先明确几个概念：

1、LEO（last end offset）：

当前**replica**存的最大的**offset**的下一个值

2、HW（high watermark）：

小于 **HW** 值的offset所对应的消息被认为是“已提交”或“已备份”的消息，才对消费者可见。水桶原理

**
 什么是主题、分区
 在 Kafka 中，Topic 是一个存储消息的逻辑概念，可以认为是一个消息集合。每条消息发送到 Kafka 集群的消息都有一个类别。物理上来说，不同的 Topic 的消息是分开存储的，每个 Topic 可以有多个生产者向它发送消息，也可以有多个消费者去消费其中的消息。**

每个 Topic 可以划分多个分区（每个 Topic 至少有一个分区），同一 Topic 下的不同分区包含的消息是不同的。每个消息在被添加到分区时，都会被分配一个 offset，它是消息在此分区中的唯一编号，Kafka 通过 offset 保证消息在分区内的顺序，offset 的顺序不跨分区，即 Kafka 只保证在同一个分区内的消息是有序的。



**消费语义**

最多一次：自动提交间隔缩小、先提交再处理

至少一次 ：自动提交间隔扩大，先处理再提交

正好一次： 业务自己保证通过唯一的序号自己做觅等。



**kafka 生产者如何保证觅等：**

生产者 生产消息会产生一个连续的序号 ，投递失败会重试。broker 处理的时候根据 序号保证 同一个消息只写入一次。消费端觅等只能由业务自己来做。

**kafka 消费者如何保证觅等：**

业务自己保证通过唯一的序号自己做觅等。

如果使用 stream ，系统内部也可以保证正好一次。







**kafka的一致性**

kafka的副本采用的是异步拉取机制，也没有开放follower的读能力，其读操作仍然由该分区的Leader提供，因而一致性保证机制相对算简单。

kafka的一致性指分区的leader发生切换前后，读取的消息是一致的。这个通过HighWatermark实现，高水位取分区的ISR副本中最小的LEO，消费者只能读取到HW的上一条记录，这里LEO表示LogEndOffset，指该副本中当前日志的下一条。如图所示，只能读到[消息Message](https://www.zhihu.com/search?q=消息Message&search_source=Entity&hybrid_search_source=Entity&hybrid_search_extra={"sourceType"%3A"article"%2C"sourceId"%3A107705346})2。





**Kafka怎么保证数据的一致性和可靠性？**

**1**、可靠性的保证？

下面通过从topic的分区副本、producer发送到broker、leader选举三个方面来阐述kafka的可靠性。



<1>、Topic的分区副本：

其实在kafka-0.8.0之前的版本是还没有副本这个概念的，在之后版本引入了副本这个架构，每个分区设置几个副本，可以在设置主题的时候可以通过replication-factor参数来设置，也可以在broker级别中设置defalut.replication-factor来指定，一般我们都设置为3；

三个副本中有一个副本是leader，两个副本是follower，leader负责消息的读写，follower负责定期从leader中复制最新的消息，保证follower和leader的消息一致性，当leader宕机后，会从follower中选举出新的leader负责读写消息，通过分区副本的架构，虽然引入了数据冗余，但是保证了kafka的高可靠。

Kafka的分区多副本是Kafka可靠性的核心保证，把消息写入到多个副本可以使Kafka在崩溃时保证消息的持久性及可靠性。



<2>、Producer发送消息到broker：

topic的每个分区内的事件都是有序的，但是各个分区间的事件不是有序的，producer发送消息到broker时通过acks参数来确认消息是否发送成功,request.required.acks参数有三个值来代表不同的含义;

acks=0：表示只要producer通过网络传输将消息发送给broker，那么就会认为消息已经成功写入Kafka；但是如果网卡故障或者发送的对象不能序列化就会错误；

acks=1：表示发送消息的消息leader已经接收并写入到分区数据文件中，就会返回成功或者错误的响应，如果这时候leader发生选举，生产者会再次发送消息直到新的leader接收并写入分区文件；但是这种方式还是可能发生数据丢失，当follower还没来得及从leader中复制最新的消息，leader就宕机了，那么这时候就会造成数据丢失；

acks=-1：代表leader和follower都已经成功写入消息是才会返回确认的响应，但是这种方式效率最低，因为要等到当前消息已经被leader和follower都写入返回响应才能继续下条消息的发送；

所以根据不用的业务场景，设置不同的acks值，当然producer发送消息有两种方式：同步和异步，异步的方式虽然能增加消息发送的性能，但是会增加数据丢失风险，所以为了保证数据的可靠性，需要将发送方式设置为同步(sync)。



<3>、Leader选举

在每个分区的leader都会维护一个ISR列表，ISR里面就是follower在broker的编号，只有跟得上leader的follower副本才能加入到ISR列表，只有这个列表里面的follower才能被选举为leader，所以在leader挂了的时候，并且unclean.leader.election.enable=false(关闭不完全的leader选举)的情况下，会从ISR列表中选取第一个follower作为新的leader，来保证消息的高可靠性。



综上所述，要保证kafka消息的可靠性，至少需要配置一下参数：

topic级别：replication-factor>=3；

producer级别：acks=-1；同时发送模式设置producer.type=sync；

broker级别：关闭不完全的leader选举，即unclean.leader.election.enable=false;



2、数据一致性？

这里说的一致性指的是不管是老的leader还是新的leader，consumer都能读到一样的数据。



假设分区副本为3，副本0位leader，副本1和2位follower，在ISR列表里面副本0已经写入了message4，但是consumer只能读取message2，这是因为所有副本都同步了message2，只有High water mark以上的message才能被consumer读取，而High water mark取决于ISR列表里偏移量最小的分区，对应上图中的副本2；

所以在message还没有被follower同步完成时会被认为是"不安全的"，如果consumer读取了副本0中的message4，这时候leader挂了，选举了副本1为新的leader，别的消费者去消费的时候就没有message4，就会造成不同的consumer消费的数据不一致，破坏了数据的一致性。

在引入了High water mark机制后，会导致broker之间的消息复制因为某些原因变慢，消息到达消费者的时间也会延长(需要等消息复制完了才能消费)，延迟的时间可以通过参数来设置：replica.lag.time.max.ms(它指定了副本在复制消息时可被允许的最大延迟时间)



**kafka 优点**

1. 吞吐量大，在高并发量场景下也能满足实时性
2. 基于**partation** 容易扩展消费能力
3. 数据是持久化的，允许重新消费队列
4. 一致性和可用性皆有 （可以是个 **ca** 系统，Kafka设计是运行在一个数据中心，网络分区问题基本不会发生，所以理论上是CA系统）

白话理解**CAP**与**Kafka** ：[**https://blog.csdn.net/weixin_43469680/article/details/115204832#kafka_AC_APCP_49**](https://blog.csdn.net/weixin_43469680/article/details/115204832#kafka_AC_APCP_49)



**kafka 的缺点？**

**1.**错误重试完全需要业务方自己做

没有类似 **rocket mq** 里的重试队列和死信队列，如果消费的时候处理遇到失败，只能由业务方自己实现



**2.**使用中最多就是至少一次语义

理论上存在三种语义，但是基于实现 最多，提高**commit** 频率和先提交先处理消息，也没办法保证在重启后不会重复，所以想要正好一次消费一定要业务方自己做的。



**3.topic** 或者 **partation** 超过一定数量导致性能下降



**kafka 如何变成 ap 、cp 、ca 系统？**

由于 **kafak** 本质是一个数据中心，集群里的机器存储在同一个内网当中所以几乎没有 **P** 发生，所以一般是 **ca** 系统

如果真的发证网络分区的情况，我们可以从两方面去调整让其变成 **cp** 或者 **ap** 系统

**1.**调整**commit** 设置为调用成功即提交或者**leader** 写完即提交，

**2.**打开不完全的选举，（**osr** 也可以参与选举）

这样系统在故障的时候尽最大可能的保持在可用的状态，为 **ap** 系统





**1.**调整 **commit** 设置为必须有多少**follower** 写成功才可以

**2.**关闭不完全选举（**isr** 才可以参与选举）

**3.**每个 **leader** 至少两个 副本

通过副本机制，达到了可靠性同时保证了一致性，为 **cp** 系统







**kafka 如何保证至多、至少、正好一次语义**

至多一次：提高自动 **commit** 的频率，先提交再处理消息

至少一次：放宽自动 **commit** 的频率，先处理消息然后再提交

正好一次： 生产者自己会维护一个递增序号，系统内重试的时候可以保证正好一次。消费者只能自己在消息里附带唯一序号做去重。



实际使用中肯定是至少一次的，需要业务端自己做重试 





**kafka延迟队列、重试队列、死信队列**

[**https://blog.csdn.net/jy02268879/article/details/106014372**](https://blog.csdn.net/jy02268879/article/details/106014372)

延迟队列：

先将延迟消息发送到内部主题，然后启动一个中间进程，从内部拉取消息并推送到客户端的真实主题中

内部主题数据结构：单层时间轮，按秒为单位，一秒存储一个文件。 支持两小时延迟总共会有 **7200** 个文件。但是在操作**IO** 的时候，只需要以滑动窗口的形式访问固定个数的文件句柄。整体上提高时间精度。

同时还可以使用后台任务定时清理已经过期的文件。



重试队列、私信队列：消费者端触发。按时间间隔（**5**、**10**、**15 s**）分为几个内部重试主题，逐级递减，最后一次投入私信队列的单独主题。



延迟队列 **rocksDB** 实现 ：[**https://segmentfault.com/a/1190000022417868**](https://segmentfault.com/a/1190000022417868)



**kafka 可以实现事物吗，如何实现？**



**可以，在** **stream** **里（上游消费直接投递到下游）** **通过事物协调者进行二阶段提交。根据事物****ID** **维护原子性。**