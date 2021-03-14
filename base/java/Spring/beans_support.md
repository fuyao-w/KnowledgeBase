beans.support 包下面是一些重要的接口的实现。

### DefaultSingletonBeanRegistry

共享bean实例的通用注册表，实现SingletonBeanRegistry。 允许通过bean名称注册应该为注册表的所有调用者共享的单例实例。
还支持在关闭注册表时销毁DisposableBean实例（可能对应于已注册的单例，也可能不对应于已注册的单例）。 可以注册bean之间的依赖关系以强制执行适当的关闭顺序。

该类主要用作BeanFactory实现的基类，分解了单例 bean 实例的通用管理。 请注意，ConfigurableBeanFactory 接口扩展了SingletonBeanRegistry接口。

请注意，与AbstractBeanFactory和DefaultListableBeanFactory（继承自它）相比，此类既不假定bean定义概念也不假定bean实例的特定创建过程。 或者也可以用作委托的嵌套助手。

默认的单例注册器类内部通过一些列的Map 将BaenFactctory 需要掌管的单例的关系存储下来。

```java
/**
 * null 单例对象的内部标记：
 * 用作 ConcurrentHashMap 的标记值（不支持空值）。
 */
protected static final Object NULL_OBJECT = new Object();


/**单例对象的缓存: bean name --> bean instance */
private final Map<String, Object> singletonObjects = new ConcurrentHashMap<String, Object>(256);

/** 单身工厂的缓存: bean name --> ObjectFactory */
private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<String, ObjectFactory<?>>(16);

/** 早期单例对象的缓存： bean name --> bean instance */
private final Map<String, Object> earlySingletonObjects = new HashMap<String, Object>(16);

/** 已经注册的单例 set, 以注册顺序包含bean名称 */
private final Set<String> registeredSingletons = new LinkedHashSet<String>(256);

/**当前正在创建的bean的名称*/
private final Set<String> singletonsCurrentlyInCreation =
      Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>(16));

/** 当前从创建检查中排除的bean的名称 */
private final Set<String> inCreationCheckExclusions =
      Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>(16));

/** 被抑制的异常列表，可用于关联相关原因 */
private Set<Exception> suppressedExceptions;

/** 用于只是当前是否在 bean 销毁过程中请求 bean */
private boolean singletonsCurrentlyInDestruction = false;

/** Disposable bean 实例: bean name --> disposable instance */
private final Map<String, Object> disposableBeans = new LinkedHashMap<String, Object>();

/** 存储有包含关系的 bean 的Map: bean name --> Set of bean names that the bean contains */
private final Map<String, Set<String>> containedBeanMap = new ConcurrentHashMap<String, Set<String>>(16);

/** Map between dependent bean names: bean name --> Set of dependent bean names */
private final Map<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<String, Set<String>>(64);

/** Map between depending bean names: bean name --> Set of bean names for the bean's dependencies */
private final Map<String, Set<String>> dependenciesForBeanMap = new ConcurrentHashMap<String, Set<String>>(64);
```

### FactoryBeanRegistrySupport

支持需要处理 FactoryBean 实例的单例注册表的基类，与 DefaultSingletonBeanRegistry的单例管理集成。
用作AbstractBeanFactory的基类。

### AbstractBeanFactory

BeanFactory 实现的抽象基类，提供 ConfigurableBeanFactory SPI的全部功能。不可当做可列出的 bean 工厂：因此也可以用作 bean 工厂实现的基类，它从一些后端资源获取 bean 定义（其中bean定义访问是一项昂贵的操作）。
该类提供单例缓存（通过其基类 DefaultSingletonBeanRegistry，单例/原型确定，FactoryBean处理，别名，用于子 bean定义的bean定义合并，以及bean销毁（DisposableBean接口，自定义销毁方法）。此外，它可以管理bean工厂层次结构（通过实现HierarchicalBeanFactory接口，在未知bean的情况下委托父进程）。

