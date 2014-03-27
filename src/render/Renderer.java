package render;

import player.Player;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;

import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.event.awt.AWTKeyAdapter;
import com.jogamp.newt.event.awt.AWTMouseAdapter;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.media.opengl.glu.GLU;
import level.Level;
import utils.Vec2f;
import utils.Vec3f;

public class Renderer implements Runnable,GLEventListener{
public static boolean running = true;
public boolean b=true;
public static Player player;
public static GL2 gl;
public static GLU glu;
public int width=800,height=600;
private Level level;

private final Vec3f plcoord = new Vec3f();
private final Vec2f cmcoord = new Vec2f();

private String sec="";
public int fps=0;

public static void frame(){
    java.awt.Frame frame = new java.awt.Frame("The Game");
    frame.setSize(800,600);
    frame.setLayout(new java.awt.BorderLayout());

    final Animator animator = new Animator();
    frame.addWindowListener(new java.awt.event.WindowAdapter() {
        @Override
    public void windowClosing(java.awt.event.WindowEvent e) {
        new Thread(new Runnable() {
        @Override
    public void run() {
        animator.stop();System.exit(0);
    }}).start();}});

    GLCanvas canvas = new GLCanvas();
    animator.add(canvas);

    final Renderer gears = new Renderer();
    canvas.addGLEventListener(gears);

    frame.add(canvas, java.awt.BorderLayout.CENTER);
    frame.validate();

    Toolkit t = Toolkit.getDefaultToolkit();
    Image i = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    Cursor noCursor = t.createCustomCursor(i, new Point(0, 0), "none");
    frame.setCursor(noCursor);
    
    frame.setVisible(true);
    animator.start();
    }

public static void destroy(){

}
@Override
public void run(){
 frame();
}

    @Override
    public void dispose(GLAutoDrawable gl) {
    System.err.println("Display: Dispose");
    }
    
    @Override
    public void display(GLAutoDrawable drawable) {
        
    gl = drawable.getGL().getGL2();

    gl.glClearColor(0.0f, 0.4f, 1.0f, 0.0f);

    if (GLProfile.isAWTAvailable() && 
        (drawable instanceof javax.media.opengl.awt.GLJPanel) &&
        !((javax.media.opengl.awt.GLJPanel) drawable).isOpaque() &&
        ((javax.media.opengl.awt.GLJPanel) drawable).shouldPreserveColorBufferIfTranslucent()) {
      gl.glClear(GL2.GL_DEPTH_BUFFER_BIT);
    } else {
      gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
    }
   if(b) gl.glPopMatrix();
   //if(b) gl.glTranslatef(player.x, player.y, player.z);
   if(b){ gl.glTranslatef( 0, plcoord.x, 0); if (plcoord.x!=0)plcoord.x=0;}
   if(b){ gl.glTranslatef( plcoord.y, 0, 0); if (plcoord.y!=0)plcoord.y=0;}
   if(b){ gl.glTranslatef( 0, plcoord.z, 0); if (plcoord.z!=0)plcoord.z=0;}
   
   if(b){ gl.glRotatef(cmcoord.x, 0, 0, 1); if (cmcoord.x!=0)cmcoord.x=0;}
   if(b){ gl.glRotatef(cmcoord.y, 1, 0, 0); if (cmcoord.y!=0)cmcoord.y=0;}
   if(b){ gl.glPushMatrix();b=true; }

   for (int x = -50; x <50; x++)
    { 
    gl.glBegin(GL2.GL_QUADS);
    for (int y = -50; y <50; y++)
      {
       
        gl.glVertex3f(x,y,0);
        gl.glColor3f(1, 0, 1);                         
        gl.glVertex3f(x+1,y,0);
        gl.glVertex3f(x+1,y+1,0);
        gl.glVertex3f(x,y+1,0);
      }
    gl.glEnd();
    
}
   // FPS
   Date date=new Date();
   if(sec.equals(new SimpleDateFormat("ss").format(date))) 
    fps+=1;
   else{
    sec = new SimpleDateFormat("ss").format(date);
    System.out.println(fps);
    fps=0;
   }
   // FPS
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
          System.err.println("Gears: Reshape "+x+"/"+y+" "+width+"x"+height);
gl = drawable.getGL().getGL2();
    gl.setSwapInterval(1);
    this.width=width;
    this.height=height;
    float h = (float)height / (float)width;
            
    gl.glMatrixMode(GL2.GL_PROJECTION);

    gl.glLoadIdentity();
    gl.glFrustum(-1.0f, 1.0f, -h, h, 5.0f, 60.0f);
    gl.glMatrixMode(GL2.GL_MODELVIEW);
    gl.glLoadIdentity();
    gl.glTranslatef(0.0f, 0.0f, -40.0f);

    }

