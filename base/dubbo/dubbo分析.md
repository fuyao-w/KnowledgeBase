dubbo 与spring 整合提供全spring 的配置，下面分析。这一过程。

通过spring 的obtainBeanFactory 方法的解析，了解到在 doLoadBeanDefinition 的时候，在遇到非基础明明空间下的 元素的时候，需要先找到相应的明明空间，这一过程大概是 spring 命名解析器通过伪 SPI 获取到所有的命名空间处理器，并将其缓存在 map中，根据命名空间名称，获取到 命名空间Handler，然后调用其 init 方法，这个方法会注册与该命名空间相关的所有的 BeanDefinitionPhaser。然后遍历所有phaser 通过sport 方法，获取该元素的解析器，对该元素进行处理。

知道了这些，我们可以直接在 dubbo 的 com.alibaba.dubbo.config.spring.schema  包下面 找到这个 dubboNameSpaceHandler。

```java
public class DubboNamespaceHandler extends NamespaceHandlerSupport {

    static {
        Version.checkDuplicate(DubboNamespaceHandler.class);
    }

    @Override
    public void init() {
        registerBeanDefinitionParser("application", new DubboBeanDefinitionParser(ApplicationConfig.class, true));
        registerBeanDefinitionParser("module", new DubboBeanDefinitionParser(ModuleConfig.class, true));
        registerBeanDefinitionParser("registry", new DubboBeanDefinitionParser(RegistryConfig.class, true));
        registerBeanDefinitionParser("monitor", new DubboBeanDefinitionParser(MonitorConfig.class, true));
        registerBeanDefinitionParser("provider", new DubboBeanDefinitionParser(ProviderConfig.class, true));
        registerBeanDefinitionParser("consumer", new DubboBeanDefinitionParser(ConsumerConfig.class, true));
        registerBeanDefinitionParser("protocol", new DubboBeanDefinitionParser(ProtocolConfig.class, true));
        registerBeanDefinitionParser("service", new DubboBeanDefinitionParser(ServiceBean.class, true));
        registerBeanDefinitionParser("reference", new DubboBeanDefinitionParser(ReferenceBean.class, false));
        registerBeanDefinitionParser("annotation", new AnnotationBeanDefinitionParser());
    }

}
```

可以看到，所有的元素解析器，在这个类下进行注册。

在配置 xml的时候，首先都是把 application 元素放在上方。那么就把它当做切入点分析。元素解析器都要在 phaser 方法里面进行处理逻辑。但是dubbo 把除了处理注解的所有逻辑都放到一起了。这么做的好处就是，这些元素谁在前面都会执行 dubbo 的初始化逻辑。。。

parse 方法将近 200 行，分段分析。

```java
RootBeanDefinition beanDefinition = new RootBeanDefinition();
beanDefinition.setBeanClass(beanClass);
beanDefinition.setLazyInit(false);
String id = element.getAttribute("id");
if ((id == null || id.length() == 0) && required) {
    String generatedBeanName = element.getAttribute("name");
    if (generatedBeanName == null || generatedBeanName.length() == 0) {
        if (ProtocolConfig.class.equals(beanClass)) {
            generatedBeanName = "dubbo";
        } else {
            generatedBeanName = element.getAttribute("interface");
        }
    }
    if (generatedBeanName == null || generatedBeanName.length() == 0) {
        generatedBeanName = beanClass.getName();
    }
    id = generatedBeanName;
    int counter = 2;
    while (parserContext.getRegistry().containsBeanDefinition(id)) {
        id = generatedBeanName + (counter++);
    }
}
```

开头这一部分创建了 RootBeanDefinition。然后获取元素的 name 属性，为beanDefinition 的名称赋值。如果已经注册了该名称的beanDefinition，则通过 counter 解决重名问题。

```java
if (id != null && id.length() > 0) {
    if (parserContext.getRegistry().containsBeanDefinition(id)) {
        throw new IllegalStateException("Duplicate spring bean id " + id);
    }
    parserContext.getRegistry().registerBeanDefinition(id, beanDefinition);
    beanDefinition.getPropertyValues().addPropertyValue("id", id);
}
```

下面是注册 beanDefinition，如果还是有重名的就抛出异常。

