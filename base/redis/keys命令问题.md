# KEYS 模式

**自1.0.0起可用。**

**时间复杂度：** O（N），其中N是数据库中的密钥数，假设数据库中的密钥名称和给定模式的长度有限。

返回所有匹配的键`pattern`。

虽然此操作的时间复杂度为O（N），但恒定时间相当低。例如，在入门级笔记本电脑上运行的Redis可以在40毫秒内扫描100万个密钥数据库。

**警告**：将[KEYS](https://redis.io/commands/keys)视为仅应在生产环境中使用的命令。在针对大型数据库执行时可能会破坏性能。此命令用于调试和特殊操作，例如更改键空间布局。不要在常规应用程序代码中使用[KEYS](https://redis.io/commands/keys)。如果您正在寻找一种在密钥空间的子集中查找密钥的方法，请考虑使用[SCAN](https://redis.io/commands/scan)或[sets](https://redis.io/topics/data-types#sets)。

支持的 glob-style 模式：

- `h?llo` matches `hello`, `hallo` and `hxllo`
- `h*llo` matches `hllo` and `heeeello`
- `h[ae]llo` matches `hello` and `hallo,` but not `hillo`
- `h[^e]llo` matches `hallo`, `hbllo`, ... but not `hello`
- `h[a-b]llo` matches `hallo` and `hbllo`

`\`如果要逐字匹配，请使用以转义特殊字符。



## 返回值

[数组回复](https://redis.io/topics/protocol#array-reply)：匹配的密钥列表`pattern`。



## 例子

```
redis> MSET firstname Jack lastname Stuntman age 35
"OK"
redis> KEYS *name*
1) "firstname"
2) "lastname"
redis> KEYS a??
1) "age"
redis> KEYS *
1) "firstname"
2) "age"
3) "lastname"
redis> 
```



# SCAN 游标 [MATCH模式] [COUNT计数]

**自2.8.0起可用。**

**时间复杂度：**每次通话都是O（1）。O（N）用于完整迭代，包括足够的命令调用以使光标返回0. N是集合内元素的数量。

使用[SCAN](https://redis.io/commands/scan)命令和密切相关的命令[SSCAN](https://redis.io/commands/sscan)，[HSCAN](https://redis.io/commands/hscan)和[ZSCAN](https://redis.io/commands/zscan)以便递增地迭代元素集合。

- [SCAN](https://redis.io/commands/scan)迭代当前所选Redis数据库中的密钥集。
- [SSCAN](https://redis.io/commands/sscan)迭代集合类型的元素。
- [HSCAN](https://redis.io/commands/hscan)迭代Hash类型及其相关值的字段。
- [ZSCAN](https://redis.io/commands/zscan)迭代排序集类型的元素及其相关分数。

由于这些命令允许增量迭代，每次调用只返回少量元素，因此可以在生产中使用它们，而不会像[KEYS](https://redis.io/commands/keys)或[SMEMBERS](https://redis.io/commands/smembers)这样的命令的缺点，这些命令可能会在被调用时长时间（甚至几秒钟）阻塞服务器钥匙或元素的大集合。

但是，虽然像[SMEMBERS](https://redis.io/commands/smembers)这样的阻塞命令能够在给定时刻提供属于Set的所有元素，但SCAN系列命令仅对返回的元素提供有限保证，因为我们递增迭代的集合可以在迭代过程中更改。

请注意，[SCAN](https://redis.io/commands/scan)，[SSCAN](https://redis.io/commands/sscan)，[HSCAN](https://redis.io/commands/hscan)和[ZSCAN的](https://redis.io/commands/zscan)工作方式非常相似，因此本文档涵盖了所有四个命令。然而，一个明显的区别是，在[SSCAN](https://redis.io/commands/sscan)，[HSCAN](https://redis.io/commands/hscan)和[ZSCAN](https://redis.io/commands/zscan)的情况下，第一个参数是包含Set，Hash或Sorted Set值的键的名称。该[SCAN](https://redis.io/commands/scan)命令不需要任何按键名称参数，因为它在当前数据库中遍历键，所以迭代对象是数据库本身。



## SCAN基本用法

SCAN是基于游标的迭代器。这意味着在每次调用命令时，服务器都会返回一个更新的游标，用户需要在下一次调用中将其用作游标参数。

当游标设置为0时开始迭代，并且当服务器返回的游标为0时终止。以下是SCAN迭代的示例：

```
redis 127.0.0.1:6379> scan 0
1) "17"
2)  1) "key:12"
    2) "key:8"
    3) "key:4"
    4) "key:14"
    5) "key:16"
    6) "key:17"
    7) "key:15"
    8) "key:10"
    9) "key:3"
   10) "key:7"
   11) "key:1"
redis 127.0.0.1:6379> scan 17
1) "0"
2) 1) "key:5"
   2) "key:18"
   3) "key:0"
   4) "key:2"
   5) "key:19"
   6) "key:13"
   7) "key:6"
   8) "key:9"
   9) "key:11"
