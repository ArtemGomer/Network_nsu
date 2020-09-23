import myApplication.MyApplication;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Bad format of input");
            return;
        }
        MyApplication myApplication = null;
        try {
            myApplication = new MyApplication(args[0]);
            System.out.println("Type 'end' to stop app!");
        } catch (IOException ex) {
            System.err.println("Can not create myApplication");
            ex.printStackTrace();
            System.exit(1);
        }
        try {
            myApplication.startWork();
        } catch (IOException ex) {
            System.err.println("Some errors occurred");
            ex.printStackTrace();
        } finally {
            myApplication.endWork();
        }
    }
}
