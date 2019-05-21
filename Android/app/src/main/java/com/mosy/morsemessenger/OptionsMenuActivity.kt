package com.mosy.morsemessenger

import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem



import kotlinx.android.synthetic.main.dialog_rules.view.*

open class OptionsMenuActivity : AppCompatActivity() {

    //Options-Menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        val inflater = menuInflater
        inflater.inflate(R.menu.options_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.rules -> {
                //openDialog
                showRulesDialog()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    fun showRulesDialog () {
        //Inflate the dialog with custom view
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rules, null)
        //AlertDialogBuilder
        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
        //show dialog
        val alertDialog = dialogBuilder.show()
        //login button click of custom layout
        dialogView.dialog_OKbtn.setOnClickListener {
            //dismiss dialog
            alertDialog.dismiss()
        }
    }

}
