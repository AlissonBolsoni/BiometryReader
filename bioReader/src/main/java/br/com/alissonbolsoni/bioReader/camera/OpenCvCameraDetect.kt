package br.com.alissonbolsoni.bioReader.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Camera
import android.util.Log
import android.view.SurfaceView
import br.com.alissonbolsoni.bioReader.R
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream

class OpenCvCameraDetect(
        private val context: Context,
        private val callback: CameraCallback) : CameraBridgeViewBase.CvCameraViewListener2, Runnable {

    companion object {
        private const val LOG = "OpenCvCameraDetect"
    }

    private var cameraBridgeViewBase: BioReaderJavaCameraView? = null
//    private var cameraBridgeViewBase: CameraBridgeViewBase? = null

    private val baseLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(context) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Log.i(LOG, "OpenCV loaded successfully")
                    cameraBridgeViewBase!!.enableView()
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    @Volatile
    private var running: RunningStatus = RunningStatus.STOP
    @Volatile
    private var pictureTaken = false

    @Volatile
    private var qtdFaces = 0

    @Volatile
    private var matTmpProcessingFace: Mat? = null

    private var cascadeClassifier: CascadeClassifier? = null
    private var mCascadeFile: File? = null

    init {
        try {
            loadFileCascade()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        cameraBridgeViewBase = callback.getSurface()
        cameraBridgeViewBase?.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT)
        cameraBridgeViewBase?.visibility = SurfaceView.VISIBLE
        cameraBridgeViewBase?.setCvCameraViewListener(this)
    }

    fun resume() {
        if (OpenCVLoader.initDebug()) {
            Log.d(LOG, "OpenCV library found inside package. Using it!")
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        } else {
            Log.d(LOG, "Internal OpenCV library not found. Using OpenCV Manager for initialization")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, context, baseLoaderCallback)
        }
        cascadeClassifier = CascadeClassifier(mCascadeFile!!.absolutePath)
        cascadeClassifier?.load(mCascadeFile!!.absolutePath)
        startFaceDetect()
    }


    @Throws(Throwable::class)
    private fun loadFileCascade() {
        val cascadeDir = context.getDir("haarcascade_frontalface_alt", Context.MODE_PRIVATE)
        mCascadeFile = File(cascadeDir, "haarcascade_frontalface_alt.xml")
        if (!mCascadeFile!!.exists()) {
            val os = FileOutputStream(mCascadeFile)
            val `is` = context.resources.openRawResource(R.raw.haarcascade_frontalface_alt)
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (`is`.read(buffer).also { bytesRead = it } != -1) {
                os.write(buffer, 0, bytesRead)
            }
            `is`.close()
            os.close()
        }
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat? {
        if (matTmpProcessingFace == null) {
            matTmpProcessingFace = inputFrame.gray()
        }
        return inputFrame.rgba()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {}

    override fun onCameraViewStopped() {}

    private fun startFaceDetect() {
        if (running == RunningStatus.RUNNING) return
        Thread(this).start()
    }

    override fun run() {
        running = RunningStatus.RUNNING
        while (running == RunningStatus.RUNNING) {
            try {
                if (matTmpProcessingFace != null) {
                    val matOfRect = MatOfRect()
                    cascadeClassifier!!.detectMultiScale(matTmpProcessingFace, matOfRect)
                    val newQtdFaces = matOfRect.toList().size
                    if (qtdFaces != newQtdFaces) {
                        qtdFaces = newQtdFaces
                        Thread.sleep(200)
                        takePicture()
                    }
                    Thread.sleep(500) //if you want an interval
                    matTmpProcessingFace = null
                }
                Thread.sleep(50)
            } catch (t: Throwable) {
                try {
                    Thread.sleep(1000)
                } catch (tt: Throwable) {
                }
            }
        }
        running = RunningStatus.STOP
        if (pictureTaken)
            callback.faceDetect(true)
    }

    fun disableCamera() {
        println("disable")
        running = RunningStatus.STOPPING
        if (cameraBridgeViewBase != null) cameraBridgeViewBase!!.disableView()
    }

    private fun takePicture() {
        val pictureCallback: Camera.PictureCallback = Camera.PictureCallback { data, _ ->
            val picture: Bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
            try {
                val file = File(callback.getPicturePath())
                if (file.exists()) file.delete()

                FileOutputStream(file.path, false).use { fos ->
                    picture.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                }

                cameraBridgeViewBase?.startPreview()
                picture.recycle()
                this.pictureTaken = true
                disableCamera()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        cameraBridgeViewBase?.takePicture(pictureCallback)
    }

    enum class RunningStatus {
        RUNNING, STOPPING, STOP
    }
}