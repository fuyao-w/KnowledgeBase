SpringMVC 使用 WebApplicationContext。

```java
public interface WebApplicationContext extends ApplicationContext
```

用于为Web应用程序提供配置的界面。 这在应用程序运行时是只读的，但如果实现支持，则可以重新加载。
此接口将getServletContext（）方法添加到通用ApplicationContext接口，并定义在引导过程中必须绑定根上下文的已知应用程序属性名称。

与通用应用程序上下文一样，Web应用程序上下文是分层的。 每个应用程序都有一个根上下文，而应用程序中的每个servlet（包括MVC框架中的调度程序servlet）都有自己的子上下文。

除了标准的应用程序上下文生命周期功能之外，WebApplicationContext实现还需要检测ServletContextAware bean并相应地调用setServletContext方法。

该类只定义了一个方法

```java
ServletContext getServletContext();
```

### ConfigurableWebApplicationContext

```java
public interface ConfigurableWebApplicationContext extends WebApplicationContext, ConfigurableApplicationContext {
```

由可配置的Web应用程序上下文实现的接口。 ContextLoader和FrameworkServlet支持。
注意：在调用从ConfigurableApplicationContext继承的ConfigurableApplicationContext.refresh（）方法之前，需要调用此接口的setter。 它们不会导致自己初始化上下文。

该接口提供了 servletContext  和 ServletConfig  的set 方法。

### AbstractRefreshableWebApplicationContext

```java
public abstract class AbstractRefreshableWebApplicationContext extends AbstractRefreshableConfigApplicationContext
      implements ConfigurableWebApplicationContext, ThemeSource {
```

AbstractRefreshableApplicationContext子类，用于为Web环境实现ConfigurableWebApplicationContext接口。提供“configLocations”属性，通过Web应用程序启动时的ConfigurableWebApplicationContext接口填充。
此类与AbstractRefreshableApplicationContext一样容易子类化：您需要实现的是AbstractRefreshableApplicationContext.loadBeanDefinitions（org.springframework.beans.factory.support.DefaultListableBeanFactory）方法;有关详细信息，请参阅超类javadoc。请注意，实现应该从getConfigLocations（）方法返回的位置指定的文件中加载bean定义。

将资源路径解释为servlet上下文资源，即作为Web应用程序根目录下的路径。绝对路径，例如对于Web应用程序根目录之外的文件，可以通过“file：”URL访问，由DefaultResourceLoader实现。

除了AbstractApplicationContext检测到的特殊bean之外，此类还在上下文中的特殊bean名称“themeSource”下检测ThemeSource类型的bean。

这是要为不同的bean定义格式进行子类化的Web上下文。这样的上下文实现可以指定为ContextLoader的“contextClass”context-param或FrameworkServlet的“contextClass”init-param，替换默认的XmlWebApplicationContext。然后它将分别自动接收“contextConfigLocation”context-param或init-param。

请注意，WebApplicationContext实现通常应根据通过ConfigurableWebApplicationContext接口接收的配置进行自我配置。相反，独立应用程序上下文可能允许在自定义启动代码中进行配置（例如，GenericApplicationContext）。

```java
@Override
protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
   beanFactory.addBeanPostProcessor(new ServletContextAwareProcessor(this.servletContext, this.servletConfig));
   beanFactory.ignoreDependencyInterface(ServletContextAware.class);
   beanFactory.ignoreDependencyInterface(ServletConfigAware.class);

   WebApplicationContextUtils.registerWebApplicationScopes(beanFactory, this.servletContext);
   WebApplicationContextUtils.registerEnvironmentBeans(beanFactory, this.servletContext, this.servletConfig);
}
```

该类实现了 postProcessBeanFactory 并添加了一个 ServletContextAwareProcessor 后处理器。

```java
@Override
public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
   if (getServletContext() != null && bean instanceof ServletContextAware) {
      ((ServletContextAware) bean).setServletContext(getServletContext());
   }
   if (getServletConfig() != null && bean instanceof ServletConfigAware) {
      ((ServletConfigAware) bean).setServletConfig(getServletConfig());
   }
   return bean;
}
```

