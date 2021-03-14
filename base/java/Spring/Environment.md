## Environment

环境是集成在容器中的抽象，它模拟了应用程序环境的两个关键方面：[*profiles*](https://docs.spring.io/spring/docs/4.3.22.RELEASE/spring-framework-reference/htmlsingle/#beans-definition-profiles) 和 [*properties*](https://docs.spring.io/spring/docs/4.3.22.RELEASE/spring-framework-reference/htmlsingle/#beans-property-source-abstraction)。

profile 是仅在给定配置文件处于活动状态时才向容器注册的Bean定义的命名逻辑组。 可以将Bean分配给配置文件，无论是以XML还是通过注释定义。 与配置文件相关的 Environment 对象的作用是确定哪些配置文件（如果有）当前处于活动状态，以及默认情况下哪些配置文件（如果有）应处于活动状态。

properties 在几乎所有应用程序中都发挥着重要作用，可能源自各种源：属性文件，JVM系统属性，系统环境变量，JNDI，servlet上下文参数，ad-hoc属性对象，Maps 等。 与属性相关的Environment 对象的作用是为用户提供方便的服务接口，用于配置属性源和从中解析属性。

### EnvironmentCapable

指示包含和公开 Environment引用的组件的接口。

所有Spring应用程序上下文都是 EnvironmentCapable，该接口主要用于在接受BeanFactory实例的框架方法中执行 instanceof 检查，这些实例可能实际上也可能不是ApplicationContext实例，以便在环境可用时与环境进行交互。

如前所述，ApplicationContext 扩展了 EnvironmentCapable，从而暴露了一个`getEnvironment（）`方法; 但是，ConfigurableApplicationContext 重新定义了 `getEnvironment（）`并缩小了签名以返回 ConfigurableEnvironment。 结果是，在从 ConfigurableApplicationContext 访问 Environment 对象之前，它是“只读”，此时它也可以配置。

![](G:\KnowledgeBase\picture\Spring\environment_two.png)

## PropertySource

表示 name/value 属性对的源的抽象基类。底层源对象可以是封装属性的任何类型T。示例包括Properties对象，Map对象，ServletContext 和 ServletConfig 对象（用于访问init参数）。探索PropertySource类型层次结构以查看提供的实现。
PropertySource 对象通常不是孤立使用的，而是通过 PropertySources 对象使用，该对象聚合属性源并与 PropertyResolver 实现结合使用，PropertyResolver实现可以跨PropertySource集执行基于优先级的搜索。

PropertySource标识不是基于封装属性的内容确定的，而是基于PropertySource的名称。这对于在集合上下文中操作PropertySource对象很有用。有关详细信息，请参阅MutablePropertySources 中的操作以及`named（String）`和`toString（）`方法。

请注意，在使用 @Configuration 类时，@PropertySource 注解提供了一种方便的声明式方法，可以将属性源添加到封闭环境中。

通过 PropertySource 可以从不同的源（比如 Map 集合，系统变量）中获取属性，或者获取属性源。它是一个泛型类，参数类型为不同的源的类型。



## EnumerablePropertySource

一个 PropertySource 实现，能够询问其底层源对象以枚举所有可能的属性名称/值对。暴露 `getPropertyNames（）`方法以允许调用者内省可用属性，而无需访问底层源对象。这也有助于更有效地实现 `containsProperty（String）`，因为它可以调用`getPropertyNames（）`并遍历返回的数组，而不是尝试调用可能更昂贵的 `PropertySource.getProperty（String）``。实现可以考虑缓存getPropertyNames（）`的结果以充分利用此性能机会。
大多数框架提供的PropertySource实现都是可枚举的;反例是 JndiPropertySource，由于JNDI的性质，在任何给定时间都无法确定所有可能的属性名称;相反，它只能尝试访问属性（通过PropertySource.getProperty（String））以评估它是否存在。

## CompositePropertySource

迭代一组 PropertySource 实例的复合 PropertySource 实现。 在多个属性源共享相同名称的情况下是必要的，例如 当多个值提供给 `@PropertySource `时。
从Spring 4.1.2开始，此类扩展 EnumerablePropertySource 而不是普通的PropertySource，根据所有包含的源（尽可能）累积的属性名称公开` getPropertyNames（）`。

该类提供了从多个同名属性源获取属性的能力。

## MapPropertySource

MapPropertySource 继承CompositePropertySource ，使用Map 集合作为源的类型。

### PropertiesPropertySource、SystemEnvironmentPropertySource

PropertiesPropertySource 使用 java.utils 包下面的 Properties 作为属性源。

SystemEnvironmentPropertySource 是 MapPropertySource的专业化设计用于系统环境变量。 补偿Bash 和其他 shell 中的约束，这些shell不允许包含句点字符和/或连字符的变量; 还允许对属性名称进行大写变体，以便更加惯用shell使用。

例如，对getProperty（“foo.bar”）的调用将尝试查找原始属性或任何“等效”属性的值，返回找到的第一个：

- `foo.bar` - the original name
- `foo_bar` - with underscores for periods (if any)
- `FOO.BAR` - original, with upper case
- `FOO_BAR` - with underscores and upper case

上述的任何连字符变体都可以使用，甚至可以混合使用点/连字符变体。
这同样适用于对 containsProperty（String） 的调用，如果存在任何上述属性，则返回true，否则返回false。

将profiles 或默认配置文件指定为环境变量时，此功能特别有用。 在Bash下不允许以下内容：

```java
spring.profiles.active=p1 java -classpath ... MyApp
```

但是，允许使用以下语法，这也是更常规的：

```
SPRING_PROFILES_ACTIVE=p1 java -classpath ... MyApp
```

为此类（或包）启用调试或跟踪级别日志记录，以获取解释何时发生这些“属性名称解析”的消息。

默认情况下，此属性源包含在StandardEnvironment及其所有子类中。

## CommandLinePropertySource

由命令行参数支持的 PropertySource 实现的抽象基类。 参数化类型 T 表示命令行选项的基础源。 对于 SimpleCommandLinePropertySource，这可能像String数组一样简单，或者在JOptCommandLinePropertySource 的情况下特定于特定API，例如JOpt的OptionSet。

目的和一般用法：

用于独立的基于Spring的应用程序，即通过传统的 main 方法引导的应用程序(比如通过Spring Boot)，接受来自命令行的String [] 参数。 在许多情况下，直接在main方法中处理命令行参数可能就足够了，但在其他情况下，可能需要将参数作为值注入 Spring bean。 正是后一组CommandLinePropertySource 变得有用的情况。 CommandLinePropertySource 通常会添加到Spring ApplicationContext 的Environment中，此时所有命令行参数都可通过PropertyResolver.getProperty（String）系列方法获得。 例如：

```java
public static void main(String[] args) {
     CommandLinePropertySource clps = ...;
     AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
     ctx.getEnvironment().getPropertySources().addFirst(clps);
     ctx.register(AppConfig.class);
     ctx.refresh();
 }
```

使用上面的引导逻辑，AppConfig类可以`@Inject` Spring Environment并直接查询它的属性：

```java
 @Configuration
 public class AppConfig {

     @Inject Environment env;

     @Bean
     public void DataSource dataSource() {
         MyVendorDataSource dataSource = new MyVendorDataSource();
         dataSource.setHostname(env.getProperty("db.hostname", "localhost"));
         dataSource.setUsername(env.getRequiredProperty("db.username"));
         dataSource.setPassword(env.getRequiredProperty("db.password"));
         // ...
         return dataSource;
     }
 }
```

因为CommandLinePropertySource使用#`addFirst`方法添加到Environment的MutablePropertySources集合中，所以它具有最高的搜索优先级，这意味着虽然“db.hostname”和其他属性可能存在于其他属性源（如系统环境变量）中，但它将是 首先从命令行属性源中选择。 这是一种合理的方法，因为命令行中指定的参数自然比指定为环境变量的参数更具体。
作为注入环境的替代方法，Spring的`@Value`注释可用于注入这些属性，因为已经直接或通过使用`<context：property-placeholder>`元素注册了 PropertySourcesPropertyResolver bean。 例如：

```java
 @Component
 public class MyComponent {

     @Value("my.property:defaultVal")
     private String myProperty;

     public void getMyProperty() {
         return this.myProperty;
     }

     // ...
 }
```

使用选项参数：

单个命令行参数通过常用的 `PropertySource.getProperty（String）` 和`PropertySource.containsProperty（String）`方法表示为属性。 例如，给定以下命令行：

```java
--o1=v1 --o2
```

'o1'和'o2'被视为“选项参数”，以下断言将评估为true：

```java
 CommandLinePropertySource ps = ...
 assert ps.containsProperty("o1") == true;
 assert ps.containsProperty("o2") == true;
 assert ps.containsProperty("o3") == false;
 assert ps.getProperty("o1").equals("v1");
 assert ps.getProperty("o2").equals("");
 assert ps.getProperty("o3") == null;
```

请注意，'o2'选项没有参数，但getProperty（“o2”）解析为空字符串（“”）而不是null，而getProperty（“o3”）解析为null，因为它未指定。 此行为与所有PropertySource实现遵循的常规协定一致。
另请注意，虽然上面的示例中使用了“ - ”来表示选项参数，但此语法可能因各个命令行参数库而异。 例如，基于JOpt或Commons CLI的实现可能允许单个破折号（“ - ”）“短”选项参数等。

使用非选项参数：

通过这种抽象也支持非选项参数。 任何没有选项样式前缀（如“ - ”或“ - ”）的参数都被视为“非选项参数”，可通过特殊的“nonOptionArgs”属性获得。 如果指定了多个非选项参数，则此属性的值将是包含所有参数的逗号分隔的字符串。 此方法确保CommandLinePropertySource中所有属性的简单且一致的返回类型（String），同时在与Spring Environment及其内置ConversionService结合使用时可以进行转换。 请考虑以下示例：

```
--o1=v1 --o2=v2 /path/to/file1 /path/to/file2
```

在此示例中，“o1”和“o2”将被视为“选项参数”，而两个文件系统路径则被视为“非选项参数”。 因此，以下断言将评估为真：

```java
 CommandLinePropertySource ps = ...
 assert ps.containsProperty("o1") == true;
 assert ps.containsProperty("o2") == true;
 assert ps.containsProperty("nonOptionArgs") == true;
 assert ps.getProperty("o1").equals("v1");
 assert ps.getProperty("o2").equals("v2");
 assert ps.getProperty("nonOptionArgs").equals("/path/to/file1,/path/to/file2");
 
```

如上所述，当与Spring Environment抽象结合使用时，这个以逗号分隔的字符串可以很容易地转换为String数组或列表：

```java
 Environment env = applicationContext.getEnvironment();
 String[] nonOptionArgs = env.getProperty("nonOptionArgs", String[].class);
 assert nonOptionArgs[0].equals("/path/to/file1");
 assert nonOptionArgs[1].equals("/path/to/file2");
```

可以通过setNonOptionArgsPropertyName（String）方法自定义特殊“非选项参数”属性的名称。 建议这样做，因为它为非选项参数提供了适当的语义值。 例如，如果将文件系统路径指定为非选项参数，则最好将它们称为“file.locations”，而不是默认的“nonOptionArgs”：

```java
public static void main(String[] args) {
     CommandLinePropertySource clps = ...;
     clps.setNonOptionArgsPropertyName("file.locations");

     AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
     ctx.getEnvironment().getPropertySources().addFirst(clps);
     ctx.register(AppConfig.class);
     ctx.refresh();
 }
```

限制:

此抽象并非旨在公开底层命令行解析API（如JOpt或Commons CLI）的全部功能。 它的意图恰恰相反：提供最简单的抽象，以便在解析后访问命令行参数。 因此，典型案例将涉及完全配置底层命令行解析API，解析进入main方法的参数的String []，然后简单地将解析结果提供给CommandLinePropertySource的实现。 此时，所有参数都可以被视为“选项”或“非选项”参数，并且如上所述可以通过常规PropertySource和Environment API访问。

### SimpleCommandLineArgsParser

命令行PropertySource的解析器。

## JOptCommandLinePropertySource

由JOpt OptionSet 支持的 CommandLinePropertySource实现。

典型用法:

针对提供给main方法的参数的 String [] 配置并执行OptionParser，并使用生成的OptionSet对象创建JOptCommandLinePropertySource：

```java
public static void main(String[] args) {
     OptionParser parser = new OptionParser();
     parser.accepts("option1");
     parser.accepts("option2").withRequiredArg();
     OptionSet options = parser.parse(args);
     PropertySource ps = new JOptCommandLinePropertySource(options);
     // ...
 }
```

有关完整的一般用法示例，请参见CommandLinePropertySource。
需要JOpt简易版4.3或更高版本。 测试JOpt直到5.0。

### PropertySources

作为 PropertySource 的容器，容纳多个PropertySource。

### MutablePropertySources

PropertySources接口的默认实现。 实现了基于优先级的 PropertySource 管理。允许操作包含的属性源，并提供用于复制现有 PropertySources实例的构造函数。
在诸如`addFirst（org.springframework.core.env.PropertySource <？>）`和`addLast（org.springframework.core.env.PropertySource <？>）`等方法中提到优先级的情况下，这是关于属性的顺序 使用PropertyResolver解析给定属性时将搜索源。



## Environment 相关 ##



![](G:\KnowledgeBase\picture\Spring\environment_one.png)

env 包下面主要分为两部分，一部分用于处理 profile，另一部分用于处理 property。

### PropertyResolver

用于解析任何基础源的属性的接口。定义了关于 property 的 getter 和 setter，处理占位符（${...}）的方法。

### Environment

表示当前应用程序正在运行的环境的接口。模拟应用程序环境的两个关键方面：profile 和 properties。与 properties 访问相关的方法通过 PropertyResolver 超接口公开。
profile 是仅在给定配置文件处于活动状态时才向容器注册的Bean定义的命名逻辑组。可以将Bean分配给配置文件，无论是以XML还是通过注释定义;有关语法详细信息，请参阅spring-beans 3.1模式或@Profile注释。与配置文件相关的Environment对象的作用是确定哪些配置文件（如果有）当前处于活动状态，以及默认情况下哪些配置文件（如果有）应处于活动状态。

properties 在几乎所有应用程序中都发挥着重要作用，可能源自各种源：属性文件，JVM系统属性，系统环境变量，JNDI，servlet上下文参数，ad-hoc属性对象，映射等。与属性相关的环境对象的作用是为用户提供方便的服务接口，用于配置属性源和从中解析属性。

在ApplicationContext中管理的Bean可以使用 EnvironmentAware或 `@Inject  `注册为 Environment，以便直接查询配置文件状态或解析属性。

但是，在大多数情况下，应用程序级 bean 不需要直接与 Environment 交互，除了使用属性占位符配置器（例如：PropertySourcesPlaceholderConfigurer，它本身就是EnvironmentAware，从Spring 3.1开始，在使用`<context：property-placeholder /> `时默认注册。）将 $(...) 替换为真实值的时候。

必须通过ConfigurableEnvironment接口完成环境对象的配置，该接口从所有AbstractApplicationContext 子类 `getEnvironment（）`方法返回。有关在应用程序上下文`refresh()` 之前演示属性源操作的用法示例，请参阅ConfigurableEnvironment Javadoc。

environment 接口继承了 PropertyResolver，定义了获取 Profile 状态的一些方法。

### ConfigurablePropertyResolver

配置接口由大多数 PropertyResolver类型实现。 提供用于访问和自定义将属性值从一种类型转换为另一种类型时使用的 ConversionService 的工具。定义了设置 ConfigurableConversionService ，设置占位符前缀，分隔符。确定必须有的 property 的方法。

### ConfigurableEnvironment

财产来源可能被删除，重新排序或替换; 可以使用从getPropertySources（）返回的MutablePropertySources实例添加其他属性源。 以下示例针对ConfigurableEnvironment的StandardEnvironment实现，但通常适用于任何实现，但特定的默认属性源可能不同。

##### 示例：添加具有最高搜索优先级的新属性源

```java
 ConfigurableEnvironment environment = new StandardEnvironment();
 MutablePropertySources propertySources = environment.getPropertySources();
 Map<String, String> myMap = new HashMap<>();
 myMap.put("xyz", "myValue");
 propertySources.addFirst(new MapPropertySource("MY_MAP", myMap));
 
```

###### 示例：删除默认系统属性属性源

```java
 MutablePropertySources propertySources = environment.getPropertySources();
 propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)
```

##### 示例：模拟系统环境以进行测试

```java
MutablePropertySources propertySources =  environment.getPropertySources();
MockPropertySource mockEnvVars = new MockPropertySource().withProperty("xyz", "myValue");
propertySources.replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, mockEnvVars);
```

当ApplicationContext正在使用Environment时，在调用上下文的refresh（）方法之前执行任何此类PropertySource操作都很重要。 这可确保在容器引导过程中所有属性源都可用，包括属性占位符配置器使用。

定义了设置活动profile，系统 profile，合并 profile 的方法。

### AbstractEnvironment

环境实现的抽象基类。支持保留的默认配置文件名称的概念，并允许通过`ACTIVE_PROFILES_PROPERTY_NAME`和`DEFAULT_PROFILES_PROPERTY_NAME `属性指定活动和默认配置文件。

具体的子类主要区别在于它们默认添加的 PropertySource 对象。 AbstractEnvironment 没有添加任何内容。 子类应通过受保护的 `customizePropertySources（MutablePropertySources）`钩子提供属性源，而客户端应使用`ConfigurableEnvironment.getPropertySources（）`进行自定义并对MutablePropertySources API 进行操作。 有关用法示例，请参阅ConfigurableEnvironment javadoc。

### StandardEnvironment	

该类实现了钩子方法 customizePropertySources,用于添加属性源。

适用于“标准”（即非Web）应用程序的环境实现。

除了可配置环境的常用功能（如属性解析和与配置文件相关的操作）之外，此实现还配置两个默认属性源，按以下顺序搜索：

- system properties
- system environment variables

也就是说，如果 key“xyz” 既存在于JVM系统属性中，也存在于当前进程的环境变量集中，则系统属性中的键“xyz”的值将从对 environment.getProperty的调用返回（“XYZ”）。 默认情况下会选择此排序，因为系统属性是 per-JVM，而环境变量在给定系统上的许多JVM上可能是相同的。 赋予系统属性优先权允许基于每个JVM覆盖环境变量。

可以删除，重新排序或替换这些默认属性源; 可以使用`AbstractEnvironment.getPropertySources（）`中提供的 MutablePropertySources 实例添加其他属性源。 有关用法示例，请参阅 ConfigurableEnvironment Javadoc。

有关在shell环境（例如Bash）中特殊处理属性名称的详细信息，请参阅SystemEnvironmentPropertySource javadoc，该环境禁止使用变量名称中的句点字符。

### AbstractPropertyResolver

用于解析 Property 的抽象类

### PropertySourcesPropertyResolver

PropertyResolver 实现，它针对一组基础PropertySources解析属性值。



## EnvironmentCapable

用于持有和向 applicationContext 暴露 environment 的接口
所有Spring应用程序上下文都是 EnvironmentCapable，该接口主要用于在接受BeanFactory实例的框架方法中执行 instanceof 检查，这些实例可能实际上也可能不是ApplicationContext实例，以便在环境可用时与环境进行交互。

如前所述，ApplicationContext 扩展了 EnvironmentCapable，从而暴露了一个 getEnvironment（）方法; 但是，ConfigurableApplicationContext 重新定义了getEnvironment（）并缩小了签名以返回ConfigurableEnvironment。 结果是，在从ConfigurableApplicationContext访问Environment对象之前，它是“只读”，此时它也可以配置。

## 总结

Environment 代表了当前程序所处的 Profile，和当前程序可以获取的 key/value 属性 properties。