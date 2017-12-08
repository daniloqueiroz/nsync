package nsync.metadata

import nsync.asyncTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import commons.AsyncFile
import java.net.URI
import java.nio.file.Paths

class FSTabTest {
    private val filePath = Paths.get("/tmp/$FSTAB_FILENAME")
    private var catalog: Fstab? = null

    @Before fun setUp() {
        filePath.toFile().let {
            if (it.exists()) it.delete()
        }
        catalog = Fstab(AsyncFile(filePath, Fsentry))
    }

    @Test fun find_NoFS() {
        val entry = this.catalog?.findFS("lala")

        assertThat(entry).isNull()
    }

    @Test fun addFS_find_returnFS() = asyncTest {
        val localUri = URI("file:///tmp")
        val remoteUri = URI("file:///tmp/2")
        val addedFS = catalog!!.addFS(localUri, remoteUri)

        assertThat(addedFS.localUri).isEqualTo(localUri)
        assertThat(addedFS.remoteUri).isEqualTo(remoteUri)

        val fs = catalog?.findFS(addedFS.id)
        assertThat(fs?.localUri).isEqualTo(addedFS.localUri)
        assertThat(fs?.remoteUri).isEqualTo(addedFS.remoteUri)
    }

    @Test fun addFS_loadFromDisk_find_returnFS() = asyncTest {
        val localUri1 = URI("file:///tmp/1")
        val remoteUri1 = URI("file:///tmp/2")
        val localUri2 = URI("file:///tmp/3")
        val remoteUri2 = URI("file:///tmp/4")
        catalog!!.addFS(localUri1, remoteUri1)
        val addedFS = catalog!!.addFS(localUri2, remoteUri2)
        catalog = Fstab(AsyncFile(filePath, Fsentry))

        val fs = catalog?.findFS(addedFS.id)
        assertThat(fs?.localUri).isEqualTo(addedFS.localUri)
        assertThat(fs?.remoteUri).isEqualTo(addedFS.remoteUri)
    }

    @Test(expected = IllegalArgumentException::class)
    fun addTwoFS_differentLocal_sameRemote_throwsException() = asyncTest {
        val localUri1 = URI("file:///tmp")
        val localUri2 = URI("file:///tmp1")
        val remoteUri = URI("file:///tmp/2")

        catalog!!.addFS(localUri1, remoteUri)
        catalog!!.addFS(localUri2, remoteUri)
    }

    @Test(expected = IllegalArgumentException::class)
    fun addTwoFS_sameLocal_sameLocal_throwsException() = asyncTest {
        val localUri1 = URI("file:///tmp")
        val remoteUri1 = URI("file:///tmp1")
        val remoteUri2 = URI("file:///tmp/2")

        catalog?.addFS(localUri1, remoteUri1)
        catalog?.addFS(localUri1, remoteUri2)
    }
}