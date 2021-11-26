package com.kshitijpatil.elementaryeditor.ui.edit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.kshitijpatil.elementaryeditor.R
import com.kshitijpatil.elementaryeditor.databinding.DialogExportProgressBinding
import com.kshitijpatil.elementaryeditor.util.viewLifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class ExportProgressDialog(
    private val editRequestId: UUID,
    private val saveWorkerId: UUID
) : DialogFragment(R.layout.dialog_export_progress) {
    private var _binding: DialogExportProgressBinding? = null
    private val binding get() = _binding!!
    private val workManager by lazy { WorkManager.getInstance(requireContext()) }
    private var exportSucceeded = false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DialogExportProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
        updateUiForEditWorkState()
        updateUiForSaveImageWorkState()
        binding.btnCancel.setOnClickListener {
            workManager.cancelWorkById(editRequestId)
            workManager.cancelWorkById(saveWorkerId)
        }
    }

    private fun updateUiForEditWorkState() {
        workManager.getWorkInfoByIdLiveData(editRequestId)
            .observe(viewLifecycleOwner) { workInfo ->
                binding.ivStatus.isVisible = workInfo.state == WorkInfo.State.FAILED
                when (workInfo.state) {
                    WorkInfo.State.ENQUEUED,
                    WorkInfo.State.RUNNING -> {
                        binding.txtTitle.text = getString(R.string.progress_processing_image)
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        // wait save to finish before setting up success UI
                        exportSucceeded = true
                    }
                    WorkInfo.State.FAILED -> {
                        binding.txtTitle.text = getString(R.string.error_editing_failed)
                        loadErrorImageView()
                        delayedDismiss()
                    }
                    WorkInfo.State.CANCELLED -> {
                        binding.txtTitle.text = getString(R.string.info_cancelled)
                        delayedDismiss()
                    }
                    WorkInfo.State.BLOCKED -> {
                        // keep showing progress indicator
                    }
                }
            }
    }

    private fun delayedDismiss() {
        viewLifecycleScope.launch {
            delay(400)
            dismiss()
        }
    }

    private fun updateUiForSaveImageWorkState() {
        workManager.getWorkInfoByIdLiveData(saveWorkerId)
            .observe(viewLifecycleOwner) { workInfo ->
                binding.ivStatus.isVisible = workInfo.state == WorkInfo.State.CANCELLED
                when (workInfo.state) {
                    WorkInfo.State.ENQUEUED,
                    WorkInfo.State.RUNNING -> {
                        binding.txtTitle.text = getString(R.string.progress_saving)
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        if (exportSucceeded) {
                            loadSuccessImageView()
                            binding.txtTitle.text = getString(R.string.info_saved)
                        } else {
                            loadErrorImageView()
                            binding.txtTitle.text = getString(R.string.error_editing_failed)
                        }
                        delayedDismiss()
                    }
                    WorkInfo.State.FAILED -> {
                        binding.txtTitle.text = getString(R.string.error_exporting)
                        loadErrorImageView()
                        delayedDismiss()
                    }
                    WorkInfo.State.CANCELLED -> {
                        binding.txtTitle.text = getString(R.string.info_cancelled)
                        delayedDismiss()
                    }
                    WorkInfo.State.BLOCKED -> {
                        // keep showing progress indicator
                    }
                }
            }
    }

    private fun loadSuccessImageView() {
        Glide.with(requireContext())
            .load(R.drawable.ic_rounded_check)
            .into(binding.ivStatus)
        binding.ivStatus.setColorFilter(
            ContextCompat.getColor(
                requireContext(),
                android.R.color.holo_green_dark
            )
        )
    }

    private fun loadErrorImageView() {
        Glide.with(requireContext())
            .load(R.drawable.ic_filled_rounded_cancel)
            .into(binding.ivStatus)
        binding.ivStatus.setColorFilter(
            ContextCompat.getColor(
                requireContext(),
                android.R.color.holo_red_dark
            )
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}