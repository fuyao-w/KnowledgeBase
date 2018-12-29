## VarHandle

VarHandle是对变量或参数定义的变量系列的动态强类型引用，包括静态字段，非静态字段，数组元素或堆外数据结构的组件。 在各种访问模式下都支持访问这些变量，包括普通读/写访问，易失性读/写访问以及CAS。
VarHandles是不可变的，没有可见状态。 VarHandles不能被用户子类化。

VarHandle有：

- 变量类型T，VarHandle引用的每个变量的类型; 
- 和坐标类型列表CT1，CT2，...，CTn，共同定位此VarHandle引用的变量的坐标表达式的类型。

变量和坐标类型可以是原始类型或引用类型，并由Class对象表示。坐标类型列表可以为空。
生成或查找VarHandle实例的工厂方法记录了支持的变量类型和坐标类型列表。

每种访问模式都与一种访问模式方法相关联，这是一种以访问模式命名的签名多态方法。当在VarHandle实例上调用访问模式方法时，调用的初始参数是坐标表达式，它们精确地指示要访问变量的对象。调用的尾随参数表示访问模式的重要值。例如，各种比较和设置或比较和交换访问模式需要两个尾随参数用于变量的预期值和新值。

不会静态检查调用访问模式方法的arity和参数类型。相反，每种访问模式方法都指定一种访问模式类型，表示为MethodType的一个实例，它充当一种方法签名，动态检查参数。访问模式类型根据VarHandle实例的坐标类型和访问模式的重要值类型给出形式参数类型。访问模式类型也提供返回类型，通常根据VarHandle实例的变量类型。在VarHandle实例上调用访问模式方法时，调用站点的符号类型描述符，调用的运行时参数类型以及返回值的运行时类型必须与访问模式中给出的类型匹配类型。如果匹配失败，将抛出运行时异常。例如，访问模式方法compareAndSet（java.lang.Object ...）指定如果其接收者是具有坐标类型CT1，...，CTn和变量类型T的VarHandle实例，则其访问模式类型为（CT1） c1，...，CTn cn，T expectedValue，T newValue）boolean。假设VarHandle实例可以访问数组元素，并且其坐标类型是String []和int，而其变量类型是String。此VarHandle实例上compareAndSet的访问模式类型为（String [] c1，int c2，String expectedValue，String newValue）boolean。这样的VarHandle实例可能由数组工厂方法和访问数组元素产生，如下所示：

```java
 String[] sa = ...
 VarHandle avh = MethodHandles.arrayElementVarHandle(String[].class);
 boolean r = avh.compareAndSet(sa, 10, "expected", "new");
```

访问模式控制原子性和一致性属性。 普通读（get）和写（set）访问保证仅对于引用和最多32位的原始值是按位原子的，并且对于除执行线程之外的线程没有强加可观察的排序约束。 对于访问同一变量，不透明操作是按位原子和相干有序的。 除了遵守不透明属性之外，在匹配释放模式写入及其先前的访问之后，还会对获取模式读取及其后续访问进行排序。 除了遵守Acquire和Release属性之外，所有Volatile操作都是相互完全排序的。

访问模式分为以下几类：

- 读取访问模式，获取指定内存排序效果下的变量值。属于该组的相应访问模式方法的集合包括方法get，getVolatile，getAcquire，getOpaque。
- 写入访问模式，在指定的内存排序效果下设置变量的值。属于该组的相应访问模式方法的集合包括方法set，setVolatile，setRelease，setOpaque。
- 原子更新访问模式，例如，在指定的内存排序效果下，原子地比较和设置变量的值。属于该组的相应访问模式方法的集合包括方法compareAndSet，weakCompareAndSetPlain，weakCompareAndSet，weakCompareAndSetAcquire，weakCompareAndSetRelease，compareAndExchangeAcquire，compareAndExchange，compareAndExchangeRelease，getAndSet，getAndSetAcquire，getAndSetRelease。
- 数字原子更新访问模式，例如，通过在指定的内存排序效果下添加变量的值，以原子方式获取和设置。属于该组的相应访问模式方法集包括方法getAndAdd，getAndAddAcquire，getAndAddRelease，
- 按位原子更新访问模式，例如，在指定的内存排序效果下，以原子方式获取和按位OR变量的值。属于该组的相应访问模式方法的集合包括方法getAndBitwiseOr，getAndBitwiseOrAcquire，getAndBitwiseOrRelease，getAndBitwiseAnd，getAndBitwiseAndAcquire，getAndBitwiseAndRelease，getAndBitwiseXor，getAndBitwiseXorAcquire，getAndBitwiseXorRelease。

