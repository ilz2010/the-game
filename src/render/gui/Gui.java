package render.gui;

import com.jogamp.opengl.util.Animator;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;
import javax.swing.JFrame;
import org.fenggui.IWidget;
import org.fenggui.actor.ScreenshotActor;
import org.fenggui.binding.render.jogl.EventBinding;
import render.gui.widgets.Loading;
import render.gui.widgets.StandartButton;

public class Gui extends JFrame{
  private static final long     serialVersionUID = 1L;
  private EventBinding          eventbinding ;
  private GL2                   gl;
  private GLU                   glu              = new GLU();
  private GLCanvas              canvas;
  private Display               display          = null;
  private GLEventListener       eventListener    = null;
  private float                 rotwAngle         = 0;
  private long                  lastFrame        = 0;

  public Gui(){
   canvas = new GLCanvas();
   eventListener = new Listener();
   canvas.addGLEventListener(eventListener);

   getContentPane().add(canvas, java.awt.BorderLayout.CENTER);
   setSize(800, 400);

   Animator animator = new Animator(canvas);
   animator.setRunAsFastAsPossible(true);
   animator.setPrintExceptions(true);
   animator.start();

   setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  }


  public void buildGUI(){
   IWidget w;
   // 0 - Loading
   w = new Loading(display);
   display.addWidget(0, w);
   
   w  = new StandartButton("Test 1",100,400);
   display.addWidget(1, w);
   
   w = new StandartButton("Test 2",100,200);
   display.addWidget(1, w);

   // display.addWidget(0, 
   //  new StandartFPS(100,200)
    
  //  );


  }
  public void resize(){ this.display.resize(); }
  public void changeGui(Integer id){ 
   //this.display.changeGui(id);
  }
  @Override public final void setVisible(boolean b){ super.setVisible(b); }
  public GLCanvas getCanvas(){ return canvas; }
  
  
  private class Listener implements GLEventListener{
   private ScreenshotActor screenshotActor;

   @Override public void display(GLAutoDrawable arg0){
    gl.glLoadIdentity();
    gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

    display.display();
      
    screenshotActor.renderToDos(display.getBinding().getOpenGL(), display.getWidth(), display.getHeight());
   }
   
   @Override public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    gl.glViewport(0, 0, width, height);
    gl.glMatrixMode(GL2.GL_PROJECTION);
    gl.glLoadIdentity();
    glu.gluPerspective(45, (double) width / (double) height, 4, 1000);
    gl.glMatrixMode(GL2.GL_MODELVIEW);
    gl.glLoadIdentity();
    
    resize();
   }
   
   public void displayChanged(GLAutoDrawable arg0, boolean arg1, boolean arg2){}
   
   @Override public void init(GLAutoDrawable drawable){
    gl = drawable.getGL().getGL2();
    gl.glClearColor(146f / 255f, 164f / 255f, 1, 0.0f);
    gl.glEnable(GL2.GL_BLEND);

    gl.glDisable(GL2.GL_TEXTURE_2D);
    gl.glEnable(GL2.GL_DEPTH_TEST);
    gl.glDepthFunc(GL2.GL_LEQUAL);
    gl.glShadeModel(GL2.GL_SMOOTH);
    gl.glEnable(GL2.GL_LIGHTING);
    gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_AMBIENT, new float[] { 146f / 255f, 164f / 255f, 1f, 1.0f }, 0);
    gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_DIFFUSE, new float[] { 1f, 1f, 1f, 1.0f }, 0);
    gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_POSITION, new float[] { -100, -100, 400 }, 0);
    gl.glEnable(GL2.GL_LIGHT1);
    
    display = new Display(canvas);

    eventbinding = new EventBinding(canvas, display);
    
    buildGUI();

    lastFrame = System.nanoTime();
    screenshotActor = new ScreenshotActor();
    screenshotActor.hook(display);
   }
   
   @Override public void dispose(GLAutoDrawable drawable) {}
  }

}