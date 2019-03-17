```java
public class EmbeddedWebApplicationContext extends GenericWebApplicationContext {
```

EmbeddedWebApplicationContext 是嵌入式的Spring boot 上下文，它的AnnotationConfigEmbeddedWebApplicationContext 由spring Boot 实例化。

本文接在 spring boot 分析之后。

### 嵌入式 tomcat 容器的初始化

```java
public class AnnotationConfigEmbeddedWebApplicationContext
      extends EmbeddedWebApplicationContext {
```

```java
public AnnotationConfigEmbeddedWebApplicationContext() {
   this.reader = new AnnotatedBeanDefinitionReader(this);
   this.scanner = new ClassPathBeanDefinitionScanner(this);
}
```

AnnotationConfigEmbeddedWebApplicationContext 的构造方法创建了一个 注解的 bean 定义读取器，一个类路径的 Bean 定义扫描器。

```java
@Override
public final void refresh() throws BeansException, IllegalStateException {
   try {
      super.refresh();
   }
   catch (RuntimeException ex) {
      stopAndReleaseEmbeddedServletContainer();
      throw ex;
   }
}
```

启动上下文的 refresh 方法。

```java
@Override
protected void onRefresh() {
   super.onRefresh();
   try {
      createEmbeddedServletContainer();
   }
   catch (Throwable ex) {
      throw new ApplicationContextException("Unable to start embedded container",
            ex);
   }
}
```

refresh 中的`onRefresh` 方法调用 `createEmbeddedServletContainer`，创建嵌入式servlet 容器。下面简单了解一下。

```java
public interface EmbeddedServletContainer {

	/**
	 * Starts the embedded servlet container. Calling this method on an already started
	 * container has no effect.
	 * @throws EmbeddedServletContainerException if the container cannot be started
	 */
	void start() throws EmbeddedServletContainerException;

	/**
	 * Stops the embedded servlet container. Calling this method on an already stopped
	 * container has no effect.
	 * @throws EmbeddedServletContainerException if the container cannot be stopped
	 */
	void stop() throws EmbeddedServletContainerException;

	/**
	 * Return the port this server is listening on.
	 * @return the port (or -1 if none)
	 */
	int getPort();

}
```

`EmbeddedServletContainer` 为嵌入式servlet 容器的通用接口。该接口有三个实现类，分别为

1. TomcatEmbeddedServletContainer
2. JettyEmbeddedServletContainer
3. UndertowEmbeddedServletContainer



```java
public class TomcatEmbeddedServletContainer implements EmbeddedServletContainer {

   private static final Log logger = LogFactory
         .getLog(TomcatEmbeddedServletContainer.class);

   private static final AtomicInteger containerCounter = new AtomicInteger(-1);

   private final Object monitor = new Object();

   private final Map<Service, Connector[]> serviceConnectors = new HashMap<Service, Connector[]>();

   private final Tomcat tomcat;

   private final boolean autoStart;

   private volatile boolean started;
```

其中的 `Tomcat` 字段就是嵌入式的 `Tomcat`

```java
private void createEmbeddedServletContainer() {
   EmbeddedServletContainer localContainer = this.embeddedServletContainer;
   ServletContext localServletContext = getServletContext();
   if (localContainer == null && localServletContext == null) {
      EmbeddedServletContainerFactory containerFactory = getEmbeddedServletContainerFactory();
      this.embeddedServletContainer = containerFactory
            .getEmbeddedServletContainer(getSelfInitializer());
   }
   else if (localServletContext != null) {
      try {
         getSelfInitializer().onStartup(localServletContext);
      }
      catch (ServletException ex) {
         throw new ApplicationContextException("Cannot initialize servlet context",
               ex);
      }
   }
   initPropertySources();
}
```

首先获取 `EmbeddedServletContainer` 与 `servletContext`，他俩为空的时候，就需要通过`EmbeddedServletContainerFactory` 从 beanFactory 中取出`EmbeddedServletContainerFactory`。

```java
public class TomcatEmbeddedServletContainerFactory
      extends AbstractEmbeddedServletContainerFactory implements ResourceLoaderAware {
```

默认获取 `TomcatEmbeddedServletContainerFactory` 然后调用其`getEmbeddedServletContainer`。

```java
public EmbeddedServletContainer getEmbeddedServletContainer(
      ServletContextInitializer... initializers) {
   Tomcat tomcat = new Tomcat();
   File baseDir = (this.baseDirectory != null) ? this.baseDirectory
         : createTempDir("tomcat");
   tomcat.setBaseDir(baseDir.getAbsolutePath());
   Connector connector = new Connector(this.protocol);
   tomcat.getService().addConnector(connector);
   customizeConnector(connector);
   tomcat.setConnector(connector);
   tomcat.getHost().setAutoDeploy(false);
   configureEngine(tomcat.getEngine());
   for (Connector additionalConnector : this.additionalTomcatConnectors) {
      tomcat.getService().addConnector(additionalConnector);
   }
   prepareContext(tomcat.getHost(), initializers);
   return getTomcatEmbeddedServletContainer(tomcat);
}
```

在这里实例化嵌入式`Tomcat`然后调用`getTomcatEmbeddedServletContainer`，将 tomcat 作为参数，new 一个 `TomcatEmbeddedServletContainer`。

如果`localServletContext`不为空。还要对其进行初始化设置。

创建了一个匿名内部类。

```java
private org.springframework.boot.web.servlet.ServletContextInitializer getSelfInitializer() {
   return new ServletContextInitializer() {
      @Override
      public void onStartup(ServletContext servletContext) throws ServletException {
         selfInitialize(servletContext);
      }
   };
}
```

实际调用的是。

```java
private void selfInitialize(ServletContext servletContext) throws ServletException {
   prepareEmbeddedWebApplicationContext(servletContext);
   registerApplicationScope(servletContext);
   WebApplicationContextUtils.registerEnvironmentBeans(getBeanFactory(),
         servletContext);
   for (ServletContextInitializer beans : getServletContextInitializerBeans()) {
      beans.onStartup(servletContext);
   }
}
```

主要的工作是注册了三个web 相关的`scope`。

最后根据 servletContext 手机属性源。

### 注意：创建 tomcat 容器的时候，就已经通过 FutureTask 接口异步扫描注册了所有的 servlet 的映射地址。

### 

Spring MVC 接下来的行为，是将所有 controller 里面的注解 RequestMapping 的方法的映射路径进行注册。

