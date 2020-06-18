package br.com.alissonbolsoni.bioReader.scanners.fs80;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

import com.futronictech.Scanner;
import com.futronictech.UsbDeviceDataExchangeImpl;

import br.com.alissonbolsoni.bioReader.scanners.ScannerInterface;
import br.com.alissonbolsoni.bioReader.scanners.ScannerListener;

public class FPScanner implements ScannerInterface, Runnable {

    private static final int THREAD_STATUS_RUNNING = 1;
    private static final int THREAD_STATUS_STOPPING = 2;
    private static final int THREAD_STATUS_STOPPED = 3;

    //**
    private final boolean singleFrame;//= true;
    private final boolean frameMode;// = true;
    private final boolean lfd;//= false;
    private final boolean invertImage;// = true;

    private final Scanner devScan = new Scanner();
    private boolean ledControl;
    private int imageWidth = 0;
    private int imageHeight = 0;
    private int[] pixels = null;
    private Bitmap bitmapFP = null;
    private Canvas canvas = null;
    private Paint paint = null;
    private byte[] imageFP = null;
    private int threadStatus = THREAD_STATUS_STOPPED;
    private int driverStatus = DRIVER_STATUS_CLOSED;
    //**
    private UsbDeviceDataExchangeImpl usbDeviceDataExchange = null;
    private ScannerListener scannerListener;

    public FPScanner(ScannerListener scannerListener, boolean lfd) {
        this(scannerListener, true, true, lfd, true);
    }

