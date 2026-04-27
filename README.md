# 📱 Crypto Monitor — Android com Arquitetura Declarativa

> Projeto didático desenvolvido para ensinar os fundamentos de desenvolvimento Android moderno com **Kotlin**, **Jetpack Compose** e **arquitetura declarativa (MVVM)**.

---

## 🎯 Objetivo

Criar um aplicativo Android que consulta a cotação atual do **Bitcoin (BTC)** em reais (BRL) usando a API pública do **Mercado Bitcoin**, exibindo os dados de forma reativa e organizada.

---

## 🏗️ Arquitetura do Projeto

O projeto segue o padrão **MVVM (Model - View - ViewModel)**, que separa as responsabilidades do app em camadas bem definidas:

```
app/src/main/java/carreiras/com/github/cryptomonitor/
│
├── model/         → Dados (o que o app manipula)
├── service/       → Comunicação com a API
├── viewmodel/     → Lógica e estado da tela
└── ui/
    └── theme/
        └── screens/   → Interface visual (Jetpack Compose)
```

### Por que MVVM?

- A **View** (tela) não precisa saber como os dados são buscados — ela só exibe o que o ViewModel entrega.
- O **ViewModel** não sabe nada sobre a interface — ele só gerencia o estado.
- O **Model** representa os dados puros, sem lógica de negócio ou de exibição.

Isso torna o código **testável, manutenível e escalável**.

---

## 📦 Camadas em Detalhe

### 1. Model — `model/TickerResponse.kt`

Representa os dados retornados pela API do Mercado Bitcoin.

```kotlin
class TickerResponse(
    val ticker: Ticker
)

class Ticker(
    val high: String,   // Maior preço nas últimas 24h
    val low: String,    // Menor preço nas últimas 24h
    val vol: String,    // Volume negociado
    val last: String,   // Último preço negociado
    val buy: String,    // Melhor preço de compra
    val sell: String,   // Melhor preço de venda
    val date: Long      // Timestamp Unix da cotação
)
```

> 💡 **Para o aluno:** O Model é apenas um "molde" dos dados. Ele não faz nada — só representa a estrutura da informação que vem da API.

---

### 2. Service — `service/MercadoBitcoinService.kt`

Interface que define a chamada HTTP para a API. Este arquivo contém **1 interface e 1 método**:

---

#### 2.1 `interface MercadoBitcoinService`

```kotlin
interface MercadoBitcoinService {
    @GET("api/BTC/ticker/")
    suspend fun getTicker(): Response<TickerResponse>
}
```

Uma `interface` no Kotlin (e em Java) é um **contrato** — ela define *o que* deve ser feito, mas não *como*. Quem implementa os detalhes é o **Retrofit**, que em tempo de execução gera automaticamente uma classe que sabe fazer a requisição HTTP.

> 💡 **Para o aluno:** Você nunca vai escrever a implementação dessa interface manualmente. O Retrofit usa reflexão e proxies dinâmicos para criar a implementação por você. Basta declarar o contrato e o Retrofit cuida do resto.

---

#### 2.2 `suspend fun getTicker(): Response<TickerResponse>`

```kotlin
@GET("api/BTC/ticker/")
suspend fun getTicker(): Response<TickerResponse>
```

**O que cada parte significa:**

- **`@GET("api/BTC/ticker/")`** — anotação do Retrofit que indica que esta função faz uma requisição HTTP do tipo `GET` para o endpoint `api/BTC/ticker/`. A URL completa fica: `https://www.mercadobitcoin.net/api/BTC/ticker/`

- **`suspend`** — palavra-chave que transforma esta função em uma **coroutine** (explicado em detalhes abaixo)

- **`Response<TickerResponse>`** — o retorno é um objeto `Response` do Retrofit que encapsula tanto o corpo da resposta (`TickerResponse`) quanto informações HTTP como código de status (`200`, `404`, etc.), headers, etc.

---

#### 📌 O que é uma Coroutine?

Esta é uma das partes mais importantes do projeto moderno em Android. Entender coroutines é essencial.

**O problema que coroutines resolvem:**

Imagine que você precisa buscar dados na internet. Essa operação pode levar de milissegundos a vários segundos. Se você fizer isso na **thread principal** (a thread que controla a interface do usuário), o app **congela** — a tela trava, botões não respondem, e o Android pode exibir o famoso aviso **"Application Not Responding" (ANR)**.

A solução clássica era usar **callbacks** ou **threads manuais**:

```kotlin
// Jeito antigo e verboso com callback
service.getTicker(object : Callback<TickerResponse> {
    override fun onResponse(call: Call<TickerResponse>, response: Response<TickerResponse>) {
        // sucesso
    }
    override fun onFailure(call: Call<TickerResponse>, t: Throwable) {
        // erro
    }
})
```

Isso funciona, mas gera o chamado **"Callback Hell"** — código difícil de ler, manter e testar, especialmente quando há múltiplas chamadas encadeadas.

**A solução moderna — Coroutines:**

Uma **coroutine** é uma função que pode ser **suspensa** (pausada) e **retomada** depois, sem bloquear a thread onde está rodando.

Com coroutines, o mesmo código fica assim:

```kotlin
// Jeito moderno com coroutines
viewModelScope.launch {
    val response = service.getTicker() // suspende aqui enquanto aguarda a resposta
    // continua aqui quando a resposta chegar
}
```

O código parece sequencial e síncrono, mas por baixo dos panos é assíncrono.

**Como funciona na prática:**

```
Thread Principal (UI)
        │
        │ viewModelScope.launch { ... }
        │
        ├──► Coroutine inicia
        │         │
        │         │ service.getTicker()  ← função suspend
        │         │
        │         │ [PAUSA — aguarda resposta da API]
        │         │ [Thread principal fica LIVRE para atualizar a UI]
        │         │
        │         │ [RETOMA — resposta chegou]
        │         │
        │         └──► _uiState.value = CryptoUiState.Success(...)
        │
```

> 💡 **Para o aluno:** Pense em coroutine como um **garçom inteligente**. Ele anota seu pedido (chama a API), vai para a cozinha (faz a requisição), mas em vez de ficar parado esperando a comida ficar pronta (bloqueando a thread), ele vai atender outras mesas (mantém a UI responsiva). Quando a comida fica pronta (API responde), ele volta e entrega (atualiza o estado).

