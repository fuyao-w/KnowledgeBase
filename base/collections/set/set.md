## Set ##

    public interface Set<E> extends Collection<E>

### java doc ###
不包含重复元素的集合。更正式地说，集合不包含元素对e1和e2，使得e1.equals（e2）和至多一个null元素。正如其名称所暗示的，此接口模拟数学集抽象。
除了从Collection接口继承的那些之外，Set接口在所有构造函数的契约以及add，equals和hashCode方法的契约上放置了额外的规定。为方便起见，此处还包括其他继承方法的声明。 （这些声明附带的规范是针对Set接口定制的，但它们不包含任何其他规定。）

对构造函数的额外规定，毫不奇怪，所有构造函数必须创建一个不包含重复元素的集合（如上所定义）。

注意：如果将可变对象用作set元素，则必须非常小心。如果在对象是集合中的元素的同时以影响等于比较的方式更改对象的值，
则不指定集合的​​行为。这种禁令的一个特例是，不允许将一个集合作为一个元素包含在内。

某些集合实现对它们可能包含的元素有限制。例如，某些实现禁止null元素，并且一些实现对其元素的类型有限制。
尝试添加不合格的元素会引发未经检查的异常，通常是NullPointerException或ClassCastException。
试图查询不合格元素的存在可能会引发异常，或者它可能只是返回false;一些实现将展示前一种行为，一些将展示后者。更一般地，
尝试对不合格的元素进行操作，其完成不会导致将不合格的元素插入到集合中，可以在实现的选择中抛出异常或者它可以成功。
此类异常在此接口的规范中标记为“可选”。

#### 不可修改的集合 ####

`Set.of()`和`Set.copyOf()`静态工厂方法提供了一种创建不可修改集的便捷方法。这些方法创建的Set实例具有以下特征：

它们是不可修改的。无法添加或删除元素。在Set上调用任何mutator方法将始终导致抛出UnsupportedOperationException。但是，如果包含的元素本身是可变的，则可能导致Set表现不一致或其内容似乎发生变化。
他们不允许使用null元素。尝试使用null元素创建它们会导致NullPointerException。
如果所有元素都可序列化，则它们是可序列化的。
他们在创建时拒绝重复元素。传递给静态工厂方法的重复元素会导致IllegalArgumentException。
set元素的迭代顺序未指定，可能会发生变化。
它们是基于价值的。调用者不应对返回实例的身份做出任何假设。工厂可以自由创建新实例或重用现有实例。因此，对这些实例的标识敏感操作（引用相等（==），标识哈希代码和同步）是不可靠的，应该避免。
它们按序列化表单页面上的指定进行序列化。




    static <E> Set<E> of(E e1) {
        return new ImmutableCollections.Set12<>(e1);
    }

它返回了一个新的不可变的set集合。虽然返回的对象有`add()`方法，但是如果调用`add`的话就会抛出

    java.lang.UnsupportedOperationException


## SortSet ##

    public interface SortedSet<E> extends Set<E>

### java doc ###

一个集合，进一步提供其元素的总排序。元素按照它们的自然顺序排序，或者通过通常在排序集创建时提供的比较器排序。 set的迭代器将按升序元素顺序遍历集合。提供了几个额外的操作以利用订购。 （此接口是SortedMap的集合模拟。）
插入到有序集中的所有元素必须实现Comparable接口（或者由指定的比较器接受）。此外，所有这些元素必须是可相互比较的：e1.compareTo（e2）（或comparator.compare（e1，e2））不得为有序集合中的任何元素e1和e2抛出ClassCastException。尝试违反此限制将导致违规方法或构造函数调用抛出ClassCastException。

请注意，如果有序集合要正确实现Set接口，则由有序集合维护的排序（无论是否提供显式比较器）必须与equals一致。 （有关与equals一致的精确定义，请参阅Comparable接口或Comparator接口。）这是因为Set接口是根据equals操作定义的，但是有序集使用compareTo（或compare）方法执行所有元素比较因此，从排序集的角度来看，这种方法被认为相等的两个元素是相等的。即使排序与equals不一致，排序集的行为也是明确定义的;它只是不遵守Set接口的一般合同。

所有通用排序集实现类都应提供四个“标准”构造函数：1）void（无参数）构造函数，它根据元素的自然顺序创建一个空的有序集。 2）具有Comparator类型的单个参数的构造函数，它创建一个根据指定的比较器排序的空的有序集。 3）具有Collection类型的单个参数的构造函数，它创建一个新的有序集合，其元素与其参数相同，并根据元素的自然顺序进行排序。 4）具有SortedSet类型的单个参数的构造函数，它创建一个新的有序集，其具有与输入有序集相同的元素和相同的顺序。由于接口不能包含构造函数，因此无法强制执行此建议。

注意：有几种方法返回具有受限范围的子集。这样的范围是半开放的，即它们包括它们的低端点但不包括它们的高端点（如果适用）。如果您需要一个封闭范围（包括两个端点），并且元素类型允许计算给定值的后继，则只需从lowEndpoint请求子范围到后继（highEndpoint）。例如，假设s是一组有序的字符串。以下习语获取一个视图，其中包含s中从低到高的所有字符串，包括：

    SortedSet<String> sub = s.subSet(low, high+"\0");

可以使用类似的技术来生成开放范围（其中既不包含端点）。以下习语获得一个包含s中从低到高的所有字符串的视图，不包括：

    SortedSet<String> sub = s.subSet(low+"\0", high);


## NavigableSet ##

    public interface NavigableSet<E> extends SortedSet<E>

### java doc ###

使用导航方法扩展的SortedSet返回给定搜索目标的最接近匹配。方法lower（E），floor（E），ceiling（E）和high（E）返回元素分别小于，小于或等于，大于或等于，大于给定元素，如果没有则返回null这样的元素。
可以按升序或降序访问和遍历NavigableSet。 descendingSet（）方法返回集合的视图，其中所有关系和方向方法的感知都被反转。升序操作和视图的性能可能比降序操作的速度快。此接口还定义了方法pollFirst（）和pollLast（），它们返回并删除最低和最高元素（如果存在），否则返回null。方法subSet（E，boolean，E，boolean），headSet（E，boolean）和tailSet（E，boolean）与类似名称的SortedSet方法的不同之处在于接受描述下限和上限是包含还是排除的其他参数。任何NavigableSet的子集都必须实现NavigableSet接口。

在允许空元素的实现中，导航方法的返回值可能是不明确的。但是，即使在这种情况下，也可以通过检查contains（null）来消除结果的歧义。为避免此类问题，鼓励此接口的实现不允许插入null元素。
 （请注意，有条件的Comparable元素集本质上不允许null。）

方法subSet（E，E），headSet（E）和tailSet（E）被指定返回SortedSet以允许对SortedSet的现有实现进行兼容改进以实现NavigableSet，但鼓励此接口的扩展和实现覆盖这些方法返回NavigableSet。

## AbstractSet ##

    public abstract class AbstractSet<E> extends AbstractCollection<E> implements Set<E>

### java doc ###
此类提供set接口的骨干实现，以最大限度地减少实现此接口所需的工作量。
实现这一过程的过程是通过提交set接口完成的。例如， 例如，add方法不允许将多个对象实例添加到集合中。

请注意，此类不会覆盖AbstractCollection类中的任何摘要。 它只是添加了equals和hashCode的实现。


### 分析 ###

以上的接口和抽象类都是set的顶层接口。