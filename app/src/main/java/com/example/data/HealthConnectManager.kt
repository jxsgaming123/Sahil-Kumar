package com.example.data

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HealthConnectManager(private val context: Context) {

    private val healthConnectClient by lazy {
        try {
            if (isAvailable()) {
                HealthConnectClient.getOrCreate(context)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("HealthConnectManager", "Error initializing HealthConnectClient", e)
            null
        }
    }

    fun isAvailable(): Boolean {
        return try {
            val status = HealthConnectClient.getSdkStatus(context)
            status == HealthConnectClient.SDK_AVAILABLE
        } catch (e: Exception) {
            false
        }
    }

    suspend fun hasReadStepsPermission(): Boolean {
        val client = healthConnectClient ?: return false
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            granted.contains(HealthPermission.getReadPermission(StepsRecord::class))
        } catch (e: Exception) {
            Log.e("HealthConnectManager", "Error checking permissions", e)
            false
        }
    }

    fun getRequiredPermissions(): Set<String> {
        return setOf(HealthPermission.getReadPermission(StepsRecord::class))
    }

    suspend fun fetchDailySteps(): Int {
        val client = healthConnectClient ?: return 0
        if (!hasReadStepsPermission()) {
            Log.w("HealthConnectManager", "Missing READ_STEPS permission for Health Connect")
            return 0
        }
        return try {
            val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfToday = Instant.now()

            val request = ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfToday)
            )
            val response = client.readRecords(request)
            val totalSteps = response.records.sumOf { it.count }.toInt()
            Log.d("HealthConnectManager", "Fetched $totalSteps steps from Health Connect")
            totalSteps
        } catch (e: Exception) {
            Log.e("HealthConnectManager", "Error reading steps from Health Connect", e)
            0
        }
    }
}
