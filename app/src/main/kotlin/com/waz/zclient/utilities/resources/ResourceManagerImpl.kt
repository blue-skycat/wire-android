package com.waz.zclient.utilities.resources

import android.content.res.Resources

class ResourceManagerImpl(val resources: Resources) : ResourceManager {

    override fun getStringArray(array: Int): Array<String> {
        return resources.getStringArray(array)
    }
}