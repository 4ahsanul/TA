package com.fimo.aidentist.ui.navigation.profile

import android.app.Activity.RESULT_OK
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.fimo.aidentist.databinding.FragmentProfileBinding
import com.fimo.aidentist.helper.PreferenceHelper
import com.fimo.aidentist.ui.menu.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream

class ProfileFragment : Fragment() {
    private lateinit var sharedPref: PreferenceHelper
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var fAuth: FirebaseAuth

    // Photo Camera
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
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            requireActivity().startActivity(intent)
            requireActivity().finish()
            fAuth.signOut()
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fAuth = FirebaseAuth.getInstance()

        val user = fAuth.currentUser

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
                //Make string value for this text
                binding.etPhone.setText("Masukkan nomor telepon anda")
            } else {
                binding.etPhone.setText(user.phoneNumber)
            }
        }

        binding.avatar.setOnClickListener {
            intentCamera()
        }

        binding.btnSave.setOnClickListener {
            val image = when {
                ::imageUri.isInitialized -> imageUri
                user?.photoUrl == null -> Uri.parse("https://raw.githubusercontent.com/4ahsanul/Workstation/main/ic_avatar_profile_hd-removebg-preview.png?token=GHSAT0AAAAAAB5JEK2JD7JX7E53JWN7L42MY6QOFWA")
                else -> user.photoUrl
            }

            val name = binding.etName.text.toString().trim()

            if (name.isEmpty()) {
                binding.etName.error = "Nama harus diisi"
                binding.etName.requestFocus()
                return@setOnClickListener
            }

            UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .setPhotoUri(image)
                .build().also {
                    user?.updateProfile(it)?.addOnCompleteListener {
                        if (it.isSuccessful) {
                            Toast.makeText(
                                activity,
                                "Profile berhasil diperbarui",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                activity,
                                "${it.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
        }

        binding.icUnverified.setOnClickListener {
            user?.sendEmailVerification()?.addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(
                        activity,
                        "Email verifikasi telah terkirim",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        activity,
                        "${it.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun intentCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            activity?.packageManager?.let {
                intent.resolveActivity(it).also {
                    //Deprecated but still works, so why not
                    startActivityForResult(intent, REQUEST_CAMERA)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK) {
            val imgBitmap = data?.extras?.get("data") as Bitmap
            uploadImage(imgBitmap)
        }
    }

    private fun uploadImage(imgBitmap: Bitmap) {
        val byteAOS = ByteArrayOutputStream()
        val reference = FirebaseStorage.getInstance().reference.child(
            "imagesUpProfile/${FirebaseAuth.getInstance().currentUser?.uid}"
        )

        imgBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteAOS)
        val image = byteAOS.toByteArray()

        reference.putBytes(image).addOnCompleteListener {
            if (it.isSuccessful) {
                reference.downloadUrl.addOnCompleteListener {
                    it.result?.let {
                        imageUri = it
                        binding.avatar.setImageBitmap(imgBitmap)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}