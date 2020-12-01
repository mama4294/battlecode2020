package maloneplayer;

import battlecode.common.*;

public class Robot {
    RobotController rc;
    Communications comms;
    boolean debug = true;
    int RefineryCount = 0;
    int NetGunCount = 0;
    int MinerCount = 0;
    int DesignSchoolCount = 0;
    int closeNetguns = 0;
    int VaporatorCount = 0;
    int FulfillmentCenterCount = 0;
    int nearbyFriendlyLandscapers = 0;

    int turnCount = 0;


    public Robot(RobotController r) {
        this.rc = r;
        comms = new Communications(rc);
    }

    public void takeTurn() throws GameActionException {
        turnCount += 1;
    }

    boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        }
        return false;
    }

}