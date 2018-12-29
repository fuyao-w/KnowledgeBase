## AtomicInteger

```java
public class AtomicInteger extends Number implements java.io.Serializable 
```

### java doc

可以原子方式更新的int值。 有关原子访问属性的描述，请参阅VarHandle规范。 AtomicInteger用于诸如原子递增计数器的应用程序中，不能用作Integer的替代。 但是，此类确实扩展了Number，以允许通过处理基于数字的类的工具和实用程序进行统一访问。