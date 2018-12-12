package com.pyamsoft.padlock.purge

import dagger.Subcomponent

@Subcomponent(modules = [PurgeProvider::class, PurgeModule::class])
interface PurgeComponent {

  fun inject(fragment: PurgeFragment)
}
