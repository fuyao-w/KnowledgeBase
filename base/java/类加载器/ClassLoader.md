##  ClassLoader

类加载器是一个负责加载类的对象。 ClassLoader类是一个抽象类。 给定类的二进制名称，类加载器应尝试定位或生成那些构成类定义的数据。 典型的策略是将名称转换为文件名，然后从文件系统中读取该名称的“类文件”。

每个Class对象都包含对定义它的ClassLoader的引用。

**数组类的类对象不是由类加载器创建的，而是根据Java运行时的需要自动创建的。** `Class.getClassLoader（）`返回的数组类的类加载器与其元素类型的类加载器相同; 如果元素类型是基本类型，则数组类没有类加载器。

应用程序实现ClassLoader的子类，以便扩展Java虚拟机动态加载类的方式。

安全管理器通常可以使用类加载器来指示安全域。

除了加载类之外，类加载器还负责定位资源。 资源是一些数据（例如“.class”文件，配置数据或图像），用抽象的“/”分隔的路径名称标识。 资源通常与应用程序或库打包在一起，以便可以通过应用程序或库中的代码来定位它们。 在某些情况下，会包含资源，以便其他库可以找到它们。

ClassLoader类使用委派模型来搜索类和资源。 ClassLoader的每个实例都有一个关联的父类加载器。当请求查找类或资源时，ClassLoader实例通常会在尝试查找类或资源本身之前将对类或资源的搜索委托给其父类加载器。

支持并发加载类的类加载器称为并行加载类加载器，需要通过调用`ClassLoader.registerAsParallelCapable`方法在类初始化时注册自己。请注意，ClassLoader类默认注册为并行。但是，如果它们具有并行能力，它的子类仍然需要注册自己。在委托模型不是严格分层的环境中，类加载器需要具有并行能力，否则类加载会导致死锁，因为加载器锁在类加载过程的持续时间内保持（请参阅loadClass方法）。

### 运行时内置类加载器

Java运行时具有以下内置类加载器：

- Bootstrap 类加载器。它是虚拟机的内置类加载器，通常表示为null，并且没有父加载器。

- Platform 加载器。平台类加载器可以看到所有平台类，它们可以用作ClassLoader实例的父级。平台类包括Java SE平台API，它们的实现类和JDK特定的运行时类由平台类加载器或其祖先定义。

  为了允许 升级/覆盖  定义到平台类加载器的模块，并且升级后的模块读取定义到除Platform 类加载器及其祖先之外的类加载器的模块，那么平台类加载器可能必须委托给其他类加载器，例如，application 类加载器。换句话说，Platform 类加载器可以看到定义到除Platform 类加载器及其祖先之外的类加载器的命名模块中的类。

- System 类加载器。它也称为 application 类加载器，与 Platform 类加载器不同。System 类加载器通常用于在应用程序类路径，模块路径和JDK特定工具上定义类。Platform 类加载器是系统类加载器的父级或祖先，所有平台类都是可见的。

通常，Java虚拟机以与平台相关的方式从本地文件系统加载类。 但是，某些类可能不是源自文件; 它们可能来自其他来源，例如网络，或者它们可以由应用程序构建。 方法`defineClass`将字节数组转换为类Class的实例。 可以使用`Class.newInstance`创建此新定义的类的实例。

由类加载器创建的对象的方法和构造函数可以引用其他类。 要确定所引用的类，Java虚拟机将调用最初创建该类的类加载器的loadClass方法。

例如，应用程序可以创建网络类加载器以从服务器下载类文件。 示例代码可能如下所示：

```java
   ClassLoader loader = new NetworkClassLoader(host, port);
   Object main = loader.loadClass("Main", true).newInstance();
        . . .
```

网络类加载器子类必须定义方法`findClass`和`loadClassData`以从网络加载类。 一旦下载了构成类的字节，就应该使用	`defineClass`方法创建一个类实例。 示例实现是：

```java
     class NetworkClassLoader extends ClassLoader {
         String host;
         int port;

         public Class findClass(String name) {
             byte[] b = loadClassData(name);
             return defineClass(name, b, 0, b.length);
         }

         private byte[] loadClassData(String name) {
             // load the class data from the connection
              . . .
         }
     }
 
```

### 二进制名称

作为ClassLoader中方法的String参数提供的任何类名必须是The Java™Language Specification定义的二进制名。

有效类名的示例包括：

```java
   "java.lang.String"
   "javax.swing.JSpinner$DefaultEditor"
   "java.security.KeyStore$Builder$FileBuilder$1"
   "java.net.URLClassLoader$3$1"
```

作为ClassLoader中方法的String参数提供的任何包名称必须是空字符串（表示未命名的包）或由Java™语言规范定义的完全限定名称。

