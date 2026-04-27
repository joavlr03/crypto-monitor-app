package carreiras.com.github.cryptomonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import carreiras.com.github.cryptomonitor.ui.theme.CryptomonitorTheme
import carreiras.com.github.cryptomonitor.ui.theme.screens.CryptoMonitorScreen
import carreiras.com.github.cryptomonitor.viewmodel.CryptoViewModel

/**
 * Ponto de entrada principal do aplicativo.
 *
 * Responsável por inicializar o tema visual, configurar o modo edge-to-edge
 * (tela cheia sem bordas do sistema) e entregar o controle da interface ao
 * Jetpack Compose por meio da função [CryptoMonitorScreen].
 *
 * O [CryptoViewModel] é obtido via delegação `by viewModels()`, garantindo que
 * a instância sobreviva a rotações de tela e outras recriações de Activity.
 */
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
