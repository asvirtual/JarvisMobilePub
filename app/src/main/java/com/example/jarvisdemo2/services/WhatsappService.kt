package com.example.jarvisdemo2.services

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.example.jarvisdemo2.utilities.Constants


class WhatsappService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (rootInActiveWindow == null) {
            return
        }
        val rootInActiveWindow = AccessibilityNodeInfoCompat.wrap(
            rootInActiveWindow
        )

        // Whatsapp Message EditText id
        val messageNodeList =
            rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.whatsapp:id/entry")
        if (messageNodeList == null || messageNodeList.isEmpty()) {
            return
        }

        // check if the whatsapp message EditText field is filled with text and ending with your suffix (explanation above)
        val messageField = messageNodeList[0]
        if (messageField.text == null || messageField.text.isEmpty() || !messageField.text.toString()
                .endsWith(Constants.WHATSAPP_MESSAGE_SUFFIX)
        ) { // So your service doesn't process any message, but the ones ending your apps suffix
            return
        }

        val arguments = Bundle()
        arguments.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            messageField.text.toString().replace(Constants.WHATSAPP_MESSAGE_SUFFIX, "")
        )
        messageField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

        // Whatsapp send button id
        val sendMessageNodeInfoList =
            rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send")
        if (sendMessageNodeInfoList == null || sendMessageNodeInfoList.isEmpty()) {
            return
        }
        val sendMessageButton = sendMessageNodeInfoList[0]
        if (!sendMessageButton.isVisibleToUser) {
            return
        }

        // Now fire a click on the send button
        sendMessageButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)

        // Now go back to your app by clicking on the Android back button twice:
        // First one to leave the conversation screen
        // Second one to leave whatsapp
        try {
            Thread.sleep(500) // hack for certain devices in which the immediate back click is too fast to handle
            performGlobalAction(GLOBAL_ACTION_BACK)
            Thread.sleep(500) // same hack as above
        } catch (ignored: InterruptedException) {
        }
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    override fun onInterrupt() {

    }
}