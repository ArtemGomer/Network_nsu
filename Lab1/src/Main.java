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
            myApplication.startWork();
        } catch (IOException ex) {
            if (myApplication != null) {
                myApplication.endWork();
            }
            System.err.println("Some errors occurred");
            ex.printStackTrace();
        }
    }
}
