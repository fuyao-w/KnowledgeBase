package innerclass;

import java.util.StringJoiner;

/**
 * @author wangfy
 * @Description 局部内部类是定义在一个方法或者一个作用域里面的类，它和成员内部类的区别在于局部内部类的访问仅限于方法内或者该作用域内。
 * @date 2019/3/2
 **/
public class LocalInnerClass {
    public Runnable createThread() {

        class myRunnable implements Runnable {
            String str = "局部内部类";
            final static int a = 0;//只允许声明常量，不允许声明静态变量

            @Override
            public void run() {
                System.out.println(str);
            }
        }
        return new myRunnable();
    }

    public static void main(String[] args) {

        StringJoiner stringJoiner = new StringJoiner(",", "[", "]");
        stringJoiner.add("wfy");
        stringJoiner.add("lyj");
        stringJoiner.add("wyj");
        System.out.println(stringJoiner.toString());


        LocalInnerClass innerClass = new LocalInnerClass();
        Thread thread = new Thread(innerClass.createThread());
        thread.start();


    }
}
