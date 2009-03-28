package braszek;

import java.util.HashMap;

import battlecode.common.*;
import braszek.AbstractRobot.MessageType;

public class Archon extends AbstractRobot {
	private int[] newRobots = new int[RobotType.values().length];
	private int lastRobot;
	private RobotType currentRobotType;
	private final int spawnDelay = 1;
	private HashMap<RobotType, Integer> spawnRobots = new HashMap<RobotType, Integer>();
	private int numberOfKamikadze = 0;

	public Archon(RobotController rc) {
		super(rc);
		for (int i = 0; i < RobotType.values().length; i++) {
			newRobots[i] = 0;
		}
		spawnRobots.put(RobotType.WORKER, 40);
		spawnRobots.put(RobotType.SOLDIER, 40);
		 
		currentRobotType = RobotType.WORKER;
		state = RobotState.ARCHON_FIND_DEPOSIT;
	}

	public void run() throws GameActionException {
		while(myRC.isMovementActive()) {
        	myRC.yield();
        }
		archonStrategy();
		myRC.yield();
	}
	
	private void archonStrategy() throws GameActionException {
		updateStatus();
		switch (state) {
	   		case ARCHON_FIND_DEPOSIT:
	   			FluxDeposit[] nerbyDeposits = myRC.senseNearbyFluxDeposits();
	   			MapLocation depositLocation = null;
	   			boolean onDeposit = false;
	   			boolean freeDeposit = false;
	   			
	   			for (FluxDeposit deposit : nerbyDeposits) {
	   				depositLocation = myRC.senseFluxDepositInfo(deposit).location;
	   				
	   				if (depositLocation.equals(myRC.getLocation())) {
	   					onDeposit = true;
	   				}
	   				
	   				if (myRC.senseAirRobotAtLocation(depositLocation) == null) {
	   					freeDeposit = true;
	   				}
	   			}
	   			
	   			if (onDeposit) {
	   				state = RobotState.ARCHON_ON_DEPOSIT;
	   				myHome = depositLocation;
	   				
	   			} else { 	
	   				if (freeDeposit || generator.nextInt(50) == 0 ) {
	   					goTo(myRC.senseDirectionToUnownedFluxDeposit());
	   				}
	   				else {
	   					goTo(myRC.getDirection());
	   				}
	   			}
	   			
	   			boolean spawnKamikadze = enemyDistance < 30;
	   			numberOfKamikadze = 0;
	   			
	   			while (spawnKamikadze && numberOfKamikadze < 3) {
	   			//for (int i = 0; i < 10; i++) {
	   				if (myRC.getEventualEnergonLevel() > 2.0/3.0 * myRC.getMaxEnergonLevel()) {
	   					myRC.yield();
	   					spawnRobot(RobotType.SOLDIER, false);
	   					transferEnergon();
	   				}
	   			}
	   			break;
	   		case ARCHON_ON_DEPOSIT:  			
				if (myRC.getEventualEnergonLevel() > 2.0/3.0 * myRC.getMaxEnergonLevel() && spawnRobots.size() > 0) {
	   				if (newRobots[currentRobotType.ordinal()] < spawnRobots.get(currentRobotType)) {
	   					spawnRobot(currentRobotType, true);
	   				}
	   				else {
	   					spawnRobots.remove(currentRobotType);
	   				}
	   				
	   				if (spawnRobots.size() > 0) {
	   					int height = myRC.senseHeightOfLocation(myHome);
	   					
	   					if (enemyDistance < 30) {
	   						if (spawnRobots.containsKey(RobotType.SOLDIER)) {
	   							currentRobotType = RobotType.SOLDIER;
	   						}
	   						else {
	   							currentRobotType = RobotType.WORKER;
	   						}
	   					}
	   					else if (height < 6) {
	   						if (spawnRobots.containsKey(RobotType.WORKER)) {
	   							currentRobotType = RobotType.WORKER;
	   						}
	   						else {
	   							currentRobotType = RobotType.SOLDIER;
	   						}
	   					}
	   					else {
	   						if (currentRobotType == RobotType.SOLDIER && spawnRobots.containsKey(RobotType.WORKER)) {
	   							currentRobotType = RobotType.WORKER;
	   						}
	   						else if (spawnRobots.containsKey(RobotType.SOLDIER)) {
	   							currentRobotType = RobotType.SOLDIER;
	   						}
	   					}
	   				}

	   				lastRobot = Clock.getRoundNum();
	   			}
	   			
	   			break;
		}
		transferEnergon();
		myRC.yield();
		sendEnemyLocation();
		
		myRC.yield();
		sendSpam();
	}
	
	
	
	private void spawnRobot(RobotType robotType, boolean onDeposit) throws GameActionException {
		MapLocation bestLocation = null;
		int bestHeight = Integer.MIN_VALUE;
		   
		for (Direction currentDirection : Direction.values()) {
			if (!currentDirection.equals(Direction.OMNI) && !currentDirection.equals(Direction.NONE)) {
				MapLocation currentLocation = myRC.getLocation().add(currentDirection);
				TerrainTile currentTile = myRC.senseTerrainTile(currentLocation);
			   
				if (currentTile.isTraversableAtHeight(RobotLevel.ON_GROUND)) {
					if (myRC.senseGroundRobotAtLocation(currentLocation) == null) {
						if (currentTile.getHeight() > bestHeight) {
							bestHeight = currentTile.getHeight();
							bestLocation = currentLocation;
						}
					}
				}
			}
		}
		
		if (bestLocation != null) {
			if (myRC.getLocation().add(myRC.getDirection()).equals(bestLocation) && myRC.getEnergonLevel() > robotType.spawnCost()) {
				myRC.spawn(robotType);

				if (onDeposit) {
					newRobots[robotType.ordinal()]++;
					
					Message m = new Message();
					m.ints = new int[1];
					m.ints[0] = MessageType.SET_HOME.ordinal();
   		   			m.locations = new MapLocation[1];
   		   			m.locations[0] = myRC.getLocation();
   		   			sendMessage(m);
				}
				else {
					numberOfKamikadze++;
					Message m = new Message();
					m.ints = new int[1];
					m.ints[0] = MessageType.KAMIKADZE.ordinal();
   		   			m.locations = enemyLocations;
   		   			sendMessage(m);
				}
   		   		
				myRC.yield();
				myRC.transferEnergon(Math.min(robotType.maxEnergon(), myRC.getEventualEnergonLevel()), bestLocation, RobotLevel.ON_GROUND);
			} else {
				if (!onDeposit) {
					waitUntilMovementIdle();
				}
				myRC.setDirection(myRC.getLocation().directionTo(bestLocation));
			}
		}
	}

	@Override
	protected void receiveMessage(Message m) {
		// TODO Auto-generated method stub		
	}
}
