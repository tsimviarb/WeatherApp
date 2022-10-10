package com.tim.weatherapp

import android.app.AlertDialog
import android.content.Context
import android.widget.Button

object DialogManager {
    fun locationSettingDialog(context: Context, listener: Listener){

        val dialog = AlertDialog.Builder(context).create()

        dialog.setMessage("You should activate location or type position\nDo you want to set location in settings?")

        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "YES"){ _,_ ->

            listener.onClick()
        }
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "CANCEL"){ dialogInterface,_ ->
        }
        dialog.show()
    }

    interface Listener{
        fun onClick()
    }
}