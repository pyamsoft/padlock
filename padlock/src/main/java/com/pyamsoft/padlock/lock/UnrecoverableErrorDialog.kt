package com.pyamsoft.padlock.lock

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.annotation.CheckResult
import androidx.appcompat.app.AlertDialog
import com.pyamsoft.pydroid.ui.app.fragment.ToolbarDialog
import com.pyamsoft.pydroid.ui.app.fragment.requireArguments
import timber.log.Timber

class UnrecoverableErrorDialog : ToolbarDialog() {

  private lateinit var error: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    isCancelable = false
    error = requireNotNull(requireArguments().getString(ERROR))
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return AlertDialog.Builder(requireActivity())
        .setTitle("Lock Screen Error")
        .setMessage(error)
        .setCancelable(false)
        .setPositiveButton("Close PadLock") { _, _ -> dismiss() }
        .create()
  }

  override fun onDismiss(dialog: DialogInterface?) {
    super.onDismiss(dialog)
    Timber.d("Closing PadLock Lock Screen")
    requireActivity().finish()
  }

  companion object {

    private const val ERROR = "error"

    @CheckResult
    @JvmStatic
    fun newInstance(errorMessage: String): UnrecoverableErrorDialog {
      return UnrecoverableErrorDialog().apply {
        arguments = Bundle().apply {
          putString(ERROR, errorMessage)
        }
      }
    }

  }

}