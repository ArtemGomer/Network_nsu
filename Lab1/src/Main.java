import myApplication.MyApplication;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Bad format of input");
            return;
        }
        try (MyApplication myApplication = new MyApplication(args[0])) {
            myApplication.startWork();
        } catch (IOException ex) {
            System.err.println("Can not create application!");
            ex.printStackTrace();
        }
    }
}