该后处理器用于自动注入 ServletContext 和 ServletConfig 

### XmlWebApplicationContext

```java
XmlWebApplicationContext
```

xml web 应用上下文是，三个web 上下文类之一，该类通过xml 文件进行配置。

```java
/** Default config location for the root context */
public static final String DEFAULT_CONFIG_LOCATION = "/WEB-INF/applicationContext.xml";

/** Default prefix for building a config location for a namespace */
public static final String DEFAULT_CONFIG_LOCATION_PREFIX = "/WEB-INF/";

/** Default suffix for building a config location for a namespace */
public static final String DEFAULT_CONFIG_LOCATION_SUFFIX = ".xml";
```

从提供的常量可以看出，我们可以直接在 WEB-INF 目录下 定义 applicationContext.xml 文件，以便于上下文自动加载。

```java
@Override
protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException, IOException {
   // Create a new XmlBeanDefinitionReader for the given BeanFactory.
   XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);

   // Configure the bean definition reader with this context's
   // resource loading environment.
   beanDefinitionReader.setEnvironment(getEnvironment());
   beanDefinitionReader.setResourceLoader(this);
   beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));

   // Allow a subclass to provide custom initialization of the reader,
   // then proceed with actually loading the bean definitions.
   initBeanDefinitionReader(beanDefinitionReader);
   loadBeanDefinitions(beanDefinitionReader);
}
```

该类试下了 loadBeanDefinitions 方法（与 AbstractXmlApplicationContext 中的方法相同），以定义 bean 的加载注册方式。

该类定义了 beanDefinitionReader 用于加载、读取、注册配置文件，initBeanDefinitionReader 是一个钩子。loadBeanDefinitions 通过调用 beanDefinitinReader 处理所有的配置文件。在AbstractBeanDefinitionsReader 中实现。

```java
public int loadBeanDefinitions(String location, Set<Resource> actualResources) throws BeanDefinitionStoreException {
   ResourceLoader resourceLoader = getResourceLoader();
   if (resourceLoader == null) {
      throw new BeanDefinitionStoreException(
            "Cannot import bean definitions from location [" + location + "]: no ResourceLoader available");
   }

   if (resourceLoader instanceof ResourcePatternResolver) {
      // Resource pattern matching available.
      try {
         Resource[] resources = ((ResourcePatternResolver) resourceLoader).getResources(location);
         int loadCount = loadBeanDefinitions(resources);
         if (actualResources != null) {
            for (Resource resource : resources) {
               actualResources.add(resource);
            }
         }
         if (logger.isDebugEnabled()) {
            logger.debug("Loaded " + loadCount + " bean definitions from location pattern [" + location + "]");
         }
         return loadCount;
      }
      catch (IOException ex) {
         throw new BeanDefinitionStoreException(
               "Could not resolve bean definition resource pattern [" + location + "]", ex);
      }
   }
   else {
      // Can only load single resources by absolute URL.
      Resource resource = resourceLoader.getResource(location);
      int loadCount = loadBeanDefinitions(resource);
      if (actualResources != null) {
         actualResources.add(resource);
      }
      if (logger.isDebugEnabled()) {
         logger.debug("Loaded " + loadCount + " bean definitions from location [" + location + "]");
      }
      return loadCount;
   }
}
```

该方法主要执行获取 resource 并调用 xmlBeanDefinitionReader 的 loadBeanDefinitions 方法。

