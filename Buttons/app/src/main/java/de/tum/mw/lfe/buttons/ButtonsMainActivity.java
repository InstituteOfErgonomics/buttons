package de.tum.mw.lfe.buttons;

//------------------------------------------------------
//Revision History 'Button' application for experiments
// see
//  Conti, A.S. , Krause, M. ,  An, D., Bengler, K., (2015)
//      The effect of varying target sizes and distances between target and non-target elements
//      on goal-directed hand movement times while driving. AHFE 2015
//
//------------------------------------------------------
//Version	Date			Author				Mod
//1			Feb, 2015		Michael Krause		initial
//------------------------------------------------------

/*
  Copyright (C) 2014  Michael Krause (krause@tum.de), Institute of Ergonomics, Technische Universität München

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.TimeZone;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ButtonsMainActivity extends Activity implements View.OnTouchListener{

	private static final String TAG = "Buttons.Activity";
	private static final String PREFERENCES = "buttonsPreferences";
	private Handler mHandler = new Handler();	
	private ButtonsMainActivity mContext = this;
	private PowerManager.WakeLock mWakeLock;

	
	private ServerRunnable mServerRunnable = null;
	private Thread mServerThread = null;
	private List<byte[]> mToSend = new ArrayList<byte[]>();
	public static final int PORT = 7007; // open this port
	public static final int PACKETSIZE = 32;//bytes in mToSend packets

	private TextView[] mButtons = new TextView[6];
    private int mLayout = 0;
    private long mRelease = 0;
    private File mLoggingFile = null;
    
	public static final String CSV_DELIMITER = ";"; //delimiter within csv
	public static final String CSV_LINE_END = "\r\n"; //line end in csv
	public static final String FOLDER = "BUTTONS"; //folder
	public static final String FOLDER_DATE_STR = "yyyy-MM-dd";//logging folder format
	public static final String FILE_EXT = ".txt";
	public static final String HEADER ="timestamp;rt;pos;cond;layout;button;queue;t.x;t.y;tp.x;tp.y;mid.x;mid.y;";
  
    
    public final static int LAYOUT_MAIN = 0;
    public final static int LAYOUT_1 = 1;
    public final static int LAYOUT_2 = 2;
    public final static int LAYOUT_3 = 3;
    public final static int LAYOUT_4 = 4;
    public final static int LAYOUT_5 = 5;
    public final static int LAYOUT_6 = 6;
    public final static int LAYOUT_7 = 7;
    public final static int LAYOUT_8 = 8;
    public final static int LAYOUT_9 = 9;
    
    private byte mButton = 0;
    
	//public static final byte[] BUTTON2TOUCH_REMOTE_BYTE = {'0','1','2','3','4','5','6'};//'0' work around to prevent counting from 0    
	public static final byte[] BUTTON2TOUCH_REMOTE_BYTE = {'4','5','6','1','2','3'};//'0' work around to prevent counting from 0    
	public static final char NOT_CONNECTED = 0;
	public static final char CONNECTED = 1;
	public static final char UPDATE_MARKER_TEXT = 2;    
    
    private AudioTrack mAudioTrack = null;	
    private AudioRecord mAudioRecord = null;	
	private Boolean mExternalButtonClosed = false;
	private Boolean mExternalButtonEnabled = false;	
    private AudioRecorderRunnable mAudioRecorderRunnable = null;
    private Thread mAudioRecorderThread = null;
    private int mRestoreVolume = 0;//the app stores the Music stream value onResume and restores the value onPause 
    
    private static final int OUT_SOUND_DURATION = 1; // [s]
    private static final int OUT_SAMPLE_RATE = 22050;
    private static final int OUT_SAMPLE_NUM = OUT_SOUND_DURATION  * OUT_SAMPLE_RATE;
    private short mOutSoundData[] = new short[OUT_SAMPLE_NUM];
    private static final int IN_SAMPLE_RATE = OUT_SAMPLE_RATE*2;
    private static final short IN_BUTTON_DEBOUNCE = 100;//milli sec for debouncing
    
    
	public static final int DIKABLIS_EVENT_START = 0; 
	public static final int DIKABLIS_EVENT_STOP = 1; 
	public static final int DIKABLIS_EVENT = 2; 
	
    private static DikablisThread mDikablisThread = null;
    private void kickOffDikablisThread(){
		if (mDikablisThread == null){
			Log.d(TAG, "start dikablis thread");
			mDikablisThread = new DikablisThread();
			mDikablisThread.start();
	    }
	}   
    
	
	//-------------------------------------------------------------------
    private Runnable bgHandsOff = new Runnable() {
		@Override
		public void run() {
			RelativeLayout relLayout = (RelativeLayout)findViewById(R.id.relLayout);
			relLayout.setBackgroundColor(0xffffffff);
		}
	};			
    
    private Runnable bgHandsOn = new Runnable() {
		@Override
		public void run() {
			RelativeLayout relLayout = (RelativeLayout)findViewById(R.id.relLayout);
			relLayout.setBackgroundColor(0xffaaffaa);
		}
	};			
    
	
    private Runnable rearrange = new Runnable() {
		@Override
		public void run() {
			
			
			SeekBar thresholdSB = (SeekBar)findViewById(R.id.thresholdSB);
		   	CheckBox externalButtonC = (CheckBox)findViewById(R.id.externalC);
		   	RadioButton externalButtonR = (RadioButton)findViewById(R.id.buttonClosedR);
		   	RadioButton connectedRB = (RadioButton)findViewById(R.id.connectedRB);
			TextView ip = (TextView)findViewById(R.id.ipTv);
		   	
		   	
			TextView textView1 = (TextView)findViewById(R.id.textView1);
			TextView textView2 = (TextView)findViewById(R.id.textView2);
			
			TextView dikablisIp = (TextView)findViewById(R.id.dikablisIp);
		   	
			Spinner positionS = (Spinner)findViewById(R.id.positionS);
		   	CheckBox dynamicC = (CheckBox)findViewById(R.id.dynamicC);
			
		   	if (mLayout == LAYOUT_MAIN){   		
		   		thresholdSB.setVisibility(View.VISIBLE);
		   		externalButtonC.setVisibility(View.VISIBLE);
		   		externalButtonR.setVisibility(View.VISIBLE);
		   		connectedRB.setVisibility(View.VISIBLE);
		   		ip.setVisibility(View.VISIBLE);
		   		textView1.setVisibility(View.VISIBLE);
		   		textView2.setVisibility(View.VISIBLE);

		   		dikablisIp.setVisibility(View.VISIBLE);
		   		
		   		positionS.setVisibility(View.VISIBLE);
		   		dynamicC.setVisibility(View.VISIBLE);
		   		
				for( TextView button: mButtons)
				{
					if (button != null){
						button.setVisibility(View.INVISIBLE);
					}
				}
		   		
		   	}else{
		   		thresholdSB.setVisibility(View.INVISIBLE);
		   		externalButtonC.setVisibility(View.INVISIBLE);
		   		externalButtonR.setVisibility(View.INVISIBLE);
		   		connectedRB.setVisibility(View.INVISIBLE);
		   		ip.setVisibility(View.INVISIBLE);
		   		textView1.setVisibility(View.INVISIBLE);
		   		textView2.setVisibility(View.INVISIBLE);
		   		
		   		dikablisIp.setVisibility(View.INVISIBLE);
		   		
		   		positionS.setVisibility(View.INVISIBLE);
		   		dynamicC.setVisibility(View.INVISIBLE);
		   		
		   		
				for( TextView button: mButtons )
				{
					if (button != null){
						button.setVisibility(View.VISIBLE);
					}
				}

		   	}
		   	
		   	DisplayMetrics dm = new DisplayMetrics();
		   	getWindowManager().getDefaultDisplay().getMetrics(dm);

		   	 // Display device width and height in pixels
		   	 int screenHeightpx = dm.heightPixels;
		   	 int screenWidthpx  = dm.widthPixels;

		   	 // Display device dpi value of X Y in pixels
		   	 float screenDPIx = dm.xdpi;
		   	 float screenDPIy = dm.ydpi;		   	
		   	
		   	 
		    //RotateAnimation rotate= (RotateAnimation)AnimationUtils.loadAnimation(getApplicationContext(),R.anim.rotate); 		   	 
		   	 
		   	 int size=0; // kanten länge mm
		   	 int distance=0; // abstand in mm
		   	 //size   10,15,20mm
		   	 //distance 3,5,10mm
		   	 
		   	 
		        switch(mLayout)
		        { 
		        case LAYOUT_1:
		        	size = 10;
		        	distance = 3;
		        	break;	
		        case LAYOUT_2:
		        	size = 10;
		        	distance = 5;
		        	break;	
		        case LAYOUT_3:
		        	size = 10;
		        	distance = 10;
		        	break;	
		        case LAYOUT_4:
		        	size = 15;
		        	distance = 3;
		        	break;	
		        case LAYOUT_5:
		        	size = 15;
		        	distance = 5;
		        	break;	
		        case LAYOUT_6:
		        	size = 15;
		        	distance = 10;
		        	break;
		        case LAYOUT_7:
		        	size = 20;
		        	distance = 3;
		        	break;	
		        case LAYOUT_8:
		        	size = 20;
		        	distance = 5;
		        	break;	
		        case LAYOUT_9:
		        	size = 20;
		        	distance = 10;
		        	break;	
		        	
		        default:
		        }

			 int w = Math.round((screenDPIx / 25.4f) * (float)size);
			 int h = Math.round((screenDPIy / 25.4f) * (float)size);
			 int mx = Math.round((screenDPIx / 25.4f) * (float)distance);
			 int my = Math.round((screenDPIy / 25.4f) * (float)distance);
			 //calculate top/bottom margin
			 int mtop = (screenHeightpx - (3*h + 2*my)) / 2;
			 int mleft = (screenWidthpx - (2*w + 1*mx)) / 2;

			 Log.i(TAG, ">>> Screen screenHeightpx:" + screenHeightpx +" screenWidthpx:"+screenWidthpx+" screenDPIx:"+screenDPIx+" screenDPIy:"+screenDPIy);
			 Log.i(TAG, ">>> Layout:"+mLayout+" w:" + w +" h:"+h+" mx:"+mx+" my:"+my);

		    for(int i = 0; i< 6; i++){
			    //mButtons[i].clearAnimation();
			    
		    	mButtons[i].setTextSize(TypedValue.COMPLEX_UNIT_PX, 18);
		    	mButtons[i].setBackgroundColor(Color.BLACK);
		    	mButtons[i].setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);

		    	RelativeLayout.LayoutParams param = (LayoutParams) mButtons[i].getLayoutParams();
		    	param.width = w;
		    	param.height = h;
		    	param.topMargin = mtop + (i%3)*(h + my);
		    	param.leftMargin = mleft + (i/3)*(w + mx);
		    	mButtons[i].setLayoutParams(param);
			    //mButtons[i].startAnimation(rotate);
		    	
		    }
		   	
		   	/*
	      	RelativeLayout layout = (RelativeLayout) getView().findViewById(R.id.relLayout); 
	      	
	      	ImageView hv = (ImageView)getView().findViewById(HEUER);

	      	float factor = oversampling*hv.getMeasuredHeight()/100.0f;
	      	int bitmapSize = layout.getMeasuredHeight();          	
	      	
	      	Bitmap hb = Bitmap.createBitmap(oversampling*bitmapSize, oversampling*bitmapSize, Bitmap.Config.ARGB_8888);
	      	Canvas hc = new Canvas(hb);
	      	
	        Paint paint = new Paint(); 
	      	paint.setStyle(Paint.Style.STROKE);
	      	paint.setFlags(Paint.ANTI_ALIAS_FLAG);
	      	
	      	
	      	paint.setStrokeWidth(14*factor); 
	  		paint.setColor(Color.RED);
	  		RectF rectF = new RectF(20*factor, 20*factor, 80*factor, 80*factor);
	  		//hc.drawOval(rectF, paint);
	  		paint.setColor(Color.GRAY);
	  		hc.drawArc (rectF, -90, greenAngle, false, paint);//green arc		   	
		   	
			*/
		} 			    	
    };    
    
    
	//-------------------------------------------------------------------
	
	
	
    class ServerRunnable implements Runnable {
    	private CommunicationThread commThread;
    	private Thread thread;
    	private ServerSocket mServerSocket;
    	private Socket mClientSocket;
    	
    	public ServerRunnable(){
            try {
            	if (mServerSocket == null) mServerSocket = new ServerSocket(PORT);
            } catch (Exception e) {
            	Log.e(TAG,"ServerThread failed on open port: "+ e.getMessage());
            }
    	}
 
        public void run() {
            
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(1000);
                    mClientSocket = mServerSocket.accept();
                    commThread = new CommunicationThread(mClientSocket);
                    thread = new Thread(commThread);
                    thread.start();
                } catch (Exception e) {
                	if(!Thread.currentThread().isInterrupted()){//only log error if this was not an intentional interrupt
                		Log.e(TAG,"ServerThread failed on accept connection: "+ e.getMessage());
                	}	
                }
            }//while
            
            closeSockets();
            
        }//run
        
        //helpers
        public void closeSockets(){
            try{
                if (mServerSocket != null) mServerSocket.close();
                if (mClientSocket != null) mClientSocket.close();
              } catch (Exception e) {
            	  Log.e(TAG,"ServerThread failed to close sockets: "+ e.getMessage());
              }
        }
        
    	public String ipStatus(){
    		String tempStr = "";
    	      if((mServerSocket != null) && (mServerSocket.isBound()) ){
    		       tempStr += getIpAddress() + ":"+PORT;
    		      }else{
    			       tempStr += "-----";	    	  
    		      }
    	      return tempStr;
    	}
    	
    	public String getIpAddress(){
    	    try {
    	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
    	            NetworkInterface intf = en.nextElement();
    	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
    	                InetAddress inetAddress = enumIpAddr.nextElement();
    	                if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
    	                    return inetAddress.getHostAddress().toString();
    	                }
    	            }
    	        }
    	    } catch (Exception e) {
    			Log.e(TAG, "getIpAddress() failed: " + e.getMessage());
    	    }
    	    return "---";
    	}	
    	
   
    }
    
