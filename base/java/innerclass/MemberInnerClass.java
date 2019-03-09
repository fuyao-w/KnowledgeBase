package innerclass;

/**
 * @author wangfy
 * @Description 成员内部类，成员内部类中可以获取外部类的所有方法和变量(包括static)
 * 外部类获取内部类的成员需要创建内部类的实例。内部类不能有static 成员（final staic 可以）
 * 当成员内部类与外部类的成员签名相同时，默认获取内部类成员。获取外部类同签名成员需要调用
 * 外部类.this.成员 来获取。
 * 内部类可以使用任何包访问权限。
 * @date 2019/3/2
 **/
public class MemberInnerClass {
    private String outterStr = "外部类";
    private static int outterInt = 1;

    public void repeat() {
        System.out.println("外部类重复方法");
    }

    public void outter() {

        inner inner = new inner();
        System.out.println("外部类方法");
        inner.inners();
    }

    private class inner {
        String innerStr = "内部类";
        private int innerInt = 2;
        final static int s = 0; //只允许声明常量

        public void inners() {
            System.out.println("从内部类获取外部类字段:" + outterStr);
        }

        public void repeat() {

            System.out.println("内部类repeat 方法");
            MemberInnerClass.this.repeat();
        }
    }

    public static void main(String[] args) {
        MemberInnerClass memberInnerClass = new MemberInnerClass();
        memberInnerClass.ssss();

    }

    public void ssss() {
        inner inner = new MemberInnerClass.inner();
        inner.repeat();
    }

}
