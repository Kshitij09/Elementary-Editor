package com.kshitijpatil.elementaryeditor.ui.edit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kshitijpatil.elementaryeditor.databinding.BottomSheetSaveChangesAlertBinding

class SaveChangesAlertBottomSheet(var onActionSelectedListener: OnActionSelectedListener? = null) :
    BottomSheetDialogFragment() {
    private var _binding: BottomSheetSaveChangesAlertBinding? = null
    private val binding: BottomSheetSaveChangesAlertBinding get() = _binding!!

    fun interface OnActionSelectedListener {
        fun onActionSelected(changeAction: ChangeAction)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = BottomSheetSaveChangesAlertBinding.inflate(inflater, container, false)
        binding.btnCancel.setOnClickListener { setActionAndDismiss(ChangeAction.CANCEL) }
        binding.btnDiscard.setOnClickListener { setActionAndDismiss(ChangeAction.DISCARD) }
        binding.btnSave.setOnClickListener { setActionAndDismiss(ChangeAction.SAVE) }
        return binding.root
    }

    private fun setActionAndDismiss(changeAction: ChangeAction) {
        onActionSelectedListener?.onActionSelected(changeAction)
        dismiss()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}