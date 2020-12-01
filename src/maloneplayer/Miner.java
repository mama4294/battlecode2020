package maloneplayer;

import battlecode.common.*;

import java.util.ArrayList;

public class Miner extends Unit {
    static final int SCOUT = 0; // default to search for patches of soup and what not
    static final int MINER = 1; // default to go and mine nearest souplocation it knows
    static final int RETURNING = 2; // Return to refinery or HQ
    static final int BUILDING = 3;
    static final int ATTACK = 4;

    static int role = MINER; // default ROLE
    static RobotType unitToBuild; // unit to build if role is building
    static final int[][] BFSDeltas35 = {{0, 0}, {1, 0}, {0, -1}, {-1, 0}, {0, 1}, {2, 0}, {1, -1}, {0, -2}, {-1, -1}, {-2, 0}, {-1, 1}, {0, 2}, {1, 1}, {3, 0}, {2, -1}, {1, -2}, {0, -3}, {-1, -2}, {-2, -1}, {-3, 0}, {-2, 1}, {-1, 2}, {0, 3}, {1, 2}, {2, 1}, {4, 0}, {3, -1}, {2, -2}, {1, -3}, {0, -4}, {-1, -3}, {-2, -2}, {-3, -1}, {-4, 0}, {-3, 1}, {-2, 2}, {-1, 3}, {0, 4}, {1, 3}, {2, 2}, {3, 1}, {5, 0}, {4, -1}, {3, -2}, {2, -3}, {1, -4}, {0, -5}, {-1, -4}, {-2, -3}, {-3, -2}, {-4, -1}, {-5, 0}, {-4, 1}, {-3, 2}, {-2, 3}, {-1, 4}, {0, 5}, {1, 4}, {2, 3}, {3, 2}, {4, 1}, {5, -1}, {4, -2}, {3, -3}, {2, -4}, {1, -5}, {-1, -5}, {-2, -4}, {-3, -3}, {-4, -2}, {-5, -1}, {-5, 1}, {-4, 2}, {-3, 3}, {-2, 4}, {-1, 5}, {1, 5}, {2, 4}, {3, 3}, {4, 2}, {5, 1}, {5, -2}, {4, -3}, {3, -4}, {2, -5}, {-2, -5}, {-3, -4}, {-4, -3}, {-5, -2}, {-5, 2}, {-4, 3}, {-3, 4}, {-2, 5}, {2, 5}, {3, 4}, {4, 3}, {5, 2}, {5, -3}, {4, -4}, {3, -5}, {-3, -5}, {-4, -4}, {-5, -3}, {-5, 3}, {-4, 4}, {-3, 5}, {3, 5}, {4, 4}, {5, 3}};

    int RefineryCount = 0;
    int NetGunCount = 0;
    int MinerCount = 0;
    int DesignSchoolCount = 0;
    int closeNetguns = 0;
    int VaporatorCount = 0;
    int FulfillmentCenterCount = 0;
    int nearbyFriendlyLandscapers = 0;


    Direction minedDirection;
    Direction buildDir = Direction.NORTH;
    MapLocation lastDepositedRefinery;
    MapLocation targetSoupLoc;
    MapLocation depositLoc;

    int numDesignSchools = 0;
    ArrayList<MapLocation> soupLocations = new ArrayList<MapLocation>();
    boolean debug = true;
    boolean proceedWithBuild = true;
    boolean proceed = true;


    public Miner(RobotController r) {
        super(r);
    }

