package braszek;

import battlecode.common.*;

public class RobotPlayer implements Runnable {

   private AbstractRobot player;
  
   public RobotPlayer(RobotController rc) {
      switch (rc.getRobotType()) {
      	  case ARCHON:
      		  player = new Archon(rc);
      		  break;
      	  case WORKER:
      		  player = new Worker(rc);   		  
      		  break;
      }
   }
   
   public void run() {
      while(true){
         try{
            /*** beginning of main loop ***/
        	player.run(); 
            /*** end of main loop ***/
         }catch(Exception e) {
            System.out.println("caught exception:");
            e.printStackTrace();
         }
      }
   }
}