//-------------------------------------------------------------------

	class CommunicationThread implements Runnable {

		private Socket clientSocket;
		private BufferedReader input;
		//private BufferedWriter output;
		private OutputStream output;

		
		public CommunicationThread(Socket clientSocket) {
			this.clientSocket = clientSocket;

			try {
				this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
				//this.output = new BufferedWriter(new OutputStreamWriter(this.clientSocket.getOutputStream()));
				this.output = this.clientSocket.getOutputStream();
			} catch (Exception e) {
            	Log.e(TAG,"CommunicationThread failed on create streams: "+ e.getMessage());
			}
		}

		public void run() {
			
		    Message msg = mGuiHandler.obtainMessage();
		    msg.what = CONNECTED;
		    mGuiHandler.sendMessage(msg);

			int read;
			while ((!Thread.currentThread().isInterrupted()) && (!clientSocket.isClosed())) {

                try {
                    SystemClock.sleep(1);//adjust to your needs
                }catch(Exception e){}

				try {
					if(input.ready()){
						read = input.read();							
					}else{
						read =-1;
					}
					if (read != -1){							  
					  for (byte i=0; i < BUTTON2TOUCH_REMOTE_BYTE.length; i++){
						 if (read == BUTTON2TOUCH_REMOTE_BYTE[i]){
							  mButton = i;
						      Message msg2 = mGuiHandler.obtainMessage();
						      msg2.what = UPDATE_MARKER_TEXT;
						      mGuiHandler.sendMessage(msg2);

						 }
					 } //for
					  
				        switch(read)
				        { 
				        case 'A':
				        	changeToLayout(LAYOUT_1);
				        	break;	
				        case 'S':
				        	changeToLayout(LAYOUT_2);
				        	break;	
				        case 'D':
				        	changeToLayout(LAYOUT_3);
				        	break;	
				        case 'F':
				        	changeToLayout(LAYOUT_4);
				        	break;	
				        case 'G':
				        	changeToLayout(LAYOUT_5);
				        	break;	
				        case 'H':
				        	changeToLayout(LAYOUT_6);
				        	break;	
				        case 'J':
				        	changeToLayout(LAYOUT_7);
				        	break;	
				        case 'K':
				        	changeToLayout(LAYOUT_8);
				        	break;	
				        case 'L':
				        	changeToLayout(LAYOUT_9);
				        	break;	
				        	
				        default:
				        }
					     
					}//if
					
					
											
					//output

					synchronized(mToSend){//sync against append and clear
						if(mToSend.size() > 0){
							Log.d(TAG,"Send");
							output.write(mToSend.get(0),0,PACKETSIZE);//send first in queue
							//TODO_DLAB3 output.write(mToSend.get(0),0,mToSend.get(0).length);//send first in queue
							output.flush();
							mToSend.remove(0);//remove first from queue
							
						}
					}//sync
					
					
				} catch (Exception e) {
	            	Log.e(TAG,"CommunicationThread failed while input/output: "+ e.getMessage());
	            	Thread.currentThread().interrupt();
				}
				
			}//while
			try{	
				input.close();
				output.close();
			} catch (Exception e) {
            	Log.e(TAG,"CommunicationThread failed on closing streams: "+ e.getMessage());
			}
		    Message msg2 = mGuiHandler.obtainMessage();
		    msg2.what = NOT_CONNECTED;
		    mGuiHandler.sendMessage(msg2);

		}//run

	}
