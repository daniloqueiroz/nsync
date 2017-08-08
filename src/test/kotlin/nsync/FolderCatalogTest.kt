package nsync

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class FolderCatalogTestTest {

    private var conf: Configuration? = null
    private var catalog: FolderCatalog? = null

    @Before fun setUp() {
        conf = Configuration()
        catalog = FolderCatalog(conf!!)
    }

    @Test fun find_NoFolder() {
        val folder = this.catalog?.find("lala")

        assertThat(folder).isNull()
    }

    @Test fun addFolder_andFind_returnFolder() {
        val localUri = "file:///tmp"
        val remoteUri = "file:///tmp/2"
        val uid = this.catalog?.register(localUri, remoteUri)!!.folderId

        val folder = this.catalog?.find(uid)
        assertThat(folder?.localFolder).isEqualTo(localUri)
        assertThat(folder?.remoteFolder).isEqualTo(remoteUri)
    }

    @Test(expected = IllegalArgumentException::class)
    fun addTwoFoldersWithSameDestination_throwsException() {
        val localUri1 = "file:///tmp"
        val localUri2 = "file:///tmp1"
        val remoteUri = "file:///tmp/2"

        this.catalog?.register(localUri1, remoteUri)
        this.catalog?.register(localUri2, remoteUri)
    }

    @Test(expected = IllegalArgumentException::class)
    fun addSameFoldersWithDifferentDestination_throwsException() {
        val localUri1 = "file:///tmp"
        val remoteUri1 = "file:///tmp/1"
        val remoteUri2 = "file:///tmp/2"

        val folder1 = this.catalog?.register(localUri1, remoteUri1)!!
        assertThat(folder1.localFolder).isEqualTo(localUri1)
        assertThat(folder1.remoteFolder).isEqualTo(remoteUri1)


        val folder2 = this.catalog?.register(localUri1, remoteUri1)!!
        assertThat(folder2.localFolder).isEqualTo(localUri1)
        assertThat(folder2.remoteFolder).isEqualTo(remoteUri2)

    }
}