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

它声明了一个 ResourceUtils（spring-core.util 下的一个工具类，定义了资源的 URL 前缀模式，和一些操作 URL 和文件的方法） 中的 URL前缀：`classpath:`，使得开发者可以通过相对类路径获取到加载资源。然后声明了`getResource` 与 `getClassLoader`两个方法。

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

### IO.SUPPORT包

在 spring-core.io包下面还有一个子包 support。该包扩展了ResourceLoader 提供了通过位置的模式（Ant样式路径模式）加载资源的方式。

### ResourcePatternResolver

用于将位置模式（例如，Ant样式路径模式）解析为Resource对象的策略接口。
这是ResourceLoader接口的扩展。传入的ResourceLoader（例如，在上下文中运行时通过ResourceLoaderAware传入的ApplicationContext）可以检查它是否也实现了这个扩展接口。

PathMatchingResourcePatternResolver 是一个独立的实现，可在ApplicationContext外部使用，也由ResourceArrayPropertyEditor用于填充Resource数组bean属性。

可以与任何类型的位置模式一起使用（例如“/WEB-INF/*-context.xml”）：输入模式必须与策略实现相匹配。此接口仅指定转换方法而不是特定的模式格式。

此接口还为类路径中的所有匹配资源建议新的资源前缀“classpath *：”。请注意，在这种情况下，资源位置应该是没有占位符的路径（例如“/beans.xml”）; JAR文件或类目录可以包含多个同名文件。

```java
public interface ResourcePatternResolver extends ResourceLoader {

	/**
	* 类路径中所有匹配资源的伪URL前缀：“classpath *：”
	* 这与ResourceLoader的类路径URL前缀的不同之处在于它检
	* 索给定名称的所有匹配资源（例如“/beans.xml”），例如
	* 在根目录中 所有已部署的JAR文件。
    */
	String CLASSPATH_ALL_URL_PREFIX = "classpath*:";

	/**
	 * 将给定的位置模式解析为Resource对象。
	 * 应尽可能避免重叠指向相同物理资源的资源条目。 
	 * 结果应该是一个不重复的集合。
	 **/
	Resource[] getResources(String locationPattern) throws IOException;

}
```

ResourcePatternResolver 作为 ResourceLoader 的扩展，提供了更高级的功能。

## PathMatchingResourcePatternResolvers

ResourcePatternResolver 接口的实现，它能够将指定的资源位置路径解析为一个或多个匹配的资源。 源路径可以是一个简单的路径，它与目标资源具有一对一的映射关系，或者可以包含特殊的`classpath *：`前缀和 `/`或内部Ant风格的正则表达式（使用Spring的AntPathMatcher实用程序进行匹配）。 后者都是有效的通配符。

##### 没有通配符：

没有通配符：

在简单的情况下，如果指定的位置路径不以`classpath *：`前缀开头，并且不包含 PathMatcher 模式，则此解析器将通过底层ResourceLoader上的 `getResource（）`调用返回单个资源。 示例是真实的URL，例如“file：C：/context.xml”，伪URL，例如“classpath：/context.xml”，以及简单的无前缀路径，例如“/WEB-INF/context.xml”。 后者将以特定于底层ResourceLoader的方式解析（例如，WebApplicationContext 的 ServletContextResource）。

##### **Ant-style Patterns:**

当路径位置包含 Ant 样式模式时，例如：

```
 /WEB-INF/*-context.xml
 com/mycompany/**/applicationContext.xml
 file:C:/some/path/*-context.xml
 classpath:com/mycompany/**/applicationContext.xml
