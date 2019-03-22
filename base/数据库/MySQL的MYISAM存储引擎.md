##  MyISAM存储引擎

- [16.2.1 MyISAM启动选项](https://dev.mysql.com/doc/refman/8.0/en/myisam-start.html)
- [16.2.2密钥所需的空间](https://dev.mysql.com/doc/refman/8.0/en/key-space.html)
- [16.2.3 MyISAM表存储格式](https://dev.mysql.com/doc/refman/8.0/en/myisam-table-formats.html)
- [16.2.4 MyISAM表问题](https://dev.mysql.com/doc/refman/8.0/en/myisam-table-problems.html)



`MyISAM`基于旧的（不再可用）`ISAM`存储引擎，但有许多有用的扩展。



**表16.2 MyISAM存储引擎功能**

| 特征                                                         | 支持                                                         |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| **B树索引**                                                  | 是                                                           |
| **备份/时间点恢复**（在服务器中实现，而不是在存储引擎中实现。） | 是                                                           |
| **群集数据库支持**                                           | 没有                                                         |
| **聚簇索引**                                                 | 没有                                                         |
| **压缩数据**                                                 | 是（仅在使用压缩行格式时支持压缩MyISAM表。使用带MyISAM的压缩行格式的表是只读的。） |
| **数据缓存**                                                 | 没有                                                         |
| **加密数据**                                                 | 是（通过加密功能在服务器中实现。）                           |
| **外键支持**                                                 | 没有                                                         |
| **全文搜索索引**                                             | 是                                                           |
| **地理空间数据类型支持**                                     | 是                                                           |
| **地理空间索引支持**                                         | 是                                                           |
| **哈希索引**                                                 | 没有                                                         |
| **索引缓存**                                                 | 是                                                           |
| **锁定粒度**                                                 | 表                                                           |
| **MVCC**                                                     | 没有                                                         |
| **复制支持**（在服务器中实现，而不是在存储引擎中实现。）     | 是                                                           |
| **存储限制**                                                 | 256TB                                                        |
| **T树索引**                                                  | 没有                                                         |
| **事物**                                                     | 没有                                                         |
| **更新数据字典的统计信息**                                   | 是                                                           |



每个`MyISAM`表都存储在磁盘上的两个文件中。这些文件的名称以表名开头，并具有指示文件类型的扩展名。数据文件具有 `.MYD`（`MYData`）扩展名。索引文件具有`.MYI`（`MYIndex`）扩展名。表定义存储在MySQL数据字典中。

要明确指定您想要一个`MyISAM` 表，请使用`ENGINE`表选项指示：

```sql
CREATE TABLE t (i INT) ENGINE = MYISAM;
```

在MySQL 8.0中，通常需要使用它 `ENGINE`来指定`MyISAM` 存储引擎，因为`InnoDB`它是默认引擎。

您可以`MyISAM`使用[**mysqlcheck**](https://dev.mysql.com/doc/refman/8.0/en/mysqlcheck.html)客户端或[**myisamchk**](https://dev.mysql.com/doc/refman/8.0/en/myisamchk.html) 实用程序检查或修复表 。您还可以`MyISAM`使用[**myisampack**](https://dev.mysql.com/doc/refman/8.0/en/myisampack.html)压缩表 以占用更少的空间。请参见 [第4.5.3节“ **mysqlcheck** - 表维护程序”](https://dev.mysql.com/doc/refman/8.0/en/mysqlcheck.html)，[第4.6.4节“ **myisamchk** - MyISAM表维护实用程序”](https://dev.mysql.com/doc/refman/8.0/en/myisamchk.html)和 [第4.6.6节“ **myisampack** - 生成压缩，只读MyISAM表”](https://dev.mysql.com/doc/refman/8.0/en/myisampack.html)。

在MySQL 8.0中，`MyISAM`存储引擎不提供分区支持。*在以前版本的MySQL中创建的**分区 MyISAM表不能在MySQL 8.0中使用*。有关更多信息，请参见 [第23.6.2节“分区与存储引擎相关的限制”](https://dev.mysql.com/doc/refman/8.0/en/partitioning-limitations-storage-engines.html)。有关升级此类表以便在MySQL 8.0中使用它们的帮助，请参见 [第2.11.4节“MySQL 8.0中的更改”](https://dev.mysql.com/doc/refman/8.0/en/upgrading-from-previous-series.html)。

`MyISAM` 表具有以下特征：

- 所有数据值首先以低字节存储。这使得数据机和操作系统独立。二进制可移植性的唯一要求是机器使用二进制补码有符号整数和IEEE浮点格式。这些要求在主流机器中广泛使用。二进制兼容性可能不适用于有时具有特殊处理器的嵌入式系统。

  首先存储低字节数据没有明显的速度损失; 表行中的字节通常是未对齐的，并且按顺序读取未对齐字节所需的处理比按相反顺序处理要多得多。此外，与其他代码相比，服务器中获取列值的代码不是时间关键。

- 所有数字键值首先与高字节一起存储，以允许更好的索引压缩。

- 支持大文件的文件系统和操作系统支持大文件（最大63位文件长度）。

- 表中有（2 32）2 （1.844E + 19）行的限制`MyISAM`。

- 每个`MyISAM` 表的最大索引数为64。

  每个索引的最大列数为16。

- 最大key长度为1000个字节。这也可以通过更改源和重新编译来更改。对于长度大于250字节的密钥的情况，使用比默认的1024字节更大的密钥块大小。

- 当按排序顺序插入行时（如使用 `AUTO_INCREMENT`列时），将拆分索引树，以便高节点仅包含一个键。这样可以提高索引树中的空间利用率。

- `AUTO_INCREMENT` 支持每个表 对一列进行内部处理。`MyISAM` 自动更新此列 [`INSERT`](https://dev.mysql.com/doc/refman/8.0/en/insert.html)以及 [`UPDATE`](https://dev.mysql.com/doc/refman/8.0/en/update.html)操作。这使得 `AUTO_INCREMENT`列更快（至少10％）。删除后，序列顶部的值不会重复使用。（当`AUTO_INCREMENT`列被定义为多列索引的最后一列时，会重复使用从序列顶部删除的值。）`AUTO_INCREMENT`可以使用[`ALTER TABLE`](https://dev.mysql.com/doc/refman/8.0/en/alter-table.html)或 [**myisamchk**](https://dev.mysql.com/doc/refman/8.0/en/myisamchk.html)重置该值 。

- 将删除与更新和插入混合时，动态大小的行的碎片要小得多。这是通过自动组合相邻的已删除块并通过在删除下一个块时扩展块来完成的。

- `MyISAM`支持并发插入：如果表在数据文件的中间没有空闲块，则可以[`INSERT`](https://dev.mysql.com/doc/refman/8.0/en/insert.html)在其他线程从表中读取的同时将 新行放入其中。由于删除行或使用比其当前内容更多的数据更新动态长度行，可能会发生空闲块。当所有空闲块都用完（填写）后，将来的插入会再次并发。请参见 [第8.11.3节“并发插入”](https://dev.mysql.com/doc/refman/8.0/en/concurrent-inserts.html)。

- 您可以将数据文件和索引文件放在不同的物理设备上的不同目录中，以便使用`DATA DIRECTORY`和`INDEX DIRECTORY`表选项获得更快的速度[`CREATE TABLE`](https://dev.mysql.com/doc/refman/8.0/en/create-table.html)。请参见[第13.1.20节“CREATE TABLE语法”](https://dev.mysql.com/doc/refman/8.0/en/create-table.html)。

- [`BLOB`](https://dev.mysql.com/doc/refman/8.0/en/blob.html)和 [`TEXT`](https://dev.mysql.com/doc/refman/8.0/en/blob.html)列可以编入索引。

- `NULL`索引列中允许使用值。每个密钥需要0到1个字节。

- 每个字符列可以具有不同的字符集。请参见 [第10章，*字符集，排序规则，Unicode*](https://dev.mysql.com/doc/refman/8.0/en/charset.html)。

- `MyISAM`索引文件中 有一个标志，指示表是否已正确关闭。如果 使用该选项启动 [**mysqld**](https://dev.mysql.com/doc/refman/8.0/en/mysqld.html)[`--myisam-recover-options`](https://dev.mysql.com/doc/refman/8.0/en/server-options.html#option_mysqld_myisam-recover-options)， `MyISAM`则会在打开时自动检查表，如果表未正确关闭则会对表进行修复。

- 如果使用该[`--update-state`](https://dev.mysql.com/doc/refman/8.0/en/myisamchk-check-options.html#option_myisamchk_update-state) 选项运行，[ **myisamchk**](https://dev.mysql.com/doc/refman/8.0/en/myisamchk.html)会将表标记为已选中。[**myisamchk --fast**](https://dev.mysql.com/doc/refman/8.0/en/myisamchk.html)仅检查那些没有此标记的表。

- [**myisamchk --analyze**](https://dev.mysql.com/doc/refman/8.0/en/myisamchk.html)存储部分密钥以及整个密钥的统计信息。

- [**myisampack**](https://dev.mysql.com/doc/refman/8.0/en/myisampack.html)可以打包 [`BLOB`](https://dev.mysql.com/doc/refman/8.0/en/blob.html)和 [`VARCHAR`](https://dev.mysql.com/doc/refman/8.0/en/char.html)列。

`MyISAM` 还支持以下功能：

- 支持真实[`VARCHAR`](https://dev.mysql.com/doc/refman/8.0/en/char.html)类型; 一[`VARCHAR`](https://dev.mysql.com/doc/refman/8.0/en/char.html)列开始与存储在一个或两个字节的长度。
- 具有[`VARCHAR`](https://dev.mysql.com/doc/refman/8.0/en/char.html)列的表可以具有固定或动态的行长度。
- 表中列[`VARCHAR`](https://dev.mysql.com/doc/refman/8.0/en/char.html)和 [`CHAR`](https://dev.mysql.com/doc/refman/8.0/en/char.html)列 的长度总和 可能高达64KB。
- 任意长度限制`UNIQUE`。