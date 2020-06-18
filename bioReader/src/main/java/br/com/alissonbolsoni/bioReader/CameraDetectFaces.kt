package br.com.alissonbolsoni.bioReader

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import android.view.SurfaceHolder
//import br.com.grupocriar.swapandroid.media.camera.CameraPreviewFacesAbs
//
//import org.opencv.android.BaseLoaderCallback
//import org.opencv.android.LoaderCallbackInterface
//import org.opencv.android.OpenCVLoader
//import org.opencv.android.Utils
//import org.opencv.core.Mat
//import org.opencv.core.MatOfByte
//import org.opencv.core.MatOfRect
//import org.opencv.imgcodecs.Imgcodecs
//import org.opencv.objdetect.CascadeClassifier

import java.io.File
import java.io.FileOutputStream
class CameraDetectFaces{
//class CameraDetectFaces(activity: Activity, cameraToUse: Int, private var callbackFaces: CallbackFaces)
//    : CameraPreviewFacesAbs(activity, cameraToUse, false, 0){

//    private var cascadeClassifier: CascadeClassifier? = null
//    private var mCascadeFile: File? = null
//
//    private var flagToProcess = true
//
//    init {
//        isDrawTextFacesDetects = false
//    }
//
//    override fun surfaceCreated(surfaceHolder: SurfaceHolder?) {
//        super.surfaceCreated(surfaceHolder)
//
//        val baseLoaderCallback = object : BaseLoaderCallback(activity) {
//
//            override fun onManagerConnected(status: Int) {
//                when (status) {
//                    LoaderCallbackInterface.SUCCESS -> {
//                        Log.i(TAG, "OpenCV loaded successfully")
//                    }
//                    else -> {
//                        super.onManagerConnected(status)
//                    }
//                }
//            }
//        }
//        try {
//            loadFileCascade()
//            if (OpenCVLoader.initDebug()) {
//                Log.d(TAG, "OpenCV library found inside package. Using it!")
//                baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
//                cascadeClassifier = CascadeClassifier(mCascadeFile!!.absolutePath)
//                cascadeClassifier!!.load(mCascadeFile!!.absolutePath)
//            } else {
//                if (camera.parameters.maxNumDetectedFaces <= 0) {
//                    callbackFaces.onErrorLoadFaceDetect()
//                }
//            }
//        } catch (t: Throwable) {
//            t.printStackTrace()
//        }
//    }
//
//    @Throws(Throwable::class)
//    private fun loadFileCascade() {
//        val cascadeDir = activity.getDir("haarcascade_frontalface_alt", Context.MODE_PRIVATE)
//        mCascadeFile = File(cascadeDir, "haarcascade_frontalface_alt.xml")
//
//        if (!mCascadeFile!!.exists()) {
//            val os = FileOutputStream(mCascadeFile)
//            val stream = resources.openRawResource(R.raw.haarcascade_frontalface_alt)
//            val buffer = ByteArray(4096)
//            var bytesRead = 0
//            while (bytesRead != -1) {
//                bytesRead = stream.read(buffer)
//                if (bytesRead > 0) {
//                    os.write(buffer, 0, bytesRead)
//                }
//            }
//            stream.close()
//            os.close()
//        }
//    }
//
//    override fun onConfigureEnableFaceDetect(native: Boolean): Boolean {
//        return true
//    }
//
//
//    override fun processFacesNonNativeRects(bytes: ByteArray): List<Rect>? {
//        return null
//    }
//
//    override fun processFacesNonNative(bytes: ByteArray): Int {
//        val mat = Imgcodecs.imdecode(MatOfByte(*bytes), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED)
//
//        val matOfRect = MatOfRect()
//        cascadeClassifier!!.detectMultiScale(mat, matOfRect)
//        val faces = matOfRect.toList().size
//        processedFacesNative(faces)
//        return faces
//    }
//
//    override fun processedFacesNative(faces: Int) {
//        if (flagToProcess) {
//            flagToProcess = false
//            proc(faces)
//        }
//    }
//
//    fun releaseFlagToProcess() {
//        flagToProcess = true
//    }
//
//    private fun proc(faces: Int) {
//        try {
//            activity.runOnUiThread { callbackFaces.onFaceDetect(faces) }
//        } catch (t: Throwable) {
//            t.printStackTrace()
//        }
//
//    }
//
//    fun setCallbackFaces(callbackFaces: CallbackFaces) {
//        this.callbackFaces = callbackFaces
//    }
//
//    fun getFaces(bitmapFoto: Bitmap?): Int {
//        if (bitmapFoto == null) {
//            return -1
//        }
//
//        //Caso o OpenCV não foi carregado e esta utilizando detecção nativa
//        //retorna 1 por default. Já que não é possível saber quantas faces possue a foto;
//        if (null == cascadeClassifier && isFaceDetectNativeEnabled) {
//            return 1
//        }
//
//        val mat = Mat()
//        Utils.bitmapToMat(bitmapFoto, mat)
//        val matOfRect = MatOfRect()
//        cascadeClassifier!!.detectMultiScale(mat, matOfRect)
//        return matOfRect.toList().size
//    }
//
//    interface CallbackFaces {
//        fun onFaceDetect(faces: Int)
//        fun onErrorLoadFaceDetect()
//    }
//
//    companion object {
//        private val TAG = CameraPreviewFacesAbs::class.java.name
//    }
}
