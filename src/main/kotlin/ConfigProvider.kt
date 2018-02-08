abstract class ConfigProvider <K, V> {
    fun onCreate(callback : (K, V) -> Unit) {

    }

    fun onUpdate(callback : (K, V) -> Unit) {

    }

    fun onDelete(callback : (K, V) -> Unit) {

    }
}