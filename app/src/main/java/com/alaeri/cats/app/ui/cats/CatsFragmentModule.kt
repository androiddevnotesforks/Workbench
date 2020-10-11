package com.alaeri.cats.app.ui.cats

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.extras.viewholder.ViewHolderProvider
import androidx.recyclerview.widget.extras.viewholder.adapter.PagedListAdapterWithVHProvider
import androidx.recyclerview.widget.extras.viewholder.factory.IViewHolderFactory
import androidx.recyclerview.widget.extras.viewholder.factory.ViewHolderFactory
import com.alaeri.cats.app.cats.Cat
import com.alaeri.cats.app.databinding.CatItemBinding
import com.alaeri.command.core.Command
import com.alaeri.command.di.commandModule
import org.koin.core.module.Module

/**
 * Created by Emmanuel Requier on 22/04/2020.
 */
object CatsFragmentModule
val catsFragmentModule : Command<Module> = CatsFragmentModule.commandModule {
    commandScope<CatsFragment> {
        scoped { (fragment : CatsFragment) ->  fragment  }
        factory { get<Fragment>().lifecycle }
        scoped<ViewModelStoreOwner> {
            val vmStore = ViewModelStore()
            ViewModelStoreOwner { vmStore }
        }
        factory<Factory> {
            object: Factory{
                override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                    return CatViewModel(get()) as T
                }
            }
        }
        factory<IViewHolderFactory<Cat, CatItemVH>> {
            ViewHolderFactory.newInstance<Cat, CatItemVH, CatItemBinding>(CatItemBinding::inflate){
                    binding, _ -> CatItemVH.CatVH(binding, get(), get())
            }
        }
        factory {

            val viewHolderFactories = getAll<IViewHolderFactory<Cat, CatItemVH>>()
            ViewHolderProvider<Cat, CatItemVH>(
                viewHolderFactories
            )

        }
        factory {
            PagedListUseCase(get(), get())
        }
        factory {
            RefreshUseCase(get(), get())
        }
        factory{
            val diffCallback = CatDiffCallback()
            val asyncDifferConfig: AsyncDifferConfig<Cat> = AsyncDifferConfig.Builder<Cat>(diffCallback).build()
            PagedListAdapterWithVHProvider<Cat, CatItemVH>(get(), asyncDifferConfig)
        }
        viewmodel { CatsViewModel(get(), get(), get()) } }

}

