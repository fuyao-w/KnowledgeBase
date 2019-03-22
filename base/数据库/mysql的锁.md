###  InnoDB锁

本节介绍使用的锁类型 `InnoDB`。

1. [共享和独占锁](https://dev.mysql.com/doc/refman/8.0/en/innodb-locking.html#innodb-shared-exclusive-locks)

2. [意向锁](https://dev.mysql.com/doc/refman/8.0/en/innodb-locking.html#innodb-intention-locks)

3. [记录锁](https://dev.mysql.com/doc/refman/8.0/en/innodb-locking.html#innodb-record-locks)
4. [间隙锁定](https://dev.mysql.com/doc/refman/8.0/en/innodb-locking.html#innodb-gap-locks)
5. [next-key锁](https://dev.mysql.com/doc/refman/8.0/en/innodb-locking.html#innodb-next-key-locks)
6. [插入意向锁](https://dev.mysql.com/doc/refman/8.0/en/innodb-locking.html#innodb-insert-intention-locks)
7. [AUTO-INC锁](https://dev.mysql.com/doc/refman/8.0/en/innodb-locking.html#innodb-auto-inc-locks)
8. [空间索引的谓词锁](https://dev.mysql.com/doc/refman/8.0/en/innodb-locking.html#innodb-predicate-locks)

#### 共享和独占锁

`InnoDB`实现标准的行级锁定，其中有两种类型的锁， [shared（`S`）锁](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_shared_lock)和[exclusive（`X`）锁](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_exclusive_lock)。

- [共享（`S`）锁](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_shared_lock)允许持有锁的事务读取行。
- 一个[独占（`X`）锁](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_exclusive_lock)允许持有锁的事务更新或删除行。

如果事务在行上`T1`持有一个shared（`S`）锁`r`，那么来自某个不同事务`T2` 的对行锁的请求`r`将按如下方式处理：

- 由A请求`T2`用于 `S`锁可以立即被授予。其结果是，无论是`T1`与`T2` 持有`S`的锁`r`。
- 通过请求`T2`一个 `X`锁不能立即授予。

如果事务在行上`T1`持有exclusive（`X`）锁`r`，则不能立即授予来自某个不同事务`T2`的锁定任何类型的锁的请求`r`。相反，事务`T2`必须等待事务`T1`释放其对行的锁定`r`。

#### 意向锁

`InnoDB`支持*多粒度锁定*，允许行锁和表锁共存。例如，诸如 在指定表上[`LOCK TABLES ... WRITE`](https://dev.mysql.com/doc/refman/8.0/en/lock-tables.html)进行独占锁定（`X`锁定）之类的语句 。要在多个粒度级别实现锁定，请`InnoDB`使用[意向锁](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_intention_lock)。意向锁是表级锁，它指示事务稍后对表中的行所需的锁（共享或独占）类型。意向锁有两种类型：

- [意向共享锁](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_intention_shared_lock)（`IS`）表示事务打算在表中的各个行上设置共享锁。
- [意向独占锁](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_intention_exclusive_lock)（`IX`）表示事务打算在表中的各个行上设置独占锁。

例如，[`SELECT ... FOR SHARE`](https://dev.mysql.com/doc/refman/8.0/en/select.html)设置`IS`锁定并 [`SELECT ... FOR UPDATE`](https://dev.mysql.com/doc/refman/8.0/en/select.html)设置`IX`锁定。

意向锁定协议如下：

- 在事务可以获取表中某行的共享锁之前，它必须首先在表上获取IS锁或更强。
- 在事务可以获取表中某行的独占锁之前，它必须首先获取表上的IX锁。

表级锁定类型兼容性总结在以下矩阵中。

|      | `X`  | `IX` | `S`  | `IS` |
| ---- | ---- | ---- | ---- | ---- |
| `X`  | 冲突 | 冲突 | 冲突 | 冲突 |
| `IX` | 冲突 | 兼容 | 冲突 | 兼容 |
| `S`  | 冲突 | 冲突 | 兼容 | 兼容 |
| `IS` | 冲突 | 兼容 | 兼容 | 兼容 |

如果请求事务与现有锁兼容，则授予锁，但如果它与现有锁冲突则不会。事务等待，直到释放冲突的现有锁。如果锁定请求与现有锁冲突而无法授予，因为它会导致 [死锁](https://dev.mysql.com/doc/refman/8.0/en/glossary.html#glos_deadlock)，则会发生错误。

意向锁定不会阻止除完整表请求之外的任何内容（例如，[`LOCK TABLES ... WRITE`](https://dev.mysql.com/doc/refman/8.0/en/lock-tables.html)）。意向锁定的主要目的是显示某人正在锁定行，或者要锁定表中的行。

意向锁定的事务数据[`SHOW ENGINE INNODB STATUS`](https://dev.mysql.com/doc/refman/8.0/en/show-engine.html)与 [InnoDB监视器](https://dev.mysql.com/doc/refman/8.0/en/innodb-standard-monitor.html) 输出中的以下内容类似：

```sql
TABLE LOCK table `test`.`t` trx id 10080 lock mode IX
```

#### 记录锁

记录锁定是对索引记录的锁定。例如， `SELECT c1 FROM t WHERE c1 = 10 FOR UPDATE;` 可以防止从插入，更新或删除行，其中的值的任何其它交易`t.c1`是 `10`。

即使定义了没有索引的表，记录锁也始终锁定索引记录。对于此类情况，请 `InnoDB`创建隐藏的聚簇索引并使用此索引进行记录锁定。请参见 [第15.6.2.1节“聚簇和二级索引”](https://dev.mysql.com/doc/refman/8.0/en/innodb-index-types.html)。

记录锁的事务数据[`SHOW ENGINE INNODB STATUS`](https://dev.mysql.com/doc/refman/8.0/en/show-engine.html)与 [InnoDB监视器](https://dev.mysql.com/doc/refman/8.0/en/innodb-standard-monitor.html) 输出中的以下内容类似：

```sql
RECORD LOCKS space id 58 page no 3 n bits 72 index `PRIMARY` of table `test`.`t` 
trx id 10078 lock_mode X locks rec but not gap
Record lock, heap no 2 PHYSICAL RECORD: n_fields 3; compact format; info bits 0
 0: len 4; hex 8000000a; asc     ;;
 1: len 6; hex 00000000274f; asc     'O;;
 2: len 7; hex b60000019d0110; asc        ;;
```

#### 间隙锁

间隙锁定是锁定索引记录之间的间隙，或锁定在第一个或最后一个索引记录之前的间隙。例如，`SELECT c1 FROM t WHERE c1 BETWEEN 10 and 20 FOR UPDATE;`阻止其他事务将值`15`插入列`t.c1`，无论列 中是否已存在任何此类值，因为该范围中所有现有值之间的间隙都已锁定。

间隙可能跨越单个索引值，多个索引值，甚至可能为空。

差距锁是性能和并发之间权衡的一部分，用于某些事务隔离级别而不是其他级别。

使用唯一索引锁定行以搜索唯一行的语句不需要间隙锁定。（这不包括搜索条件仅包含多列唯一索引的某些列的情况;在这种情况下，确实会发生间隙锁定。）例如，如果`id`列具有唯一索引，则以下语句仅使用具有`id`值100 的行的索引记录锁定，其他会话是否在前一个间隙中插入行无关紧要：

```sql
SELECT * FROM child WHERE id = 100;
```

如果`id`未编入索引或具有非唯一索引，则该语句会锁定前一个间隙。

此处值得注意的是，冲突锁可以通过不同的事务保持在间隙上。例如，事务A可以在间隙上保持共享间隙锁定（间隙S锁定），而事务B在同一间隙上保持独占间隙锁定（间隙X锁定）。允许冲突间隙锁定的原因是，如果从索引中清除记录，则必须合并由不同事务保留在记录上的间隙锁定。

间隙锁定`InnoDB`是“ 纯粹抑制 ”，这意味着它们的唯一目的是防止其他事务插入间隙。差距锁可以共存。一个事务占用的间隙锁定不会阻止另一个事务在同一个间隙上进行间隙锁定。共享和独占间隙锁之间没有区别。它们彼此不冲突，它们执行相同的功能。

可以明确禁用间隙锁定。如果将事务隔离级别更改为，则会发生这种情况 [`READ COMMITTED`](https://dev.mysql.com/doc/refman/8.0/en/innodb-transaction-isolation-levels.html#isolevel_read-committed)。在这些情况下，对于搜索和索引扫描禁用间隙锁定，并且仅用于外键约束检查和重复键检查。

使用[`READ COMMITTED`](https://dev.mysql.com/doc/refman/8.0/en/innodb-transaction-isolation-levels.html#isolevel_read-committed)隔离级别还有其他影响 。在MySQL评估`WHERE`条件后，将释放非匹配行的记录锁。对于 `UPDATE`语句，`InnoDB` 执行“ 半一致 ”读取，以便将最新提交的版本返回给MySQL，以便MySQL可以确定该行是否符合`WHERE` 条件[`UPDATE`](https://dev.mysql.com/doc/refman/8.0/en/update.html)。

#### 下一键锁

下一键锁定是索引记录上的记录锁和索引记录之前的间隙上的间隙锁的组合。

锁定一个范围，并且锁定记录本身。对于行的查询，都是采用该方法，主要目的是解决幻读的问题。

`InnoDB`以这样一种方式执行行级锁定：当它搜索或扫描表索引时，它会在遇到的索引记录上设置共享锁或排它锁。因此，行级锁实际上是索引记录锁。索引记录上的下一键锁定也会影响该索引记录之前的“ 间隙 ”。也就是说，下一键锁定是索引记录锁定加上索引记录之前的间隙上的间隙锁定。如果一个会话`R`在索引中具有共享或独占锁定记录 ，则另一个会话不能`R`在索引顺序之前的间隙中插入新索引记录 。

假设索引包含值10,11,13和20.此索引的可能的下一个键锁定包括以下间隔，其中圆括号表示排除间隔端点，方括号表示包含端点：

```none
(negative infinity, 10]
(10, 11]
(11, 13]
(13, 20]
(20, positive infinity)
```

对于最后一个间隔，下一个键锁定将间隙锁定在索引中最大值之上，而“ supremum ” 伪记录的值高于索引中实际的任何值。supremum不是真正的索引记录，因此，实际上，此下一键锁定仅锁定最大索引值之后的间隙。

默认情况下，`InnoDB`以 [`REPEATABLE READ`](https://dev.mysql.com/doc/refman/8.0/en/innodb-transaction-isolation-levels.html#isolevel_repeatable-read)事务隔离级别运行。在这种情况下，`InnoDB`使用下一键锁进行搜索和索引扫描，这会阻止幻像行（请参见[第15.7.4节“幻影行”](https://dev.mysql.com/doc/refman/8.0/en/innodb-next-key-locking.html)）。

下一键锁的事务数据[`SHOW ENGINE INNODB STATUS`](https://dev.mysql.com/doc/refman/8.0/en/show-engine.html)与 [InnoDB监视器](https://dev.mysql.com/doc/refman/8.0/en/innodb-standard-monitor.html) 输出中的以下内容类似：

```sql
RECORD LOCKS space id 58 page no 3 n bits 72 index `PRIMARY` of table `test`.`t` 
trx id 10080 lock_mode X
Record lock, heap no 1 PHYSICAL RECORD: n_fields 1; compact format; info bits 0
 0: len 8; hex 73757072656d756d; asc supremum;;

Record lock, heap no 2 PHYSICAL RECORD: n_fields 3; compact format; info bits 0
 0: len 4; hex 8000000a; asc     ;;
 1: len 6; hex 00000000274f; asc     'O;;
 2: len 7; hex b60000019d0110; asc        ;;
```

#### 插入意向锁

插入意向锁定是一种由[`INSERT`](https://dev.mysql.com/doc/refman/8.0/en/insert.html)行插入之前的操作设置的间隙锁。该锁定表示以这样的方式插入的意向：如果多个事务插入到相同的索引间隙中，如果它们不在间隙中的相同位置插入，则无需等待其他事务。比如说有索引记录4和7，有两个事务想要分别插入5，6，在获取插入行上的独占锁之前，每个锁都使用插入意图锁锁定4和7之间的间隙，但是不要互相阻塞，因为行是不冲突的，意向锁的涉及是为了插入的正确和高效。

 

以下示例演示了在获取插入记录的独占锁之前采用插入意向锁定的事务。该示例涉及两个客户端，A和B.

客户端A创建一个包含两个索引记录（90和102）的表，然后启动一个事务，该事务对ID大于100的索引记录放置独占锁。独占锁包括记录102之前的间隙锁：

```sql
mysql> CREATE TABLE child (id int(11) NOT NULL, PRIMARY KEY(id)) ENGINE=InnoDB;
mysql> INSERT INTO child (id) values (90),(102);

mysql> START TRANSACTION;
mysql> SELECT * FROM child WHERE id > 100 FOR UPDATE;
+-----+
| id  |
+-----+
| 102 |
+-----+
```

客户端B开始事务以将记录插入间隙。该事务在等待获取独占锁时采用插入意向锁。

```sql
mysql> START TRANSACTION;
mysql> INSERT INTO child (id) VALUES (101);
```

插入意向锁的事务数据[`SHOW ENGINE INNODB STATUS`](https://dev.mysql.com/doc/refman/8.0/en/show-engine.html)与 [InnoDB监视器](https://dev.mysql.com/doc/refman/8.0/en/innodb-standard-monitor.html) 输出中的以下内容类似 ：

```sql
RECORD LOCKS space id 31 page no 3 n bits 72 index `PRIMARY` of table `test`.`child`
trx id 8731 lock_mode X locks gap before rec insert intention waiting
Record lock, heap no 3 PHYSICAL RECORD: n_fields 3; compact format; info bits 0
 0: len 4; hex 80000066; asc    f;;
 1: len 6; hex 000000002215; asc     " ;;
 2: len 7; hex 9000000172011c; asc     r  ;;...
```

#### AUTO-INC锁定

一个`AUTO-INC`锁是通过交易将与表中取得一个特殊的表级锁 `AUTO_INCREMENT`列。在最简单的情况下，如果一个事务正在向表中插入值，则任何其他事务必须等待对该表执行自己的插入，以便第一个事务插入的行接收连续的主键值。

该[`innodb_autoinc_lock_mode`](https://dev.mysql.com/doc/refman/8.0/en/innodb-parameters.html#sysvar_innodb_autoinc_lock_mode) 配置选项控制用于自动增加锁定的算法。它允许您选择如何在可预测的自动增量值序列和插入操作的最大并发之间进行权衡。

有关更多信息，请参见 [第15.6.1.4节“InnoDB中的AUTO_INCREMENT处理”](https://dev.mysql.com/doc/refman/8.0/en/innodb-auto-increment-handling.html)。

#### 空间索引的谓词锁

`InnoDB`支持`SPATIAL` 对包含空间列的列进行索引（请参见 [第11.5.9节“优化空间分析”](https://dev.mysql.com/doc/refman/8.0/en/optimizing-spatial-analysis.html)）。

要处理涉及`SPATIAL`索引的操作的锁定 ，下一键锁定不能很好地支持[`REPEATABLE READ`](https://dev.mysql.com/doc/refman/8.0/en/innodb-transaction-isolation-levels.html#isolevel_repeatable-read)或 [`SERIALIZABLE`](https://dev.mysql.com/doc/refman/8.0/en/innodb-transaction-isolation-levels.html#isolevel_serializable)事务隔离级别。多维数据中没有绝对排序概念，因此不清楚哪个是 “ 下一个”密钥。

要为具有`SPATIAL`索引的表启用隔离级别 ，请`InnoDB` 使用谓词锁。甲`SPATIAL`索引包含最小外接矩形（MBR）值，因此， `InnoDB`通过设置用于查询的MBR值的谓词锁强制上的索引一致的读取。其他事务无法插入或修改与查询条件匹配的行。