**Por que `suspend` no `getTicker()`?**

A palavra `suspend` é uma **marcação** que diz ao compilador Kotlin: *"esta função pode ser pausada — ela só pode ser chamada de dentro de uma coroutine ou de outra função suspend"*.

É por isso que no `CryptoViewModel`, a chamada ao `getTicker()` está dentro de `viewModelScope.launch { }` — que cria a coroutine necessária para chamar uma função `suspend`.

**`viewModelScope` — o gerente das coroutines:**

```kotlin
viewModelScope.launch {
    val response = service.getTicker()
}
```

O `viewModelScope` é um escopo de coroutine vinculado ao ciclo de vida do ViewModel. Isso significa que se o usuário sair da tela e o ViewModel for destruído, **todas as coroutines em andamento são canceladas automaticamente**, evitando vazamentos de memória.

---

### 3. ServiceFactory — `service/MercadoBitcoinServiceFactory.kt`

Responsável por construir e configurar o cliente HTTP com o **Retrofit**. Este arquivo contém **1 classe e 1 método**:

---

#### 3.1 `class MercadoBitcoinServiceFactory`

```kotlin
class MercadoBitcoinServiceFactory {
    fun create(): MercadoBitcoinService { ... }
}
```

Aplica o padrão de projeto **Factory Method** — uma classe cuja única responsabilidade é **criar e entregar um objeto pronto para uso**.

Sem ela, o `CryptoViewModel` teria que conhecer todos os detalhes de configuração do Retrofit (URL base, conversor, etc.), o que criaria acoplamento desnecessário. Com a Factory, o ViewModel simplesmente chama:

```kotlin
private val service = MercadoBitcoinServiceFactory().create()
```

E recebe um `MercadoBitcoinService` pronto, sem saber nada sobre como ele foi construído.

> 💡 **Para o aluno:** O padrão Factory é como uma **fábrica de carros** — você não precisa saber montar um motor, soldar a lataria e calibrar os pneus. Você vai à loja e recebe o carro pronto. A `MercadoBitcoinServiceFactory` é essa loja: você pede um serviço e ela entrega configurado.

---

#### 3.2 `fun create(): MercadoBitcoinService`

```kotlin
fun create(): MercadoBitcoinService {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://www.mercadobitcoin.net/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    return retrofit.create(MercadoBitcoinService::class.java)
}
```

**O que cada linha faz:**

- **`Retrofit.Builder()`** — inicia a construção de uma instância do Retrofit usando o padrão **Builder** (cada método encadeado configura uma parte)
- **`.baseUrl("https://www.mercadobitcoin.net/")`** — define a URL raiz da API. Todos os endpoints declarados na interface (`@GET("api/BTC/ticker/")`) serão concatenados a ela
- **`.addConverterFactory(GsonConverterFactory.create())`** — registra o conversor JSON → Kotlin (explicado abaixo)
- **`.build()`** — finaliza a construção e retorna a instância do Retrofit configurada
- **`retrofit.create(MercadoBitcoinService::class.java)`** — gera em tempo de execução uma implementação concreta da interface `MercadoBitcoinService`

---

#### 📌 O que é o Retrofit?

**Retrofit** é uma biblioteca criada pela **Square** que transforma interfaces Kotlin/Java em clientes HTTP completos, sem que você precise escrever uma linha de código de rede.

**O problema que o Retrofit resolve:**

Fazer uma requisição HTTP "na mão" em Android é extremamente verboso:

```kotlin
// Jeito manual — sem Retrofit
val url = URL("https://www.mercadobitcoin.net/api/BTC/ticker/")
val connection = url.openConnection() as HttpURLConnection
connection.requestMethod = "GET"
connection.connect()

val inputStream = connection.inputStream
val response = inputStream.bufferedReader().readText()

// Agora você precisa parsear o JSON manualmente...
val jsonObject = JSONObject(response)
val ticker = jsonObject.getJSONObject("ticker")
val last = ticker.getString("last")
// ... e assim por diante para cada campo
```

Além de verboso, esse código não trata erros de rede, não é assíncrono por padrão e não converte o JSON automaticamente.

**Com Retrofit, o mesmo resultado é obtido assim:**

```kotlin
// Jeito moderno — com Retrofit
val response = service.getTicker()
val ticker = response.body()?.ticker
```

Duas linhas. O Retrofit cuida de abrir a conexão, fazer a requisição, ler a resposta e converter o JSON.

---

**Como o Retrofit funciona por baixo dos panos:**

```
Você chama:  service.getTicker()
                    │
                    ▼
        Retrofit intercepta a chamada
                    │
                    ▼
        Lê as anotações: @GET("api/BTC/ticker/")
                    │
                    ▼
        Monta a URL completa:
        https://www.mercadobitcoin.net/api/BTC/ticker/
                    │
                    ▼
        Executa a requisição HTTP GET
                    │
                    ▼
        Recebe o JSON da API:
        {"ticker": {"last": "650000.00", "high": "660000.00", ...}}
                    │
                    ▼
        GsonConverterFactory converte o JSON
        em um objeto TickerResponse automaticamente
                    │
                    ▼
        Retorna: Response<TickerResponse>
```

---

#### 📌 O que é o GsonConverterFactory?

O **Gson** é uma biblioteca do Google que converte objetos Java/Kotlin em JSON e vice-versa. O **GsonConverterFactory** é o adaptador que conecta o Gson ao Retrofit.

**Como funciona a conversão:**

A API retorna este JSON:
```json
{
  "ticker": {
    "high": "660000.00",
    "low": "640000.00",
    "vol": "12.34567",
    "last": "650000.00",
    "buy": "649000.00",
    "sell": "651000.00",
    "date": 1712345678
  }
}
```

O Gson lê esse JSON e preenche automaticamente os campos da classe `TickerResponse`:

