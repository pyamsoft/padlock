package com.pyamsoft.padlock.purge

import dagger.Subcomponent

@Subcomponent(modules = [PurgeModule::class])
interface PurgeComponent {

  fun inject(fragment: PurgeFragment)
}