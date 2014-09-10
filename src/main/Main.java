package main;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import mods.basemod.CoreModContainer;
import mods.basemod.ModContainer;
import utils.Error;
import utils.MTimer;
import utils.Options;
import utils.exceptions.TermEx; 

public final class Main implements Runnable{
 public final static String mdir = "/usr/games/game/"; 
 
 public final static Logger log  = Logger.getLogger(Main.class.getName()); 
 public final static Options OPTIONS = new Options(mdir + "options.db"); 
 public final static Error ERR_LOG   = new Error();
 public final static IdMap IdMap = new IdMap();
 public final static MTimer timer = new MTimer();
 public final static ModContainer mods = new ModContainer();
 public final static CoreModContainer core = new CoreModContainer();
 public final static render.Render rend = new render.Render();
 public final static Main main = new Main();
 public final static Thread Tm = new Thread( main );
 public final static Thread Tr = new Thread( rend );
 
 
 public boolean running = true;

 
  
 public void init() throws TermEx, InterruptedException{ 
  mods.loadDir();
  //rend.initfinal();
 }
 
 @Override
 public void run(){
  try{    
   init();

//   while (running){  
//    Tm.wait();
//   }
  
   destroy();
  }catch(TermEx ex){
   System.err.println(ex.s);
   Tm.interrupt();
  } catch ( InterruptedException ex ) {
   
  } 
 }
  


 public void destroy(){
  ERR_LOG.check();
  
  //level.save();
 }
 
 public static void main(String[] args){
  try {
   LogManager.getLogManager().readConfiguration(new FileInputStream("/usr/games/game/logging.properties"));
  } catch (IOException e) {
   System.err.println("Could not setup logger configuration: " + e.toString());
  }
  
  Tm.start();
  Tr.start();
 }   
}
