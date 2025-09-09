package com.eno.protokolle.data

import com.eno.protokolle.newmodel.ProtokollConstruct

object ProtokollRepo {
    @Volatile var construct: ProtokollConstruct? = null
}
