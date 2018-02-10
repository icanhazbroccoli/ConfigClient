import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.Charset

class JSONFileConfigProvider <K, V : ConfigObject> (
    val file : File,
    val syncStrategy: ConfigProviderSyncStrategy = ConfigProviderSyncStrategy.NONE,
    val keyExtractor: KeyExtractor<K, JSONObject>,
    val valueExtractor: ValueExtractor<V, JSONObject>
) : ConfigProvider<K, V>(syncStrategy) {

    val DEFAULT_CHARSET : Charset = Charsets.UTF_8

    override fun doResolve() {
        if (!file.exists()) {
            throw JSONFileDoesNotExistException("File $file could not be found")
        }
        val jsonStr = file.readText(DEFAULT_CHARSET)
        val jsonArray = JSONArray(jsonStr)
        val newPoolMap = jsonArray.mapIndexed { index, _ -> jsonArray.getJSONObject(index) }
            .associateBy({ keyExtractor.extractKey(it) }, { valueExtractor.extractValue(it) })

        processUpdate(ConfigObjectPool(newPoolMap))
    }
}

