package com.shinsak.travle

import android.app.Application
import com.shinsak.travle.data.TripRepository

class TravleApp : Application() {
    val repo: TripRepository by lazy { TripRepository(this) }
}