```java
if (ProtocolConfig.class.equals(beanClass)) {
    for (String name : parserContext.getRegistry().getBeanDefinitionNames()) {
        BeanDefinition definition = parserContext.getRegistry().getBeanDefinition(name);
        PropertyValue property = definition.getPropertyValues().getPropertyValue("protocol");
        if (property != null) {
            Object value = property.getValue();
            if (value instanceof ProtocolConfig && id.equals(((ProtocolConfig) value).getName())) {
                definition.getPropertyValues().addPropertyValue("protocol", new RuntimeBeanReference(id));
            }
        }
    }
}
```

接下来处理 protocol 元素，遍历所有的的 beanDefinition 中有 protocol 属性的 value, 如果value 也是ProtocolConfig 类型的，则向该 beanDefinition 中，继续添加 key 为 protocol 的 PrototyValue。value为 id ，现在代表协议名称。默认的在第一段设置为 dubbo。

```java
else if (ServiceBean.class.equals(beanClass)) {
    String className = element.getAttribute("class");
    if (className != null && className.length() > 0) {
        RootBeanDefinition classDefinition = new RootBeanDefinition();
        classDefinition.setBeanClass(ReflectUtils.forName(className));
        classDefinition.setLazyInit(false);
        parseProperties(element.getChildNodes(), classDefinition);
        beanDefinition.getPropertyValues().addPropertyValue("ref", new BeanDefinitionHolder(classDefinition, id + "Impl"));
    }
}
```

这部分用于处理 service 元素。代表服务提供者暴露服务配置。获取了 class 属性，然后通过反射实例化获取class 对象。添加了一个BeanDefinition 属性 ref ，value 为bean 名称为 id+ impl。

```java
} else if (ProviderConfig.class.equals(beanClass)) {
    parseNested(element, parserContext, ServiceBean.class, true, "service", "provider", id, beanDefinition);
} else if (ConsumerConfig.class.equals(beanClass)) {
    parseNested(element, parserContext, ReferenceBean.class, false, "reference", "consumer", id, beanDefinition);
}
```

处理生产者与消费之。调用 `parseNested`。

```java
Set<String> props = new HashSet<String>();
ManagedMap parameters = null;
for (Method setter : beanClass.getMethods()) {
    String name = setter.getName();
    if (name.length() > 3 && name.startsWith("set")
            && Modifier.isPublic(setter.getModifiers())
            && setter.getParameterTypes().length == 1) {
        Class<?> type = setter.getParameterTypes()[0];
        String property = StringUtils.camelToSplitName(name.substring(3, 4).toLowerCase() + name.substring(4), "-");
        props.add(property);
        
```

接下来应该准备进行对传递进来的配置类，进行setter 注入。获取了所有setter方法，并将其属性名称放进props set 中。

```java
Method getter = null;
try {
    getter = beanClass.getMethod("get" + name.substring(3), new Class<?>[0]);
} catch (NoSuchMethodException e) {
    try {
        getter = beanClass.getMethod("is" + name.substring(3), new Class<?>[0]);
    } catch (NoSuchMethodException e2) {
    }
}
if (getter == null
        || !Modifier.isPublic(getter.getModifiers())
        || !type.equals(getter.getReturnType())) {
    continue;
}
```

接下来获取 getter 或者 isxxx 方法。如果没找到则 continue。

```java
if ("parameters".equals(property)) {
    parameters = parseParameters(element.getChildNodes(), beanDefinition);
} else if ("methods".equals(property)) {
    parseMethods(id, element.getChildNodes(), beanDefinition, parserContext);
} else if ("arguments".equals(property)) {
    parseArguments(id, element.getChildNodes(), beanDefinition, parserContext);
}
```

如果注入的是以上三个属性的话，则在对其子节点进行具体解析。

```java
 else {
    String value = element.getAttribute(property);
    if (value != null) {
        value = value.trim();
        if (value.length() > 0) {
            if ("registry".equals(property) && RegistryConfig.NO_AVAILABLE.equalsIgnoreCase(value)) {
                RegistryConfig registryConfig = new RegistryConfig();
                registryConfig.setAddress(RegistryConfig.NO_AVAILABLE);
                beanDefinition.getPropertyValues().addPropertyValue(property, registryConfig);
            } else if ("registry".equals(property) && value.indexOf(',') != -1) {
                parseMultiRef("registries", value, beanDefinition, parserContext);
            } else if ("provider".equals(property) && value.indexOf(',') != -1) {
                parseMultiRef("providers", value, beanDefinition, parserContext);
            } else if ("protocol".equals(property) && value.indexOf(',') != -1) {
                parseMultiRef("protocols", value, beanDefinition, parserContext);
            } 
```

如果不是以上三类的话，则调用paeseMultiRef，在往后也是对所有标签的各种设置，就先略过。

