package com.app.droidcast.utils.ioc;

import com.app.droidcast.ioc.DroidCastModule;
import dagger.Module;

// Check module includes if needed
@Module(includes = { DroidCastModule.class }) public class DroidCastTestModule {
}
