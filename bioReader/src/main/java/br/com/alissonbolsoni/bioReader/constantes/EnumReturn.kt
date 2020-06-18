package br.com.alissonbolsoni.bioReader.constantes

import java.io.Serializable

enum class EnumReturn(val codigoRetorno: Int, val descRetorno: String) : Serializable {

    SUCESSO(0, "SUCESSO"),
    ERRO_CARREGAR_SCANNER(1, "ERRO CARREGAR SCANNER"),
    ERRO_CAPTURAR_FOTO(4, "ERRO CAPTURAR FOTO"),
    ERRO_DEDO_FALSO(7, "BIOMETRIA NÃO DETECTADA");

}