子类实现的主要模板方法是 `getBeanDefinition（java.lang.String）`和`createBean（java.lang.String，org.springframework.beans.factory.support.RootBeanDefinition，java.lang.Object []）`，检索一个给定bean名称的bean定义，并分别为给定的bean定义创建bean实例。可以在DefaultListableBeanFactory和AbstractAutowireCapableBeanFactory中找到这些操作的默认实现。

该类没有实现`createBean`方法。

```java
/** 父bean工厂，用于bean继承支持 */
private BeanFactory parentBeanFactory;

/** 如有必要，ClassLoader用于解析bean类名*/
private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

/**如有必要，ClassLoader暂时解析bean类名 */
private ClassLoader tempClassLoader;

/** 是否缓存bean元数据，或者为每次访问重新获取它*/
private boolean cacheBeanMetadata = true;

/** bean定义值中表达式的解析策略 */
private BeanExpressionResolver beanExpressionResolver;

/**使用Spring ConversionService而不是PropertyEditors */
private ConversionService conversionService;

/** 自定义PropertyEditorRegistrars以应用于此工厂的bean */
private final Set<PropertyEditorRegistrar> propertyEditorRegistrars =
      new LinkedHashSet<PropertyEditorRegistrar>(4);

/** 自定义PropertyEditors应用于此工厂的bean */
private final Map<Class<?>, Class<? extends PropertyEditor>> customEditors = new HashMap<Class<?>, Class<? extends PropertyEditor>>(4);

/** 要使用的自定义TypeConverter，请覆盖默认的PropertyEditor机制 */
private TypeConverter typeConverter;

/** String resolvers to apply e.g. to annotation attribute values */
private final List<StringValueResolver> embeddedValueResolvers = new LinkedList<StringValueResolver>();

/** 要在createBean中应用的BeanPostProcessors */
private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<BeanPostProcessor>();

/** 指示是否已注册任何InstantiationAwareBeanPostProcessors*/
private boolean hasInstantiationAwareBeanPostProcessors;

/**指示是否已注册任何DestructionAwareBeanPostProcessors*/
private boolean hasDestructionAwareBeanPostProcessors;

/** 从范围标识符String映射到相应的Scope */
private final Map<String, Scope> scopes = new LinkedHashMap<String, Scope>(8);

/** Security context used when running with a SecurityManager */
private SecurityContextProvider securityContextProvider;

/**从bean名称映射到合并的RootBeanDefinition */
private final Map<String, RootBeanDefinition> mergedBeanDefinitions =
      new ConcurrentHashMap<String, RootBeanDefinition>(256);

/** 已经创建至少一次的bean的名称 */
private final Set<String> alreadyCreated =
      Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>(256));

/** 当前正在创建的bean的名称 */
private final ThreadLocal<Object> prototypesCurrentlyInCreation = new NamedThreadLocal<Object>("Prototype beans currently in creation");
```

### AbstractAutowireCapableBeanFactory

Abstract bean工厂的超类，它实现了默认的bean创建，具有RootBeanDefinition类指定的全部功能。除了AbstractBeanFactory的createBean（java.lang.Class <T>）方法之外，还实现了AutowireCapableBeanFactory接口。
提供bean创建（具有构造函数解析），属性填充，装配（包括自动装配）和初始化。处理运行时bean引用，解析托管集合，调用初始化方法等。支持自动装配构造函数，按名称的属性和按类型的属性。

子类实现的主要模板方法是 AutowireCapableBeanFactory.resolveDependency（DependencyDescriptor，String，Set，TypeConverter），用于按类型自动装配。在工厂能够搜索其bean定义的情况下，匹配bean通常将通过这种搜索来实现。对于其他工厂样式，可以实现简化的匹配算法。

请注意，此类不承担或实现bean定义注册表功能。有关ListableBeanFactory和BeanDefinitionRegistry接口的实现，请参阅DefaultListableBeanFactory，它们分别代表此类工厂的API和SPI视图。

### BeanDefinitionRegistry

