package com.example.lojamamager.data

import com.google.gson.annotations.SerializedName

// Data class para mapear a resposta da API do ViaCEP
data class EnderecoResponse(
    @SerializedName("cep") val cep: String,
    @SerializedName("logradouro") val logradouro: String,
    @SerializedName("complemento") val complemento: String,
    @SerializedName("bairro") val bairro: String,
    @SerializedName("localidade") val localidade: String, // Cidade
    @SerializedName("uf") val uf: String,
    @SerializedName("ibge") val ibge: String,
    @SerializedName("gia") val gia: String,
    @SerializedName("ddd") val ddd: String,
    @SerializedName("siafi") val siafi: String,
    @SerializedName("erro") val erro: Boolean? // ViaCEP retorna "erro": true se o CEP não for encontrado
)