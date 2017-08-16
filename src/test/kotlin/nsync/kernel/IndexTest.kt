package nsync.kernel

import kotlinx.coroutines.experimental.runBlocking
import nsync.asyncTest
import nsync.index.AsyncFileChannelIndex
import nsync.index.DataRecord
import nsync.index.SynchronizationStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class AsyncFileChannelIndexTest {
    var baseDir: Path? = null

    @Before
    fun setup() {
//        baseDir = Paths.get("/tmp/testing");
//        Files.createDirectories(baseDir)
        baseDir = Files.createTempDirectory("testing")
    }

    @Test
    fun emptyIndexFindReturnsNull() = asyncTest {
        val index = AsyncFileChannelIndex(baseDir!!, "uid1")
        assertThat(index.get("/abc/xyz")).isNull()
    }

    @Test
    fun recordsDataAndFind_ReturnsIt() = asyncTest {

        val index = AsyncFileChannelIndex(baseDir!!, "uid2")
        val relative1 = "/lala/file1"
        val relative2 = "/lala/file2"
        val data1 = DataRecord(
                byteArrayOf(1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0),
                10L, 10000, SynchronizationStatus.PENDING)
        val data2 = DataRecord(
                byteArrayOf(0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1),
                100L, 100000, SynchronizationStatus.SYNCHRONIZED)

        index.set(relative1, data1)
        index.set(relative2, data2)

        assertThat(index.get(relative2)).isEqualTo(data2)
        assertThat(index.get(relative1)).isEqualTo(data1)
    }

    @Test
    fun recordsDataDeleteAndFind() = asyncTest {

        val index = AsyncFileChannelIndex(baseDir!!, "uid2")
        val relative1 = "/lala/file1"
        val data1 = DataRecord(
                byteArrayOf(1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0),
                10L, 10000, SynchronizationStatus.PENDING)


        index.set(relative1, data1)

        assertThat(index.contains(relative1)).isTrue()

        index.remove(relative1)

        assertThat(index.contains(relative1)).isFalse()
    }
}