执行步骤在 refresh 的 `finishBeanFactoryInitialization`方法中。`finishBeanFactoryInitialization` 的功能是将所有为实例化的bean（非lazy init 的）。

`finishBeanFactoryInitialization` 里面在实例化 bean 的同时，还会执行 spring 的所有回调接口，比如 `Aware接口` `beanPostProcess` 、`InitializingBean` 。

```java
public class RequestMappingHandlerMapping extends RequestMappingInfoHandlerMapping
		implements MatchableHandlerMapping, EmbeddedValueResolverAware {
```

` RequestMappingHandlerMapping` 就是处理 `Controller` 中注解 `RequestMapping` 方法的处理器，如果在网上查，一般会说，通过HandlerMapping 找到`handle` ，但是没说究竟`handle`是什么。接下来的步骤就是 找出这些 `handle`。`RequestMappingHandlerMapping` 继承`RequestMappingInfoHandlerMapping`，他又继承自`AbstractHandlerMethodMapping`

```java
public abstract class AbstractHandlerMethodMapping<T> extends AbstractHandlerMapping implements InitializingBean {
```

注意到`AbstractHandlerMethodMapping` 实现了 `InitializingBean` ，关键步骤就在其回调方法上实现。

直接看回调方法`afterPropertiesSet`

```java
@Override
public void afterPropertiesSet() {
   this.config = new RequestMappingInfo.BuilderConfiguration();
   this.config.setUrlPathHelper(getUrlPathHelper());
   this.config.setPathMatcher(getPathMatcher());
   this.config.setSuffixPatternMatch(this.useSuffixPatternMatch);
   this.config.setTrailingSlashMatch(this.useTrailingSlashMatch);
   this.config.setRegisteredSuffixPatternMatch(this.useRegisteredSuffixPatternMatch);
   this.config.setContentNegotiationManager(getContentNegotiationManager());

   super.afterPropertiesSet();
}
```

首先创建`BuilderConfiguration` 类，这个类用于解析 path用。

然后调用父类的

```java
@Override
public void afterPropertiesSet() {
   initHandlerMethods();
}

/**
 * Scan beans in the ApplicationContext, detect and register handler methods.
 * @see #isHandler(Class)
 * @see #getMappingForMethod(Method, Class)
 * @see #handlerMethodsInitialized(Map)
 */
protected void initHandlerMethods() {
   if (logger.isDebugEnabled()) {
      logger.debug("Looking for request mappings in application context: " + getApplicationContext());
   }
   String[] beanNames = (this.detectHandlerMethodsInAncestorContexts ?
         BeanFactoryUtils.beanNamesForTypeIncludingAncestors(getApplicationContext(), Object.class) :
         getApplicationContext().getBeanNamesForType(Object.class));

   for (String beanName : beanNames) {
      if (!beanName.startsWith(SCOPED_TARGET_NAME_PREFIX)) {
         Class<?> beanType = null;
         try {
            beanType = getApplicationContext().getType(beanName);
         }
         catch (Throwable ex) {
            // An unresolvable bean type, probably from a lazy bean - let's ignore it.
            if (logger.isDebugEnabled()) {
               logger.debug("Could not resolve target class for bean with name '" + beanName + "'", ex);
            }
         }
         if (beanType != null && isHandler(beanType)) {
            detectHandlerMethods(beanName);
         }
      }
   }
   handlerMethodsInitialized(getHandlerMethods());
}
```

首先BeanFactory  中按 `Object `类型获取所有的 bean 名称。

如果 bean 名称不是 `scopedTarget`开头的就处理该实例。下面获取该bean 的`Class` ,然后检查该Class 是否是Handler。

```java
@Override
protected boolean isHandler(Class<?> beanType) {
   return (AnnotatedElementUtils.hasAnnotation(beanType, Controller.class) ||
         AnnotatedElementUtils.hasAnnotation(beanType, RequestMapping.class));
}
```

就是检查该类有没有 `Controller`，或者`RequestMapping` 这连个注解。

```java
protected void detectHandlerMethods(final Object handler) {
   Class<?> handlerType = (handler instanceof String ?
         getApplicationContext().getType((String) handler) : handler.getClass());
   final Class<?> userType = ClassUtils.getUserClass(handlerType);

   Map<Method, T> methods = MethodIntrospector.selectMethods(userType,
         new MethodIntrospector.MetadataLookup<T>() {
            @Override
            public T inspect(Method method) {
               try {
                  return getMappingForMethod(method, userType);
               }
               catch (Throwable ex) {
                  throw new IllegalStateException("Invalid mapping on handler class [" +
                        userType.getName() + "]: " + method, ex);
               }
            }
         });

   if (logger.isDebugEnabled()) {
      logger.debug(methods.size() + " request handler methods found on " + userType + ": " + methods);
   }
   for (Map.Entry<Method, T> entry : methods.entrySet()) {
      Method invocableMethod = AopUtils.selectInvocableMethod(entry.getKey(), userType);
      T mapping = entry.getValue();
      registerHandlerMethod(handler, invocableMethod, mapping);
   }
}
```

调用 `detectHandlerMethods` 寻找Controller 中的处理方法。

如果传入的是 bean 的名称则，通过 BeanFactory 获取其Class 类。如果该类是 代理类，则通过 ClassUtils 获取其超类。

然后调用`selectMethods`创建一个匿名内部类调用 `getMappingForMethod` 找到映射方法。

```java
public static <T> Map<Method, T> selectMethods(Class<?> targetType, final MetadataLookup<T> metadataLookup) {
   final Map<Method, T> methodMap = new LinkedHashMap<Method, T>();
   Set<Class<?>> handlerTypes = new LinkedHashSet<Class<?>>();
   Class<?> specificHandlerType = null;

   if (!Proxy.isProxyClass(targetType)) {
      handlerTypes.add(targetType);
      specificHandlerType = targetType;
   }
   handlerTypes.addAll(Arrays.asList(targetType.getInterfaces()));

   for (Class<?> currentHandlerType : handlerTypes) {
      final Class<?> targetClass = (specificHandlerType != null ? specificHandlerType : currentHandlerType);

      ReflectionUtils.doWithMethods(currentHandlerType, new ReflectionUtils.MethodCallback() {
         @Override
         public void doWith(Method method) {
            Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
            T result = metadataLookup.inspect(specificMethod);
            if (result != null) {
               Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);
               if (bridgedMethod == specificMethod || metadataLookup.inspect(bridgedMethod) == null) {
                  methodMap.put(specificMethod, result);
               }
            }
         }
      }, ReflectionUtils.USER_DECLARED_METHODS);
   }

   return methodMap;
}
```

