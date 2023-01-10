package com.fimo.aidentist.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// Pemangkasan Fitur
@Parcelize
data class DoctorModel(
    var name: String,
    var category: String,
    var rating: String,
    var schedule: String,
    var avatar: Int,
): Parcelable
