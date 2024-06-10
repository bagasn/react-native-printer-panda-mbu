package com.printerpanda;

import static com.facebook.react.bridge.UiThreadUtil.runOnUiThread;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;

import net.posprinter.posprinterface.IMyBinder;
import net.posprinter.posprinterface.ProcessData;
import net.posprinter.posprinterface.TaskCallback;
import net.posprinter.service.PosprinterService;
import net.posprinter.utils.BitmapProcess;
import net.posprinter.utils.BitmapToByteData;
import net.posprinter.utils.DataForSendToPrinterPos58;
import net.posprinter.utils.DataForSendToPrinterPos80;
import net.posprinter.utils.DataForSendToPrinterTSC;
import net.posprinter.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ReactNativePrinterPandaModule extends ReactContextBaseJavaModule {

    private static Context context;
    private static BluetoothAdapter BluetoothService;
    private final ReactApplicationContext reactContext;
    public static boolean ISCONNECT = false;
    public static IMyBinder myBinder;

    public static final int WIDTH_58 = 384;
    public static final int WIDTH_80 = 576;
    private int deviceWidth = WIDTH_58;

    ServiceConnection mSerconnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            myBinder = (IMyBinder) service;
            //show("Connected to printer", Toast.LENGTH_LONG);
            Log.e("myBinder", "connect");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e("myBinder", "disconnect");
        }
    };

    public ReactNativePrinterPandaModule(ReactApplicationContext reactContext ) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "ReactNativePrinterPanda";
    }

    @ReactMethod
    public void sampleMethod(String stringArgument, int numberArgument, Callback callback) {
        // TODO: Implement some actually useful functionality
        callback.invoke("Received numberArgument: " + numberArgument + " stringArgument: " + stringArgument);
    }

    private List<String> btList = new ArrayList<>();
    private ArrayList<String> btFoundList = new ArrayList<>();
    private ArrayAdapter<String> BtBoudAdapter, BtfoundAdapter;
    private View BtDialogView;
    private ListView BtBoundLv, BtFoundLv;
    private LinearLayout ll_BtFound;
    private AlertDialog btdialog;
    private Button btScan;
    //private DeviceReceiver BtReciever;
    private BluetoothAdapter bluetoothAdapter;


    @ReactMethod
    public void checkBluetooth() {
        //This method will check and connect to printer or nor.
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            reactContext.startActivityForResult(intent, 1, null);

        } else {
            //show("bluetooth enabled", Toast.LENGTH_SHORT);
//      BtReciever=new DeviceReceiver(btFoundList,BtfoundAdapter,BtFoundLv);
            Intent intent = new Intent(reactContext, PosprinterService.class);
            reactContext.bindService(intent, mSerconnection, Context.BIND_AUTO_CREATE);

        }
    }

    @ReactMethod
    @SuppressLint("MissingPermission")
    public void findAvailableDevice(Promise promise) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        btList.clear();
        if (!bluetoothAdapter.isEnabled() && bluetoothAdapter != null) {
            //show("Bluetooth disabled", Toast.LENGTH_SHORT);
            //checkBluetooth();
        } else {
            Set<BluetoothDevice> device = bluetoothAdapter.getBondedDevices();
            WritableArray dataBt = new WritableNativeArray();
            Integer key = 0;
            if (((Set) device).size() > 0) {
                //存在已经配对过的蓝牙设备
                for (Iterator<BluetoothDevice> it = device.iterator(); it.hasNext(); ) {
                    BluetoothDevice btd = it.next();
                    //btList.add(btd.getName()+'\n'+btd.getAddress());
                    btList.add(btd.getAddress());
                    //dataBt.putString(String.valueOf("bt-"+key), btd.getAddress());
                    dataBt.pushString(String.valueOf(btd.getName() + "=" + btd.getAddress()));
                    //BtBoudAdapter.notifyDataSetChanged();
                    key++;
                }
                //connectBT(btList.get(1));
                promise.resolve(dataBt);
//      printBarcode();
                //show(btList.get(1), Toast.LENGTH_LONG);
            } else {  //不存在已经配对过的蓝牙设备
                btList.add("No can be matched to use bluetooth");
                BtBoudAdapter.notifyDataSetChanged();
            }
        }
    }

    @ReactMethod
    private void connectPrinter(String address, final Promise promise) {
        String a = address.trim();

        Intent intent = new Intent(reactContext, PosprinterService.class);
        reactContext.bindService(intent, mSerconnection, Context.BIND_AUTO_CREATE);
        //show(a, Toast.LENGTH_LONG);
        if (a.equals(null) || a.equals("")) {
            //show("Failed", Toast.LENGTH_SHORT);
        } else {
            //show("onn here success"+a, Toast.LENGTH_SHORT);
            myBinder.ConnectBtPort(a, new TaskCallback() {
                @Override
                public void OnSucceed() {
                    ISCONNECT = true;
                    //show("Sucess", Toast.LENGTH_SHORT);
                    promise.resolve(true);
                    //printBarcode();
                }

                @Override
                public void OnFailed() {
                    ISCONNECT = false;
                    //promise.resolve("Error");
                    promise.resolve(false);
                    //show("Failed error", Toast.LENGTH_SHORT);
                }
            });
        }
    }

    @ReactMethod
    private void printText(final String printStr, final Promise promise) {
        if (ISCONNECT) {
            Intent intent = new Intent(reactContext, PosprinterService.class);
            reactContext.bindService(intent, mSerconnection, Context.BIND_AUTO_CREATE);
            myBinder.WriteSendData(new TaskCallback() {
                @Override
                public void OnSucceed() {
                    //show("Success", Toast.LENGTH_LONG);
                    promise.resolve(true);
                }

                @Override
                public void OnFailed() {
                    //show("Failed", Toast.LENGTH_LONG);
                    promise.resolve(false);
                }
            }, new ProcessData() {
                @Override
                public List<byte[]> processDataBeforeSend() {
                    List<byte[]> list = new ArrayList<>();
                    list.add(DataForSendToPrinterPos58.initializePrinter());
                    list.add(DataForSendToPrinterPos58.selectAlignment(1));
                    list.add(StringUtils.strTobytes(printStr));
                    list.add(DataForSendToPrinterPos58.printAndFeedForward(5));
                    return list;
                }
            });

        } else {
            //show("Failed", Toast.LENGTH_LONG);
            promise.resolve(false);
        }

    }

    @ReactMethod
    public void printPic(String base64encodeStr, final Promise promise) {

        byte[] bytes = Base64.decode(base64encodeStr, Base64.DEFAULT);
        final Bitmap mBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        final Bitmap bitmap1 =  BitmapProcess.compressBmpByYourWidth
                (mBitmap,200);

        if (ISCONNECT) {
            Intent intent = new Intent(reactContext, PosprinterService.class);
            reactContext.bindService(intent, mSerconnection, Context.BIND_AUTO_CREATE);
            myBinder.WriteSendData(new TaskCallback() {
                @Override
                public void OnSucceed() {
                    //show("Success", Toast.LENGTH_LONG);
                    promise.resolve(true);
                }

                @Override
                public void OnFailed() {
                    //show("Failed", Toast.LENGTH_LONG);
                    promise.resolve(false);
                }
            }, new ProcessData() {
                @Override
                public List<byte[]> processDataBeforeSend() {
                    List<byte[]> list = new ArrayList<>();
                    list.add(DataForSendToPrinterPos80.selectAlignment(2));
                    list.add(DataForSendToPrinterPos80.initializePrinter());
                    
                    List<Bitmap> blist= new ArrayList<>();
                    blist = BitmapProcess.cutBitmap(50,bitmap1);
                    for (int i= 0 ;i<blist.size();i++){
                        list.add(DataForSendToPrinterPos80.printRasterBmp(0,blist.get(i), BitmapToByteData.BmpType.Threshold, BitmapToByteData.AlignType.Center,300));
                    }
//                    list.add(StringUtils.strTobytes("1234567890qwertyuiopakjbdscm nkjdv mcdskjb"));
                    list.add(DataForSendToPrinterPos80.printAndFeedLine());
                    return list;
                }
            });

        } else {
            //show("Failed", Toast.LENGTH_LONG);
            promise.resolve(false);
        }


    }


    private static WritableMap convertJsonToMap(JSONObject jsonObject) throws JSONException {
        WritableMap map = new WritableNativeMap();

        Iterator<String> iterator = jsonObject.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject) {
                map.putMap(key, convertJsonToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                map.putArray(key, convertJsonToArray((JSONArray) value));
            } else if (value instanceof Boolean) {
                map.putBoolean(key, (Boolean) value);
            } else if (value instanceof Integer) {
                map.putInt(key, (Integer) value);
            } else if (value instanceof Double) {
                map.putDouble(key, (Double) value);
            } else if (value instanceof String) {
                map.putString(key, (String) value);
            } else {
                map.putString(key, value.toString());
            }
        }
        return map;
    }

    private static WritableArray convertJsonToArray(JSONArray jsonArray) throws JSONException {
        WritableArray array = new WritableNativeArray();

        for (int i = 0; i < jsonArray.length(); i++) {
            Object value = jsonArray.get(i);
            if (value instanceof JSONObject) {
                array.pushMap(convertJsonToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                array.pushArray(convertJsonToArray((JSONArray) value));
            } else if (value instanceof Boolean) {
                array.pushBoolean((Boolean) value);
            } else if (value instanceof Integer) {
                array.pushInt((Integer) value);
            } else if (value instanceof Double) {
                array.pushDouble((Double) value);
            } else if (value instanceof String) {
                array.pushString((String) value);
            } else {
                array.pushString(value.toString());
            }
        }
        return array;
    }

    private static JSONObject convertMapToJson(ReadableMap readableMap) throws JSONException {
        JSONObject object = new JSONObject();
        ReadableMapKeySetIterator iterator = readableMap.keySetIterator();
        while (iterator.hasNextKey()) {
            String key = iterator.nextKey();
            switch (readableMap.getType(key)) {
                case Null:
                    object.put(key, JSONObject.NULL);
                    break;
                case Boolean:
                    object.put(key, readableMap.getBoolean(key));
                    break;
                case Number:
                    object.put(key, readableMap.getDouble(key));
                    break;
                case String:
                    object.put(key, readableMap.getString(key));
                    break;
                case Map:
                    object.put(key, convertMapToJson(readableMap.getMap(key)));
                    break;
                case Array:
                    object.put(key, convertArrayToJson(readableMap.getArray(key)));
                    break;
            }
        }
        return object;
    }

    private static JSONArray convertArrayToJson(ReadableArray readableArray) throws JSONException {
        JSONArray array = new JSONArray();
        for (int i = 0; i < readableArray.size(); i++) {
            switch (readableArray.getType(i)) {
                case Null:
                    break;
                case Boolean:
                    array.put(readableArray.getBoolean(i));
                    break;
                case Number:
                    array.put(readableArray.getDouble(i));
                    break;
                case String:
                    array.put(readableArray.getString(i));
                    break;
                case Map:
                    array.put(convertMapToJson(readableArray.getMap(i)));
                    break;
                case Array:
                    array.put(convertArrayToJson(readableArray.getArray(i)));
                    break;
            }
        }
        return array;
    }
}