    public void takeTurn() throws GameActionException {
        super.takeTurn();
        findFriendlies();
        boolean mined = false;

        numDesignSchools += comms.getNewDesignSchoolCount();
        comms.updateSoupLocations(soupLocations);


        //Check if Soup is missing
        if (soupLocations.size() > 0) {
            MapLocation targetSoupLoc = soupLocations.get(0);
            if (rc.canSenseLocation(targetSoupLoc) && rc.senseSoup(targetSoupLoc) == 0) {
                if (targetLoc == soupLocations.get(0)) {
                    targetLoc = null;
                }
                soupLocations.remove(0);
            }
        }

        //Search for soup
        int soupNearbyCount = 0; // amount of soup nearby in BFS search range
        for (int i = 0; i < BFSDeltas35.length; i++) {
            int[] deltas = BFSDeltas35[i];
            MapLocation checkLoc = rc.getLocation().translate(deltas[0], deltas[1]);
            if (rc.canSenseLocation(checkLoc)) {
                if (rc.senseSoup(checkLoc) > 0) {
                    soupNearbyCount += rc.senseSoup(checkLoc);
                }
            }
        }
        if (debug) System.out.println(soupNearbyCount + " Soup Nearby and " + RefineryCount + " Refineries Nearby");

        if (RefineryCount == 0 && rc.getTeamSoup() >= RobotType.REFINERY.cost && soupNearbyCount > 700) {
            role = BUILDING;
            unitToBuild = RobotType.REFINERY;
        }

        if (rc.getTeamSoup() >= RobotType.VAPORATOR.cost + 200) {
            role = BUILDING;
            unitToBuild = RobotType.VAPORATOR;
        }


        if (role == MINER) {
            // Strategy: MINE if possible!

            // try to mine if mining max rate one turn won't go over soup limit (waste of mining power)
            if (rc.getSoupCarrying() <= RobotType.MINER.soupLimit - GameConstants.SOUP_MINING_RATE) {
                for (Direction dir : Util.directions) {
                    // for each direction, check if there is soup in that direction
                    MapLocation newLoc = rc.adjacentLocation(dir);
                    if (rc.canMineSoup(dir)) {

                        //Mine Soup
                        rc.mineSoup(dir);
                        minedDirection = dir;
                        //mined = true;
                        if (debug) System.out.println("I mined soup! " + rc.getSoupCarrying());

                        //Broadcast Soup Location
                        MapLocation soupLoc = rc.getLocation().add(dir);
                        if (!soupLocations.contains(soupLoc)) {
                            comms.broadcastSoupLocation(soupLoc);
                        }
                        if (debug) {
                            System.out.println("Turn: " + turnCount + " - I mined " + newLoc + "; Now have " + rc.getSoupCarrying());
                        }
                        break;
                    }
                }
            }
            // else if we are near full, we go to nearest refinery known, otherwise go to HQ
            else {

                nav.setTargetLoc(HQLocation); //Set to something else
                role = RETURNING;
            }

            if (numDesignSchools == 0 && rc.getTeamSoup() >= RobotType.DESIGN_SCHOOL.cost) {
                role = BUILDING;
                unitToBuild = RobotType.DESIGN_SCHOOL;
            }


            //Travel to soup locations
            if (soupLocations.size() > 0) {
                targetLoc = soupLocations.get(0);
                if (debug) System.out.println("moving towards soup");
                //Else move randomly
            }


        } else if (role == RETURNING) {

            //Find deposit location
            if (lastDepositedRefinery != null) {
                depositLoc = lastDepositedRefinery;
            } else {
                depositLoc = HQLocation;
            }

            RobotInfo[] nearbyFriendlyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
            for (int i = nearbyFriendlyRobots.length; --i >= 0; ) {
                RobotInfo info = nearbyFriendlyRobots[i];
                if (info.type == RobotType.REFINERY) {
                    depositLoc = info.location;
                }
            }


            // targetLoc should be place miner tries to return to
            targetLoc = depositLoc;
            if (rc.getLocation().isAdjacentTo(depositLoc)) {
                // else we are there, deposit and start mining again
                Direction depositDir = rc.getLocation().directionTo(depositLoc);
                if (rc.canDepositSoup(depositDir)) {
                    rc.depositSoup(depositDir, rc.getSoupCarrying());
                    if (debug) System.out.println("Deposited soup to " + depositLoc);
                    lastDepositedRefinery = targetLoc; // update to targetLoc so we don't accidentally set HQ as a possible deposit loc

                    // reset roles
                    role = MINER;
                    targetLoc = null;

                }
            }
        } else if (role == BUILDING) {
            proceedWithBuild = true;

            // if we are trying to build but we already have one, stop, or if we already built it cuz soup went down, STOP
            if (debug) System.out.println("Trying to build " + unitToBuild + " | soup rn: " + rc.getTeamSoup());

            if (unitToBuild == RobotType.REFINERY && RefineryCount > 0) {
                role = MINER;
                proceedWithBuild = false;
            }
            if (unitToBuild == RobotType.DESIGN_SCHOOL && DesignSchoolCount > 0) {
                role = MINER;
                proceedWithBuild = false;
            }
            if (unitToBuild == RobotType.FULFILLMENT_CENTER && FulfillmentCenterCount > 0) {
                role = MINER;
                proceedWithBuild = false;
            }
            if (unitToBuild == RobotType.VAPORATOR && rc.getTeamSoup() < 450) {
                role = MINER;
                proceedWithBuild = false;
            }


            //Actually build the unit
            if (proceedWithBuild) {
                // if building a building, only build on odd x odd
                for (int i = 9; --i >= 1; ) {
                    MapLocation buildLoc = rc.adjacentLocation(buildDir);
                    // same parity and must not be too close

                    // if school or FC, just build asap, otherwise build on grid, not dig locations, and can't be next to flood, if next to flood, height must be 12
                    if (rc.onTheMap(buildLoc)) {
                        if (debug) System.out.println("Checkign build dir " + buildDir);
                        if ((unitToBuild == RobotType.REFINERY || unitToBuild == RobotType.DESIGN_SCHOOL || unitToBuild == RobotType.FULFILLMENT_CENTER ||
                                (((buildLoc.x % 2 != HQLocation.x % 2 && buildLoc.y % 2 != HQLocation.y % 2) || buildLoc.distanceSquaredTo(HQLocation) == 5)
                                        && (rc.senseElevation(buildLoc) >= 5)))) {
                            proceed = true;
                        } else {
                            // has to be adjacent if early on and is school or FC
                            if (!buildLoc.isAdjacentTo(HQLocation) && (unitToBuild == RobotType.DESIGN_SCHOOL || unitToBuild == RobotType.FULFILLMENT_CENTER) && rc.getRoundNum() <= 200) {
                                proceed = false;
                            }
                        }

                        if (proceed && tryBuild(unitToBuild, buildDir)) {
                            break;
                        }

                    }
                }
                buildDir = buildDir.rotateRight();
            }
            // if we built a refinery, we also try and build a vaporator given funds

            // go back to miner role
            role = MINER;
        }


        // Move to target location
        if (targetLoc != null) {
            // don't go to enemy!
            Direction greedyDir = nav.getBugPathMove(targetLoc);
            if (debug) {
                rc.setIndicatorLine(rc.getLocation(), targetLoc, 255, 255, 0);
                System.out.println("Moving to " + rc.adjacentLocation((greedyDir)) + " to get to " + targetLoc);
            }
            nav.pathfind(targetLoc);
            //nav.tryMove(greedyDir); // wasting bytecode probably here
        }

        //Move randomly
        else if (nav.goTo(Util.randomDirection())) {
            if (debug) System.out.println("I moved randomly!");

            //Error
        } else {
            // no targetLoc and is a miner, if on map edge,
            if (debug) System.out.println("I have no target");
        }

        if (debug) {
            System.out.println("Miner " + role + " - Bytecode used: " + Clock.getBytecodeNum() +
                    " | Bytecode left: " + Clock.getBytecodesLeft() +
                    " | SoupLoc Target: " + targetSoupLoc + " | targetLoc: " + targetLoc +
                    " | Cooldown: " + rc.getCooldownTurns() + " | soup: " + rc.getSoupCarrying());
        }
    }

