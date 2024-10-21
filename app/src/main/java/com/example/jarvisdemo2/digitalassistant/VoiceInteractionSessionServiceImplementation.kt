package com.example.jarvisdemo2.digitalassistant

import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService

class VoiceInteractionSessionServiceImplementation: VoiceInteractionSessionService() {
    override fun onNewSession(bundle: Bundle?): VoiceInteractionSession? {
        return VoiceInteractionSession(this)
    }
}