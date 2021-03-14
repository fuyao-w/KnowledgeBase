### AutowiredAnnotationBeanPostProcessor

自动注入的原理是通过特殊的后处理器 AutowiredAnnotationBeanPostProcessor 实现的。

doCreateBean 是在`getBean` 是创建bean 调用的方法。

```java
protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final Object[] args)
      throws BeanCreationException {

   // Instantiate the bean.
   BeanWrapper instanceWrapper = null;
   if (mbd.isSingleton()) {
      instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
   }
   if (instanceWrapper == null) {
      instanceWrapper = createBeanInstance(beanName, mbd, args);
   }
   final Object bean = (instanceWrapper != null ? instanceWrapper.getWrappedInstance() : null);
   Class<?> beanType = (instanceWrapper != null ? instanceWrapper.getWrappedClass() : null);
   mbd.resolvedTargetType = beanType;

   // Allow post-processors to modify the merged bean definition.
   synchronized (mbd.postProcessingLock) {
      if (!mbd.postProcessed) {
         try {
            applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
         }
         catch (Throwable ex) {
            throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                  "Post-processing of merged bean definition failed", ex);
         }
         mbd.postProcessed = true;
      }
   }

   // Eagerly cache singletons to be able to resolve circular references
   // even when triggered by lifecycle interfaces like BeanFactoryAware.
   boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
         isSingletonCurrentlyInCreation(beanName));
   if (earlySingletonExposure) {
      if (logger.isDebugEnabled()) {
         logger.debug("Eagerly caching bean '" + beanName +
               "' to allow for resolving potential circular references");
      }
      addSingletonFactory(beanName, new ObjectFactory<Object>() {
         @Override
         public Object getObject() throws BeansException {
            return getEarlyBeanReference(beanName, mbd, bean);
         }
      });
   }

   // Initialize the bean instance.
   Object exposedObject = bean;
   try {
      populateBean(beanName, mbd, instanceWrapper);
      if (exposedObject != null) {
         exposedObject = initializeBean(beanName, exposedObject, mbd);
      }
   }
   catch (Throwable ex) {
      if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
         throw (BeanCreationException) ex;
      }
      else {
         throw new BeanCreationException(
               mbd.getResourceDescription(), beanName, "Initialization of bean failed", ex);
      }
   }

   if (earlySingletonExposure) {
      Object earlySingletonReference = getSingleton(beanName, false);
      if (earlySingletonReference != null) {
         if (exposedObject == bean) {
            exposedObject = earlySingletonReference;
         }
         else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
            String[] dependentBeans = getDependentBeans(beanName);
            Set<String> actualDependentBeans = new LinkedHashSet<String>(dependentBeans.length);
            for (String dependentBean : dependentBeans) {
               if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
                  actualDependentBeans.add(dependentBean);
               }
            }
            if (!actualDependentBeans.isEmpty()) {
               throw new BeanCurrentlyInCreationException(beanName,
                     "Bean with name '" + beanName + "' has been injected into other beans [" +
                     StringUtils.collectionToCommaDelimitedString(actualDependentBeans) +
                     "] in its raw version as part of a circular reference, but has eventually been " +
                     "wrapped. This means that said other beans do not use the final version of the " +
                     "bean. This is often the result of over-eager type matching - consider using " +
                     "'getBeanNamesOfType' with the 'allowEagerInit' flag turned off, for example.");
            }
         }
      }
   }

   // Register bean as disposable.
   try {
      registerDisposableBeanIfNecessary(beanName, bean, mbd);
   }
   catch (BeanDefinitionValidationException ex) {
      throw new BeanCreationException(
            mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
   }

   return exposedObject;
}
```

执行自动注入处理器的逻辑如下。

```java
synchronized (mbd.postProcessingLock) {
   if (!mbd.postProcessed) {
      try {
         applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
      }
      catch (Throwable ex) {
         throw new BeanCreationException(mbd.getResourceDescription(), beanName,
               "Post-processing of merged bean definition failed", ex);
      }
      mbd.postProcessed = true;
   }
}
```

```java
protected void applyMergedBeanDefinitionPostProcessors(RootBeanDefinition mbd, Class<?> beanType, String beanName) {
   for (BeanPostProcessor bp : getBeanPostProcessors()) {
      if (bp instanceof MergedBeanDefinitionPostProcessor) {
         MergedBeanDefinitionPostProcessor bdp = (MergedBeanDefinitionPostProcessor) bp;
         bdp.postProcessMergedBeanDefinition(mbd, beanType, beanName);
      }
   }
}
```

AutowiredAnnotationBeanPostProcessor 实现 AutowiredAnnotationBeanPostProcessor 接口，调用 postProcessMergedBeanDefinition。

```java
@Override
public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
   if (beanType != null) {
      InjectionMetadata metadata = findAutowiringMetadata(beanName, beanType, null);
      metadata.checkConfigMembers(beanDefinition);
   }
}
```

