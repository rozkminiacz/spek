package org.spekframework.intellij

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.impl.compiled.ClsArrayInitializerMemberValueImpl
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

enum class PsiSynonymType {
    GROUP,
    ACTION,
    TEST
}

data class PsiSynonym(val annotation: PsiAnnotation) {
    val type: PsiSynonymType by lazy {
        val type = annotation.findAttributeValue("type")!!.text

        when {
            type.endsWith("SynonymType.GROUP") -> PsiSynonymType.GROUP
            type.endsWith("SynonymType.ACTION") -> PsiSynonymType.ACTION
            type.endsWith("SynonymType.TEST") -> PsiSynonymType.TEST
            else -> throw IllegalArgumentException("Unsupported synonym: $type.")
        }
    }

    val prefix: String by lazy {
        annotation.findAttributeValue("prefix")!!.text.removeSurrounding("\"")
    }

    val excluded: Boolean by lazy {
        annotation.findAttributeValue("excluded")!!.text.toBoolean()
    }
}

enum class PsiDescriptionLocation {
    TYPE_PARAMETER,
    VALUE_PARAMETER
}

class PsiDescription(val annotation: PsiAnnotation) {
    val index: Int by lazy {
        annotation.findAttributeValue("index")!!.text.toInt()
    }

    val location: PsiDescriptionLocation by lazy {
        val text = annotation.findAttributeValue("location")!!.text
        when {
            text.endsWith("DescriptionLocation.TYPE_PARAMETER") -> PsiDescriptionLocation.TYPE_PARAMETER
            text.endsWith("DescriptionLocation.VALUE_PARAMETER") -> PsiDescriptionLocation.VALUE_PARAMETER
            else -> throw IllegalArgumentException("Unknown location type: $text")
        }
    }
}

class PsiDescriptions(val annotation: PsiAnnotation) {
    val sources: Array<PsiDescription> by lazy {
        val value = annotation.findAttributeValue("sources")!! as ClsArrayInitializerMemberValueImpl
        val sources = value.initializers.map { it as PsiAnnotation }
            .map(::PsiDescription)
        sources.toTypedArray()
    }
}

class SynonymContext(val synonym: PsiSynonym, val descriptions: PsiDescriptions) {
    fun isExcluded(): Boolean = synonym.excluded

    fun constructDescription(callExpression: KtCallExpression): String {
        return descriptions.sources.map {
            when (it.location) {
                PsiDescriptionLocation.TYPE_PARAMETER -> throw UnsupportedFeatureException("Type parameter description is currently unsupported.")
                PsiDescriptionLocation.VALUE_PARAMETER -> {
                    val argument = callExpression.valueArguments.getOrNull(it.index)
                    val expression = argument?.getArgumentExpression()

                    when (expression) {
                        is KtStringTemplateExpression -> {
                            if (!expression.hasInterpolation()) {
                                // might be empty at some point, especially when user is still typing
                                expression.entries.firstOrNull()?.text ?: ""
                            } else {
                                throw UnsupportedFeatureException("Descriptions with interpolation are currently unsupported.")
                            }
                        }
                        // can happen when user is still typing
                        else -> throw UnsupportedFeatureException("Value argument description should be a string.")
                    }
                }
                else -> throw IllegalArgumentException("Invalid location: ${it.location}")
            }
        }.fold(synonym.prefix) { prev, current ->
            "$prev $current"
        }
    }
}
