package Second_player;

//import static org.junit.Assert.*;

import battlecode.common.*;
//import org.junit.Test;

import javax.naming.directory.DirContext;
import java.awt.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.ResourceBundle;

public class RobotPlayer {
    private enum MessageType {
        SAVE_CHIPS
    }
    static int turnCount = 0;
    static final Random rng = new Random(6147);
    static boolean isMessanger = false;
    static boolean isSaving = false;
    static ArrayList<MapLocation> knownTowers = new ArrayList<>();
    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };
    public static void run(RobotController rc) throws GameActionException{
        if (rc.getType() == UnitType.MOPPER && rc.getID() % 2 == 0){
            isMessanger = true;
        }
        while (true){
            turnCount += 1;
            try {
                switch (rc.getType()) {
                    case SOLDIER: runSoldier(rc); break;
                    case MOPPER: runMopper(rc); break;
                    case SPLASHER: runSplasher(rc); break;
                    default: runTower(rc); break;
                }
            } catch(GameActionException e){
                System.out.println("Game Action Exception!!");
                e.printStackTrace();
            } catch(Exception e){
                System.out.println("Exception!!");
                e.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }
    public static void runTower(RobotController rc) throws GameActionException{
//        if(rc.canUpgradeTower(rc.getLocation()))
//            rc.upgradeTower(rc.getLocation());
        Direction dir = directions[rng.nextInt(directions.length)];
        MapLocation nextLoc = rc.getLocation().add(dir);
        int robotType = rng.nextInt(3);
        if(robotType == 0 && rc.canBuildRobot(UnitType.SOLDIER, nextLoc)){
            rc.buildRobot(UnitType.SOLDIER, nextLoc);
            System.out.println("Build a Soldier");
        }
        if(robotType == 1 && rc.canBuildRobot(UnitType.SPLASHER, nextLoc)){
            rc.buildRobot(UnitType.SPLASHER, nextLoc);
            System.out.println("Build a Splasher");
        }
        if(robotType == 2 && rc.canBuildRobot(UnitType.MOPPER, nextLoc)){
            rc.buildRobot(UnitType.MOPPER, nextLoc);
            System.out.println("Build a Mopper");
        }
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        for (RobotInfo robot: nearbyRobots){
            if (rc.canAttack(robot.getLocation())){
                rc.attack(robot.getLocation());
            }
        }
    }
    public static void runSoldier(RobotController rc) throws GameActionException{
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        MapInfo curRuin = null;
        int curDist = 99999;

        for (MapInfo tile: nearbyTiles){
            if (tile.hasRuin()) {
                int dist = tile. getMapLocation().distanceSquaredTo(rc.getLocation());
                if (dist < curDist) {
                    curRuin = tile;
                    curDist = dist;
                }
            }
        }
        if (curRuin != null){
            MapLocation targetLoc = curRuin.getMapLocation();
            Direction dir = rc.getLocation().directionTo(targetLoc);
            if (rc.canMove(dir))
                rc.move(dir);
            MapLocation checkMarked = targetLoc.subtract(dir);
            if (rc.senseMapInfo(checkMarked).getMark() == PaintType.EMPTY && rc.canMarkTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc)){
                rc.markTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc);
            }
            for (MapInfo patternTile : rc.senseNearbyMapInfos(targetLoc, 8)){
                if(patternTile.getMark() != patternTile.getPaint() && patternTile.getMark() != PaintType.EMPTY)
                    if(rc.canAttack(patternTile.getMapLocation()))
                        if(patternTile.getMark() == PaintType.ALLY_SECONDARY)
                            rc.attack(patternTile.getMapLocation(), true);
                        else
                            rc.attack(patternTile.getMapLocation(), false);
            }
            if(rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc))
                rc.completeTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, targetLoc);
        }
        Direction dir = directions[rng.nextInt(directions.length)];
        MapLocation nextLoc = rc.getLocation().add(dir);
        if (rc.canMove(dir))
            rc.move(dir);
        if (rc.canAttack(nextLoc)) {
            MapInfo nextLocInfo = rc.senseMapInfo(nextLoc);
            if(!nextLocInfo.getPaint().isAlly())
                rc.attack(nextLoc);
        }
    }
    public static void runMopper(RobotController rc) throws GameActionException{
        if (isMessanger){
            rc.setIndicatorDot(rc.getLocation(), 255,0,0);
        }
        if (isMessanger && isSaving && knownTowers.size() > 0){
            MapLocation dst = knownTowers.get(0);
            rc.getLocation().directionTo(dst);
        }
        Direction dir = directions[rng.nextInt(directions.length)];
        MapLocation nextLoc = rc.getLocation().add(dir);
        if (rc.canMove(dir))
            rc.move(dir);
        if (rc.canMopSwing(dir))
            rc.mopSwing(dir);
        else if (rc.canAttack(nextLoc))
            rc.attack(nextLoc);

        updateEnemyRobots(rc);
        if (isMessanger){
            updateFriendlyTowers(rc);
            checkNearbyRuins(rc);
        }
    }
    public static void runSplasher(RobotController rc) throws GameActionException{
        Direction dir = directions[rng.nextInt(directions.length)];
        MapLocation nextLoc = rc.getLocation().add(dir);
        if (rc.canMove(dir))
            rc.move(dir);
        if (rc.canAttack(nextLoc))
            rc.attack(nextLoc);
    }
    public static void checkNearbyRuins(RobotController rc) throws GameActionException{
        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        for (MapInfo tile: nearbyTiles){
            if (!tile.hasRuin()) continue;
            if (rc.senseRobotAtLocation(tile.getMapLocation()) != null) continue;

            Direction dir = tile.getMapLocation().directionTo(rc.getLocation());
            MapLocation markTile = tile.getMapLocation().add(dir);
            if (rc.senseMapInfo(markTile).getMark().isAlly()) continue;
//            if (tile.hasRuin() && rc.senseRobotAtLocation(tile.getMapLocation()) == null) {
            isSaving = true;
            return;

        }
    }
    public static void updateFriendlyTowers (RobotController rc) throws GameActionException{
        RobotInfo[] allyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
        for (RobotInfo ally: allyRobots){
            if (!ally.getType().isTowerType()) continue;
            MapLocation allyloc = ally.location;
            if (knownTowers.contains(allyloc)){
                if(isSaving){
                    if (rc.canSendMessage(allyloc, MessageType.SAVE_CHIPS.ordinal())) {
                        rc.sendMessage(allyloc, MessageType.SAVE_CHIPS.ordinal());
                        isSaving = false;
                    }
                }
                continue;
            }

            knownTowers.add(allyloc);

        }
    }
    public static void updateEnemyRobots(RobotController rc) throws GameActionException{
        // Sensing methods can be passed in a radius of -1 to automatically
        // use the largest possible value.
        RobotInfo[] enemyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        if (enemyRobots.length != 0){
            rc.setIndicatorString("There are nearby enemy robots! Scary!");
            // Save an array of locations with enemy robots in them for possible future use.
            MapLocation[] enemyLocations = new MapLocation[enemyRobots.length];
            for (int i = 0; i < enemyRobots.length; i++){
                enemyLocations[i] = enemyRobots[i].getLocation();
            }
            RobotInfo[] allyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
            // Occasionally try to tell nearby allies how many enemy robots we see.
            if (rc.getRoundNum() % 20 == 0){
                for (RobotInfo ally : allyRobots){
                    if (rc.canSendMessage(ally.location, enemyRobots.length)){
                        rc.sendMessage(ally.location, enemyRobots.length);
                    }
                }
            }
        }
    }


//		@Test
//	public void testSanity() {
//		Assert.assertEquals(2, 1+1);
//	}

}
