## Bean 工厂

spring ioc 的核心实现在 spring-beans中。ApplicationContext 通过 beanFactory 获取到注册的 bean。

![](G:\KnowledgeBase\picture\Spring\beans_beanfactory.png)

### BeanFactory

用于访问Spring bean 容器的根接口。 这是bean容器的基本客户端视图; 诸如`ListableBeanFactory`和`ConfigurableBeanFactory`之类的其他接口可用于特定目的。

此接口由包含许多 bean 定义的对象实现，每个 bean定义由String名称唯一标识。 根据bean定义，工厂将返回包含对象的独立实例（Prototype 设计模式）或单个共享实例（Singleton 设计模式的高级替代，其中实例是范围中的单例工厂）。 将返回哪种类型的实例取决于 bean 工厂配置：API是相同的。 从Spring 2.0开始，根据具体的应用程序上下文（例如Web环境中的“请求”和“会话”范围），可以使用更多的范围。

这种方法的重点是 BeanFactory 是应用程序组件的**中央注册表**，并集中应用程序组件的配置（例如，不再需要单个对象读取属性文件）。 有关此方法的优点的讨论，请参见“Expert One-on-One J2EE 设计和开发”的第4章和第11章。

请注意，通常最好依靠依赖注入（“ push ”配置）来通过 setter 或构造函数来配置应用程序对象，而不是像BeanFactory查找一样使用任何形式的“ pull ”配置。 Spring的依赖注入功能是使用这个BeanFactory接口及其子接口实现的。

通常，BeanFactory将加载存储在配置源（例如XML文档）中的bean定义，并使用`org.springframework.beans`包来配置bean。 但是，实现可以直接在Java代码中直接返回它创建的Java对象。 对如何存储定义没有限制：LDAP，RDBMS，XML，属性文件等。鼓励实现支持bean之间的引用（依赖注入）。

与 ListableBeanFactory 中的方法相反，如果这是 HierarchicalBeanFactory，则此接口中的所有操作也将检查父工厂。 如果在此工厂实例中找不到 bean，则会询问直接父工厂。 此工厂实例中的Bean应该在任何父工厂中覆盖同名的Bean。

Bean工厂实现应尽可能支持标准bean生命周期接口。 完整的初始化方法及其标准顺序是：

1. BeanNameAware's `setBeanName`
2. BeanClassLoaderAware's `setBeanClassLoader`
3. BeanFactoryAware's `setBeanFactory`
4. EnvironmentAware's `setEnvironment`
5. EmbeddedValueResolverAware's `setEmbeddedValueResolver`
6. ResourceLoaderAware's `setResourceLoader` (only applicable when running in an application context)
7. ApplicationEventPublisherAware's `setApplicationEventPublisher` (only applicable when running in an application context)
8. MessageSourceAware's `setMessageSource` (only applicable when running in an application context)
9. ApplicationContextAware's `setApplicationContext` (only applicable when running in an application context)
10. ServletContextAware's `setServletContext` (only applicable when running in a web application context)
11. `postProcessBeforeInitialization` methods of BeanPostProcessors
12. InitializingBean's `afterPropertiesSet`
13. a custom init-method definition
14. `postProcessAfterInitialization` methods of BeanPostProcessors

关闭bean工厂时，以下生命周期方法适用：

1. `postProcessBeforeDestruction` methods of DestructionAwareBeanPostProcessors
2. DisposableBean's `destroy`
3. a custom destroy-method definition

BeanFactory 定义了一个常量 

```java
String FACTORY_BEAN_PREFIX = "&";
```

用于取消引用 FactoryBean 实例，并将其与FactoryBean创建的bean区分开来。 例如，如果名为myJndiObject的bean是FactoryBean，则获取＆myJndiObject将返回工厂，而不是工厂返回的实例。

##### 用法：

```java
public class SpringFactory implements FactoryBean<SpringBean> {
    @Override
    public SpringBean getObject() throws Exception {
        return new SpringBean("工厂bean");
    }

    @Override
    public Class<?> getObjectType() {
        return SpringBean.class;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }
}
```

##### xml 配置：

```xml
<bean class="spring.SpringFactory" id="springFactory" name="springFactory2" >
    <description>测试spring bean 工厂</description>
</bean>
```

##### 获取：

```java
ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("ApplicationContext.xml");
Object springBean =  applicationContext.getBean("&springFactory");
```

