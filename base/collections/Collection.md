## Collection  ###

    public interface Collection<E> extends Iterable<E> 

### java doc ###

集合层次结构中的根接口。集合表示一组对象，称为其元素。有些集合允许重复元素而其他集合则不允许。有些是有序的，有些是无序的。 JDK不提供此接口的任何直接实现：它提供了更具体的子接口（如Set和List）的实现。此接口通常用于传递集合并在需要最大通用性的情况下对其进行操作。
包或多个集合（可能包含重复元素的无序集合）应直接实现此接口。

所有通用Collection实现类（通常通过其子接口间接实现Collection）应提供两个“标准”构造函数：void（无参数）构造函数，它创建一个空集合，以及一个构造函数，其中包含一个类型的参数Collection，使用与其参数相同的元素创建新集合。实际上，后一个构造函数允许用户复制任何集合，从而生成所需实现类型的等效集合。没有办法强制执行此约定（因为接口不能包含构造函数），但Java平台库中的所有通用Collection实现都符合。

某些方法被指定为可选的。如果集合实现未实现特定操作，则应定义相应的方法以抛出UnsupportedOperationException。这些方法在集合接口的方法规范中标记为“可选操作”。

某些集合实现对它们可能包含的元素有限制。例如，某些实现禁止null元素，并且一些实现对其元素的类型有限制。尝试添加不合格的元素会引发未经检查的异常，通常是NullPointerException或ClassCastException。试图查询不合格元素的存在可能会引发异常，或者它可能只是返回false;一些实现将展示前一种行为，一些将展示后者。更一般地，尝试对不合格的元素进行操作，其完成不会导致将不合格的元素插入到集合中，可以在实现的选择中抛出异常或者它可以成功。此类异常在此接口的规范中标记为“可选”。

由每个集合决定自己的同步策略。在实现没有更强的保证的情况下，未定义的行为可能是由于另一个线程正在变异的集合上的任何方法的调用而导致的;这包括直接调用，将​​集合传递给可能执行调用的方法，以及使用现有迭代器来检查集合。

Collections Framework接口中的许多方法都是根据equals方法定义的。例如，contains（Object o）方法的规范说：“当且仅当此集合包含至少一个元素e时才返回true（o == null？e == null：o.equals（e）） “。不应将此规范解释为暗示使用非null参数o调用Collection.contains将导致为任何元素e调用o.equals（e）。实现可以自由地实现优化，从而避免等于调用，例如，通过首先比较两个元素的哈希码。 （Object.hashCode（）规范保证具有不等哈希码的两个对象不能相等。）更一般地，各种集合框架接口的实现可以自由地利用底层Object方法的指定行为，无论实现者认为它是否合适。

执行集合的递归遍历的某些集合操作可能会失败，并且集合直接或间接包含自身的自引用实例也会失败。这包括clone（），equals（），hashCode（）和toString（）方法。实现可以可选地处理自引用场景，但是大多数当前实现不这样做。

#### View Collections ####

大多数集合管理它们包含的元素的存储。相比之下，视图集合本身不存储元素，而是依赖于后备集合来存储实际元素。视图集合本身未处理的操作将委派给后备集合。视图集合的示例包括由Collections.checkedCollection，Collections.synchronizedCollection和Collections.unmodifiableCollection等方法返回的包装器集合。视图集合的其他示例包括提供相同元素的不同表示的集合，例如，由List.subList，NavigableSet.subSet或Map.entrySet提供。对视图集合所做的任何更改都在视图集合中可见。相应地，对视图集合所做的任何更改（如果允许更改）都会写入后备集合。虽然它们在技术上不是集合，但Iterator和ListIterator的实例也可以允许将修改写入后备集合，并且在某些情况下，迭代期间Iterator可以看到对后备集合的修改。

#### Unmodifiable Collections ####

该接口的某些方法被认为是“破坏性的”并且被称为“mutator”方法，因为它们修改它们操作的集合中包含的对象组。如果此集合实现不支持该操作，则可以指定它们抛出UnsupportedOperationException。如果调用对集合没有影响，则此类方法应该（但不是必须）抛出UnsupportedOperationException。例如，考虑不支持添加操作的集合。如果在此集合上调用addAll方法，并将一个空集合作为参数，会发生什么？添加零元素没有任何影响，因此允许此集合只是不做任何事情而不是抛出异常。但是，建议此类情况无条件地抛出异常，因为仅在某些情况下抛出可能会导致编程错误。

不可修改的集合是一个集合，其所有mutator方法（如上所定义）都被指定为抛出UnsupportedOperationException。因此，无法通过调用任何方法来修改此类集合。要使集合正确无法修改，从中派生的任何视图集合也必须是不可修改的。例如，如果List是不可修改的，则List.subList返回的List也是不可修改的。

不可修改的集合不一定是不可变的。如果包含的元素是可变的，那么整个集合显然是可变的，即使它可能是不可修改的。例如，考虑两个包含可变元素的不可修改列表。如果元素已经变异，则调用list1.equals（list2）的结果可能因调用一次而有所不同，即使两个列表都是不可修改的。但是，如果不可修改的集合包含所有不可变元素，则可以认为它是有效的不可变的。

#### Unmodifiable View Collections ####

不可修改的视图集合是一个不可修改的集合，也是一个支持集合的视图。如上所述，其mutator方法抛出UnsupportedOperationException，而读取和查询方法则委托给后备集合。其结果是提供对后备集合的只读访问。这对于组件来说非常有用，可以为用户提供对内部集合的读访问权限，同时防止他们意外地修改此类集合。不可修改的视图集合的示例是Collections.unmodifiableCollection，Collections.unmodifiableList和相关方法返回的集合。

请注意，可能仍然可以对支持集合进行更改，如果它们发生，则通过不可修改的视图可以看到它们。因此，不可修改的视图集合不一定是不可变的。但是，如果不可修改视图的后备集合实际上是不可变的，或者对后备集合的唯一引用是通过不可修改的视图，则视图可以被视为有效不可变。



## AbstractCollection ##

该类提供了Collection接口的骨架实现，以尽量减少实现此接口所需的工作量。
该抽象类的方法都是基于迭代器实现的,为了实现一个不可修改的集合，程序员只需要扩展这个类并提供iterator和size方法的实现。 （ iterator方法返回的迭代器必须实现hasNext和next ） 
要实现可修改的集合，程序员必须另外覆盖此类的add方法（否则将抛出UnsupportedOperationException ），并且由iterator方法返回的迭代器必须另外实现其remove方法。

该类有一个变量 `static final int MAX_ARRAY_SIZE= Integer.MAX_VALUE - 8;`这个变量代表要分配的最大数组大小。 有些VM会在数组中保留一些header信息。 尝试分配更大的数组可能会导致OutOfMemoryError：请求的数组大小超过VM限制。

该类没有`get()`方法

该类也有两个抽象方法是子类必须实现的
1. `public abstract Iterator<E> iterator(); //返回此collection中包含的元素的迭代器。`
 
2. `public abstract int size();` 



