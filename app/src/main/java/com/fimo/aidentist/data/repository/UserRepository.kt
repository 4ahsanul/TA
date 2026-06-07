package com.fimo.aidentist.data.repository

import android.graphics.Bitmap
import com.fimo.aidentist.data.model.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

class UserRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    suspend fun getUserDiseaseData(uid: String): Resource<Map<String, Any?>> {
        return try {
            val document = firestore.collection("users").document(uid).get().await()
            if (document.exists() && document.data != null) {
                @Suppress("UNCHECKED_CAST")
                Resource.Success(document.data as Map<String, Any?>)
            } else {
                Resource.Error("User data not found")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get user data")
        }
    }

    suspend fun saveDiseaseData(uid: String, disease: String, confidence: Float): Resource<Unit> {
        return try {
            val data = hashMapOf(
                "disease" to disease,
                "confidence" to confidence
            )
            firestore.collection("users").document(uid).set(data).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to save disease data")
        }
    }

    suspend fun updateProfile(displayName: String, photoUri: android.net.Uri?): Resource<Unit> {
        return try {
            val user = auth.currentUser ?: return Resource.Error("No user logged in")
            val request = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .setPhotoUri(photoUri)
                .build()
            user.updateProfile(request).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update profile")
        }
    }

    suspend fun uploadProfileImage(uid: String, bitmap: Bitmap): Resource<android.net.Uri> {
        return try {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val imageBytes = baos.toByteArray()

            val reference = storage.reference.child("imagesUpProfile/$uid")
            reference.putBytes(imageBytes).await()
            val downloadUri = reference.downloadUrl.await()
            Resource.Success(downloadUri)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to upload image")
        }
    }
}
