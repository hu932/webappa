package com.xpj.app

import android.content.Context

object Prefs {
    private const val NAME = "xpj_prefs"
    private const val KEY_DISPATCH = "dispatch_id"

    fun getDispatch(ctx: Context): String =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString(KEY_DISPATCH, "") ?: ""

    fun saveDispatch(ctx: Context, id: String) {
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putString(KEY_DISPATCH, id).apply()
    }
}
