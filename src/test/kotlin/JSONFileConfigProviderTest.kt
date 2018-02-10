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

    val jsonConfigStr2 = """
        [
            {"k": "a", "v": "string:abc"},
            {"k": "c", "v": "string:c"},
            {"k": "d", "v": "string:d"}
        ]
    """

    class TestConfigObject(var v : String) : ConfigObject {
        override fun onUpdate(o: Any?) {
            if (o is TestConfigObject) { v = o.v }
        }
        override fun toString(): String { return "TestConfigObject{v: $v}" }
    }

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

    @Test
    fun doResolveWithWatcher() {
        val file = File.createTempFile("test_json_config_file", "json")
        file.writeText(jsonConfigStr, Charsets.UTF_8)

        val jsonFileConfigProvider = JSONFileConfigProvider(
            file = file,
            syncStrategy = ConfigProviderSyncStrategy.POLL,
            keyExtractor = object : KeyExtractor<String, JSONObject> {
                override fun extractKey(jsonObj: JSONObject): String { return jsonObj.getString("k") }
            },
            valueExtractor = object : ValueExtractor<TestConfigObject, JSONObject> {
                override fun extractValue(jsonObject: JSONObject): TestConfigObject { return TestConfigObject(jsonObject.getString("v")) }
            },
            refreshInterval = 500
        )

        jsonFileConfigProvider.resolve()

        assert(jsonFileConfigProvider.config.containsKey("a"))
        assert(jsonFileConfigProvider.config.containsKey("b"))
        assert(jsonFileConfigProvider.config.containsKey("c"))

        assert(jsonFileConfigProvider.config["a"]?.v.equals("string:a"))
        assert(jsonFileConfigProvider.config["b"]?.v.equals("string:b"))
        assert(jsonFileConfigProvider.config["c"]?.v.equals("string:c"))

        file.writeText(jsonConfigStr2, Charsets.UTF_8)

        Thread.sleep(1_000)

        assert(jsonFileConfigProvider.config.containsKey("a"))
        assert( ! jsonFileConfigProvider.config.containsKey("b"))
        assert(jsonFileConfigProvider.config.containsKey("c"))
        assert(jsonFileConfigProvider.config.containsKey("d"))

        assert(jsonFileConfigProvider.config["a"]?.v.equals("string:abc"))
        assert(jsonFileConfigProvider.config["c"]?.v.equals("string:c"))
        assert(jsonFileConfigProvider.config["d"]?.v.equals("string:d"))

    }
}