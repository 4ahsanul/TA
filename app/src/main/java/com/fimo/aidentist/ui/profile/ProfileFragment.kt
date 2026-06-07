package com.fimo.aidentist.ui.profile

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.fimo.aidentist.data.model.Resource
import com.fimo.aidentist.databinding.FragmentProfileBinding
import com.fimo.aidentist.ui.auth.LoginActivity
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val profileViewModel: ProfileViewModel by viewModels()

    private lateinit var imageUri: Uri

    companion object {
        const val REQUEST_CAMERA = 100
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.buttonLogout.setOnClickListener {
            profileViewModel.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            requireActivity().startActivity(intent)
            requireActivity().finish()
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeUserProfile()
        observeUpdateState()
        observeImageUploadState()
        observeEmailVerificationState()
        setupActions()
    }

    private fun observeUserProfile() {
        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.userProfile.collect { user ->
                if (user != null) {
                    if (user.photoUrl != null) {
                        Picasso.get().load(user.photoUrl).into(binding.avatar)
                    } else {
                        Picasso.get()
                            .load("https://raw.githubusercontent.com/4ahsanul/Workstation/main/ic_avatar_profile_hd-removebg-preview.png?token=GHSAT0AAAAAAB5JEK2JD7JX7E53JWN7L42MY6QOFWA")
                            .into(binding.avatar)
                    }

                    binding.etName.setText(user.displayName)
                    binding.etMail.setText(user.email)

                    if (user.isEmailVerified) {
                        binding.icVerified.visibility = View.VISIBLE
                    } else {
                        binding.icUnverified.visibility = View.VISIBLE
                    }

                    if (user.phoneNumber.isNullOrEmpty()) {
                        binding.etPhone.setText("Masukkan nomor telepon anda")
                    } else {
                        binding.etPhone.setText(user.phoneNumber)
                    }
                }
            }
        }
    }

    private fun observeUpdateState() {
        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.updateState.collect { state ->
                when (state) {
                    is Resource.Success -> {
                        Toast.makeText(activity, "Profile berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        profileViewModel.resetUpdateState()
                    }
                    is Resource.Error -> {
                        Toast.makeText(activity, state.message, Toast.LENGTH_SHORT).show()
                        profileViewModel.resetUpdateState()
                    }
                    is Resource.Loading -> { /* TODO: show loading indicator */ }
                    null -> { /* idle state */ }
                }
            }
        }
    }

    private fun observeImageUploadState() {
        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.imageUploadState.collect { state ->
                when (state) {
                    is Resource.Success -> {
                        state.data?.let { uri ->
                            imageUri = uri
                        }
                        profileViewModel.resetImageUploadState()
                    }
                    is Resource.Error -> {
                        Toast.makeText(activity, state.message, Toast.LENGTH_SHORT).show()
                        profileViewModel.resetImageUploadState()
                    }
                    is Resource.Loading -> { /* TODO: show loading indicator */ }
                    null -> { /* idle state */ }
                }
            }
        }
    }

    private fun observeEmailVerificationState() {
        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.emailVerificationState.collect { state ->
                when (state) {
                    is Resource.Success -> {
                        Toast.makeText(activity, "Email verifikasi telah terkirim", Toast.LENGTH_SHORT).show()
                        profileViewModel.resetEmailVerificationState()
                    }
                    is Resource.Error -> {
                        Toast.makeText(activity, state.message, Toast.LENGTH_SHORT).show()
                        profileViewModel.resetEmailVerificationState()
                    }
                    is Resource.Loading -> { /* TODO: show loading indicator */ }
                    null -> { /* idle state */ }
                }
            }
        }
    }

    private fun setupActions() {
        binding.avatar.setOnClickListener {
            intentCamera()
        }

        binding.btnSave.setOnClickListener {
            val image = when {
                ::imageUri.isInitialized -> imageUri
                else -> Uri.parse("https://raw.githubusercontent.com/4ahsanul/Workstation/main/ic_avatar_profile_hd-removebg-preview.png?token=GHSAT0AAAAAAB5JEK2JD7JX7E53JWN7L42MY6QOFWA")
            }

            val name = binding.etName.text.toString().trim()

            if (name.isEmpty()) {
                binding.etName.error = "Nama harus diisi"
                binding.etName.requestFocus()
                return@setOnClickListener
            }

            profileViewModel.updateProfile(name, image)
        }

        binding.icUnverified.setOnClickListener {
            profileViewModel.sendEmailVerification()
        }
    }

    private fun intentCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            activity?.packageManager?.let {
                intent.resolveActivity(it).also {
                    startActivityForResult(intent, REQUEST_CAMERA)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK) {
            val imgBitmap = data?.extras?.get("data") as Bitmap
            binding.avatar.setImageBitmap(imgBitmap)
            profileViewModel.uploadProfileImage(imgBitmap)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
