## Spring 事物

```java
public interface TransactionDefinition {
```

定义符合 Spring 的事务属性的接口。 基于类似于EJB CMT属性的传播行为定义。
请注意，除非启动实际的新事务，否则不会应用隔离级别和超时设置。 由于只有PROPAGATION_REQUIRED，PROPAGATION_REQUIRES_NEW和PROPAGATION_NESTED会导致这种情况，因此在其他情况下指定这些设置通常没有意义。 此外，请注意，并非所有事务管理器都支持这些高级功能，因此在给定非默认值时可能会抛出相应的异常。

只读标志适用于任何事务上下文，无论是由实际资源事务支持还是在资源级别以非事务方式操作。 在后一种情况下，该标志仅适用于应用程序内的受管资源，例如Hibernate会话。

| Modifier and Type | Field and Description                                        |
| ----------------- | ------------------------------------------------------------ |
| `static int`      | `ISOLATION_DEFAULT` 使用基础数据存储的默认隔离级别。         |
| `static int`      | `ISOLATION_READ_COMMITTED`表示禁止脏读;可以发生不可重复的读取和幻像读取。 |
| `static int`      | `ISOLATION_READ_UNCOMMITTED`表示可能发生脏读，不可重复读和幻像读。 |
| `static int`      | `ISOLATION_REPEATABLE_READ`表示防止脏读和不可重复读; 可以发生幻像读取。 |
| `static int`      | `ISOLATION_SERIALIZABLE`表示防止脏读，不可重复读和幻像读。   |
| `static int`      | `PROPAGATION_MANDATORY`支持当前交易; 如果不存在当前事务则抛出异常。 |
| `static int`      | `PROPAGATION_NESTED`如果当前事务存在，则在嵌套事务中执行，其行为类似于PROPAGATION_REQUIRED 。 |
| `static int`      | `PROPAGATION_NEVER`不支持当前交易; 如果当前事务存在则抛出异常。 |
| `static int`      | `PROPAGATION_NOT_SUPPORTED`不支持当前交易; 而是总是以非交易方式执行。 |
| `static int`      | `PROPAGATION_REQUIRED`支持当前交易; 如果不存在则创建一个新的。 |
| `static int`      | `PROPAGATION_REQUIRES_NEW`创建一个新事务，暂停当前事务（如果存在）。 |
| `static int`      | `PROPAGATION_SUPPORTS`支持当前交易; 如果不存在则执行非事务性。 |
| `static int`      | `TIMEOUT_DEFAULT`使用基础事务系统的默认超时，如果不支持超时，则使用none。 |

| Modifier and Type  | Method and Description                 |
| ------------------ | -------------------------------------- |
| `int`              | `getIsolationLevel()`返回隔离级别      |
| `java.lang.String` | `getName()`返回事物名                  |
| `int`              | `getPropagationBehavior()`返回传播行为 |
| `int`              | `getTimeout()`返回事物超时             |
| `boolean`          | `isReadOnly()`返回是否优化为只读事务。 |

## SavepointManager

```java
SavepointManagerpublic interface SavepointManager {
```

指定API的接口，以通用方式以编程方式管理事务保存点。 通过TransactionStatus进行扩展，以显示特定事务的保存点管理功能。
请注意，保存点只能在活动事务中工作。 只需使用此程序化保存点处理即可满足高级需求; 否则，最好使用PROPAGATION_NESTED进行子事务。

此接口受JDBC 3.0的Savepoint机制启发，但独立于任何特定的持久性技术。

| `java.lang.Object` | `createSavepoint()`创建一个保存点                            |
| ------------------ | ------------------------------------------------------------ |
| `void`             | `releaseSavepoint(java.lang.Object savepoint)`显式释放给定的保存点。 |
| `void`             | `rollbackToSavepoint(java.lang.Object savepoint)`回滚到给定的保存点。 |

### TransactionStatus

```java
public interface TransactionStatus extends SavepointManager, Flushable {
```

表示事物状态。
**事务代码可以使用它来检索状态信息，并以编程方式请求回滚**（而不是抛出导致隐式回滚的异常）。

包含SavepointManager接口，以提供对保存点管理工具的访问。 请注意，只有在基础事务管理器支持的情况下，保存点管理才可用。

