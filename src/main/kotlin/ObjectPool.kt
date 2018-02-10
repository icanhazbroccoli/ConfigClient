class ObjectPool<K, V>(source: Map<K, V>) : HashMap<K, V>() {
    init {
        this.putAll(source)
    }

    constructor() : this(mapOf())
}