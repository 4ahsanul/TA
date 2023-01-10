package com.fimo.aidentist.ui.menu.auth

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fimo.aidentist.R
import com.fimo.aidentist.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var fAuth: FirebaseAuth
    private val db = Firebase.firestore

    private val genderItems = listOf("Laki - Laki", "Perempuan")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fAuth = Firebase.auth

        playAnimation()
        setForm()
        setupAction()
        setupView()
    }

    private fun playAnimation() {
        ObjectAnimator.ofFloat(binding.imageViewSignup, View.TRANSLATION_X, -30F, 30F).apply {
            duration = 6000
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            startDelay = 500
        }.start()

        val title = ObjectAnimator.ofFloat(binding.titleTextView, View.ALPHA, 1F).setDuration(500)
        val nameInput =
            ObjectAnimator.ofFloat(binding.nameEditTextLayout, View.ALPHA, 1F).setDuration(500)
        val emailInput =
            ObjectAnimator.ofFloat(binding.emailEditTextLayout, View.ALPHA, 1F).setDuration(500)
        val genderInput =
            ObjectAnimator.ofFloat(binding.jenisEditTextLayout, View.ALPHA, 1F).setDuration(500)
        val telephoneInput =
            ObjectAnimator.ofFloat(binding.phoneEditTextLayout, View.ALPHA, 1F).setDuration(500)
        val passwordInput =
            ObjectAnimator.ofFloat(binding.passwordEditTextLayout, View.ALPHA, 1F).setDuration(500)
        val button = ObjectAnimator.ofFloat(binding.buttonSignUp, View.ALPHA, 1F).setDuration(500)
        val signupTitle =
            ObjectAnimator.ofFloat(binding.tvTittleLogin, View.ALPHA, 15F).setDuration(500)
        val signUpButton = ObjectAnimator.ofFloat(binding.tvLogin, View.ALPHA, 15F).setDuration(500)

        //Show animate alternate
        AnimatorSet().apply {
            playSequentially(
                title,
                nameInput,
                emailInput,
                genderInput,
                telephoneInput,
                passwordInput,
                button,
                signupTitle,
                signUpButton
            )
            start()
        }
    }

    private fun firebaseSignUp() {
        fAuth.createUserWithEmailAndPassword(
            binding.emailEditText.text.toString(),
            binding.passwordEditText.text.toString()
        ).addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(this, "Register Success", Toast.LENGTH_SHORT).show()
                firebaseSignIn()
            } else {
                Toast.makeText(this, it.exception?.message, Toast.LENGTH_SHORT).show()

            }
        }
    }

    private fun firebaseSignIn() {
        fAuth.signInWithEmailAndPassword(
            binding.emailEditText.text.toString(),
            binding.passwordEditText.text.toString()
        ).addOnCompleteListener {
            if (it.isSuccessful) {
                val nama = hashMapOf(
                    "nama" to binding.nameEditText.text.toString()
                )
                db.collection("users").document(fAuth.currentUser?.uid.toString())
                    .set(nama)
                    .addOnSuccessListener {
                        Log.d(ContentValues.TAG, "Berhasil Menyimpan Data")
                    }
                    .addOnFailureListener { e ->
                        Log.w(ContentValues.TAG, "Error adding document", e)
                    }

                Toast.makeText(applicationContext, "Berhasil Membuat Akun", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, it.exception?.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setForm() {
        val genderAdapter = ArrayAdapter(this, R.layout.item_list_dropdown, genderItems)
        (binding.jenisEditTextLayout.editText as? AutoCompleteTextView)?.setAdapter(genderAdapter)
    }

    private fun setupAction() {
        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        binding.buttonSignUp.setOnClickListener {
            firebaseSignUp()
        }
        setupView()
    }

    private fun setupView() {
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
        supportActionBar?.hide()
    }
}