Spring IOC 的核心就是 ApplicationContext，首先从它开始分析：

ApplicationContext:用于为应用程序提供配置的中央接口。 这在应用程序运行时是只读的，但如果实现支持，则可以重新加载。

ApplicationContext提供：

- Bean工厂方法，用于访问应用程序组件 继承自ListableBeanFactory。
- 以通用方式加载文件资源的能力。 继承自ResourceLoader接口。
- 将事件发布到已注册的侦听器的功能。 继承自ApplicationEventPublisher接口。
- 解决消息，支持国际化的能力。 继承自MessageSource接口。
- 从父上下文继承。 后代上下文中的定义始终优先。 这意味着，例如，整个Web应用程序可以使用单个父上下文，而每个servlet都有自己的子上下文，该上下文独立于任何其他servlet的子上下文。

除了标准 BeanFactory 生命周期功能之外，ApplicationContext 实现还检测并调用ApplicationContextAware bean以及ResourceLoaderAware，ApplicationEventPublisherAware和MessageSourceAware beans。

![](G:\KnowledgeBase\picture\Spring\ApplicationContextDiagram.png)

先看一下 ApplicationContext 的顶层接口

* ResourcePatternResolver：用于将位置模式（例如，Ant样式路径模式）解析为Resource对象的策略接口。
* ApplicationEventPublisher：封装事件发布功能的接口。 用作 ApplicationContext 的超类接口。
* MessageSource：用于解析消息的策略接口，支持此类消息的参数化和国际化。
* EnvironmentCapable：表明所拥有的和公开Environment引用的组件的接口。 所有Spring应用程序上下文都是EnvironmentCapable，该接口主要用于在接受BeanFactory实例的框架方法中执行instanceof检查，这些实例可能实际上也可能不是ApplicationContext实例，以便在环境可用时与环境进行交互。
* ListableBeanFactory：BeanFactory 接口的扩展，需要被可以枚举出所有 bean 的 bean 工厂来实现，而不是按照客户端要求逐个按名称查找 bean。
* HierarchicalBeanFactory：由bean工厂实现的子接口，可以是层次结构的一部分。

## 资源

首先来了解在 Spring 中的资源是怎么加载的。

![](G:\KnowledgeBase\picture\Spring\Spring_core_io_one.png)

资源处理有关的代码在 Spring core 模块的`org.springframework.core.io`包中，顶级接口为`InputStreamSource`接口。它仅定义了一个`getInputStream()`方法。

### InputStreamSource

#### java doc

给定InputStream的资源实现。仅在没有其他特定资源实现适用时才应使用。 特别是，在可能的情况下，更应该倾向于ByteArrayResource或任何基于文件的Resource实现。

与其他Resource实现相比，这是已打开资源的描述符 - 因此从isOpen（）返回true。 如果需要将资源描述符保留在某处，或者需要多次读取流，请不要使用InputStreamResource。

### Resource 

Resource 接口代表资源的抽象。

#### java doc 

从实际类型的底层资源（例如文件或类路径资源）中抽象出来的资源描述符的接口。
如果InputStream以物理形式存在，则可以为每个资源打开，但只能为某些资源返回URL或File句柄。 实际行为是特定于实现的。

通过这个接口可以对资源进行判是否存在、打开，获取资源的描述信息，资源的句柄等。

Resource 接口下面定义了两个范围更加精细的 Rresouce 接口，ContextResource 和 FileSystemResource ，分别用于通过上下文和文件系统加载资源。还有一个抽象类 AbstractResource，实现了一些处理资源的简单操作。

### ContextResource 

从封闭的“上下文”加载的资源的扩展接口，例如， 来自ServletContext，但也来自普通类路径路径或相对文件系统路径（指定时没有显式前缀，因此相对于本地ResourceLoader的上下文应用）。

它只定义了一个方法用于返回封闭“上下文”中的路径。这通常是相对于特定于上下文的根目录的路径，例如， ServletContext 根目录或 PortletContext 根目录。

### WritableResource

支持写入资源的扩展接口。 提供OutputStream访问器。

它提供了两个方法，用于判断该资源是否可以修改，和获取该资源的输入流。

### AbstractResource

资源实现的便捷基类，预先实现典型行为。
`exists`方法将检查是否可以打开File或InputStream; `isOpen`总是会返回 false; `getURL`和`getFile`抛出异常; 和`toString`将返回描述。

AbstractResource 下面定义了具体的获取资源的方式，一般的情况下，都是从类路径获取资源。

### AbstractFileResolvingResource 

对于从文件系统获得资源的方式 ，定义了 AbstractFileResolvingResource 用于将URL解析为文件引用的资源的抽象基类，例如UrlResource或ClassPathResource。和检测URL中的“文件”协议以及JBoss“vfs”协议，相应地解析文件系统引用。

它提供了处理文件资源的基础方法。将基于类路径的资源和基于 URL 的资源的基础行为提取出来。

### ClassPathResource

#### java doc

类路径资源的资源实现。 使用给定的ClassLoader或给定的Class来加载资源。
如果类路径资源驻留在文件系统中，则支持解析为java.io.File，但不支持JAR中的资源。 始终支持解析为URL。

