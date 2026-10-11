package com.justnpcx.mirage.widget

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.TypedArrayUtils
import androidx.preference.EditTextPreference
import com.justnpcx.mirage.R
import com.justnpcx.mirage.ktx.USER_AGENT

class UserAgentPreference
@JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = TypedArrayUtils.getAttr(
        context, R.attr.editTextPreferenceStyle, android.R.attr.editTextPreferenceStyle
    )
) : EditTextPreference(context, attrs, defStyle) {

    public override fun notifyChanged() {
        super.notifyChanged()
    }

    override fun getSummary(): CharSequence? {
        if (text.isNullOrBlank()) {
            return USER_AGENT
        }
        return super.getSummary()
    }

}