//-------------------------------------------------------------------
	final Handler mGuiHandler = new Handler(){
		  @Override
		  public void handleMessage(Message msg) {
			super.handleMessage(msg);
			    
			RadioButton connectedRB = (RadioButton)findViewById(R.id.connectedRB);

		    if(msg.what==NOT_CONNECTED){
				connectedRB.setChecked(false);
		    }
		    if(msg.what==CONNECTED){
				connectedRB.setChecked(true);
			}		    
		    if(msg.what==UPDATE_MARKER_TEXT){
		    	String temp = new String(Byte.toString(mButton));//convert char to string
		    	connectedRB.setText(temp);

			    for(int i = 0; i< 6; i++){
			    	if (i == mButton){
				    	mButtons[i].setBackgroundColor(Color.BLACK);			    		
				    	//mButtons[i].setBackgroundColor(Color.RED);//TODO only for test: change color
			    	}else{
				    	mButtons[i].setBackgroundColor(Color.BLACK);			    		
			    	}
			    }
			    
			}
			TextView ip = (TextView)findViewById(R.id.ipTv);
			ip.setText(mServerRunnable.ipStatus());

		  }
		};  
    
	//-------------------------------------------------------------------
	   public class AudioRecorderRunnable implements Runnable {
		   		    
		    private static final boolean OPEN = false;// external button open
		    private static final boolean CLOSED = true;// external button closed		   
	        private boolean endRecording = false;
	        private ButtonsMainActivity parent = null;
	        private long externalButtonLastPressed=0; 
	        private long externalButtonLastReleased=0;
	        private long avgAudioLevel=0;
	        private int musicVolume=0;
	        private boolean waitingForRelease = false;
	        
	        public AudioRecorderRunnable(ButtonsMainActivity a){
	        	parent = a;
	        	
	        	try{
	    			AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE); 
	    			musicVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)/2;//set music volume to 30%
	        	}catch(Exception e){
	        		Log.e(TAG, "getStreamMaxVolume() failed: "+e.getMessage());
	        	}
	        	
      	setMusicVolume(musicVolume);

	        }
	        
	        public int getMusicVolume(){
	        	int vol = 0;
	        	try{
	    			AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE); 
	    	        vol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
	    	        
	        	}catch(Exception e){
	        		Log.e(TAG, "getMusicVolume() failed: "+e.getMessage());
	        	}
	        	return vol;

	        } 		        
	        
	        public void setMusicVolume(int vol){
	        	try{
	    			AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE); 
	    	        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0);
	    	        
	        	}catch(Exception e){
	        		Log.e(TAG, "setMusicVolume() failed: "+e.getMessage());
	        	}

	        } 		        
	        
	        public int getAvgAudioLevel(){
	        	return (int)avgAudioLevel;
	        }
	        public void end(){
	        	endRecording = true;
	        }
	        
	        public void releaseRecorder(){
	            if (mAudioRecord != null) {
	                try {
	                	mAudioRecord.stop();
	                	mAudioRecord.release();

	                } catch (Exception e) {
	                    Log.e(TAG, "release mAudioRecord failed: "+e.getMessage());
	                }
	                mAudioRecord = null;
	            }
	        	
	        }
	    	
	        public void run() {
	            int minBufferSize;
	            short[] inSoundData;

	            try {
	                minBufferSize = AudioRecord.getMinBufferSize(IN_SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);

	                mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, IN_SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
	                inSoundData = new short[minBufferSize];
	                mAudioRecord.startRecording();

	        		short[] previous_in_abs_temp = new short[4];
	        		
	                while (!endRecording) {
	                	int read = mAudioRecord.read(inSoundData, 0, inSoundData.length);
	                	long sum = 0;
	                	long count = 0;
	            		int avg_in_abs_temp;

	                	if (read > 0){//no error
	                		long now = SystemClock.uptimeMillis();
	                		//long now = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(); 
	                		short in_abs_temp;

	                		int threshold_pressed = parent.getThreshold();
	                		int threshold_released = parent.getThreshold() -parent.getThreshold()/2;//we lower the released threshold by 50%, kind of hysteresis
      				
	                		for (int i=0; i < read; i++){
	                			in_abs_temp = (short)Math.abs(inSoundData[i]);
	                			sum += in_abs_temp;
	                			// do some weak averaging, if we captured the zero crossing
	                			previous_in_abs_temp[3] = previous_in_abs_temp[2];
	                			previous_in_abs_temp[2] = previous_in_abs_temp[1];
	                			previous_in_abs_temp[1] = previous_in_abs_temp[0];
	                			previous_in_abs_temp[0] = in_abs_temp;
	                			avg_in_abs_temp = (previous_in_abs_temp[0] + previous_in_abs_temp[1] + previous_in_abs_temp[2] + previous_in_abs_temp[3])/4;
	                			
	            				//when the sample was taken:
	            				//  assumed reference is 'now' 
	            				//  the first sample was ('read'*1000)/ IN_SAMPLE_RATE  ago in time
	            				long timeOfThisSample = now - ((read-i)*1000/ IN_SAMPLE_RATE);
	            				
	            				
	                			if (avg_in_abs_temp > threshold_pressed){//button pressed
	                				count++;
	                				
	                				if ( (mExternalButtonClosed == OPEN) &&
	                					 ((timeOfThisSample - externalButtonLastPressed ) > IN_BUTTON_DEBOUNCE) &&
	                					 ((timeOfThisSample - externalButtonLastReleased) > IN_BUTTON_DEBOUNCE))//DEBUGED!!!
	                					 {//debounce
	                						//button pressed EVENT
	                						externalButtonLastPressed = timeOfThisSample;
	                						mExternalButtonClosed = CLOSED;
	                						//Log.i(TAG,"button closed "+Long.toString(count) +" read:" +Integer.toString(read));
	                						//parent.response(timeOfThisSample, true);//log as external button result
	                                		//Log.d(TAG,"---------------------------------pressed " + Long.toString(now));
	
	                				}//else discard as 'bouncing'
	                			}	
	                   			if (avg_in_abs_temp < threshold_released){//button released
	                				
	                				if ( (mExternalButtonClosed == CLOSED) &&
	                					 ((timeOfThisSample - externalButtonLastPressed ) > IN_BUTTON_DEBOUNCE) && //DEBUGGED!!!
	                   					 ((timeOfThisSample - externalButtonLastReleased) > IN_BUTTON_DEBOUNCE)){//debounce
	                   						//button pressed EVENT
	                						externalButtonLastReleased = timeOfThisSample;
	                						mExternalButtonClosed = OPEN;
	                						//Log.i(TAG,"button open");
	                   						//parent.buttonReleased()
	                						buttonReleased(timeOfThisSample);
	                   				}//else discard as 'bouncing'
	                				
	                			}
	                			
	                		}
	                		//Log.i(TAG, "audio level: "+Float.toString(sum/(float)read));
	                		avgAudioLevel = sum/read;
	                		parent.refreshGui();
	                	}
	                	
	                	if (getMusicVolume() != musicVolume) setMusicVolume(musicVolume);
	                	
	                	
	                	if  (
	                		  (externalButtonLastReleased < externalButtonLastPressed) &&
	                		  (!waitingForRelease)
	                		  ){
	                		
	                		//mHandler.removeCallbacks(bgGrey);
	                		mHandler.post(bgHandsOn);
	                		
	                		waitingForRelease = true;
	                	}
	                	
	                	if (waitingForRelease && (mExternalButtonClosed == OPEN)){
	                		mHandler.post(bgHandsOff);
	                		waitingForRelease = false;
	                	} 
	                	
	                	
	                }//while
	                
	                releaseRecorder();

	            } catch (Exception e) {
	                Log.e(TAG, "audio recording runnable failed: "+e.getMessage());
	            }

	        }
	    } 
	   //--------------------------------------
	
		public void startServer(){
			if (mServerRunnable == null){
				mServerRunnable	= new ServerRunnable();
			}
			if (mServerThread == null){
			    mServerThread = new Thread(mServerRunnable);
			    mServerThread.start();
			}
			
			TextView ip = (TextView)findViewById(R.id.ipTv);
		    ip.setText(mServerRunnable.ipStatus());
		}
		
		public void stopServer(){
	        try {
	        	mServerThread.interrupt();
	        	mServerRunnable.closeSockets();
	        } catch (Exception e) {
				Log.e(TAG, "mServerThread.interrupt() failed: " + e.getMessage());
	        }
		}
		
    @Override
    protected void onStop() {
        super.onStop();
        
        stopServer();
    }		
       
   	@Override
   	public void onDestroy() {
           super.onDestroy();	
           
		   TextView ipTv = (TextView)findViewById(R.id.dikablisIp);	
		   ipTv.removeTextChangedListener(textWatcher);

           if(mWakeLock != null){
             	mWakeLock.release();
            }
  		
  	}	
   	public TextWatcher textWatcher = new TextWatcher(){
        public void afterTextChanged(Editable s) {
			TextView dikablisIp = (TextView)findViewById(R.id.dikablisIp);
			if ((dikablisIp != null) && (mDikablisThread != null)){
	        	String ip = dikablisIp.getText().toString();
		        mDikablisThread.setIp(ip);
				toasting(ip,300);

			}	
        }
        public void beforeTextChanged(CharSequence s, int start, int count, int after){}
        public void onTextChanged(CharSequence s, int start, int before, int count){


        }

    };	       
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        //no title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,  WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
        //full light
        android.provider.Settings.System.putInt(getContentResolver(), android.provider.Settings.System.SCREEN_BRIGHTNESS, 255); 
	 				
		
		setContentView(R.layout.activity_main);
		//Helpers.onActivityCreateSetLayout(this);
		
		
		//load from preferences
        SharedPreferences settings = mContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        int audioThreshold = settings.getInt("audioThreshold", Short.MAX_VALUE/4);

        String ip = settings.getString("dikablisIp", "192.168.0.1");
        
		TextView ipTv = (TextView)findViewById(R.id.dikablisIp);	
		ipTv.setText(ip);
		kickOffDikablisThread();
        mDikablisThread.setIp(ip);
		ipTv.addTextChangedListener(textWatcher); 			

        
        //if (Helpers.isMainLayout()){
        
		   	CheckBox externalButtonC = (CheckBox)findViewById(R.id.externalC);
		   	mExternalButtonEnabled = settings.getBoolean("mExternalButtonEnabled", false);
		   	externalButtonC.setChecked(mExternalButtonEnabled);
		   	
	   	    SeekBar thresholdSB = (SeekBar)findViewById(R.id.thresholdSB);
			thresholdSB.setMax(Short.MAX_VALUE);
			thresholdSB.setProgress(audioThreshold);
			thresholdSB.setProgressDrawable(getResources().getDrawable(R.drawable.audiothreshold));
        //}
			

		    getWakeLock();
			
		    RelativeLayout rl = (RelativeLayout) findViewById(R.id.relLayout);
			if (rl != null){
				rl.setOnTouchListener(this);			
			}
			
	 
		    
		    for(int i = 0; i< 6; i++){
		    	//mButtonsParam[i] =  new RelativeLayout.LayoutParams(10,10);
		    	mButtons[i] = new TextView(getApplicationContext());
		    	//mButtons[i].setText(Integer.toString(i+1));		    	
		    	mButtons[i].setVisibility(View.INVISIBLE);

			    rl.addView(mButtons[i]);
		    	
		    }
			 /*
			for( int button: mButtons )
			{
				Button b = (Button)findViewById(button);
				if (b != null){
					b.setOnTouchListener(this);
				}
			}
			*/
		
				
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
			
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.main, menu);
		 menu.add(Menu.NONE, LAYOUT_MAIN, Menu.NONE, "Main");
		 menu.add(Menu.NONE, LAYOUT_1, Menu.NONE, "S10 M3");
		 menu.add(Menu.NONE, LAYOUT_2, Menu.NONE, "S10 M5");
		 menu.add(Menu.NONE, LAYOUT_3, Menu.NONE, "S10 M10");
		 menu.add(Menu.NONE, LAYOUT_4, Menu.NONE, "S15 M3");
		 menu.add(Menu.NONE, LAYOUT_5, Menu.NONE, "S15 M5");
		 menu.add(Menu.NONE, LAYOUT_6, Menu.NONE, "S15 M10");
		 menu.add(Menu.NONE, LAYOUT_7, Menu.NONE, "S20 M3");
		 menu.add(Menu.NONE, LAYOUT_8, Menu.NONE, "S20 M5");
		 menu.add(Menu.NONE, LAYOUT_9, Menu.NONE, "S20 M10");
		
		return true;
	}
	
   @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
        case LAYOUT_MAIN:
        	changeToLayout(LAYOUT_MAIN);
        	return true;  
        case LAYOUT_1:
        case LAYOUT_2:
        case LAYOUT_3:
        case LAYOUT_4:
        case LAYOUT_5:
        case LAYOUT_6:
        case LAYOUT_7:
        case LAYOUT_8:
        case LAYOUT_9:
        	changeToLayout(item.getItemId());
        	return true;  
        default:
            return super.onOptionsItemSelected(item);
        }
    }	


	@Override
	public void onResume() {
       super.onResume();
       
       mLoggingFile = prepareLogging();
       
       startServer();//is also called in onCreate,
       
       if(mExternalButtonEnabled){
       	enableExternalButton();//turn on sound out, to detect external button press
       }
       
       kickOffDikablisThread();       
       
	}	

	
	@Override
	public void onPause() {
       super.onPause();
       
       saveToPrefs();
       
       if(mExternalButtonEnabled){
       	disableExternalButton();//turn off recording, restore sound , etc.
       }   
       
	    if (mDikablisThread != null){
	    	mDikablisThread.end();
	    	mDikablisThread = null;
	    }      
       
	}   
   
	private void saveToPrefs(){
			//save changes to app preferences
		    SharedPreferences settings = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
		    SharedPreferences.Editor editor = settings.edit();
		    editor.putBoolean("mExternalButtonEnabled", mExternalButtonEnabled);
		    editor.putString("dikablisIp", mDikablisThread.getIp());
		    SeekBar thresholdSB = (SeekBar)findViewById(R.id.thresholdSB);
		    if (thresholdSB != null){
		    	editor.putInt("audioThreshold", thresholdSB.getProgress());
		    }
		    editor.commit();	
	}

	private void toasting(final String msg, final int duration){
		Context context = getApplicationContext();
		CharSequence text = msg;
		Toast toast = Toast.makeText(context, text, duration);
		toast.show();		
	}	
	
   protected void getWakeLock(){
	    try{
			PowerManager powerManger = (PowerManager) getSystemService(Context.POWER_SERVICE);
	        mWakeLock = powerManger.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP|PowerManager.FULL_WAKE_LOCK, "de.tum.ergonomie.buttons");
	        mWakeLock.acquire();
		}catch(Exception e){
       	Log.e(TAG,"get wakelock failed:"+ e.getMessage());
		}	
   }
   
	   
