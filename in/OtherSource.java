public class OtherSource {

    public static void main(String[] args) {
        OtherSource os = new OtherSource();
        for (int i = 2; i < 500 && i%1==0; i = i*i) {
            System.out.println(i/2);
        }
    }
}