```

解析器遵循更复杂但定义的过程来尝试解析通配符。 它为直到最后一个非通配符段的路径生成一个Resource，并从中获取一个URL。 如果此URL不是“jar：”URL或特定于容器的变体（例如，WebLogic中的“zip：”，WebSphere中的“wsjar”等），则从中获取`java.io.File`，并使用 通过遍历文件系统来解析通配符。对于 jar URL，解析器要么从它获取`java.net.JarURLConnection`，要么手动解析 jar URL，然后遍历jar文件的内容，以解决通配符。

##### 对便携性的影响：

如果指定的路径已经是文件 URL（显式或隐式，因为基本ResourceLoader是文件系统），那么通配符保证以完全可移植的方式工作。

如果指定的路径是类路径位置，则解析程序必须通过`Classloader.getResource（）`调用获取最后一个非通配符路径段URL。由于这只是路径的一个节点（不是最后的文件），因此在这种情况下，实际上未定义（在 ClassLoader Javadocs 中）究竟返回了什么类型的URL。在实践中，它通常是表示目录的`java.io.File`，其中类路径资源解析为文件系统位置，或者某种类型的 jar URL，其中类路径资源解析为 jar 位置。尽管如此，这项行为仍存在可移植性问题。

如果获取最后一个非通配符段的 jar URL，则解析器必须能够从中获取`java.net.JarURLConnection`，或者手动解析 jar URL，以便能够遍历jar的内容，并解析通配符。这适用于大多数环境，但在其他环境中会失败，强烈建议在依赖它之前，在特定环境中对来自jar的资源的通配符解析进行全面测试。

##### **classpath\*: Prefix:**

通过`classpath *：`前缀，可以检索具有相同名称的多个类路径资源。 例如，`classpath *：META-INF / beans.xml`将在类路径中找到所有“beans.xml”文件，无论是在“classes”目录中还是在JAR文件中。 这对于在每个jar文件中的相同位置自动检测同名的配置文件特别有用。 在内部，这通过`ClassLoader.getResources（）`调用发生，并且是完全可移植的。

`classpath *：`前缀也可以与位置路径的其余部分中的 PathMatcher 模式组合，例如“classpath *：META-INF / *-beans.xml”。 在这种情况下，解析策略非常简单：在最后一个非通配符路径段上使用`ClassLoader.getResources（）`调用来获取类加载器层次结构中的所有匹配资源，然后关闭每个资源，将上述相同的 PathMatcher 解析策略用于通配符子路径。

##### 其他说明：

警告：请注意，`classpath*:`与 Ant 样式模式结合使用时，只能在模式启动前与至少一个根目录可靠地工作，除非实际目标文件驻留在文件系统中。 这意味着像“classpath * :* .xml”这样的模式不会从jar文件的根目录中检索文件，而只能从扩展目录的根目录中检索文件。 这源于JDK的`ClassLoader.getResources（）`方法中的限制，该方法仅返回传入的空字符串的文件系统位置（指示搜索的潜在根）。 此 ResourcePatternResolver 实现尝试通过 URLClassLoader 内省和“java.class.path”清单评估来缓解 jar 根查找限制; 但是，没有可移植性保证。

警告：如果要搜索的根包在多个类路径位置中可用，则不保证具有“classpath：”资源的Ant样式模式可以找到匹配的资源。 这是因为资源如

```
com/mycompany/package1/service-context.xml
```

可能只在一个位置，但是当一个路径如

```
  classpath:com/mycompany/**/service-context.xml
```

用于尝试解决它，解析器将解决getResource（“com / mycompany”）;返回的（第一个）URL。 如果此基本包节点存在于多个类加载器位置中，则实际的最终资源可能不在下面。 因此，最好在这种情况下使用具有相同Ant样式模式的“classpath *：”，它将搜索包含根包的所有类路径位置。

PathMatchingResourcePatternResolvers 使用 DefaultResourceLoader 作为它的资源加载器，使用AntPathMatcher 作为它的模式匹配器。

### AntPathMatcher

Ant样式路径模式的PathMatcher实现。
部分映射代码已经从Apache Ant中借用。

映射使用以下规则匹配URL：

- `？` 匹配一个字符
-  `*`  匹配零个或多个字符
- `**` 匹配路径中的零个或多个目录
- {spring：[a-z] +} 将正则表达式 `[a-z] +`作为名为“spring”的路径变量进行匹配

##### Examples

- `com/t?st.jsp` — matches `com/test.jsp` but also `com/tast.jsp` or `com/txst.jsp`
- `com/*.jsp` — matches all `.jsp` files in the `com` directory
- `com/**/test.jsp` — matches all `test.jsp` files underneath the `com` path
- `org/springframework/**/*.jsp` — matches all `.jsp` files underneath the `org/springframework` path
- `org/**/servlet/bla.jsp` — matches `org/springframework/servlet/bla.jsp` but also`org/springframework/testing/servlet/bla.jsp` and `org/servlet/bla.jsp`
- `com/{filename:\\w+}.jsp` will match `com/test.jsp` and assign the value `test` to the `filename` variable

**注意:**模式和路径必须都是绝对的，或者必须都是相对的，以使两者匹配。 因此，建议此实现的用户清理模式，以便在它们使用的上下文中使用“/”作为前缀。

小结：support 包下面的其他类适用于文件里属性读取，SPI 机制的工具类。

# 资源加载总结

Spring 将 Resource 表达为不同资源，通过不同的 ResourceLoader 加载资源。通过	PathMatchingResourcePatternResolvers 实现将通配符使用在路径中，作为加载资源的高级实现。



