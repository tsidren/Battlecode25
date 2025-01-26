package First_Player;

//import static org.junit.Assert.*;

import battlecode.common.*;
//import org.junit.Test;

import javax.naming.directory.DirContext;
import java.awt.*;
import java.util.Random;
import java.util.ResourceBundle;

public class RobotPlayer {
	static int turnCount = 0;
	static final Random rng = new Random(6147);
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
		while (true){
			turnCount += 1;
			try {
				switch (rc.getType()) {
					case SOLDIER:
						runSoldier(rc);
						break;
					case MOPPER:
						runMopper(rc);
						break;
					case SPLASHER:
						runSplasher(rc);
						break;
					default:
						runTower(rc);
						break;
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
		if(rc.canUpgradeTower(rc.getLocation()))
			rc.upgradeTower(rc.getLocation());
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
		for (MapInfo tile: nearbyTiles){
			if (tile.hasRuin())
				curRuin = tile;
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
		Direction dir = directions[rng.nextInt(directions.length)];
		MapLocation nextLoc = rc.getLocation().add(dir);
		if (rc.canMove(dir))
			rc.move(dir);
		if (rc.canMopSwing(dir))
			rc.mopSwing(dir);
		else if (rc.canAttack(nextLoc))
			rc.attack(nextLoc);
	}
	public static void runSplasher(RobotController rc) throws GameActionException{
		Direction dir = directions[rng.nextInt(directions.length)];
		MapLocation nextLoc = rc.getLocation().add(dir);
		if (rc.canMove(dir))
			rc.move(dir);
		if (rc.canAttack(nextLoc))
			rc.attack(nextLoc);
	}
//		@Test
//	public void testSanity() {
//		Assert.assertEquals(2, 1+1);
//	}

}
