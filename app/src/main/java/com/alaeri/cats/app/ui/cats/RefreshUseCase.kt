package com.alaeri.cats.app.ui.cats

import com.alaeri.cats.app.cats.CatRepository
import com.alaeri.cats.app.user.UserRepository
import com.alaeri.cats.app.logBlockingFlow
import com.alaeri.cats.app.logFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take

class RefreshUseCase(private val userRepository: UserRepository, private val catRepository: CatRepository){

    operator fun invoke(): Flow<NetworkState> = logBlockingFlow("RefreshUseCase") {
        flow{
            emit(NetworkState.Loading)
            try {
                val user = logFlow("userFlow"){ userRepository.currentUser } .take(1).first()
                if (user != null) {
                    catRepository.refresh(user)
                } else {
                    throw RuntimeException("no user connected")
                }
                emit(NetworkState.Idle())
            } catch (e: Exception) {
                emit(NetworkState.Idle(e))
            }
        }

    }
}