```kotlin
// O Gson faz esse "mapeamento" automaticamente
class TickerResponse(val ticker: Ticker)
class Ticker(
    val high: String,  // ← recebe "660000.00"
    val low: String,   // ← recebe "640000.00"
    val vol: String,   // ← recebe "12.34567"
    val last: String,  // ← recebe "650000.00"
    val buy: String,   // ← recebe "649000.00"
    val sell: String,  // ← recebe "651000.00"
    val date: Long     // ← recebe 1712345678
)
```

> 💡 **Para o aluno:** O Gson usa o **nome das propriedades** da classe para encontrar os campos correspondentes no JSON. Por isso o nome `high` na classe Kotlin deve ser exatamente igual à chave `"high"` no JSON. Se quiser usar nomes diferentes, pode usar a anotação `@SerializedName("nome_no_json")`.

---

#### 📌 O padrão Builder

O Retrofit usa o padrão **Builder** para sua construção:

```kotlin
Retrofit.Builder()       // 1. Cria o builder
    .baseUrl("...")       // 2. Configura a URL
    .addConverterFactory(...) // 3. Configura o conversor
    .build()             // 4. Constrói o objeto final
```

> 💡 **Para o aluno:** O padrão Builder é usado quando um objeto tem muitas configurações opcionais. Em vez de um construtor com 10 parâmetros (difícil de usar e lembrar a ordem), você encadeia apenas os métodos que precisa. É como **montar um sanduíche no Subway** — você escolhe só o que quer, na ordem que quiser.

---

### 4. ViewModel — `viewmodel/CryptoViewModel.kt`

O coração da lógica do aplicativo. Gerencia o **estado da UI** e faz a chamada à API. Este arquivo contém **1 sealed class com 4 estados**, **1 ViewModel** e **2 funções**:

---

#### 4.1 `sealed class CryptoUiState`

```kotlin
sealed class CryptoUiState {
    object Initial : CryptoUiState()
    object Loading : CryptoUiState()
    data class Success(val ticker: TickerResponse) : CryptoUiState()
    data class Error(val message: String) : CryptoUiState()
}
```

Define **todos os estados possíveis** da interface. Cada estado representa um momento diferente na vida da tela:

| Estado | Quando ocorre | Dados que carrega |
|---|---|---|
| `Initial` | App acabou de abrir ou usuário voltou à tela inicial | Nenhum |
| `Loading` | Requisição à API em andamento | Nenhum |
| `Success` | API respondeu com sucesso | `ticker: TickerResponse` |
| `Error` | API falhou ou sem internet | `message: String` |

**Por que `sealed class` e não `enum`?**

Um `enum` só pode ter valores simples. Uma `sealed class` permite que cada estado carregue **dados diferentes**:

```kotlin
// enum NÃO consegue fazer isso:
enum class UiState {
    SUCCESS // não tem como guardar o ticker aqui
}

// sealed class CONSEGUE:
data class Success(val ticker: TickerResponse) : CryptoUiState()
data class Error(val message: String) : CryptoUiState()
```

Além disso, o compilador Kotlin **garante** que todo `when` trate todos os estados — se você esquecer um caso, o código nem compila.

> 💡 **Para o aluno:** Pense na `sealed class` como um **semáforo avançado**. Um semáforo comum tem 3 luzes (verde, amarelo, vermelho). A `sealed class` é um semáforo onde cada luz pode também trazer informação adicional — a luz verde (Success) acende e já traz o valor da cotação junto.

---

#### 4.2 `class CryptoViewModel`

```kotlin
class CryptoViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<CryptoUiState>(CryptoUiState.Initial)
    val uiState: StateFlow<CryptoUiState> = _uiState.asStateFlow()
    private val service = MercadoBitcoinServiceFactory().create()
}
```

Herda de `ViewModel()` — uma classe do Jetpack que **sobrevive a rotações de tela** e outras recriações de Activity. Isso é fundamental: se o usuário girar o celular, o Android recria a `Activity` do zero, mas o `ViewModel` permanece vivo com o estado intacto.

**Três responsabilidades desta classe:**
1. Armazenar e expor o estado da UI (`_uiState` / `uiState`)
2. Criar o serviço de API (`service`)
3. Executar as operações de negócio (`fetchTickerData`, `resetToInitial`)

---

#### 📌 O que é State (Estado)?

**State** é um dos conceitos mais fundamentais do desenvolvimento moderno — entendê-lo bem muda completamente a forma como você pensa em interfaces.

**A definição simples:**

> State é qualquer dado que, quando muda, faz a interface precisar ser atualizada.

**No mundo antigo (XML + Views imperativas):**

```kotlin
// Você era responsável por atualizar CADA elemento manualmente
progressBar.visibility = View.VISIBLE
textViewPreco.visibility = View.GONE
buttonAtualizar.isEnabled = false
recyclerView.adapter = null
// ... e assim por diante para cada widget
```

O problema: se você esquecer de atualizar um elemento, a tela fica inconsistente. Com 10 elementos na tela, são 10 lugares para errar.

**No mundo moderno (Compose + State):**

```kotlin
// Você define UM estado
_uiState.value = CryptoUiState.Loading

// A tela reage automaticamente a ele
when (state) {
    is Loading -> CircularProgressIndicator() // Compose cuida do resto
    is Success -> CryptoContent(...)
    ...
}
```

Você muda **um único valor**, e toda a tela se atualiza de forma consistente e automática.

---

**Como o State flui no projeto:**

```
Usuário clica "Carregar Cotação"
            │
            ▼
    viewModel.fetchTickerData()
            │
            ▼
    _uiState.value = Loading   ← STATE MUDA
            │
            ▼
    CryptoMonitorScreen detecta a mudança
    (via collectAsState())
            │
            ▼
    Compose redesenha a tela
    mostrando CircularProgressIndicator
            │
            ▼
    API responde com sucesso
            │
            ▼
    _uiState.value = Success(ticker)   ← STATE MUDA
            │
            ▼
    CryptoMonitorScreen detecta a mudança
            │
            ▼
    Compose redesenha a tela
    mostrando CryptoContent com os dados
```

> 💡 **Para o aluno:** Pense no State como o **roteiro de um teatro**. O roteiro (state) diz qual cena está acontecendo. Os atores (composables) simplesmente seguem o roteiro. Quando o diretor (ViewModel) muda a cena no roteiro, todos os atores automaticamente mudam seu comportamento — sem que o diretor precise falar individualmente com cada um.

