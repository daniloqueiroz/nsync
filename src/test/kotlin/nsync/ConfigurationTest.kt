package nsync;

import org.junit.Test
import org.assertj.core.api.Assertions.*

class ConfigurationTest {

    @Test fun setAndReadSynchronization_defaultBackend() {
        val data = mutableMapOf<String, SyncFolder>("id" to SyncFolder("id", "/home", "/backup"))

        val conf = Configuration()

        conf.synchronization = data

        assertThat(conf.synchronization).isEqualTo(data)
    }
}