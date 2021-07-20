public class JavaSourceSample {
    // TODO: manejar atributos de clase

    public static void main(String[] args) {
        System.out.println("Hello world!");
        JavaSourceSample j = new JavaSourceSample();
        j.show(3, "algo", args);
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

    /*
    public void forLoop() {
        for (int i = 0; i < 100; i = i*i) {
            System.out.println(i/2);
        }
    }
    */
}