---

**`MutableStateFlow` vs `StateFlow` — o par público/privado:**

```kotlin
private val _uiState = MutableStateFlow<CryptoUiState>(CryptoUiState.Initial)
val uiState: StateFlow<CryptoUiState> = _uiState.asStateFlow()
```

Esta é uma convenção muito importante no Android moderno:

| | `_uiState` (privado) | `uiState` (público) |
|---|---|---|
| Tipo | `MutableStateFlow` | `StateFlow` |
| Quem acessa | Apenas o ViewModel | A tela (Compose) |
| Pode alterar? | ✅ Sim | ❌ Não — só leitura |
| Prefixo | `_` (underscore) | Sem prefixo |

O `_uiState` é o "controle remoto com todos os botões". O `uiState` é a "tela da TV" — você vê o que está passando, mas não pode mudar o canal.

Isso aplica o princípio de **encapsulamento**: somente o ViewModel tem autoridade para mudar o estado. A tela é apenas uma observadora.

---

#### 4.3 `fun resetToInitial()`

```kotlin
fun resetToInitial() {
    _uiState.value = CryptoUiState.Initial
}
```

A função mais simples do projeto — uma linha só. Reseta o estado para `Initial`, o que faz o Compose automaticamente trocar `CryptoContent` por `InitialContent`.

**Quando é chamada:** ao clicar no botão **"VOLTAR À TELA INICIAL"** em `CryptoContent`.

> 💡 **Para o aluno:** Repare que a função não toca na interface diretamente. Ela apenas muda o estado. É a tela que reage — e isso é a essência do MVVM. O ViewModel nunca sabe como a interface está sendo desenhada.

---

#### 4.4 `fun fetchTickerData()`

```kotlin
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
```

A função mais complexa do projeto. Vamos dissecar cada parte:

**`viewModelScope.launch { }`**
Abre uma coroutine no escopo do ViewModel. Tudo dentro deste bloco roda de forma assíncrona sem bloquear a UI. (Veja seção 2 para detalhes sobre coroutines)

**`_uiState.value = CryptoUiState.Loading`**
Imediatamente ao iniciar, muda o estado para `Loading`. A tela já exibe o `CircularProgressIndicator` antes mesmo de a requisição começar.

**`try { } catch (e: Exception) { }`**
Envolve toda a chamada de rede em um bloco de tratamento de erros. Se ocorrer qualquer exceção (sem internet, timeout, etc.), o `catch` captura e muda o estado para `Error`.

**`response.isSuccessful`**
Verifica se o código HTTP da resposta é entre `200` e `299`. Mesmo que a requisição chegue ao servidor, ele pode retornar um erro (ex: `404 Not Found`).

**`response.body()?.let { } ?: run { }`**
- `response.body()` — extrai o objeto `TickerResponse` do corpo da resposta
- `?.let { }` — executa o bloco apenas se o body **não for nulo** (operador safe-call)
- `?: run { }` — executa este bloco se o body **for nulo** (Elvis operator)

**`when (response.code())`**
Mapeia os principais códigos de erro HTTP para mensagens amigáveis:

| Código | Significado |
|---|---|
| `400` | Bad Request — requisição mal formada |
| `401` | Unauthorized — sem autenticação |
| `403` | Forbidden — sem permissão |
| `404` | Not Found — endpoint não existe |

> 💡 **Para o aluno:** Repare no fluxo de estados dentro desta função: ela **sempre** termina em `Success` ou `Error` — nunca fica presa em `Loading`. Isso é importante: uma UI que fica "carregando para sempre" por causa de um erro não tratado é um dos bugs mais frustrantes para o usuário.

---

### 5. Tela — `ui/theme/screens/CryptoMonitorScreen.kt`

Toda a interface é construída com **Jetpack Compose** — uma abordagem declarativa onde você descreve *como a tela deve parecer* para cada estado, e o framework cuida de atualizar o que mudou.

Este arquivo contém **6 funções**, cada uma com uma responsabilidade clara:

---

