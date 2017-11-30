package nsync

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object Configuration {

    val directory: Path = Paths.get(System.getProperty("user.home"), ".config", "nsync")

    init {
        if (!Files.exists(Configuration.directory)) {
            Files.createDirectory(Configuration.directory)
        }
    }
}