ClassPathResource 通过 `Class`或者 `ClassLoader`  用`path`作为资源的相对路径获取输入流。

```java
public InputStream getInputStream() throws IOException {
   InputStream is;
   if (this.clazz != null) {
      is = this.clazz.getResourceAsStream(this.path);
   }
   else if (this.classLoader != null) {
      is = this.classLoader.getResourceAsStream(this.path);
   }
   else {
      is = ClassLoader.getSystemResourceAsStream(this.path);
   }
   if (is == null) {
      throw new FileNotFoundException(getDescription() + " cannot be opened because it does not exist");
   }
   return is;
}
```

获取顺序为先通过 `Class` 获取，如果`Class`为空，则通过`ClassLoader`获取。

### UrlResource

java.net.URL定位器的资源实现。支持将URL， 并在“file：”协议的情况下，将文件作为资源解析。

它支持通过网络获取输入流，或者通过文件路径获取文件。

### ByteArrayResource

给定字节数组的资源实现。
为给定的字节数组创建一个ByteArrayInputStream。

用于从任何给定的字节数组加载内容，而不必求助于一次性使用InputStreamResource。 特别适用于从本地内容创建邮件附件，其中JavaMail需要能够多次读取流。

### Resource 小结

主要的资源形式就是以上三种。接下来看一下 ResourceLoader，用于通过资源的位置将资源解析成`Rrsource`。

### ResourceLoader

用于加载资源的策略接口（例如，类路径或文件系统资源）。 需要ApplicationContext来提供此功能，以及扩展的ResourcePatternResolver支持。
DefaultResourceLoader是一个独立的实现，可以在ApplicationContext外部使用，也可以由ResourceEditor使用。

使用特定上下文的资源加载策略，在ApplicationContext中运行时，可以从Strings填充Resource和Resource数组类型的Bean属性。

它声明了一个 URL前缀：`classpath:`，使得开发者可以通过相对类路径获取到加载资源。然后声明了`getResource` 与 `getClassLoader`两个方法。

### DefaultResourceLoader

ResourceLoader接口的默认实现。 由ResourceEditor使用，并作为AbstractApplicationContext的基类。 也可以单独使用。
如果位置值是URL，则返回UrlResource;如果是非URL路径或“classpath：”伪URL，则返回ClassPathResource。

核心方法：

```java
@Override
public Resource getResource(String location) {
   Assert.notNull(location, "Location must not be null");

   for (ProtocolResolver protocolResolver : this.protocolResolvers) {
      Resource resource = protocolResolver.resolve(location, this);
      if (resource != null) {
         return resource;
      }
   }

   if (location.startsWith("/")) {
      return getResourceByPath(location);
   }
   else if (location.startsWith(CLASSPATH_URL_PREFIX)) {
      return new ClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()), getClassLoader());
   }
   else {
      try {
         // Try to parse the location as a URL...
         URL url = new URL(location);
         return new UrlResource(url);
      }
      catch (MalformedURLException ex) {
         // No URL -> resolve as resource path.
         return getResourceByPath(location);
      }
   }
}
```

首先尝试通过 ProtocolResolver 加载资源，ProtocolResolver 是基于 SPI 的接口。通过实现`reslove`方法用户可以按照自己的意愿加载资源。

当通过ProtocolResolver 加载失败的时候

* 如果路径以`/`开头，则创建扩 ClassPathContextResource，该类继承 ClassPathResource 并实现了 ContextResource，将ClassPathResource 扩展为可以表达上下文相关路径的 Resource。
* 如果以 `ClassPath：`开头则创建 ClassPathResource。
* 接下来尝试创建 UrlResource，通过网络地址或者文件创建资源。
* 如果不能解析为 URL 则在异常中，创建 ClassPathContextResource。

### ClassRelativeResourceLoader

将普通资源路径解析为相对资源路径，获取Resources。

### FileSystemResourceLoader

通过文件系统获取Resource。

### ResourceEditor

资源描述符的编辑器，用于自动转换字符串表示的位置，例如 `file：C：/myfile.txt`或`classpath：myfile.txt`到 Resource 属性，而不是使用String location属性。
该路径可能包含`$ {...}`占位符，需要解析为环境属性：例如`$ {user.dir}`。 默认情况下会忽略无法解析的占位符。

默认情况下，使用DefaultResourceLoader委派ResourceLoader进行繁重的工作。

ResourceEditor 实现了属性编辑器 PropertyEditor。通过`getAsText`、`setAsText`。可以将相对路径转换成绝对路径，同时可以通过`ignoreUnresolvablePlaceholders`参数控制是否需要解析，占位符。

核心方法是`resolvePath`：

```java
protected String resolvePath(String path) {
   if (this.propertyResolver == null) {
      this.propertyResolver = new StandardEnvironment();
   }
   return (this.ignoreUnresolvablePlaceholders ? this.propertyResolver.resolvePlaceholders(path) :
         this.propertyResolver.resolveRequiredPlaceholders(path));
}
```

默认通过 StandardEnvironment 提供的方法解析路径。关于Environment 后续分析。





