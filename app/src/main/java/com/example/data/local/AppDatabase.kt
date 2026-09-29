package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        GuestSessionEntity::class,
        SessionEntity::class,
        HealthProfileEntity::class,
        EmergencyContactEntity::class,
        SavedHospitalEntity::class,
        SymptomCategoryEntity::class,
        SymptomEntity::class,
        HealthAssessmentEntity::class,
        AssessmentSymptomEntity::class,
        HospitalEntity::class,
        HospitalDepartmentEntity::class,
        SpecializationEntity::class,
        HospitalSpecializationEntity::class,
        HospitalApiEntity::class,
        BedEntity::class,
        BedTypeEntity::class,
        BedAvailabilityEntity::class,
        ApiSyncLogEntity::class,
        HospitalSearchEntity::class,
        HospitalSearchResultEntity::class,
        HospitalSelectionEntity::class,
        NavigationRequestEntity::class,
        EmergencyRequestEntity::class,
        AmbulanceProviderEntity::class,
        AmbulanceEntity::class,
        AmbulanceRequestEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        MedicalReportEntity::class,
        PrescriptionEntity::class,
        MedicineEntity::class,
        PrescriptionMedicineEntity::class,
        AiAnalysisEntity::class,
        DoctorEntity::class,
        AppointmentEntity::class,
        QueueEntity::class,
        ReferralEntity::class,
        DiagnosticTestEntity::class,
        FacilityDiagnosticEntity::class,
        MedicineAvailabilityEntity::class,
        FollowUpEntity::class,
        MaternalHealthEntity::class,
        ChildHealthEntity::class,
        ChronicCareEntity::class,
        NotificationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun smartHealthDao(): SmartHealthDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_health_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
