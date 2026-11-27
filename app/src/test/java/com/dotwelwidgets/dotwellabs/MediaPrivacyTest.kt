package com.dotwelwidgets.dotwellabs

import com.dotwelwidgets.dotwellabs.media.MediaActionReceiver
import com.dotwelwidgets.dotwellabs.media.MediaPrivacy
import com.dotwelwidgets.dotwellabs.media.MediaTransportAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaPrivacyTest {
    @Test
    fun masksVisibleCharactersAndKeepsWhitespace() {
        assertEquals("\u2022\u2022\u2022 \u2022\u2022", MediaPrivacy.mask("Bob hi"))
    }

    @Test
    fun onlyKnownTransportActionsAreAccepted() {
        assertEquals(
            MediaTransportAction.PLAY_PAUSE,
            MediaActionReceiver.parseAction("PLAY_PAUSE"),
        )
        assertNull(MediaActionReceiver.parseAction("DELETE_ALL"))
        assertNull(MediaActionReceiver.parseAction(null))
    }
}