| Modifier and Type | Method and Description                                       |
| ----------------- | ------------------------------------------------------------ |
| `void`            | `flush()`如果适用，将基础会话刷新到数据存储区：例如， 对 Hibernate / JPA会话有影响。对jdbc 无影响。 |
| `boolean`         | `hasSavepoint()`返回此事务是否在内部携带保存点，即基于保存点创建为嵌套事务。 |
| `boolean`         | `isCompleted()`返回此事务是否已完成，即是否已提交或回滚。    |
| `boolean`         | `isNewTransaction()`返回当前交易是否是新的; 否则参与现有事物，或者可能不会首先在实际事物中运行。 |
| `boolean`         | `isRollbackOnly()`返回事务是否已标记为仅回滚（由应用程序或事务基础结构）。 |
| `void`            | `setRollbackOnly()`Set the transaction rollback-only.        |



### PlatformTransactionManager

```java
public interface PlatformTransactionManager {
```

`PlatformTransactionManager` 是其他orm 框架为了，与Spring 整合而应该实现的接口。

这是 Spring 的事务基础结构的中心接口。 应用程序可以直接使用它，但它主要不是API：通常，应用程序可以使用TransactionTemplate或通过AOP进行声明式事务划分。
对于实现者，建议从提供的AbstractPlatformTransactionManager类派生，该类预先实现定义的传播行为并负责事务同步处理。 子类必须为底层事务的特定状态实现模板方法，例如：begin，suspend，resume，commit。

此策略接口的默认实现是JtaTransactionManager和DataSourceTransactionManager，它们可以作为其他事务策略的实现指南。

| Modifier and Type   | Method and Description                                       |
| ------------------- | ------------------------------------------------------------ |
| `void`              | `commit(TransactionStatus status)`就给定事务的状态提交给定事务。 |
| `TransactionStatus` | `getTransaction(TransactionDefinition definition)`根据指定的传播行为，返回当前活动的事务或创建新事务。 |
| `void`              | `rollback(TransactionStatus status)`执行给定事务的回滚。     |

### DataSourceTransactionManager

单个JDBC 或 ibatis DataSource 的 PlatformTransactionManager实现。只要安装程序使用javax.sql.DataSource 作为其连接工厂机制，此类就能够在任何具有任何JDBC驱动程序的环境中工作。将JDBC连接从指定的DataSource绑定到当前线程，可能允许每个DataSource一个线程绑定的Connection。
注意：此事务管理器操作的DataSource需要返回独立的Connections。 Connections可能来自池（典型情况），但DataSource不能返回线程范围/请求范围的Connections等。此事务管理器将根据指定的传播行为将Connections与线程绑定事务本身关联。它假定即使在正在进行的交易中也可以获得单独的独立连接。

需要应用程序代码来通过DataSourceUtils.getConnection（DataSource）而不是标准的Java EE样式的DataSource.getConnection（）调用来检索JDBC Connection。诸如JdbcTemplate之类的Spring类隐式使用此策略。如果不与此事务管理器结合使用，则DataSourceUtils查找策略的行为与本机DataSource查找完全相同;因此它可以以便携方式使用。

或者，您可以允许应用程序代码使用标准Java EE样式的查找模式DataSource.getConnection（），例如，对于根本不了解Spring的遗留代码。在这种情况下，为目标DataSource定义一个TransactionAwareDataSourceProxy，并将该代理DataSource传递给您的DAO，它们在访问时将自动参与Spring管理的事务。

支持自定义隔离级别，以及作为适当的JDBC语句超时应用的超时。要支持后者，应用程序代码必须使用JdbcTemplate，为每个创建的JDBC语句调用DataSourceUtils.applyTransactionTimeout（java.sql.Statement，javax.sql.DataSource），或者通过TransactionAwareDataSourceProxy创建超时感知JDBC连接和语句自动。

考虑为目标DataSource定义LazyConnectionDataSourceProxy，将此事务管理器和DAO指向它。这将导致“空”事务的优化处理，即没有执行任何JDBC语句的事务。在执行Statement之前，LazyConnectionDataSourceProxy不会从目标DataSource获取实际的JDBC连接，而是懒惰地将指定的事务设置应用于目标Connection。

此事务管理器通过JDBC 3.0 Savepoint机制支持嵌套事务。 “nestedTransactionAllowed”标志默认为“true”，因为嵌套事务对包含保存点的JDBC驱动程序（例如Oracle JDBC驱动程序）没有限制。

此事务管理器可用作单个资源情况下JtaTransactionManager的替代，因为它不需要支持JTA的容器，通常与本地定义的JDBC DataSource（例如Apache Commons DBCP连接池）结合使用。在本地策略和JTA环境之间切换只是配置问题！

从4.3.4开始，此事务管理器触发已注册事务同步的刷新回调（如果同步通常处于活动状态），假设资源在基础JDBC连接上运行。这允许类似于JtaTransactionManager的设置，特别是关于懒惰注册的ORM资源（例如，Hibernate会话）。