如果springFactory前面加上“&”，则获取到的是 SpringFactory 类的实例，而非通过其`getObject()`方法获取的 bean 实例。

### FactoryBean

由BeanFactory中使用的对象实现的接口，这些对象本身就是单个对象的工厂。 如果bean实现了这个接口，它将被用作公开的对象的工厂，而不是直接作为将自己公开的bean实例。

**注意：实现此接口的bean不能用作普通bean。**FactoryBean以bean样式定义，但通过`getObject（）`创建对象。

FactoryBeans可以支持单例和原型，可以根据需要延迟地创建对象，也可以在启动时就创建对象。 SmartFactoryBean 接口允许公开更细粒度的行为元数据。

此接口在框架内部大量使用，例如AOP ProxyFactoryBean或JndiObjectFactoryBean。 它也可以用于定制组件; 但是，这仅适用于基础架构代码。

FactoryBean是一个程序化合同。 实现不应该依赖注释驱动的注入或其他反射设施。 getObjectType（）getObject（）调用可能在引导过程的早期到达，甚至在任何后处理器设置之前。 如果您需要访问其他bean，请实现BeanFactoryAware并以编程方式获取它们。

最后，FactoryBean对象参与包含BeanFactory的bean创建同步。 除了FactoryBean本身（或类似）中的延迟初始化之外，通常不需要内部同步。

### SmartFactoryBean

FactoryBean 接口的扩展。 实现可以指示它们是否总是返回独立实例，因为它们的FactoryBean.isSingleton（）实现返回false并不清楚地指示独立实例。
如果简答的 FactoryBean.isSingleton（）实现返回false，则假定不实现此扩展接口的FactoryBean 实现始终不是单例的; 只能按需访问公开的对象。

注意：此接口是一个专用接口，主要供框架内和协作框架内部使用。 通常，应用程序提供的FactoryBeans应该只实现普通的FactoryBean接口。 即使在点发行版中，也可以向此扩展接口添加新方法。

### ListableBeanFactory

BeanFactory 接口的扩展，可以枚举所有bean实例，而不是按客户端的请求逐个尝试按名称查找 bean。 预加载所有bean定义（例如基于XML的工厂）的BeanFactory实现可以实现此接口。

如果这是 HierarchicalBeanFactory，则返回值不会考虑任何 BeanFactory 层次结构，而只会涉及当前工厂中定义的bean。使用 BeanFactoryUtils 帮助程序类来考虑祖先工厂中的bean。

此接口中的方法将仅考虑此工厂的 bean 定义。它们将忽略已通过其他方式注册的任何单例bean，如 ConfigurableBeanFactory 的 registerSingleton 方法，但getBeanNamesOfType 和 getBeansOfType 除外，它将检查此类手动注册的单例。当然，BeanFactory的getBean也允许透明访问这些特殊的bean。但是，在典型的场景中，所有bean都将由外部bean定义定义，因此大多数应用程序不需要担心这种区别。

注意：除了getBeanDefinitionCount 和 containsBeanDefinition之外，此接口中的方法不是为频繁调用而设计的。运行可能很慢。

### HierarchicalBeanFactory

由 bean工厂实现的子接口，可以是层次结构的一部分。
可以在ConfigurableBeanFactory接口中找到允许以可配置方式设置父级的bean工厂的相应setParentBeanFactory方法。

## 注入

![](G:\KnowledgeBase\picture\Spring\beans_aware.png)

### Aware

标记超级接口，表明 bean 有资格被 Spring 通过回调方法注入某些属于框架的对象。 实际的方法签名由各个子接口确定，但通常应该只包含一个接受单个参数的void返回方法。
请注意，仅实现Aware不提供默认功能。 相反，必须明确地进行处理，例如在BeanPostProcessor中。 有关处理特定* Aware接口回调的示例，请参阅ApplicationContextAwareProcessor。

可以注入 BeanName、ClassLoader、BeanFactory、在bean 实例创建和销毁时的 init 和 destorty 方法 。

## beans.config 包

该包下面主要定义了 bean 工厂后处理器、bean 后处理器、FactoryBean实例。

## BeanFactoryPostProcessor

用法：启动容器时，在 Bean 实例创建前，修改 BeanDefinition 的属性值（最好不要在业务 bean 上实现它，这会导致，bean的过早实例化）。

