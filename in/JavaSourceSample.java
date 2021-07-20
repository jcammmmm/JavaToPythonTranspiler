public class JavaSourceSample {

    public static String crossVariable;
    public int lazyIdentity = 0;
    public static double obsFactor = 9;

    public void instanceFun() {
        System.out.println(lazyIdentity);
    }

    public static void staticFun() {
        System.out.println(obsFactor);
    }

    public String show(Integer j, String smthg, Object o) {
        Integer x;
        Double d = 4.1, b = 10.1;
        String s = "cadena";
        double n = 9292.2;
        System.out.println(d);
        System.out.println(s);
        System.out.println(n);
        return "";
    }

    public void forLoop() {
        for (int i = 2; i < 500 && i%1==0; i = i*i) {
            System.out.println(i/2);
        }

        int j = 12;
        for (j = 2; !(j > 30); j = j*j) {
            System.out.println(j);
        }

        int k = 2;
        for (; k < 10; k = k*k) {}
    }

    public static void main(String[] args) {
        System.out.println("Hello world!");

        JavaSourceSample j = new JavaSourceSample();
        j.show(3, "algo", args);
        j.forLoop();
        j.instanceFun();

        staticFun();
    }
}