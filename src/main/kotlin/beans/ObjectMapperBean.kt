package com.codymikol.com.codymikol.beans

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.koin.core.annotation.Single

@Single
class ObjectMapperBean {
    val value: ObjectMapper by lazy { ObjectMapper().registerKotlinModule() }
}