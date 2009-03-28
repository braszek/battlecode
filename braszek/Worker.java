package braszek;

import battlecode.common.*;

public class Worker extends AbstractRobot {
	
	public Worker(RobotController rc) {
		super(rc);
		state = RobotState.WORKER_INIT;
	}

	public void run() throws GameActionException {
		super.run();
		while(myRC.isMovementActive()) {
        	myRC.yield();
        }		
		waitUntilMovementIdle();
		workerStrategy();
		myRC.yield();
	}
	
	private void workerStrategy() throws GameActionException {
		updateStatus();
		switch (state) {
			case WORKER_INIT:
				if (myHome != null) {
					state = RobotState.WORKER_LOAD_BLOCK;
				}
				break;
		   	case WORKER_FIND_PLACE_TO_UNLOAD_BLOCK:
		   		goTo(myRC.getLocation().directionTo(myHome));
		   		if (myRC.getLocation().isAdjacentTo(myHome)) {
		   			state = RobotState.WORKER_UNLOAD_BLOCK;
		   		}
		   		break;
		   	case WORKER_UNLOAD_BLOCK:
		   		if (myRC.getNumBlocks() > 0) {
		   			if (myRC.canUnloadBlockToLocation(myRC.getLocation().add(myRC.getLocation().directionTo(myHome)))) {
		   				myRC.unloadBlockToLocation(myRC.getLocation().add(myRC.getLocation().directionTo(myHome)));
		   				break;
					}
		   			else if (myRC.canUnloadBlockToLocation(myRC.getLocation().add(myRC.getLocation().directionTo(myHome).rotateLeft()))){
		   				myRC.unloadBlockToLocation(myRC.getLocation().add(myRC.getLocation().directionTo(myHome).rotateLeft()));
		   				break;
					}
		   			else if (myRC.canUnloadBlockToLocation(myRC.getLocation().add(myRC.getLocation().directionTo(myHome).rotateRight()))){
		   				myRC.unloadBlockToLocation(myRC.getLocation().add(myRC.getLocation().directionTo(myHome).rotateRight()));
		   				break;
		   			}
		   			if (myRC.getLocation().equals(myHome)) {
		   				Direction bestDirection = null;
		   				int bestHeight = Integer.MIN_VALUE;
	  						
		   				for (Direction d : Direction.values()) {
		   					if (!d.equals(Direction.OMNI) && !d.equals(Direction.NONE)) {
		   						int height = myRC.senseHeightOfLocation(myRC.getLocation().add(d));
		   						if (height > bestHeight) {
		   							bestHeight = height;
		   							bestDirection = d;
	  							}
	  						}
	  					}
		   				goTo(bestDirection);
	   				}
		   			else {
		   				goTo(myRC.getLocation().directionTo(myHome).opposite());
	   				}
		   		}
		   		else {
		   			state = RobotState.WORKER_LOAD_BLOCK;
		   		}	   			
		   		break;
		   	case WORKER_LOAD_BLOCK:
		   		MapLocation[] blocks = myRC.senseNearbyBlocks();
		   			
		   		if (blocks.length > 0) {   				
		   			MapLocation blockLocation = null;
		   			int bestDistance = Integer.MAX_VALUE;
		   				
		   			if (myHome != null) {
		   				bestDistance = Integer.MIN_VALUE;
		   					
		   				for (MapLocation block : blocks) {
		   					int currentDistance = myHome.distanceSquaredTo(block);
			   					
		   					if (currentDistance > bestDistance && !myHome.isAdjacentTo(block) && !myHome.equals(block) && RobotType.WORKER.energonUpkeep() * currentDistance * 2 < myRC.getEventualEnergonLevel()) {
		   						bestDistance = currentDistance;
		   						blockLocation = block;
			   				}		   					
			   			}
		   			}
		   				
		   			if (blockLocation != null) {
		   				if (myRC.getEventualEnergonLevel() > 2.0/3.0 * myRC.getMaxEnergonLevel()) {
		   					if (myRC.canLoadBlockFromLocation(blockLocation)) {
		   						myRC.loadBlockFromLocation(blockLocation);
		   						state = RobotState.WORKER_FIND_PLACE_TO_UNLOAD_BLOCK;
		   					}
		   					else {
		   						goTo(myRC.getLocation().directionTo(blockLocation));
		   					}
		   				} else {
		   					Direction direction = myRC.getDirection();
		   					Direction current = direction;
		   						
		   					while (!myRC.canLoadBlockFromLocation(myRC.getLocation().add(current)) && !current.equals(direction.rotateLeft())) {
		   						current = current.rotateRight();
		   					}
		   				   
		   					if (myRC.canLoadBlockFromLocation(myRC.getLocation().add(current)) && !myHome.isAdjacentTo(myRC.getLocation().add(current)) && !myHome.equals(myRC.getLocation().add(current))) {
		   						blockLocation = myRC.getLocation().add(current);
		   					}
		   					else {
		   						blockLocation = null;
		   					}
		   						
		   					if (blockLocation != null) {
		   						myRC.loadBlockFromLocation(blockLocation);
		   						state = RobotState.WORKER_FIND_PLACE_TO_UNLOAD_BLOCK;
		   					} else {
		   						goTo(myRC.getLocation().directionTo(myHome));
		   					}
		   				}
		   			}
		   			else {
		   				goTo(myRC.getDirection());
		   			}
		   		}
		   		else {
		   			goTo(myRC.getLocation().directionTo(myHome));
		   		}
		   		break;
		}

		transferEnergon();
		myRC.yield();
		sendEnemyLocation();
		myRC.yield();
		sendSpam();
	}

	@Override
	protected void receiveMessage(Message m) {
		if (m.ints != null && m.strings != null && m.ints.length == 1 && m.strings.length == 2 && m.strings[1].equals(myRC.getTeam().toString())) {
			switch (MessageType.values()[m.ints[0]]) {
				case SET_HOME:
					setHome(m);
					break;
			}
		}
	}
	

}