```java
public int loadBeanDefinitions(EncodedResource encodedResource) throws BeanDefinitionStoreException {
   Assert.notNull(encodedResource, "EncodedResource must not be null");
   if (logger.isInfoEnabled()) {
      logger.info("Loading XML bean definitions from " + encodedResource);
   }

   Set<EncodedResource> currentResources = this.resourcesCurrentlyBeingLoaded.get();
   if (currentResources == null) {
      currentResources = new HashSet<EncodedResource>(4);
      this.resourcesCurrentlyBeingLoaded.set(currentResources);
   }
   if (!currentResources.add(encodedResource)) {
      throw new BeanDefinitionStoreException(
            "Detected cyclic loading of " + encodedResource + " - check your import definitions!");
   }
   try {
      InputStream inputStream = encodedResource.getResource().getInputStream();
      try {
         InputSource inputSource = new InputSource(inputStream);
         if (encodedResource.getEncoding() != null) {
            inputSource.setEncoding(encodedResource.getEncoding());
         }
         return doLoadBeanDefinitions(inputSource, encodedResource.getResource());
      }
      finally {
         inputStream.close();
      }
   }
   catch (IOException ex) {
      throw new BeanDefinitionStoreException(
            "IOException parsing XML document from " + encodedResource.getResource(), ex);
   }
   finally {
      currentResources.remove(encodedResource);
      if (currentResources.isEmpty()) {
         this.resourcesCurrentlyBeingLoaded.remove();
      }
   }
}
```

该方法获取资源的输入流，并调用 doLoadBeanDefinitions 将通过输入流将配置文件解析成 Document，然后调用 registerBeanDefinitions 进行Doucument 的解析工作，并且注册解析的BeanDefinition 。

```java
public int registerBeanDefinitions(Document doc, Resource resource) throws BeanDefinitionStoreException {
   BeanDefinitionDocumentReader documentReader = createBeanDefinitionDocumentReader();
   int countBefore = getRegistry().getBeanDefinitionCount();
   documentReader.registerBeanDefinitions(doc, createReaderContext(resource));
   return getRegistry().getBeanDefinitionCount() - countBefore;
}
```

该方法返回本次总共注册的bean 定义。

```java
@Override
public void registerBeanDefinitions(Document doc, XmlReaderContext readerContext) {
   this.readerContext = readerContext;
   logger.debug("Loading bean definitions");
   Element root = doc.getDocumentElement();
   doRegisterBeanDefinitions(root);
}
```

```java
protected void doRegisterBeanDefinitions(Element root) {
   // Any nested <beans> elements will cause recursion in this method. In
   // order to propagate and preserve <beans> default-* attributes correctly,
   // keep track of the current (parent) delegate, which may be null. Create
   // the new (child) delegate with a reference to the parent for fallback purposes,
   // then ultimately reset this.delegate back to its original (parent) reference.
   // this behavior emulates a stack of delegates without actually necessitating one.
   BeanDefinitionParserDelegate parent = this.delegate;
   this.delegate = createDelegate(getReaderContext(), root, parent);

   if (this.delegate.isDefaultNamespace(root)) {
      String profileSpec = root.getAttribute(PROFILE_ATTRIBUTE);
      if (StringUtils.hasText(profileSpec)) {
         String[] specifiedProfiles = StringUtils.tokenizeToStringArray(
               profileSpec, BeanDefinitionParserDelegate.MULTI_VALUE_ATTRIBUTE_DELIMITERS);
         if (!getReaderContext().getEnvironment().acceptsProfiles(specifiedProfiles)) {
            if (logger.isInfoEnabled()) {
               logger.info("Skipped XML bean definition file due to specified profiles [" + profileSpec +
                     "] not matching: " + getReaderContext().getResource());
            }
            return;
         }
      }
   }

   preProcessXml(root);
   parseBeanDefinitions(root, this.delegate);
   postProcessXml(root);

   this.delegate = parent;
}
```

首先获取 profile 元素，如果当前配置文件不属于活动的 profile 则，直接返回不解析次配置文件。

调用 parseBeanDefinitions 解析跟级别的document元素。 pre 和 post 是钩子。

```java
protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {
   if (delegate.isDefaultNamespace(root)) {
      NodeList nl = root.getChildNodes();
      for (int i = 0; i < nl.getLength(); i++) {
         Node node = nl.item(i);
         if (node instanceof Element) {
            Element ele = (Element) node;
            if (delegate.isDefaultNamespace(ele)) {
               parseDefaultElement(ele, delegate);
            }
            else {
               delegate.parseCustomElement(ele);
            }
         }
      }
   }
   else {
      delegate.parseCustomElement(root);
   }
}
```

