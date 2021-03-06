package render;

import javax.media.opengl.GL2;
import org.fenggui.binding.render.Graphics;
import org.fenggui.event.FocusEvent;
import org.fenggui.event.key.*;
import org.fenggui.event.mouse.*;
import render.gui.Gui;
import utils.containers.vec.Vec2;

public class Render implements Runnable {

 public static boolean running = true;
 private final Vec2<Integer> mcoord = new Vec2<>();
 private final Hooks hook = new Hooks();
 public GL2 gl;
//
// private MTimer timer;
// private Player player;

 private Gui gui;

 public void init () {
  this.gui = new Gui();
  gui.setTitle("The Game");
  gui.setVisible(true);
  gl = gui.getGl();
 }

 public static void destroy () {

 }

 @Override
 public void run () {
  init();

 }

 public void initfinal () {
  this.gui.changeGui(1);
 }

 public void renderW ( Graphics g ) {
 }

 public void resizeW () {
 }

 public void initW () {
 }

 public Hooks gethook () {
  return hook;
 }

 public void setMcoord ( int x, int y ) {
  mcoord.sX(x);
  mcoord.sY(y);
 }

 public class Hooks {

  private MouseAdapter mhook;
  private KeyAdapter khook;

  public void mouseEntered ( MouseEnteredEvent e ) {
   mhook.mouseEntered(e);
  }

  public void mouseExited ( MouseExitedEvent e ) {
   mhook.mouseExited(e);
  }

  public void mousePressed ( MousePressedEvent e ) {
   mhook.mousePressed(e);
  }

  public void mouseMoved ( int displayX, int displayY ) {
   mhook.mouseMoved(null);
   setMcoord(displayY, displayY);
  }

  public void mouseDragged ( MouseDraggedEvent e ) {
   mhook.mouseDragged(e);
  }

  public void mouseReleased ( MouseReleasedEvent e ) {
   mhook.mouseReleased(e);
  }

  public void mouseClicked ( MouseClickedEvent e ) {
   mhook.mouseClicked(e);
  }

  public void mouseDoubleClicked ( MouseDoubleClickedEvent e ) {
   mhook.mouseDoubleClicked(e);
  }

  public void mouseWheel ( MouseWheelEvent e ) {
   mhook.mouseWheel(e);
  }

  public void keyPressed ( KeyPressedEvent e ) {
   khook.keyPressed(e);
  }

  public void keyReleased ( KeyReleasedEvent e ) {
   khook.keyReleased(e);
  }

  public void keyTyped ( KeyTypedEvent e ) {
   khook.keyTyped(e);
  }

  public void focusChanged ( FocusEvent focusEvent ) {
  }
 }
}
