package nsync;

import org.junit.Test
import org.assertj.core.api.Assertions.*

class ConfigurationTest {

    @Test fun setAndReadSynchronization_defaultBackend() {
        val data = mutableMapOf<String, ConfSyncFolder>("id" to ConfSyncFolder("id", "/home", "/backup"))

        val conf = Configuration()

        conf.synchronization = data

        assertThat(conf.synchronization).isEqualTo(data)
    }
}