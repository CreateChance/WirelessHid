package com.baniel.wirelesshid;

/**
 * Created by baniel on 6/12/16.
 */
public class Constant {

    public static final int HID_TCP_PORT = 34567;
    public static final int HID_MULTICAST_PORT = 34568;
    public static final int HID_MULTICAST_RECEIVE_PORT = 34569;

    public static final String HID_MULTICAST_ADDRESS = "224.0.0.1";

    public static final String HID_SERVICE_DISCOVERY_REQ = "WIRELESSHID_ANDROID";
    public static final String HID_SERVICE_DISCOVERY_RSP = "WIRELESSHID_PC";
    public static final String HID_SERVICE_DISCONNECT = "WIRELESSHID_DISCONNECT";

    //code of mouse right and left button, copy from jdk1.7 doc.
    public static final int MOUSE_BUTTON_LEFT                           = 16;
    public static final int MOUSE_BUTTON_RIGHT                          = 4;
}
