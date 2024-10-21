package com.example.jarvisdemo2.utilities

import android.content.Context
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

object Translator {

    private var italianEnglishTranslator: Translator
    private var englishItalianTranslator: Translator

    init {
        val itEnOptions = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ITALIAN)
            .setTargetLanguage(TranslateLanguage.ENGLISH)
            .build()
        italianEnglishTranslator = Translation.getClient(itEnOptions)

        val enItOptions = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.ITALIAN)
            .build()
        englishItalianTranslator = Translation.getClient(enItOptions)
    }

    fun translate(src: String = "it", to: String = "en", text: String, callback: TranslationCallback, context: Context) {

        val itEnOptions = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ITALIAN)
            .setTargetLanguage(TranslateLanguage.ENGLISH)
            .build()
        italianEnglishTranslator = Translation.getClient(itEnOptions)

        val enItOptions = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.ITALIAN)
            .build()
        englishItalianTranslator = Translation.getClient(enItOptions)

        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        italianEnglishTranslator.downloadModelIfNeeded(conditions)
            /*.addOnSuccessListener {
                Constants.makeToast(context, "Model Downloaded!")
            }*/
            .addOnFailureListener { exception ->
                Constants.makeToast(context, exception.message.toString())
            }

        englishItalianTranslator.downloadModelIfNeeded(conditions)
            /*.addOnSuccessListener {
                Constants.makeToast(context, "Model Downloaded!")
            }*/
            .addOnFailureListener { exception ->
                Constants.makeToast(context, exception.message.toString())
            }

        if (src == "it" && to == "en") {
            italianEnglishTranslator.translate(text)
                .addOnSuccessListener { translatedText ->
                    callback.onSuccess(translatedText)
                }
                .addOnFailureListener { exception ->
                    callback.onError(exception.message.toString())
                }
        } else if (src == "en" && to == "it") {
            englishItalianTranslator.translate(text)
                .addOnSuccessListener { translatedText ->
                    callback.onSuccess(translatedText)
                }
                .addOnFailureListener { exception ->
                    callback.onError(exception.message.toString())
                }
        }
    }

    interface TranslationCallback {
        fun onSuccess(text: String)
        fun onError(error: String)
    }
}