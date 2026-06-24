package com.techtower.meetingtranscriber.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRepositoryTest {
    @Test
    fun discoveryCompactList_defaultsToDetailedAndPersistsChoice() {
        val repository = SettingsRepository(FakeSharedPreferences())

        assertFalse(repository.isDiscoveryCompactList())

        repository.saveDiscoveryCompactList(true)
        assertTrue(repository.isDiscoveryCompactList())

        repository.saveDiscoveryCompactList(false)
        assertFalse(repository.isDiscoveryCompactList())
    }
}
