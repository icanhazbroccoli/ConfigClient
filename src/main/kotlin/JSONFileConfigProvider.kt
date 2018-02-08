import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

class JSONFileConfigProvider <K, V> (
    val file : File,
    val syncStrategy: ConfigProviderSyncStrategy = ConfigProviderSyncStrategy.NONE,
    val keyExtractor: KeyExtractor<K, JSONObject>,
    val valueExtractor: ValueExtractor<V, JSONObject>
) : ConfigProvider<K, V>() {

    lateinit private var watcher : ConfigProviderWatcher

    val DEFAULT_CHARSET : Charset = Charsets.UTF_8

    enum class State { READY, RESOLVING, RESOLVED, FAILURE }
    var state : State = State.READY

    init {
        watcher = when (syncStrategy) {
            ConfigProviderSyncStrategy.NONE -> NoneConfigProviderWatcher()
            ConfigProviderSyncStrategy.POLL -> PollConfigProviderWatcher(1_000, { TODO() })
            else -> NoneConfigProviderWatcher()
        }
    }

    @Throws
    @Synchronized
    fun resolve() {

        when (state) {
            State.RESOLVING -> throw ResolvingInProgressConfigProviderException("The config provider is resolving")
            State.FAILURE -> throw ResolvedFailureConfigProviderException("The config provider is in non-recoverable failure state")
            else -> null
        }

        try {
            if (!file.exists()) {
                throw JSONFileDoesNotExistException("File $file could not be found")
            }
            val jsonStr = file.readText(DEFAULT_CHARSET)
            val jsonArray = JSONArray(jsonStr)
            val newPool = jsonArray.mapIndexed { index, _ -> jsonArray.getJSONObject(index) }
                .associateBy({ keyExtractor.extractKey(it) }, { valueExtractor.extractValue(it) })
        } catch (e : Exception) {
            state = State.FAILURE
        }
    }
}

class ResolvingInProgressConfigProviderException(message : String) : Exception(message)
class ResolvedFailureConfigProviderException(message : String) : Exception(message)