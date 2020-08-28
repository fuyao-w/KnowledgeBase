# InnoDB中如何处理AUTO_INCREMENT

InnoDB提供了一种可配置的锁定机制，该机制可以显着提高将行添加到具有AUTO_INCREMENT列的表中的SQL语句的可伸缩性和性能。** 要将AUTO_INCREMENT机制用于InnoDB表，必须将AUTO_INCREMENT列定义为索引的一部分**，以便可以对表执行与索引`SELECT MAX（ai_col）`查找等效的操作，以获得最大列值。 通常，这是通过使该列成为某个表索引的第一列来实现的。

本节描述了AUTO_INCREMENT锁定模式的行为，不同`AUTO_INCREMENT`锁定模式设置的使用含义以及InnoDB如何初始化AUTO_INCREMENT计数器。

## 自增锁定模式

本节介绍用于生成自动增量值的`AUTO_INCREMENT`锁定模式的行为，以及每种锁定模式如何影响复制。 在启动时使用innodb_autoinc_lock_mode 配置参数配置 AUTO_INCREMENT 锁定模式。

以下术语用于描述`innodb_autoinc_lock_mode`设置：

* INSERT-like 语句

所有在表中生成新行的语句，包括`INSERT，INSERT ... SELECT，REPLACE，REPLACE ... SELECT和LOAD DATA`。 包括**simple-inserts**，**bulk-inserts**和**mixed-mode**插入。

1. Simple inserts

可以预先确定要插入行数的语句（最初处理该语句时）。 这包括单行和多行 INSERT 和没有嵌套的子查询的 REPLACE 语句，但是 `INSERT ... ON DUPLICATE KEY UPDATE` 并不算简单插入。

2. Bulk inserts

事先不知道要插入行数（以及所需的自动增量值的数目）的语句。 这包括**INSERT ... SELECT**，**REPLACE ... SELECT**和**LOAD DATA**语句，但不包括普通**INSERT**。 在处理每一行时，InnoDB 为每行的`AUTO_INCREMENT`列分配一个新值。

3. Mixed-mode inserts

这些是**Simple inserts**语句，用于指定一些（但不是全部）新行的自动增量值。下面是一个示例，其中c1是表t1的AUTO_INCREMENT 列：

```sql
INSERT INTO t1 (c1,c2) VALUES (1,'a'), (NULL,'b'), (5,'c'), (NULL,'d');
```

另一种类型的**Mixed-mode inserts**是**INSERT ... ON DUPLICATE KEY UPDATE**，在最坏的情况下，实际上是先进性`INSERT`，然后是 `UPDATE`，在此期间，更新阶段**可能会也可能不会**使用`AUTO_INCREMENT`列的分配值。 

**innodb_autoinc_lock_mode**配置参数有三种可能的设置。 **traditional**，**consecutive**或**interleaved**锁定模式的设置分别为0、1或2。 从MySQL 8.0开始，**interleaved**锁定模式（`innodb_autoinc_lock_mode = 2`）是默认设置。 在MySQL 8.0之前，**consecutive**锁定模式是默认设置（`innodb_autoinc_lock_mode = 1`）。

MySQL 8.0 中**Mixed-mode inserts**模式的默认设置反映了从基于语句的复制到基于行的复制作为默认复制类型的变化。 基于语句的复制需要连续的**AUTO_INC**锁定模式，以确保为给定的SQL语句序列以可预测和可重复的顺序分配自动增量值，而基于行的复制对SQL语句的执行顺序不敏感 。

* `innodb_autoinc_lock_mode = 0` (传统锁模式)

传统的锁定模式提供了与 MySQL 5.1 中引入 innodb_autoinc_lock_mode 配置参数之前相同的行为。提供了传统的锁定模式选项是为避免语义上存在的差异而造成的向后兼容，性能测试以及解决**Mixed-mode inserts**可能产生的问题。

在这种锁定模式下，所有`INSERT-like`的语句都将获得特殊的表级**AUTO-INC锁**，以便将其插入具有AUTO_INCREMENT 列的表中。此锁通常保持在**语句的末尾（而不是事务的末尾）**，以确保为给定的`INSERT`语句序列以可预测和可重复的顺序分配自动增量值，并确保`AUTO_INCREMENT`为任何给定语句分配的值都是连续的。

