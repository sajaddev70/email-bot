package com.easystyleshop.massengerapp.di

import androidx.lifecycle.ViewModel
import com.easystyleshop.massengerapp.viewmodel.RegisterViewModel
import com.easystyleshop.massengerapp.viewmodel.ProfileViewModel
import com.easystyleshop.massengerapp.viewmodel.MessageViewModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(ViewModelComponent::class)
abstract class ViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(RegisterViewModel::class)
    abstract fun bindRegisterViewModel(viewModel: RegisterViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ProfileViewModel::class)
    abstract fun bindProfileViewModel(viewModel: ProfileViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(MessageViewModel::class)
    abstract fun bindMessageViewModel(viewModel: MessageViewModel): ViewModel
}
