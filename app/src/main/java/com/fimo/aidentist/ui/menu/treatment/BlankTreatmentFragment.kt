package com.fimo.aidentist.ui.menu.treatment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.fimo.aidentist.R
import com.fimo.aidentist.data.model.Resource
import com.fimo.aidentist.databinding.FragmentBlankTreatmentBinding
import kotlinx.coroutines.launch

class BlankTreatmentFragment : Fragment() {
    private var _binding: FragmentBlankTreatmentBinding? = null
    private val binding get() = _binding!!
    private val treatmentViewModel: TreatmentViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBlankTreatmentBinding.inflate(inflater, container, false)
        val view = binding.root
        treatmentViewModel.loadUserData()
        observeDiseaseState()
        return view
    }

    private fun observeDiseaseState() {
        viewLifecycleOwner.lifecycleScope.launch {
            treatmentViewModel.diseaseState.collect { state ->
                when (state) {
                    is Resource.Success -> {
                        val data = state.data
                        val disease = data?.get("disease") as? String
                        if (disease != null && disease != "null") {
                            routeTreatment(disease)
                        } else {
                            replaceFragment(ScanTreatmentFragment())
                        }
                    }
                    is Resource.Error -> {
                        Log.d("BlankTreatmentFragment", "Error: ${state.message}")
                        replaceFragment(ScanTreatmentFragment())
                    }
                    is Resource.Loading -> { /* loading */ }
                    null -> { /* idle */ }
                }
            }
        }
    }

    private fun routeTreatment(disease: String) {
        when (disease) {
            "Healthy", "Dental Discoloration", "Periodontal Disease" -> {
                replaceFragment(TreatmentFragment())
            }
            else -> replaceFragment(ScanTreatmentFragment())
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        val nav = parentFragmentManager
        val trans = nav.beginTransaction()
        trans.replace(R.id.containerTreatment, fragment)
        trans.commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
