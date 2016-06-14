package com.baniel.wirelesshid;

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Robot;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.omg.PortableServer.ServantActivator;

import com.baniel.wirelesshid.WirelessHidProto.HidData;

public class WirelessHidClient {
	
	//Robot object
	private static Robot mRobot = null;
	
	//mouse data
	private static int mPrevX = MouseInfo.getPointerInfo().getLocation().x;
	private static int mPrevY = MouseInfo.getPointerInfo().getLocation().y;
	
	public static void main(String[] args) {
		final int HID_TCP_PORT = 34567;
		Socket mSocket = null;
		InputStream is = null;
		
		HidData data = null;
		try {
			mSocket = new Socket(args[0], HID_TCP_PORT);
			is = mSocket.getInputStream();
			mRobot = new Robot();
			printClientInfo(mSocket);
			while (true) {
				data = HidData.parseDelimitedFrom(is);
				if (data != null) {
					handleData(data);
				} else {
					break;
				}
			}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		} catch (AWTException e) {
			// TODO: handle exception
			System.exit(-1);
		}
		
		System.out.println("Connection lost.");
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
