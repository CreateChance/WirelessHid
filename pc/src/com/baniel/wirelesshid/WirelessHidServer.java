package com.baniel.wirelesshid;

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Robot;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import com.baniel.wirelesshid.WirelessHidProto.HidData;

public class WirelessHidServer {
	
	//Robot object
	private static Robot mRobot = null;
	
	//mouse data
	private static int mPrevX = MouseInfo.getPointerInfo().getLocation().x;
	private static int mPrevY = MouseInfo.getPointerInfo().getLocation().y;
	
	private static MulticastSocket multicastSocket = null;
	private static InputStream is = null;
	private static ServerSocket mServerSocket = null;
	private static InetAddress group = null;
	private static Socket mSocket = null;
	
	public static void main(String[] args) {
		
		HidData data = null;
		
		while (true) {
			// listen for UDP multicast packet and then response to it.
			try {
				multicastSocket = new MulticastSocket(Constant.HID_MULTICAST_PORT);
				group = InetAddress.getByName(Constant.HID_MULTICAST_ADDRESS);
				multicastSocket.joinGroup(group);
				
				DatagramPacket packet;
				byte[] buf = new byte[256];
		        packet = new DatagramPacket(buf, buf.length);
		        
		        while (true) {
		            System.out.println("listen for service discover msg.");
		            multicastSocket.receive(packet);
		            
		            String msg = new String(packet.getData()).trim();
		            if (Constant.HID_SERVICE_DISCOVERY_REQ.equals(msg)) {
		            	DatagramPacket pk = new DatagramPacket(Constant.HID_SERVICE_DISCOVERY_RSP.getBytes(), 
		            			Constant.HID_SERVICE_DISCOVERY_RSP.length(), group, Constant.HID_MULTICAST_PORT);
		            	multicastSocket.send(pk);
		            	break;
		            } else {
		            	System.out.println("This package is not valid packet, just ignore it. \n");
		            	packet.setData(new byte[256]);
		            }
		        }
				
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			try {
				
				System.out.println("I'm waiting for connecting.");
		        mServerSocket = new ServerSocket(Constant.HID_TCP_PORT);
		        mServerSocket.setReuseAddress(true);
		        new Thread(new Runnable() {
		        	int tryTimes = 0;
					public void run() {
						while (mSocket == null) {
							// mSocket is null, that means has no connection now.
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
	
							try {
								if (tryTimes >= 5) {
									// we have tried five time, and we failed.
									mServerSocket.close();
									break;
								} else {
									DatagramPacket pk = new DatagramPacket(Constant.HID_SERVICE_DISCOVERY_RSP.getBytes(), 
					            			Constant.HID_SERVICE_DISCOVERY_RSP.length(), group, Constant.HID_MULTICAST_PORT);
					            	multicastSocket.send(pk);
					            	tryTimes++;
								}
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}).start();
		        mSocket = mServerSocket.accept();
				
				is = mSocket.getInputStream();
				mRobot = new Robot();
				printClientInfo(mSocket);
				
				getDisconnectListenThread().start();
				while (true) {
					data = HidData.parseDelimitedFrom(is);
					if (data != null) {
						handleData(data);
					} else {
						break;
					}
				}
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (AWTException e) {
			} finally {
				try {
					mServerSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			mSocket = null;
			System.out.println("Connection lost.");
		}
	}
	
	private static void printClientInfo(Socket socket) {
		System.out.println("Client connected!");
		System.out.println("remote info: " + socket.getRemoteSocketAddress());
	}
	
	private static void handleData(HidData data) {
		if (data == null) {
			return;
		}
		
		int type = data.getType().getNumber();
		switch (type) {
		case HidData.DataType.MOUSE_MOVE_VALUE:
			doMouseMove(data.getXShift(), data.getYShift());
			break;
		case HidData.DataType.MOUSE_CLICK_PRESS_VALUE:
			doMousePress(data.getMouseKeyValue());
			break;
		case HidData.DataType.MOUSE_CLICK_RELEASE_VALUE:
			doMouseRelease(data.getMouseKeyValue());
			break;
		case HidData.DataType.MOUSE_SCROLL_VALUE:
			doMouseScroll(data.getMouseScroll());
			break;
		case HidData.DataType.KEYBOARD_HIT_VALUE:
			doKeyHit(data.getKeyboardValue());
			break;
		case HidData.DataType.KEYBOARD_LONG_PRESS_VALUE:
			doKeyLongPress(data.getKeyboardValue());
			break;
		case HidData.DataType.KEYBOARD_LONG_RELEASE_VALUE:
			doKeyLongRelease(data.getKeyboardValue());
			break;

		default:
			break;
		}
		
	}
	
	private static Thread getDisconnectListenThread() {
		return new Thread(new Runnable() {
			
			@Override
			public void run() {
				while (true) {
					DatagramPacket packet = new DatagramPacket(new byte[256], 256);
					try {
						multicastSocket.receive(packet);
						
						String msg = new String(packet.getData()).trim();
						
						if (Constant.HID_SERVICE_DISCONNECT.equals(msg)) {
							is.close();
							System.out.println("receive disconnect msg, so disconnect it.");
							break;
						}
					} catch (IOException e) {
						e.printStackTrace();
						try {
							is.close();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						break;
					}
				}
			}
		});
	}
	
	//move mouse pointer to posX,posY of current position.
	private static void doMouseMove(int posX, int posY) {
		mRobot.mouseMove(mPrevX + posX, mPrevY + posY);
		mPrevX += posX;
		mPrevY += posY;
	}
	
	//handle mouse button press.
	private static void doMousePress(int keyValue) {
		//System.out.println("mouse click value = " + keyValue);
		mRobot.mousePress(keyValue);
	}
	
	private static void doMouseRelease(int keyValue) {
		mRobot.mouseRelease(keyValue);
	}
	
	private static void doMouseScroll(int amt) {
		mRobot.mouseWheel(amt);
	}
	
	private static void doKeyHit(int keyCode) {
		mRobot.keyPress(keyCode);
		mRobot.keyRelease(keyCode);
	}
	
	private static void doKeyLongPress(int keyCode) {
		mRobot.keyPress(keyCode);
	}
	
	private static void doKeyLongRelease(int keyCode) {
		mRobot.keyRelease(keyCode);
	}
}