#### 5.1 `CryptoMonitorScreen(viewModel, modifier)`

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoMonitorScreen(
    viewModel: CryptoViewModel,
    modifier: Modifier = Modifier
)
```

É o **composable raiz** da tela — o ponto de entrada da interface. Ele é chamado diretamente pela `MainActivity`.

**O que ele faz:**
- Observa o `uiState` do ViewModel usando `collectAsState()`, que converte o `StateFlow` em um estado reativo do Compose
- Monta o `Scaffold` com a `TopAppBar` ("Crypto Monitor") no topo
- Dentro do `Scaffold`, usa um `when` para decidir **qual composable renderizar** de acordo com o estado atual:

```kotlin
when (val state = uiState) {
    is CryptoUiState.Initial  -> InitialContent(...)
    is CryptoUiState.Loading  -> CircularProgressIndicator()
    is CryptoUiState.Success  -> CryptoContent(...)
    is CryptoUiState.Error    -> ErrorContent(...)
}
```

> 💡 **Para o aluno:** O `val state = uiState` dentro do `when` é um **smart cast** — o Kotlin já sabe que dentro de cada bloco `is CryptoUiState.Success`, por exemplo, o `state` é do tipo `Success`, dando acesso direto à propriedade `state.ticker` sem nenhum cast manual.

---

#### 5.2 `CryptoContent(ticker, onRefresh, onBack)`

```kotlin
@Composable
fun CryptoContent(
    ticker: TickerResponse,
    onRefresh: () -> Unit,
    onBack: () -> Unit
)
```

Exibido quando o estado é `CryptoUiState.Success`. É a tela principal com os **dados reais da cotação**.

**O que ele faz:**
- Renderiza um `Card` com fundo `surfaceVariant` contendo:
  - Nome **"Bitcoin (BTC)"** em destaque
  - **Preço atual** (`ticker.last`) formatado em BRL com `NumberFormat`, em verde (`0xFF4CAF50`)
  - **Data/hora** da cotação convertida do timestamp Unix (`ticker.date * 1000L`) usando `SimpleDateFormat`
  - Um `HorizontalDivider` separando o preço das informações adicionais
  - Dois `Row`s com `InfoItem`s para **Máxima/Mínima** e **Compra/Venda**
  - Um `InfoItem` para o **Volume**
- Abaixo do card, dois botões:
  - `Button` → **"ATUALIZAR COTAÇÃO"** — chama `onRefresh`
  - `OutlinedButton` → **"VOLTAR À TELA INICIAL"** — chama `onBack`

> 💡 **Para o aluno:** Os callbacks `onRefresh` e `onBack` são do tipo `() -> Unit` — funções sem parâmetros e sem retorno. Isso é o padrão no Compose para comunicar eventos da View para o ViewModel sem criar acoplamento direto entre eles.

---

#### 5.3 `InfoItem(label, value, modifier)`

```kotlin
@Composable
fun InfoItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
)
```

Componente reutilizável simples que exibe um **par rótulo + valor** empilhados verticalmente.

**O que ele faz:**
- Renderiza uma `Column` com dois `Text`s:
  - O `label` em estilo `labelMedium` com cor `onSurfaceVariant` (tom mais suave)
  - O `value` em estilo `bodyLarge` com `FontWeight.SemiBold`

**Onde é usado:** em `CryptoContent` para exibir Máxima, Mínima, Compra, Venda e Volume.

> 💡 **Para o aluno:** O parâmetro `modifier: Modifier = Modifier` com valor padrão é uma convenção do Compose — permite que quem chama o composable passe um modifier customizado (como `fillMaxWidth()`), mas não obriga. Isso torna o componente **flexível e reutilizável**.

---

#### 5.4 `ErrorContent(message, onRetry)`

```kotlin
@Composable
fun ErrorContent(
    message: String,
    onRetry: () -> Unit
)
```

Exibido quando o estado é `CryptoUiState.Error`. Informa o usuário sobre o problema e oferece uma saída.

**O que ele faz:**
- Exibe o emoji **❌** em tamanho grande (`64.sp`)
- Exibe o título **"Erro ao carregar dados"**
- Exibe a `message` de erro (vinda do ViewModel) em cor `MaterialTheme.colorScheme.error`
- Exibe um `Button` **"Tentar Novamente"** com ícone de refresh que chama `onRetry`

> 💡 **Para o aluno:** A mensagem de erro é gerada no `CryptoViewModel` com base no código HTTP ou na exceção lançada. A tela não sabe a origem do erro — ela apenas exibe o que recebe. Isso é separação de responsabilidades na prática.

---

#### 5.5 `InitialContent(onLoadData)`

```kotlin
@Composable
fun InitialContent(
    onLoadData: () -> Unit
)
```

Exibido quando o estado é `CryptoUiState.Initial` — ou seja, quando o app acabou de abrir ou o usuário voltou à tela inicial.

**O que ele faz:**
- Exibe o símbolo **₿** em `80.sp` com a cor primária do tema
- Exibe o título **"Crypto Monitor"**
- Exibe o subtítulo **"Acompanhe a cotação do Bitcoin em tempo real"**
- Exibe um `Button` **"Carregar Cotação"** com ícone de refresh que chama `onLoadData`

> 💡 **Para o aluno:** Esta tela é intencional — o app não carrega dados automaticamente ao abrir. Isso respeita a experiência do usuário (ele decide quando buscar) e evita chamadas de rede desnecessárias. É uma escolha de design de UX, não uma limitação técnica.

---

#### 5.6 `formatCurrency(value): String`

```kotlin
fun formatCurrency(value: String): String
```

Função utilitária (não é um composable) responsável por **formatar valores numéricos em moeda brasileira**.

**O que ele faz:**
- Tenta converter a `String` recebida em `Double` com `toDoubleOrNull()`
- Se a conversão falhar (valor inválido), retorna a `String` original sem modificação
- Se der certo, usa `NumberFormat.getCurrencyInstance()` com `Locale` configurado para `pt-BR` para formatar o valor como moeda (ex: `R$ 650.000,50`)

**Onde é usado:** em `CryptoContent` para formatar `high`, `low`, `buy` e `sell` do ticker.

> 💡 **Para o aluno:** O uso de `Locale.Builder().setLanguage("pt").setRegion("BR").build()` em vez de `Locale("pt", "BR")` é a forma moderna e não deprecada de criar um Locale específico no Java/Kotlin. Garante que o símbolo `R$`, o ponto como separador de milhar e a vírgula como separador decimal sejam aplicados corretamente.

---

### 6. MainActivity — `MainActivity.kt`

É o **ponto de entrada do aplicativo Android**. Todo app Android precisa de pelo menos uma `Activity` — ela é a porta de entrada que o sistema operacional conhece e sabe como abrir.

```kotlin
class MainActivity : ComponentActivity() {
    private val viewModel: CryptoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CryptomonitorTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CryptoMonitorScreen(viewModel = viewModel)
                }
            }
        }
    }
}
```

---

#### 6.1 Herança — `class MainActivity : ComponentActivity()`

```kotlin
class MainActivity : ComponentActivity()
```

O `:` no Kotlin indica **herança** — `MainActivity` herda de `ComponentActivity`.

A hierarquia completa de herança é:

```
Activity                          ← classe base do Android (SO)
    └── ComponentActivity         ← adiciona suporte a Lifecycle e ViewModel
            └── MainActivity      ← nossa classe, ponto de entrada do app
