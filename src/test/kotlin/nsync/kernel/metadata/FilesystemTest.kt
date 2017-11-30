package nsync.kernel.metadata

import nsync.asyncTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class FilesystemTest {
    var baseDir: Path? = null

    @Before
    fun setup() {
//        baseDir = Paths.get("/tmp/testing");
//        Files.createDirectories(baseDir)
        baseDir = Files.createTempDirectory("testing")
    }

    @Test
    fun emptyIndexFindReturnsNull() = asyncTest {
        val index = Filesystem(baseDir!!, "uid1")
        assertThat(index.get("/abc/xyz")).isNull()
    }

    @Test
    fun recordsDataAndFind_ReturnsIt() = asyncTest {

        val index = Filesystem(baseDir!!, "uid2")
        val relative1 = "/lala/file1"
        val id1 = "id1"
        val relative2 = "/lala/file2"
        val id2 = "id2"
        val data1 = Inode(
                byteArrayOf(1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0),
                10L, 10000, Status.PENDING
        )
        val data2 = Inode(
                byteArrayOf(0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1),
                100L, 100000, Status.SYNCHRONIZED
        )

        index.set(id1, relative1, data1)
        index.set(id2, relative2, data2)

        assertThat(index.get(id2)).isEqualTo(data2)
        assertThat(index.get(index.findByPath(relative1)!!)).isEqualTo(data1)
    }

    @Test
    fun recordsDataDeleteAndFind() = asyncTest {

        val index = Filesystem(baseDir!!, "uid2")
        val relative1 = "/lala/file1"
        val id1 = "id1"
        val data1 = Inode(
                byteArrayOf(1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0),
                10L, 10000, Status.PENDING)


        index.set(id1, relative1, data1)

        assertThat(index.get(id1)).isNotNull()

        index.remove(id1)

        assertThat(index.get(id1)).isNull()
        assertThat(index.findByPath(relative1)).isNull()
    }
}