包含bean Definition 的注册表的接口，例如 RootBeanDefinition 和 ChildBeanDefinition实例。 通常由BeanFactories实现，BeanFactories 内部使用AbstractBeanDefinition层次结构。
这是Spring的bean facrory包中唯一封装bean Definition 注册的接口。 标准BeanFactory接口仅涵盖对完全配置的工厂实例的访问。

Spring的bean定义读者希望能够在这个接口的实现上工作。 Spring核心中的已知实现者是DefaultListableBeanFactory和GenericApplicationContext 。

### SimpleBeanDefinitionRegistry

BeanDefinitionRegistry接口的简单实现。 仅提供注册表功能，内置无工厂功能。例如，可用于测试bean定义读取器。

核心的获取Bean 的方法是 `doGetBean()`下面来分析这个方法。:

```java
protected <T> T doGetBean(
      final String name, final Class<T> requiredType, final Object[] args, boolean typeCheckOnly)
      throws BeansException {
```

第一步、首先需要将希望获取 bean 的名称转换为规范名称。：

```java
final String beanName = transformedBeanName(name);
```

```java
protected String transformedBeanName(String name) {
   return canonicalName(BeanFactoryUtils.transformedBeanName(name));
}
```

先调用了`BeanFactoryUtils.transformedBeanName`方法处理bean 名称。`BeanFactoryUtils`类在`BeanFactoryUtils.transformedBeanName`包中。

```java
public static String transformedBeanName(String name) {
   Assert.notNull(name, "'name' must not be null");
   String beanName = name;
   while (beanName.startsWith(BeanFactory.FACTORY_BEAN_PREFIX)) {
      beanName = beanName.substring(BeanFactory.FACTORY_BEAN_PREFIX.length());
   }
   return beanName;
}
```

该方法主要将我们想获取 FactoryBean 实例时候，在bean 名称前面添加的前缀 `&`去掉。返回之后在交由`canonicalName`处理。`canonicalName` 是`SimpleAliasRegistry`类中的方法。用于确定原始名称，将别名解析为规范名称。

```java
public String canonicalName(String name) {
   String canonicalName = name;
   // Handle aliasing...
   String resolvedName;
   do {
      resolvedName = this.aliasMap.get(canonicalName);
      if (resolvedName != null) {
         canonicalName = resolvedName;
      }
   }
   while (resolvedName != null);
   return canonicalName;
}
```

这个方法会从`aliasMap`中获取 name 的别名，如果别名还有别名则会一直获取。最后返回最后一个别名。

第二步、检查单例缓存以手动注册单例

```java
Object bean;

// Eagerly check singleton cache for manually registered singletons.
Object sharedInstance = getSingleton(beanName);
if (sharedInstance != null && args == null) {
   if (logger.isDebugEnabled()) {
      if (isSingletonCurrentlyInCreation(beanName)) {
         logger.debug("Returning eagerly cached instance of singleton bean '" + beanName +
               "' that is not fully initialized yet - a consequence of a circular reference");
      }
      else {
         logger.debug("Returning cached instance of singleton bean '" + beanName + "'");
      }
   }
   bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
}
```

在这一步中首先声明了一个 Object，它就是返回给调用者的bean。最终调用`getSingleton`

```java
protected Object getSingleton(String beanName, boolean allowEarlyReference) {
   Object singletonObject = this.singletonObjects.get(beanName);
   if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
      synchronized (this.singletonObjects) {
         singletonObject = this.earlySingletonObjects.get(beanName);
         if (singletonObject == null && allowEarlyReference) {
            ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
            if (singletonFactory != null) {
               singletonObject = singletonFactory.getObject();
               this.earlySingletonObjects.put(beanName, singletonObject);
               this.singletonFactories.remove(beanName);
            }
         }
      }
   }
   return (singletonObject != NULL_OBJECT ? singletonObject : null);
}
```

首先尝试从单例对象的缓存中尝试获取如果没有或者通过`isSingletonCurrentlyInCreation`方法判断该对象正在创建当中，则尝试从早期（有循环依赖没有解析）的单例缓存中获取，