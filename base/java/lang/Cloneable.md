## Cloneable（标志类）

类实现了`Cloneable`接口，以向 `Object.clone（）`方法指示该方法合法地为该类的实例制作字段的字段副本。
**在未实现Cloneable接口的实例上调用Object的clone方法会导致抛出异常CloneNotSupportedException。**

按照惯例，实现此接口的类应使用公共方法覆盖Object.clone（受保护）。 有关重写此方法的详细信息，请参阅Object.clone（）。

请注意，此接口不包含克隆方法。 因此，仅仅通过实现该接口实现来克隆对象是不可能的。 即使反射调用clone方法，也无法保证它会成功。

我们想要自己的类有克隆的功能的话，就应该实现该接口，并且重写`clone1e`方法。