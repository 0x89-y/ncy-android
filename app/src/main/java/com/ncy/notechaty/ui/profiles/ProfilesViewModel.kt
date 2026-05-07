package com.ncy.notechaty.ui.profiles

import androidx.lifecycle.ViewModel
import com.ncy.notechaty.data.model.Profile
import com.ncy.notechaty.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ProfilesViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    val profiles: StateFlow<List<Profile>> = profileRepository.profiles
    val activeProfileId: StateFlow<String> = profileRepository.activeProfileId

    fun createProfile(name: String) {
        profileRepository.createProfile(name)
    }

    fun renameProfile(id: String, name: String) {
        profileRepository.renameProfile(id, name)
    }

    fun deleteProfile(id: String) {
        if (profileRepository.profiles.value.size <= 1) return
        profileRepository.deleteProfile(id)
    }

    fun setProfilePin(id: String, pin: String) {
        profileRepository.setProfilePin(id, pin)
    }

    fun clearProfilePin(id: String) {
        profileRepository.clearProfilePin(id)
    }

    fun setProfileIcon(id: String, iconKey: String) {
        profileRepository.setProfileIcon(id, iconKey)
    }
}