**对于基于语句的复制**，这意味着在副本服务器上复制SQL语句时，自动增量列使用与源服务器相同的值。执行多个`INSERT`语句的结果是确定性的，并且副本将复制与源上相同的数据。如果对由多个`INSERT`语句生成的自动增量值进行交织，则两个并发`INSERT`语句的结果将是不确定的，并且无法使用基于语句的复制可靠地传播到副本服务器。

为了清楚起见，请考虑使用该表的示例：

```sql
CREATE TABLE t1 (
  c1 INT(11) NOT NULL AUTO_INCREMENT,
  c2 VARCHAR(10) DEFAULT NULL,
  PRIMARY KEY (c1)
) ENGINE=InnoDB;
```

假设有两个事务正在运行，每个事务都将行插入到具有`AUTO_INCREMENT`列的表中。 一个事务使用插入1000行的`INSERT ... SELECT`语句，另一事务使用插入一行的简单`INSERT`语句：

```sql
Tx1: INSERT INTO t1 (c2) SELECT 1000 rows from another table ...
Tx2: INSERT INTO t1 (c2) VALUES ('xxx');
```

`InnoDB` 无法预先告知`Tx1`的`INSERT`语句中从`SELECT`检索了多少行，并且随着语句的进行，它每次为一行分配一个自动递增值。使用**表级锁（保持在该语句的末尾）**，一次只能执行一个引用表`t1`的`INSER`T语句，并且不同语句生成的编号也不是交错的。` Tx1 INSERT ... SELECT`语句生成的自动增量值是连续的，并且`Tx2`中INSERT语句使用的（单个）自动增量值小于或大于用于`Tx1`的所有增量，这具体取决于哪个语句首先执行。

只要从二进制日志重播时，SQL语句以相同的顺序执行（使用基于语句的复制时，或在恢复方案中），结果与`Tx1`和`Tx2`首次运行时的结果相同。因此，在语句结束之前保持的表级锁使使用自动增量的`INSERT`语句可以安全地用于基于语句的复制。但是，当多个事务同时执行`insert`语句时，这些表级锁会限制并发性和可伸缩性。

在前面的示例中，如果没有表级锁定，则`Tx2`中用于`INSERT`的自动增量列的值完全取决于语句执行的时间。如果`Tx2`的`INSERT`在`Tx1`的`INSERT`运行时（而不是在启动之前或完成之后）执行，则两个INSERT语句分配的特定自动增量值是不确定的，并且可能因运行而异。

在**consecutive**锁定模式下，InnoDB可以避免对行数事先已知的**simple insert**语句使用表级`AUTO-INC`锁定，并且仍然为基于语句的复制保留确定性的执行顺序和安全性。

如果您不使用二进制日志来重播SQL语句作为恢复或复制的一部分，则可以使用**interleaved**锁定模式来消除对表级`AUTO-INC`锁定的所有使用，从而获得更大的并发性和性能，但代价是允许语句分配的自动增量编号之间有间隔，并可能使同时执行的语句分配的编号交错。

* `innodb_autoinc_lock_mode = 1` ( **consecutive** lock mode)

在这种模式下，对于所有`INSERT ... SELECT`，`REPLACE ... SELECT`和`LOAD DATA`语句，`bulk inserts`使用特殊的AUTO-INC表级锁并将其保持到语句结束。**一次只能执行一条持有AUTO-INC锁的语句。如果批量插入操作的源表与目标表不同，则在对源表中selects的第一行进行共享锁之后，将对目标表执行AUTO-INC锁。如果批量插入操作的源和目标在同一表中，则在对所有选定行进行共享锁之后，将获取AUTO-INC锁。**

**simple-insert**（预先知道要插入的行数）通过在**互斥锁（轻量级锁）**的控制下获得所需数量（插入多少行分配多少次，无论是否有行自己制定的自增键）的自动增量值来避免表级`AUTO-INC`锁定,而且仅在分配过程中才保持锁，直到语句完成为止。除非另一个事务持有AUTO-INC锁，否则不使用表级`AUTO-INC`锁。如果另一个事务持有`AUTO-INC`锁，则**simple-insert**将等待`AUTO-INC`锁，就好像它是**bulk-insert**一样。

这种锁定模式可确保在实现不知道插入行数的`INSERT语句的情况下`（并且随着语句的进行自动分配编号），所有**insert-like**分配的所有自动递增值语句是连续的，并且操作对于基于语句的复制是安全的。

简而言之，此锁定模式可显着提高可伸缩性，同时可安全地用于基于语句的复制。此外，与**传统**锁定模式一样，任何给定语句分配的自动递增编号都是连续的。对于任何使用自动递增的语句，与**传统**模式相比，语义没有任何变化，但有一个重要的例外。

