package com.example.lojamamager.data

import com.example.lojamamager.data.Cliente
import com.example.lojamamager.data.Pedido

data class PedidoComCliente(
    val pedido: Pedido,
    val cliente: Cliente?
)