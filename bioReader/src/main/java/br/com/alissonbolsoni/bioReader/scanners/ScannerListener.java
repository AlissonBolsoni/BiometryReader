package br.com.alissonbolsoni.bioReader.scanners;

public interface ScannerListener {

    int SN_LOG = 1;
    int SN_ERROR = 2;
    int SN_SUCCESS_LOAD_SCANNER = 3;
    int SN_ERROR_LOAD_SCANNER = 4;
    int SN_ERROR_PERMISSION_DENY = 5;
    int SN_FAKE_FINGER_DETECT = 6;
    int SN_UPDATE_IMAGE = 7;

    void scannerNotify(int type,String log);

}
