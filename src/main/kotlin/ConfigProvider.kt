abstract class ConfigProvider <K, V> {

    val SHOULD_DELETE_KEY = 0
    val SHOULD_UPDATE_KEY = 1
    val SHOULD_CREATE_KEY = 2

    enum class State { READY, RESOLVING, RESOLVED, FAILURE }

    protected var state : State = State.READY

    protected val objectPool : ObjectPool<K, V> = ObjectPool()

    protected abstract fun doResolve()

    @Throws
    @Synchronized
    fun resolve() {
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

    protected fun processUpdate(newPool : ObjectPool<K, V>) {
        val objectPoolKeys = objectPool.keys
        val newPoolKeys = newPool.keys

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
    }
}

class ResolvingInProgressConfigProviderException(message : String) : Exception(message)
class ResolvedFailureConfigProviderException(message : String) : Exception(message)