```java
NamedNodeMap attributes = element.getAttributes();
int len = attributes.getLength();
for (int i = 0; i < len; i++) {
    Node node = attributes.item(i);
    String name = node.getLocalName();
    if (!props.contains(name)) {
        if (parameters == null) {
            parameters = new ManagedMap();
        }
        String value = node.getNodeValue();
        parameters.put(name, new TypedStringValue(value, String.class));
    }
}
if (parameters != null) {
    beanDefinition.getPropertyValues().addPropertyValue("parameters", parameters);
}
return beanDefinition;
}	
```

最后剩下的配置标签添加进 beanDefinition 中，返回。

### 服务导出

服务导出的入口方法是 ServiceBean 的 onApplicationEvent。onApplicationEvent 是一个事件响应方法，该方法会在收到 Spring 上下文刷新事件后执行服务导出操作。首先判断是否应该导出。

前置工作主要包含两个部分，分别是配置检查，以及 URL 装配。在导出服务之前，Dubbo 需要检查用户的配置是否合理，或者为用户补充缺省配置。配置检查完成后，接下来需要根据这些配置组装 URL。在 Dubbo 中，URL 的作用十分重要。Dubbo 使用 URL 作为配置载体，所有的拓展点都是通过 URL 获取配置。这一点，官方文档中有所说明。

#### 2.1.2 多协议多注册中心导出服务

Dubbo 允许我们使用不同的协议导出服务，也允许我们向多个注册中心注册服务。Dubbo 在 doExportUrls 方法中对多协议，多注册中心进行了支持。

#### 2.1.3 组装 URL

配置检查完毕后，紧接着要做的事情是根据配置，以及其他一些信息组装 URL。前面说过，URL 是 Dubbo 配置的载体，通过 URL 可让 Dubbo 的各种配置在各个模块之间传递。URL 之于 Dubbo，犹如水之于鱼，非常重要。大家在阅读 Dubbo 服务导出相关源码的过程中，要注意 URL 内容的变化。既然 URL 如此重要，那么下面我们来了解一下 URL 组装的过程。

上面的代码首先是将一些信息，比如版本、时间戳、方法名以及各种配置对象的字段信息放入到 map 中，map 中的内容将作为 URL 的查询字符串。构建好 map 后，紧接着是获取上下文路径、主机名以及端口号等信息。最后将 map 和主机名等数据传给 URL 构造方法创建 URL 对象。需要注意的是，这里出现的 URL 并非 java.net.URL，而是 com.alibaba.dubbo.common.URL。

### 导出 Dubbo 服务

前置工作做完，接下来就可以进行服务导出了。服务导出分为导出到本地 (JVM)，和导出到远程。在深入分析服务导出的源码前，我们先来从宏观层面上看一下服务导出逻辑。如下：

不管是导出到本地，还是远程。进行服务导出之前，均需要先创建 Invoker，这是一个很重要的步骤。因此下面先来分析 Invoker 的创建过程。

先创建注册中心实例，之后再通过注册中心实例注册服务。本节先到这，接下来分析数据订阅过程。

### 2.2.1 Invoker 创建过程

在 Dubbo 中，Invoker 是一个非常重要的模型。在服务提供端，以及服务引用端均会出现 Invoker。Dubbo 官方文档中对 Invoker 进行了说明，这里引用一下。

> Invoker 是实体域，它是 Dubbo 的核心模型，其它模型都向它靠扰，或转换成它，它代表一个可执行体，可向它发起 invoke 调用，它有可能是一个本地的实现，也可能是一个远程的实现，也可能一个集群实现。

既然 Invoker 如此重要，那么我们很有必要搞清楚 Invoker 的用途。Invoker 是由 ProxyFactory 创建而来，Dubbo 默认的 ProxyFactory 实现类是 JavassistProxyFactory。下面我们到 JavassistProxyFactory 代码中，探索 Invoker 的创建过程。如下：

### 2.2.3 导出服务到远程

与导出服务到本地相比，导出服务到远程的过程要复杂不少，其包含了服务导出与服务注册两个过程。这两个过程涉及到了大量的调用，比较复杂。按照代码执行顺序，本节先来分析服务导出逻辑，服务注册逻辑将在下一节进行分析。下面开始分析，我们把目光移动到 RegistryProtocol 的 export 方法上。

上面代码看起来比较复杂，主要做如下一些操作：

1. 调用 doLocalExport 导出服务
2. 向注册中心注册服务
3. 向注册中心进行订阅 override 数据
4. 创建并返回 DestroyableExporter