允许自定义修改应用程序上下文的 BeanDefinition，调整上下文的基础bean工厂的bean 属性值。
应用程序上下文可以在其bean定义中自动检测BeanFactoryPostProcessor bean，并在创建任何其他bean之前应用它们。

对于以系统管理员为目标的自定义配置文件非常有用，这些文件覆盖在应用程

请参阅 PropertyResourceConfigurer及其针对此类配置需求的开箱即用解决方案的具体实现。

BeanFactoryPostProcessor 可以与 bean 定义交互并修改bean定义，但绝不能与bean实例交互（在 bean 上实现该接口） 。 这样做可能会导致bean过早实例化，违反容器并导致意外的副作用。 如果需要bean实例交互，请考虑实现 BeanPostProcessor。

Spring 也定义了几个 BeanFactoryPostProcessor 的子类来实现特定的工厂后处理器。

### DeprecatedBeanWarner

Bean工厂发布处理器，记录已经过期的 bean的警告。当工厂后处理器的`postProcessBeanFactory`方法扫描到 `@Deprecated` 注解的时候会生成相应的日志提醒该bean已经过期。

### CustomEditorConfigurer

BeanFactoryPostProcessor实现，允许方便地注册自定义属性编辑器。
如果您想注册PropertyEditor实例，从Spring 2.0开始的推荐用法是使用自定义PropertyEditorRegistrar 实现，然后在给定的注册表上注册任何所需的编辑器实例。 每个PropertyEditorRegistrar都可以注册任意数量的自定义编辑器。

```java
 <bean id="customEditorConfigurer" class="org.springframework.beans.factory.config.CustomEditorConfigurer">
   <property name="propertyEditorRegistrars">
     <list>
       <bean class="mypackage.MyCustomDateEditorRegistrar"/>
       <bean class="mypackage.MyObjectEditorRegistrar"/>
     </list>
   </property>
 </bean>
 
```

通过customEditors属性注册PropertyEditor类完全没问题。 Spring将为每次编辑尝试创建它们的新实例：

```java
<bean id="customEditorConfigurer" class="org.springframework.beans.factory.config.CustomEditorConfigurer">
   <property name="customEditors">
     <map>
       <entry key="java.util.Date" value="mypackage.MyCustomDateEditor"/>
       <entry key="mypackage.MyObject" value="mypackage.MyObjectEditor"/>
     </map>
   </property>
 </bean>
```

请注意，您不应该通过customEditors属性注册PropertyEditor bean实例，因为PropertyEditors是有状态的，然后必须为每次编辑尝试同步实例。 如果您需要控制PropertyEditors的实例化过程，请使用PropertyEditorRegistrar注册它们。

还支持“java.lang.String []” - 样式数组类名和基本类名（例如“boolean”）。 代表ClassUtils进行实际的类名解析。

注意：使用此配置器注册的自定义属性编辑器不适用于数据绑定。 需要在DataBinder上注册用于数据绑定的自定义编辑器：使用公共基类或委托给常见的PropertyEditorRegistrar实现来重用编辑器注册。

### CustomScopeConfigurer

用于添加 自定义的 bean 范围，在注册后置工厂处理器时，将自定义的范围，使用该处理器提供的 setter 注入即可。

简单的BeanFactoryPostProcessor实现，它使用包含的ConfigurableBeanFactory注册自定义 Scope。
将使用传递给postProcessBeanFactory（ConfigurableListableBeanFactory）方法的ConfigurableListableBeanFactory注册所有提供的作用域。

此类允许自定义作用域的声明性注册。 或者，考虑实现以编程方式调用ConfigurableBeanFactory.registerScope（java.lang.String，org.springframework.beans.factory.config.Scope）的自定义BeanFactoryPostProcessor。

### PropertyResourceConfigurer

可以从文件添加 Property，并且可以通过重写`convertPropertyValue`对 properties 进行转换处理。

允许从属性资源（即属性文件）配置各个 bean 属性值。 对于以系统管理员为目标的自定义配置文件非常有用，这些文件覆盖在应用程

分发中提供了两个具体实现：

PropertyOverrideConfigurer 用于“beanName.property = value”样式重写（将属性文件中的值推送到bean定义中）
PropertyPlaceholderConfigurer 用于替换“$ {...}”占位符（将属性文件中的值拉入bean定义）

