package nsync

import kotlinx.coroutines.experimental.runBlocking
import nsync.kernel.FolderCatalog
import nsync.kernel.bus.NBus
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class FolderCatalogTest {

    private var conf: Configuration? = null
    private var catalog: FolderCatalog? = null

    @Before fun setUp() {
        val bus = NBus()
        conf = Configuration()
        catalog = FolderCatalog(conf!!, bus)
    }

    @Test fun find_NoFolder() {
        val folder = this.catalog?.find("lala")

        assertThat(folder).isNull()
    }

    @Test fun addFolder_andFind_returnFolder() = runBlocking<Unit> {
        val localUri = "file:///tmp"
        val remoteUri = "file:///tmp/2"
        val uid = catalog?.register(localUri, remoteUri)!!.folderId

        val folder = catalog?.find(uid)
        assertThat(folder?.localFolder).isEqualTo(localUri)
        assertThat(folder?.remoteFolder).isEqualTo(remoteUri)
    }

    @Test(expected = IllegalArgumentException::class)
    fun addTwoFoldersWithSameDestination_throwsException() = runBlocking<Unit> {
        val localUri1 = "file:///tmp"
        val localUri2 = "file:///tmp1"
        val remoteUri = "file:///tmp/2"

        catalog?.register(localUri1, remoteUri)
        catalog?.register(localUri2, remoteUri)
    }

    @Test(expected = IllegalArgumentException::class)
    fun addSameFoldersWithDifferentDestination_throwsException() = runBlocking<Unit> {
        val localUri1 = "file:///tmp"
        val remoteUri1 = "file:///tmp/1"
        val remoteUri2 = "file:///tmp/2"

        val folder1 = catalog?.register(localUri1, remoteUri1)!!
        assertThat(folder1.localFolder).isEqualTo(localUri1)
        assertThat(folder1.remoteFolder).isEqualTo(remoteUri1)


        val folder2 = catalog?.register(localUri1, remoteUri1)!!
        assertThat(folder2.localFolder).isEqualTo(localUri1)
        assertThat(folder2.remoteFolder).isEqualTo(remoteUri2)

    }
}