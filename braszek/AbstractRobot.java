package braszek;

import java.util.Random;
import battlecode.common.*;

public abstract class AbstractRobot {
	protected RobotController myRC;
	protected enum RobotState { 
		ARCHON_FIND_DEPOSIT, 
		ARCHON_ON_DEPOSIT, 
		WORKER_LOAD_BLOCK, 
		WORKER_UNLOAD_BLOCK, 
		WORKER_INIT, 
		WORKER_FIND_PLACE_TO_UNLOAD_BLOCK };
	protected RobotState state;
	protected Random generator;
	
	public AbstractRobot(RobotController rc) {
		myRC = rc;
		generator = new Random();
	}
	
	abstract public void run() throws GameActionException; 
	
	protected void goTo(Direction direction) throws GameActionException {
		Direction destinationDirection = direction;
		Direction currentDirection = destinationDirection;
			
		while (!myRC.canMove(currentDirection) && !currentDirection.equals(destinationDirection.rotateLeft())) {
			currentDirection = currentDirection.rotateRight();
		}

		if (myRC.canMove(currentDirection)) {
		   if (!myRC.getDirection().equals(currentDirection)) {
			   myRC.setDirection(currentDirection);
		   }
		   else {
			   myRC.moveForward();
		   }
		}
	}
	
	protected void transferEnergon() throws GameActionException {
		for (Robot robot : myRC.senseNearbyGroundRobots()) {
			RobotInfo robotInfo = myRC.senseRobotInfo(robot);

			if (robotInfo.team == myRC.getTeam()) {
				MapLocation robotLocation = robotInfo.location;
				   
				if (myRC.getLocation().isAdjacentTo(robotLocation))
					myRC.transferEnergon(Math.min(robotInfo.maxEnergon-robotInfo.eventualEnergon, myRC.getEnergonLevel()/2), robotLocation, RobotLevel.ON_GROUND);
			}
		}
	}
}
