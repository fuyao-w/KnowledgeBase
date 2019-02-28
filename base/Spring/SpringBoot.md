### @SpringBootApplication

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(excludeFilters = {
      @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
      @Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class) })
public @interface SpringBootApplication {
```

### 创建 SpringApplication

Spring Boot 通过Main方法启动，在启动类上一般需要注解 `@SpringBootApplication`，该注解上面有三个SpringBoot 提供的注解。需要用户为ComponentScan 设置Spring 自动配置所需要扫描的包。如果不填写默认启动类的包作为根目录。

```java
public static void main(String[] args) {

    SpringApplication.run(MVCdemo.class, args);

    synchronized (AnnoBean.class) {
        try {
            AnnoBean.class.wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
```

通过 SpringApplication 类的run方法启动。

```java
public SpringApplication(Object... sources) {
   initialize(sources);
}
```

run 方法创建 SpringApplication实例，来调用`run`。

```java
private void initialize(Object[] sources) {
   if (sources != null && sources.length > 0) {
      this.sources.addAll(Arrays.asList(sources));
   }
   this.webEnvironment = deduceWebEnvironment();
   setInitializers((Collection) getSpringFactoriesInstances(
         ApplicationContextInitializer.class));
   setListeners((Collection) getSpringFactoriesInstances(ApplicationListener.class));
   this.mainApplicationClass = deduceMainApplicationClass();
}
```

`initialize` 向 `Set<Object> sources`添加启动类，可以获取其所在包作为扫描用户目录的跟目录。所以也可以不向`SpringBootApplication`注解添加 `scanBasePackages`。

接下来调用 `deduceWebEnvironment` 来确认当前是否是 web 环境。

```java
private static final String[] WEB_ENVIRONMENT_CLASSES = { "javax.servlet.Servlet",		"org.springframework.web.context.ConfigurableWebApplicationContext" };
private boolean deduceWebEnvironment() {
   for (String className : WEB_ENVIRONMENT_CLASSES) {
      if (!ClassUtils.isPresent(className, null)) {
         return false;
      }
   }
   return true;
}
```

只要Classpath 中有 `servlet`和 `ConfigurableWebApplicationContext` 这两个类，就代表当前可以是  web 环境。

然后设置`ApplicationContextInitializer`的 初始化回调。但首先应该找到并创建他们。

```java
private <T> Collection<? extends T> getSpringFactoriesInstances(Class<T> type,
      Class<?>[] parameterTypes, Object... args) {
   ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
   // Use names and ensure unique to protect against duplicates
   Set<String> names = new LinkedHashSet<String>(
         SpringFactoriesLoader.loadFactoryNames(type, classLoader));
   List<T> instances = createSpringFactoriesInstances(type, parameterTypes,
         classLoader, args, names);
   AnnotationAwareOrderComparator.sort(instances);
   return instances;
}
```

```java
public static List<String> loadFactoryNames(Class<?> factoryClass, ClassLoader classLoader) {
   String factoryClassName = factoryClass.getName();
   try {
      Enumeration<URL> urls = (classLoader != null ? classLoader.getResources(FACTORIES_RESOURCE_LOCATION) :
            ClassLoader.getSystemResources(FACTORIES_RESOURCE_LOCATION));
      List<String> result = new ArrayList<String>();
      while (urls.hasMoreElements()) {
         URL url = urls.nextElement();
         Properties properties = PropertiesLoaderUtils.loadProperties(new UrlResource(url));
         String propertyValue = properties.getProperty(factoryClassName);
         for (String factoryName : StringUtils.commaDelimitedListToStringArray(propertyValue)) {
            result.add(factoryName.trim());
         }
      }
      return result;
   }
   catch (IOException ex) {
      throw new IllegalArgumentException("Unable to load factories from location [" +
            FACTORIES_RESOURCE_LOCATION + "]", ex);
   }
}
```

调用`loadFactoryNames ` 加载`ApplicationContextInitializer`类型的类名。为什么要获取`ClassLoader classLoader = Thread.currentThread().getContextClassLoader();`？这个获取 ClassLader 事实上破坏了java 的双亲委派模型。因为spring 现在需要从 `public static final String FACTORIES_RESOURCE_LOCATION = "META-INF/spring.factories";`获取信息。这是 java 的 SPI 机制。但是如果使用正常的双亲委派的话，就加载不到ClassPath所在的资源了。所以求全在Thread 中加入了`getContextClassLoader`方法。

然后将获取的文本读入property，最终在ClassPath 获得了六个Class。

0 = "org.springframework.boot.context.ConfigurationWarningsApplicationContextInitializer"
1 = "org.springframework.boot.context.ContextIdApplicationContextInitializer"
2 = "org.springframework.boot.context.config.DelegatingApplicationContextInitializer"
3 = "org.springframework.boot.context.embedded.ServerPortInfoApplicationContextInitializer"
4 = "org.springframework.boot.autoconfigure.SharedMetadataReaderFactoryContextInitializer"
5 = "org.springframework.boot.autoconfigure.logging.AutoConfigurationReportLoggingInitializer"

然后调用 `createSpringFactoriesInstances` 创建其实例。

按相容的办法创建了十个监听器。

0 = {ConfigFileApplicationListener@3359} 
1 = {AnsiOutputApplicationListener@3360} 
2 = {LoggingApplicationListener@3361} 
3 = {ClasspathLoggingApplicationListener@3362} 
4 = {BackgroundPreinitializer@3363} 
5 = {DelegatingApplicationListener@3364} 
6 = {ParentContextCloserApplicationListener@3365} 
7 = {ClearCachesApplicationListener@3366} 
8 = {FileEncodingApplicationListener@3367} 
9 = {LiquibaseServiceLocatorApplicationListener@3368} 

最后 调用 `deduceMainApplicationClass();`获取main函数所在的Class。

### 调用`run`

创建完SprignApplication 实例后就可以调用其 run 方法了。

```java
public ConfigurableApplicationContext run(String... args) {
   StopWatch stopWatch = new StopWatch();
   stopWatch.start();
   ConfigurableApplicationContext context = null;
   FailureAnalyzers analyzers = null;
   configureHeadlessProperty();
   SpringApplicationRunListeners listeners = getRunListeners(args);
   listeners.starting();
   try {
      ApplicationArguments applicationArguments = new DefaultApplicationArguments(
            args);
      ConfigurableEnvironment environment = prepareEnvironment(listeners,
            applicationArguments);
      Banner printedBanner = printBanner(environment);
      context = createApplicationContext();
      analyzers = new FailureAnalyzers(context);
      prepareContext(context, environment, listeners, applicationArguments,
            printedBanner);
      refreshContext(context);
      afterRefresh(context, applicationArguments);
      listeners.finished(context, null);
      stopWatch.stop();
      if (this.logStartupInfo) {
         new StartupInfoLogger(this.mainApplicationClass)
               .logStarted(getApplicationLog(), stopWatch);
      }
      return context;
   }
   catch (Throwable ex) {
      handleRunFailure(context, listeners, analyzers, ex);
      throw new IllegalStateException(ex);
   }
}
```

首先穿件了一个`StopWatch`类，用于监控功能Spring 的启动情况。

接下来要发布通过监听器发布事件了。

```java
private SpringApplicationRunListeners getRunListeners(String[] args) {
   Class<?>[] types = new Class<?>[] { SpringApplication.class, String[].class };
   return new SpringApplicationRunListeners(logger, getSpringFactoriesInstances(
         SpringApplicationRunListener.class, types, this, args));
}
```

依然通过SPI 加载Class。加载后调用`listeners.starting();`发布时间。

接下来创建 ApplicationArguments和ConfigurableEnvironment对象，并且在 `ConfigurableEnvironment`创建中发布事件。

```java
Banner printedBanner = printBanner(environment);
```

然后将spring 的启动 banner 图绘制出来。

下一步根据当前是否是web环境创建 `AnnotationConfigEmbeddedWebApplicationContext`或者`AnnotationConfigApplicationContext`。

```java
protected ConfigurableApplicationContext createApplicationContext() {
   Class<?> contextClass = this.applicationContextClass;
   if (contextClass == null) {
      try {
         contextClass = Class.forName(this.webEnvironment
               ? DEFAULT_WEB_CONTEXT_CLASS : DEFAULT_CONTEXT_CLASS);
      }
      catch (ClassNotFoundException ex) {
         throw new IllegalStateException(
               "Unable create a default ApplicationContext, "
                     + "please specify an ApplicationContextClass",
               ex);
      }
   }
   return (ConfigurableApplicationContext) BeanUtils.instantiate(contextClass);
}
```

实例化 上下文后，对其进行一些设置。

```java
private void prepareContext(ConfigurableApplicationContext context,
      ConfigurableEnvironment environment, SpringApplicationRunListeners listeners,
      ApplicationArguments applicationArguments, Banner printedBanner) {
   context.setEnvironment(environment);
   postProcessApplicationContext(context);
   applyInitializers(context);
   listeners.contextPrepared(context);
   if (this.logStartupInfo) {
      logStartupInfo(context.getParent() == null);
      logStartupProfileInfo(context);
   }

   // Add boot specific singleton beans
   context.getBeanFactory().registerSingleton("springApplicationArguments",
         applicationArguments);
   if (printedBanner != null) {
      context.getBeanFactory().registerSingleton("springBootBanner", printedBanner);
   }

   // Load the sources
   Set<Object> sources = getSources();
   Assert.notEmpty(sources, "Sources must not be empty");
   load(context, sources.toArray(new Object[sources.size()]));
   listeners.contextLoaded(context);
}
```

重要的一步是调用 `load` 加载启动类的 BeanDefinition 并向 bean定义注册器注册。然后向 context 注册监听器，并发布相应事件。

准备工作做完就开始正式 refresh 了。

```java
private void refreshContext(ConfigurableApplicationContext context) {
   refresh(context);
   if (this.registerShutdownHook) {
      try {
         context.registerShutdownHook();
      }
      catch (AccessControlException ex) {
         // Not allowed in some environments.
      }
   }
}
```