Spring AOP 默认使用jdk 动态代理。也可以使用CGlib 代理。默认情况下如果业务类未实现接口，而是实现了类，则是哟 CGLIB。

@AspectJ 支持

Spring 使用AspectJ 的注解，将切面声明为使用注解的 java 类。但是AOP 运行时仍然是 Spring AOP,并不依赖 AspectJ 编译器或者 weaver。

