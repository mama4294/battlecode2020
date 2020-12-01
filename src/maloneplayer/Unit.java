package maloneplayer;

import battlecode.common.*;

public class Unit extends Robot {

    Navigation nav;
    MapLocation targetLoc;
    MapLocation hqLoc;
    MapLocation HQLocation;


    public Unit(RobotController r) {
        super(r);
        nav = new Navigation(rc);
    }


    public void takeTurn() throws GameActionException {
        super.takeTurn();

        findHQ();
    }

    public void findHQ() throws GameActionException {
        if (hqLoc == null) {
            // search surroundings for HQ
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.type == RobotType.HQ && robot.team == rc.getTeam()) {
                    hqLoc = robot.location;
                    HQLocation = hqLoc;
                }
            }
            if (hqLoc == null) {
                // if still null, search the blockchain
                hqLoc = comms.getHqLocFromBlockchain();
                HQLocation = hqLoc;
            }
        }
    }


}