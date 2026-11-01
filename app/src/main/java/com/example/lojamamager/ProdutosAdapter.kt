import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lojamamager.DetalheProdutoActivity
import com.example.lojamamager.R
import com.example.lojamamager.data.Produto

class ProdutosAdapter(
    private var produtos: List<Produto>,
    private val onClick: (Produto) -> Unit,
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<ProdutosAdapter.ProdutoViewHolder>() {

    // Gerenciadores do estado da seleção em lote
    val itensSelecionados = mutableSetOf<String>()
    var modoSelecaoAtivo = false

    class ProdutoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nomeProduto: TextView = itemView.findViewById(R.id.textViewNomeProduto)
        val precoProduto: TextView = itemView.findViewById(R.id.textViewPrecoProduto)
        val categoriaProduto: TextView = itemView.findViewById(R.id.textViewCategoriaProduto)
        val quantidadeEstoque: TextView = itemView.findViewById(R.id.textViewQuantidadeEstoque)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProdutoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_produto, parent, false)
        return ProdutoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProdutoViewHolder, position: Int) {
        val produto = produtos[position]
        holder.nomeProduto.text = produto.nome
        holder.precoProduto.text = "Preço: R$ %.2f".format(produto.precoVenda)
        holder.categoriaProduto.text = "Cat.: ${produto.categoria}"
        holder.quantidadeEstoque.text = "Estoque: ${produto.quantidadeEstoque} unidades"

        // Efeito Visual: Altera o fundo do item caso ele esteja selecionado na lista

        if (itensSelecionados.contains(produto.id)) {
            holder.itemView.isActivated = true
            holder.itemView.setBackgroundResource(R.drawable.item_selecionado_background)
        } else {
            holder.itemView.isActivated = false
            // Restaura o fundo padrão (com cantos arredondados se aplicável)
            holder.itemView.setBackgroundResource(R.drawable.item_selecionado_background)
        }
        holder.itemView.setOnClickListener {
            if (modoSelecaoAtivo) {
                toggleSelecao(produto.id, position)
            } else {
                onClick(produto)
            }
        }

        holder.itemView.setOnLongClickListener {
            if (!modoSelecaoAtivo) {
                modoSelecaoAtivo = true
            }
            toggleSelecao(produto.id, position)
            true
        }
    }

    private fun toggleSelecao(id: String?, position: Int) {
        if (id == null) return
        if (itensSelecionados.contains(id)) {
            itensSelecionados.remove(id)
        } else {
            itensSelecionados.add(id)
        }
        notifyItemChanged(position)
        onSelectionChanged(itensSelecionados.size)
    }

    override fun getItemCount(): Int = produtos.size

    // Retorna a lista de IDs selecionados para exclusão
    fun getProdutosSelecionados(): List<String> = itensSelecionados.toList()

    // Limpa a seleção e restaura o estado normal do adapter
    fun desativarModoSelecao() {
        modoSelecaoAtivo = false
        itensSelecionados.clear()
        notifyDataSetChanged()
    }

    fun updateProdutos(newProdutos: List<Produto>) {
        produtos = newProdutos
        notifyDataSetChanged()
    }
}