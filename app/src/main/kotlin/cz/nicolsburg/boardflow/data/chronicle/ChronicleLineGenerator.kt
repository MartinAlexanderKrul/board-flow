package cz.nicolsburg.boardflow.data.chronicle

data class ChronicleRequest(
    val gameName: String,
    val moods: List<String>,
    val quote: String,
    val playerNames: List<String>,
    val playerColors: List<String>
)

data class ChronicleAiConfig(
    val apiKey: String,
    val modelName: String,
    val availableModels: List<String>
)

interface ChronicleLineGenerator {
    suspend fun generate(request: ChronicleRequest, config: ChronicleAiConfig): Result<String>
}