public void enableExternalButton(){
	mExternalButtonEnabled= true;
	refreshGui();
	
 getMusicVolume();
 startSoundOut();
 startSoundInRec();	
}	   
public void disableExternalButton(){
	mExternalButtonEnabled= false;
	refreshGui();

	
 restoreMusicVolume();
 stopSoundOut();        
 stopSoundInRec();	
}	   
	    


void getMusicVolume(){//get music volume so we can restore later
	try{
		AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		mRestoreVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
	}catch(Exception e){
		Log.e(TAG, "getVolume() failed: "+e.getMessage());
	}	
}

void restoreMusicVolume(){
	try{
		AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
     audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mRestoreVolume, 0);
	}catch(Exception e){
		Log.e(TAG, "restoreMusicVolume() failed: "+e.getMessage());
	}	
}

private void startSoundInRec(){
 mAudioRecorderRunnable = new AudioRecorderRunnable(this);
 mAudioRecorderThread = new Thread(mAudioRecorderRunnable);
 mAudioRecorderThread.start();
 
}	

private void stopSoundInRec(){
 if (mAudioRecorderRunnable != null){
 	mAudioRecorderRunnable.end();
 	mAudioRecorderRunnable = null;
 }   	    
}

private void stopSoundOut(){
 if (mAudioTrack != null){
 	mAudioTrack.stop(); 
 	mAudioTrack.release();
 }
}

