package de.tum.mw.lfe.buttons;

//------------------------------------------------------
//Revision History 'Button' application for experiments
// see
//  [1] Kremser, F., Feldhütter, A., Conti, A.S. , Krause, M. ,  An, D., Bengler, K., (2015)
//      The Influence of Target Location on Hand Movement Times while Driving. AHFE 2015
//  [2] Conti, A.S. , Kremser, F., Krause, M. ,  An, D., Bengler, K., (2015)
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class DikablisThread extends Thread {
	//dont forget: android manifest permission INTERNET 

	private static final String TAG = "Buttons.DikablisThread";
	
    public static final int RETRY_AFTER_MS = 2000;//retry to reconnect after xxx ms
    public Handler mHandler;
    public Handler mCallbackHandler;//if dikablis return data we will forward it to this handler.
    public int mCallbackHandlerWhatValue;//msg value for callback.
    public String mIp = "192.168.10.20";
    public int mPort = 2001;	    
	public static final int CONNECT = 1;	    
	public static final int COMMAND = 2;	    
	public static final int CLOSE   = 3;
	
	
	private long mLastTry;//last time we tried to log in
    private boolean mEndThread = false;
    private boolean mRunClient = true;
	private Socket mClient = null;
	private PrintWriter mOut = null;
	private BufferedReader mIn = null;
	private int mTxCounter = 0;
	private int mRxCounter = 0;
    private boolean mError = false;
	private List<String> mCommands = new ArrayList<String>();
	private long mLastDikablisFrame=0;
    
	
	public long getLastDikablisFrame(){
		return mLastDikablisFrame;
	}
	
	public boolean isConnected(){
		
		if (mClient == null) return false;
		
		if ((mClient != null) || (mClient.isConnected()) || (!mError)){
			return true;
		}else{
			return false;
		}
		
	}
	
    public void sendCommand(String command){
    	Log.d(TAG,"queued command:+ "+command);
    	synchronized(mCommands){
    		mCommands.add(command);
    	}
    }
    
    public void resetCommandQueue(){
    	synchronized(mCommands){
    		mCommands.clear();
    	}	
    }
    
    
    public void close(){
    	//synchronized(mClient){
    		try {      
			  if (mClient != null) {
				  //mClient.getOutputStream().close();
				  //mClient.getInputStream().close();
				  mOut.close();
				  mIn.close();
				  mClient.close();

			  }    
			} catch (IOException e) {
 				Log.e(TAG, "close failed: " +e.getMessage());					
			} 
    	//}
    }
    
    public void end(){
		  mRunClient = false;
		  mEndThread = true;
		  close();	
    }
    
    public void connect(String ip, int port){
    		try{
			   Log.d(TAG, "Try to connect to IP:"+ ip + " Port:" + Integer.toString(port) );			
 			   mClient = new Socket(ip, port);
 			   mClient.setTcpNoDelay(true);
 			   mOut = new PrintWriter(mClient.getOutputStream(),true);
 			   mIn = new BufferedReader(new InputStreamReader(mClient.getInputStream()));   
 			} catch(UnknownHostException e) {
 			   Log.e(TAG, "Unknown host: "+e.getMessage());
 			} catch(IOException e) {
 				Log.e(TAG, "No I/O: "+e.getMessage());
 			}	
    }
    
    @Override
    public void run() {
    	
    	Log.d(TAG, "Hello from DikablisThread");
    	
    	resetCommandQueue();
    	
    	//-------prepare handler & looper--------------
        Looper.prepare();
        
        /*
        mHandler = new Handler() {
                public void handleMessage(Message msg) {
                    // Act on the message
                	// connect to IP/port
                	if (msg.what == CONNECT){
                		//TODO implement
                	}                    	
                	// add dikablis command to command queue----
                	if (msg.what == COMMAND){
                		String command = (String)msg.obj;
                		mCommands.add(command);
                	}
                	// close
                	if (msg.what == CLOSE){
                		//TODO implement
                	
                	}
                }
        };
        Looper.loop();
        */
        
        while(!mEndThread){
        	while(mRunClient){
        		  //connect-------------------------------
	    			if ((mClient == null) || (!mClient.isConnected()) || mError){
		    			try{	
		    				close();
		    				long now = System.currentTimeMillis();
		    				if (now - mLastTry > RETRY_AFTER_MS){//wait before try again
		    					connect(mIp, mPort);
		    					mLastTry = now;
		    				}
		    				
			    			if (mClient.isConnected()){
			    				Log.d(TAG, "Try to login");
				                mOut.println("Luser,pass");
				                mOut.flush();
				                mRxCounter++;
				                //TODO evaluate server response
				                mError = false;
			    			}else{
			    				Log.d(TAG, "connect: Nope");
			    				Thread.sleep(RETRY_AFTER_MS, 0);//if connection fails wait before try again			    				
			    			}        	
		    			}catch(Exception e){
			 				Log.e(TAG, "error while connecting : "+e.getMessage());
			 				try{
		    				Thread.sleep(RETRY_AFTER_MS, 0);//if connection fails wait before try again
			 				}catch(Exception ex){}
		    			}	
	    			}
	    			
	    			//send--------------
	    			try{
	    				synchronized(mCommands){
		    				if ((mClient.isConnected()) && (mCommands.size() > 0)){
			    				String firstCommandInQueue = mCommands.get(0);
				                mOut.println(firstCommandInQueue);
				                //TODO_DLAB3 null termination for triggers
				                //mOut.print(firstCommandInQueue);
				                //mOut.print(0);
				                mOut.flush();
	    						Log.d(TAG,Integer.toString(mTxCounter)+" TX: "+firstCommandInQueue);
				                mTxCounter++;
				                mCommands.remove(0);//remove first in queue
		    				} 
	    				}
	    			}catch(Exception e){
		 				Log.e(TAG, "error in runClient send: " +e.getMessage());
		 				mError = true;
	    			}	
	    			//receive--------------
	    			try{
	    				String rxTemp = null;
	    				if (mIn != null){
	    					rxTemp = mIn.readLine(); 
	    				}
	    				if (rxTemp != null){
	    					/*
	    					if (mCallbackHandler != null){//send to callback
	    						Message msg = mCallbackHandler.obtainMessage();
	    						msg.what = mCallbackHandlerWhatValue;
	    						msg.obj = rxTemp;
	    						mCallbackHandler.sendMessage(msg);
	    					}
	    					*/
	    						    					
	    		        	String[] temp = rxTemp.split("\\t");
	    		        	try{
		    		        	if (temp[0].startsWith("J")){ 
		    		        		temp[0] = temp[0].replace("J", "");
		    		        		mLastDikablisFrame = Long.parseLong(temp[0]);
		    		        	}
	    		        	}catch(Exception ex){
	    		        		mLastDikablisFrame = -1;
	    		        		Log.e(TAG, "Failed to convert Dikablis frameCount: "+ex.getMessage());
	    		        	}
	    					
    						//Log.i(TAG,Integer.toString(mRxCounter)+" RX: "+rxTemp);
	    					mRxCounter++;
	    				}
	    			}catch(Exception e){
		 				Log.e(TAG, "error in runClient receive: "+e.getMessage());
		 				mError = true;
	    			}	
	            }
        }//while (mRunClient)
        
        close();
        
    }//while(!mEndThread)
    
    
    public String getIp(){
    	return mIp;
    }
    
    public void setIp(String ip){
    	mIp = ip;
    }
    
}









