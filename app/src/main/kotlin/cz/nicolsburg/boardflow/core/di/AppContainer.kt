package cz.nicolsburg.boardflow.core.di

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import cz.nicolsburg.boardflow.data.BggRepository
import cz.nicolsburg.boardflow.data.CanonicalCollectionStore
import cz.nicolsburg.boardflow.data.GeminiRepository
import cz.nicolsburg.boardflow.data.SecurePreferences
import cz.nicolsburg.boardflow.data.chronicle.FallbackChronicleComposer
import cz.nicolsburg.boardflow.data.chronicle.GeminiChronicleLineGenerator
import cz.nicolsburg.boardflow.data.chronicle.SessionChronicleService

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val securePreferences = SecurePreferences(appContext)
    val canonicalCollectionStore = CanonicalCollectionStore.getInstance(appContext)
    val bggRepository = BggRepository()
    val geminiRepo = GeminiRepository()
    val chronicleService = SessionChronicleService(
        lineGenerator = GeminiChronicleLineGenerator(),
        fallbackComposer = FallbackChronicleComposer()
    )

    fun isOnline(): Boolean {
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
