## Runnable	

```java
public interface Runnable
```

### java doc 

Runnable接口应由任何其实例由线程执行的类实现。 该类必须定义一个没有参数的方法，称为run。
此接口旨在为希望在活动时执行代码的对象提供通用协议。 例如，Runnable由Thread类实现。 活动只是意味着一个线程已经启动但尚未停止。

另外，Runnable提供了一个类活动而不是继承Thread的方法。 实现Runnable的类可以在没有子类化Thread的情况下运行，方法是实例化一个Thread实例并将其自身作为目标传递。 在大多数情况下，如果您只计划覆盖run（）方法而不使用其他Thread方法，则应使用Runnable接口。 这很重要，因为除非程序员打算修改或增强类的基本行为，否则不应对类进行子类化。





```java
//当使用实现接口Runnable的对象来创建线程时，启动该线程会导致在该单独执行的线程中调用该对象的run方法。
//方法运行的一般合同是它可以采取任何行动。
public abstract void run();
```

### 总结

Runnable接口是创建线程的最基本的实现。只有一个run()方法。如果想创建一个线程，可以重写该接口，并将实例传递给Thread类，并且调用Thread类的start()方法。