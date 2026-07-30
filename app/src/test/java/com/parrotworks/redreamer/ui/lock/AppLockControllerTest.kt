package com.parrotworks.redreamer.ui.lock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockControllerTest {

    private val controller = AppLockController()

    @Test
    fun `starts locked`() {
        assertFalse(controller.unlocked.value)
    }

    @Test
    fun `unlock and relock flip the flag`() {
        controller.unlock()
        assertTrue(controller.unlocked.value)
        controller.relock()
        assertFalse(controller.unlocked.value)
    }

    @Test
    fun `no system flow is active by default`() {
        assertFalse(controller.isInSystemFlow)
    }

    @Test
    fun `an open system flow suppresses relocking`() {
        controller.beginSystemFlow()
        assertTrue(controller.isInSystemFlow)
        controller.endSystemFlow()
        assertFalse(controller.isInSystemFlow)
    }

    @Test
    fun `nested flows only clear once all have ended`() {
        controller.beginSystemFlow()
        controller.beginSystemFlow()
        controller.endSystemFlow()

        assertTrue("one flow is still open", controller.isInSystemFlow)

        controller.endSystemFlow()
        assertFalse(controller.isInSystemFlow)
    }

    @Test
    fun `ending more flows than were started cannot drive the count negative`() {
        controller.endSystemFlow()
        controller.endSystemFlow()
        controller.beginSystemFlow()

        // A negative count would swallow this begin and leave the app unlockable-but-unlocked.
        assertTrue(controller.isInSystemFlow)
    }

    @Test
    fun `returning to the foreground clears any leaked flow`() {
        controller.beginSystemFlow()
        controller.beginSystemFlow()

        controller.onReturnedToForeground()

        assertFalse("a missed end must never disable the lock permanently", controller.isInSystemFlow)
    }

    @Test
    fun `returning to the foreground does not by itself unlock`() {
        controller.onReturnedToForeground()
        assertFalse(controller.unlocked.value)
    }
}
