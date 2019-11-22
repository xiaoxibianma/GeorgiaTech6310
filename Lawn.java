/**
 * @Author: Tianyi Yang
 * @Description:
 * @Date:
 */

import java.io.File;
import java.util.*;

/**
 * @Author: Tianyi Yang
 * @Description:
 * @Date:
 */
public class Lawn {

    /** -------------------------------------------------------------------
     ------------------------- class variables --------------------------
     ---------------------------------------------------------------------*/

    private static Random randGenerator;

    /** ---------lawn variables-------- */
    private static final int DEFAULT_WIDTH = 20;
    private static final int DEFAULT_HEIGHT = 20;
    private Integer lawnWidth;
    private Integer lawnHeight;
    private Integer[][] lawnInfo;
    private Integer numberOfMowers;

    /** ---------movers-------- */
    //Integer array which stored Mover instances
    private Mover[] movers;

    //used for non-random strategy, record movers orientation of each move
    private List<List<Integer>> orientations;

    private HashMap<String, Integer[]> direction_Map;

    private String trackAction; //next step of mover
    private String trackNewDirection;
    private String trackMoveCheck;

    // String like "Grass, Grass, mover, ..." after scan
    private String trackScanResults;

    private Integer turnLimit;

    //initial Grass Number
    private Integer initialGrass;

    private final int EMPTY_CODE = 0;
    private final int GRASS_CODE = 1;
    private final int CRATER_CODE = 2;

    private final int MAX_MOWERS = 2;
    private final int OK_CODE = 1;
    private final int CRASH_CODE = -1;

    private final String[] ORIENT_LIST = {"north", "northeast", "east", "southeast", "south", "southwest", "west", "northwest"};


    /** -------------------------------------------------------------------
     ------------------------- Constructor --------------------------
     ---------------------------------------------------------------------*/
    public Lawn() {
        randGenerator = new Random();

        movers = new Mover[MAX_MOWERS];

        lawnHeight = 0;
        lawnWidth = 0;
        lawnInfo = new Integer[DEFAULT_WIDTH][DEFAULT_HEIGHT];
        numberOfMowers = -1;
        initialGrass = -1;
        orientations = new ArrayList<>();

        direction_Map = new HashMap<>();
        direction_Map.put("north", new Integer[]{0, 1});
        direction_Map.put("northeast", new Integer[]{1, 1});
        direction_Map.put("east", new Integer[]{1, 0});
        direction_Map.put("southeast", new Integer[]{1, -1});
        direction_Map.put("south", new Integer[]{0, -1});
        direction_Map.put("southwest", new Integer[]{-1, -1});
        direction_Map.put("west", new Integer[]{-1, 0});
        direction_Map.put("northwest", new Integer[]{-1, 1});

        turnLimit = -1;
    }


    /** -------------------------------------------------------------------
     ----------------------- Initialize Lawn and Mover  --------------------------
     ---------------------------------------------------------------------*/
    public void uploadStartingFile(String testFileName) {
        final String DELIMITER = ",";

        try {

            Scanner takeCommand = new Scanner(new File(testFileName));
            String[] tokens;
            int i, j, k;

            // read in the lawn information
            tokens = takeCommand.nextLine().split(DELIMITER);
            lawnWidth = Integer.parseInt(tokens[0]);
            tokens = takeCommand.nextLine().split(DELIMITER);
            lawnHeight = Integer.parseInt(tokens[0]);

            // generate the lawn information
            lawnInfo = new Integer[lawnWidth][lawnHeight];
            for (i = 0; i < lawnWidth; i++) {
                for (j = 0; j < lawnHeight; j++) {
                    lawnInfo[i][j] = GRASS_CODE;
                }
            }

            // set up teh initial number of movers
            tokens = takeCommand.nextLine().split(DELIMITER);
            numberOfMowers = Integer.parseInt(tokens[0]);

            for (k = 0; k < numberOfMowers; k++) {
                tokens = takeCommand.nextLine().split(DELIMITER);

                // set up mover's initial position
                movers[k] = new Mover(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]));

                // set up mover's initial orientation
                movers[k].curDirection = tokens[2];

                // set up movers strategy either random or personal strategy
                movers[k].moveStrategy = Integer.parseInt(tokens[3]);

                // set up mover's initial condition either ok or crash
                movers[k].moveStatue = OK_CODE;

                // set up mover's initial last action, set it to "pass"
                movers[k].lastAction = "pass";

                orientations.add(new ArrayList<>());

                // set up the grass at the initial location to be Empty
                int x = movers[k].xPosition;
                int y = movers[k].yPosition;
                lawnInfo[x][y] = EMPTY_CODE;
            }

