import java.util.Scanner;

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

    public static void read() {
        Scanner input = new Scanner(System.in);
        String data = input.nextLine();
        System.out.println(data);
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

    public void ifStatement() {
        int temperatura = 24;
        boolean condicion1 = true;
        boolean condicion2 = false;

        if (temperatura > 25) {
            System.out.println("A la playa!!!");
        }

        if (temperatura <= 25) {
            System.out.println("Esperando al buen tiempo...");
        }
        if (temperatura > 25) {
            System.out.println("A la playa!!!");
        } else {
            System.out.println("Esperando al buen tiempo...");
        }

        if (condicion1) {
            System.out.println("A");
        } else if (condicion2) {
            System.out.println("B");
        } else if (condicion2) {
            if (temperatura > 25) {
                System.out.println("A la playa!!!");
            } else {
                System.out.println("Esperando al buen tiempo...");
            }
        } else {
            System.out.println("D");
        }
    }

    public void switchExample() {
        int day = 5;
        int day2 = 2;
        String dayString;
        String dayString2;
        switch (day) {
            case 1:
                switch (day) {
                    case 1:
                        dayString = "Lunes";
                        break;
                    case 2:
                        dayString = "2";
                        break;
                    default:
                        dayString = "3";
                        break;
                }
                break;
            case 2:
                dayString = "Martes";
                break;
            case 3:
                dayString = "Miercoles";
                break;
            case 4:
                dayString = "Jueves";
                break;
            case 5:
                dayString = "Viernes";
                break;
            case 6:
                dayString = "Sabado";
                break;
            case 7:
                dayString = "Domingo";
                break;
            default:
                dayString = "Dia inválido";
                break;
        }
        switch (day2) {
            case 1:
                dayString2 = "1";
                break;
            case 2:
                dayString2 = "2";
                break;
            default:
                dayString2 = "3";
                break;
        }
        System.out.println(dayString);
        System.out.println(dayString2);
    }

    public void whileLoop() {
        System.out.println("Test para 'while':");
        int i = 2;
        while (i < 10) {
            System.out.println(i);
            forLoop();
            i = i + 1;
        }
    }

    public void doWhileLoop() {
        System.out.println("Test para 'doWhile':");
        int i = 1;
        do {
            System.out.println("iteracion: " + i);
            i = i + 1;
        } while(i <= 3);
    }

    public void forLoop() {
        for (int i = 2; i < 500 && i%1==0; i = i*i) {
            System.out.println(i/2);
        }

        int j = 12;
        for (j = 2; !(j > 30); j = j*j) {
            System.out.println("El nuevo valor es: " + j);
        }

        int k = 2;
        for (; k < 10; k = k*k) {}
    }

    public static void main(String[] args) {
        String demo = "Hello: " + 1 + ", World: " + 2;
        System.out.println(demo);
        JavaSourceSample j = new JavaSourceSample();
        j.show(3, "algo", args);
        System.out.println("------------------------------");
        j.forLoop();
        System.out.println("..............................");
        j.instanceFun();
        System.out.println("------------------------------");
        j.whileLoop();
        System.out.println("..............................");
        staticFun();
        System.out.println("------------------------------");
        j.switchExample();
        System.out.println("..............................");
        j.ifStatement();
        System.out.println("------------------------------");
        j.doWhileLoop();
        System.out.println("..............................");
        read();
    }
}