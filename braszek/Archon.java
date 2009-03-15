package braszek;

import battlecode.common.*;

public class Archon extends AbstractRobot {
	private int newWorkers;
	private int lastWorker;

	public Archon(RobotController rc) {
		super(rc);
		newWorkers = 0;
		state = RobotState.ARCHON_FIND_DEPOSIT;
	}

	@Override
	public void run() throws GameActionException {
		while(myRC.isMovementActive()) {
        	myRC.yield();
        }
		archonStrategy();
		myRC.yield();
	}
	
	private void archonStrategy() throws GameActionException {
		   switch (state) {
	   		case ARCHON_FIND_DEPOSIT:
	   			FluxDeposit[] nerbyDeposits = myRC.senseNearbyFluxDeposits();
	   			MapLocation depositLocation;
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
	   			} else { 	
	   				if (freeDeposit || generator.nextInt(50) == 0 ) {
	   					goTo(myRC.senseDirectionToUnownedFluxDeposit());
	   				}
	   				else {
	   					goTo(myRC.getDirection());
	   				}
	   			}
	   			break;
	   		case ARCHON_ON_DEPOSIT:
	   			if (Clock.getRoundNum() - lastWorker > 1 && newWorkers < 8) {
	   				lastWorker = Clock.getRoundNum();
	   				spawnWorker();
	   				
	   			}
	   			transferEnergon();
	   			break;
	   }

	}
	
	private void spawnWorker() throws GameActionException {
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
			if (myRC.getLocation().add(myRC.getDirection()).equals(bestLocation) && myRC.getEnergonLevel() > RobotType.WORKER.spawnCost()) {
				myRC.spawn(RobotType.WORKER);
				newWorkers++;
				myRC.yield();
				myRC.transferEnergon(Math.min(RobotType.WORKER.maxEnergon(), myRC.getEventualEnergonLevel()), bestLocation, RobotLevel.ON_GROUND);
			} else {
				myRC.setDirection(myRC.getLocation().directionTo(bestLocation));
			}
		}
	}


	
}
