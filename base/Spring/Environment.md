## Environment

环境是集成在容器中的抽象，它模拟了应用程序环境的两个关键方面：[*profiles*](https://docs.spring.io/spring/docs/4.3.22.RELEASE/spring-framework-reference/htmlsingle/#beans-definition-profiles) 和 [*properties*](https://docs.spring.io/spring/docs/4.3.22.RELEASE/spring-framework-reference/htmlsingle/#beans-property-source-abstraction).。

profile 是仅在给定配置文件处于活动状态时才向容器注册的Bean定义的命名逻辑组。 可以将Bean分配给配置文件，无论是以XML还是通过注释定义。 与配置文件相关的 Environment 对象的作用是确定哪些配置文件（如果有）当前处于活动状态，以及默认情况下哪些配置文件（如果有）应处于活动状态。

properties 在几乎所有应用程序中都发挥着重要作用，可能源自各种源：属性文件，JVM系统属性，系统环境变量，JNDI，servlet上下文参数，ad-hoc属性对象，Maps 等。 与属性相关的Environment 对象的作用是为用户提供方便的服务接口，用于配置属性源和从中解析属性。

### EnvironmentCapable

指示包含和公开 Environment引用的组件的接口。

所有Spring应用程序上下文都是 EnvironmentCapable，该接口主要用于在接受BeanFactory实例的框架方法中执行 instanceof 检查，这些实例可能实际上也可能不是ApplicationContext实例，以便在环境可用时与环境进行交互。

如前所述，ApplicationContext 扩展了 EnvironmentCapable，从而暴露了一个`getEnvironment（）`方法; 但是，ConfigurableApplicationContext 重新定义了 `getEnvironment（）`并缩小了签名以返回 ConfigurableEnvironment。 结果是，在从 ConfigurableApplicationContext 访问 Environment 对象之前，它是“只读”，此时它也可以配置。

![](G:\KnowledgeBase\picture\Spring\environment_two.png)

## PropertySource

表示 name/value 属性对的源的抽象基类。底层源对象可以是封装属性的任何类型T。示例包括Properties对象，Map对象，ServletContext和ServletConfig对象（用于访问init参数）。探索PropertySource类型层次结构以查看提供的实现。
PropertySource对象通常不是孤立使用的，而是通过 PropertySources 对象使用，该对象聚合属性源并与 PropertyResolver 实现结合使用，PropertyResolver实现可以跨PropertySource集执行基于优先级的搜索。

PropertySource标识不是基于封装属性的内容确定的，而是基于PropertySource的名称。这对于在集合上下文中操作PropertySource对象很有用。有关详细信息，请参阅MutablePropertySources中的操作以及`named（String）`和`toString（）`方法。

请注意，在使用 @Configuration 类时，@PropertyTource 注解提供了一种方便的声明式方法，可以将属性源添加到封闭环境中。









## 小结 ##

与 Environment 相关的类在 spring-core.env 包下面。

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
 MutablePropertySources propertySources = environment.getPropertySources();
 MockPropertySource mockEnvVars = new MockPropertySource().withProperty("xyz", "myValue");
 propertySources.replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, mockEnvVars);
```

当ApplicationContext正在使用Environment时，在调用上下文的refresh（）方法之前执行任何此类PropertySource操作都很重要。 这可确保在容器引导过程中所有属性源都可用，包括属性占位符配置器使用。

定义了设置活动profile，系统 profile，合并 profile 的方法。

### AbstractEnvironment

环境实现的抽象基类。支持保留的默认配置文件名称的概念，并允许通过ACTIVE_PROFILES_PROPERTY_NAME和DEFAULT_PROFILES_PROPERTY_NAME属性指定活动和默认配置文件。

具体的子类主要区别在于它们默认添加的 PropertySource 对象。 AbstractEnvironment 没有添加任何内容。 子类应通过受保护的 `customizePropertySources（MutablePropertySources）`钩子提供属性源，而客户端应使用`ConfigurableEnvironment.getPropertySources（）`进行自定义并对MutablePropertySources API 进行操作。 有关用法示例，请参阅ConfigurableEnvironment javadoc。