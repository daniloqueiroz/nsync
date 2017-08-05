package nsync

import org.mapdb.DB
import org.mapdb.DBMaker
import org.mapdb.HTreeMap
import org.mapdb.Serializer
import java.io.File
import java.io.Serializable
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.reflect.KProperty

open class ConfigurationStorage {
    open val data: MutableMap<String, Any> = mutableMapOf()

    operator fun <R> getValue(thisRef: Configuration, property: KProperty<*>): R {
        return this.data.get(property.name) as R
    }

    operator fun <R> setValue(thisRef: Configuration, property: KProperty<*>, value: R) {
        this.data.put(property.name, value as Any)
    }
}

class BinaryFileConfigurationStorage(val file: File) : ConfigurationStorage() {
    val db: DB = DBMaker.fileDB(file).closeOnJvmShutdown().fileMmapEnableIfSupported().make()
    override val data: HTreeMap<String, Any> = db.hashMap("config", Serializer.STRING, Serializer.JAVA).createOrOpen()
}


class Configuration(backend: ConfigurationStorage = ConfigurationStorage()) {
    companion object Constants {
        // TODO on windows use different path
        val directory = Paths.get(System.getProperty("user.home"), ".config", "nsync")

        init {
            if (!Files.exists(Configuration.directory)) {
                Files.createDirectory(Configuration.directory)
            }
        }
    }

    var synchronization: MutableMap<String, ConfSyncFolder>? by backend
}

data class ConfSyncFolder(
        val uid: String,
        val localFolder: String,
        val remoteFolder: String): Serializable