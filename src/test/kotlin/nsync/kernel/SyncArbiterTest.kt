package nsync.kernel

import kotlinx.coroutines.experimental.runBlocking
import nsync.SimpleBus
import nsync.asyncTest
import nsync.kernel.bus.DeleteFile
import nsync.kernel.bus.FileDeleted
import nsync.kernel.synchronization.SyncArbiter
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.util.Files
import org.junit.Before
import org.junit.Test
import java.nio.file.Paths

class SyncArbiterTest {

    private val dataFolder = Files.newTemporaryFolder().toPath()
    private var arbiter: SyncArbiter? = null
    private var bus: SimpleBus? = null

    private val id = "uid"
    private val folder = SyncFolder(id, "file:///tmp/a", "file:///tmp/b")

    @Before
    fun setup() = runBlocking<Unit>{
        bus = SimpleBus()
        arbiter = SyncArbiter(bus!!)

        bus!!.signals.forEach {
            arbiter?.handle(it)
        }
        bus!!.reset()
    }

    @Test
    fun fileDeletedSignal_firesDeleteFileSignal() = asyncTest {
        val path = Paths.get("/tmp", "/a/file")

        arbiter?.handle(FileDeleted(LocalFile(id, path)))

        val published = bus?.signals?.first() as DeleteFile
        assertThat(published.payload.folder).isEqualTo(folder)
        assertThat(published.payload.localFilePath).isEqualTo(path)
    }

    @Test
    fun fileDeletedSignal_deletesFromIndex() = asyncTest {
        val path = Paths.get("/tmp", "/a/file")
        val file = LocalFile(id, path)

        arbiter?.handle(FileDeleted(file))

        assertThat(bus?.signals?.size).isEqualTo(1)
    }

}