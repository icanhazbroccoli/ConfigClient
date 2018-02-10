import org.junit.Test

import org.junit.Assert.*

class ConfigProviderTest {

    class TestConfigObject(var v : String) : ConfigObject {
        override fun onUpdate(o: Any?) {
            if (o is TestConfigObject) {
                v = o.v
            }
        }
        override fun onDelete() {}
        override fun toString(): String {
            return "TestConfigObject{v: $v}"
        }
    }

    @Test
    fun resolve() {

        var methodCalled : Boolean = false

        class TestConfigProvider : ConfigProvider<String, TestConfigObject>() {
            override fun doResolve() {
                assert(this.state == State.RESOLVING) { "doResolve() is called while the object is in RESOLVING state" }
                methodCalled = true
            }
        }

        val testConfigProvider = TestConfigProvider()
        testConfigProvider.resolve()

        assert(methodCalled) { "doResolve() method has been called" }
    }

    @Test
    fun processUpdate() {

        class TestConfigProvider : ConfigProvider<String, TestConfigObject>() {
            override fun doResolve() {
                processUpdate(ConfigObjectPool(mapOf(
                    Pair("a", TestConfigObject("a")),
                    Pair("b", TestConfigObject("b")),
                    Pair("c", TestConfigObject("c"))
                )))
            }

            fun testCreate() {
                processUpdate(ConfigObjectPool(mapOf(
                    Pair("a", TestConfigObject("a")),
                    Pair("b", TestConfigObject("b")),
                    Pair("c", TestConfigObject("c")),
                    Pair("d", TestConfigObject("d"))
                )))
            }

            fun testUpdate() {
                processUpdate(ConfigObjectPool(mapOf(
                    Pair("a", TestConfigObject("abc")),
                    Pair("b", TestConfigObject("b")),
                    Pair("c", TestConfigObject("c")),
                    Pair("d", TestConfigObject("d"))
                )))
            }

            fun testDelete() {
                processUpdate(ConfigObjectPool(mapOf(
                    Pair("a", TestConfigObject("abc")),
                    Pair("d", TestConfigObject("d"))
                )))
            }
        }

        val testConfigProvider = TestConfigProvider()
        testConfigProvider.resolve()

        assert(testConfigProvider.config.containsKey("a"))
        assert(testConfigProvider.config.containsKey("b"))
        assert(testConfigProvider.config.containsKey("c"))

        assert(testConfigProvider.config["a"]?.v.equals("a"))
        assert(testConfigProvider.config["b"]?.v.equals("b"))
        assert(testConfigProvider.config["c"]?.v.equals("c"))

        // CREATE

        testConfigProvider.testCreate()

        assert(testConfigProvider.config.containsKey("a"))
        assert(testConfigProvider.config.containsKey("b"))
        assert(testConfigProvider.config.containsKey("c"))
        assert(testConfigProvider.config.containsKey("d"))

        assert(testConfigProvider.config["a"]?.v.equals("a"))
        assert(testConfigProvider.config["b"]?.v.equals("b"))
        assert(testConfigProvider.config["c"]?.v.equals("c"))
        assert(testConfigProvider.config["d"]?.v.equals("d"))

        // UPDATE

        testConfigProvider.testUpdate()

        assert(testConfigProvider.config.containsKey("a"))
        assert(testConfigProvider.config.containsKey("b"))
        assert(testConfigProvider.config.containsKey("c"))
        assert(testConfigProvider.config.containsKey("d"))

        assert(testConfigProvider.config["a"]?.v.equals("abc"))
        assert(testConfigProvider.config["b"]?.v.equals("b"))
        assert(testConfigProvider.config["c"]?.v.equals("c"))
        assert(testConfigProvider.config["d"]?.v.equals("d"))

        // DELETE

        testConfigProvider.testDelete()

        assert(testConfigProvider.config.containsKey("a"))
        assert( ! testConfigProvider.config.containsKey("b"))
        assert( ! testConfigProvider.config.containsKey("c"))
        assert(testConfigProvider.config.containsKey("d"))

        assert(testConfigProvider.config["a"]?.v.equals("abc"))
        assert(testConfigProvider.config["d"]?.v.equals("d"))

    }
}