private void startSoundOut(){
	
	int n=0;//index
	
	 for (int i = 0; i < OUT_SAMPLE_NUM; ++i) {
	 	if (i%2 == 0){//even
	 		mOutSoundData[n++] = Short.MAX_VALUE;
	 	}else{//odd
	 		mOutSoundData[n++] = Short.MIN_VALUE;
	 	}
	 }
		
	 mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, OUT_SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, mOutSoundData.length*2, AudioTrack.MODE_STATIC);
	 mAudioTrack.write(mOutSoundData, 0, mOutSoundData.length);
	mAudioTrack.setLoopPoints(0, OUT_SAMPLE_NUM-2, -1);
	 mAudioTrack.play();
	 	 
	 //ensure audioTrack volume is on
	 float max = mAudioTrack.getMaxVolume();
	 mAudioTrack.setStereoVolume(max, max);

}	

public int getThreshold(){
	int ret = -1;
	    SeekBar thresholdSB = (SeekBar)findViewById(R.id.thresholdSB);
	    ret = thresholdSB.getProgress();
	return ret;
}

public void  toggleExternalButton(View v){//is called by click on enable external Bbtton check box
	mExternalButtonEnabled = !(mExternalButtonEnabled);
	if(mExternalButtonEnabled){
		enableExternalButton();
	}else{
		disableExternalButton();
	}
	
   	CheckBox externalButtonC = (CheckBox)findViewById(R.id.externalC);	
   	externalButtonC.setChecked(mExternalButtonEnabled);	
}
//-------------------------------------------------------------------

