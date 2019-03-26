Environment 的类总结在 [Environment.md](Environment.md)  中，下面分析具体代码。

先看一下占位符的用法 

```java
properties.setProperty("midname", "names");
properties.setProperty("{midname}", "names");
hello ${mid${p:name}}  -> hello names
hello ${{midname}} -> hello names
```

```java
public class PropertyPlaceholderHelper {

   private static final Log logger = LogFactory.getLog(PropertyPlaceholderHelper.class);

   private static final Map<String, String> wellKnownSimplePrefixes = new HashMap<String, String>(4);

   static {
       //预置占位符
      wellKnownSimplePrefixes.put("}", "{");
      wellKnownSimplePrefixes.put("]", "[");
      wellKnownSimplePrefixes.put(")", "(");
   }

	// 占位符前缀
   private final String placeholderPrefix;
	// 占位符后缀
   private final String placeholderSuffix;
	//简单前缀，例如 ${ -> { 用于嵌套
    //但是不会解析它，例如 {a} 。propertys中也要有相应的 key {a}。
    //又或者 ${${a}} 将会提取出 ${a}
   private final String simplePrefix;
	// 分隔符，用于解析占位符元素用
   private final String valueSeparator;
	//忽略解析不了的占位符
   private final boolean ignoreUnresolvablePlaceholders;


   /**
    * Creates a new {@code PropertyPlaceholderHelper} that uses the supplied prefix and suffix.
    * Unresolvable placeholders are ignored.
    * @param placeholderPrefix the prefix that denotes the start of a placeholder
    * @param placeholderSuffix the suffix that denotes the end of a placeholder
    */
   public PropertyPlaceholderHelper(String placeholderPrefix, String placeholderSuffix) {
      this(placeholderPrefix, placeholderSuffix, null, true);
   }

   /**
    * Creates a new {@code PropertyPlaceholderHelper} that uses the supplied prefix and suffix.
    * @param placeholderPrefix the prefix that denotes the start of a placeholder
    * @param placeholderSuffix the suffix that denotes the end of a placeholder
    * @param valueSeparator the separating character between the placeholder variable
    * and the associated default value, if any
    * @param ignoreUnresolvablePlaceholders indicates whether unresolvable placeholders should
    * be ignored ({@code true}) or cause an exception ({@code false})
    */
   public PropertyPlaceholderHelper(String placeholderPrefix, String placeholderSuffix,
         String valueSeparator, boolean ignoreUnresolvablePlaceholders) {
		// 默认传递进来的是 AbstractPropertyResolver 类里面定义的 prefix -> ${   suffix ->}  
       // valueSeparator -> : 
      Assert.notNull(placeholderPrefix, "'placeholderPrefix' must not be null");
      Assert.notNull(placeholderSuffix, "'placeholderSuffix' must not be null");
      this.placeholderPrefix = placeholderPrefix;
      this.placeholderSuffix = placeholderSuffix;
      String simplePrefixForSuffix = wellKnownSimplePrefixes.get(this.placeholderSuffix);
      if (simplePrefixForSuffix != null && this.placeholderPrefix.endsWith(simplePrefixForSuffix)) {		//如果占位符后缀，在预置占位符里，则将预置占位符的前缀赋值给 simplePrefix
         this.simplePrefix = simplePrefixForSuffix;
      }
      else {
         this.simplePrefix = this.placeholderPrefix;
      }
      this.valueSeparator = valueSeparator;
      this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
   }


   /**
    * Replaces all placeholders of format {@code ${name}} with the corresponding
    * property from the supplied {@link Properties}.
    * @param value the value containing the placeholders to be replaced
    * @param properties the {@code Properties} to use for replacement
    * @return the supplied value with placeholders replaced inline
    */
   public String replacePlaceholders(String value, final Properties properties) {
      Assert.notNull(properties, "'properties' must not be null");
       // 调用replacePlaceholders 并传递给它一个 策略接口，PlaceholderResolver（在该类的最底部定义）
      return replacePlaceholders(value, new PlaceholderResolver() {
         @Override
         public String resolvePlaceholder(String placeholderName) {
            return properties.getProperty(placeholderName);
         }
      });
   }

   /**
    * Replaces all placeholders of format {@code ${name}} with the value returned
    * from the supplied {@link PlaceholderResolver}.
    * @param value the value containing the placeholders to be replaced
    * @param placeholderResolver the {@code PlaceholderResolver} to use for replacement
    * @return the supplied value with placeholders replaced inline
    */
   public String replacePlaceholders(String value, PlaceholderResolver placeholderResolver) {
      Assert.notNull(value, "'value' must not be null");
      return parseStringValue(value, placeholderResolver, new HashSet<String>());
   }

   protected String parseStringValue(
         String value, PlaceholderResolver placeholderResolver, Set<String> visitedPlaceholders) {

      StringBuilder result = new StringBuilder(value);
		// 寻找第一个 ${ 前缀所在的位置。
      int startIndex = value.indexOf(this.placeholderPrefix);
      while (startIndex != -1) {
          //寻找最后一个 } 所在位置，如果嵌套 { 则找到最外层匹配的 }
          //如果没有对应嵌套层数的 } 也会返回 -1
         int endIndex = findPlaceholderEndIndex(result, startIndex);
         if (endIndex != -1) {
             //将 ${ } 之间的占位符解析出来。可能包含 {aa} 这样的情况
            String placeholder = result.substring(startIndex + this.placeholderPrefix.length(), endIndex);
             //保存
            String originalPlaceholder = placeholder;
             //如果有循环引用则直接判处异常
            if (!visitedPlaceholders.add(originalPlaceholder)) {
               throw new IllegalArgumentException(
                     "Circular placeholder reference '" + originalPlaceholder + "' in property definitions");
            }
            // 递归调用，解析占位符键中包含的占位符。{a} 这样的占位符会之间原样返回。
            placeholder = parseStringValue(placeholder, placeholderResolver, visitedPlaceholders);
            // 现在获取完全解析密钥的值,从策略接口中返回 Property 的value 值。默认从Properties 中获取。
            String propVal = placeholderResolver.resolvePlaceholder(placeholder);
            if (propVal == null && this.valueSeparator != null) {
                //如果获取不到 value 但是 分割符不为空，尝试将占位符内容分割开再尝试获取
      
               int separatorIndex = placeholder.indexOf(this.valueSeparator);
               if (separatorIndex != -1) {
                   // 分隔符左边的是真正的占位符，右边就是他的默认值
                  String actualPlaceholder = placeholder.substring(0, separatorIndex);
                   //默认值，如果通过第一个分割字符串还是获取不到的话，则将 ： 后面的字符串当做 value
                  String defaultValue = placeholder.substring(separatorIndex + this.valueSeparator.length());
                   //通过 propertys 返回 value
                  propVal = placeholderResolver.resolvePlaceholder(actualPlaceholder);
                  //如果没有值则用默认值代替
                  if (propVal == null) {
                     propVal = defaultValue;
                  }
               }
            }
            if (propVal != null) {
               // 递归调用，解析先前解析的占位符值中包含的占位符。
               propVal = parseStringValue(propVal, placeholderResolver, visitedPlaceholders);
               result.replace(startIndex, endIndex + this.placeholderSuffix.length(), propVal);
               if (logger.isTraceEnabled()) {
                  logger.trace("Resolved placeholder '" + placeholder + "'");
               }
               startIndex = result.indexOf(this.placeholderPrefix, startIndex + propVal.length());
            }
            else if (this.ignoreUnresolvablePlaceholders) {
               // Proceed with unprocessed value.
               startIndex = result.indexOf(this.placeholderPrefix, endIndex + this.placeholderSuffix.length());
            }
            else {
               throw new IllegalArgumentException("Could not resolve placeholder '" +
                     placeholder + "'" + " in value \"" + value + "\"");
            }
            visitedPlaceholders.remove(originalPlaceholder);
         }
         else {
            startIndex = -1;
         }
      }

      return result.toString();
   }

    //找到 ${ 对应的 }
   private int findPlaceholderEndIndex(CharSequence buf, int startIndex) {
       
      int index = startIndex + this.placeholderPrefix.length();
       // 计数 { } 嵌套
      int withinNestedPlaceholder = 0;
      while (index < buf.length()) {
          //如果找到了一个后悔，并且此时嵌套层数为0 则返回 index
         if (StringUtils.substringMatch(buf, index, this.placeholderSuffix)) {
            if (withinNestedPlaceholder > 0) {
                //否则减少嵌套计数
               withinNestedPlaceholder--;
               index = index + this.placeholderSuffix.length();
            }
            else {
               return index;
            }
         }
          // 遇到了嵌套 { 或者 ${
         else if (StringUtils.substringMatch(buf, index, this.simplePrefix)) {
            withinNestedPlaceholder++;
            index = index + this.simplePrefix.length();
         }
         else {
            index++;
         }
      }
      return -1;
   }


   /**
    * Strategy interface used to resolve replacement values for placeholders contained in Strings.
    */
   public interface PlaceholderResolver {

      /**
       * Resolve the supplied placeholder name to the replacement value.
       * @param placeholderName the name of the placeholder to resolve
       * @return the replacement value, or {@code null} if no replacement is to be made
       */
      String resolvePlaceholder(String placeholderName);
   }

}
```