    public FPScanner(ScannerListener scannerListener, boolean singleFrame, boolean frameMode,
                     boolean lfd, boolean invertImage) {
        super();
        this.scannerListener = scannerListener;
        this.singleFrame = singleFrame;
        this.frameMode = frameMode;
        this.lfd = lfd;
        this.invertImage = invertImage;
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case UsbDeviceDataExchangeImpl.HANDLER_MESSAGE_ALLOW_DEVICE:
                    driverStatus = DRIVER_STATUS_OPEN;
                    scannerListener.scannerNotify(ScannerListener.SN_SUCCESS_LOAD_SCANNER, null);
                    break;
                case UsbDeviceDataExchangeImpl.HANDLER_MESSAGE_DENY_DEVICE:
                    driverStatus = DRIVER_STATUS_DENY;
                    scannerListener.scannerNotify(ScannerListener.SN_ERROR_PERMISSION_DENY, null);
                    break;
                default:
                    String a = null;
                    if (msg.obj != null && msg.obj instanceof String) {
                        a = (String) msg.obj;
                    }
                    scannerListener.scannerNotify(msg.what, a);
                    break;
            }
        }
    };

    public void loadDriver(Context context) throws IllegalStateException {
        if (usbDeviceDataExchange == null) {
            usbDeviceDataExchange = new UsbDeviceDataExchangeImpl(context, handler);
        }
        driverStatus = DRIVER_STATUS_AWAITING;
        usbDeviceDataExchange.CloseDevice();
        if (usbDeviceDataExchange.OpenDevice(0, true)) {
            handler.obtainMessage(UsbDeviceDataExchangeImpl.HANDLER_MESSAGE_ALLOW_DEVICE).sendToTarget();
        } else if (!usbDeviceDataExchange.IsPendingOpen()) {
            throw new IllegalStateException("Device not open or connected");
        }
    }

    @Override
    public void startScan() {
        if (THREAD_STATUS_STOPPED == threadStatus) {
            new Thread(this, "FPScannerThread").start();
        }
    }

    @Override
    public int getDriverStatus() {
        return driverStatus;
    }

    @Override
    public void stopScan() {
        if (THREAD_STATUS_RUNNING == threadStatus) {
            threadStatus = THREAD_STATUS_STOPPING;
        }
    }

    @Override
    public void closeDriver(Context context) {
        stopScan();
        while (THREAD_STATUS_STOPPED != threadStatus) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
            }
        }
        if (usbDeviceDataExchange != null) {
            usbDeviceDataExchange.CloseDevice();
        }
        driverStatus = DRIVER_STATUS_CLOSED;
    }

    public void run() {
        boolean scannerLoad = false;
        threadStatus = THREAD_STATUS_RUNNING;
        try {
            while (THREAD_STATUS_RUNNING == threadStatus) {
                if (!scannerLoad) {
                    scannerLoad = loadScanner();
                    if (!scannerLoad) {
                        return;
                    }
                }
                configureFlags();
                long lT1 = SystemClock.uptimeMillis();
                boolean isCaptured;

                if (frameMode) {
                    isCaptured = devScan.GetFrame(imageFP);
                } else {
                    isCaptured = devScan.GetImage2(4, imageFP);
                }

                controlLED(lfd ? 200 : 200);

                if (isCaptured) {
                    String strInfo;
                    if (frameMode) {
                        strInfo = "OK. GetFrame time is " + (SystemClock.uptimeMillis() - lT1) + "(ms)";
                    } else {
                        strInfo = "OK. GetFrame2 time is " + (SystemClock.uptimeMillis() - lT1) + "(ms)";
                    }
                    handler.obtainMessage(ScannerListener.SN_LOG, -1, -1, strInfo).sendToTarget();

                    if (singleFrame) {
                        threadStatus = THREAD_STATUS_STOPPING;
                    }

                    refreshBitmap();
                } else {
                    int errCode = devScan.GetErrorCode();
                    if (errCode == devScan.FTR_ERROR_NO_FRAME) {
                        handler.obtainMessage(ScannerListener.SN_FAKE_FINGER_DETECT, -1, -1, "BIOMETRIA NÃO DETECTADA").sendToTarget();
                        break;
                    }
                    if (errCode != devScan.FTR_ERROR_EMPTY_FRAME && errCode != devScan.FTR_ERROR_MOVABLE_FINGER) {
                        handler.obtainMessage(ScannerListener.SN_ERROR).sendToTarget();
                        break;
                    }
                }
            }
            closeScanner();
        } finally {
            threadStatus = THREAD_STATUS_STOPPED;
        }
    }

    public Bitmap getBitmap() {
        return bitmapFP;
    }

    private void refreshBitmap() {
        for (int i = 0; i < imageWidth * imageHeight; i++) {
            pixels[i] = Color.rgb(imageFP[i], imageFP[i], imageFP[i]);
        }
        canvas.drawBitmap(pixels, 0, imageWidth, 0, 0, imageWidth, imageHeight, false, paint);
        handler.obtainMessage(ScannerListener.SN_UPDATE_IMAGE).sendToTarget();
    }

    private void initFingerPictureParameters(int wight, int height) {
        imageWidth = wight;
        imageHeight = height;

        imageFP = new byte[imageWidth * imageHeight];
        pixels = new int[imageWidth * imageHeight];

        bitmapFP = Bitmap.createBitmap(wight, height, Bitmap.Config.RGB_565);

        canvas = new Canvas(bitmapFP);
        paint = new Paint();

        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
    }

    private void closeScanner() {
        devScan.SetDiodesStatus(0, 0);
        devScan.CloseDeviceUsbHost();
    }

    private void controlLED(long sleep) {
        if (!lfd || ledControl) {
            devScan.SetDiodesStatus(255, 0);
            ledControl = false;
        } else {
            devScan.SetDiodesStatus(0, 255);
            ledControl = true;
        }
        try {
            Thread.sleep(sleep);
        } catch (Exception e) {
        }
    }

    private boolean loadScanner() {
        boolean bRet;
        bRet = devScan.OpenDeviceOnInterfaceUsbHost(usbDeviceDataExchange);

        if (bRet) {
            if (devScan.GetImageSize()) {
                initFingerPictureParameters(devScan.GetImageWidth(), devScan.GetImageHeight());
                String strInfo = devScan.GetVersionInfo();
                handler.obtainMessage(ScannerListener.SN_LOG, -1, -1, strInfo).sendToTarget();
                configureFlags();
                return true;
            } else {
                devScan.CloseDeviceUsbHost();
                handler.obtainMessage(ScannerListener.SN_ERROR_LOAD_SCANNER, -1, -1, devScan.GetErrorMessage()).sendToTarget();
            }
        } else {
            usbDeviceDataExchange.CloseDevice();
            handler.obtainMessage(ScannerListener.SN_ERROR_LOAD_SCANNER, -1, -1, devScan.GetErrorMessage()).sendToTarget();
        }
        return false;
    }

    private boolean configureFlags() {
        int flag = 0;
        int mask = devScan.FTR_OPTIONS_DETECT_FAKE_FINGER | devScan.FTR_OPTIONS_INVERT_IMAGE;
        if (lfd) {
            flag |= devScan.FTR_OPTIONS_DETECT_FAKE_FINGER;
        }
        if (invertImage) {
            flag |= devScan.FTR_OPTIONS_INVERT_IMAGE;
        }
        if (devScan.SetOptions(mask, flag)) {
            return true;
        }
        handler.obtainMessage(ScannerListener.SN_LOG, -1, -1, devScan.GetErrorMessage()).sendToTarget();
        return false;
    }
}