public void	refreshGui(){//refresh/set enabled statuses of buttons etc.
	mHandler.post(new refresh());
}

class refresh implements Runnable {//called by audio recording runnable to refresh threshold progressbar

	@Override
	public void run() {
		//if(Helpers.isMainLayout()) {
			SeekBar thresholdSB = (SeekBar)findViewById(R.id.thresholdSB);
  			if (mAudioRecorderRunnable != null) thresholdSB.setSecondaryProgress(mAudioRecorderRunnable.getAvgAudioLevel());//visualize audio level
   			thresholdSB.setMax(Short.MAX_VALUE);
   			if (mAudioRecorderRunnable != null) thresholdSB.setSecondaryProgress(mAudioRecorderRunnable.getAvgAudioLevel());//visualize audio level

  			
		   	RadioButton externalButtonR = (RadioButton)findViewById(R.id.buttonClosedR);
		   	externalButtonR.setChecked(mExternalButtonClosed);
		//} 		
		
	}
}

private  void changeToLayout(int layout)
{

	mLayout = layout;

   	mHandler.post(rearrange);
   	
  	
}


public void writeToFile(byte[] data, File file){
  		
		if (data == null){//error
		Log.e(TAG,"writeFile() data==null?!");
   		finish();//we quit. we will not continue without file logging
		}
		
	FileOutputStream dest = null; 
						
	try {
		dest = new FileOutputStream(file, true);
		dest.write(data);
	}catch(Exception e){
		Log.e(TAG,"writeFile() failed. msg: " + e.getMessage());
   		finish();//we quit. we will not continue without file logging
		
	}finally {
		try{
			dest.flush();
			dest.close();
		}catch(Exception e){}
	}
	
	return;
}
public File  prepareLogging(){

	File file = null;
	File folder = null;
	SimpleDateFormat  dateFormat = new SimpleDateFormat(FOLDER_DATE_STR);
	String folderTimeStr =  dateFormat.format(new Date());
	String timestamp = Long.toString(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis());
   try{
	   //try to prepare external logging
	   String folderStr = Environment.getExternalStorageDirectory () + File.separator + FOLDER + File.separator + folderTimeStr;
	   file = new File(folderStr, timestamp + FILE_EXT);
	   folder = new File(folderStr);
	   folder.mkdirs();//create missing dirs
	   file.createNewFile();
	   if (!file.canWrite()) throw new Exception();
   }catch(Exception e){
	   try{
		   Log.e(TAG,"maybe no SD card inserted");//toast
		   finish();//we quit. we will not continue without file logging

		   //we do not log to internal memory, its not so easy to get the files back, external is easier via usb mass storage
		   /*
		   //try to prepare internal logging
			File intfolder = getApplicationContext().getDir("data", Context.MODE_WORLD_WRITEABLE);
			String folderStr = intfolder.getAbsolutePath() + File.separator + folderTimeStr;
			toasting("logging internal to: " +folderStr, Toast.LENGTH_LONG);
			file = new File(folderStr, timestamp + FILE_EXT);
		    folder = new File(folderStr);
		    folder.mkdirs();//create missing dirs
			file.createNewFile();
			if (!file.canWrite()) throw new Exception();
			*/
	   }catch(Exception e2){
		   file= null;
    	   Log.e(TAG,"exception during prepareLogging(): " + e2.getMessage());//toast
		   finish();//we quit. we will not continue without file logging
	   } 
   
   }
   
   try{
	String header = HEADER +  getVersionString() + CSV_LINE_END;
    byte[] headerBytes = header.getBytes("US-ASCII");
	writeToFile(headerBytes,file);
   }catch(Exception e3){
	   Log.e(TAG,"error writing header: "+e3.getMessage());//toast
	   finish();//we quit. we will not continue without file logging
   }

   return file;
}	

	
InetAddress getBroadcastAddress() throws IOException {
    WifiManager wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
    DhcpInfo dhcp = wifi.getDhcpInfo();
    // handle null somehow

    int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
    byte[] quads = new byte[4];
    for (int k = 0; k < 4; k++)
      quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
    return InetAddress.getByAddress(quads);
}