**混合模式插入**例外，用户在多行`insert`时主动为 AUTO_INCRMEMT 列提供了显式值。对于此类插入，InnoDB分配的`AUTO_INCRMEMT`值的数量大于要插入的行数。但是，所有自动分配的值都是连续生成的（因此高于最近执行的先前语句生成的自动增量值）。**多余出来的值将被丢弃。(意思就是innoDB 分配的自增数还是连续的，但是最大的自增数会大于用户分配的最大值，中间没用到的间隔就被丢弃了)**

* `innodb_autoinc_lock_mode = 2` (**interleaved** lock mode)

在这种锁定模式下，**INSERT-like**语句不使用表级别的AUTO-INC锁定，并且可以同时执行多个语句。这是最快，最具扩展性的锁定模式，但是当使用基于语句的复制或恢复方案从二进制日志中重播SQL语句时，这是不安全的。

在这种锁定模式下，保证自动增量值是唯一的，并且在所有同时执行的**INSERT-like**语句中单调递增。但是，由于多个语句可以同时生成数字（也就是说，在语句之间交错分配数字），因此为任何给定语句插入的行生成的值可能不是连续的。

如果仅执行的语句是提前知道要插入的行数的**simple-insert**，则为单个语句生成的数字没有任何间隙。（**mixed-insert** 锁模式例外外）。当执行**bulk-insert**时，任何给定语句分配的自动增量值可能存在间隙。

# InnoDB AUTO_INCREMENT锁定模式的用法含义

* 在复制中使用自动增量

如果使用的是基于语句的复制，请将`innodb_autoinc_lock_mode`设置为0或1，并在源及其副本上使用相同的值。如果使用`innodb_autoinc_lock_mode = 2（interleaved`）或源和副本不使用相同锁定模式的配置，则不能确保副本上的自动增量值与源副本上的自动增量值相同。

如果您使用的是基于行的复制或混合格式的复制，则所有自动增量锁定模式都是安全的，因为基于行的复制对SQL语句的执行顺序不敏感（并且混合格式对基于语句的复制不安全的所有语句都使用基于行的复制）。

* **丢失**的自动增量值和序列间隔

  在所有锁定模式（0、1和2）下，如果生成自动增量值的事务回滚，则这些自动增量值将**丢失**。一旦为自动增量列生成了一个值，就无法回滚该值，无论**INSERT-like**语句是否完成以及包含的事务是否回滚。这种丢失的值不会被重复使用。因此，在表的`AUTO_INCREMENT`列中存储的值中可能存在间隙。

* 为`AUTO_INCREMENT`列指定 NULL 或 0

在所有锁定模式（0、1和2）中，如果用户为INSERT中的AUTO_INCREMENT列指定NULL或0，则InnoDB会将行视为未指定值，并为其生成新值。

* 为`AUTO_INCREMENT`列分配一个负值

在所有锁定模式（0、1和2）中，自动递增机制没有定义将负值分配给AUTO_INCREMENT列的行为（`报错：ERROR 1264 (22003): Out of range value for column 'id' at row 1`）

* 如果AUTO_INCREMENT值大于指定整数类型的最大整数

在所有锁定模式（0、1和2）中，自动递增机制没有定义如果该值变得大于可以以指定整数类型存储的最大整数时的行为(`报错：ERROR 1264 (22003): Out of range value for column 'id' at row 1`)

* **bulk-insert**的自动增量值的间隙

在将`innodb_autoinc_lock_mode`设置为0(**traditional**)，或1(**consecutive**)的情况下，任何给定语句生成的自动增量值都是连续的，没有间隙，因为表级`AUTO-INC`锁一直保持到语句末尾。并且一次只能执行一个这样的语句。

在将`innodb_autoinc_lock_mode`设置为2（**interleaved**）的情况下，**bulk-insert**生成的自动增量值可能存在间隙，但前提是必须同时执行**insert-like**语句。

对于锁定模式1或2，在连续的语句之间可能会出现间隙，因为对于批量插入，可能不知道每个语句所需的自动递增值的确切数量，并且分配的数量可能会高估。

* 由**mixed lock mode**分配的自动增量值

考虑一个**mixed mode** 插入，其中**simple-insert**指定一些（但不是全部）结果行的自动增量值。这样的语句在锁定模式0、1和2下的行为不同。例如，假定c1是表t1的`AUTO_INCREMENT`列，并且最近自动生成的序列号是100。

