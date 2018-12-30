## Method

```java
public final class Method extends Executable
```

### java doc

方法提供有关类或接口上的单个方法的信息和访问权限。 反射的方法可以是类方法或实例方法（包括抽象方法）。
方法允许在将实际参数与基础方法的形式参数进行匹配时进行扩展转换，但如果发生缩小转换，则会抛出IllegalArgumentException。

![反射类图](https://github.com/TransientWang/KnowledgeBase/tree/master/picture/Method类图.png)

先看一下类图，最顶层的接口AnnotatedElement，是reflect包中所有类的顶层接口。作用是获取成员的注解信息。GenericDeclaration是声明类型变量的所有实体的通用接口，用于获取方法的参数信息。Member接口定义了获取成员的修饰符，名称，声明类，是否是由编译器引入的方法（由程序员编写，而不是jdk本身提供的）。

AccessibleObject类是Field，Method和Constructor对象的基类（称为反射对象）。它提供了将反射对象标记为在使用时禁止检查Java语言访问控制的功能。这允许具有足够权限的复杂应用程序（例如Java对象序列化或其他持久性机制）以通常被禁止的方式操纵对象。
Java语言访问控制可防止在顶级类之外使用私有成员;在包外访问只有包权限的成员;受保护的成员在他们的包或子类之外;除非在导出的包中声明并且用户读取其模块，否则他们的模块外部的公共成员。默认情况下，当使用Fields，Methods或Constructors获取或设置字段，调用方法或创建和初始化类的新实例时，将强制执行Java语言访问控制（使用一种变体）。每个反射对象都会检查使用它的代码是否在适当的类，包或模块中。

Java语言访问控制的一个变体是反射对象的检查假定可读性。也就是说，假定包含使用反射对象的模块读取声明了基础字段，方法或构造函数的模块。

是否可以抑制对Java语言访问控制的检查（因此，是否可以启用访问）取决于反射对象是否对应于导出或打开包中的成员（请参阅`setAccessible（boolean）`）。

Executable是Method和Constructor的共享超类。

### 反射执行方法原理分析

需要了解的字段

```java
/**
 * 此接口提供声明 java.lang.reflect.Method.invoke（）。
 * 每个Method对象都配置了一个（可能是动态生成的）类来实现此接口。
 **/
private volatile MethodAccessor methodAccessor;
// 用于共享MethodAccessors。 这个分支结构目前只有两层深
// （即一个根方法和可能有许多指向它的Method对象。）
//
//如果此分支结构将包含循环，则注释代码中可能会发生死锁。
private Method              root;
```

root对象在调用`getDeclaredMethod`获取Method对象的时候通过Method的`copy()`方法赋值的，用于方便的切换两种不同版本的。

### invoke

```java
public Object invoke(Object obj, Object... args)
    throws IllegalAccessException, IllegalArgumentException,
       InvocationTargetException
{
    if (!override) {
        Class<?> caller = Reflection.getCallerClass();
        checkAccess(caller, clazz,
                    Modifier.isStatic(modifiers) ? null : obj.getClass(),
                    modifiers);
    }
    MethodAccessor ma = methodAccessor;             // read volatile
    if (ma == null) {
        ma = acquireMethodAccessor();
    }
    return ma.invoke(obj, args);
}
```

首先进行权限检查，`override`字段属于accessableObject，如果为false。我们就不能访问权限修饰符为私有的成员。可以通过`setAccessible`修改这个字段。然后获取MethodAccessor对象，这个对象是真正执行方法调用的对象。：

```java
private MethodAccessor acquireMethodAccessor() {
    // First check to see if one has been created yet, and take it
    // if so
    MethodAccessor tmp = null;
    if (root != null) tmp = root.getMethodAccessor();
    if (tmp != null) {
        methodAccessor = tmp;
    } else {
        // Otherwise fabricate one and propagate it up to the root
        tmp = reflectionFactory.newMethodAccessor(this);
        setMethodAccessor(tmp);
    }

    return tmp;
}
```

如果root不为null,则尝试通过root获取MethodAccessor。如果获取到则通过反射工厂创建一个MethodAccessor对象。

```java
public MethodAccessor newMethodAccessor(Method method) {
    checkInitted();

    if (Reflection.isCallerSensitive(method)) {
        Method altMethod = findMethodForReflection(method);
        if (altMethod != null) {
            method = altMethod;
        }
    }

    // use the root Method that will not cache caller class
    Method root = langReflectAccess.getRoot(method);
    if (root != null) {
        method = root;
    }

    if (noInflation && !ReflectUtil.isVMAnonymousClass(method.getDeclaringClass())) {
        return new MethodAccessorGenerator().
            generateMethod(method.getDeclaringClass(),
                           method.getName(),
                           method.getParameterTypes(),
                           method.getReturnType(),
                           method.getExceptionTypes(),
                           method.getModifiers());
    } else {
        NativeMethodAccessorImpl acc =
            new NativeMethodAccessorImpl(method);
        DelegatingMethodAccessorImpl res =
            new DelegatingMethodAccessorImpl(acc);
        acc.setParent(res);
        return res;
    }
}
```

`isCallerSensitive`测试给定方法是否对调用者敏感，并且声明类是由引导类加载器还是平台类加载器定义的，正常情况下返回false。如果不设置通货膨胀机制并且检查Class 不是由` jdk.internal.misc.Unsafe＃defineAnonymousClass`定义的VM匿名类（不要与Java语言匿名内部类混淆）。则通过MethodAccessorGenerator创建字节码模式的MethodAccessor，否则创建native版本的MethodAccessor。

```java
class DelegatingMethodAccessorImpl extends MethodAccessorImpl {
    private MethodAccessorImpl delegate;

    DelegatingMethodAccessorImpl(MethodAccessorImpl delegate) {
        setDelegate(delegate);
    }

    public Object invoke(Object obj, Object[] args)
        throws IllegalArgumentException, InvocationTargetException
    {
        return delegate.invoke(obj, args);
    }

    void setDelegate(MethodAccessorImpl delegate) {
        this.delegate = delegate;
    }
}
```

实现类DelegatingMethodAccessorImpl，里面唯一的一个字段`MethodAccessorImpl delegate;`在`invoke`的实现方法里调用`delegate.invoke`，这是很明显的代理模式。

```java
class NativeMethodAccessorImpl extends MethodAccessorImpl {
    private final Method method;
    private DelegatingMethodAccessorImpl parent;
    private int numInvocations;

    NativeMethodAccessorImpl(Method method) {
        this.method = method;
    }

    public Object invoke(Object obj, Object[] args)
        throws IllegalArgumentException, InvocationTargetException
    {
        // 我们不能对属于vm-anonymous类的方法进行扩充，因为这种类不
        // 能通过名称引用，因此无法从生成的字节码中找到。
        if (++numInvocations > ReflectionFactory.inflationThreshold()
                && !ReflectUtil.isVMAnonymousClass(method.getDeclaringClass())) {
            MethodAccessorImpl acc = (MethodAccessorImpl)
                new MethodAccessorGenerator().
                    generateMethod(method.getDeclaringClass(),
                                   method.getName(),
                                   method.getParameterTypes(),
                                   method.getReturnType(),
                                   method.getExceptionTypes(),
                                   method.getModifiers());
            parent.setDelegate(acc);
        }

        return invoke0(method, obj, args);
    }

    void setParent(DelegatingMethodAccessorImpl parent) {
        this.parent = parent;
    }

    private static native Object invoke0(Method m, Object obj, Object[] args);
}
```

NativeMethodAccessorImpl做为在反射调用方法时候的native版本实现。仅用于Method的前几次调用; 之后，切换到基于字节码的实现。numInvocations用于实现通过膨胀机制。通货膨胀机制的控制开关在反射工厂中。

```java

// “通货膨胀”机制。 加载字节码以实现Method.invoke（）和Constructor
// .newInstance（）目前的成本比第一次调用的本机代码多出3-4倍（尽管后
// 续调用的基准测试速度超过了20倍）。 不幸的是，这种成本增加了某些应
// 用程序的启动时间，这些应用程序集中使用反射（但每个类只有一次）来
// 自我引导。 为了避免这种损失，我们在方法和构造函数的前几次调用中重
// 用现有的JVM入口点，然后切换到基于字节码的实现。
 
//可以访问的包私有 NativeMethodAccessorImpl和NativeConstructorAccessorImpl
private static boolean noInflation        = false;
private static int     inflationThreshold = 15;
```

可以使用VM参数关闭通过膨胀机制（sun.reflect.noInflation）或者修改阈值（sun.reflect.inflationThreshold）。这两个参数名称可以在反射工厂类中看到。如果调用反射机制超过了阈值，则MethodAccessorGenerator创建字节码版本的MethodAccessor。然后通过委托类替换真正执行的MethodAccessorMethodAccessor。

另外这两个版本的执行中还涉及到了[内联](https://blog.csdn.net/ke_weiquan/article/details/51946174)。想了解更多可以查看[关于反射的一个log](https://rednaxelafx.iteye.com/blog/548536)