```

**Por que `ComponentActivity` e não `Activity` diretamente?**

A `ComponentActivity` adiciona suporte a recursos modernos do Jetpack, como:
- **`viewModels()`** — delegação de ViewModel com ciclo de vida correto
- **`setContent { }`** — integração com Jetpack Compose
- **Lifecycle awareness** — sabe quando está em primeiro plano, pausada, destruída, etc.

> 💡 **Para o aluno:** Herança é um dos pilares da Orientação a Objetos. Quando `MainActivity` herda de `ComponentActivity`, ela **ganha automaticamente** todo o comportamento já implementado na classe pai — como gerenciar o ciclo de vida, tratar rotações de tela e integrar com o sistema Android. Você só precisa sobrescrever o que é específico do seu app.

---

#### 6.2 `private val viewModel: CryptoViewModel by viewModels()`

```kotlin
private val viewModel: CryptoViewModel by viewModels()
```

Cria e obtém a instância do `CryptoViewModel` usando **delegação de propriedade** (`by`).

**O que o `by viewModels()` faz:**
- Na **primeira vez** que a Activity é criada → instancia o `CryptoViewModel`
- Se a Activity for **recriada** (rotação de tela, mudança de tema) → retorna a **mesma instância** já existente, preservando o estado

Sem `by viewModels()`, você teria que gerenciar isso manualmente com `ViewModelProvider`, o que é mais verboso e propenso a erros.

> 💡 **Para o aluno:** Imagine que você está preenchendo um formulário longo no celular e gira a tela. Sem `viewModels()`, todos os dados preenchidos seriam perdidos porque a Activity recomeça do zero. Com `viewModels()`, o ViewModel sobrevive à recriação e os dados permanecem intactos.

---

#### 6.3 `override fun onCreate(savedInstanceState: Bundle?)`

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // ...
}
```

`onCreate` é um método do **ciclo de vida da Activity**, herdado de `ComponentActivity` → `Activity`. O `override` indica que estamos **sobrescrevendo** o comportamento padrão da classe pai para adicionar o nosso.

**O ciclo de vida completo de uma Activity:**

```
onCreate()   ← app abre — aqui configuramos a UI
    │
onStart()    ← Activity fica visível
    │
onResume()   ← Activity em primeiro plano, interagindo com usuário
    │
onPause()    ← outra Activity vem para frente
    │
onStop()     ← Activity não está mais visível
    │
onDestroy()  ← Activity é destruída (usuário fechou ou SO matou)
```

O `onCreate` é chamado **uma vez** quando a Activity é criada — é o lugar certo para configurar a interface, obter o ViewModel e inicializar recursos.

**`super.onCreate(savedInstanceState)`**

A primeira linha sempre chama o `onCreate` da classe pai. Isso garante que toda a inicialização interna do Android (gerenciamento de estado, fragmentos, etc.) aconteça antes do nosso código. Nunca pule essa chamada.

**`savedInstanceState: Bundle?`**

É um pacote de dados que o Android salva automaticamente antes de destruir a Activity em situações como rotação de tela. Quando a Activity é recriada, esse Bundle é passado de volta com os dados salvos. O `?` indica que pode ser nulo — na primeira abertura do app, não há nada salvo.

> 💡 **Para o aluno:** O `override` é como um funcionário que recebe uma tarefa padrão da empresa (`super.onCreate`) mas adiciona seus próprios passos específicos depois. Primeiro segue o procedimento padrão, depois faz o que é particular ao seu caso.

---

#### 6.4 `enableEdgeToEdge()`

```kotlin
enableEdgeToEdge()
```

Configura o app para ocupar **toda a tela**, incluindo as áreas atrás da barra de status (topo) e da barra de navegação (rodapé). O conteúdo se estende por baixo dessas barras do sistema, criando uma experiência visual mais imersiva e moderna.

Sem essa chamada, o app ficaria "dentro" das barras do sistema, com bordas brancas ou pretas nas extremidades.

---

#### 6.5 `setContent { }` — a ponte entre Android e Compose

```kotlin
setContent {
    CryptomonitorTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            CryptoMonitorScreen(viewModel = viewModel)
        }
    }
}
```

`setContent` é a **porta de entrada do Jetpack Compose** na Activity. É ele que diz ao Android: *"a partir daqui, a interface é gerenciada pelo Compose, não por XMLs"*.

Tudo dentro do bloco `setContent { }` é um **composable** — funções que descrevem a interface de forma declarativa.

---

#### 6.6 `CryptomonitorTheme { }` — o tema visual

```kotlin
CryptomonitorTheme {
    // conteúdo dentro do tema
}
```

É o composable definido em `Theme.kt` que aplica o **Material Design 3** a toda a hierarquia de componentes filhos. Ele fornece:

- **Esquema de cores** — primária, secundária, de erro, de superfície, etc.
- **Tipografia** — estilos de texto (`headlineLarge`, `bodyMedium`, `labelSmall`, etc.)
- **Suporte a modo escuro** — detecta automaticamente a preferência do sistema
- **Cores dinâmicas** — no Android 12+, adapta as cores ao papel de parede do usuário

Qualquer composable dentro do tema pode usar `MaterialTheme.colorScheme.primary` ou `MaterialTheme.typography.headlineMedium`, por exemplo, e receberá automaticamente o valor correto do tema.

> 💡 **Para o aluno:** O `CryptomonitorTheme` funciona como um **estilo global de CSS** no desenvolvimento web. Em vez de definir fonte, cor e tamanho em cada elemento individualmente, você define uma vez no tema e todos os componentes filhos herdam automaticamente. Mudar o tema muda a aparência de toda a aplicação de uma vez.

---

#### 6.7 `Surface` — a base da interface

```kotlin
Surface(modifier = Modifier.fillMaxSize()) {
    CryptoMonitorScreen(viewModel = viewModel)
}
```

`Surface` é um composable do Material Design que representa uma **superfície física** — como uma folha de papel sobre uma mesa. Ele:

- Define a **cor de fundo** da tela (por padrão, usa `MaterialTheme.colorScheme.background`)
- Garante que o **conteúdo filho** tenha o contraste correto de cores (texto escuro sobre fundo claro, texto claro sobre fundo escuro)
- Aplica **elevação** e **sombra** quando necessário
- Ocupa toda a tela graças ao `Modifier.fillMaxSize()`

**Por que usar `Surface` em vez de um `Box` ou `Column` diretamente?**

Porque o `Surface` é semanticamente correto no Material Design — ele representa uma camada da interface com propriedades físicas (cor, elevação, forma). Usar `Surface` na raiz garante que o tema seja aplicado corretamente em toda a tela.

> 💡 **Para o aluno:** Pense no `Surface` como a **tela branca de um caderno**. Antes de escrever qualquer coisa, você precisa da folha. O `Surface` é essa folha — ele fornece o fundo sobre o qual todos os outros componentes são desenhados, com as cores e estilos corretos do tema.

---


## 🧠 Conceitos Fundamentais

### 📌 Model vs ViewModel — qual a diferença?

