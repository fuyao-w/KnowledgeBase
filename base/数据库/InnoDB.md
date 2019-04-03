### InnoDB的主要优势

- 它的[DML](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_dml)操作遵循 [ACID](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_acid)模型， 具有 [提交](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_commit)， [回滚](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_rollback)和 [崩溃恢复](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_crash_recovery) 功能的[事务](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_transaction)来保护用户数据。有关更多信息[，](https://dev.mysql.com/doc/refman/8.0/en/mysql-acid.html)请参见 [第15.2节“InnoDB和ACID模型”](https://dev.mysql.com/doc/refman/8.0/en/mysql-acid.html)。
- 行级[锁定](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_locking)和Oracle风格的[一致性读取可](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_consistent_read)提高多用户并发性和性能。有关更多信息[，](https://dev.mysql.com/doc/refman/8.0/en/innodb-locking-transaction-model.html)请参见[第15.7节“InnoDB锁定和事务模型”](https://dev.mysql.com/doc/refman/8.0/en/innodb-locking-transaction-model.html)。
- `InnoDB`表格将您的数据排列在磁盘上，以根据[主键](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_primary_key)优化查询 。每个 `InnoDB`表都有一个称为[聚簇索引](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_clustered_index)的主键索引 ，用于组织数据以最小化主键查找的I / O. 有关更多信息[，](https://dev.mysql.com/doc/refman/8.0/en/innodb-index-types.html)请参见[第15.6.2.1节“聚簇和二级索引”](https://dev.mysql.com/doc/refman/8.0/en/innodb-index-types.html)。
- 要保持数据 [完整性](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_referential_integrity)，请 `InnoDB`支持 [`FOREIGN KEY`](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_foreign_key)约束。使用外键，将检查插入，更新和删除，以确保它们不会导致不同表之间的不一致。有关更多信息[，](https://dev.mysql.com/doc/refman/8.0/en/innodb-foreign-key-constraints.html)请参见 [第15.6.1.5节“InnoDB和FOREIGN KEY约束”](https://dev.mysql.com/doc/refman/8.0/en/innodb-foreign-key-constraints.html)。

**表15.1 InnoDB存储引擎功能**

| 特征                                                         | 支持                                                         |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| **B树索引**                                                  | 是                                                           |
| **备份/时间点恢复**（在服务器中实现，而不是在存储引擎中实现。） | 是                                                           |
| **群集数据库支持**                                           | 没有                                                         |
| **聚簇索引**                                                 | 是                                                           |
| **压缩数据**                                                 | 是                                                           |
| **数据缓存**                                                 | 是                                                           |
| **加密数据**                                                 | 是（通过加密功能在服务器中实现;在MySQL 5.7及更高版本中，支持静态数据表空间加密。） |
| **外键支持**                                                 | 是                                                           |
| **全文搜索索引**                                             | 是（在MySQL 5.6及更高版本中可以使用InnoDB对FULLTEXT索引的支持。） |
| **地理空间数据类型支持**                                     | 是                                                           |
| **地理空间索引支持**                                         | 是（在MySQL 5.7及更高版本中可以使用InnoDB对地理空间索引的支持。） |
| **哈希索引**                                                 | 否（InnoDB在内部利用哈希索引来实现其自适应哈希索引功能。）   |
| **索引缓存**                                                 | 是                                                           |
| **锁定粒度**                                                 | 行                                                           |
| **MVCC**                                                     | 是                                                           |
| **复制支持**（在服务器中实现，而不是在存储引擎中实现。）     | 是                                                           |
| **存储限制**                                                 | 64TB                                                         |
| **T树索引**                                                  | 没有                                                         |
| **交易**                                                     | 是                                                           |
| **更新数据字典的统计信息**                                   | 是                                                           |

要比较`InnoDB`MySQL提供的其他存储引擎的功能，请参阅[第16章](https://dev.mysql.com/doc/refman/8.0/en/storage-engines.html)[*备用存储引擎中*](https://dev.mysql.com/doc/refman/8.0/en/storage-engines.html)的*存储引擎功能*表 。

### 表优化

本节介绍使用`InnoDB`表时的最佳实践 。

- 使用最常查询的列或列 指定每个表的[主键](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_primary_key)， 如果没有明显的主键，则指定 [自动增量](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_auto_increment)值。

- 根据来自这些表的相同ID值从多个表中提取数据的位置 使用[连接](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_join)。要获得快速连接性能，请在连接列上定义 [外键](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_foreign_key)，并在每个表中声明具有相同数据类型的列。添加外键可确保对引用的列建立索引，从而提高性能。外键还会将删除或更新传播到所有受影响的表，如果父表中不存在相应的ID，则会阻止在子表中插入数据。

- 关闭[自动提交](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_autocommit)。每秒承诺数百次会限制性能（受存储设备写入速度的限制）。

- 分组组相关的[DML](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_dml) 操作成 [交易](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_transaction)，通过包围他们`START TRANSACTION`和 `COMMIT`报表。虽然你不想过于频繁地提交，你也不想发出的巨大的批次 [`INSERT`](https://dev.mysql.com/doc/refman/8.0/en/insert.html)， [`UPDATE`](https://dev.mysql.com/doc/refman/8.0/en/update.html)或者[`DELETE`](https://dev.mysql.com/doc/refman/8.0/en/delete.html)，如果没有犯了几个小时运行的语句。

- 不使用[`LOCK TABLES`](https://dev.mysql.com/doc/refman/8.0/en/lock-tables.html) 语句。`InnoDB`可以同时处理多个会话，同时读取和写入同一个表，而不会牺牲可靠性或高性能。要获得对一组行的独占写访问权，请使用 [`SELECT ... FOR UPDATE`](https://dev.mysql.com/doc/refman/8.0/en/innodb-locking-reads.html)语法仅锁定要更新的行。

- 启用该 [`innodb_file_per_table`](https://dev.mysql.com/doc/refman/8.0/en/innodb-parameters.html#sysvar_innodb_file_per_table)选项或使用通用表空间将表的数据和索引放入单独的文件中，而不是 [系统表空间](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_system_tablespace)。

  [`innodb_file_per_table`](https://dev.mysql.com/doc/refman/8.0/en/innodb-parameters.html#sysvar_innodb_file_per_table) 默认情况下启用 该选项。

- 评估您的数据和访问模式是否受益于`InnoDB`表或页面 [压缩](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_compression)功能。您可以在`InnoDB`不牺牲读/写功能的情况下压缩表。

- 使用选项运行服务器， [`--sql_mode=NO_ENGINE_SUBSTITUTION`](https://dev.mysql.com/doc/refman/8.0/en/server-system-variables.html#sysvar_sql_mode) 以防止在使用`ENGINE=`子句中 指定的引擎出现问题时使用其他存储引擎创建表 [`CREATE TABLE`](https://dev.mysql.com/doc/refman/8.0/en/create-table.html)。