### 2.2.4 服务注册

本节我们来分析服务注册过程，服务注册操作对于 Dubbo 来说不是必需的，通过服务直连的方式就可以绕过注册中心。但通常我们不会这么做，直连方式不利于服务治理，仅推荐在测试服务时使用。对于 Dubbo 来说，注册中心虽不是必需，但却是必要的。因此，关于注册中心以及服务注册相关逻辑，我们也需要搞懂。

本节内容以 Zookeeper 注册中心作为分析目标，其他类型注册中心大家可自行分析。下面从服务注册的入口方法开始分析，我们把目光再次移到 RegistryProtocol 的 export 方法上。如下：

#### 2.2.4.1 创建注册中心

本节内容以 Zookeeper 注册中心为例进行分析。下面先来看一下 getRegistry 方法的源码，这个方法由 AbstractRegistryFactory 实现。如下：

本篇文章详细分析了 Dubbo 服务导出过程，包括配置检测，URL 组装，Invoker 创建过程、导出服务以及注册服务等等。







## 服务引入

服务引用的入口方法为 ReferenceBean 的 getObject 方法，该方法定义在 Spring 的 FactoryBean 接口中，ReferenceBean 实现了这个方法。实现代码如下：



首先是方法开始到分割线1之间的代码。这段代码主要用于检测 ConsumerConfig 实例是否存在，如不存在则创建一个新的实例，然后通过系统变量或 dubbo.properties 配置文件填充 ConsumerConfig 的字段。接着是检测泛化配置，并根据配置设置 interfaceClass 的值。接着来看分割线1到分割线2之间的逻辑。这段逻辑用于从系统属性或配置文件中加载与接口名相对应的配置，并将解析结果赋值给 url 字段。url 字段的作用一般是用于点对点调用。继续向下看，分割线2和分割线3之间的代码用于检测几个核心配置类是否为空，为空则尝试从其他配置类中获取。分割线3与分割线4之间的代码主要用于收集各种配置，并将配置存储到 map 中。分割线4和分割线5之间的代码用于处理 MethodConfig 实例。该实例包含了事件通知配置，比如 onreturn、onthrow、oninvoke 等。分割线5到方法结尾的代码主要用于解析服务消费者 ip，以及调用 createProxy 创建代理对象。关于该方法的详细分析，将会在接下来的章节中展开。

### 3.2 引用服务

本节我们要从 createProxy 开始看起。从字面意思上来看，createProxy 似乎只是用于创建代理对象的。但实际上并非如此，该方法还会调用其他方法构建以及合并 Invoker 实例。具体细节如下。

上面代码很多，不过逻辑比较清晰。首先根据配置检查是否为本地调用，若是，则调用 InjvmProtocol 的 refer 方法生成 InjvmInvoker 实例。若不是，则读取直连配置项，或注册中心 url，并将读取到的 url 存储到 urls 中。然后根据 urls 元素数量进行后续操作。若 urls 元素数量为1，则直接通过 Protocol 自适应拓展类构建 Invoker 实例接口。若 urls 元素数量大于1，即存在多个注册中心或服务直连 url，此时先根据 url 构建 Invoker。然后再通过 Cluster 合并多个 Invoker，最后调用 ProxyFactory 生成代理类。Invoker 的构建过程以及代理类的过程比较重要，因此接下来将分两小节对这两个过程进行分析。

#### 3.2.1 创建 Invoker

Invoker 是 Dubbo 的核心模型，代表一个可执行体。在服务提供方，Invoker 用于调用服务提供类。在服务消费方，Invoker 用于执行远程调用。Invoker 是由 Protocol 实现类构建而来。Protocol 实现类有很多，本节会分析最常用的两个，分别是 RegistryProtocol 和 DubboProtocol，其他的大家自行分析。下面先来分析 DubboProtocol 的 refer 方法源码。如下：

```java
public <T> Invoker<T> refer(Class<T> serviceType, URL url) throws RpcException {
    optimizeSerialization(url);
    // 创建 DubboInvoker
    DubboInvoker<T> invoker = new DubboInvoker<T>(serviceType, url, getClients(url), invokers);
    invokers.add(invoker);
    return invoker;
}
```

上面方法看起来比较简单，不过这里有一个调用需要我们注意一下，即 getClients。这个方法用于获取客户端实例，实例类型为 ExchangeClient。ExchangeClient 实际上并不具备通信能力，它需要基于更底层的客户端实例进行通信。比如 NettyClient、MinaClient 等，默认情况下，Dubbo 使用 NettyClient 进行通信。接下来，我们简单看一下 getClients 方法的逻辑。