public void send2Silab(long timestamp, int rt, PointF touch, Point targetMid){

	/*
	 * packet PACKETSIZE =32
	4 int timestamp
	4 int rt
	1 b condition
	1 b position
	1 b layout
	1 b button
	4 int touch.x
	4 int touch.y
	4 int target.x
	4 int target.y
	4 int queue	
	 */
	
   	byte position = getPosition();

   	byte condition = getCondition();
  	
	
	byte[] bytes = new byte[PACKETSIZE];
	ByteBuffer bytebuffer = ByteBuffer.wrap(bytes);
	//bytebuffer.order(ByteOrder.BIG_ENDIAN);
	bytebuffer.putInt((int)timestamp);
	bytebuffer.putInt(rt);
	bytebuffer.put((byte)condition);
	bytebuffer.put((byte)position);
	bytebuffer.put((byte)mLayout);
	bytebuffer.put((byte)mButton);

	
	bytebuffer.putInt((int)touch.x);
	bytebuffer.putInt((int)touch.y);
	bytebuffer.putInt(targetMid.x);
	bytebuffer.putInt(targetMid.y);
			
	synchronized(mToSend){//sync against send and clear
		
		bytebuffer.putInt(mToSend.size());//how many packets are quequed befoe this packet
		
		//TODO_DLAB3 comment this out if you want to send string datat to DLAB3
		mToSend.add(bytes);//queque this packet
		
		//Log.i(TAG,"queued packets: " +Integer.toString(mToSend.size()));
	}//sync	
}


