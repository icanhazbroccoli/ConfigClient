import kotlinx.coroutines.experimental.async
import java.io.File
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.util.*
import kotlin.concurrent.timer

interface ConfigProviderWatcher {

    fun watch() {}

    fun stop() {}

    fun onFireUp(callback: () -> Unit) {}
}

class ConfigProviderWatcherFactory {
    companion object {
        @JvmStatic
        fun newWatcher(watcherStrategy : ConfigProviderWatcherStrategy, extra : Map<String, Any?>? = null) : ConfigProviderWatcher {
            return when (watcherStrategy) {
                ConfigProviderWatcherStrategy.NONE -> NoneConfigProviderWatcher()
                ConfigProviderWatcherStrategy.POLL -> PollConfigProviderWatcher(extra)
                ConfigProviderWatcherStrategy.FS_EVENT -> FSEventConfigProviderWatcher(extra)
                else -> NoneConfigProviderWatcher()
            }
        }
    }
}

class NoneConfigProviderWatcher : ConfigProviderWatcher

open class PollConfigProviderWatcher(extra : Map<String, Any?>?) : ConfigProviderWatcher {

    val timerPeriod : Long = (extra?.get("refresh_interval") ?: 1_000) as Long

    protected var isWatching : Boolean = false

    protected var callback: (TimerTask.() -> Unit)? = null

    protected var t : Timer? = null

    @Synchronized
    override fun onFireUp(callback: () -> Unit) {
        this.callback = { callback() }
    }

    @Synchronized
    @Throws
    override fun watch() {
        if ( ! isWatching) {

            if (callback == null) {
                throw ConfigWatcherIncompleteInitException("The watcher is missing a callback handler")
            }

            t = timer(daemon = true, period = timerPeriod, action = callback!!, initialDelay = timerPeriod)
            isWatching = true
        } else {
            throw ConfigWatcherAlreadyStartedException("The poll watcher has already been started")
        }
    }

    @Synchronized
    @Throws
    override fun stop() {
        if (isWatching) {
            t!!.cancel()
            isWatching = false
        } else {
            throw ConfigWatcherNotRunningException("The poll watcher is not running")
        }
    }
}

open class FSEventConfigProviderWatcher(extra : Map<String, Any?>?) : ConfigProviderWatcher {

    protected var watchService : WatchService? = null
    protected var path : Path? = null
    protected var shouldStop : Boolean = false

    private val file : File = extra!!.get("file") as File

    private var callback : (() -> Unit)? = null

    override fun onFireUp(callback: () -> Unit) {
        this.callback = callback
    }

    @Synchronized
    @Throws
    override fun watch() {

        if (watchService != null) {
            throw ConfigWatcherAlreadyStartedException("The FS event watcher has already been started")
        }

        if (callback == null) {
            throw ConfigWatcherIncompleteInitException("The watcher is missing a callback handler")
        }

        path = if ( ! file.isDirectory) {
            file.parentFile.toPath()
        } else file.toPath()

        println("Path: $path")

        watchService = path!!.fileSystem.newWatchService()

        path!!.register(watchService, ENTRY_MODIFY)

        async {
            println("Starting an async watcher")

            var watchKey: WatchKey? = null

            while (!shouldStop) {

                println("Watchin'")

                watchKey = watchService!!.take()
                println("Watch key: $watchKey")

                for (watchEvent in watchKey.pollEvents()) {
                    println("FS event happened: ${watchEvent.context()}, kind: ${watchEvent.kind()}")
                    println("Another one line of logs")
                    val modifiedFileName = watchEvent.context().toString()
                    println("Modified file name: $modifiedFileName, original one: ${file.name}")
                    if (watchEvent.context().toString().equals(file.name)) {
                        println("Source file contents changed, running the callback")
                        callback!!()
                    }
                }

                if (!watchKey.reset()) {
                    break
                }
            }
        }
    }

    @Synchronized
    @Throws
    override fun stop() {
        if (watchService != null) {
            shouldStop = true
        } else {
            throw ConfigWatcherNotRunningException("The FS event watcher is not running")
        }
    }
}