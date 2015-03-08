package mods.basemod.containers;
import level.Level;
import level.player.Player;
import utils.MActionListener;

public class Server {
 private final MActionListener actions = new MActionListener();
 private final Level level = new Level();
 private final ModsContainer mods = new ModsContainer();
 private final Player player = new Player();
 // public final static Resources RES = new Resources();
 // public final static Repository REP = new Repository();
 private boolean running = true;

 public MActionListener getActions () {
  return actions;
 }

 public Level getLevel () {
  return level;
 }

 public ModsContainer getMods () {
  return mods;
 }

 public Player getPlayer () {
  return this.player;
 }

 public boolean isRunning () {
  return running;
 }

 public void setRunning ( boolean b ) {
  this.running = b;
 }

}