            // set up the crater information
            tokens = takeCommand.nextLine().split(DELIMITER);
            int numCraters = Integer.parseInt(tokens[0]);

            for (k = 0; k < numCraters; k++) {
                tokens = takeCommand.nextLine().split(DELIMITER);

                // place a crater at the given location
                lawnInfo[Integer.parseInt(tokens[0])][Integer.parseInt(tokens[1])] = CRATER_CODE;
            }

            //count the amount of initial grass
            initialGrass = lawnHeight * lawnWidth;
            initialGrass -= numCraters;

            // set up the Max turn
            tokens = takeCommand.nextLine().split(DELIMITER);
            turnLimit = Integer.parseInt(tokens[0]);

            takeCommand.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println();
        }
    }


    /**
     * check the mover's strategy and make the action
     * @param id
     */
    public void go(int id) {

        // check mover's strategy
        Integer randomStrategy = movers[id].moveStrategy;

        if (randomStrategy == 0) {  //random strategy
            doRandomStrategy(id);
        } else {                    //non-random strategy
            doNonRandomStrategy(id);
        }
    }


    /**
     * it's random strategy, make a sequences of actions
     * @param: mover id
     */
    public void doRandomStrategy(int id) {
        pollMowerForAction(id);
        validateMowerAction(id);
        displayActionAndResponses(id);

    }


    /**
     * it's Non - random strategy, make a sequences of actions
     * @param: mover id
     */
    public void doNonRandomStrategy(int id) {

        // decide the action and update lastAction
        decideAction(id);

        //validate the action
        validate(id);

        displayActionAndResponses(id);

    }


    /**
     * decide the next action of that non-random strategy mover
     * @param id
     */
    public void decideAction(int id) {
        // the action we did in last turn
        Mover mover = movers[id];
        String action = mover.decideAction();
        if (action == null) {
            movers[id].curDirection = trackNewDirection;
            trackMoveCheck = "ok";
            return;
        }
        trackAction = action;
    }


    /**
     * So far, we already decide the next step of action, in this step, we are going to decide if the
     * action is valid
     * @param id
     */
    public void validate(int id) {

        Mover mv = movers[id];

        if (trackAction.equals("scan")) {
            //get around information
            trackScanResults = scanAroundSquare(mv.xPosition, mv.yPosition);

            //update information to that mover
            mv.lastScan = trackScanResults;
            trackMoveCheck = "ok";

        } else if (trackAction.equals("steer")) {

            // if next action is "steer", we are going to decide which orientation we are going to turn
            decideOrientation(id);

            //update movers's orientation
            mv.curDirection = trackNewDirection;
            trackMoveCheck = "ok";

        } else if (trackAction.equals("pass")) {
            trackMoveCheck = "ok";

        } else if (trackAction.equals("move")) {
            move(id);

        } else {
            mv.moveStatue = CRASH_CODE;
            trackMoveCheck = "crash";
        }
    }


    /**
     * So far, we make sure that the next action is "steer", so we are going to set a orientation which
     * trying to help our mover reduce the ability of crashing
     * @param id
     */
    public void decideOrientation(int id) {

        // get last round scan result
        String around = movers[id].lastScan;

        String[] list = around.split(",");
        boolean findGrass = false;

        for (int i = 0; i < list.length; i++) {
            if (list[i].equals("grass")) {
                trackNewDirection = ORIENT_LIST[i];
                findGrass = true;
                orientations.get(id).add(i); //record this orientation
                break;
            }
        }

        // if not find grass, backtracking to last step
        if (!findGrass && orientations.get(id).size() != 0) {
            // orientation list for moverX
            List<Integer> orientationList = orientations.get(id);

            //get last orientation that mover passed
            Integer lastOri = orientationList.get(orientationList.size() - 1);

            //remove that
            orientationList.remove(orientationList.size() - 1);

            // back to last position
            trackNewDirection = ORIENT_LIST[(lastOri + 4) % 8];

        } else if (!findGrass && orientations.get(id).size() == 0){ // no grass around && no past orientation passed
            for (int i = list.length - 1; i >= 0; i--) {
                if (list[i].equals("empty")) {
                    trackNewDirection = ORIENT_LIST[i];
                }
            }
        }
    }



    /**
     * Random decide the next step of that specific mover
     * @param id
     */
    public void pollMowerForAction(int id) {
        int moveRandomChoice;

        moveRandomChoice = randGenerator.nextInt(100);

        if (moveRandomChoice < 10) {
            // do nothing
            trackAction = "pass";
        } else if (moveRandomChoice < 35) {
            // check your surroundings
            trackAction = "scan";
        } else if (moveRandomChoice < 60) {
            // change direction
            trackAction = "steer";
        } else {
            // move forward
            trackAction = "move";
        }

        // determine a new direction
        moveRandomChoice = randGenerator.nextInt(100);
        if (trackAction.equals("steer") && moveRandomChoice < 85) {
            int ptr = 0;
            while(ptr < ORIENT_LIST.length && !movers[id].curDirection.equals(ORIENT_LIST[ptr])) {
                ptr++;
            }
            trackNewDirection = ORIENT_LIST[(ptr + 1) % ORIENT_LIST.length];
        } else {
            trackNewDirection = movers[id].curDirection;
        }
    }


    /**
     * validate the trackAction and update the trackMoveCheck and Lawn
     * @param id
     */
    public void validateMowerAction(int id) {

        if (trackAction.equals("scan")) {
            // in the case of a scan, return the information for the eight surrounding squares
            // always use a northbound orientation
            trackScanResults = scanAroundSquare(movers[id].xPosition, movers[id].yPosition);
            trackMoveCheck = "ok";

        } else if (trackAction.equals("pass")) {
            trackMoveCheck = "ok";

        } else if (trackAction.equals("steer")) {
            movers[id].curDirection = trackNewDirection;
            trackMoveCheck = "ok";

        } else if (trackAction.equals("move")) {
            move(id);
        } else {
            movers[id].moveStatue = CRASH_CODE;
            trackMoveCheck = "crash";
        }
    }


    /**
     *  move function(cound be used int Both random and non-random strategy)
     * @param id
     */
    public void move(int id) {
        int xOrientation, yOrientation;
        int x = movers[id].xPosition; // current mover x position
        int y = movers[id].yPosition; // current mover y position

        // in the case of a move, ensure that the move doesn't cross craters or fences
        String orien = movers[id].curDirection;
        xOrientation = direction_Map.get(orien)[0];
        yOrientation = direction_Map.get(orien)[1];

        int newSquareX = x + xOrientation;
        int newSquareY = y + yOrientation;

        // hit fence
        if (newSquareX < 0 || newSquareX >= lawnWidth || newSquareY < 0 || newSquareY >= lawnHeight) {
            // mower hit a fence
            movers[id].moveStatue = CRASH_CODE;
            trackMoveCheck = "crash";

            /** --Important -- */
            // remove mover
            lawnInfo[x][y] = EMPTY_CODE;
        }

        //  hit crater
        else if (lawnInfo[newSquareX][newSquareY] == CRATER_CODE) {
            movers[id].moveStatue = CRASH_CODE;
            trackMoveCheck = "crash";

            /** --Important -- */
            // remove mover
            lawnInfo[x][y] = EMPTY_CODE;
        }

        // mower collided with the other mower
        else if (curValidMover() == 2 && newSquareX == movers[1 - id].xPosition && newSquareY == movers[1 - id].yPosition) {
            movers[id].moveStatue = CRASH_CODE;
            movers[1 - id].moveStatue = CRASH_CODE;
            trackMoveCheck = "crash";

            // remove Both mover
            lawnInfo[x][y] = EMPTY_CODE;
            lawnInfo[movers[1 - id].xPosition][movers[1 - id].yPosition] = EMPTY_CODE;

        }

        // mover move is successful
        else {
            movers[id].xPosition = newSquareX;
            movers[id].yPosition = newSquareY;

            // update lawn status
            lawnInfo[newSquareX][newSquareY] = EMPTY_CODE;
            trackMoveCheck = "ok";
        }

    }


    /**
     *
     * @param : X index of Specific mover index
     * @param: Y index of Specific mover index
     * @return: String contains objects in 8 directions such as "mower, grass, grass, ...."
     */
    public String scanAroundSquare(int targetX, int targetY) {
        String nextSquare, resultString = "";

        for (int k = 0; k < ORIENT_LIST.length; k++) {
            String lookThisWay = ORIENT_LIST[k];
            int offsetX = direction_Map.get(lookThisWay)[0];
            int offsetY = direction_Map.get(lookThisWay)[1];

            int checkX = targetX + offsetX;
            int checkY = targetY + offsetY;

            if (checkX < 0 || checkX >= lawnWidth || checkY < 0 || checkY >= lawnHeight) {
                nextSquare = "fence";
            } else if (movers[0] != null && movers[0].moveStatue == OK_CODE && checkX == movers[0].xPosition && checkY == movers[0].yPosition) {
                nextSquare = "mower";
            } else if (movers[1] != null && movers[1].moveStatue == OK_CODE && checkX == movers[1].xPosition && checkY == movers[1].yPosition) {
                nextSquare = "mower";
            } else {
                switch (lawnInfo[checkX][checkY]) {
                    case EMPTY_CODE:
                        nextSquare = "empty";
                        break;
                    case GRASS_CODE:
                        nextSquare = "grass";
                        break;
                    case CRATER_CODE:
                        nextSquare = "crater";
                        break;
                    default:
                        nextSquare = "unknown";
                        break;
                }
            }

            if (resultString.isEmpty()) { resultString = nextSquare; }
            else { resultString = resultString + "," + nextSquare; }
        }

        return resultString;
    }


    /**
     * check current valid mover (not crashed)
     * @return
     */
    public int curValidMover() {
        int res = 0;
        for (Mover mv : movers) {
            if (mv != null && mv.moveStatue == OK_CODE ) {
                res++;
            }
        }
        return res;
    }


    /**
     *
     * @param id
     */
    public void displayActionAndResponses(int id) {
        // display the mower's actions
        System.out.print("m" + String.valueOf(id) + "," + trackAction);
        if (trackAction.equals("steer")) {
            System.out.println("," + trackNewDirection);
        } else {
            System.out.println();
        }

        // display the simulation checks and/or responses
        if (trackAction.equals("move") || trackAction.equals("steer") || trackAction.equals("pass")) {
            System.out.println(trackMoveCheck);
        } else if (trackAction.equals("scan")) {
            System.out.println(trackScanResults);
        } else {
            System.out.println("action not recognized");
        }
    }


    /**
     * Graph helper function
     */
    private void renderHorizontalBar(int size) {
        System.out.print(" ");
        for (int k = 0; k < size; k++) {
            System.out.print("-");
        }
        System.out.println("");
    }


    /**
     * Graph helper function
     */
    public void renderLawn() {
        int i, j;
        int charWidth = 2 * lawnWidth + 2;

        // display the rows of the lawn from top to bottom
        for (j = lawnHeight - 1; j >= 0; j--) {
            renderHorizontalBar(charWidth);

            // display the Y-direction identifier
            System.out.print(j);

            // display the contents of each square on this row
            for (i = 0; i < lawnWidth; i++) {
                System.out.print("|");

                // the mower overrides all other contents
                if (movers[0].moveStatue == OK_CODE && i == movers[0].xPosition && j == movers[0].yPosition) {
                    System.out.print("0");
                } else if (movers[1].moveStatue == OK_CODE && i == movers[1].xPosition && j == movers[1].yPosition) {
                    System.out.print("1");
                } else {
                    switch (lawnInfo[i][j]) {
                        case EMPTY_CODE:
                            System.out.print(" ");
                            break;
                        case GRASS_CODE:
                            System.out.print("g");
                            break;
                        case CRATER_CODE:
                            System.out.print("c");
                            break;
                        default:
                            break;
                    }
                }
            }
            System.out.println("|");
        }
        renderHorizontalBar(charWidth);

        // display the column X-direction identifiers
        System.out.print(" ");
        for (i = 0; i < lawnWidth; i++) {
            System.out.print(" " + i);
        }
        System.out.println("");

        // display the mower's directions
        for(int k = 0; k < MAX_MOWERS; k++) {
            if (movers[k].moveStatue == CRASH_CODE) { continue; }
            System.out.println("dir m" + String.valueOf(k) + ": " + movers[i].curDirection);
        }
        System.out.println("");
    }


    /**
     *
     * @return turn Limit
     */
    public Integer simulationDuration() {
        return turnLimit;
    }


    /**
     *
     * @return number of Movers
     */
    public Integer mowerCount() {
        return numberOfMowers;
    }


    /**
     * if all movers can not moved return true, else return false
     * @return does all movers can not removed ?
     */
    public Boolean mowersAllStopped() {
        for(int k = 0; k < numberOfMowers; k++) {
            if (movers[k].moveStatue == OK_CODE) { return Boolean.FALSE; }
        }
        return Boolean.TRUE;
    }


    /**
     *  to see if that specific mover is crashed
     * @param id
     * @return boolean
     */

    public Boolean mowerStopped(int id) {
        return movers[id].moveStatue == CRASH_CODE;
    }

    /**
     *
     * @return the initial total amount of grass
     */

    public int initialGrass () {
        return initialGrass;
    }


    /**
     *
     * @return the total amount has been cut
     */

    public int grassHasBeenCut() {
        int lawnSize = lawnWidth * lawnHeight;
        int numCraters = 0;
        int numGrass = 0;
        for (int i = 0; i < lawnWidth; i++) {
            for (int j = 0; j < lawnHeight; j++) {
                if (lawnInfo[i][j] == CRATER_CODE) { numCraters++; }
                if (lawnInfo[i][j] == GRASS_CODE) { numGrass++; }
            }
        }
        int potentialCut = lawnSize - numCraters;
        return potentialCut - numGrass;

    }


    /**
     *
     * @param completeTurns
     */
    public void finalReport(int completeTurns) {
        int lawnSize = lawnWidth * lawnHeight;
        int numCraters = 0;
        int numGrass = 0;
        for (int i = 0; i < lawnWidth; i++) {
            for (int j = 0; j < lawnHeight; j++) {
                if (lawnInfo[i][j] == CRATER_CODE) { numCraters++; }
                if (lawnInfo[i][j] == GRASS_CODE) { numGrass++; }
            }
        }
        int potentialCut = lawnSize - numCraters;
        int actualCut = potentialCut - numGrass;
        System.out.println(String.valueOf(lawnSize) + "," + String.valueOf(potentialCut) + "," + String.valueOf(actualCut) + "," + String.valueOf(completeTurns));
    }
}

