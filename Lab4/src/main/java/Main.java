
import Control.GameNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Main {

    private final static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Create GameNode.");
        GameNode node = new GameNode();
        logger.info("Start main menu in GameNode.");
        node.toMainMenu();
    }
}
