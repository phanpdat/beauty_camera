package com.example.beauty_cameracamera_app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CameraModule {
    // Sẽ thêm các Provider cho CameraProvider, OpenGL Shader Manager ở đây sau
}
