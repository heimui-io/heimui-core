package io.heimui.core

import io.heimui.core.presentation.state.DriverBackedFormDraftStorage
import io.heimui.core.presentation.state.HeimStateManager
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HeimFormDraftStorageTest {

    @Test
    fun `test save restore and clear form draft`() = runTest {
        val draftStorage = DriverBackedFormDraftStorage()

        // 1. Save draft
        draftStorage.saveDraft("checkout_screen", mapOf("email" to "user@example.com", "name" to "Alice"))

        // 2. Retrieve draft
        val draft = draftStorage.getDraft("checkout_screen")
        assertNotNull(draft)
        assertEquals("user@example.com", draft["email"])
        assertEquals("Alice", draft["name"])

        // 3. Restore into state manager
        val stateManager = HeimStateManager("checkout_screen")
        stateManager.restoreDraft(draft)
        assertEquals("user@example.com", stateManager.getValue("email"))
        assertEquals("Alice", stateManager.getValue("name"))

        // 4. Clear draft
        draftStorage.clearDraft("checkout_screen")
        assertNull(draftStorage.getDraft("checkout_screen"))
    }

    @Test
    fun `draft written against another screen version is discarded rather than restored`() = runTest {
        val storage = DriverBackedFormDraftStorage()

        // A draft saved while the screen was version 1.0.0.
        val saver = HeimStateManager(
            screenId = "kyc",
            draftStorage = storage,
            screenVersion = "1.0.0",
        )
        saver.applyScreenVersion("1.0.0")
        saver.updateValue("doc_type", "passport")
        saver.flushDraft()
        assertNotNull(storage.getDraft("kyc"))

        // The screen has since been republished as 2.0.0, where `doc_type` means something else.
        // Restoring the old value would fill the new field with an answer the user never gave.
        val loader = HeimStateManager(screenId = "kyc", draftStorage = storage)
        loader.applyScreenVersion("2.0.0")

        assertEquals("", loader.getValue("doc_type"))
        assertNull(storage.getDraft("kyc"))
    }

    @Test
    fun `a cached render of an older version does not destroy a draft written for the newer one`() = runTest {
        val storage = DriverBackedFormDraftStorage()

        // The user filled the form while the server was serving 2.0.0.
        val saver = HeimStateManager(screenId = "kyc", draftStorage = storage, screenVersion = "2.0.0")
        saver.applyScreenVersion("2.0.0")
        saver.updateValue("doc_number", "1020304050")
        saver.flushDraft()

        // Next launch, the cache still holds 1.0.0 and renders first. That render is not evidence
        // about the draft: the fresh payload is moments away and may be the version it belongs to.
        val loader = HeimStateManager(screenId = "kyc", draftStorage = storage)
        loader.applyScreenVersion("1.0.0", isStale = true)

        assertEquals("", loader.getValue("doc_number"), "a draft for another version must not be shown")
        assertNotNull(storage.getDraft("kyc"), "and it must not be destroyed on a cached render")

        // Revalidation lands on 2.0.0, which is what the draft was written for.
        loader.applyScreenVersion("2.0.0")
        assertEquals("1020304050", loader.getValue("doc_number"))
    }

    @Test
    fun `a verified render of another version still discards the draft`() = runTest {
        val storage = DriverBackedFormDraftStorage()

        val saver = HeimStateManager(screenId = "kyc", draftStorage = storage, screenVersion = "1.0.0")
        saver.applyScreenVersion("1.0.0")
        saver.updateValue("doc_type", "passport")
        saver.flushDraft()

        // Not stale: the server really is on 2.0.0 now, so the draft cannot be trusted onto it.
        val loader = HeimStateManager(screenId = "kyc", draftStorage = storage)
        loader.applyScreenVersion("2.0.0")

        assertEquals("", loader.getValue("doc_type"))
        assertNull(storage.getDraft("kyc"))
    }

    /** Restoring late must never cost the user what they typed while waiting for it. */
    @Test
    fun `a late restore loses to anything the user typed on the cached render`() = runTest {
        val storage = DriverBackedFormDraftStorage()

        val saver = HeimStateManager(screenId = "kyc", draftStorage = storage, screenVersion = "2.0.0")
        saver.applyScreenVersion("2.0.0")
        saver.updateValues(mapOf("doc_number" to "from_draft", "city" to "bogota"))
        saver.flushDraft()

        val loader = HeimStateManager(screenId = "kyc", draftStorage = storage)
        loader.applyScreenVersion("1.0.0", isStale = true)
        loader.updateValue("doc_number", "typed_while_waiting")

        loader.applyScreenVersion("2.0.0")

        assertEquals("typed_while_waiting", loader.getValue("doc_number"))
        assertEquals("bogota", loader.getValue("city"), "the untouched field still comes back")
    }

    @Test
    fun `a restore made against a stale version is withdrawn when the fresh one arrives`() = runTest {
        val storage = DriverBackedFormDraftStorage()

        val saver = HeimStateManager(screenId = "kyc", draftStorage = storage, screenVersion = "1.0.0")
        saver.applyScreenVersion("1.0.0")
        saver.updateValue("doc_type", "passport")
        saver.flushDraft()

        // Stale-while-revalidate: the cached 1.0.0 renders first, then 2.0.0 arrives.
        val loader = HeimStateManager(screenId = "kyc", draftStorage = storage)
        loader.applyScreenVersion("1.0.0")
        assertEquals("passport", loader.getValue("doc_type"))

        loader.applyScreenVersion("2.0.0")

        // The value came from a version that is no longer on screen, so it must not survive --
        // and it must not be re-stamped as 2.0.0 on the next save, which would launder it. It is
        // emptied rather than removed, so the field on screen actually clears.
        assertEquals("", loader.getValue("doc_type"))
        assertTrue(loader.formState.value.containsKey("doc_type"))
        assertNull(storage.getDraft("kyc"))

        loader.updateValue("doc_type", "typed_now")
        loader.flushDraft()
        assertEquals("2.0.0", storage.getDraft("kyc")?.get("__heim_screen_version"))
    }

    @Test
    fun `draft written against the same screen version is restored`() = runTest {
        val storage = DriverBackedFormDraftStorage()

        val saver = HeimStateManager(screenId = "kyc", draftStorage = storage, screenVersion = "2.0.0")
        saver.applyScreenVersion("2.0.0")
        saver.updateValue("doc_type", "passport")
        saver.flushDraft()

        val loader = HeimStateManager(screenId = "kyc", draftStorage = storage)
        loader.applyScreenVersion("2.0.0")

        assertEquals("passport", loader.getValue("doc_type"))
    }
}
