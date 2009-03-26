package braszek;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import battlecode.common.*;

public abstract class AbstractRobot {
	protected RobotController myRC;
	protected MapLocation myHome;
	protected enum RobotState { 
		ARCHON_FIND_DEPOSIT, 
		ARCHON_ON_DEPOSIT, 
		WORKER_LOAD_BLOCK, 
		WORKER_UNLOAD_BLOCK, 
		WORKER_INIT, 
		WORKER_FIND_PLACE_TO_UNLOAD_BLOCK,
		SOLDIER_INIT,
		SOLDIER_FIND_ENEMY, 
		SOLDIER_GO_TO_ENEMY,
		SOLDIER_ATTACK_ENEMY,
		SOLDIER_GO_HOME,
		SOLDIER_PATROL,
		SOLDIER_KAMIKADZE };
	protected enum MessageType {
		SET_HOME,
		ATTACK_ENEMY,
		ENEMY_LOCATIONS,
		KAMIKADZE };
	protected RobotState state;
	protected Random generator;
	protected MapLocation[] enemyLocations;
	protected int enemyDistance;
	
	public AbstractRobot(RobotController rc) {
		myRC = rc;
		myHome = null;
		generator = new Random();
		enemyLocations = null;
		enemyDistance = Integer.MAX_VALUE;
	}
	
	public void run() throws GameActionException {
		Message[] messages = myRC.getAllMessages();
		for (Message m : messages) {
			receiveMessage(m);
		}
	}
	
	
	protected void updateStatus() {
		myRC.setIndicatorString(1, Integer.toString(enemyDistance));
		myRC.setIndicatorString(2, state.name());
	}
	protected abstract void receiveMessage(Message m) throws GameActionException;
	
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
	
	protected void sendMessage(Message m) throws GameActionException {
		m.strings = new String[2];
		m.strings[0] = myRC.getTeam().toString();
		m.strings[1] = "TRUE";
		myRC.broadcast(m);
	}
	
	protected void sendEnemyLocation() throws GameActionException {
		enemyLocations = findEnemy();
		
		if (enemyLocations.length > 0) {
			Message m = new Message();
			m.ints = new int[1];
			m.ints[0] = MessageType.ENEMY_LOCATIONS.ordinal();
			m.locations = enemyLocations;
			//myRC.broadcast(m);
			sendMessage(m);
		}
		else {
			enemyDistance = Integer.MAX_VALUE;
		}
	}
	
	protected MapLocation[] findEnemy() throws GameActionException {
		ArrayList<MapLocation> enemyLocations = new ArrayList<MapLocation>();
		
		ArrayList<Robot> allRobots = new ArrayList<Robot>();
		Robot[] airRobots = myRC.senseNearbyAirRobots(); 
		Robot[] groundRobots = myRC.senseNearbyGroundRobots();
		allRobots.addAll(Arrays.asList(airRobots));
		allRobots.addAll(Arrays.asList(groundRobots));
		
		for (Robot robot : allRobots) {
			try {
				RobotInfo info = myRC.senseRobotInfo(robot);
			
				if (!myRC.getTeam().equals(info.team)) {
					int distance = myRC.getLocation().distanceSquaredTo(info.location);
				
					if (distance < enemyDistance) {
						enemyDistance = distance;
					}
				
					enemyLocations.add(info.location);
				}
			}
			catch (GameActionException e) {
			}
		}
		
		MapLocation[] retLocations = new MapLocation[enemyLocations.size()];
		retLocations = enemyLocations.toArray(retLocations);
		
		return retLocations;
	}
	
	
	protected void setHome(Message m) {
		if (MessageType.values()[m.ints[0]] == MessageType.SET_HOME && myHome == null) {
			myHome = m.locations[0];
		}
	}
	
	protected MapLocation findNearestFluxDeposit() throws GameActionException {
		FluxDeposit[] deposits = myRC.senseNearbyFluxDeposits();
		int bestDistance = Integer.MAX_VALUE;
		MapLocation nearest = null;
			   
		for (FluxDeposit deposit : deposits) {
			MapLocation currentLocation = myRC.senseFluxDepositInfo(deposit).location;
			int currentDistance = myRC.getLocation().distanceSquaredTo(currentLocation);
					
			if (currentDistance < bestDistance) {
				bestDistance = currentDistance;
				nearest = currentLocation;
			}
		}
		   
		return nearest;
	}
	
	protected void waitUntilMovementIdle() {
		while (myRC.getRoundsUntilMovementIdle() > 0) {
			myRC.yield();
		}
	}
	
	protected void transferEnergon() throws GameActionException {
		for (Robot robot : myRC.senseNearbyGroundRobots()) {
			try {
				RobotInfo robotInfo = myRC.senseRobotInfo(robot);

				if (robotInfo.team == myRC.getTeam()) {
					MapLocation robotLocation = robotInfo.location;
				   
					if (myRC.getLocation().isAdjacentTo(robotLocation))
						myRC.transferEnergon(Math.max(0, Math.min(robotInfo.maxEnergon-robotInfo.eventualEnergon, myRC.getEnergonLevel()/2)), robotLocation, RobotLevel.ON_GROUND);
				}
			}
			catch (GameActionException e) {
				
			}
		}
	}
}
