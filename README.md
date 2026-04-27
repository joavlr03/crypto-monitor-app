# 📱 Crypto Monitor

Aplicativo Android que exibe a **cotação em tempo real do Bitcoin (BTC)** em reais (BRL), consumindo a API pública do [Mercado Bitcoin](https://www.mercadobitcoin.com.br/).

---

# 📱 Prints das telas do app em execução
<img width="281" height="627" alt="Captura de tela 2026-04-27 165119" src="https://github.com/user-attachments/assets/c9d57cbd-dabe-44f2-8b07-ab7d80d13328" />
<img width="286" height="636" alt="Captura de tela 2026-04-27 165133" src="https://github.com/user-attachments/assets/5386a473-f0ff-47e2-9ac6-c02a5c240f29" />



## 🚀 Para que serve

O Crypto Monitor permite acompanhar, com um toque, as informações mais importantes do Bitcoin no mercado brasileiro:

- 💰 Preço da última negociação
- 📈 Máxima e mínima das últimas 24h
- 🛒 Melhor preço de compra e venda
- 📦 Volume negociado nas últimas 24h
- 🕐 Horário exato da cotação

---

## ⚙️ Como executar

### Pré-requisitos

- [Android Studio](https://developer.android.com/studio) (versão Ladybug ou superior)
- JDK 21
- Android SDK com API 24 ou superior
- Emulador AVD ou dispositivo físico conectado

### Passo a passo

```bash
# 1. Clone o repositório
git clone https://github.com/SEU_USUARIO/crypto-monitor.git

# 2. Abra o projeto no Android Studio
# File → Open → selecione a pasta do projeto

# 3. Aguarde o Gradle sincronizar

# 4. Execute o app
# Clique no botão ▶ Run (ou Shift+F10)
```

> O app requer conexão com a internet para buscar os dados da API.

---

## 🌐 A — Comunicação com o Mercado Bitcoin

O app consome a API REST pública do Mercado Bitcoin, sem necessidade de autenticação.

**Endpoint utilizado:**

```
GET https://www.mercadobitcoin.net/api/BTC/ticker/
```

A comunicação é feita via **Retrofit**, uma biblioteca HTTP para Android que transforma chamadas de rede em funções Kotlin. A interface `MercadoBitcoinService` declara o endpoint com a anotação `@GET`:

```kotlin
interface MercadoBitcoinService {
    @GET("api/BTC/ticker/")
    suspend fun getTicker(): Response<TickerResponse>
}
```

A função é `suspend`, o que significa que ela roda de forma assíncrona dentro de uma **coroutine**, sem bloquear a thread principal da UI.

**Exemplo de resposta da API:**

```json
{
  "ticker": {
    "high": "680000.00",
    "low": "620000.00",
    "vol": "12.34567",
    "last": "650000.00",
    "buy": "649000.00",
    "sell": "651000.00",
    "date": 1714165200
  }
}
```

---

## 🔄 B — Conversão do Payload para Classes Kotlin

O Retrofit utiliza o **Gson** (via `GsonConverterFactory`) para converter automaticamente o JSON da resposta em objetos Kotlin. O mapeamento segue a estrutura do JSON:

```
JSON                      →  Kotlin
─────────────────────────────────────
{ "ticker": { ... } }    →  TickerResponse
  "high": "680000.00"    →  Ticker.high: String
  "low":  "620000.00"    →  Ticker.low:  String
  "last": "650000.00"    →  Ticker.last: String
  "buy":  "649000.00"    →  Ticker.buy:  String
  "sell": "651000.00"    →  Ticker.sell: String
  "vol":  "12.34567"     →  Ticker.vol:  String
  "date": 1714165200     →  Ticker.date: Long
```

As classes de modelo ficam em `model/`:

```kotlin
class TickerResponse(
    val ticker: Ticker
)

class Ticker(
    val high: String,
    val low: String,
    val vol: String,
    val last: String,
    val buy: String,
    val sell: String,
    val date: Long      // Timestamp Unix em segundos
)
```

Os valores monetários chegam como `String` e são convertidos para `Double` na camada de UI (via `toDoubleOrNull()`) apenas no momento da formatação para exibição — por exemplo, `"650000.00"` se torna `R$ 650.000,00`.

O campo `date` chega como timestamp Unix em segundos e é convertido para data legível multiplicando por 1000 (milissegundos) e usando `SimpleDateFormat`.

---

## 🔀 C — Estados de Tela

O app usa uma `sealed class` chamada `CryptoUiState` para representar todos os estados possíveis da interface. Isso garante que o compilador obrigue o tratamento de cada estado no `when`, eliminando erros em tempo de execução.

```kotlin
sealed class CryptoUiState {
    object Initial : CryptoUiState()
    object Loading : CryptoUiState()
    data class Success(val ticker: TickerResponse) : CryptoUiState()
    data class Error(val message: String) : CryptoUiState()
}
```

| Estado | Quando ocorre | O que a UI exibe |
|---|---|---|
| `Initial` | App recém aberto ou usuário clicou em "Voltar" | Tela de boas-vindas com botão "Carregar Cotação" |
| `Loading` | Chamada à API em andamento | Spinner de carregamento (`CircularProgressIndicator`) |
| `Success` | API retornou dados com sucesso | Card com todos os dados da cotação |
| `Error` | Falha de rede ou erro HTTP | Mensagem de erro com botão "Tentar Novamente" |

O estado é armazenado em um `StateFlow` dentro do `ViewModel` e a tela reage automaticamente a cada mudança.

---

## 🏭 D — Service e Factory

### MercadoBitcoinService

É uma **interface** que declara os endpoints da API. O Retrofit gera automaticamente uma implementação dessa interface em tempo de execução — ou seja, não é necessário escrever o código HTTP manualmente.

```kotlin
interface MercadoBitcoinService {
    @GET("api/BTC/ticker/")
    suspend fun getTicker(): Response<TickerResponse>
}
```

### MercadoBitcoinServiceFactory

Aplica o padrão **Factory Method**: centraliza a criação e configuração do cliente HTTP, evitando que outras classes precisem conhecer os detalhes do Retrofit.

```kotlin
class MercadoBitcoinServiceFactory {
    fun create(): MercadoBitcoinService {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.mercadobitcoin.net/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(MercadoBitcoinService::class.java)
    }
}
```

**Responsabilidades da Factory:**
- Define a URL base da API
- Configura o conversor de JSON (Gson)
- Entrega uma instância pronta de `MercadoBitcoinService` para o `CryptoViewModel`

O `ViewModel` consome a factory assim:

```kotlin
private val service = MercadoBitcoinServiceFactory().create()
```

---

## 🖼️ E — Interface Gráfica por Estados

A UI é construída inteiramente com **Jetpack Compose**. O composable raiz `CryptoMonitorScreen` observa o `uiState` e delega a renderização para composables específicos conforme o estado atual.

### Estado: `Initial` — Tela de Boas-vindas

Exibida ao abrir o app. Apresenta o símbolo ₿, o nome do app e um botão para iniciar a primeira busca.

```
┌─────────────────────┐
│    Crypto Monitor   │  ← TopAppBar
├─────────────────────┤
│                     │
│         ₿           │  ← símbolo do Bitcoin
│                     │
│   Crypto Monitor    │  ← título
│  Acompanhe a cota-  │
│  ção em tempo real  │
│                     │
│  [Carregar Cotação] │  ← dispara fetchTickerData()
│                     │
└─────────────────────┘
```

### Estado: `Loading` — Carregando

Exibido enquanto a API está sendo consultada. A UI fica bloqueada com um spinner central.

```
┌─────────────────────┐
│    Crypto Monitor   │
├─────────────────────┤
│                     │
│          ◌          │  ← CircularProgressIndicator
│                     │
└─────────────────────┘
```

### Estado: `Success` — Cotação Carregada

Exibe um card com todos os dados retornados pela API, mais dois botões de ação.

```
┌─────────────────────┐
│    Crypto Monitor   │
├─────────────────────┤
│  ┌───────────────┐  │
│  │  Bitcoin(BTC) │  │
│  │               │  │
│  │ R$650.000,00  │  │  ← last (verde)
│  │ Atualizado em │  │  ← date formatado
│  │  01/01/2025   │  │
│  │───────────────│  │
│  │ Máx   │  Mín  │  │  ← high / low
│  │ Compr │  Vend │  │  ← buy / sell
│  │    Volume     │  │  ← vol
│  └───────────────┘  │
│                     │
│  [↺ ATUALIZAR]      │  ← fetchTickerData()
│  [VOLTAR INICIAL]   │  ← resetToInitial()
└─────────────────────┘
```

### Estado: `Error` — Erro

Exibido quando a chamada falha (sem internet, timeout, erro HTTP). Mostra a causa do erro e permite tentar novamente.

```
┌─────────────────────┐
│    Crypto Monitor   │
├─────────────────────┤
│                     │
│         ❌          │
│                     │
│  Erro ao carregar   │
│       dados         │
│                     │
│  Falha na conexão   │  ← message do CryptoUiState.Error
│                     │
│  [Tentar Novamente] │  ← fetchTickerData()
│                     │
└─────────────────────┘
```

---

## 🗂️ Estrutura do Projeto

```
app/src/main/java/carreiras/com/github/cryptomonitor/
│
├── MainActivity.kt                    # Ponto de entrada, inicializa o Compose
│
├── model/
│   └── TickerResponse.kt              # Classes de dados (TickerResponse, Ticker)
│
├── service/
│   ├── MercadoBitcoinService.kt       # Interface com os endpoints da API
│   └── MercadoBitcoinServiceFactory.kt # Factory que configura o Retrofit
│
├── viewmodel/
│   └── CryptoViewModel.kt             # Lógica de negócio + estados (CryptoUiState)
│
└── ui/theme/
    ├── Color.kt                       # Paleta de cores
    ├── Theme.kt                       # Tema Material 3
    ├── Type.kt                        # Tipografia
    └── screens/
        └── CryptoMonitorScreen.kt     # Toda a interface gráfica (Composables)
```

---

## 🛠️ Tecnologias utilizadas

| Tecnologia | Uso |
|---|---|
| Kotlin | Linguagem principal |
| Jetpack Compose | Interface gráfica declarativa |
| Retrofit | Cliente HTTP para chamadas à API |
| Gson | Conversão de JSON para objetos Kotlin |
| Coroutines | Chamadas assíncronas sem bloquear a UI |
| ViewModel + StateFlow | Gerenciamento de estado seguindo MVVM |
| Material 3 | Design system com suporte a tema claro/escuro 