    @Override
   public void init(GLAutoDrawable drawable) {
    player = new Player(0,0,2,level);   

    gl  = drawable.getGL().getGL2();
    glu = new GLU();

    float pos[] = { 5.0f, 5.0f, 10.0f, 0.0f };
    float red[] = { 0.8f, 0.1f, 0.0f, 0.7f };
    float green[] = { 0.0f, 0.8f, 0.2f, 0.7f };
    float blue[] = { 0.2f, 0.2f, 1.0f, 0.7f };

    gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, pos, 0);
    gl.glEnable(GL2.GL_CULL_FACE);
    gl.glEnable(GL2.GL_LIGHTING);
    gl.glEnable(GL2.GL_LIGHT0);
    gl.glEnable(GL2.GL_DEPTH_TEST);
    gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);   
  //  gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);
    /* make the gears 
    if(0>=gear1) {
        gear1 = gl.glGenLists(1);
        gl.glNewList(gear1, GL2.GL_COMPILE);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT_AND_DIFFUSE, red, 0);
        gear(gl, 1.0f, 4.0f, 1.0f, 20, 0.7f);
        gl.glEndList();
        System.out.println("gear1 list created: "+gear1);
    } else {
        System.out.println("gear1 list reused: "+gear1);
    }
    */        
    gl.glEnable(GL2.GL_NORMALIZE);

 
    MouseListener Mouse = new RMouseAdapter();    
    KeyListener Keyboard = new RKeyAdapter();

    if (drawable instanceof Window) {
        Window window = (Window) drawable;
        window.addMouseListener(Mouse);
        window.addKeyListener(Keyboard);
    } else if (GLProfile.isAWTAvailable() && drawable instanceof java.awt.Component) {
        java.awt.Component comp = (java.awt.Component) drawable;
        new AWTMouseAdapter(Mouse).addTo(comp);
        new AWTKeyAdapter(Keyboard).addTo(comp);
    }
    }

//---------------------------

  class RKeyAdapter extends KeyAdapter {      
    @Override
    public void keyPressed(KeyEvent e) {
        int kc = e.getKeyCode();
        if(KeyEvent.VK_ESCAPE == kc){
         running=false;   
        } else if(KeyEvent.VK_LEFT == kc) {
         plcoord.x-= 1.5;
        } else if(KeyEvent.VK_RIGHT == kc) {
         plcoord.x += 1.5;
        } else if(KeyEvent.VK_UP == kc) {
         plcoord.y -= 1.5;
        } else if(KeyEvent.VK_DOWN == kc) {
         plcoord.y += 1.5;
        } else if(KeyEvent.VK_W == kc) {
         cmcoord.y -= 1;
        } else if(KeyEvent.VK_S == kc) {
         cmcoord.y += 1;
        } else if(KeyEvent.VK_A == kc) {
         cmcoord.x -= 1;
        } else if(KeyEvent.VK_D == kc) {
         cmcoord.x += 1;
        }
    }
  }
  
  class RMouseAdapter extends MouseAdapter {
      int prevx;
      int prevy;
      
      @Override
      public void mousePressed(MouseEvent e) {
    ////    prevMouseX = e.getX();
     //   prevMouseY = e.getY();
          
        if ((e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0) {
     //     mouseRButtonDown = true;
        }
      }
        
      @Override
      public void mouseReleased(MouseEvent e) {
        if ((e.getModifiers() & MouseEvent.BUTTON3_MASK) != 0) {
       //   mouseRButtonDown = false;
        }
      }
        
      @Override
      public void mouseDragged(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();

        float thetaY = 360.0f * ( (float)(x-prevx)/(float)width);
        float thetaX = 360.0f * ( (float)(prevy-y)/(float)height);
        
        prevx = x;
        prevy = y;
        
        cmcoord.x += thetaX;
        cmcoord.y += thetaY;
      }
  }
}
