package braszek;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Message;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Soldier extends AbstractRobot {
	private RobotInfo enemy;
	private MapLocation enemyLocation;
	private boolean kamikadze;

	public Soldier(RobotController rc) {
		super(rc);
		enemy = null;
		enemyLocation = null;
		state = RobotState.SOLDIER_INIT;
		kamikadze = false;
	}

	@Override
	public void run() throws GameActionException {
		super.run();
		while(myRC.isMovementActive()) {
        	myRC.yield();
        }
		waitUntilMovementIdle();
		soldierStrategy();
		myRC.yield();
	}
	
	private void chooseEnemy(MapLocation[] enemyLocations) throws GameActionException {
		enemy = null;
		int enemyDistance = Integer.MAX_VALUE;
		
		for (MapLocation eLoc : enemyLocations) {
			int distance = myRC.getLocation().distanceSquaredTo(eLoc);
			
			if (distance < enemyDistance && RobotType.SOLDIER.energonUpkeep() * distance * 2 < myRC.getEventualEnergonLevel()) {
				enemyDistance = distance;
				enemyLocation = eLoc;
				
				try {
					Robot robot = myRC.senseAirRobotAtLocation(eLoc);
					if (robot == null) {
						robot = myRC.senseGroundRobotAtLocation(eLoc);
					}
					
					if (robot != null) {
						enemy = myRC.senseRobotInfo(robot);
					}
				}
				catch (GameActionException e) {
					enemy = null;
				}
			}
		}
	}
	
	private void soldierStrategy() throws GameActionException {
		updateStatus();
		Direction myDirection = null;
		MapLocation[] enemyLocations = null;
		
		switch (state) {
			case SOLDIER_INIT:
				if (myHome != null) {
					state = RobotState.SOLDIER_PATROL;
				}
				break;
			case SOLDIER_GO_HOME:
				if (!myRC.getLocation().isAdjacentTo(myHome)) {
					goTo(myRC.getLocation().directionTo(myHome));
				} else {	
					//state = RobotState.SOLDIER_FIND_ENEMY;
					state = RobotState.SOLDIER_PATROL;
				}
				break;
			case SOLDIER_PATROL:
				if (myRC.getEventualEnergonLevel() > 1.0/2.0 * myRC.getMaxEnergonLevel()) {
					myDirection = myRC.getDirection();
					enemyLocations = findEnemy();
					chooseEnemy(enemyLocations);
					
					if (enemy != null) {
						state = RobotState.SOLDIER_GO_TO_ENEMY;
						
						Message m =  new Message();
						m.ints = new int[1];
						m.ints[0] = MessageType.ATTACK_ENEMY.ordinal();
						m.locations = new MapLocation[1];
						m.locations[0] = enemy.location;
						sendMessage(m);
					}
					else {
						goTo(myDirection);
						if (generator.nextInt(10) == 0) {
							state = RobotState.SOLDIER_FIND_ENEMY;
						}
					}
				}
				else {
					state = RobotState.SOLDIER_GO_HOME;
				}
				break;
			case SOLDIER_FIND_ENEMY:
				myDirection = myRC.getDirection();

				while (!myRC.getDirection().equals(myDirection.rotateLeft()) && state == RobotState.SOLDIER_FIND_ENEMY) {
					enemyLocations = findEnemy();
					chooseEnemy(enemyLocations);
										
					if (enemy != null) {
						state = RobotState.SOLDIER_GO_TO_ENEMY;
						
						Message m =  new Message();
						m.ints = new int[1];
						m.ints[0] = MessageType.ATTACK_ENEMY.ordinal();
						m.locations = new MapLocation[1];
						m.locations[0] = enemy.location;
						sendMessage(m);
					}
					else {
						if (myRC.getEventualEnergonLevel() > 2.0/3.0 * myRC.getMaxEnergonLevel()) {
							myRC.setDirection(myRC.getDirection().rotateRight());
							myRC.yield();
						}
						else {
							state = RobotState.SOLDIER_GO_HOME;
						}
					}
				}
				if (state == RobotState.SOLDIER_FIND_ENEMY) {
					state = RobotState.SOLDIER_PATROL;
				}
				break;
			case SOLDIER_GO_TO_ENEMY:
				if (enemy != null) {					
					if (myRC.getLocation().isAdjacentTo(enemy.location)) {
						state = RobotState.SOLDIER_ATTACK_ENEMY;
					}
					else {
						goTo(myRC.getLocation().directionTo(enemy.location));
					}
				}
				else {
					if (enemyLocation != null) {
						goTo(myRC.getLocation().directionTo(enemyLocation));
						if (findEnemy() != null) {
							state = RobotState.SOLDIER_FIND_ENEMY;
						}
					}
					else {
						state = RobotState.SOLDIER_PATROL;
					}
				}
						/*
						if (enemy != null) {
							try {
								myDirection = myRC.getDirection();
								
								while (!myRC.canSenseObject(enemy) && !myRC.getDirection().equals(myDirection.rotateLeft())) {
									myRC.setDirection(myRC.getDirection().rotateRight());
									myRC.yield();
								}
								RobotInfo enemyInfo = myRC.senseRobotInfo(enemy);
								if (myRC.getEventualEnergonLevel() > 2.0/3.0 * myRC.getMaxEnergonLevel()) { 
									if (!myRC.getLocation().isAdjacentTo(enemyInfo.location)) {
										goTo(myRC.getLocation().directionTo(enemyInfo.location));
									}
									else {
										state = RobotState.SOLDIER_ATTACK_ENEMY;
									}
								}
								else {
									state = RobotState.SOLDIER_GO_HOME;
								}
							}
							catch (GameActionException e) {
								enemy = null;
								enemyLocation = null;
								//state = RobotState.SOLDIER_FIND_ENEMY;
								state = RobotState.SOLDIER_PATROL;
							}
						}
						else {
							//state = RobotState.SOLDIER_FIND_ENEMY;
							state = RobotState.SOLDIER_PATROL;
						}
					}
				}*/
			
				break;
			case SOLDIER_ATTACK_ENEMY:
				if (enemy.location.isAdjacentTo(myRC.getLocation())) {
					if (myRC.getEnergonLevel() > myRC.getRobotType().attackPower()) {
						try {
							if (enemy.type.isAirborne()) {
								myRC.attackAir(enemy.location);
							} else {
								myRC.attackGround(enemy.location);
							}
						}
						catch (GameActionException e) {
							//state = RobotState.SOLDIER_PATROL;
						}
					}
					else {
						goTo(myRC.getLocation().directionTo(myHome));
					}
				}
				else {
					goTo(myRC.getLocation().directionTo(enemy.location));
				}
				/*
				try {
					RobotInfo enemyInfo = myRC.senseRobotInfo(enemy);
					if (enemyInfo.location.isAdjacentTo(myRC.getLocation())) {
						if (myRC.getEnergonLevel() > myRC.getRobotType().attackPower()) {
							if (enemyInfo.type.isAirborne()) {
								myRC.attackAir(enemyInfo.location);
							} else {
								myRC.attackGround(enemyInfo.location);
							}
						}
						else {
							goTo(myRC.getLocation().directionTo(myHome));
						}
					}
					else {
						goTo(myRC.getLocation().directionTo(enemyInfo.location));
						//state = RobotState.SOLDIER_GO_TO_ENEMY;
					}
				} catch (GameActionException e) {
					enemy = null;
					//state = RobotState.SOLDIER_FIND_ENEMY;
					state = RobotState.SOLDIER_PATROL;
				}
				*/
				break;
				
		}
		transferEnergon();
		myRC.yield();
		sendSpam();
	}
	
	private void setTarget(Message m) throws GameActionException {
		if (MessageType.values()[m.ints[0]] == MessageType.ATTACK_ENEMY) {
			chooseEnemy(m.locations);
			state = RobotState.SOLDIER_GO_TO_ENEMY;
		}
	}

	@Override
	protected void receiveMessage(Message m) throws GameActionException {
		if (m.ints != null && m.strings != null && m.ints.length == 1 && m.strings.length == 2 && m.strings[1].equals(myRC.getTeam().toString())) {
			switch (MessageType.values()[m.ints[0]]) {
				case SET_HOME:
					setHome(m);
					break;
				case ATTACK_ENEMY:
					if (enemy == null) {
						setTarget(m);
					}
					break;
				case ENEMY_LOCATIONS:
					if (enemy == null) {
						chooseEnemy(m.locations);
						state = RobotState.SOLDIER_GO_TO_ENEMY;
					}
					break;
				case KAMIKADZE:
					myHome = myRC.getLocation();
					chooseEnemy(m.locations);
					state = RobotState.SOLDIER_GO_TO_ENEMY;
					kamikadze = true;
					break;
			}
		}
	}

}
