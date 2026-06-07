package com.fimo.aidentist.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.fragment.app.viewModels
import com.fimo.aidentist.R
import com.fimo.aidentist.data.model.Resource
import com.fimo.aidentist.databinding.FragmentHomeBinding
import com.fimo.aidentist.ui.analisis.*
import com.fimo.aidentist.ui.menu.treatment.DailyTreatmentActivity
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        homeViewModel.loadDiseaseData()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        replaceFragment(ShimmerFragment())
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.dailyTreatment.setOnClickListener {
            val intent = Intent(activity, DailyTreatmentActivity::class.java)
            activity?.startActivity(intent)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeUserProfile()
        observeDiseaseState()
    }

    private fun observeUserProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            homeViewModel.userProfile.collect { user ->
                if (user != null) {
                    if (user.photoUrl != null) {
                        Picasso.get().load(user.photoUrl).into(binding.userProfile)
                    } else {
                        Picasso.get()
                            .load("https://raw.githubusercontent.com/4ahsanul/Workstation/main/ic_avatar_profile_hd-removebg-preview.png?token=GHSAT0AAAAAAB5JEK2JD7JX7E53JWN7L42MY6QOFWA")
                            .into(binding.userProfile)
                    }
                    binding.userNameToolbar.text = user.displayName
                }
            }
        }
    }

    private fun observeDiseaseState() {
        viewLifecycleOwner.lifecycleScope.launch {
            homeViewModel.diseaseState.collect { state ->
                when (state) {
                    is Resource.Success -> {
                        val data = state.data
                        val disease = data?.get("disease") as? String
                        if (disease != null && disease != "null") {
                            routeDiseaseFragment(disease)
                        } else {
                            replaceFragment(BlankAnalisisFragment())
                        }
                    }
                    is Resource.Error -> {
                        Log.d("HomeFragment", "Error loading disease data: ${state.message}")
                        replaceFragment(BlankAnalisisFragment())
                    }
                    is Resource.Loading -> { /* ShimmerFragment already shown */ }
                    null -> { /* idle state */ }
                }
            }
        }
    }

    private fun routeDiseaseFragment(disease: String) {
        when (disease) {
            "Healthy" -> replaceFragment(AnalisisFragment3())
            "Dental Discoloration" -> replaceFragment(AnalisisFragment2())
            "Periodontal Disease" -> replaceFragment(AnalisisFragment())
            else -> replaceFragment(BlankAnalisisFragment())
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        val nav = parentFragmentManager
        val trans = nav.beginTransaction()
        trans.replace(R.id.infoData, fragment)
        trans.commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