通过重写 `convertPropertyValue（java.lang.String） `方法，可以在读取属性值后转换它们。 例如，可以在处理加密值之前相应地检测和解密加密值。

### PropertyOverrideConfigurer

属性资源配置器，它覆盖应用程序上下文定义中的bean属性值。 它将属性文件中的值推送到bean定义中。
配置行应具有以下形式：

```properties
beanName.property=value
```

Example properties file:

```properties
dataSource.driverClassName=com.mysql.jdbc.Driver
 dataSource.url=jdbc:mysql:mydb
```

与PropertyPlaceholderConfigurer相比，原始定义对于此类bean属性可以具有默认值或根本没有值。 如果覆盖属性文件没有某个bean属性的条目，则使用默认上下文定义。
请注意，上下文定义不知道被覆盖; 因此，在查看XML定义文件时，这并不是很明显。 此外，请注意指定的覆盖值始终是文字值; 它们不会被翻译成bean引用。 当XML bean定义中的原始值指定bean引用时，这也适用。

如果多个PropertyOverrideConfigurers为同一个bean属性定义不同的值，则最后一个将获胜（由于重写机制）。

通过覆盖`convertPropertyValue`方法，可以在读取属性值后转换它们。 例如，可以在处理加密值之前相应地检测和解密加密值。

### PlaceholderConfigurerSupport

一个抽象类，用于解析配置文件占位符。

属性资源配置器的抽象基类，用于解析bean定义属性值中的占位符。 实现将值从属性文件或其他属性源拉入bean定义。
默认占位符语法遵循Ant / Log4J / JSP EL样式：

```
${...}
```

示例XML bean定义：

```xml
<bean id="dataSource"  class="org.springframework.jdbc.datasource.DriverManagerDataSource"/>
   <property name="driverClassName" value="${driver}"/>
   <property name="url" value="jdbc:${dbname}"/>
 </bean>
```

示例属性文件：

```properties
driver=com.mysql.jdbc.Driver
 dbname=mysql:mydb
```

带注释的bean定义可以使用`@Value`注释来利用属性替换：

```java
@Value("${person.age}")
```

实现检查bean引用中的简单属性值，列表，映射，道具和bean名称。 此外，占位符值还可以交叉引用其他占位符，例如：

```xml
rootPath=myrootdir
subPath=${rootPath}/subdir
```

与`PropertyOverrideConfigurer`相比，此类型的子类允许在 bean 定义中填充显式占位符。
如果配置程序无法解析占位符，则将抛出BeanDefinitionStoreException。 如果要检查多个属性文件，请通过locations属性指定多个资源。 您还可以定义多个配置器，每个配置器都有自己的占位符语法。 如果无法解析占位符，请使用`ignoreUnresolvablePlaceholders`故意禁止抛出异常。

可以通过 properties 属性为每个 configurer 实例全局定义默认属性值，也可以使用默认值分隔符逐个属性定义默认值，默认值为`：` 并可通过`setValueSeparator（String）`自定义。

示例具有默认值的XML属性：

```xml
<property name="url" value="jdbc:${dbname:defaultdb}"/>
```

### PropertyPlaceholderConfigurer

PlaceholderConfigurerSupport 子类，用于根据本地属性和/或系统属性和环境变量解析`$ {...}`占位符。
从Spring 3.1开始，PropertySourcesPlaceholderConfigurer 应优先用于此实现; 通过利用Spring 3.1中提供的 Environment 和 PropertySource 机制，它更加灵活。

PropertyPlaceholderConfigurer 仍然适合在以下情况下使用：

- spring-context模块不可用（即，一个使用Spring的BeanFactory API而不是ApplicationContext）。
- 现有配置使用“systemPropertiesMode” 和/或 “systemPropertiesModeName”属性。 鼓励用户不再使用这些设置，而是通过容器的环境配置属性源搜索顺序; 但是，通过继续使用PropertyPlaceholderConfigurer 可以保持功能的精确保留。

### PreferencesPlaceholderConfigurer

PreferencesPlaceholderConfigurer 我们可以通过该类对配置文件中的占位符进行解析。

PropertyPlaceholderConfigurer 的子类，支持JDK 1.4的 Preferences API（java.util.prefs）。
尝试首先将占位符解析为用户首选项中的键，然后是在系统首选项中，然后是在此配置程序的属性中。 因此，如果没有定义相应的首选项，则行为类似于 PropertyPlaceholderConfigurer。

