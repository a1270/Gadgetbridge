/*  Copyright (C) 2017 Andreas Shimokawa, Sami Alaoui

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.wakeuptech;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.net.Uri;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;
import java.util.Arrays;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.wakeuptech.wakeuptechConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.wakeuptech.e26Coordinator;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBAlarm;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class E26Support extends AbstractBTLEDeviceSupport {

    private static final Logger LOG = LoggerFactory.getLogger(E26Support.class);

    public BluetoothGattCharacteristic ctrlCharacteristic = null;
    public BluetoothGattCharacteristic measureCharacteristic = null;
    public BluetoothGattCharacteristic measureStepCharacteristic = null;

    private final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
    private final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();

    public E26Support() {
        super(LOG);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ACCESS);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ATTRIBUTE);
        addSupportedService(wakeuptechConstants.UUID_SERVICE_WAKEUPTECH);
        addSupportedService(wakeuptechConstants.UUID_SERVICE_WAKEUPTECHSTEP);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        LOG.info("Initializing");

        gbDevice.setState(GBDevice.State.INITIALIZING);
        gbDevice.sendDeviceUpdateIntent(getContext());

        measureCharacteristic = getCharacteristic(wakeuptechConstants.UUID_CHARACTERISTIC_MEASURE);
        ctrlCharacteristic = getCharacteristic(wakeuptechConstants.UUID_CHARACTERISTIC_CONTROL);
        measureStepCharacteristic = getCharacteristic(wakeuptechConstants.UUID_STEP_MEASURE);

        builder.setGattCallback(this);
        builder.notify(measureCharacteristic, true);

        syncSettings(builder);

        gbDevice.setState(GBDevice.State.INITIALIZED);
        gbDevice.sendDeviceUpdateIntent(getContext());

        LOG.info("Initialization Done");

        return builder;
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        if (super.onCharacteristicChanged(gatt, characteristic)) {
            return true;
        }

        UUID characteristicUUID = characteristic.getUuid();
        byte[] data = characteristic.getValue();
        //Remove header
        byte[] Headerless = Arrays.copyOfRange(data, 4, data.length);
        if (data.length == 0)
            return true;

        switch (Headerless[0]) {
            case wakeuptechConstants.RECEIVE_DEVICE_INFO:
                int fwVerNumMajor = data[6] & 0xFF;
                float fwVerNumMinor = data[7] & 0xFF;
                // Fix this. Brain not worky
                float fwVerMerged = fwVerNumMajor + ((fwVerNumMinor % 100) / 100);
                versionCmd.fwVersion = String.valueOf(fwVerMerged);
                handleGBDeviceEvent(versionCmd);
                LOG.info("Firmware version is: " + versionCmd.fwVersion);
                return true;
            case wakeuptechConstants.RECEIVE_BATTERY_LEVEL:
                batteryCmd.level = data[7];
                byte ChargeStatus = data[6];
                handleGBDeviceEvent(batteryCmd);
                LOG.info("Battery level is: " + batteryCmd.level);
                return true;
            case wakeuptechConstants.RECIEVE_SENOR_DATA:
                LOG.info("Headerless[1]: " + Headerless[1]);
                switch (Headerless[1]) {
                    case wakeuptechConstants.RECEIVE_HEARTRATE_TIMED:
                        LOG.info("Timed heart rate: " + data[6]);
                        GB.toast(getContext(), "Current heart rate:" + data[6], Toast.LENGTH_LONG, GB.INFO);
                        return true;
                    case wakeuptechConstants.RECEIVE_HEARTRATE_REALTIME:
                        LOG.info("Realtime heart rate: " + data[6]);
                        GB.toast(getContext(), "Current heart rate:" + data[6], Toast.LENGTH_LONG, GB.INFO);
                        return true;
                    case wakeuptechConstants.RECEIVE_BLOODOXYGEN_TIMED:
                        LOG.info("Timed bloodoxygen: " + data[6]);
                        GB.toast(getContext(), "Current bloodoxygen:" + data[6] + "%", Toast.LENGTH_LONG, GB.INFO);
                        return true;
                    case wakeuptechConstants.RECEIVE_BLOODOXYGEN_REALTIME:
                        LOG.info("Realtime bloodoxygen: " + data[6]);
                        GB.toast(getContext(), "Current bloodoxygen: " + data[6] + "%", Toast.LENGTH_LONG, GB.INFO);
                        return true;
                    case wakeuptechConstants.RECEIVE_BLOODPRESSURE_TIMED:
                        LOG.info("Timed bloodpressure: " + data[6] + "/" + data[7] + "mg");
                        GB.toast(getContext(), "Current bloodpressure: " + data[6] + "/" + data[7] + "mg", Toast.LENGTH_LONG, GB.INFO);
                        return true;
                    case wakeuptechConstants.RECEIVE_BLOODPRESSURE_REALTIME:
                        LOG.info("Realtime bloodpressure: " + data[6] + "/" + data[7] + "mg");
                        GB.toast(getContext(), "Current bloodpressure: " + data[6] + "/" + data[7] + "mg", Toast.LENGTH_LONG, GB.INFO);
                        return true;
                    default:
                        LOG.info("Unhandled characteristic change: " + characteristicUUID + " code: " + String.format("0x%1x ...", data[0]));
                        return true;

                }
            case wakeuptechConstants.RECEIVE_LOCATEPHONE:
                int locatephone = data[6];
                LOG.info("Recieved locate phone command:" + locatephone);
                GB.toast(getContext(), "Recieved locate phone command:" + data[6], Toast.LENGTH_LONG, GB.INFO);
                return true;
            case wakeuptechConstants.RECIEVE_ALLVITALS:
                int heartRate = data[7];
                int bloodOxygen = data[8];
                int bloodPressure = data[9];
                int bloodPressure2 = data[10];
                LOG.info("Recieved all vitals:");
                GB.toast(getContext(), String.format("Recieved all vitals:%s", data[6]), Toast.LENGTH_LONG, GB.INFO);
                return true;
            default:
                LOG.info("Unhandled characteristic change: " + characteristicUUID + " code: " + String.format("0x%1x ...", data[0]));
                return true;
        }
    }

    private void syncDateAndTime(TransactionBuilder builder) {
        Calendar cal = Calendar.getInstance();
        byte month = (byte) (cal.get(Calendar.MONTH) + 1); // bug?
        byte day = (byte) cal.get(Calendar.DAY_OF_MONTH);
        byte hour = (byte) cal.get(Calendar.HOUR_OF_DAY);
        byte minute = (byte) cal.get(Calendar.MINUTE);
        byte second = (byte) cal.get(Calendar.SECOND);
        byte[] SyncDateCMD = {(byte) 0xab, (byte) 0x00, (byte) 0x0b, (byte) 0xff, (byte) 0x93, (byte) 0x80, (byte) 0x00, (byte) 0x07, (byte) 0xe1, (byte) month, (byte) day, (byte) hour, (byte) minute, (byte) second};
        builder.write(ctrlCharacteristic, SyncDateCMD);
    }

    private void syncUserPrefs(TransactionBuilder builder) {
        byte unit = e26Coordinator.getUnit(getDevice().getAddress());
        byte age = e26Coordinator.getUserAge();
        byte height = e26Coordinator.getUserHeight();
        byte weight = e26Coordinator.getUserWeight();
        byte stepTrgt = (byte) e26Coordinator.getGoal();
        byte stepLength = 0x47;
        //Only supports metric as of now.
// There be dragons.
//        if(unit == 0x00)
//        {
//            height = (byte)(Math.ceil((double)height * 0.39370079));
//            weight = (byte)(Math.ceil((double)weight * 2.20462262));
//            stepLength = 0x1c;
//            LOG.info("Weight2: " + (byte) weight);
//        }

        byte[] SyncDataCMD = {(byte) 0xab, (byte) 0x00, (byte) 0x11, (byte) 0xff, (byte) 0x74, (byte) 0x80, (byte) stepLength, (byte) age, (byte) height, (byte) weight, (byte) unit, (byte) stepTrgt, (byte) 0x5a, (byte) 0x82, (byte) 0x3c, (byte) 0x5a, (byte) 0x3c, (byte) 0x64, (byte) 0x5d, (byte) 0x64};
        builder.write(ctrlCharacteristic, SyncDataCMD);
        GB.toast(getContext(), "Writing users prefs to device.", Toast.LENGTH_LONG, GB.INFO);

    }

    private void syncSettings(TransactionBuilder builder) {
        syncDateAndTime(builder);
        syncUserPrefs(builder);
        // Set clock to 12h format. Find better way to do this
        builder.write(ctrlCharacteristic, wakeuptechConstants.CMD_CLOCKMODE12Hour);
        // Set clock to 24h format.
        //builder.write(ctrlCharacteristic, wakeuptechConstants.CMD_CLOCKMODE24Hour);
    }

    private void showNotification(byte icon, String title, String message) {
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
    }

    @Override
    public void onDeleteNotification(int id) {

    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        try {
            TransactionBuilder builder = performInitialized("SetAlarms");

            //device supports 8 alarms
            //Default to one-time for now

            byte AlarmType = (byte) 0x80;
            for (int i = 0; i < alarms.size(); i++) {
                Calendar cal = alarms.get(i).getAlarmCal();
                if (alarms.get(i).isEnabled()) {
                    byte ALARMHOUR = (byte) (alarms.get(i).getAlarmCal().get(Calendar.HOUR_OF_DAY));
                    byte ALARMMIN = (byte) alarms.get(i).getAlarmCal().get(Calendar.MINUTE);
                    byte AlarmNumb = (byte) (i + 1);
                    byte[] AlarmCmd = {(byte) 0xab, (byte) 0x00, (byte) 0x08, (byte) 0xff, (byte) 0x73, (byte) 0x80, (byte) AlarmNumb, (byte) 0x01, (byte) ALARMHOUR, (byte) ALARMMIN, (byte) AlarmType};
                    builder.write(ctrlCharacteristic, AlarmCmd);
                } else if (!alarms.get(i).isEnabled()) {
                    byte ALARMHOUR = (byte) (alarms.get(i).getAlarmCal().get(Calendar.HOUR_OF_DAY));
                    byte ALARMMIN = (byte) alarms.get(i).getAlarmCal().get(Calendar.MINUTE);
                    byte AlarmNumb = (byte) (i + 1);
                    byte[] AlarmCmd = {(byte) 0xab, (byte) 0x00, (byte) 0x08, (byte) 0xff, (byte) 0x73, (byte) 0x80, (byte) AlarmNumb, (byte) 0x00, (byte) ALARMHOUR, (byte) ALARMMIN, (byte) AlarmType};
                    builder.write(ctrlCharacteristic, AlarmCmd);
                }
            }
            performConnected(builder.getTransaction());
        } catch (IOException e) {
            LOG.warn(e.getMessage());
        }
    }

    @Override
    public void onSetTime() {
        try {
            TransactionBuilder builder = performInitialized("SetTime");
            syncDateAndTime(builder);
            performConnected(builder.getTransaction());
        } catch (IOException e) {
            LOG.warn(e.getMessage());
        }
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
    }

    @Override
    public void onSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) {

    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {

    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {

    }

    @Override
    public void onEnableRealtimeSteps(boolean enable) {
    }

    @Override
    public void onInstallApp(Uri uri) {

    }

    @Override
    public void onAppInfoReq() {

    }

    @Override
    public void onAppStart(UUID uuid, boolean start) {

    }

    @Override
    public void onAppDelete(UUID uuid) {

    }

    @Override
    public void onAppConfiguration(UUID appUuid, String config, Integer id) {

    }

    @Override
    public void onAppReorder(UUID[] uuids) {

    }

    @Override
    public void onFetchActivityData() {

    }

    @Override
    public void onReboot() {
    }

    @Override
    public void onHeartRateTest() {
        try {
            TransactionBuilder builder = performInitialized("HeartRateTest");
            byte[] EnableHeartRateSingle = {(byte) 0xab, (byte) 0x00, (byte) 0x04, (byte) 0xff, (byte) 0x31, (byte) 0x09, (byte) 0x01};
            byte[] DisableHeartRateSingle = {(byte) 0xab, (byte) 0x00, (byte) 0x04, (byte) 0xff, (byte) 0x31, (byte) 0x09, (byte) 0x00};

            builder.write(ctrlCharacteristic, EnableHeartRateSingle);
            builder.wait(40000);
            builder.write(ctrlCharacteristic, DisableHeartRateSingle);
            performConnected(builder.getTransaction());
            GB.toast(getContext(), "Single Heartrate test takes 40 seconds.", Toast.LENGTH_LONG, GB.INFO);
        } catch (Exception e) {
            LOG.warn(e.getMessage());
        }
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {
        try {
            TransactionBuilder builder = performInitialized("RealTimeHeartMeasurement");
            byte Enabled = (byte) (enable ? 1 : 0);
            byte[] HeartRateRealtime = {(byte) 0xab, (byte) 0x00, (byte) 0x04, (byte) 0xff, (byte) 0x31, (byte) 0x09, (byte) Enabled};

            builder.write(ctrlCharacteristic, HeartRateRealtime);
            performConnected(builder.getTransaction());
            GB.toast(getContext(), "Real time heart rate.", Toast.LENGTH_LONG, GB.INFO);
        } catch (Exception e) {
            LOG.warn(e.getMessage());
        }
    }

    @Override
    public void onSendConfiguration(String config) {
        TransactionBuilder builder;
        try {
            builder = performInitialized("Sending configuration for option: " + config);
            switch (config) {
                case SettingsActivity.PREF_MEASUREMENT_SYSTEM:
                    syncUserPrefs(builder);
                    break;
                case ActivityUser.PREF_USER_HEIGHT_CM:
                    syncUserPrefs(builder);
                    break;
                case ActivityUser.PREF_USER_YEAR_OF_BIRTH:
                    syncUserPrefs(builder);
                    break;
                case ActivityUser.PREF_USER_WEIGHT_KG:
                    syncUserPrefs(builder);
                    break;
                case ActivityUser.PREF_USER_STEPS_GOAL:
                    syncUserPrefs(builder);
                    break;
            }
            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast("Error setting configuration", Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    @Override
    public void onFindDevice(boolean start) {
        try {
            TransactionBuilder builder = performInitialized("findMe");

            setFindMe(builder, start);
            performConnected(builder.getTransaction());
        } catch (IOException e) {
            GB.toast(getContext(), "Error toggling Find Me: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }

    }

    @Override
    public void onSetConstantVibration(int integer) {

    }

    @Override
    public void onScreenshotReq() {

    }

    @Override
    public void onEnableHeartRateSleepSupport(boolean enable) {

    }

    @Override
    public void onSetHeartRateMeasurementInterval(int seconds) {

    }

    @Override
    public void onAddCalendarEvent(CalendarEventSpec calendarEventSpec) {

    }

    @Override
    public void onDeleteCalendarEvent(byte type, long id) {

    }

    public void onBloodOxygenTest() {
        try {
            TransactionBuilder builder = performInitialized("BloodOxygenTest");
            byte[] EnableHeartRateSingle = {(byte) 0xab, (byte) 0x00, (byte) 0x04, (byte) 0xff, (byte) 0x31, (byte) 0x11, (byte) 0x01};
            byte[] DisableHeartRateSingle = {(byte) 0xab, (byte) 0x00, (byte) 0x04, (byte) 0xff, (byte) 0x31, (byte) 0x11, (byte) 0x00};

            builder.write(ctrlCharacteristic, EnableHeartRateSingle);
            builder.wait(40000);
            builder.write(ctrlCharacteristic, DisableHeartRateSingle);
            performConnected(builder.getTransaction());
            GB.toast(getContext(), "Single bloodoxygen test takes 40 seconds.", Toast.LENGTH_LONG, GB.INFO);
        } catch (Exception e) {
            LOG.warn(e.getMessage());
        }
    }

    public void onReturnAllVitals() {
        try {

        } catch (Exception e) {
            LOG.warn((e.getMessage()));
        }
    }

    @Override
    public void onTestNewFunction() {
        try {
            TransactionBuilder builder = performInitialized("BloodPressureTest");
            byte[] EnableHeartRateSingle = {(byte) 0xab, (byte) 0x00, (byte) 0x04, (byte) 0xff, (byte) 0x31, (byte) 0x21, (byte) 0x01};
            byte[] DisableHeartRateSingle = {(byte) 0xab, (byte) 0x00, (byte) 0x04, (byte) 0xff, (byte) 0x31, (byte) 0x21, (byte) 0x00};

            builder.write(ctrlCharacteristic, EnableHeartRateSingle);
            builder.wait(60000);
            builder.write(ctrlCharacteristic, DisableHeartRateSingle);
            performConnected(builder.getTransaction());
            GB.toast(getContext(), "Single bloodpressure test takes 60 seconds.", Toast.LENGTH_LONG, GB.INFO);
        } catch (Exception e) {
            LOG.warn(e.getMessage());
        }
    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {

    }

    private E26Support setFindMe(TransactionBuilder transaction, boolean state) {
        transaction.write(ctrlCharacteristic, wakeuptechConstants.CMD_FINDBRACELET);
        return this;
    }
}