    void findFriendlies() throws GameActionException {
        RobotInfo[] nearbyFriendlyRobots = rc.senseNearbyRobots(-1, rc.getTeam());

        for (int i = nearbyFriendlyRobots.length; --i >= 0; ) {
            RobotInfo info = nearbyFriendlyRobots[i];
            switch (info.type) {
                case REFINERY:
                    RefineryCount++;
                    break;
                case NET_GUN:
                    NetGunCount++;
                    if (rc.getLocation().distanceSquaredTo(info.location) <= 2) {
                        closeNetguns++;
                    }
                    break;
                case DESIGN_SCHOOL:
                    DesignSchoolCount++;
                    break;
                case MINER:
                    MinerCount++;
                    break;
                case FULFILLMENT_CENTER:
                    FulfillmentCenterCount++;
                    break;
                case VAPORATOR:
                    VaporatorCount++;
                    break;
                case LANDSCAPER:
                    nearbyFriendlyLandscapers++;
                    break;
            }
        }
    }
}

/*
        for (Direction dir : Util.directions)
            if (tryMine(dir)) {
                System.out.println("I mined soup! " + rc.getSoupCarrying());
                MapLocation soupLoc = rc.getLocation().add(dir);
                if (!soupLocations.contains(soupLoc)) {
                    comms.broadcastSoupLocation(soupLoc);
                }
            }
        // mine first, then when full, deposit
        for (Direction dir : Util.directions)
            if (tryRefine(dir))
                System.out.println("I refined soup! " + rc.getTeamSoup());

        if (numDesignSchools < 3){
            if(tryBuild(RobotType.DESIGN_SCHOOL, Util.randomDirection()))
                if(debug)System.out.println("created a design school");
        }

        if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
            // time to go back to the HQ
           targetLoc = hqLoc;
            if(debug)System.out.println("moving towards HQ");
        } else if (soupLocations.size() > 0) {
           targetLoc = soupLocations.get(0);
            if(debug)System.out.println("moving towards soup");
        } else if (nav.goTo(Util.randomDirection())) {
            // otherwise, move randomly as usual
            if(debug)System.out.println("I moved randomly!");
        } else {
            if(debug) System.out.println("I have nowhere to go");
        }

        if(targetLoc != null){
            Direction greedyDir = nav.getBugPathMove(targetLoc);
            rc.setIndicatorLine(rc.getLocation(), targetLoc, 255, 255, 0);
            nav.tryMove(greedyDir);
        }
    }

    boolean tryMine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMineSoup(dir)) {
            rc.mineSoup(dir);
            return true;
        } else return false;
    }


    boolean tryRefine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositSoup(dir)) {
            rc.depositSoup(dir, rc.getSoupCarrying());
            return true;
        } else return false;
    }

 */