```

在上面的示例中，第一个调用使用零作为游标，以启动迭代。第二个调用使用前一个调用返回的游标作为回复的第一个元素，即17。

如您所见，**SCAN返回值**是一个包含两个值的数组：第一个值是在下一个调用中使用的新游标，第二个值是元素数组。

由于在第二次调用中返回的游标为0，服务器向调用者发信号通知迭代完成，并且完全探索了该集合。启动游标值为0的迭代，并调用[SCAN](https://redis.io/commands/scan)直到返回的游标再次为0称为**完全迭代**。



## 扫描保证

在[SCAN](https://redis.io/commands/scan)命令，并在其他命令[SCAN](https://redis.io/commands/scan)家庭，能够提供给用户的一组相关联的全迭代保证。

- 完整迭代始终从完整迭代的开始到结束检索集合中存在的所有元素。这意味着如果在迭代开始时给定元素在集合内部，并且在迭代终止时仍然存在，那么在某些时候[SCAN](https://redis.io/commands/scan)将其返回给用户。
- 完整迭代永远不会返回从完整迭代的开始到结束不存在于集合中的任何元素。因此，如果在迭代开始之前删除了一个元素，并且迭代持续一直没有添加回集合，[SCAN将](https://redis.io/commands/scan)确保永远不会返回此元素。

但是因为[SCAN](https://redis.io/commands/scan)几乎没有关联状态（只是光标），它有以下缺点：

- 给定元素可以多次返回。由应用程序来处理重复元素的情况，例如仅使用返回的元素来执行多次重新应用时安全的操作。
- 在完整迭代期间不会始终存在于集合中的元素可以返回或不返回：它是未定义的。



## 每次SCAN调用时返回的元素数

[SCAN](https://redis.io/commands/scan)系列函数不保证每次调用返回的元素数量在给定范围内。这些命令也允许返回零元素，只要返回的游标不为零，客户端就不应该认为迭代完成。

但是，返回元素的数量是合理的，也就是说，实际上，当迭代大集合时，SCAN可以返回大约数十个元素的元素的最大数量，或者可以在单个元素中返回集合的所有元素当迭代集合足够小以在内部表示为编码数据结构时调用（这适用于小集合，散列和有序集合）。

但是，用户可以使用**COUNT**选项调整每个呼叫返回元素数量级的数量级。



## COUNT选项

虽然[SCAN](https://redis.io/commands/scan)不提供有关每次迭代返回的元素数量的保证，但可以使用**COUNT**选项凭经验调整[SCAN](https://redis.io/commands/scan)的行为。基本上使用COUNT，用户指定了*每次调用时应该完成的工作量，以便从集合中检索元素*。这**只是**实施的**一个提示**，但总的来说，这是您在实施过程中大部分时间都可以期待的。

- 默认的COUNT值为10。
- 当迭代密钥空间或大小足以由哈希表表示的Set，Hash或Sorted Set时，假设没有使用**MATCH**选项，服务器通常会返回*count*或比每次调用的*count*元素更多的*计数*。请检查*SCAN可能会*在本文档后面的部分*返回所有元素*的*原因*。
- 当迭代编码为intsets的集合（仅由整数组成的小集合），或编码为ziplists的哈希和排序集合（小哈希和由小的单个值组成的集合）时，通常所有元素都在第一个[SCAN](https://redis.io/commands/scan)调用中返回，而不管COUNT如何值。

重要提示：每次迭代**都不需要使用相同的COUNT值**。只要在下一次调用中传递的光标是在上一次调用命令时获得的光标，调用者就可以根据需要自由地将计数从一次迭代更改为另一次迭代。



## MATCH选项

可以仅迭代匹配给定glob样式模式的元素，类似于将模式作为唯一参数的[KEYS](https://redis.io/commands/keys)命令的行为。

为此，只需`MATCH <pattern>`在[SCAN](https://redis.io/commands/scan)命令的末尾附加参数（它适用于所有SCAN系列命令）。

这是使用**MATCH**迭代的示例：

```
redis 127.0.0.1:6379> sadd myset 1 2 3 foo foobar feelsgood
(integer) 6
redis 127.0.0.1:6379> sscan myset 0 match f*
1) "0"
2) 1) "foo"
   2) "feelsgood"
   3) "foobar"
