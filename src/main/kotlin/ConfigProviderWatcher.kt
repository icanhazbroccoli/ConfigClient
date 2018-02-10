import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.timer

interface ConfigProviderWatcher {
    @Throws
    fun watch() {}

    @Throws
    fun stop() {}
}

class NoneConfigProviderWatcher : ConfigProviderWatcher

open class PollConfigProviderWatcher(val timerPeriod : Long, val callback: TimerTask.() -> Unit) : ConfigProviderWatcher {

    protected val isWatching = AtomicBoolean(false)

    var t : Timer? = null

    override fun watch() {
        if (isWatching.compareAndSet(false, true)) {
            t = timer(daemon = true, period = timerPeriod, action = callback, initialDelay = timerPeriod)
        } else {
            throw ProviderWatcherAlreadyStartedException("The watcher has already been started")
        }
    }

    override fun stop() {
        if (isWatching.get()) {
            t!!.cancel()
        } else {
            throw ProviderWatcherNotRunningException("The watcher is not running")
        }
    }
}