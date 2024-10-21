package com.example.jarvisdemo2.digitalassistant

import android.content.Context
import android.os.Bundle
import android.service.voice.VoiceInteractionSession


class VoiceInteractionSessionImplementation internal constructor(context: Context?): VoiceInteractionSession(context) {
    override fun onShow(args: Bundle, showFlags: Int) {
        super.onShow(args, showFlags)
        // whatever you want to do when you hold the home button
        // i am using it to show volume control slider
        hide()
    }
}