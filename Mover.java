/**
 * @Author: Tianyi Yang
 * @Description:
 * @Date:
 */
public class Mover {

    String curDirection;  // the orientation that mover is facing
    Integer xPosition;    // mover's x position
    Integer yPosition;    // mover's y position
    Integer moveStatue;
    Integer moveStrategy; // 0 : random , 1 : personal strategy
    String lastScan;      // stored the scan information
    String lastAction;    // last action

    public Mover(Integer xPosition, Integer yPosition) {
        this.xPosition = xPosition;
        this.yPosition = yPosition;

        moveStatue = 1;
        curDirection = "north";
    }

    public String decideAction() {
        if (lastAction.equals("pass") || lastAction.equals("move")) {
            lastAction = "scan";
            return "scan";
        } else if (lastAction.equals("steer")) {
            lastAction = "move";
            return "move";
        } else if (lastAction.equals("scan")) {
            lastAction = "steer";
            return "steer";
        } return null;
    }
}
