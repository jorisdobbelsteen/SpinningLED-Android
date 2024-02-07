package nl.joris2k.spinningled.di

import android.app.Application
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import nl.joris2k.spinningled.repository.DeviceRepository
import nl.joris2k.spinningled.repository.DiscoveryRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
}
