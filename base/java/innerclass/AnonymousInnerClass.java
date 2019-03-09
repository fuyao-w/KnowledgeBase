package innerclass;

/**
 * @author wangfy
 * @Description 匿名内部类
 * @date 2019/3/2
 **/
public class AnonymousInnerClass {
    public static void main(String[] args) {
        int  s = 0;
        new Thread(new Runnable() { //可以使用 lambda 表达式替代
            @Override
            public void run() {
                System.out.println(s);
                System.out.println(this.getClass().getName());
                System.out.println("匿名");
            }
        }) {
            final static int anInt = 0; //只允许常量允许static

            @Override
            public void run() {
                super.run();
                System.out.println(this.getClass().getName());
                //super 调用的是 target runnable 类参数
                //    /* What will be run. */
                //    private Runnable target;
                //匿名内部类就是 target
            }
        }.start();
    }
}
