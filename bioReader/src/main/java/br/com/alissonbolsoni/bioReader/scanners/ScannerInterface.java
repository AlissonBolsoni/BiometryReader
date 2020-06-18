package br.com.alissonbolsoni.bioReader.scanners;

import android.content.Context;
import android.graphics.Bitmap;

public interface ScannerInterface {

    int DRIVER_STATUS_CLOSED = 1;
    int DRIVER_STATUS_OPEN = 2;
    int DRIVER_STATUS_DENY = 3;
    int DRIVER_STATUS_AWAITING = 4;

    void loadDriver(Context context);

    void closeDriver(Context context);

    int getDriverStatus();

    void startScan();

    void stopScan();

    Bitmap getBitmap();

}
