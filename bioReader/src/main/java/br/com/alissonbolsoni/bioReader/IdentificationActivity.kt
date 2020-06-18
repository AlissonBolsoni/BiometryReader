package br.com.alissonbolsoni.bioReader

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.WindowManager
import br.com.alissonbolsoni.bioReader.camera.BioReaderJavaCameraView
import br.com.alissonbolsoni.bioReader.camera.CameraCallback
import br.com.alissonbolsoni.bioReader.camera.OpenCvCameraDetect
import br.com.alissonbolsoni.bioReader.constantes.EnumFingers
import br.com.alissonbolsoni.bioReader.constantes.EnumReturn
import br.com.alissonbolsoni.bioReader.constantes.EnumScreenStatus
import br.com.alissonbolsoni.bioReader.constantes.EnumTypeFile
import br.com.alissonbolsoni.bioReader.scanners.ScannerInterface
import br.com.alissonbolsoni.bioReader.scanners.ScannerListener
import br.com.alissonbolsoni.bioReader.scanners.fs80.FPScanner
import br.com.alissonbolsoni.bioReader.util.FileUtils
import kotlinx.android.synthetic.main.activity_identificacao.*
import org.opencv.android.*
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class IdentificationActivity : AppCompatActivity(), ScannerListener, CameraCallback {

    private var paramTextExtra: String? = null
    private var paramName: String = "-"
    private var paramCpf: String = "-"
    private var paramBiometricQuality: Int = 0
    private var paramFingerPosition: EnumFingers = EnumFingers.POLEGAR_DIREITO
    private var paramScreenOrientation: Int = 0
    private var dialog: AlertDialog? = null
    private var scanner: ScannerInterface? = null
    private val percentLoad = AtomicInteger(0)
    private var savedInstanceState: Bundle? = null
    private lateinit var openCvCameraDetect: OpenCvCameraDetect
    private lateinit var picturePath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_identificacao)

        this.savedInstanceState = savedInstanceState
        checkPermissions()
    }

    private fun checkPermissions() {
        if (isPermissionsGranted()) {
            onCreateAfterPermissions()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA),
                    1)
        }
    }

    private fun isPermissionsGranted() =
            ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        checkPermissions()
    }

    private fun onCreateAfterPermissions() {
        if (savedInstanceState == null) {

            paramName = intent.getStringExtra(IT_PARAM_NAME) ?: "-"
            paramCpf = intent.getStringExtra(IT_PARAM_CPF) ?: "-"
            paramTextExtra = "${paramName}\n${paramCpf}"
            paramScreenOrientation = -1
            paramBiometricQuality = intent.getIntExtra(IT_PARAM_BIO_QUALITY, 70)
            paramFingerPosition = EnumFingers.getFingerByIndex(intent.getIntExtra(IT_PARAM_FINGER_POSITION, 1))

            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            openCvCameraDetect = OpenCvCameraDetect(this, this)

            updateInterface(getString(R.string.focus_face_), TYPE_UI_INFO)
            manageScreenElements(EnumScreenStatus.TAKING_PICTURE)
        }
        if (paramTextExtra != null && paramTextExtra!!.trim().isNotEmpty()) {
            layout_texto_extra.visibility = View.VISIBLE
            tv_texto_extra.text = paramTextExtra
        }
    }

    private fun init() {
        tv_finger_position?.text = paramFingerPosition.finger
        updateInterface(getString(R.string.msg_awaiting_driver), TYPE_UI_LOAD)
        try {
            closeScanner()
            scanner = loadScanner()
            scanner!!.loadDriver(this)
        } catch (th: Throwable) {
            Log.e(LOG, "loadDriver", th)
            finishIdentification(EnumReturn.ERRO_CARREGAR_SCANNER)
            return
        }

        if (!thRunnable)
            Thread(runnableProgressLoad, "runnableProgressLoad").start()
    }

    private var thRunnable = false

    private val runnableProgressLoad = Runnable {
        thRunnable = true
        percentLoad.set(0)
        while (thRunnable && percentLoad.get() < 100) {
            runOnUiThread { updateInterface("$percentLoad%", TYPE_UI_PROGRESS_LOAD) }

            if (percentLoad.get() == 99) {
                percentLoad.set(0)
            }

            percentLoad.addAndGet(1)
            Thread.sleep(700)
        }
        thRunnable = false
    }

    private fun loadScanner() = FPScanner(this, true)

    private fun finalizeThreadLoad() {
        thRunnable = false
    }

    override fun scannerNotify(type: Int, log: String?) {
        when (type) {
            ScannerListener.SN_SUCCESS_LOAD_SCANNER -> {
                finalizeThreadLoad()
                startScan()
                main.setBackgroundColor(Color.WHITE)
                container.visibility = View.VISIBLE
                load.visibility = View.GONE
                im_logo_top.visibility = View.GONE
            }
            ScannerListener.SN_UPDATE_IMAGE -> {
                updateInterface(getString(R.string.awaiting_save), TYPE_UI_AWAITING)
                im_biometria.setImageBitmap(scanner!!.bitmap)
                saveFileBioAsync(scanner!!.bitmap)
            }

            ScannerListener.SN_FAKE_FINGER_DETECT -> {
                updateInterface(getString(R.string.msg_finger_not_detected), TYPE_UI_ERROR)
                handlerX.sendEmptyMessageDelayed(HANDLER_WHAT_FAKE_FINGER, 3000)
            }

            ScannerListener.SN_ERROR, ScannerListener.SN_ERROR_LOAD_SCANNER -> {
                finalizeThreadLoad()
                finishIdentification(EnumReturn.ERRO_CARREGAR_SCANNER)
            }

            ScannerListener.SN_ERROR_PERMISSION_DENY -> {
                finalizeThreadLoad()
                updateInterface(getString(R.string.msg_permission_deny), TYPE_UI_LOAD)
                updateInterface("", TYPE_UI_PROGRESS_LOAD)
                handlerX.sendEmptyMessageDelayed(HANDLER_WHAT_INIT, 3000)
            }
        }
    }

    private val handlerX = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            if (msg == null) return
            when (msg.what) {
                HANDLER_WHAT_INIT -> {
                    init()
                }
                HANDLER_WHAT_FAKE_FINGER -> {
                    startScan()
                }
                HANDLER_WHAT_FAILED_SAVE_BIO -> {
                    startScan()
                }
            }
        }
    }

    private fun startScan() {
        if (null != scanner) {
            updateInterface(getString(R.string.msg_finger_position), TYPE_UI_AWAITING)
            scanner!!.startScan()
        }
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        if (null != scanner && ScannerInterface.DRIVER_STATUS_OPEN == scanner!!.driverStatus) {
            startScan()
        }
        if (this::openCvCameraDetect.isInitialized)
            openCvCameraDetect.resume()
    }

    override fun onPause() {
        super.onPause()
        if (this::openCvCameraDetect.isInitialized)
            openCvCameraDetect.disableCamera()

        try {
            if (null != dialog && dialog!!.isShowing) {
                dialog!!.dismiss()
            }
            scanner?.stopScan()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        openCvCameraDetect.disableCamera()
    }

    override fun finish() {
        handlerX.removeMessages(HANDLER_WHAT_INIT)
        handlerX.removeMessages(HANDLER_WHAT_FAKE_FINGER)
        handlerX.removeMessages(HANDLER_WHAT_CAMERA_RECREATE)
        handlerX.removeMessages(HANDLER_WHAT_FAILED_SAVE_BIO)
        finalizeThreadLoad()
        closeScanner()
        super.finish()
    }

    private fun updateInterface(text: String, type: Int) {
        //#f5b52d
        var colorBack = Color.parseColor("#00FFFFFF")
        var colorText = Color.parseColor("#000000")
        when (type) {
            TYPE_UI_INFO -> {
                colorBack = Color.parseColor("#00FFFFFF")
                colorText = Color.parseColor("#000000")
            }
            TYPE_UI_AWAITING -> {
                colorBack = Color.parseColor("#f5b52d")
                colorText = Color.parseColor("#FFFFFF")
            }
            TYPE_UI_LOAD -> {
                tv_info_load.text = text
                return
            }
            TYPE_UI_PROGRESS_LOAD -> {
                tv_progress.text = text
                return
            }
            TYPE_UI_ERROR -> {
                colorBack = Color.parseColor("#f5b52d")
                colorText = Color.parseColor("#FFFFFF")
            }
        }
        tv_status!!.setTextColor(colorText)
        lay_header!!.setBackgroundColor(colorBack)
        tv_status!!.text = text
    }

    private fun saveFileBioAsync(bitmap: Bitmap) {
        thread {
            try {
                val path = intent.getStringExtra(IT_PARAM_BIO_URI)
                val file = FileUtils.getFileFromPath(path, paramCpf, paramFingerPosition.index, EnumTypeFile.BIOMETRY)

                FileOutputStream(file, false).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, paramBiometricQuality, fos)
                }

                if (file.length() > 0) {
                    closeScanner()
                    finishIdentification(EnumReturn.SUCESSO, file.path)
                    return@thread
                }
            } catch (th: Throwable) {
                Log.e(LOG, "Falha salvar/recuperar imagem da biometria", th)
            }
            //PROBLEMAS AO SALVAR IMAGEM!!
            runOnUiThread {
                updateInterface(getString(R.string.msg_finger_not_generated), TYPE_UI_ERROR)
            }
            handlerX.sendEmptyMessageDelayed(HANDLER_WHAT_FAILED_SAVE_BIO, 3000)
        }
    }

    private fun finishIdentification(ret: EnumReturn, path: String = "") {
        val data = Intent()
        data.putExtra(IT_RETURN_CODE, ret.codigoRetorno)
        data.putExtra(IT_RETURN_MESSAGE, ret.descRetorno)
        data.putExtra(IT_RETURN_BIO_PATH, path)
        data.putExtra(IT_RETURN_PIC_PATH, picturePath)
        setResult(RESULT_OK, data)
        finish()
    }

    private fun closeScanner() {
        if (null != scanner) {
            scanner!!.stopScan()
            scanner!!.closeDriver(this)
            scanner = null
        }
    }

    override fun faceDetect(detected: Boolean) {
        if (detected) {
            runOnUiThread {
                val bmp = BitmapFactory.decodeFile(this.picturePath)
                im_picture.setImageBitmap(bmp)
                manageScreenElements(EnumScreenStatus.PICTURE_TAKED)
                init()
            }
        } else {
            updateInterface(getString(R.string.focus_face_), TYPE_UI_INFO)
            silhouette!!.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)
        }
    }

    override fun getSurface() = findViewById<BioReaderJavaCameraView>(R.id.main_surface)

    override fun getPicturePath(): String {
        picturePath = FileUtils.getFileFromPath(
                intent.getStringExtra(IT_PARAM_BIO_URI), paramCpf, null, EnumTypeFile.PICTURE).path
        return picturePath
    }

    private fun manageScreenElements(status: EnumScreenStatus) {

        when (status) {
            EnumScreenStatus.TAKING_PICTURE -> {
                camera_preview.visibility = View.VISIBLE
                im_logo.visibility = View.VISIBLE
                im_picture.visibility = View.GONE
                im_samples.visibility = View.GONE
            }
            EnumScreenStatus.PICTURE_TAKED -> {
                camera_preview.visibility = View.GONE
                im_logo.visibility = View.GONE
                im_picture.visibility = View.VISIBLE
                im_samples.visibility = View.VISIBLE
            }
        }
    }

    companion object {
        //PARAMETROS DE ENTRADA PARA COLETA DE BIOMETRIA
        const val IT_PARAM_BIO_URI = "IT_PARAM_URI_FILE"
        const val IT_PARAM_CPF = "IT_PARAM_CPF"
        const val IT_PARAM_NAME = "IT_PARAM_NAME"
        const val IT_PARAM_BIO_QUALITY = "IT_PARAM_BIO_QUALITY"
        const val IT_PARAM_FINGER_POSITION = "IT_PARAM_FINGER_POSITION"

        //PARAMETROS DE RETORNO DA COLETA DE BIOMETRIA
        const val IT_RETURN_MESSAGE = "IT_RETURN_MESSAGE"
        const val IT_RETURN_CODE = "IT_RETURN_CODE"
        const val IT_RETURN_BIO_PATH = "IT_RETURN_BIO_PATH"
        const val IT_RETURN_PIC_PATH = "IT_RETURN_PIC_PATH"

        private const val LOG = "LOG_IDENTIFICATION"

        const val TYPE_UI_INFO = 11
        const val TYPE_UI_AWAITING = 22
        const val TYPE_UI_LOAD = 33
        const val TYPE_UI_PROGRESS_LOAD = 35
        const val TYPE_UI_ERROR = 44

        const val HANDLER_WHAT_INIT = 60
        const val HANDLER_WHAT_FAKE_FINGER = 61
        const val HANDLER_WHAT_CAMERA_RECREATE = 62
        const val HANDLER_WHAT_FAILED_SAVE_BIO = 63
    }

}