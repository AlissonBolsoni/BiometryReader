package br.com.alissonbolsoni.bioReader.constantes

import java.io.Serializable

enum class EnumFingers(val index: Int, val finger: String) : Serializable {
    POLEGAR_DIREITO(1, "Coloque o POLEGAR DIREITO"),
    INDICADR_DIRETO(2, "Coloque o INDICADOR DIREITO"),
    POLEGAR_ESQUERDO(3, "Coloque o POLEGAR ESQUERDO"),
    INDICADR_ESQUERDO(4, "Coloque o INDICADOR ESQUERDO");

    companion object {
        fun getFingerByIndex(index: Int): EnumFingers {
            return values().find { it ->
                it.index == index
            } ?: POLEGAR_DIREITO
        }
    }
}