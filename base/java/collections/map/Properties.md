## Properties

### java doc

Properties类表示一组持久的属性。可以将属性保存到流中或从流中加载。属性列表中的每个键及其对应的值都是一个字符串。
属性列表可以包含另一个属性列表作为其“默认值”;如果在原始属性列表中找不到属性键，则搜索第二个属性列表。

由于Properties继承自Hashtable，因此put和putAll方法可以应用于Properties对象。强烈建议不要使用它们，因为它们允许调用者插入其键或值不是字符串的条目。应该使用setProperty方法。如果在包含非String键或值的“受损”Properties对象上调用store或save方法，则调用将失败。同样，如果在包含非String键的“受损”Properties对象上调用propertyNames或list方法，则对该方法的调用将失败。

由此类的“集合视图”（即，entrySet（），keySet（）和values（））的迭代器方法返回的迭代器可能不会快速失败（与Hashtable实现不同）。这些迭代器保证遍历元素，因为它们在构造时只存在一次，并且可能（但不保证）反映构造之后的任何修改。

load（Reader）/ store（Writer，String）方法以下面指定的简单的面向行的格式从基于字符的流加载和存储属性。 load（InputStream）/ store（OutputStream，String）方法的工作方式与load（Reader）/ store（Writer，String）对的工作方式相同，不同之处在于输入/输出流以`ISO-8859-1`字符编码进行编码。无法在此编码中直接表示的字符可以使用“Java™语言规范”第3.3节中定义的Unicode转义编写;在转义序列中只允许一个'u'字符。

loadFromXML（InputStream）和storeToXML（OutputStream，String，String）方法以简单的XML格式加载和存储属性。 默认情况下，使用UTF-8字符编码，但是如果需要，可以指定特定的编码。 实现需要支持UTF-8和UTF-16，并且可能支持其他编码。 XML属性文档具有以下DOCTYPE声明：

```
 <!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
 
```

请注意，导出或导入属性时不会访问系统URI（http://java.sun.com/dtd/properties.dtd）; 它只是作为一个字符串来唯一标识DTD，它是：

```xml
    <?xml version="1.0" encoding="UTF-8"?>

    <!-- DTD for properties -->

    <!ELEMENT properties ( comment?, entry* ) >

    <!ATTLIST properties version CDATA #FIXED "1.0">

    <!ELEMENT comment (#PCDATA) >

    <!ELEMENT entry (#PCDATA) >

    <!ATTLIST entry key CDATA #REQUIRED>
```

此类是线程安全的：多个线程可以共享单个Properties对象，而无需外部同步。



## 分析

Properties继承自HashTable,一般用于存储系统属性，`System.getProperties()`。它的标准使用方法是调用`setProperty()`而不是父类的`put`方法。使用`ConcurrentHashMap`来，存储字符串键值对。并且在相关方法上用`Sychronized`修饰。它的初始值默认是8，其实是`ConcurrentHashMap`的初始值。不能设置加载因子。可以将字符串属性，通过流输入输出，或者写入或者读取xml。