```java
private InjectionMetadata findAutowiringMetadata(String beanName, Class<?> clazz, PropertyValues pvs) {
   // Fall back to class name as cache key, for backwards compatibility with custom callers.
   String cacheKey = (StringUtils.hasLength(beanName) ? beanName : clazz.getName());
   // Quick check on the concurrent map first, with minimal locking.
   InjectionMetadata metadata = this.injectionMetadataCache.get(cacheKey);
   if (InjectionMetadata.needsRefresh(metadata, clazz)) {
      synchronized (this.injectionMetadataCache) {
         metadata = this.injectionMetadataCache.get(cacheKey);
         if (InjectionMetadata.needsRefresh(metadata, clazz)) {
            if (metadata != null) {
               metadata.clear(pvs);
            }
            try {
               metadata = buildAutowiringMetadata(clazz);
               this.injectionMetadataCache.put(cacheKey, metadata);
            }
            catch (NoClassDefFoundError err) {
               throw new IllegalStateException("Failed to introspect bean class [" + clazz.getName() +
                     "] for autowiring metadata: could not find class that it depends on", err);
            }
         }
      }
   }
   return metadata;
}
```

查找 自动注入注解的逻辑在 buildAutowiringMetadata

```java
private InjectionMetadata buildAutowiringMetadata(final Class<?> clazz) {
   LinkedList<InjectionMetadata.InjectedElement> elements = new LinkedList<InjectionMetadata.InjectedElement>();
   Class<?> targetClass = clazz;

   do {
      final LinkedList<InjectionMetadata.InjectedElement> currElements =
            new LinkedList<InjectionMetadata.InjectedElement>();

      ReflectionUtils.doWithLocalFields(targetClass, new ReflectionUtils.FieldCallback() {
         @Override
         public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
            AnnotationAttributes ann = findAutowiredAnnotation(field);
            if (ann != null) {
               if (Modifier.isStatic(field.getModifiers())) {
                  if (logger.isWarnEnabled()) {
                     logger.warn("Autowired annotation is not supported on static fields: " + field);
                  }
                  return;
               }
               boolean required = determineRequiredStatus(ann);
               currElements.add(new AutowiredFieldElement(field, required));
            }
         }
      });

      ReflectionUtils.doWithLocalMethods(targetClass, new ReflectionUtils.MethodCallback() {
         @Override
         public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
            Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
            if (!BridgeMethodResolver.isVisibilityBridgeMethodPair(method, bridgedMethod)) {
               return;
            }
            AnnotationAttributes ann = findAutowiredAnnotation(bridgedMethod);
            if (ann != null && method.equals(ClassUtils.getMostSpecificMethod(method, clazz))) {
               if (Modifier.isStatic(method.getModifiers())) {
                  if (logger.isWarnEnabled()) {
                     logger.warn("Autowired annotation is not supported on static methods: " + method);
                  }
                  return;
               }
               if (method.getParameterTypes().length == 0) {
                  if (logger.isWarnEnabled()) {
                     logger.warn("Autowired annotation should only be used on methods with parameters: " +
                           method);
                  }
               }
               boolean required = determineRequiredStatus(ann);
               PropertyDescriptor pd = BeanUtils.findPropertyForMethod(bridgedMethod, clazz);
               currElements.add(new AutowiredMethodElement(method, required, pd));
            }
         }
      });

      elements.addAll(0, currElements);
      targetClass = targetClass.getSuperclass();
   }
   while (targetClass != null && targetClass != Object.class);

   return new InjectionMetadata(clazz, elements);
}
```

分为两部分一部分用于处理字段，另一部分用于处理方法。

```java
private AnnotationAttributes findAutowiredAnnotation(AccessibleObject ao) {
   if (ao.getAnnotations().length > 0) {  // autowiring annotations have to be local
      for (Class<? extends Annotation> type : this.autowiredAnnotationTypes) {
         AnnotationAttributes attributes = AnnotatedElementUtils.getMergedAnnotationAttributes(ao, type);
         if (attributes != null) {
            return attributes;
         }
      }
   }
   return null;
}
```

只要找到 一个注解就返回。

最后需要注入的字段 被包装为 `InjectionMetadata`

```java
public InjectionMetadata(Class<?> targetClass, Collection<InjectedElement> elements) {
   this.targetClass = targetClass;
   this.injectedElements = elements;
}
```

`populateBean` 用于填充 bean 的属性。