可以看出最后将处理方法通过Map 返回，Map的value 类型为T，其实就是每个映射的实例。

先将该类和其实现的接口添加进入`handlerTypes`集合。然后将所有的方法调用`MetadataLookup`的`inspect`方法得到结果。实际调用的是`getMappingForMethod`方法。

```java
@Override
protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
   RequestMappingInfo info = createRequestMappingInfo(method);
   if (info != null) {
      RequestMappingInfo typeInfo = createRequestMappingInfo(handlerType);
      if (typeInfo != null) {
         info = typeInfo.combine(info);
      }
   }
   return info;
}
```

`handlerType`参数是处理的bean 的class 类型。`创建了一个`RequestMappingInfo`，他是一个包含该requestMapping里标注的所有条件的集合。如果info不为空，则在创建一个在类上标注 `requestMapping`的 `RequestMappingInfo`实例，然后将其组合在一起。可以形成一个完整的映射路径。

```java
private RequestMappingInfo createRequestMappingInfo(AnnotatedElement element) {
   RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(element, RequestMapping.class);
   RequestCondition<?> condition = (element instanceof Class ?
         getCustomTypeCondition((Class<?>) element) : getCustomMethodCondition((Method) element));
   return (requestMapping != null ? createRequestMappingInfo(requestMapping, condition) : null);
}
```

寻找该方法上有没有`RequestMapping注解`，有的话处理该方法，将注解中所有的条件提取出来。

```java
protected RequestMappingInfo createRequestMappingInfo(
      RequestMapping requestMapping, RequestCondition<?> customCondition) {

   return RequestMappingInfo
         .paths(resolveEmbeddedValuesInPatterns(requestMapping.path()))
         .methods(requestMapping.method())
         .params(requestMapping.params())
         .headers(requestMapping.headers())
         .consumes(requestMapping.consumes())
         .produces(requestMapping.produces())
         .mappingName(requestMapping.name())
         .customCondition(customCondition)
         .options(this.config)
         .build();
}
```

创建一个该处理方法的实例，将其所有的参数添加进去。

回到 `selectMethods`方法中，返回的 RequestMappingInfo 实例即为result T。然后将其 put 进Map 中，key 为方法 Method 实例，value 为T。

找到所有的 映射方法之后，就回到了`detectHandlerMethods`，最后的工作是将所有的方法注册到 `MappingRegistry`中。

```java
for (Map.Entry<Method, T> entry : methods.entrySet()) {
   Method invocableMethod = AopUtils.selectInvocableMethod(entry.getKey(), userType);
   T mapping = entry.getValue();
   registerHandlerMethod(handler, invocableMethod, mapping);
}
```

三个参数为映射方法的bean ，这里用string 的名称代替。调用映射方法真正要执行的方法比如父类接口上的方法，最后一个是RequestMappingInfo。

```java
public void register(T mapping, Object handler, Method method) {
   this.readWriteLock.writeLock().lock();
   try {
      HandlerMethod handlerMethod = createHandlerMethod(handler, method);
      assertUniqueMethodMapping(handlerMethod, mapping);

      if (logger.isInfoEnabled()) {
         logger.info("Mapped \"" + mapping + "\" onto " + handlerMethod);
      }
      this.mappingLookup.put(mapping, handlerMethod);

      List<String> directUrls = getDirectUrls(mapping);
      for (String url : directUrls) {
         this.urlLookup.add(url, mapping);
      }

      String name = null;
      if (getNamingStrategy() != null) {
         name = getNamingStrategy().getName(handlerMethod, mapping);
         addMappingName(name, handlerMethod);
      }

      CorsConfiguration corsConfig = initCorsConfiguration(handler, method, mapping);
      if (corsConfig != null) {
         this.corsLookup.put(handlerMethod, corsConfig);
      }

      this.registry.put(mapping, new MappingRegistration<T>(mapping, handlerMethod, directUrls, name));
   }
   finally {
      this.readWriteLock.writeLock().unlock();
   }
}
```

在这里会对找到的方法进行日志记录，我们看到相关的日志记录就来自于这里。

到这里初始化部分结束。

###  请求执行过程

### DispatcherServlet

```java
public class DispatcherServlet extends FrameworkServlet {
```

```java
public abstract class FrameworkServlet extends HttpServletBean implements ApplicationContextAware {
```

```java
public abstract class HttpServletBean extends HttpServlet implements EnvironmentCapable, EnvironmentAware {
```



HTTP请求处理程序/控制器的中央调度程序，例如 用于Web UI控制器或基于HTTP的远程服务导出器。 调度到已注册的处理程序以处理Web请求，提供方便的映射和异常处理工具。
这个servlet非常灵活：它可以与几乎任何工作流一起使用，并安装适当的适配器类。 它提供以下功能，使其与其他请求驱动的Web MVC框架区别开来：

* 它基于JavaBeans配置机制。
* 它可以使用任何HandlerMapping实现 - 预构建或作为应用程序的一部分提供 - 来控制对处理程序对象的请求路由。 默认为BeanNameUrlHandlerMapping和DefaultAnnotationHandlerMapping。 HandlerMapping对象可以在servlet的应用程序上下文中定义为bean，实现HandlerMapping接口，覆盖默认的HandlerMapping（如果存在）。 HandlerMappings可以被赋予任何bean名称（它们按类型进行测试）。
* 它可以使用任何HandlerAdapter; 这允许使用任何处理程序接口。 默认适配器分别是Spring的HttpRequestHandler和Controller接口的HttpRequestHandlerAdapter，SimpleControllerHandlerAdapter。 还将注册默认的AnnotationMethodHandlerAdapter。 HandlerAdapter对象可以作为bean添加到应用程序上下文中，覆盖默认的HandlerAdapter。 与HandlerMappings一样，HandlerAdapters可以被赋予任何bean名称（它们按类型进行测试）。
* 可以通过HandlerExceptionResolver指定调度程序的异常解析策略，例如将某些异常映射到错误页面。 默认值为AnnotationMethodHandlerExceptionResolver，ResponseStatusExceptionResolver和DefaultHandlerExceptionResolver。 可以通过应用程序上下文覆盖这些HandlerExceptionResolvers。 可以为HandlerExceptionResolver指定任何bean名称（它们按类型进行测试）。
* 可以通过ViewResolver实现指定其视图解析策略，将符号视图名称解析为View对象。默认为InternalResourceViewResolver。 ViewResolver对象可以作为bean添加到应用程序上下文中，覆盖默认的ViewResolver。 ViewResolvers可以被赋予任何bean名称（它们按类型进行测试）。
* 如果用户未提供视图或视图名称，则配置的RequestToViewNameTranslator会将当前请求转换为视图名称。相应的bean名称是“viewNameTranslator”;默认值为DefaultRequestToViewNameTranslator。
* 调度程序解决多部分请求的策略由MultipartResolver实现决定。包括Apache Commons FileUpload和Servlet 3的实现;典型的选择是CommonsMultipartResolver。 MultipartResolver bean名称是“multipartResolver”;默认为none。
* 其区域设置解析策略由LocaleResolver确定。开箱即用的实现通过HTTP接受标头，cookie或会话工作。 LocaleResolver bean名称是“localeResolver”;默认为AcceptHeaderLocaleResolver。
* 其主题解析策略由ThemeResolver决定。包括固定主题以及cookie和会话存储的实现。 ThemeResolver bean名称是“themeResolver”;默认是FixedThemeResolver。

只有在调度程序中存在相应的HandlerMapping（用于类型级注释）和/或HandlerAdapter（用于方法级注释）时，才会处理@RequestMapping注释。 默认情况下就是这种情况。 但是，如果要定义自定义HandlerMappings或HandlerAdapter，则需要确保定义相应的自定义DefaultAnnotationHandlerMapping和/或AnnotationMethodHandlerAdapter  - 前提是您打算使用@RequestMapping。

Web应用程序可以定义任意数量的DispatcherServlet。 每个servlet将在其自己的命名空间中运行，使用映射，处理程序等加载其自己的应用程序上下文。只有ContextLoaderListener加载的根应用程序上下文（如果有）将被共享。

从Spring 3.1开始，DispatcherServlet现在可以注入Web应用程序上下文，而不是在内部创建自己的上下文。 这在Servlet 3.0+环境中很有用，它支持servlet实例的编程注册。 有关详细信息，请参阅DispatcherServlet（WebApplicationContext）javadoc。



### HandlerMapping

由定义请求和处理程序对象之间的映射的对象实现的接口。
此类可以由应用程序开发人员实现，但这不是必需的，因为BeanNameUrlHandlerMapping和DefaultAnnotationHandlerMapping包含在框架中。如果在应用程序上下文中未注册HandlerMapping bean，则前者是缺省值。

HandlerMapping 实现可以支持映射的拦截器，但不必如此。处理程序将始终包装在HandlerExecutionChain实例中，可选地伴随一些HandlerInterceptor实例。 DispatcherServlet将首先按给定的顺序调用每个HandlerInterceptor的preHandle方法，如果所有preHandle方法都返回true，则最终调用处理程序本身。

参数化此映射的能力是此MVC框架的强大且不寻常的功能。例如，可以基于会话状态，cookie状态或许多其他变量编写自定义映射。没有其他MVC框架似乎同样灵活。

注意：实现可以实现Ordered接口，以便能够指定排序顺序，从而获得DispatcherServlet应用的优先级。非有序实例被视为最低优先级。

HandlerMapping 是用于处理请求，根据地址找到正确的`handler`，然后调用HandlerAdapter 来执行handler 。

### HandlerAdapter

MVC框架SPI，允许核心MVC工作流的参数化。
必须为每个处理程序类型实现的接口，以处理请求。 此接口用于允许DispatcherServlet无限扩展。 DispatcherServlet通过此接口访问所有已安装的处理程序，这意味着它不包含特定于任何处理程序类型的代码。

请注意，处理程序可以是Object类型。 这是为了使其他框架的处理程序能够与此框架集成，而无需自定义编码，以及允许不遵循任何特定Java接口的注释驱动的处理程序对象。

此接口不适用于应用程序开发人员。 它适用于想要开发自己的Web工作流程的处理程序。

注意：HandlerAdapter实现者可以实现Ordered接口，以便能够指定DispatcherServlet应用的排序顺序（因而也是优先级）。 非有序实例被视为最低优先级。



以上三个类就是处理一次请求最主要的三个相关类。

### 处理过程

如果DispatcherServlet 第一次执行请求，则先执行 HttpServlet 的 `init()` 方法（StandardWrapper 类是Tomcat 中的类，该类负责实例化servlet 并执行初始化可以查看 `allocate()`和`invoke（）`方法了解），该方法在`HttpServletBean` 中实现。

```java
@Override
public final void init() throws ServletException {
   if (logger.isDebugEnabled()) {
      logger.debug("Initializing servlet '" + getServletName() + "'");
   }

   // Set bean properties from init parameters.
   PropertyValues pvs = new ServletConfigPropertyValues(getServletConfig(), this.requiredProperties);
   if (!pvs.isEmpty()) {
      try {
         BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(this);
         ResourceLoader resourceLoader = new ServletContextResourceLoader(getServletContext());
         bw.registerCustomEditor(Resource.class, new ResourceEditor(resourceLoader, getEnvironment()));
         initBeanWrapper(bw);
         bw.setPropertyValues(pvs, true);
      }
      catch (BeansException ex) {
         if (logger.isErrorEnabled()) {
            logger.error("Failed to set bean properties on servlet '" + getServletName() + "'", ex);
         }
         throw ex;
      }
   }

   // Let subclasses do whatever initialization they like.
   initServletBean();

   if (logger.isDebugEnabled()) {
      logger.debug("Servlet '" + getServletName() + "' configured successfully");
   }
}
```

首先调用`ServletConfigPropertyValues` 获得必须的（如果有）初始化参数。必须的参数在 HttpServletBeans的`requiredProperties` 集合中。可以由子类来实现在初始化的时候的注入参数的功能。默认没有。`initServletBean`也是由子类实现的初始化逻辑。

```java
protected final void initServletBean() throws ServletException {
   getServletContext().log("Initializing Spring FrameworkServlet '" + getServletName() + "'");
   if (this.logger.isInfoEnabled()) {
      this.logger.info("FrameworkServlet '" + getServletName() + "': initialization started");
   }
   long startTime = System.currentTimeMillis();

   try {
      this.webApplicationContext = initWebApplicationContext();
      initFrameworkServlet();
   }
   catch (ServletException ex) {
      this.logger.error("Context initialization failed", ex);
      throw ex;
   }
   catch (RuntimeException ex) {
      this.logger.error("Context initialization failed", ex);
      throw ex;
   }

   if (this.logger.isInfoEnabled()) {
      long elapsedTime = System.currentTimeMillis() - startTime;
      this.logger.info("FrameworkServlet '" + getServletName() + "': initialization completed in " +
            elapsedTime + " ms");
   }
}
```

初始化上下文在当前上下文活动的时候任务就是根据上下文初始化 DispatcherServlet 的各项参数，当上下文非活动且有父上下文的时候才进行初始化。

```java
protected WebApplicationContext initWebApplicationContext() {
   WebApplicationContext rootContext =
         WebApplicationContextUtils.getWebApplicationContext(getServletContext());
   WebApplicationContext wac = null;

   if (this.webApplicationContext != null) {
      // A context instance was injected at construction time -> use it
      wac = this.webApplicationContext;
      if (wac instanceof ConfigurableWebApplicationContext) {
         ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) wac;
         if (!cwac.isActive()) {
            // The context has not yet been refreshed -> provide services such as
            // setting the parent context, setting the application context id, etc
            if (cwac.getParent() == null) {
               // The context instance was injected without an explicit parent -> set
               // the root application context (if any; may be null) as the parent
               cwac.setParent(rootContext);
            }
            configureAndRefreshWebApplicationContext(cwac);
         }
      }
   }
   if (wac == null) {
      // No context instance was injected at construction time -> see if one
      // has been registered in the servlet context. If one exists, it is assumed
      // that the parent context (if any) has already been set and that the
      // user has performed any initialization such as setting the context id
      wac = findWebApplicationContext();
   }
   if (wac == null) {
      // No context instance is defined for this servlet -> create a local one
      wac = createWebApplicationContext(rootContext);
   }