```sql
mysql> CREATE TABLE t1 (
    -> c1 INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY, 
    -> c2 CHAR(1)
    -> ) ENGINE = INNODB;	
```

现在，考虑以下**mixed lock mode**语句：

```sql
mysql> INSERT INTO t1 (c1,c2) VALUES (1,'a'), (NULL,'b'), (5,'c'), (NULL,'d');
```

在将`innodb_autoinc_lock_mode`设置为0（**传统**）的情况下，四个新行分别是：

```	sql
mysql> SELECT c1, c2 FROM t1 ORDER BY c2;
+-----+------+
| c1  | c2   |
+-----+------+
|   1 | a    |
| 101 | b    |
|   5 | c    |
| 102 | d    |
+-----+------+	
```

下一个可用的自动递增值是103，因为自动递增值一次分配一次，而不是在语句执行开始时一次分配一次。 无论是否同时执行**INSERT-like**语句（任何类型），此结果都是正确的。

在将·innodb_autoinc_lock_mode·设置为1（**consecutive**）的情况下，四个新行也是：

```sql
mysql> SELECT c1, c2 FROM t1 ORDER BY c2;
+-----+------+
| c1  | c2   |
+-----+------+
|   1 | a    |
| 101 | b    |
|   5 | c    |
| 102 | d    |
+-----+------+
```

但是，在这种情况下，下一个可用的自动递增值是105，而不是103，因为在处理语句时分配了四个自动递增值，但只使用了两个。 无论是否同时执行`INSERT-like`语句（任何类型），此结果都是正确的。

在将innodb_autoinc_lock_mode设置为模式2（**traditional**）的情况下，四个新行是：

```sql
mysql> SELECT c1, c2 FROM t1 ORDER BY c2;
+-----+------+
| c1  | c2   |
+-----+------+
|   1 | a    |
|   x | b    |
|   5 | c    |
|   y | d    |
+-----+------+
```

x和y的值是唯一的，并且比以前生成的任何行大。 但是，x和y的特定值取决于同时执行语句生成的自动增量值的数量。

最后，考虑以下语句，该语句是在最近生成的序列号为100时发出的：

```sql
mysql> INSERT INTO t1 (c1,c2) VALUES (1,'a'), (NULL,'b'), (101,'c'), (NULL,'d');	
```

在任何innodb_autoinc_lock_mode设置下，此语句都会生成重复键错误23000（`Can't write; duplicate key in table`)，因为为行（NULL，'b'）和行的插入（101，'c'）分配了101 造成失败。

* 在INSERT语句序列的中间修改AUTO_INCREMENT列值

在MySQL 5.7和更早版本中，在一系列INSERT语句的中间修改AUTO_INCREMENT列值可能会导致**重复输入**错误。 例如，如果执行UPDATE操作将`AUTO_INCREMENT`列值更改为大于当前最大自动增量值的值，则后续未指定未使用的自动增量值的INSERT操作可能会遇到**重复输入**错误。 在MySQL 8.0和更高版本中，如果将AUTO_INCREMENT列值修改为大于当前最大自动增量值的值，则将保留新值，并且随后的INSERT操作将从较大的新值开始分配自动增量值。 在下面的示例中演示了此行为。

```sql
mysql> CREATE TABLE t1 (
    -> c1 INT NOT NULL AUTO_INCREMENT,
    -> PRIMARY KEY (c1)
    ->  ) ENGINE = InnoDB;

mysql> INSERT INTO t1 VALUES(0), (0), (3);

mysql> SELECT c1 FROM t1;
+----+
| c1 |
+----+
|  1 |
|  2 |
|  3 |
+----+

mysql> UPDATE t1 SET c1 = 4 WHERE c1 = 1;

mysql> SELECT c1 FROM t1;
+----+
| c1 |
+----+
|  2 |
|  3 |
|  4 |
+----+

mysql> INSERT INTO t1 VALUES(0);

mysql> SELECT c1 FROM t1;
+----+
| c1 |
+----+
|  2 |
|  3 |
|  4 |
|  5 |
+----+
```

# InnoDB AUTO_INCREMENT计数器初始化

本节介绍InnoDB如何初始化AUTO_INCREMENT计数器。

如果为InnoDB表指定AUTO_INCREMENT列，则在内存表对象中将包含一个称为自动增量计数器的特殊计数器，该计数器在为该列分配新值时使用。