生成或查找VarHandle实例的工厂方法记录了所支持的访问模式集，其中还可能包括基于变量类型记录限制以及变量是否为只读。如果不支持访问模式，那么相应的访问模式方法将在调用时抛出UnsupportedOperationException。工厂方法应记录访问模式方法可能引发的任何其他未声明的异常。所有VarHandle实例都支持get访问模式，相应的方法永远不会抛出UnsupportedOperationException。如果VarHandle引用只读变量（例如最终字段），则不支持写入，原子更新，数字原子更新和按位原子更新访问模式，并且相应的方法抛出UnsupportedOperationException。读/写访问模式（如果支持），get和set除外，为引用类型和所有原始类型提供原子访问。除非在工厂方法的文档中另有说明，否则访问模式get和set（如果支持）为引用类型和所有基元类型提供原子访问，但32位平台上的long和double除外。

访问模式将覆盖变量声明站点上指定的任何内存排序效果。例如，使用get访问模式访问字段的VarHandle将访问由其访问模式指定的字段，即使该字段被声明为volatile。当执行混合访问时，应特别小心，因为Java内存模型可能会产生令人惊讶的结果。

除了支持在各种访问模式下访问变量之外，还提供了一组静态方法，称为内存栅栏方法，用于细粒度控制内存排序。 Java语言规范允许其他线程观察操作，好像它们是以不同于程序源代码中明显的顺序执行的，受到例如使用锁，易失性字段或VarHandles的限制。静态方法fullFence，acquireFence，releaseFence，loadLoadFence和storeStoreFence也可用于施加约束。与某些访问模式的情况一样，它们的规范是根据缺乏“重新排序”来表达的 - 如果不存在围栏，则可能出现可观察到的排序效应。访问模式方法和存储器范围方法的规范的更精确的措辞可以伴随Java语言规范的未来更新。

## Compiling invocation of access mode methods

命名访问模式方法的Java方法调用表达式可以从Java源代码调用VarHandle。从源代码的角度来看，这些方法可以接受任何参数，并且它们的多态结果（如果表达的话）可以转换为任何返回类型。形式上，这是通过为访问模式方法提供变量arity Object参数和Object返回类型（如果返回类型是多态的）来实现的，但是它们具有称为签名多态的附加质量，它将这种调用自由直接连接到JVM执行堆栈。
与通常的虚方法一样，对访问模式方法的源级调用将编译为invokevirtual指令。更不寻常的是，编译器必须记录实际的参数类型，并且可能不会对参数执行方法调用转换。相反，它必须根据自己未转换的类型生成将它们压入堆栈的指令。 VarHandle对象本身将在参数之前被压入堆栈。然后，编译器生成一个invokevirtual指令，该指令使用描述参数和返回类型的符号类型描述符来调用访问模式方法。

要发出完整的符号类型描述符，编译器还必须确定返回类型（如果是多态的）。这是基于对方法调用表达式的强制转换（如果有），或者如果调用是表达式则为Object，否则如果调用是语句则为void。演员表可以是原始类型（但不是空白）。

作为一个极端情况，一个uncasted null参数被赋予java.lang.Void的符号类型描述符。 Void类型的歧义是无害的，因为除了null引用之外没有Void类型的引用。

## Performing invocation of access mode methods

第一次执行invokevirtual指令时，它通过符号解析指令中的名称并验证方法调用是静态合法的来链接。这也适用于访问模式方法的调用。在这种情况下，将检查编译器发出的符号类型描述符的语法是否正确，并解析它包含的名称。因此，只要符号类型描述符在语法上格式良好并且存在类型，调用访问模式方法的调用虚拟指令将始终链接。
在链接之后执行invokevirtual时，JVM首先检查接收VarHandle的访问模式类型，以确保它与符号类型描述符匹配。如果类型匹配失败，则意味着调用者正在调用的访问模式方法不会出现在被调用的单个VarHandle上。

调用访问模式方法的行为就像调用MethodHandle.invoke（java.lang.Object ...）一样，其中接收方法句柄接受VarHandle实例作为前导参数。更具体地说，以下内容，其中{access-mode}对应于访问模式方法名称：

```java
 VarHandle vh = ..
 R r = (R) vh.{access-mode}(p1, p2, ..., pN);
 
```

表现得好像：

```java
 VarHandle vh = ..
 VarHandle.AccessMode am = VarHandle.AccessMode.valueFromMethodName("{access-mode}");
 MethodHandle mh = MethodHandles.varHandleExactInvoker(
                       am,
                       vh.accessModeType(am));

 R r = (R) mh.invoke(vh, p1, p2, ..., pN)
```

（模访问模式方法不声明抛出Throwable）。 这相当于：

```java
 MethodHandle mh = MethodHandles.lookup().findVirtual(
                       VarHandle.class,
                       "{access-mode}",
                       MethodType.methodType(R, p1, p2, ..., pN));

 R r = (R) mh.invokeExact(vh, p1, p2, ..., pN)
```

其中所需的方法类型是符号类型描述符，并且执行MethodHandle.invokeExact（java.lang.Object ...），因为在调用目标之前，句柄将根据需要应用引用强制转换并使用box，unbox或widen 原始值，就像通过asType一样（另请参见MethodHandles.varHandleInvoker（java.lang.invoke.VarHandle.AccessMode，java.lang.invoke.MethodType））。 更简洁地说，这种行为相当于：

