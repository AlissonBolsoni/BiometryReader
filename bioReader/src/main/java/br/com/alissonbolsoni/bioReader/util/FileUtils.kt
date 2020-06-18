package br.com.alissonbolsoni.bioReader.util

import android.os.Environment
import br.com.alissonbolsoni.bioReader.constantes.EnumTypeFile
import java.io.File

class FileUtils {

    companion object {
        fun getFileFromPath(path: String?, cpf: String, fingerPosition: Int?, type: EnumTypeFile): File {

            val fileName = if (fingerPosition == null)
                "${FileUtils.normalizeCpf(cpf)}_${type.fileName}.jpg"
            else
                "${FileUtils.normalizeCpf(cpf)}_${fingerPosition}_${type.fileName}.jpg"

            val file: File =
                    if (path == null)
                        File(Environment.getExternalStorageDirectory().path, fileName)
                    else
                        File(path, fileName)

            if (file.exists()) file.delete()
            file.createNewFile()

            return file
        }

        private fun normalizeCpf(cpf: String) = cpf.replace(".", "").replace("-", "")
    }

}