在MySQL 5.7和更早版本中，自动递增计数器仅存储在主存储器中，而不存储在磁盘上。 要在服务器重启后初始化自动增量计数器，InnoDB将在包含表AUTO_INCREMENT的表的第一个插入处执行以下语句的等效项。

```sql
SELECT MAX(ai_col) FROM table_name FOR UPDATE;
```

在MySQL 8.0中，此行为已更改。每次更改时，当前最大自动增量计数器值都会写入`redo`日志，并在每个检查点保存到引擎专用系统表中（8.0 的数据字典，之前的 schema 表）。这些更改使当前的最大自动增量计数器值在服务器重新启动后保持不变。

在正常关机后重新启动服务器时，InnoDB使用存储在数据字典系统表中的当前最大自动增量值初始化内存中自动增量计数器。

在崩溃恢复期间重启服务器时，InnoDB使用存储在数据字典系统表中的当前最大自动增量值初始化内存中自动增量计数器，并扫描`redo`日志以查找自上一个检查点以来写入的自动增量计数器值。如果重做日志记录的值大于内存中计数器的值，则应用`redo`日志记录的值。但是，如果服务器意外退出，则不能保证重新使用先前分配好的的自动增量值。每次由于INSERT或UPDATE操作而更改当前最大自动增量值时，都会将新值写入重做日志，但是如果在重做日志刷新到磁盘之前发生意外退出，则之前分配的自增值可能在服务重启之后被重新使用（**这样会不会发生重复键错误？**）。

InnoDB使用`SELECT MAX（ai_col）FROM table_name FOR UPDATE`语句的等效项来初始化自动增量计数器的唯一情况是在导入不带`.cfg`元数据文件的表时。否则，从.cfg元数据文件中读取当前的最大自动增量计数器值（如果存在）。除了初始化计数器值外，当尝试使用·`ALTER TABLE ... AUTO_INCREMENT = N FOR UPDATE`语句将计数器值设置为小于或等于当前最大自增值的时候，使用`SELECT MAX（ai_col）FROM table_name`语句的等效项用于确定表的当前最大**有效**自动增量计数器值。例如，您可以在删除某些记录后尝试将计数器值设置为较小的值。在这种情况下，必须搜索该表以确保新的计数器值不小于或等于实际的当前最大计数器值。

在MySQL 5.7和更早版本中，服务器重新启动会取消使用`CREATE TABLE或ALTER TABLE`语句中分别用于设置初始计数器值或更改现有计数器值`AUTO_INCREMENT = N`表选项的影响(**之前不论设置从几开始自增的重启后直接从1开始自增**)，该选项可在。在MySQL 8.0中，服务器重新启动不会取消`AUTO_INCREMENT = N`表选项的影响。如果将自动递增计数器初始化为特定值，或者将自动递增计数器值更改为较大的值，则新值将在服务器重新启动后保留。

> **注意：**
> ALTER TABLE ... AUTO_INCREMENT = N只能将自动递增计数器的值更改为大于当前最大值的值。

在MySQL 5.7和更早版本中，服务器在`ROLLBACK`操作之后立即重新启动可能会导致重用以前分配给回滚事务的自动增量值，从而有效地回滚当前的最大自动增量值。在MySQL 8.0中，当前的最大自动增量值得以保留，从而防止了先前分配的值的重用。

如果在初始化自动递增计数器之前使用`SHOW TABLE STATUS`语句检查了一个表，则InnoDB会打开该表并使用存储在数据字典系统表中的当前最大自动递增值来初始化计数器值。该值存储在内存中，供以后的插入或更新使用。计数器值的初始化使用对表的普通互斥锁定读取，该读取持续到事务结束。初始化新创建的表的自动递增计数器时，InnoDB遵循相同的过程，该表的用户指定自动递增值大于0。

初始化自动递增计数器后，如果在插入行时未明确指定自动递增值，则InnoDB隐式递增计数器并将新值分配给该列。如果插入明确指定自动递增列值的行，并且该值大于当前最大计数器值，则该计数器将设置为指定值。

只要服务器运行，InnoDB就会使用内存中的自动增量计数器。如前所述，当服务器停止并重新启动时，InnoDB会重新初始化自动增量计数器。

`auto_increment_offset`配置选项确定AUTO_INCREMENT列值的起点。默认设置为1。

`auto_increment_increment`配置选项控制连续列值之间的间隔。默认设置为1。

> **注意：**
> 当AUTO_INCREMENT整数列的值用完时，随后的INSERT操作将返回重复键错误。这是一般的MySQL行为。