package carreiras.com.github.cryptomonitor.service

import carreiras.com.github.cryptomonitor.model.TickerResponse
import retrofit2.Response
import retrofit2.http.GET

/**
 * Interface de comunicação com a API pública do **Mercado Bitcoin**.
 *
 * Utiliza as anotações do **Retrofit** para mapear os endpoints HTTP em
 * funções Kotlin suspensas (coroutines), permitindo chamadas assíncronas
 * sem bloquear a thread principal.
 *
 * URL base: `https://www.mercadobitcoin.net/`
 *
 * @see MercadoBitcoinServiceFactory
 */
interface MercadoBitcoinService {

    /**
     * Busca as informações atuais de cotação do Bitcoin (BTC) em reais (BRL).
     *
     * Endpoint: `GET api/BTC/ticker/`
     *
     * @return [Response] contendo um [TickerResponse] com os dados da cotação,
     *   ou um erro HTTP encapsulado no objeto [Response].
     */
    @GET("api/BTC/ticker/")
    suspend fun getTicker(): Response<TickerResponse>
}