支持系统和用户首选项树的自定义路径。 还支持占位符中指定的自定义路径（`myPath / myPlaceholderKey`）。 如果未指定，则使用相应的根节点。

## 小结

工厂后处理器应该是在 bean 创建之前执行的，需要注意 xml 文件中出现顺序。Spring 提供了几个默认的实现，用于处理过期类，属性编辑器，范围，通过配置文件（可以使用占位符）绑定属性，和 bean 属性。

## bean 后处理器

### BeanPostProcessor

工厂钩子，允许自定义修改新的bean实例，例如检查标记接口或用代理包装它们。
ApplicationContexts 可以在其bean定义中自动检测 BeanPostProcessor bean，并将它们应用于随后创建的任何 bean。 普通bean 工厂允许对后处理器进行编程注册，适用于通过该工厂创建的所有bean。

通常，通过标记接口等填充bean的后处理器将实现postProcessBeforeInitialization（java.lang.Object，java.lang.String），而使用代理包装bean的后处理器通常会实现 postProcessAfterInitialization（java.lang.Object） ，java.lang.String）。

它定义了两个方法：

postProcessBeforeInitialization：在任何bean初始化回调之前将此BeanPostProcessor应用于给定的新bean实例（如 InitializingBean 的 afterPropertiesSet 或自定义 init 方法）。 bean已经填充了属性值。 返回的bean实例可能是原始实例的包装器。

postProcessAfterInitialization：在任何bean初始化回调（如 InitializingBean 的 afterPropertiesSet或自定义 init 方法）之后，将此 BeanPostProcessor 应用于给定的新bean实例。 bean已经填充了属性值。 返回的bean实例可能是原始实例的包装器。
对于FactoryBean，将为FactoryBean实例和FactoryBean创建的对象（从Spring 2.0开始）调用此回调。 后处理器可以通过相应的 Bean instanceof FactoryBean 检查来决定是应用于FactoryBean还是应用于创建的对象。

与所有其他 BeanPostProcessor 回调相比，在`InstantiationAwareBeanPostProcessor.postProcessBeforeInstantiation（java.lang.Class <？>，java.lang.String）`方法触发的短路之后，也将调用此回调。

### InstantiationAwareBeanPostProcessor

BeanPostProcessor 的子接口，用于添加实例化前回调，实例化后但在显式属性设置或自动装配发生之前的回调。
通常用于抑制特定目标bean的默认实例化，例如创建具有特殊 TargetSource 的代理（池化目标，延迟初始化目标等），或实现其他注入策略（如字段注入）。

注意：此接口是一个专用接口，主要供框架内部使用。 建议尽可能实现普通的BeanPostProcessor接口，或者从`InstantiationAwareBeanPostProcessorAdapter`派生，以防止对此接口的扩展。

它添加了一个 `postProcessPropertyValues`方法在工厂将它们应用于给定bean之前对给定属性值进行后处理。

### DestructionAwareBeanPostProcessor

BeanPostProcessor的子接口，它添加了一个bean 销毁前的回调。
典型的用法是调用特定bean类型的自定义销毁回调，匹配相应的初始化回调。

子类需要实现`requiresDestruction` 确定是否有相应类型的 bean 需要该后处理器处理。然后在`postProcessBeforeDestruction` 执行其他的销毁逻辑。

### SmartInstantiationAwareBeanPostProcessor

扩展`InstantiationAwareBeanPostProcessor`接口，添加一个回调以预测已处理bean的最终类型。

注意：此接口是一个专用接口，主要供框架内部使用。 通常，应用程序提供的后处理器应该只实现普通的BeanPostProcessor接口，或者从InstantiationAwareBeanPostProcessorAdapter类派生。 即使在点发行版中，也可能会向此接口添加新方法。

### InstantiationAwareBeanPostProcessorAdapter

将`SmartInstantiationAwareBeanPostProcessor`上的所有方法实现为no-ops的适配器，它不会更改容器实例化的每个bean的正常处理。 子类可以仅覆盖它们实际感兴趣的那些方法。
请注意，仅当您确实需要InstantiationAwareBeanPostProcessor功能时，才建议使用此基类。 如果您只需要简单的BeanPostProcessor功能，则更喜欢该（更简单）接口的直接实现。

