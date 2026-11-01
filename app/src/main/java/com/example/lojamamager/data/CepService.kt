package com.example.lojamamager.data

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface CepService {
    @GET("ws/{cep}/json/")
    suspend fun buscarEnderecoPorCep(@Path("cep") cep: String): Response<EnderecoResponse>
}