```java
 VarHandle vh = ..
 VarHandle.AccessMode am = VarHandle.AccessMode.valueFromMethodName("{access-mode}");
 MethodHandle mh = vh.toMethodHandle(am);

 R r = (R) mh.invoke(p1, p2, ..., pN)
 
```

在这种情况下，方法句柄绑定到VarHandle实例。

### Invocation checking

在典型的程序中，VarHandle访问模式类型匹配通常会成功。但是如果匹配失败，JVM将抛出WrongMethodTypeException。
因此，在静态类型的程序中可能显示为链接错误的访问模式类型不匹配可能在使用VarHandles的程序中显示为动态WrongMethodTypeException。

由于访问模式类型包含“实时”类对象，因此方法类型匹配会同时考虑类型名称和类加载器。因此，即使在一个类加载器L1中创建VarHandle VH并在另一个L2中使用，VarHandle访问模式方法调用也是类型安全的，因为在L2中解析的调用者的符号类型描述符与原始被调用方法的符号相匹配类型描述符，在L1中解析。 L1中的分辨率发生在创建VH并分配其访问模式类型时，而L2中的分辨率发生在链接invokevirtual指令时。

除了类型描述符检查之外，VarHandles访问它的变量的能力是不受限制的。如果一个VarHandle由一个可以访问该变量的类在非公共变量上形成，那么任何接收对它的引用的调用者都可以在任何地方使用生成的VarHandle。

与Core Reflection API不同，每次调用反射方法时都会检查访问权限，在创建VarHandle时会执行VarHandle访问检查。因此，VarHandles与非公共变量或非公共类中的变量一般应保密。除非从不受信任的代码中使用它们是无害的，否则不应将它们传递给不受信任的代码。

### VarHandle creation

Java代码可以创建一个VarHandle，直接访问该代码可访问的任何字段。 这是通过一个名为MethodHandles.Lookup的基于功能的反射API完成的。 例如，可以从Lookup.findVarHandle获取非静态字段的VarHandle。 Core Reflection API对象Lookup.unreflectVarHandle还有一种转换方法。
对受保护字段成员的访问仅限于访问类或其子类之一的接收者，并且访问类必须又是受保护成员的定义类的子类（或包兄弟）。 如果VarHandle引用当前包之外的声明类的受保护的非静态字段，则receiver参数将缩小为访问类的类型。

### Interoperation between VarHandles and the Core Reflection API

使用Lookup API中的工厂方法，可以将Core Reflection API对象表示的任何字段转换为行为等效的VarHandle。例如，可以使用Lookup.unreflectVarHandle将反射Field转换为VarHandle。生成的VarHandles通常提供对底层字段的更直接和有效的访问。
作为一种特殊情况，当Core Reflection API用于查看此类中的签名多态访问模式方法时，它们显示为普通的非多态方法。它们的反射外观（由Class.getDeclaredMethod查看）不受其在此API中的特殊状态的影响。例如，Method.getModifiers将准确报告任何类似声明的方法所需的那些修饰符位，包括本机和varargs位。

与任何反射方法一样，这些方法（反映时）可以通过java.lang.reflect.Method.invoke直接调用，通过JNI调用，也可以通过Lookup.unreflect间接调用。但是，这种反射调用不会导致访问模式方法调用。这样的调用，如果传递了必需的参数（类型为Object []的单个参数），将忽略该参数并抛出UnsupportedOperationException。

由于invokevirtual指令可以在任何符号类型描述符下本机调用VarHandle访问模式方法，因此该反射视图与通过字节码的这些方法的正常呈现冲突。因此，当Class.getDeclaredMethod反射地查看时，这些本机方法可能仅被视为占位符。

要获取特定访问模式类型的调用方法，请使用MethodHandles.varHandleExactInvoker（java.lang.invoke.VarHandle.AccessMode，java.lang.invoke.MethodType）或MethodHandles.varHandleInvoker（java.lang.invoke.VarHandle。 AccessMode，java.lang.invoke.MethodType）。 Lookup.findVirtual API还能够返回一个方法句柄来为任何指定的访问模式类型调用访问模式方法，并且与MethodHandles.varHandleInvoker的行为相同（java.lang.invoke.VarHandle.AccessMode，java.lang.invoke .MethodType）。

### Interoperation between VarHandles and Java generics

可以为变量（例如字段）获取VarHandle，该变量使用Java泛型类型声明。 与Core Reflection API一样，VarHandle的变量类型将从源级别类型的擦除构造。 调用VarHandle访问模式方法时，其参数类型或返回值强制转换类型可以是泛型类型或类型实例。 如果发生这种情况，编译器在构造invokevirtual指令的符号类型描述符时将通过其擦除替换这些类型。