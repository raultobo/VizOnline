package perspectives;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import javax.media.opengl.FPSCounter;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.fixedfunc.GLMatrixFunc;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SwingWorker;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.opengl.GLWindow;

/**
 * This is a window class for Viewers. It will be able to accommodate any of the three main types of Viewers: 2D, 3D, GUI. It will call their rendering, simulation, and interactivity functions (if appropriate), it will implement double buffering for them, and it can be added to the viewer area
 * of the Environment. ViewerContainers automatically implementing panning and zooming for 2D viewers via 2dtransforms applied to the Graphics2D objects passed onto the Viewer2D's render function.
 * @author rdjianu
 *
 */
public class ViewerContainer3D extends ViewerContainer implements Runnable{
	
	
	private Viewer3D v3d;
		
	//a pointer to the parent Environment class (needed for instance to delete the viewer from the Environment if the user activates the 'X')
	final Environment env;
	
	 	
	Thread thread = null;
	
	GLWindow windowOffscreen;
	
	int dragPrevX;
	int dragPrevY;
	
	public ViewerContainer3D(Viewer v, Environment env, int width, int height)
	{	
		super(v,env,width,height);
		
		this.env = env;		
			
		v3d = (Viewer3D)v;
		
		GLProfile glp = GLProfile.getDefault();	       

	     try {
	            GLCapabilities caps = new GLCapabilities(glp);

	           windowOffscreen = createOffscreen(caps, width, height);
	           
	           
	            v3d.width = width;
	            v3d.height = height;

	            windowOffscreen.addGLEventListener(v3d);  //adding it to the offscreen window
	            

	        } catch (GLException e) {
	            e.printStackTrace();
	        }
		Thread t = new Thread(this);
		this.thread = t;
		t.start();
	}
	
	//this esentially calls the rendering/simulation functions of the viewers by creating background SwingWorker threads; these in turn will do the work;
	//we make sure we don't create a thread if one is already working and we don't try to do anything more often than 10ms.
	public void run() {
		
		long lastTime = new Date().getTime();
		
		while (true)
		{
			long ct = new Date().getTime();
			if (ct-lastTime < 50)
				try {
					//thread.slee(10-ct+lastTime);
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
			lastTime = new Date().getTime();
			

			windowOffscreen.display();
			
			this.finalImage = v3d.image;
			tb.viewport = v3d.viewport;
			tb.mvmatrix = v3d.mvmatrix;
			tb.projmatrix = v3d.projmatrix;
			
			
		}
		
		
	}
	
	public void setWidth(int w)
	{
		super.setWidth(w);
		windowOffscreen.setSize(w, this.getHeight());
		v3d.width = w;
	}
	public void setHeight(int h)
	{
		super.setHeight(h);
		windowOffscreen.setSize(this.getWidth(),h);
		v3d.height = h;
	}
	
	

	public BufferedImage getImage()
	{
		return v3d.image;
	}
	
	
	 public GLWindow createOffscreen(GLCapabilities caps, int w, int h) {
	        GLCapabilities capsOffscreen = (GLCapabilities) caps.clone();
	        capsOffscreen.setOnscreen(false);
	        //capsOffscreen.setPBuffer(pbuffer);
	        capsOffscreen.setDoubleBuffered(false);

	        Display nDisplay = NewtFactory.createDisplay(null); // local display
	        Screen nScreen = NewtFactory.createScreen(nDisplay, 0); // screen 0
	        com.jogamp.newt.Window nWindow = NewtFactory.createWindow(nScreen, capsOffscreen);

	        GLWindow windowOffscreen = GLWindow.create(nWindow);
	        windowOffscreen.setUpdateFPSFrames(FPSCounter.DEFAULT_FRAMES_PER_INTERVAL, System.err);
	        windowOffscreen.setSize(w, h);
	        windowOffscreen.setVisible(true);
	        return windowOffscreen;
	    }
	 
	 
	 
	 Trackball tb = new Trackball(0,0,0,5);
	 boolean zooming = false;
	
	 
		public void mouseDragged(int ex, int ey) {
			
			if (v3d.rotating)
			{			
				float[] camloc = v3d.getCameraLocation();	 
				float camdist = (float)Math.sqrt(camloc[0]*camloc[0] + camloc[1]*camloc[1] + camloc[2]*camloc[2]);
				
				tb.setRadius(camdist/2.2f);
				tb.setCenter(camloc[0], camloc[1], 0);
				 
		        tb.drag(ex, ey);
		        v3d.q = tb.rot;
			}
			else if (zooming)
			{
				
				float[] camloc = v3d.getCameraLocation();
				
				if (Math.abs(ex-dragPrevX) > Math.abs(ey- dragPrevY))
					camloc[2] *= (1+(ex-dragPrevX)/500.);
				else
					camloc[2] *= (1+(ey-dragPrevY)/500.);
				if (camloc[2] <1)
					camloc[2] = 1;
				
				System.out.println(camloc[2]);
				
				
				v3d.setCameraLocation(camloc);
			}
			else
			{
				/*float[] camloc = v3d.getCameraLocation();
				camloc[0] -= (ex-dragPrevX)/100.;
				camloc[1] += (ey-dragPrevY)/100.;
				v3d.setCameraLocation(camloc);*/
			}
			
			dragPrevX = ex;
			dragPrevY = ey;	
		}
		
		public void mousePressed(int x, int y, int button)
		{
			tb.click(x, y);
			if (button == 3)
				zooming = true;
			
			dragPrevX = x;
			dragPrevY = y;

		}
		
		public void mouseReleased(int x, int y, int button)
		{
			tb.click(x, y);
			zooming = false;

		}
		
		//for 2D viewers we add automatic zooming and panning; this can happen using the arrow keys and +,- keys; the key strokes are also sent to the viewer as interaction events
		public void keyPressed(KeyEvent e)
		{
			if (e.getKeyCode() == e.VK_R)
				v3d.rotating = true;
			else if (e.getKeyCode() == e.VK_UP)
				v3d.transy+=1;
			else if (e.getKeyCode() == e.VK_DOWN)
				v3d.transy-=1;
			else if (e.getKeyCode() == e.VK_LEFT)
				v3d.transx-=1;
			else if (e.getKeyCode() == e.VK_RIGHT)
				v3d.transx+=1;
			else if (e.getKeyCode() == e.VK_PLUS || e.getKeyCode() == 61)
				v3d.transz+=1;
			else if (e.getKeyCode() == e.VK_MINUS)
				v3d.transz-=1;
			
			//v2d.keyPressed(e.getKeyCode());
		}
		
		public void keyReleased(KeyEvent e)
		{
			if (e.getKeyCode() == e.VK_R)
				v3d.rotating = false;
		}


}