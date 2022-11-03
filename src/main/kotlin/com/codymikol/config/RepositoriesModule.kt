package com.codymikol.config

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

@Module
@ComponentScan("com.codymikol.repositories")
class RepositoriesModule