package innerclass;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author wangfy
 * @Description 　静态内部类也是定义在另一个类里面的类，只不过在类的前面多了一个关键字static。静态内部类是不需要依赖于外部类的，这点和类的静态成员属性有点类似，并且它不能使用外部类的非static成员变量或者方法
 * 外部类只能访问，内部类的静态属性，不能直接获取实例属性。
 * @date 2019/3/2
 **/
public class StaticInnerClass {
    static String string = "外部类";

    static void test() {
        HashMap<String, String> ha = new HashMap();
        Iterator<Map.Entry<String, String>> iterator = ha.entrySet().iterator();
        ha.entrySet().forEach(entry -> {
            System.out.println(entry.getKey());
            System.out.println(entry.getValue());
        });
        Iterator<String> iterator1 = ha.keySet().iterator();
        ha.keySet().forEach(str -> {
            System.out.println(str);
        });
        Iterator<String> iterator2 = ha.values().iterator();
        iterator2.forEachRemaining(str -> {
            System.out.println(str);
        });
        ha.values().forEach(str -> {
            System.out.println(str);
        });
        ha.forEach((str1, str2) -> {

        });

        for (Map.Entry<String, String> entry : ha.entrySet()) {

        }
        for (String string : ha.values()) {

        }

        for (String string : ha.keySet()) {

        }

        System.out.println("外部类方法");
        System.out.println();
        System.out.println(innerClass.sinnerStr);
    }

    static class innerClass {
        String innerStr = "内部类";
        static String sinnerStr = "内部类";

        public void inner() {
            System.out.println(string);
            test();
        }
    }

    public static void main(String[] args) {
        StaticInnerClass.innerClass innerClass = new StaticInnerClass.innerClass();

    }
}
