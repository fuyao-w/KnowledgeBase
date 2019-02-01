Spring IOC 的核心就是 ApplicationContext，首先从它开始分析：

ApplicationContext:用于为应用程序提供配置的中央接口。 这在应用程序运行时是只读的，但如果实现支持，则可以重新加载。

ApplicationContext提供：

- Bean工厂方法，用于访问应用程序组件 继承自ListableBeanFactory。
- 以通用方式加载文件资源的能力。 继承自ResourceLoader接口。
- 将事件发布到已注册的侦听器的功能。 继承自ApplicationEventPublisher接口。
- 解决消息，支持国际化的能力。 继承自MessageSource接口。
- 从父上下文继承。 后代上下文中的定义始终优先。 这意味着，例如，整个Web应用程序可以使用单个父上下文，而每个servlet都有自己的子上下文，该上下文独立于任何其他servlet的子上下文。

除了标准 BeanFactory 生命周期功能之外，ApplicationContext 实现还检测并调用ApplicationContextAware bean以及ResourceLoaderAware，ApplicationEventPublisherAware和MessageSourceAware beans。

![](G:\KnowledgeBase\picture\Spring\ApplicationContextDiagram.png)

先看一下 ApplicationContext 的顶层接口

- ResourcePatternResolver：用于将位置模式（例如，Ant样式路径模式）解析为Resource对象的策略接口。
- ApplicationEventPublisher：封装事件发布功能的接口。 用作 ApplicationContext 的超类接口。
- MessageSource：用于解析消息的策略接口，支持此类消息的参数化和国际化。
- EnvironmentCapable：表明所拥有的和公开 Environment 引用的组件的接口。 所有Spring应用程序上下文都是EnvironmentCapable，该接口主要用于在接受BeanFactory实例的框架方法中执行 instanceof 检查，这些实例可能实际上也可能不是ApplicationContext实例，以便在环境可用时与环境进行交互。
- ListableBeanFactory：BeanFactory 接口的扩展，需要被可以枚举出所有 bean 的 bean 工厂来实现，而不是按照客户端要求逐个按名称查找 bean。
- HierarchicalBeanFactory：由bean工厂实现的子接口，可以是层次结构的一部分。

