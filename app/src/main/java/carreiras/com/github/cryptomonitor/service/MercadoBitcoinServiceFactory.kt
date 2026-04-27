package carreiras.com.github.cryptomonitor.service

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Fábrica responsável por criar e configurar a instância do cliente HTTP
 * [MercadoBitcoinService] usando o **Retrofit**.
 *
 * Aplica o padrão **Factory Method**: centraliza a criação do serviço,
 * evitando que o [CryptoViewModel][carreiras.com.github.cryptomonitor.viewmodel.CryptoViewModel]
 * precise conhecer os detalhes de configuração do Retrofit.
 *
 * Configurações aplicadas:
 * - **Base URL:** `https://www.mercadobitcoin.net/`
 * - **Conversor:** [GsonConverterFactory] — converte automaticamente o JSON
 *   da resposta em objetos Kotlin (ex: [carreiras.com.github.cryptomonitor.model.TickerResponse]).
 */
class MercadoBitcoinServiceFactory {

    /**
     * Cria e retorna uma implementação de [MercadoBitcoinService] pronta para uso.
     *
     * @return Instância configurada de [MercadoBitcoinService].
     */
    fun create(): MercadoBitcoinService {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.mercadobitcoin.net/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(MercadoBitcoinService::class.java)
    }
}