public void log(long timestamp, int rt, PointF touch, PointF touchPrec){

    	RelativeLayout.LayoutParams param = (LayoutParams) mButtons[mButton].getLayoutParams();
    	int w = param.width;
    	int h = param.height;
    	int left = param.leftMargin;    	
    	int top = param.topMargin;
	
    	Point targetMid = new Point(0,0);
    	targetMid.x = left + w/2;
    	targetMid.y = top + h/2;
    			
		send2Silab(timestamp, rt, touch, targetMid);
		
	   
    	//toasting(Integer.toString(rt),1000);

		
	   try{
		   
		   //InetAddress temp = getBroadcastAddress();
		   //Log.d(TAG,"-----------" + temp.toString());
			String dataLine =   Long.toString(timestamp) +
								CSV_DELIMITER +	Integer.toString(rt) +
								CSV_DELIMITER +	Integer.toString(getPosition()) +
								CSV_DELIMITER +	Integer.toString(getCondition()) +
								CSV_DELIMITER +	Integer.toString(mLayout) + 
								CSV_DELIMITER +	Integer.toString(mButton) + 
								CSV_DELIMITER +	Integer.toString(mToSend.size()) +
								CSV_DELIMITER +	Float.toString(touch.x) + 
								CSV_DELIMITER +	Float.toString(touch.y) + 
								CSV_DELIMITER +	Float.toString(touchPrec.x) + 
								CSV_DELIMITER +	Float.toString(touchPrec.y) + 
								CSV_DELIMITER +	Integer.toString(targetMid.x) + 
								CSV_DELIMITER +	Integer.toString(targetMid.y) +
								CSV_LINE_END;
			
		    byte[] dataLineBytes = dataLine.getBytes("US-ASCII");
			writeToFile(dataLineBytes,mLoggingFile);
			
			//TODO_DLAB3
			//for logging data stream we want to transmitt the data line (string) not the binary data to dlab3
			//dont forget to delete/comment other mToSend.add() in send2silab that will send binary data

			/*
			synchronized(mToSend){//sync against send and clear
				
				
				mToSend.add(dataLineBytes);//queque this packet
				
			}//sync
			*/	
			
	   }catch(Exception e){
		   Log.e(TAG,"error writing header: "+e.getMessage());//toast
		   finish();//we quit. we will not continue without file logging
	   }	
	
}

//implementation of onTouch for buttonDown events
public boolean onTouch(View v, MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN ) {
    	
    	if (mLayout != LAYOUT_MAIN){
	    	long timestampDown = event.getDownTime();
	    	int rt =  (int)(timestampDown - mRelease);
	    	    	
	    	sendDikablisTrigger(DIKABLIS_EVENT_STOP);
	    	
	    	PointF touch = new PointF(0,0); 
	    	touch.x = event.getX(); 
	    	touch.y = event.getY(); 
	    	
	    	PointF touchPrec = new PointF(0,0);
	    	touchPrec.x = event.getXPrecision();
	    	touchPrec.y = event.getYPrecision();
	    	
	    	log(timestampDown, rt, touch, touchPrec);
    	}	
    	/*
    	//dispatch
        switch (v.getId()) {
        case R.id.relLayout:
            break;
        default: 
        	 break;
        }
    	 */
    	
        return false;
    }
    return false;
}	


private void sendDikablisTrigger(int eventType){//Dikablis2 trigger
	
	String triggerString = "";
	//-----------------------------
    switch(eventType){
    case DIKABLIS_EVENT_START:
        triggerString += "ES";
    	break;
    case  DIKABLIS_EVENT_STOP:
        triggerString += "EE";
    	break;
    case  DIKABLIS_EVENT:
        triggerString += "EP";
    	break;
    default:
       triggerString += "EP";
    } 	
    
   	
   	byte position = getPosition();

   	byte condition = getCondition();

    
	//-----------------------------
    triggerString += String.format("%02d", condition); //static/dynamic
	//-----------------------------
    triggerString += String.format("%02d", position); //location
	//-----------------------------
    triggerString += String.format("%02d", mLayout);  //layout        
	//-----------------------------
    triggerString += String.format("%02d", mButton);  //        

    
	 if (mDikablisThread != null){
		 mDikablisThread.sendCommand(triggerString);
	 }
	 
}



private String getVersionString(){
	String retString = "";
	String appVersionName = "";
	int appVersionCode = 0;
	try{
		appVersionName = getPackageManager().getPackageInfo(getPackageName(), 0 ).versionName;
		appVersionCode= getPackageManager().getPackageInfo(getPackageName(), 0 ).versionCode;
	}catch (Exception e) {
		Log.e(TAG, "getVersionString failed: "+e.getMessage());
	 }
	
	retString = "V"+appVersionName+"."+appVersionCode;
	
	return retString;
}		   
   
public void buttonReleased(long timestamp){

	mRelease = timestamp;
	
	sendDikablisTrigger(DIKABLIS_EVENT_START);
	
}



	

public byte getCondition(){
	CheckBox dynamicC = (CheckBox)findViewById(R.id.dynamicC);
	
	byte condition = 0;
	if (dynamicC != null){
		 if (dynamicC.isChecked()){
			condition = 2;//dynamic exp condition
		}else{
			condition = 1;//static exp condition
		}
	}
	return condition;
}	
	

public byte getPosition(){
	Spinner positionS = (Spinner)findViewById(R.id.positionS);
	
	byte position = 0;
	if (positionS != null){
		 position = (byte)(positionS.getSelectedItemPosition()+1);
	}
	return position;
}	

}
