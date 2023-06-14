package com.idunnololz.summit.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController

class SharedViewModel : ViewModel() {
    val currentNavController = MutableLiveData<NavController>()
}