## 小结

bean 后处理器是在bean 实例化之后，对bean 进行处理的接口。可以通过它在 bean 声明周期中进行初始化之前，销毁之前执行我们需要的逻辑。

![](G:\KnowledgeBase\picture\Spring\bean_factory.png)

### SingletonBeanRegistry

用于注册单例bean 的注册器。

为共享 bean 实例定义注册表的接口。 可以通过BeanFactory实现来实现，以便以统一的方式公开其单例管理工具。
ConfigurableBeanFactory接口扩展了此接口。

```java
void registerSingleton(java.lang.String beanName,
                       java.lang.Object singletonObject)
```

在给定的bean名称下，在bean注册表中将给定的现有对象注册为singleton。
应该完全初始化给定的实例 ; 注册表不会执行任何初始化回调（特别是，它不会调用InitializingBean的afterPropertiesSet方法）。 给定的实例也不会收到任何销毁回调（如DisposableBean的destroy方法）。

在完整的BeanFactory中运行时： 如果bean应该接收初始化和/或销毁回调，则注册bean definition 而不是现有实例。

通常在注册表配置期间调用，但也可用于单例的运行时注册。 因此，注册表实现应该同步单例访问; 如果它支持 BeanFactory 对单例的懒惰初始化，它将无论如何都必须这样做。

```java
java.lang.Object getSingleton(java.lang.String beanName)
```

返回在给定名称下注册的（原始）单例对象。
只检查已经实例化的单例; 不返回尚未实例化的单例 bean 定义的Object。

此方法的主要目的是访问手动注册的单例（请参阅registerSingleton（java.lang.String，java.lang.Object））。 也可以用于以原始方式访问已经创建的bean定义定义的单例。

注意：此查找方法不知道FactoryBean前缀或别名。 在获取单例实例之前，需要首先解析规范bean名称。

### ConfigurableBeanFactory

该接口提供了 BeanFactory 的各种组件的设置方法。

配置接口由大多数bean factories 实现。 除了 BeanFactory 接口中的bean factory client 方法之外，还提供配置Bean factoriy 的工具。
这个 bean工厂接口并不适用于普通的应用程序代码：坚持使用BeanFactory或ListableBeanFactory来满足典型需求。 这个扩展接口只是为了允许框架内部的即插即用和对bean工厂配置方法的特殊访问。

### AutowireCapableBeanFactory

BeanFactory 接口的扩展由能够自动装配的 bean facrory实现，前提是它们希望为现有bean 实例公开此功能。
BeanFactory 的这个子接口并不适用于普通的应用程序代码：对于典型的用例，坚持使用BeanFactory或ListableBeanFactory。

其他框架的集成代码可以利用此接口来连接和填充 Spring 无法控制其生命周期的现有Bean实例。例如，这对WebWork Actions和Tapestry Page对象特别有用。

请注意，ApplicationContext 外观并未实现此接口，因为应用程序代码几乎不使用它。也就是说，它也可以从应用程序上下文中获得，可以通过ApplicationContext的`ApplicationContext.getAutowireCapableBeanFactory（）`方法访问。

您还可以实现 BeanFactoryAware 接口，该接口即使在 ApplicationContext 中运行时也会公开内部BeanFactory，以访问AutowireCapableBeanFactory：只需将传入的BeanFactory强制转换为AutowireCapableBeanFactory。

### ConfigurableListableBeanFactory

配置接口由大多数可列出的bean Factory实现。 除了 ConfigurableBeanFactory之外，它还提供了分析和修改bean定义以及预先实例化单例的工具。
BeanFactory 的这个子接口并不适用于普通的应用程序代码：对于典型的用例，请坚持使用BeanFactory或ListableBeanFactory。 这个接口只是为了允许框架内部的即插即用，即使需要访问bean工厂配置方法。

![](G:\KnowledgeBase\picture\Spring\beans_factoryBean.png)

### AbstractFactoryBean

```java
public abstract class AbstractFactoryBean<T>
      implements FactoryBean<T>, BeanClassLoaderAware, BeanFactoryAware, InitializingBean, DisposableBean {
```

FactoryBean 实现的简单模板超类，它根据标志创建单例或原型对象。
如果“singleton”标志为true（默认值），则此类将在初始化时创建它只创建一次的对象，然后在对getObject（）方法的所有调用上返回所述单例实例。

