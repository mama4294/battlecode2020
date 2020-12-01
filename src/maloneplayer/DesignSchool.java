package maloneplayer;

import battlecode.common.*;

public class DesignSchool extends Building {
    int countLandscapers = 0;

    public DesignSchool(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();

        // will only actually happen if we haven't already broadcasted the creation
        comms.broadcastDesignSchoolCreation(rc.getLocation());

        if (rc.getTeamSoup() >= (RobotType.DESIGN_SCHOOL.cost + 100) && countLandscapers < 9)
            for (Direction dir : Util.directions) {
                if (tryBuild(RobotType.LANDSCAPER, dir)) {
                    System.out.println("made a landscaper");
                    countLandscapers++;

                }
            }
    }
}
