PropertyOverConfigurater 是用来将配置文件的key / value 属实在 beanDefinition 实例化后，覆盖原有属性值，实现注入的 beanFactory后处理器。

启动该处理器的方法有两种

1. 在xml 配置文件中配置 <context:property-override location="properties"/>

2. 在配置文件中配置 

   ```xml
   <bean class="org.springframework.beans.factory.config.PropertyOverrideConfigurer">
       <property name="location" value="properties"/>
   </bean>
   ```

3. 通过编程的方式实现该类

   ```java
   @Component
   public class MyFactoryPostProcess extends PropertyOverrideConfigurer {
       public MyFactoryPostProcess() {
           this.setLocation(new ClassPathResource("properties"));
       }
   
       @Override
       protected String convertPropertyValue(String originalValue) {
   
           return originalValue.toUpperCase();
       }
   }
   ```

   或者通过 @configuration 注解

   向容器添加该工厂后处理器。

加载机制：

分析一下 通过 property-override 配置的实现。

因为是通过xml 的特殊配置加载的，所以要从 beanFacotry 加载的时候说起。在beanFactory 初始化的时候，调用 doLoadBeanDefinition 方法从 Resource 里加载xml配置文件的时候。对扫描到的每一个元素（element）都会通过 xmlReadContext 根据命名空间从 `handlerMappings`（比如有处理 context 命名空间的，处理AOP 明明空间的等等） 中找到相应的 `NamespaceHandler`。就整个的运行流程来说，Spring MVC 的运行过程也是类似的。现在应该调用 ContextNamespaceHandler 来处理相应的解析 element（现在处理的 element 就是`property-override`）。

```java
public interface NamespaceHandler {
    	void init();
    	BeanDefinition parse(Element element, ParserContext parserContext);
    	BeanDefinitionHolder decorate(Node source, BeanDefinitionHolder definition, ParserContext parserContext);
    
}
```

有默认的命名空间处理器比如处理Bean 元素的 `BeanDefinitionParserDelegate`  调用`parseBeanDefinitionElement`处理。

```java
/**
 * Parses the supplied {@code <bean>} element. May return {@code null}
 * if there were errors during parse. Errors are reported to the
 * {@link org.springframework.beans.factory.parsing.ProblemReporter}.
 */
public BeanDefinitionHolder parseBeanDefinitionElement(Element ele, BeanDefinition containingBean) {
   String id = ele.getAttribute(ID_ATTRIBUTE);
   String nameAttr = ele.getAttribute(NAME_ATTRIBUTE);

   List<String> aliases = new ArrayList<String>();
   if (StringUtils.hasLength(nameAttr)) {
      String[] nameArr = StringUtils.tokenizeToStringArray(nameAttr, MULTI_VALUE_ATTRIBUTE_DELIMITERS);
      aliases.addAll(Arrays.asList(nameArr));
   }

   String beanName = id;
   if (!StringUtils.hasText(beanName) && !aliases.isEmpty()) {
      beanName = aliases.remove(0);
      if (logger.isDebugEnabled()) {
         logger.debug("No XML 'id' specified - using '" + beanName +
               "' as bean name and " + aliases + " as aliases");
      }
   }

   if (containingBean == null) {
      checkNameUniqueness(beanName, aliases, ele);
   }

   AbstractBeanDefinition beanDefinition = parseBeanDefinitionElement(ele, beanName, containingBean);
   if (beanDefinition != null) {
      if (!StringUtils.hasText(beanName)) {
         try {
            if (containingBean != null) {
               beanName = BeanDefinitionReaderUtils.generateBeanName(
                     beanDefinition, this.readerContext.getRegistry(), true);
            }
            else {
               beanName = this.readerContext.generateBeanName(beanDefinition);
               // Register an alias for the plain bean class name, if still possible,
               // if the generator returned the class name plus a suffix.
               // This is expected for Spring 1.2/2.0 backwards compatibility.
               String beanClassName = beanDefinition.getBeanClassName();
               if (beanClassName != null &&
                     beanName.startsWith(beanClassName) && beanName.length() > beanClassName.length() &&
                     !this.readerContext.getRegistry().isBeanNameInUse(beanClassName)) {
                  aliases.add(beanClassName);
               }
            }
            if (logger.isDebugEnabled()) {
               logger.debug("Neither XML 'id' nor 'name' specified - " +
                     "using generated bean name [" + beanName + "]");
            }
         }
         catch (Exception ex) {
            error(ex.getMessage(), ele);
            return null;
         }
      }
      String[] aliasesArray = StringUtils.toStringArray(aliases);
      return new BeanDefinitionHolder(beanDefinition, beanName, aliasesArray);
   }

   return null;
}
```