   if (!this.refreshEventReceived) {
      // Either the context is not a ConfigurableApplicationContext with refresh
      // support or the context injected at construction time had already been
      // refreshed -> trigger initial onRefresh manually here.
      onRefresh(wac);
   }

   if (this.publishContext) {
      // Publish the context as a servlet context attribute.
      String attrName = getServletContextAttributeName();
      getServletContext().setAttribute(attrName, wac);
      if (this.logger.isDebugEnabled()) {
         this.logger.debug("Published WebApplicationContext of servlet '" + getServletName() +
               "' as ServletContext attribute with name [" + attrName + "]");
      }
   }

   return wac;
}
```

`onRefresh(wac);`执行了向servlet 注入上下文。最后向servletContext 注入上下文。`initFrameworkServlet();`是一个钩子方法，由子类去实现。接下来会记录日志。

正整的分配方法首先是`doService` （在`fromeworkServlet 方法的 processRequest调用）`

```java
protected void doService(HttpServletRequest request, HttpServletResponse response) throws Exception {
   if (logger.isDebugEnabled()) {
      String resumed = WebAsyncUtils.getAsyncManager(request).hasConcurrentResult() ? " resumed" : "";
      logger.debug("DispatcherServlet with name '" + getServletName() + "'" + resumed +
            " processing " + request.getMethod() + " request for [" + getRequestUri(request) + "]");
   }

   // Keep a snapshot of the request attributes in case of an include,
   // to be able to restore the original attributes after the include.
   Map<String, Object> attributesSnapshot = null;
   if (WebUtils.isIncludeRequest(request)) {
      attributesSnapshot = new HashMap<String, Object>();
      Enumeration<?> attrNames = request.getAttributeNames();
      while (attrNames.hasMoreElements()) {
         String attrName = (String) attrNames.nextElement();
         if (this.cleanupAfterInclude || attrName.startsWith(DEFAULT_STRATEGIES_PREFIX)) {
            attributesSnapshot.put(attrName, request.getAttribute(attrName));
         }
      }
   }

   // Make framework objects available to handlers and view objects.
   request.setAttribute(WEB_APPLICATION_CONTEXT_ATTRIBUTE, getWebApplicationContext());
   request.setAttribute(LOCALE_RESOLVER_ATTRIBUTE, this.localeResolver);
   request.setAttribute(THEME_RESOLVER_ATTRIBUTE, this.themeResolver);
   request.setAttribute(THEME_SOURCE_ATTRIBUTE, getThemeSource());

   FlashMap inputFlashMap = this.flashMapManager.retrieveAndUpdate(request, response);
   if (inputFlashMap != null) {
      request.setAttribute(INPUT_FLASH_MAP_ATTRIBUTE, Collections.unmodifiableMap(inputFlashMap));
   }
   request.setAttribute(OUTPUT_FLASH_MAP_ATTRIBUTE, new FlashMap());
   request.setAttribute(FLASH_MAP_MANAGER_ATTRIBUTE, this.flashMapManager);

   try {
      doDispatch(request, response);
   }
   finally {
      if (!WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted()) {
         // Restore the original attribute snapshot, in case of an include.
         if (attributesSnapshot != null) {
            restoreAttributesAfterInclude(request, attributesSnapshot);
         }
      }
   }
}
```

首先保存了本次请求的快照，接下来向request 注入了上下文，主题解析等等。使框架对象可供处理程序和视图对象使用。

然后创建了一个 FlashMap 该类继承HashMap，用于重定向时候使用。创建后调用`doDispatch`处理实际调度到处理程序。

```java
protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
   HttpServletRequest processedRequest = request;
   HandlerExecutionChain mappedHandler = null;
   boolean multipartRequestParsed = false;

   WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);

   try {
      ModelAndView mv = null;
      Exception dispatchException = null;

      try {
         processedRequest = checkMultipart(request);
         multipartRequestParsed = (processedRequest != request);

         // Determine handler for the current request.
         mappedHandler = getHandler(processedRequest);
         if (mappedHandler == null || mappedHandler.getHandler() == null) {
            noHandlerFound(processedRequest, response);
            return;
         }

```

首先创建 WebAsyncManager 用于异步处理。往下走调用 `checkMultipart` 判定是否是`Multipart`请求，比如是媒体文件。

```java
protected HttpServletRequest checkMultipart(HttpServletRequest request) throws MultipartException {
   if (this.multipartResolver != null && this.multipartResolver.isMultipart(request)) {
      if (WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class) != null) {
         logger.debug("Request is already a MultipartHttpServletRequest - if not in a forward, " +
               "this typically results from an additional MultipartFilter in web.xml");
      }
      else if (hasMultipartException(request) ) {
         logger.debug("Multipart resolution failed for current request before - " +
               "skipping re-resolution for undisturbed error rendering");
      }
      else {
         try {
            return this.multipartResolver.resolveMultipart(request);
         }
         catch (MultipartException ex) {
            if (request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE) != null) {
               logger.debug("Multipart resolution failed for error dispatch", ex);
               // Keep processing error dispatch with regular request handle below
            }
            else {
               throw ex;
            }
         }
      }
   }
   // If not returned before: return original request.
   return request;
}
```

具体调用`StandardServletMultipartResolver`的 `isMultipart`方法判定

```java
@Override
public boolean isMultipart(HttpServletRequest request) {
   // Same check as in Commons FileUpload...
   if (!"post".equalsIgnoreCase(request.getMethod())) {
      return false;
   }
   String contentType = request.getContentType();
   return StringUtils.startsWithIgnoreCase(contentType, "multipart/");
}
```

接下来，调用`getHandler`寻找当前请求的处理程序。

```java
protected HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
   for (HandlerMapping hm : this.handlerMappings) {
      if (logger.isTraceEnabled()) {
         logger.trace(
               "Testing handler map [" + hm + "] in DispatcherServlet with name '" + getServletName() + "'");
      }
      HandlerExecutionChain handler = hm.getHandler(request);
      if (handler != null) {
         return handler;
      }
   }
   return null;
}
```

遍历所有的 handlerMapping（默认是有7 个，其中用于平常处理请求的是 `RequestMappingHandlerMapping`） 来寻找该请求的映射处理器。调用每个handlerMapping 的`getHandler`方法（在AbstractHandlerMapping 中实现）。

```java
@Override
public final HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
   Object handler = getHandlerInternal(request);
   if (handler == null) {
      handler = getDefaultHandler();
   }
   if (handler == null) {
      return null;
   }
   // Bean name or resolved handler?
   if (handler instanceof String) {
      String handlerName = (String) handler;
      handler = getApplicationContext().getBean(handlerName);
   }

   HandlerExecutionChain executionChain = getHandlerExecutionChain(handler, request);
   if (CorsUtils.isCorsRequest(request)) {
      CorsConfiguration globalConfig = this.globalCorsConfigSource.getCorsConfiguration(request);
      CorsConfiguration handlerConfig = getCorsConfiguration(handler, request);
      CorsConfiguration config = (globalConfig != null ? globalConfig.combine(handlerConfig) : handlerConfig);
      executionChain = getCorsHandlerExecutionChain(request, executionChain, config);
   }
   return executionChain;
}
```

调用`getHandlerInternal`尝试获取处理对象，如果没有获取到则尝试获取默认的Handler(默认没有)。如果获取到的处理器时string 则获取它的实例。

```java
protected HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {
   String lookupPath = getUrlPathHelper().getLookupPathForRequest(request);
   if (logger.isDebugEnabled()) {
      logger.debug("Looking up handler method for path " + lookupPath);
   }
   this.mappingRegistry.acquireReadLock();
   try {
      HandlerMethod handlerMethod = lookupHandlerMethod(lookupPath, request);
      if (logger.isDebugEnabled()) {
         if (handlerMethod != null) {
            logger.debug("Returning handler method [" + handlerMethod + "]");
         }
         else {
            logger.debug("Did not find handler method for [" + lookupPath + "]");
         }
      }
      return (handlerMethod != null ? handlerMethod.createWithResolvedBean() : null);
   }
   finally {
      this.mappingRegistry.releaseReadLock();
   }
}
```

`getHandlerInternal`首先解析请求的路径。然后获取读锁，在调用 `lookupHandlerMethod` 根据路径寻找处理程序,找到后返回。

```java
protected HandlerMethod lookupHandlerMethod(String lookupPath, HttpServletRequest request) throws Exception {
   List<Match> matches = new ArrayList<Match>();
   List<T> directPathMatches = this.mappingRegistry.getMappingsByUrl(lookupPath);
   if (directPathMatches != null) {
      addMatchingMappings(directPathMatches, matches, request);
   }
   if (matches.isEmpty()) {
      // No choice but to go through all mappings...
      addMatchingMappings(this.mappingRegistry.getMappings().keySet(), matches, request);
   }

   if (!matches.isEmpty()) {
      Comparator<Match> comparator = new MatchComparator(getMappingComparator(request));
      Collections.sort(matches, comparator);
      if (logger.isTraceEnabled()) {
         logger.trace("Found " + matches.size() + " matching mapping(s) for [" +
               lookupPath + "] : " + matches);
      }
      Match bestMatch = matches.get(0);
      if (matches.size() > 1) {
         if (CorsUtils.isPreFlightRequest(request)) {
            return PREFLIGHT_AMBIGUOUS_MATCH;
         }
         Match secondBestMatch = matches.get(1);
         if (comparator.compare(bestMatch, secondBestMatch) == 0) {
            Method m1 = bestMatch.handlerMethod.getMethod();
            Method m2 = secondBestMatch.handlerMethod.getMethod();
            throw new IllegalStateException("Ambiguous handler methods mapped for HTTP path '" +
                  request.getRequestURL() + "': {" + m1 + ", " + m2 + "}");
         }
      }
      request.setAttribute(BEST_MATCHING_HANDLER_ATTRIBUTE, bestMatch.handlerMethod);
      handleMatch(bestMatch.mapping, lookupPath, request);
      return bestMatch.handlerMethod;
   }
   else {
      return handleNoMatch(this.mappingRegistry.getMappings().keySet(), lookupPath, request);
   }
}
```

通过 `mappingRegistry`(是一个Map)通过路径获取 RequestMappingInfo 的list，然后调用`addMatchingMappings` 将找到的处理方法与RequestMappingInfo 封装进 `Match`内部类，然后添加进 matches list 中。

如果没有找到则将所有之前扫描注册的映射对象，全部添加进入 matches 中。接下来会对扫描到的`RequestMappingInfo `进行排序。得到优先级最高的一个，如果有多个`RequestMappingInfo`的话，再从 matches 中抽出第二个与第一个比较，如果有两个相同的，则抛出异常。

然后将找到的处理程序与映射路径和相关的信息（handleMatch方法）注入 request 中返回。

```java
protected void handleMatch(RequestMappingInfo info, String lookupPath, HttpServletRequest request) {
   super.handleMatch(info, lookupPath, request);

   String bestPattern;
   Map<String, String> uriVariables;
   Map<String, String> decodedUriVariables;

   Set<String> patterns = info.getPatternsCondition().getPatterns();
   if (patterns.isEmpty()) {
      bestPattern = lookupPath;
      uriVariables = Collections.emptyMap();
      decodedUriVariables = Collections.emptyMap();
   }
   else {
      bestPattern = patterns.iterator().next();
      uriVariables = getPathMatcher().extractUriTemplateVariables(bestPattern, lookupPath);
      decodedUriVariables = getUrlPathHelper().decodePathVariables(request, uriVariables);
   }

   request.setAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE, bestPattern);
   request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, decodedUriVariables);

   if (isMatrixVariableContentAvailable()) {
      Map<String, MultiValueMap<String, String>> matrixVars = extractMatrixVariables(request, uriVariables);
      request.setAttribute(HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE, matrixVars);
   }

   if (!info.getProducesCondition().getProducibleMediaTypes().isEmpty()) {
      Set<MediaType> mediaTypes = info.getProducesCondition().getProducibleMediaTypes();
      request.setAttribute(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, mediaTypes);
   }
}
```

返回 getHandlerInteral 调用 `createWithResolvedBean`方法实例化 controller。最后解锁返回。

```java
public HandlerMethod createWithResolvedBean() {
   Object handler = this.bean;
   if (this.bean instanceof String) {
      String beanName = (String) this.bean;
      handler = this.beanFactory.getBean(beanName);
   }
   return new HandlerMethod(this, handler);
}
```

返回到`getHandler` 找到handler 后调用 `getHandlerExecutionChain`返回执行器链。

```java
protected HandlerExecutionChain getHandlerExecutionChain(Object handler, HttpServletRequest request) {
   HandlerExecutionChain chain = (handler instanceof HandlerExecutionChain ?
         (HandlerExecutionChain) handler : new HandlerExecutionChain(handler));

   String lookupPath = this.urlPathHelper.getLookupPathForRequest(request);
   for (HandlerInterceptor interceptor : this.adaptedInterceptors) {
      if (interceptor instanceof MappedInterceptor) {
         MappedInterceptor mappedInterceptor = (MappedInterceptor) interceptor;
         if (mappedInterceptor.matches(lookupPath, this.pathMatcher)) {
            chain.addInterceptor(mappedInterceptor.getInterceptor());
         }
      }
      else {
         chain.addInterceptor(interceptor);
      }
   }
   return chain;
}
```

由于参数 handler 是处理逻辑方法，那么就直接创建一个`HandlerExecutionChain`没有拦截器。然后向执行器链添加拦截器（2 个），最后返回执行器链到 `doDispatch`。’

```java
    // Determine handler adapter for the current request.
   HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());

   // Process last-modified header, if supported by the handler.
   String method = request.getMethod();
   boolean isGet = "GET".equals(method);
   if (isGet || "HEAD".equals(method)) {
      long lastModified = ha.getLastModified(request, mappedHandler.getHandler());
      if (logger.isDebugEnabled()) {
         logger.debug("Last-Modified value for [" + getRequestUri(request) + "] is: " + lastModified);
      }
      if (new ServletWebRequest(request, response).checkNotModified(lastModified) && isGet) {
         return;
      }
   }

   if (!mappedHandler.applyPreHandle(processedRequest, response)) {
      return;
   }

   // Actually invoke the handler.
   mv = ha.handle(processedRequest, response, mappedHandler.getHandler());

   if (asyncManager.isConcurrentHandlingStarted()) {
      return;
   }

   applyDefaultViewName(processedRequest, mv);
   mappedHandler.applyPostHandle(processedRequest, response, mv);
}
```

已经寻找到执行的handler 并封装成执行器链了。接下来的任务就是寻找可以具体调用handler 的handlerAdapter 了。

```java
protected HandlerAdapter getHandlerAdapter(Object handler) throws ServletException {
   for (HandlerAdapter ha : this.handlerAdapters) {
      if (logger.isTraceEnabled()) {
         logger.trace("Testing handler adapter [" + ha + "]");
      }
      if (ha.supports(handler)) {
         return ha;
      }
   }
   throw new ServletException("No adapter for handler [" + handler +
         "]: The DispatcherServlet configuration needs to include a HandlerAdapter that supports this handler");
}
```

遍历所有的适配器调用 supports 进行匹配。找到的应该是`RequestMappingHandlerAdapter`类。

```java
@Override
public final boolean supports(Object handler) {
   return (handler instanceof HandlerMethod && supportsInternal((HandlerMethod) handler));
}
```

然后判断 `checkNotModified` 如果请求不需要进一步处理则直接返回。正常情况则调用`applyPreHandle`。

```java
boolean applyPreHandle(HttpServletRequest request, HttpServletResponse response) throws Exception {
   HandlerInterceptor[] interceptors = getInterceptors();
   if (!ObjectUtils.isEmpty(interceptors)) {
      for (int i = 0; i < interceptors.length; i++) {
         HandlerInterceptor interceptor = interceptors[i];
         if (!interceptor.preHandle(request, response, this.handler)) {
            triggerAfterCompletion(request, response, null);
            return false;
         }
         this.interceptorIndex = i;
      }
   }
   return true;
}
```

在执行请求之前调用拦截器。

然后`handle`方法真正的调用处理逻辑。

```java
@Override
public final ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {

   return handleInternal(request, response, (HandlerMethod) handler);
}
```

```java
@Override
protected ModelAndView handleInternal(HttpServletRequest request,
      HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {

   ModelAndView mav;
   checkRequest(request);

   // Execute invokeHandlerMethod in synchronized block if required.
   if (this.synchronizeOnSession) {
      HttpSession session = request.getSession(false);
      if (session != null) {
         Object mutex = WebUtils.getSessionMutex(session);
         synchronized (mutex) {
            mav = invokeHandlerMethod(request, response, handlerMethod);
         }
      }
      else {
         // No HttpSession available -> no mutex necessary
         mav = invokeHandlerMethod(request, response, handlerMethod);
      }
   }
   else {
      // No synchronization on session demanded at all...
      mav = invokeHandlerMethod(request, response, handlerMethod);
   }

   if (!response.containsHeader(HEADER_CACHE_CONTROL)) {
      if (getSessionAttributesHandler(handlerMethod).hasSessionAttributes()) {
         applyCacheSeconds(response, this.cacheSecondsForSessionAttributeHandlers);
      }
      else {
         prepareResponse(response);
      }
   }

   return mav;
}
```

如果需要可以在 Synchronized 中执行。正常调用`invokeHandlerMethod`

```java
protected ModelAndView invokeHandlerMethod(HttpServletRequest request,
      HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {

   ServletWebRequest webRequest = new ServletWebRequest(request, response);
   try {
      WebDataBinderFactory binderFactory = getDataBinderFactory(handlerMethod);
      ModelFactory modelFactory = getModelFactory(handlerMethod, binderFactory);

      ServletInvocableHandlerMethod invocableMethod = createInvocableHandlerMethod(handlerMethod);
      invocableMethod.setHandlerMethodArgumentResolvers(this.argumentResolvers);
      invocableMethod.setHandlerMethodReturnValueHandlers(this.returnValueHandlers);
      invocableMethod.setDataBinderFactory(binderFactory);
      invocableMethod.setParameterNameDiscoverer(this.parameterNameDiscoverer);

      ModelAndViewContainer mavContainer = new ModelAndViewContainer();
      mavContainer.addAllAttributes(RequestContextUtils.getInputFlashMap(request));
      modelFactory.initModel(webRequest, mavContainer, invocableMethod);
      mavContainer.setIgnoreDefaultModelOnRedirect(this.ignoreDefaultModelOnRedirect);

      AsyncWebRequest asyncWebRequest = WebAsyncUtils.createAsyncWebRequest(request, response);
      asyncWebRequest.setTimeout(this.asyncRequestTimeout);

      WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
      asyncManager.setTaskExecutor(this.taskExecutor);
      asyncManager.setAsyncWebRequest(asyncWebRequest);
      asyncManager.registerCallableInterceptors(this.callableInterceptors);
      asyncManager.registerDeferredResultInterceptors(this.deferredResultInterceptors);

      if (asyncManager.hasConcurrentResult()) {
         Object result = asyncManager.getConcurrentResult();
         mavContainer = (ModelAndViewContainer) asyncManager.getConcurrentResultContext()[0];
         asyncManager.clearConcurrentResult();
         if (logger.isDebugEnabled()) {
            logger.debug("Found concurrent result value [" + result + "]");
         }
         invocableMethod = invocableMethod.wrapConcurrentResult(result);
      }

      invocableMethod.invokeAndHandle(webRequest, mavContainer);
      if (asyncManager.isConcurrentHandlingStarted()) {
         return null;
      }

      return getModelAndView(mavContainer, modelFactory, webRequest);
   }
   finally {
      webRequest.requestCompleted();
   }
}
```

首先为执行handler 做准备工作。`WebDataBinderFactory`用来创建` WebDataBinder` 对象，该对象用来将request 请求的信息，转换成java bean。`ModelFactory`用于在调用 handler 之前创建Mdel 共hadler 使用，并在返回后处理model。`ServletInvocableHandlerMethod` 是要执行的handler方法。`ModelAndViewContainer` 用于实例化时自动创建模型。

最后调用`invokeAndHandle`

```java
public void invokeAndHandle(ServletWebRequest webRequest, ModelAndViewContainer mavContainer,
      Object... providedArgs) throws Exception {

   Object returnValue = invokeForRequest(webRequest, mavContainer, providedArgs);
   setResponseStatus(webRequest);

   if (returnValue == null) {
      if (isRequestNotModified(webRequest) || getResponseStatus() != null || mavContainer.isRequestHandled()) {
         mavContainer.setRequestHandled(true);
         return;
      }
   }
   else if (StringUtils.hasText(getResponseStatusReason())) {
      mavContainer.setRequestHandled(true);
      return;
   }

   mavContainer.setRequestHandled(false);
   try {
      this.returnValueHandlers.handleReturnValue(
            returnValue, getReturnValueType(returnValue), mavContainer, webRequest);
   }
   catch (Exception ex) {
      if (logger.isTraceEnabled()) {
         logger.trace(getReturnValueHandlingErrorMessage("Error handling return value", returnValue), ex);
      }
      throw ex;
   }
}
```

`invokeForRequest` 用于构建 handler 的所需要的参数。通过反射调用handler。得到返回值后调用`handleReturnValue`处理返回值。遍历所有的返回值处理器，针对不同的返回值有不同的处理器，比如返回ModelAndView、字符串。将这些信息写入Response 中。

处理完后返回 handleInternal，执行`webRequest.requestCompleted();`处理清理现场。并返回ModeAndView，返回字符串的为空。然后按情况将结果缓存。

返回到 doDispatch ，调用`applyPostHandle`对handler 进行后处理。调用`processDispatchResult`处理结果将异常解析为ModeAndView，或者对视图进行渲染。

```java
private void processDispatchResult(HttpServletRequest request, HttpServletResponse response,
      HandlerExecutionChain mappedHandler, ModelAndView mv, Exception exception) throws Exception {

   boolean errorView = false;

   if (exception != null) {
      if (exception instanceof ModelAndViewDefiningException) {
         logger.debug("ModelAndViewDefiningException encountered", exception);
         mv = ((ModelAndViewDefiningException) exception).getModelAndView();
      }
      else {
         Object handler = (mappedHandler != null ? mappedHandler.getHandler() : null);
         mv = processHandlerException(request, response, handler, exception);
         errorView = (mv != null);
      }
   }

   // Did the handler return a view to render?
   if (mv != null && !mv.wasCleared()) {
      render(mv, request, response);
      if (errorView) {
         WebUtils.clearErrorRequestAttributes(request);
      }
   }
   else {
      if (logger.isDebugEnabled()) {
         logger.debug("Null ModelAndView returned to DispatcherServlet with name '" + getServletName() +
               "': assuming HandlerAdapter completed request handling");
      }
   }

   if (WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted()) {
      // Concurrent handling started during a forward
      return;
   }

   if (mappedHandler != null) {
      mappedHandler.triggerAfterCompletion(request, response, null);
   }
}
```

​	`render` 对视图进行渲染。`	处理异常。

最后由tomcat 返回。