#### 3.2.2 创建代理

Invoker 创建完毕后，接下来要做的事情是为服务接口生成代理对象。有了代理对象，即可进行远程调用。代理对象生成的入口方法为 ProxyFactory 的 getProxy，接下来进行分析。

```java
public <T> T getProxy(Invoker<T> invoker) throws RpcException {
    // 调用重载方法
    return getProxy(invoker, false);
}

public <T> T getProxy(Invoker<T> invoker, boolean generic) throws RpcException {
    Class<?>[] interfaces = null;
    // 获取接口列表
    String config = invoker.getUrl().getParameter("interfaces");
    if (config != null && config.length() > 0) {
        // 切分接口列表
        String[] types = Constants.COMMA_SPLIT_PATTERN.split(config);
        if (types != null && types.length > 0) {
            interfaces = new Class<?>[types.length + 2];
            // 设置服务接口类和 EchoService.class 到 interfaces 中
            interfaces[0] = invoker.getInterface();
            interfaces[1] = EchoService.class;
            for (int i = 0; i < types.length; i++) {
                // 加载接口类
                interfaces[i + 1] = ReflectUtils.forName(types[i]);
            }
        }
    }
    if (interfaces == null) {
        interfaces = new Class<?>[]{invoker.getInterface(), EchoService.class};
    }

    // 为 http 和 hessian 协议提供泛化调用支持，参考 pull request #1827
    if (!invoker.getInterface().equals(GenericService.class) && generic) {
        int len = interfaces.length;
        Class<?>[] temp = interfaces;
        // 创建新的 interfaces 数组
        interfaces = new Class<?>[len + 1];
        System.arraycopy(temp, 0, interfaces, 0, len);
        // 设置 GenericService.class 到数组中
        interfaces[len] = GenericService.class;
    }

    // 调用重载方法
    return getProxy(invoker, interfaces);
}

public abstract <T> T getProxy(Invoker<T> invoker, Class<?>[] types);
```

如上，上面大段代码都是用来获取 interfaces 数组的，我们继续往下看。getProxy(Invoker, Class<?>[]) 这个方法是一个抽象方法，下面我们到 JavassistProxyFactory 类中看一下该方法的实现代码。

```java
public <T> T getProxy(Invoker<T> invoker, Class<?>[] interfaces) {
    // 生成 Proxy 子类（Proxy 是抽象类）。并调用 Proxy 子类的 newInstance 方法创建 Proxy 实例
    return (T) Proxy.getProxy(interfaces).newInstance(new InvokerInvocationHandler(invoker));
}
```

上面代码并不多，首先是通过 Proxy 的 getProxy 方法获取 Proxy 子类，然后创建 InvokerInvocationHandler 对象，并将该对象传给 newInstance 生成 Proxy 实例。InvokerInvocationHandler 实现自 JDK 的 InvocationHandler 接口，具体的用途是拦截接口类调用。该类逻辑比较简单，这里就不分析了。下面我们重点关注一下 Proxy 的 getProxy 方法，如下。

上面代码比较复杂，我们写了大量的注释。大家在阅读这段代码时，要搞清楚 ccp 和 ccm 的用途，不然会被搞晕。ccp 用于为服务接口生成代理类，比如我们有一个 DemoService 接口，这个接口代理类就是由 ccp 生成的。ccm 则是用于为 org.apache.dubbo.common.bytecode.Proxy 抽象类生成子类，主要是实现 Proxy 类的抽象方法。下面以 org.apache.dubbo.demo.DemoService 这个接口为例，来看一下该接口代理类代码大致是怎样的（忽略 EchoService 接口）。

```java
package org.apache.dubbo.common.bytecode;

public class proxy0 implements org.apache.dubbo.demo.DemoService {

    public static java.lang.reflect.Method[] methods;

    private java.lang.reflect.InvocationHandler handler;

    public proxy0() {
    }

    public proxy0(java.lang.reflect.InvocationHandler arg0) {
        handler = $1;
    }

    public java.lang.String sayHello(java.lang.String arg0) {
        Object[] args = new Object[1];
        args[0] = ($w) $1;
        Object ret = handler.invoke(this, methods[0], args);
        return (java.lang.String) ret;
    }
}
```

好了，到这里代理类生成逻辑就分析完了。整个过程比较复杂，大家需要耐心看一下。