redis 127.0.0.1:6379>
```

值得注意的是，**MATCH**过滤器是在从集合中检索元素之后应用的，就在将数据返回到客户端之前。这意味着如果模式匹配集合中的非常少的元素，[SCAN](https://redis.io/commands/scan)很可能在大多数迭代中不返回任何元素。一个例子如下所示：

```
redis 127.0.0.1:6379> scan 0 MATCH *11*
1) "288"
2) 1) "key:911"
redis 127.0.0.1:6379> scan 288 MATCH *11*
1) "224"
2) (empty list or set)
redis 127.0.0.1:6379> scan 224 MATCH *11*
1) "80"
2) (empty list or set)
redis 127.0.0.1:6379> scan 80 MATCH *11*
1) "176"
2) (empty list or set)
redis 127.0.0.1:6379> scan 176 MATCH *11* COUNT 1000
1) "0"
2)  1) "key:611"
    2) "key:711"
    3) "key:118"
    4) "key:117"
    5) "key:311"
    6) "key:112"
    7) "key:111"
    8) "key:110"
    9) "key:113"
   10) "key:211"
   11) "key:411"
   12) "key:115"
   13) "key:116"
   14) "key:114"
   15) "key:119"
   16) "key:811"
   17) "key:511"
   18) "key:11"
redis 127.0.0.1:6379>
```

正如您所看到的，大多数调用返回零元素，但最后一次调用使用了COUNT为1000，以强制命令对该迭代执行更多扫描。



## 多个并行迭代

无限数量的客户端可以同时迭代同一个集合，因为迭代器的完整状态在游标中，在每次调用时都会获得并返回给客户端。服务器端根本没有采取任何状态。



## 终止中间的迭代

由于没有状态服务器端，但是光标捕获了完整状态，因此调用者可以自由地终止迭代，而不会以任何方式向服务器发送信号。无限次迭代可以启动，永不终止，没有任何问题。



## 使用损坏的游标调用SCAN

使用断开，否定，超出范围或其他无效游标调用[SCAN](https://redis.io/commands/scan)将导致未定义的行为但从未进入崩溃。未定义的是[SCAN](https://redis.io/commands/scan)实现无法再保证返回元素的保证。

唯一有效的游标是：

- 开始迭代时光标值为0。
- 前一次调用SCAN返回的游标，以便继续迭代。



## 保证终止

该[SCAN](https://redis.io/commands/scan)算法保证只有在迭代集合的大小保持一定到给定的最大尺寸终止，否则迭代的集合，总是增长可能导致到[SCAN](https://redis.io/commands/scan)永远不会终止一个完整的迭代。

这很容易直观地看出：如果集合增长，为了访问所有可能的元素，还有越来越多的工作要做，并且终止迭代的能力取决于对[SCAN](https://redis.io/commands/scan)的调用次数及其与COUNT选项值的比较。收集增长的速度。



## 为什么SCAN可以在一次调用中返回聚合数据类型的所有项目？

在`COUNT`选项文档中，我们声明有时这个命令系列可以在单个调用中一次性返回Set，Hash或Sorted Set的所有元素，而不管`COUNT`选项值如何。发生这种情况的原因是，只有当我们扫描的聚合数据类型表示为哈希表时，才能实现基于游标的迭代器，并且它很有用。然而，Redis使用[内存优化](https://redis.io/topics/memory-optimization)，其中小聚合数据类型，直到它们达到给定数量的项目或给定的单个元素的最大大小，使用紧凑的单分配打包编码来表示。在这种情况下，[SCAN](https://redis.io/commands/scan) 没有有意义的游标返回，并且必须立即迭代整个数据结构，所以唯一合理的行为是返回调用中的所有内容。

但是，一旦数据结构更大并且被提升为使用实际哈希表，[SCAN](https://redis.io/commands/scan)系列命令将采用正常行为。请注意，由于返回所有元素的这种特殊行为仅适用于小聚合，因此它对命令复杂性或延迟没有影响。但是，转换为实际哈希表的确切限制是[用户可配置的](https://redis.io/topics/memory-optimization)，因此在单个调用中返回的最大元素数取决于聚合数据类型的大小，仍然使用打包表示。

另请注意，此行为特定于[SSCAN](https://redis.io/commands/sscan)，[HSCAN](https://redis.io/commands/hscan)和[ZSCAN](https://redis.io/commands/zscan)。[SCAN](https://redis.io/commands/scan)本身从不显示此行为，因为密钥空间始终由哈希表表示。



## 返回值

[SCAN](https://redis.io/commands/scan)，[SSCAN](https://redis.io/commands/sscan)，[HSCAN](https://redis.io/commands/hscan)和[ZSCAN](https://redis.io/commands/zscan)返回两个元素的多批量回复，其中第一个元素是表示无符号64位数字（光标）的字符串，第二个元素是具有元素数组的多个批量。

- [SCAN](https://redis.io/commands/scan)元素数组是键列表。
- [SSCAN](https://redis.io/commands/sscan)元素数组是Set成员列表。
- [HSCAN](https://redis.io/commands/hscan)元素数组包含两个元素，一个字段和一个值，用于Hash的每个返回元素。
- [ZSCAN](https://redis.io/commands/zscan)元素数组包含两个元素，即成员及其关联的分数，用于已排序集合的每个返回元素。



## 其他例子

迭代哈希值。

```
redis 127.0.0.1:6379> hmset hash name Jack age 33
OK
redis 127.0.0.1:6379> hscan hash 0
1) "0"
2) 1) "name"
   2) "Jack"
   3) "age"
   4) "33"
```