否则，每次调用getObject（）方法时，此类都将创建一个新实例。 子类负责实现抽象的createInstance（）模板方法，以实际创建要公开的对象。

该类的子类实现了创建集合的 FactoryBean 。

可以创建 MapFactoryBean、	SetFactoryBean、ListFactoryBean

例子：

只需要继承 ListFactoryBean。

```java
public class MyListFactoryBean extends ListFactoryBean {

}
```

在 xml 中声明 targetListClass 和 sourceList 即可。

```xml
<bean class="spring.MyListFactoryBean" id="listFactoryBean" >
    <property name="targetListClass" value="java.util.LinkedList"></property>
    <property name="sourceList" >
        <list value-type="java.lang.String">
            <value type="java.lang.String" >天津</value>
        </list>
    </property>
    
</bean>
```





### ObjectFactoryCreatingFactoryBean

FactoryBean实现，它返回一个ObjectFactory值，该ObjectFactory又返回一个源自BeanFactory的bean。
因此，这可以用于避免客户端对象直接调用BeanFactory.getBean（String）来从BeanFactory获取（通常是原型）bean，这将违反控制原理的反转。 相反，通过使用此类，客户端对象可以作为一个属性提供给ObjectFactory实例，该属性直接只返回一个目标bean（同样，它通常是一个原型bean）。

基于XML的BeanFactory中的示例配置可能如下所示：

```xml
<beans>
   <!-- Prototype bean since we have state -->
   <bean id="myService" class="a.b.c.MyService" scope="prototype"/>

   <bean id="myServiceFactory"
       class="org.springframework.beans.factory.config.ObjectFactoryCreatingFactoryBean">
     <property name="targetBeanName"><idref local="myService"/></property>
   </bean>

   <bean id="clientBean" class="a.b.c.MyClientBean">
     <property name="myServiceFactory" ref="myServiceFactory"/>
   </bean>

</beans>
```

随之而来的MyClientBean类实现可能如下所示：

```java
package a.b.c;

 import org.springframework.beans.factory.ObjectFactory;

 public class MyClientBean {

   private ObjectFactory<MyService> myServiceFactory;

   public void setMyServiceFactory(ObjectFactory<MyService> myServiceFactory) {
     this.myServiceFactory = myServiceFactory;
   }

   public void someBusinessMethod() {
     // get a 'fresh', brand new MyService instance
     MyService service = this.myServiceFactory.getObject();
     // use the service object to effect the business logic...
   }
 }
```

对象创建模式的这种应用的另一种方法是使用 ServiceLocatorFactoryBean 来源（原型）bean。 ServiceLocatorFactoryBean方法的优点是，不需要依赖任何特定于Spring的接口（如ObjectFactory），但缺点是需要生成运行时类。 请查阅ServiceLocatorFactoryBean JavaDoc以获得更全面的讨论

### ProviderCreatingFactoryBean

FactoryBean 实现，返回一个值，该值是JSR-330 Provider，后者又返回一个源自BeanFactory的bean。
这基本上是Spring的旧的ObjectFactoryCreatingFactoryBean的JSR-330兼容变体。 它可以用于传统的外部依赖注入配置，该配置以javax.inject.Provider类型的属性或构造函数参数为目标，作为JSR-330的 @Inject 注释驱动方法的替代。

## 其他

## BeanDefinition

BeanDefinition描述了一个bean实例，它具有属性值，构造函数参数值以及具体实现提供的更多信息。
这只是一个最小的接口：主要目的是允许 BeanFactoryPostProcessor（如PropertyPlaceholderConfigurer）内省和修改属性值和其他bean元数据。

### BeanDefinitionVisitor

它代表了spring 控制的每个bean 的定义。继承 AttributeAccessor, BeanMetadataElement

遍历BeanDefinition对象的访问者类，特别是包含在其中的属性值和构造函数参数值，解析bean元数据值。
由PropertyPlaceholderConfigurer用于解析BeanDefinition中包含的所有String值，解析找到的任何占位符。

### BeanDefinitionHolder

拥有名称和别名的BeanDefinition的持有者。 可以注册为内部bean的占位符。
也可以用于内部bean定义的编程注册。 如果您不关心BeanNameAware等，注册RootBeanDefinition或ChildBeanDefinition就足够了。