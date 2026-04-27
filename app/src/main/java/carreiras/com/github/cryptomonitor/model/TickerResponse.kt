package carreiras.com.github.cryptomonitor.model

/**
 * Representa a resposta da API do Mercado Bitcoin para a consulta de ticker.
 *
 * @property ticker Objeto contendo os dados atuais de cotação do Bitcoin.
 */
class TickerResponse(
    val ticker: Ticker
)

/**
 * Contém as informações de cotação do Bitcoin retornadas pela API.
 *
 * @property high Maior preço de negociação nas últimas 24 horas, em reais (BRL).
 * @property low Menor preço de negociação nas últimas 24 horas, em reais (BRL).
 * @property vol Volume de Bitcoin negociado nas últimas 24 horas.
 * @property last Preço da última negociação realizada, em reais (BRL).
 * @property buy Melhor preço de compra atual (bid), em reais (BRL).
 * @property sell Melhor preço de venda atual (ask), em reais (BRL).
 * @property date Timestamp Unix (em segundos) do momento em que os dados foram gerados.
 */
class Ticker(
    val high: String,
    val low: String,
    val vol: String,
    val last: String,
    val buy: String,
    val sell: String,
    val date: Long
)