Esta é uma das dúvidas mais comuns de quem está aprendendo MVVM. Os nomes são parecidos, mas as responsabilidades são completamente diferentes.

---

#### O que é o Model?

O **Model** representa os **dados puros** da aplicação — ele é apenas uma estrutura que guarda informação. Não tem lógica, não faz chamadas de rede, não sabe nada sobre a interface.

No nosso projeto, o Model é composto por duas classes:

```kotlin
class TickerResponse(val ticker: Ticker)

class Ticker(
    val high: String,  // Maior preço nas últimas 24h
    val low: String,   // Menor preço nas últimas 24h
    val vol: String,   // Volume negociado
    val last: String,  // Último preço negociado
    val buy: String,   // Melhor preço de compra
    val sell: String,  // Melhor preço de venda
    val date: Long     // Timestamp Unix da cotação
)
```

Repare que essas classes **não fazem nada** — elas apenas existem para representar os dados que vêm da API. É o equivalente a uma ficha cadastral: ela guarda informações, mas não age sozinha.

> 💡 **Para o aluno:** Pense no Model como uma **caixa com gavetas**. Cada gaveta tem um nome (`high`, `low`, `last`...) e guarda um valor. A caixa não faz nada sozinha — ela só existe para organizar e transportar os dados.

---

#### O que é o ViewModel?

O **ViewModel** é o **cérebro da tela** — ele contém toda a lógica de negócio, decide o que fazer com os dados e expõe o estado atual para a interface.

No nosso projeto:

```kotlin
class CryptoViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<CryptoUiState>(CryptoUiState.Initial)
    val uiState: StateFlow<CryptoUiState> = _uiState.asStateFlow()

    fun fetchTickerData() { ... } // busca dados na API
    fun resetToInitial() { ... }  // reseta o estado
}
```

O ViewModel:
- **Faz chamadas** à API via `MercadoBitcoinServiceFactory`
- **Recebe** o `TickerResponse` (Model) como resultado
- **Decide** o que fazer com ele (emitir `Success`, `Error`, etc.)
- **Expõe** o estado para a tela via `StateFlow`

> 💡 **Para o aluno:** Se o Model é a **caixa com gavetas**, o ViewModel é o **gerente do depósito**. Ele sabe onde buscar as caixas (API), sabe o que fazer quando uma caixa chega (atualizar o estado) e sabe o que fazer quando algo dá errado (emitir erro). A loja (a tela) só espera o gerente avisar que tem produto disponível.

---

#### A diferença resumida em uma tabela:

| | **Model** | **ViewModel** |
|---|---|---|
| **O que é** | Estrutura de dados | Gerenciador de lógica e estado |
| **O que faz** | Nada — apenas guarda dados | Busca dados, trata erros, expõe estado |
| **Conhece a tela?** | ❌ Não | ❌ Não |
| **Conhece a API?** | ❌ Não | ✅ Sim (via ServiceFactory) |
| **Sobrevive à rotação?** | Depende de quem o segura | ✅ Sim — herda de `ViewModel()` |
| **Exemplo no projeto** | `TickerResponse`, `Ticker` | `CryptoViewModel` |

---

### 📌 CryptoUiState vs CryptoViewModel — qual a diferença?

Outro ponto que gera confusão: ambos estão no mesmo arquivo, mas têm papéis completamente distintos.

---

#### O que é o `CryptoUiState`?

É uma **sealed class** que define **o que a tela pode estar mostrando** em cada momento. Ela não faz nada — apenas representa os possíveis estados:

```kotlin
sealed class CryptoUiState {
    object Initial : CryptoUiState()
    object Loading : CryptoUiState()
    data class Success(val ticker: TickerResponse) : CryptoUiState()
    data class Error(val message: String) : CryptoUiState()
}
```

Pense no `CryptoUiState` como um **semáforo** — ele só indica em qual estado o sistema está. Não é ele quem muda a luz; quem faz isso é o `CryptoViewModel`.

**Características do `CryptoUiState`:**
- É passivo — apenas descreve um estado
- Não tem funções
- Pode carregar dados (`Success` carrega o `ticker`, `Error` carrega a `message`)
- É imutável — uma vez criado, não muda

---

#### O que é o `CryptoViewModel`?

É a **classe ativa** — ela age, decide e comunica. É quem:

1. **Cria** o estado inicial (`CryptoUiState.Initial`)
2. **Transiciona** entre os estados conforme os eventos acontecem
3. **Expõe** o estado atual para a tela via `StateFlow`
4. **Responde** às ações do usuário (`fetchTickerData`, `resetToInitial`)

```kotlin
class CryptoViewModel : ViewModel() {

    // Guarda e expõe o estado atual
    private val _uiState = MutableStateFlow<CryptoUiState>(CryptoUiState.Initial)
    val uiState: StateFlow<CryptoUiState> = _uiState.asStateFlow()

    // Muda o estado conforme os eventos
    fun fetchTickerData() {
        _uiState.value = CryptoUiState.Loading   // ← muda para Loading
        // ...
        _uiState.value = CryptoUiState.Success(...) // ← muda para Success
    }
}
```

---

#### A relação entre os dois:

```
CryptoViewModel                          CryptoUiState
     │                                        │
     │  _uiState.value = CryptoUiState.Loading
     │ ─────────────────────────────────────► │
     │                                        │
     │  _uiState.value = CryptoUiState.Success(ticker)
     │ ─────────────────────────────────────► │
     │                                        │
     │                              CryptoMonitorScreen
     │                              observa via collectAsState()
     │                              e redesenha a tela
```

> 💡 **Para o aluno:** `CryptoUiState` é o **placar do jogo** — ele mostra o resultado atual (0x0, 1x0, fim de jogo). `CryptoViewModel` é o **árbitro** — ele é quem atualiza o placar conforme os eventos do jogo acontecem. A tela (`CryptoMonitorScreen`) é a **torcida** — ela olha para o placar e reage ao que está vendo.

---

#### A diferença resumida em uma tabela:

| | **CryptoUiState** | **CryptoViewModel** |
|---|---|---|
| **Tipo** | `sealed class` | `class` que herda de `ViewModel()` |
| **Papel** | Descrever o estado | Gerenciar e transicionar o estado |
| **Tem funções?** | ❌ Não | ✅ Sim (`fetchTickerData`, `resetToInitial`) |
| **Quem o cria?** | O próprio `CryptoViewModel` | O Android (via `by viewModels()`) |
| **A tela conhece?** | ✅ Sim — usa no `when` | ✅ Sim — chama suas funções |
| **É ativo ou passivo?** | Passivo — só descreve | Ativo — age e decide |

---

## 📚 Dependências Utilizadas

Gerenciadas pelo **Version Catalog** (`gradle/libs.versions.toml`) — uma forma centralizada e moderna de declarar dependências no Android.

| Biblioteca | Finalidade |
|---|---|
| `Jetpack Compose BOM` | Gerencia versões de todas as libs do Compose |
| `Material3` | Componentes visuais modernos (botões, cards, etc.) |
| `Lifecycle ViewModel KTX` | Suporte ao ViewModel com coroutines |
| `Activity Compose` | Integra o Compose com a Activity |
| `Retrofit` | Cliente HTTP para chamadas à API |
| `Converter Gson` | Converte JSON em objetos Kotlin |
| `Coroutines (via viewModelScope)` | Execução assíncrona sem bloquear a UI |

---

## 🔄 Fluxo Completo do App

```
Usuário abre o app
        │
        ▼
  [InitialContent]
  Tela de boas-vindas
        │
        │ clica em "Carregar Cotação"
        ▼
  [Loading]
  viewModel.fetchTickerData()
  Retrofit chama a API do Mercado Bitcoin
        │
        ├── sucesso ──► [CryptoContent] exibe cotação
        │                     │
        │               clica "Atualizar" ──► repete o fluxo
        │               clica "Voltar"    ──► volta ao Initial
        │
        └── erro ────► [ErrorContent] exibe mensagem
                              │
                        clica "Tentar Novamente" ──► repete o fluxo
```

---

## 🛠️ Como executar o projeto

### Pré-requisitos

- Android Studio Hedgehog ou superior
- JDK 17+
- Emulador ou dispositivo físico com Android 7.0+ (API 24+)
- Conexão com a internet (para buscar a cotação)

### Passos

```bash
# Clone o repositório
git clone https://github.com/carreiras/crypto-monitor-declarative-architecture.git

# Abra no Android Studio e aguarde a sincronização do Gradle
# Execute com Shift + F10 ou clique em Run ▶
```

---

## 🗂️ Histórico de Commits

Os commits foram organizados de forma didática, seguindo a ordem natural das camadas da arquitetura MVVM — do dado mais simples (Model) até o ponto de entrada do app (MainActivity). Cada commit representa uma única responsabilidade, facilitando o acompanhamento do histórico pelo aluno.

| # | Mensagem | O que foi feito |
|---|---|---|
| 1 | `first commit` | Estrutura inicial do projeto |
| 2 | `chore: remove .idea from git tracking and update .gitignore` | Removida a pasta `.idea` do rastreamento Git e `.gitignore` reorganizado |
| 3 | `build: update AndroidManifest, app build.gradle and libs.versions.toml` | Atualização das configurações de build e dependências |
| 4 | `feat: TickerResponse e Ticker` | Criação das classes de modelo `TickerResponse` e `Ticker` |
| 5 | `feat: MercadoBitcoinService` | Criação da interface de comunicação com a API |
| 6 | `feat: MercadoBitcoinServiceFactory` | Criação da classe fábrica do Retrofit |
| 7 | `feat: CryptoUiState e CryptoViewModel` | Criação da sealed class de estados e do ViewModel |
| 8 | `feat: CryptoMonitorScreen` | Criação dos composables da tela e correção de APIs deprecadas |
| 9 | `feat: CryptoMonitorScreen Previews` | Criação dos previews para cada composable da tela — tema claro, tema escuro e componentes isolados |
| 10 | `feat: MainActivity` | Criação da Activity — ponto de entrada do app |
| 11 | `feat: README` | Criação do README com explicação detalhada de cada camada |

---

## 📝 Documentação

Todos os arquivos do projeto possuem documentação no padrão **KDoc** (equivalente ao JavaDoc para Kotlin), cobrindo:

| Arquivo | O que está documentado |
|---|---|
| `TickerResponse.kt` | Classes de modelo e todas as propriedades |
| `MercadoBitcoinService.kt` | Interface e o método `getTicker()` |
| `MercadoBitcoinServiceFactory.kt` | Classe fábrica e o método `create()` |
| `CryptoViewModel.kt` | Sealed class, todos os estados, ViewModel e funções |
| `CryptoMonitorScreen.kt` | Todos os composables e a função `formatCurrency()` |
| `MainActivity.kt` | Papel da Activity e o uso de `by viewModels()` |
| `Theme.kt` | Tema principal e os esquemas de cores claro/escuro |
| `Color.kt` | Paleta de cores com explicação das variantes 40/80 |
| `Type.kt` | Sistema tipográfico do Material Design 3 |
| `ExampleUnitTest.kt` | Estrutura e propósito dos testes unitários |
| `ExampleInstrumentedTest.kt` | Estrutura e propósito dos testes instrumentados |

---

## ✅ Boas Práticas Aplicadas

- ✔ **Separação de responsabilidades** com MVVM
- ✔ **StateFlow** para gerenciamento reativo de estado
- ✔ **Sealed class** para estados tipados e seguros
- ✔ **Coroutines** para chamadas assíncronas sem callbacks
- ✔ **Version Catalog** para gerenciamento centralizado de dependências
- ✔ **`.gitignore`** configurado corretamente (sem arquivos de IDE no repositório)
- ✔ **KDoc** para documentação das classes de modelo

---

## 👨‍🏫 Considerações Finais

Este projeto demonstra como construir um app Android moderno do zero, aplicando os conceitos mais utilizados no mercado hoje:

> *"Em vez de dizer ao app **como** atualizar a tela, você diz **como a tela deve parecer** para cada estado — e o Compose cuida do resto."*

Esse é o princípio da **programação declarativa**, e é exatamente o que diferencia o desenvolvimento Android moderno do legado com XML e `findViewById`.

