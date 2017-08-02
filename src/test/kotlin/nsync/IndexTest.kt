package nsync.index

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class AsyncFileChannelIndexTest {
    var baseDir: Path? = null

    @Before fun setup() {
//        baseDir = Paths.get("/tmp/testing");
//        Files.createDirectories(baseDir)
        baseDir = Files.createTempDirectory("testing")
    }

    @Test fun emptyIndexFindReturnsNull() = runBlocking<Unit> {
        val index = AsyncFileChannelIndex(baseDir!!, "uid1")
        assertThat(index["/abc/xyz"].await()).isNull()
    }

    @Test fun recordsDataAndFindReturnsIt() = runBlocking<Unit> {

        val index = AsyncFileChannelIndex(baseDir!!, "uid2")
        val relative1 = "/lala/file1"
        val relative2 = "/lala/file2"
        val data1 = DataRecord(
                byteArrayOf(1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0),
                10L, 10000, SynchronizationStatus.PENDING)
        val data2 = DataRecord(
                byteArrayOf(0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1),
                100L, 100000, SynchronizationStatus.SYNCHRONIZED)

        async(CommonPool) {
            index[relative1] = data1
            index[relative2] = data2

            assertThat(index[relative2]).isEqualTo(data2)
            assertThat(index[relative1]).isEqualTo(data1)
        }
    }
}