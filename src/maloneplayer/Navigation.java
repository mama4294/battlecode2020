package maloneplayer;

import battlecode.common.*;

import java.util.Map;

public class Navigation {
    RobotController rc;
    static int closestToTargetLocSoFar = 9999999;

    // usually the position to move towards
    static MapLocation targetLoc;
    static MapLocation targetLocPrev;
    static Direction lastDirMove = Direction.NORTH;
    static MapLocation lastWallChecked = null;
    // static LinkedList<MapLocation> lastLocs = new LinkedList<>();
    static int roundsSinceLastResetOfClosestTargetdist = 0;
    boolean debug = true;
    boolean freepathing = true;

    // state related only to navigation should go here

    public Navigation(RobotController r) {
        rc = r;
    }

    boolean tryMove(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMove(dir) && !rc.senseFlooding(rc.getLocation().add(dir))) {
            rc.move(dir);
            return true;
        } else return false;
    }

    // tries to move in the general direction of dir
    boolean goTo(Direction dir) throws GameActionException {
        Direction[] toTry = {dir, dir.rotateLeft(), dir.rotateLeft().rotateLeft(), dir.rotateRight(), dir.rotateRight().rotateRight()};
        for (Direction d : toTry) {
            if (tryMove(d))
                return true;
        }
        return false;
    }

    // navigate towards a particular location
    boolean goTo(MapLocation destination) throws GameActionException {
        return goTo(rc.getLocation().directionTo(destination));
    }


    void setTargetLoc(MapLocation loc) {
        // if target is null or doesn't equal our new loc, set target
        if (targetLoc == null || !targetLoc.equals(loc)) {
            targetLoc = loc;
            closestToTargetLocSoFar = 99999999;
            if (targetLoc != null) {
                rc.getLocation().distanceSquaredTo(targetLoc);
            }
        }
    }

    void pathfind(MapLocation target) throws GameActionException {

        // Reset if new target location
        if (targetLocPrev != null) {
            if (target != targetLocPrev) {
                closestToTargetLocSoFar = rc.getLocation().distanceSquaredTo(target);
                freepathing = true;
            }
        }

        Direction dir = rc.getLocation().directionTo(target);

        //Go directly towards the target (or left/right)
        if (freepathing) {
            if (tryMove(dir)) {
                closestToTargetLocSoFar = rc.getLocation().distanceSquaredTo(target);
            } else if (tryMove(dir.rotateRight())) {
                closestToTargetLocSoFar = rc.getLocation().distanceSquaredTo(target);
            } else if (tryMove(dir.rotateLeft())) {
                closestToTargetLocSoFar = rc.getLocation().distanceSquaredTo(target);
            } else {
                //Stuck. Switch to wall hugging
                freepathing = false;
            }
        }

        //Hug right hand of wall
        if (!freepathing) {

            for (int i = 8; --i >= 0; ) {
                if (tryMove(dir)) {
                    if (rc.getLocation().distanceSquaredTo(target) < closestToTargetLocSoFar) {
                        freepathing = true;
                    }
                    break;
                }
                dir = dir.rotateRight();
            }
        }
        targetLocPrev = target;
    }


    //bugpath
    Direction getBugPathMove(MapLocation target) throws GameActionException {
        roundsSinceLastResetOfClosestTargetdist++;
        if (rc.getLocation().equals(target)) {
            return Direction.CENTER;
        }
        // every 20 rounds, reset the closest distance
        if (roundsSinceLastResetOfClosestTargetdist >= 20) {
            roundsSinceLastResetOfClosestTargetdist = 0;
            closestToTargetLocSoFar = 99999999;
        }

        // Reset if new target location
        if (targetLocPrev != null) {
            if (target != targetLocPrev) {
                closestToTargetLocSoFar = 99999999;
            }
        }
        targetLocPrev = target;


        //Recalucate this turn
        Direction dir = lastDirMove;
        Direction greedyDir = null;
        Direction wallDir = lastDirMove;
        if (lastWallChecked != null) {
            wallDir = rc.getLocation().directionTo(lastWallChecked);
            dir = rc.getLocation().directionTo(lastWallChecked);
        }

        boolean foundNonDangerousDir = false;
        boolean wallDirSet = false;
        boolean lastWallUpdated = false;
        int closestGreedyDist = 99999999;
        if (debug)
            System.out.println("Last dir: " + lastDirMove + " | Last Wall checked " + lastWallChecked + " | Closest Target Dist So Far " + closestToTargetLocSoFar);

        //Check direction. Rotate right. Repeat 8 times
        for (int i = 8; --i >= 0; ) {
            if (debug) System.out.println("Checking dir: " + dir);
            if (true) {  // originally ... if (!dangerousDirections.contains(dir)) {
                MapLocation greedyLoc = rc.adjacentLocation(dir);
                int greedyDist = greedyLoc.distanceSquaredTo(target);

                // if it is closer than the closest we have ever been, we can sense it as well and its not flooding
                if (greedyDist < closestGreedyDist && rc.canSenseLocation(greedyLoc) && !rc.senseFlooding(greedyLoc)) {
                    if (debug) System.out.println(dir + " is greedier " + greedyDist);
                    // if we can move there
                    if (rc.canMove(dir)) {
                        if (debug) System.out.println(dir + " can move ");
                        // update and move
                        closestGreedyDist = greedyDist;
                        greedyDir = dir;
                        foundNonDangerousDir = true;
                    }
                }
                if (!wallDirSet) {
                    if (rc.canSenseLocation(greedyLoc) && !rc.senseFlooding(greedyLoc) && rc.canMove(dir)) {
                        wallDir = dir;
                        wallDirSet = true;
                        foundNonDangerousDir = true;
                        if (!lastWallUpdated) {
                            // not updated ever?
                            lastWallChecked = null;
                        }
                        if (debug) System.out.println(dir + ": new wall near direction now set ");
                    } else {
                        lastWallChecked = greedyLoc; // last wall checked that is blocked
                        lastWallUpdated = true;
                    }
                }
            } else {
                if (debug) System.out.println("Dir " + dir + " is dangerous!");
            }
            dir = dir.rotateRight();

        }
        if (closestGreedyDist < closestToTargetLocSoFar) {
            closestToTargetLocSoFar = closestGreedyDist;
            lastDirMove = greedyDir;
            return greedyDir;
        }

        if (foundNonDangerousDir) {
            lastDirMove = wallDir;
            return wallDir;
        } else {
            return Direction.CENTER;
        }
    }


}

