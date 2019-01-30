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

可以从文件添加Property ，并处理。

允许从属性资源（即属性文件）配置各个 bean 属性值。 对于以系统管理员为目标的自定义配置文件非常有用，这些文件覆盖在应用程

分发中提供了两个具体实现：

PropertyOverrideConfigurer 用于“beanName.property = value”样式重写（将属性文件中的值推送到bean定义中）
PropertyPlaceholderConfigurer 用于替换“$ {...}”占位符（将属性文件中的值拉入bean定义）

通过重写 `convertPropertyValue（java.lang.String） `方法，可以在读取属性值后转换它们。 例如，可以在处理加密值之前相应地检测和解密加密值。