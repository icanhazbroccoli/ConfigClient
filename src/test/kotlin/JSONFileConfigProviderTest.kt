import org.json.JSONObject
import org.junit.Test

import org.junit.Assert.*
import java.io.File
import java.nio.charset.Charset

class JSONFileConfigProviderTest {

    val jsonConfigStr = """
        [
            {"k": "a", "v": "string:a"},
            {"k": "b", "v": "string:b"},
            {"k": "c", "v": "string:c"}
        ]
    """

    class TestConfigObject(val v : String) : ConfigObject

    @Test
    fun doResolve() {
        val file = File.createTempFile("test_json_config_file", "json")
        file.writeText(jsonConfigStr, Charsets.UTF_8)

        val jsonFileConfigProvider = JSONFileConfigProvider(
            file = file,
            syncStrategy = ConfigProviderSyncStrategy.NONE,
            keyExtractor = object : KeyExtractor<String, JSONObject> {
                override fun extractKey(jsonObj: JSONObject): String { return jsonObj.getString("k") }
            },
            valueExtractor = object : ValueExtractor<TestConfigObject, JSONObject> {
                override fun extractValue(jsonObject: JSONObject): TestConfigObject { return TestConfigObject(jsonObject.getString("v")) }
            }
        )

        jsonFileConfigProvider.resolve()

        assert(jsonFileConfigProvider.config.containsKey("a"))
        assert(jsonFileConfigProvider.config.containsKey("b"))
        assert(jsonFileConfigProvider.config.containsKey("c"))

        assert(jsonFileConfigProvider.config["a"]?.v.equals("string:a"))
        assert(jsonFileConfigProvider.config["b"]?.v.equals("string:b"))
        assert(jsonFileConfigProvider.config["c"]?.v.equals("string:c"))

        file.delete()
    }
}