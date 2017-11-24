package nodomain.freeyourgadget.gadgetbridge.devices.wakeuptech;

import java.util.UUID;

public final class wakeuptechConstants {
    public static final UUID UUID_CHARACTERISTIC_CONTROL = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID UUID_CHARACTERISTIC_MEASURE = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID UUID_SERVICE_WAKEUPTECH = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID UUID_SERVICE_WAKEUPTECHSTEP = UUID.fromString("0000fee7-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_STEP_MEASURE = UUID.fromString("0000fee7-0000-1000-8000-00805f9b34fb");

    public static final byte CMD_SET_DATE_AND_TIME = 0x08;
    public static final byte CMD_SET_HEARTRATE_AUTO = 0x38;
    public static final byte CMD_SET_HEARTRATE_WARNING_VALUE = 0x01;
    public static final byte CMD_SET_TARGET_STEPS = 0x03;
    public static final byte CMD_SET_ALARM = (byte) 0x73;
    public static final byte CMD_GET_STEP_COUNT = 0x1D;
    public static final byte CMD_GET_SLEEP_TIME = 0x32;
    public static final byte CMD_SET_NOON_TIME = 0x26;
    public static final byte CMD_SET_SLEEP_TIME = 0x27;
    public static final byte CMD_SET_DND_SETTINGS = 0x39;
    public static final byte CMD_SET_INACTIVITY_WARNING_TIME = 0x24;
    public static final byte CMD_ACTION_HEARTRATE_SWITCH = 0x0D;
    public static final byte CMD_ACTION_SHOW_NOTIFICATION = 0x2C;
    public static final byte CMD_ACTION_REBOOT_DEVICE = 0x0E;

    public static final byte RECEIVE_BATTERY_LEVEL = (byte) 0x91;
    public static final byte RECEIVE_DEVICE_INFO = (byte) 0x92;
    public static final byte RECEIVE_STEPS_DATA = (byte) 0xF9;
    public static final byte RECEIVE_LOCATEPHONE = (byte) 0x7d;

    public static final byte RECIEVE_SENOR_DATA = (byte) 0x31;
    // Heart Rate
    public static final byte RECEIVE_HEARTRATE_TIMED = (byte) 0x09;
    public static final byte RECEIVE_HEARTRATE_REALTIME = (byte) 0x0a;
    // Blood Oxygen
    public static final byte RECEIVE_BLOODOXYGEN_TIMED = (byte) 0x11;
    public static final byte RECEIVE_BLOODOXYGEN_REALTIME = (byte) 0x12;
    // Blood Pressure
    public static final byte RECEIVE_BLOODPRESSURE_TIMED = (byte) 0x21;
    public static final byte RECEIVE_BLOODPRESSURE_REALTIME = (byte) 0x22;
    // Heartrate, BloodOxygen, and BloodPressure
    public static final byte RECIEVE_ALLVITALS = (byte) 0x32;


    public static final byte[] CMD_FINDBRACELET = {(byte) 0xab, (byte) 0x00, (byte) 0x03, (byte) 0xff, (byte) 0x71, (byte) 0x80};
    public static final byte[] CMD_CLOCKMODE12Hour = {(byte) 0xab, (byte) 0x00, (byte) 0x04, (byte) 0xff, (byte) 0x7c, (byte) 0x80, (byte) 0x01};
    public static final byte[] CMD_CLOCKMODE24Hour = {(byte) 0xab, (byte) 0x00, (byte) 0x04, (byte) 0xff, (byte) 0x7c, (byte) 0x80, (byte) 0x00};

    public static final byte ICON_CALL = 0;
    public static final byte ICON_SMS = 1;
    public static final byte ICON_WECHAT = 2;
    public static final byte ICON_QQ = 3;
    public static final byte ICON_FACEBOOK = 4;
    public static final byte ICON_SKYPE = 5;
    public static final byte ICON_TWITTER = 6;
    public static final byte ICON_WHATSAPP = 7;
    public static final byte ICON_LINE = 8;
}