```java
protected void populateBean(String beanName, RootBeanDefinition mbd, BeanWrapper bw) {
   PropertyValues pvs = mbd.getPropertyValues();

   if (bw == null) {
      if (!pvs.isEmpty()) {
         throw new BeanCreationException(
               mbd.getResourceDescription(), beanName, "Cannot apply property values to null instance");
      }
      else {
         // Skip property population phase for null instance.
         return;
      }
   }

   // Give any InstantiationAwareBeanPostProcessors the opportunity to modify the
   // state of the bean before properties are set. This can be used, for example,
   // to support styles of field injection.
   boolean continueWithPropertyPopulation = true;

   if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
      for (BeanPostProcessor bp : getBeanPostProcessors()) {
         if (bp instanceof InstantiationAwareBeanPostProcessor) {
            InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
            if (!ibp.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)) {
               continueWithPropertyPopulation = false;
               break;
            }
         }
      }
   }

   if (!continueWithPropertyPopulation) {
      return;
   }

   if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_NAME ||
         mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_TYPE) {
      MutablePropertyValues newPvs = new MutablePropertyValues(pvs);

      // Add property values based on autowire by name if applicable.
      if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_NAME) {
         autowireByName(beanName, mbd, bw, newPvs);
      }

      // Add property values based on autowire by type if applicable.
      if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_TYPE) {
         autowireByType(beanName, mbd, bw, newPvs);
      }

      pvs = newPvs;
   }

   boolean hasInstAwareBpps = hasInstantiationAwareBeanPostProcessors();
   boolean needsDepCheck = (mbd.getDependencyCheck() != RootBeanDefinition.DEPENDENCY_CHECK_NONE);

   if (hasInstAwareBpps || needsDepCheck) {
      PropertyDescriptor[] filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
      if (hasInstAwareBpps) {
         for (BeanPostProcessor bp : getBeanPostProcessors()) {
            if (bp instanceof InstantiationAwareBeanPostProcessor) {
               InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
               pvs = ibp.postProcessPropertyValues(pvs, filteredPds, bw.getWrappedInstance(), beanName);
               if (pvs == null) {
                  return;
               }
            }
         }
      }
      if (needsDepCheck) {
         checkDependencies(beanName, mbd, filteredPds, pvs);
      }
   }

   applyPropertyValues(beanName, mbd, bw, pvs);
}
```

填充逻辑在 

```java
if (hasInstAwareBpps || needsDepCheck) {
   PropertyDescriptor[] filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
   if (hasInstAwareBpps) {
      for (BeanPostProcessor bp : getBeanPostProcessors()) {
         if (bp instanceof InstantiationAwareBeanPostProcessor) {
            InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
            pvs = ibp.postProcessPropertyValues(pvs, filteredPds, bw.getWrappedInstance(), beanName);
            if (pvs == null) {
               return;
            }
         }
      }
   }
   if (needsDepCheck) {
      checkDependencies(beanName, mbd, filteredPds, pvs);
   }
}
```

调用 AutowiredaAnnotationBeanPostProcesser 的`postProcessPropertyValues`

```java
public PropertyValues postProcessPropertyValues(
      PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeanCreationException {

   InjectionMetadata metadata = findAutowiringMetadata(beanName, bean.getClass(), pvs);
   try {
      metadata.inject(bean, beanName, pvs);
   }
   catch (BeanCreationException ex) {
      throw ex;
   }
   catch (Throwable ex) {
      throw new BeanCreationException(beanName, "Injection of autowired dependencies failed", ex);
   }
   return pvs;
}
```

在获取到之前的注入元素  InjectionMetadata 后调用 `inject`

```JAVA
    @Override
   protected void inject(Object bean, String beanName, PropertyValues pvs) throws Throwable {
      Field field = (Field) this.member;
      Object value;
      if (this.cached) {
         value = resolvedCachedArgument(beanName, this.cachedFieldValue);
      }
      else {
         DependencyDescriptor desc = new DependencyDescriptor(field, this.required);
         desc.setContainingClass(bean.getClass());
         Set<String> autowiredBeanNames = new LinkedHashSet<String>(1);
         TypeConverter typeConverter = beanFactory.getTypeConverter();
         try {
            value = beanFactory.resolveDependency(desc, beanName, autowiredBeanNames, typeConverter);
         }
         catch (BeansException ex) {
            throw new UnsatisfiedDependencyException(null, beanName, new InjectionPoint(field), ex);
         }
         synchronized (this) {
            if (!this.cached) {
               if (value != null || this.required) {
                  this.cachedFieldValue = desc;
                  registerDependentBeans(beanName, autowiredBeanNames);
                  if (autowiredBeanNames.size() == 1) {
                     String autowiredBeanName = autowiredBeanNames.iterator().next();
                     if (beanFactory.containsBean(autowiredBeanName) &&
                           beanFactory.isTypeMatch(autowiredBeanName, field.getType())) {
                        this.cachedFieldValue = new ShortcutDependencyDescriptor(
                              desc, autowiredBeanName, field.getType());
                     }
                  }
               }
               else {
                  this.cachedFieldValue = null;
               }
               this.cached = true;
            }
         }
      }
      if (value != null) {
         ReflectionUtils.makeAccessible(field);
         field.set(bean, value);
      }
   }
}
```

最终通过反射给字段赋值。