当然还有处理特殊情况的命名空间处理器。就比如处理`property-override)` 的。

下面 `NamespaceHandler`需要根据localName(property-override)找到对应的`BeanDefinitionParser`，对应的就为 `PropertyOverrideBeanDefinitionParser`。然后调用其`parse`方法来解析 elememt 。得到`AbstractBeanDefinition`然后将其注册。

```java
class PropertyOverrideBeanDefinitionParser extends AbstractPropertyLoadingBeanDefinitionParser {

   @Override
   protected Class<?> getBeanClass(Element element) {
      return PropertyOverrideConfigurer.class;
   }

   @Override
   protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
      super.doParse(element, parserContext, builder);

      builder.addPropertyValue("ignoreInvalidKeys",
            Boolean.valueOf(element.getAttribute("ignore-unresolvable")));

   }

}
```



```java
@Override
public final BeanDefinition parse(Element element, ParserContext parserContext) {
   AbstractBeanDefinition definition = parseInternal(element, parserContext);
   if (definition != null && !parserContext.isNested()) {
      try {
         String id = resolveId(element, definition, parserContext);
         if (!StringUtils.hasText(id)) {
            parserContext.getReaderContext().error(
                  "Id is required for element '" + parserContext.getDelegate().getLocalName(element)
                        + "' when used as a top-level tag", element);
         }
         String[] aliases = null;
         if (shouldParseNameAsAliases()) {
            String name = element.getAttribute(NAME_ATTRIBUTE);
            if (StringUtils.hasLength(name)) {
               aliases = StringUtils.trimArrayElements(StringUtils.commaDelimitedListToStringArray(name));
            }
         }
         BeanDefinitionHolder holder = new BeanDefinitionHolder(definition, id, aliases);
         registerBeanDefinition(holder, parserContext.getRegistry());
         if (shouldFireEvents()) {
            BeanComponentDefinition componentDefinition = new BeanComponentDefinition(holder);
            postProcessComponentDefinition(componentDefinition);
            parserContext.registerComponent(componentDefinition);
         }
      }
      catch (BeanDefinitionStoreException ex) {
         parserContext.getReaderContext().error(ex.getMessage(), element);
         return null;
      }
   }
   return definition;
}
```

调用 AbstractSingleBeanDefinitionParser 的`parseInternal` 方法解析，并返回其 BeanDefinition。

```java
protected final AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
   BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition();
   String parentName = getParentName(element);
   if (parentName != null) {
      builder.getRawBeanDefinition().setParentName(parentName);
   }
   Class<?> beanClass = getBeanClass(element);
   if (beanClass != null) {
      builder.getRawBeanDefinition().setBeanClass(beanClass);
   }
   else {
      String beanClassName = getBeanClassName(element);
      if (beanClassName != null) {
         builder.getRawBeanDefinition().setBeanClassName(beanClassName);
      }
   }
   builder.getRawBeanDefinition().setSource(parserContext.extractSource(element));
   if (parserContext.isNested()) {
      // Inner bean definition must receive same scope as containing bean.
      builder.setScope(parserContext.getContainingBeanDefinition().getScope());
   }
   if (parserContext.isDefaultLazyInit()) {
      // Default-lazy-init applies to custom bean definitions as well.
      builder.setLazyInit(true);
   }
   doParse(element, parserContext, builder);
   return builder.getBeanDefinition();
}
```

通过调用`doParse`生成beanDefinition，添加到`BeanDefinitionBuilder`。

```java
@Override
protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
   String location = element.getAttribute("location");
   if (StringUtils.hasLength(location)) {
      location = parserContext.getReaderContext().getEnvironment().resolvePlaceholders(location);
      String[] locations = StringUtils.commaDelimitedListToStringArray(location);
      builder.addPropertyValue("locations", locations);
   }

   String propertiesRef = element.getAttribute("properties-ref");
   if (StringUtils.hasLength(propertiesRef)) {
      builder.addPropertyReference("properties", propertiesRef);
   }

   String fileEncoding = element.getAttribute("file-encoding");
   if (StringUtils.hasLength(fileEncoding)) {
      builder.addPropertyValue("fileEncoding", fileEncoding);
   }

   String order = element.getAttribute("order");
   if (StringUtils.hasLength(order)) {
      builder.addPropertyValue("order", Integer.valueOf(order));
   }

   builder.addPropertyValue("ignoreResourceNotFound",
         Boolean.valueOf(element.getAttribute("ignore-resource-not-found")));

   builder.addPropertyValue("localOverride",
         Boolean.valueOf(element.getAttribute("local-override")));

   builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
}
```

这样一个element 解析过程就完成了。