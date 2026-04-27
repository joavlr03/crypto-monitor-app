package carreiras.com.github.cryptomonitor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import carreiras.com.github.cryptomonitor.model.TickerResponse
import carreiras.com.github.cryptomonitor.service.MercadoBitcoinServiceFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Representa todos os estados possíveis da interface do Crypto Monitor.
 *
 * Usar uma `sealed class` garante que o compilador force o tratamento de
 * **todos os estados** no `when`, eliminando erros em tempo de execução.
 */
sealed class CryptoUiState {
    /** Estado de carregamento — uma busca está em andamento. */
    object Loading : CryptoUiState()

    /**
     * Estado de sucesso — a cotação foi obtida com êxito.
     *
     * @property ticker Os dados de cotação retornados pela API.
     */
    data class Success(val ticker: TickerResponse) : CryptoUiState()

    /**
     * Estado de erro — a busca falhou por algum motivo.
     *
     * @property message Mensagem descritiva do erro ocorrido.
     */
    data class Error(val message: String) : CryptoUiState()

    /** Estado inicial — o app foi aberto mas nenhuma busca foi realizada ainda. */
    object Initial : CryptoUiState()
}

/**
 * ViewModel responsável por gerenciar o estado e a lógica de negócio
 * da tela principal do Crypto Monitor.
 *
 * Segue o padrão **MVVM**: a View (Compose) observa o [uiState] e se
 * redesenha automaticamente quando ele muda. O ViewModel não conhece
 * nada sobre a interface — ele apenas expõe estado e responde a eventos.
 *
 * O estado é armazenado em um [StateFlow], que é seguro para múltiplas
 * coletas e sobrevive a recomposições do Compose.
 *
 * As chamadas de rede são executadas dentro de [viewModelScope], que
 * cancela automaticamente as coroutines quando o ViewModel é destruído.
 */
class CryptoViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<CryptoUiState>(CryptoUiState.Initial)

    /**
     * Estado atual da UI, exposto como [StateFlow] somente leitura.
     * A tela observa este fluxo e se atualiza a cada mudança de estado.
     */
    val uiState: StateFlow<CryptoUiState> = _uiState.asStateFlow()

    private val service = MercadoBitcoinServiceFactory().create()

    /**
     * Reseta o estado da UI para [CryptoUiState.Initial], voltando à tela inicial.
     * Chamado quando o usuário clica em "Voltar à Tela Inicial".
     */
    fun resetToInitial() {
        _uiState.value = CryptoUiState.Initial
    }

    /**
     * Busca a cotação atual do Bitcoin na API do Mercado Bitcoin.
     *
     * Atualiza o [uiState] para [CryptoUiState.Loading] imediatamente,
     * depois para [CryptoUiState.Success] em caso de êxito ou
     * [CryptoUiState.Error] em caso de falha (HTTP ou rede).
     *
     * A execução ocorre em uma coroutine dentro do [viewModelScope],
     * portanto nunca bloqueia a thread principal (UI thread).
     */
    fun fetchTickerData() {
        viewModelScope.launch {
            _uiState.value = CryptoUiState.Loading

            try {
                val response = service.getTicker()

                if (response.isSuccessful) {
                    response.body()?.let { tickerResponse ->
                        _uiState.value = CryptoUiState.Success(tickerResponse)
                    } ?: run {
                        _uiState.value = CryptoUiState.Error("Resposta vazia do servidor")
                    }
                } else {
                    val errorMessage = when (response.code()) {
                        400 -> "Bad Request"
                        401 -> "Unauthorized"
                        403 -> "Forbidden"
                        404 -> "Not Found"
                        else -> "Erro desconhecido: ${response.code()}"
                    }
                    _uiState.value = CryptoUiState.Error(errorMessage)
                }
            } catch (e: Exception) {
                _uiState.value = CryptoUiState.Error("Falha na chamada: ${e.message}")
            }
        }
    }
}

