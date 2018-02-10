abstract class ConfigProvider <K, V : ConfigObject> {

    val SHOULD_DELETE_KEY = 0
    val SHOULD_UPDATE_KEY = 1
    val SHOULD_CREATE_KEY = 2

    enum class State { READY, RESOLVING, RESOLVED, FAILURE }

    protected var state : State = State.READY

    protected val configObjectPool: ConfigObjectPool<K, V> = ConfigObjectPool()

    protected abstract fun doResolve()

    val config : ConfigObjectPool<K, V>
        get() = configObjectPool

    @Throws
    @Synchronized
    open fun resolve() {
        when (state) {
            State.RESOLVING -> throw ResolvingInProgressConfigProviderException("The config provider is resolving")
            State.FAILURE -> throw ResolvedFailureConfigProviderException("The config provider is in non-recoverable failure state")
            else -> null
        }

        state = State.RESOLVING

        try {
            doResolve()
            state = State.RESOLVED
        } catch (e : Exception) {
            state = State.FAILURE
        }
    }

    @Synchronized
    protected open fun processUpdate(newPoolConfig: ConfigObjectPool<K, V>) {
        val objectPoolKeys = configObjectPool.keys
        val newPoolKeys = newPoolConfig.keys

        val addUpdDel = objectPoolKeys
            .union(newPoolKeys)
            .groupBy({
                val existsInOld = objectPoolKeys.contains(it)
                val existsInNew = newPoolKeys.contains(it)
                if (existsInOld && existsInNew) SHOULD_UPDATE_KEY
                else if (existsInNew) SHOULD_CREATE_KEY
                else SHOULD_DELETE_KEY
            })
        val keysToBeUpdated = addUpdDel[SHOULD_UPDATE_KEY]
        val keysToBeDeleted = addUpdDel[SHOULD_DELETE_KEY]
        val keysToBeCreated = addUpdDel[SHOULD_CREATE_KEY]

        keysToBeDeleted?.forEach { configObjectPool[it]!!.onDelete(); configObjectPool.remove(it) }
        keysToBeUpdated?.forEach { configObjectPool.get(it)?.onUpdate(newPoolConfig[it]) }
        keysToBeCreated?.forEach { configObjectPool[it] = newPoolConfig[it]!! }
    }
}