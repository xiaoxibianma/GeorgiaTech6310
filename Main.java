/**
 * @Author: Tianyi Yang
 * @Description:
 * @Date:
 */
public class Main {
    public static void main(String[] args) {
        Lawn lawn = new Lawn();
        int trackTurnsCompleted = 0;
        Boolean showState = Boolean.FALSE;

        // check for the test scenario file name

         if (args.length < 1) {
            System.out.println("ERROR: Test scenario file name not found.");
            return;
         }

         if (args.length >= 2 && (args[1].equals("-v") || args[1].equals("-verbose")))
         { showState = Boolean.TRUE; }

        // initial Lawn
        //monitorSim.uploadStartingFile(args[0]);
        lawn.uploadStartingFile(args[0]);


        // run the simulation for a fixed number of steps
        for(int turns = 0; turns < lawn.simulationDuration(); turns++) {
            trackTurnsCompleted = turns;

            // all movers crashed -> break -> final report
            if (lawn.mowersAllStopped()) {
                break;
            }

            // all grasses had been cut
            if (lawn.initialGrass() == lawn.grassHasBeenCut()) {
                break;
            }

            // not satisfy pause condition, let's keep going
            for (int k = 0; k < lawn.mowerCount(); k++) {

                if (lawn.mowerStopped(k)) { continue; }

                // start action
                lawn.go(k);

                // render the state of the lawn after each command
                if (showState) { lawn.renderLawn(); }
            }
        }

        lawn.finalReport(trackTurnsCompleted);
    }
}
