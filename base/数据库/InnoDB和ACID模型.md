[ACID](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_acid)模式是一组数据库设计原则强调的是，对于业务数据和关键任务应用重要的可靠性方面。MySQL包含诸如的组件`InnoDB`存储引擎与ACID模型紧密结合，因此数据不会损坏，并且不会因软件崩溃和硬件故障等特殊情况而导致结果失真。当您依赖符合ACID的功能时，您无需重新发明一致性检查和崩溃恢复机制。如果您有其他软件安全措施，超可靠硬件或可以容忍少量数据丢失或不一致的应用程序，您可以调整MySQL设置以交换一些ACID可靠性以获得更高的性能或吞吐量。

以下部分讨论MySQL功能（尤其是`InnoDB`存储引擎）如何 与ACID模型的类别进行交互：

- **A**：原子性
- **C**：一致性
- **I** : 隔离性
- **D**：持久性

### 原子性

ACID模型 的**原子性**方面主要涉及`InnoDB` [事物](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_transaction)。相关的MySQL功能包括：

- 自动提交设置。
- [`COMMIT`](https://dev.mysql.com/doc/refman/8.0/en/commit.html) 声明。
- [`ROLLBACK`](https://dev.mysql.com/doc/refman/8.0/en/commit.html) 声明。
- 来自`INFORMATION_SCHEMA` 表格的运营数据。

### 一致性

ACID模型 的**一致性**方面主要涉及内部`InnoDB`处理以保护数据免于崩溃。相关的MySQL功能包括：

- `InnoDB` [双写缓冲区](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_doublewrite_buffer)。

- `InnoDB` [崩溃恢复](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_crash_recovery)。

  双写缓冲区

  `InnoDB`使用称为doublewrite的文件刷新技术。在从页面写入数据文件之前， `InnoDB`第一次将它们写入称为双写缓冲区中的连续区域。只有在写入和刷新到doublewrite缓冲区之后，`InnoDB`才会 将页面写入数据文件中的正确位置。如果在页面写入过程中存在操作系统，存储子系统或[**mysqld**](https://dev.mysql.com/doc/refman/8.0/en/mysqld.html)进程崩溃，则`InnoDB`可以在**崩溃恢复**期间从doublewrite缓冲区中找到该页面的完整副本 。

  尽管数据总是写入两次，但双写缓冲区不需要两倍的I / O开销或两倍的I / O操作。数据作为一个大的顺序块写入缓冲区本身，只需一次`fsync()`调用操作系统。

  要关闭doublewrite缓冲区，请指定该选项 [`innodb_doublewrite=0`](https://dev.mysql.com/doc/refman/8.0/en/innodb-parameters.html#sysvar_innodb_doublewrite)。

### 隔离性

ACID模型 的**隔离**方面主要涉及`InnoDB` [事务](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_transaction)，特别是适用于每个事务的[隔离级别](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_isolation_level)。相关的MySQL功能包括：

- [自动提交](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_autocommit)设置。
- `SET ISOLATION LEVEL` 声明。
- `InnoDB` [锁定](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_locking) 的低级细节。在性能调整期间，您可以通过`INFORMATION_SCHEMA`表格 查看这些详细信

### 持久性

ACID模型 的**持久性**方面涉及MySQL软件功能与您的特定硬件配置交互。由于取决于CPU，网络和存储设备的功能的许多可能性，这方面是最复杂的提供具体指导方针。（这些指南可能采取购买“ 新硬件 ”的形式 。）相关的MySQL功能包括：

- `InnoDB` [doublewrite buffer](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_doublewrite_buffer)，由[`innodb_doublewrite`](https://dev.mysql.com/doc/refman/8.0/en/innodb-parameters.html#sysvar_innodb_doublewrite) 配置选项打开和关闭 。
- 配置选项 [`innodb_flush_log_at_trx_commit`](https://dev.mysql.com/doc/refman/8.0/en/innodb-parameters.html#sysvar_innodb_flush_log_at_trx_commit)。
- 配置选项 [`sync_binlog`](https://dev.mysql.com/doc/refman/8.0/en/replication-options-binary-log.html#sysvar_sync_binlog)。
- 配置选项 [`innodb_file_per_table`](https://dev.mysql.com/doc/refman/8.0/en/innodb-parameters.html#sysvar_innodb_file_per_table)。
- 在存储设备中写入缓冲区，例如磁盘驱动器，SSD或RAID阵列。
- 存储设备中的电池备份缓存。
- 用于运行MySQL的操作系统，特别是它对`fsync()`系统调用的支持。
- 不间断电源（UPS）保护运行MySQL服务器和存储MySQL数据的所有计算机服务器和存储设备的电源。
- 您的备份策略，例如备份的频率和类型以及备份保留期。
- 对于分布式或托管数据应用程序，MySQL服务器的硬件所在的数据中心的特定特征，以及数据中心之间的网络连接。