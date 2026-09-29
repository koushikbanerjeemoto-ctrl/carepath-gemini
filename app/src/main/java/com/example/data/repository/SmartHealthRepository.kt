package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.remote.CarePathBackendClient
import com.example.domain.AIActionEngine
import com.example.domain.AIResponse
import com.example.domain.DistanceCalculator
import com.example.domain.TriageEngine
import com.example.domain.TriageInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

data class HospitalWithDistance(
    val hospital: HospitalEntity,
    val distanceKm: Double,
    val distanceFormatted: String,
    val bedAvailabilities: List<BedAvailabilityEntity> = emptyList(),
    val totalIcuAvailable: Int = 0,
    val totalEmergencyAvailable: Int = 0,
    val totalGeneralAvailable: Int = 0
)

data class HospitalFilterState(
    val query: String = "",
    val type: HospitalType? = null,
    val specialization: String? = null,
    val requireIcu: Boolean = false,
    val requireEmergencyBed: Boolean = false,
    val maxDistanceKm: Double = 50.0
)

class SmartHealthRepository(
    private val dao: SmartHealthDao,
    context: Context? = null
) {
    private val prefs =
        context?.getSharedPreferences("smart_health_prefs", Context.MODE_PRIVATE)

    // Current user location
    private val _currentLocation =
        MutableStateFlow(Pair(22.44335, 88.41543))

    val currentLocation: StateFlow<Pair<Double, Double>> =
        _currentLocation.asStateFlow()

    private val _currentLocationName =
        MutableStateFlow("Sonarpur / Narendrapur, Kolkata")

    val currentLocationName: StateFlow<String> =
        _currentLocationName.asStateFlow()

    private val _activeRole =
        MutableStateFlow(UserRole.GUEST)

    val activeRole: StateFlow<UserRole> =
        _activeRole.asStateFlow()

    private val _currentLanguage =
        MutableStateFlow(
            prefs?.getString("selected_language", "EN") ?: "EN"
        )

    val currentLanguage: StateFlow<String> =
        _currentLanguage.asStateFlow()

    private val _filterState =
        MutableStateFlow(HospitalFilterState())

    val filterState: StateFlow<HospitalFilterState> =
        _filterState.asStateFlow()

    val currentUser: Flow<UserEntity?> =
        dao.getCurrentUser()

    val latestGuestSession: Flow<GuestSessionEntity?> =
        dao.getLatestGuestSession()

    val healthProfile: Flow<HealthProfileEntity?> =
        dao.getHealthProfile()

    val emergencyContacts: Flow<List<EmergencyContactEntity>> =
        dao.getAllEmergencyContacts()

    val savedHospitals: Flow<List<SavedHospitalEntity>> =
        dao.getSavedHospitals()

    val symptomCategories: Flow<List<SymptomCategoryEntity>> =
        dao.getAllSymptomCategories()

    val allSymptoms: Flow<List<SymptomEntity>> =
        dao.getAllSymptoms()

    val healthAssessments: Flow<List<HealthAssessmentEntity>> =
        dao.getAllAssessments()

    val allHospitals: Flow<List<HospitalEntity>> =
        dao.getAllHospitals()

    val allBedAvailabilities: Flow<List<BedAvailabilityEntity>> =
        dao.getAllBedAvailabilities()

    val ambulances: Flow<List<AmbulanceEntity>> =
        dao.getAllAmbulances()

    val activeAmbulanceRequest: Flow<AmbulanceRequestEntity?> =
        dao.getActiveAmbulanceRequest()

    val allAmbulanceRequests: Flow<List<AmbulanceRequestEntity>> =
        dao.getAllAmbulanceRequests()

    val chatMessages: Flow<List<ChatMessageEntity>> =
        dao.getAllChatMessages()

    val medicalReports: Flow<List<MedicalReportEntity>> =
        dao.getAllMedicalReports()

    val prescriptions: Flow<List<PrescriptionEntity>> =
        dao.getAllPrescriptions()

    val medicines: Flow<List<MedicineEntity>> =
        dao.getAllMedicines()

    val medicineAvailabilities: Flow<List<MedicineAvailabilityEntity>> =
        dao.getAllMedicineAvailabilities()

    val diagnosticTests: Flow<List<DiagnosticTestEntity>> =
        dao.getAllDiagnosticTests()

    val facilityDiagnostics: Flow<List<FacilityDiagnosticEntity>> =
        dao.getAllFacilityDiagnostics()

    val doctors: Flow<List<DoctorEntity>> =
        dao.getAllDoctors()

    val appointments: Flow<List<AppointmentEntity>> =
        dao.getAllAppointments()

    val liveQueue: Flow<QueueEntity?> =
        dao.getLiveQueue()

    val referrals: Flow<List<ReferralEntity>> =
        dao.getAllReferrals()

    val followUps: Flow<List<FollowUpEntity>> =
        dao.getAllFollowUps()

    val maternalHealth: Flow<MaternalHealthEntity?> =
        dao.getMaternalHealth()

    val childHealth: Flow<ChildHealthEntity?> =
        dao.getChildHealth()

    val chronicCare: Flow<List<ChronicCareEntity>> =
        dao.getAllChronicCare()

    val apiSyncLogs: Flow<List<ApiSyncLogEntity>> =
        dao.getAllApiSyncLogs()


    // ---------------------------------------------------------
    // HOSPITALS
    // ---------------------------------------------------------

    val filteredHospitalsWithDistance: Flow<List<HospitalWithDistance>> =
        combine(
            dao.getAllHospitals(),
            dao.getAllBedAvailabilities(),
            _currentLocation,
            _filterState
        ) { hospitals, beds, location, filter ->

            val userLat = location.first
            val userLng = location.second

            hospitals
                .map { hosp ->

                    val dist =
                        DistanceCalculator.calculateDistanceKm(
                            userLat,
                            userLng,
                            hosp.latitude,
                            hosp.longitude
                        )

                    val hospBeds =
                        beds.filter {
                            it.hospitalId == hosp.id
                        }

                    val icuAvail =
                        hospBeds
                            .find {
                                it.bedTypeName.contains(
                                    "ICU",
                                    ignoreCase = true
                                )
                            }
                            ?.available ?: 0

                    val emAvail =
                        hospBeds
                            .find {
                                it.bedTypeName.contains(
                                    "Emergency",
                                    ignoreCase = true
                                )
                            }
                            ?.available ?: 0

                    val genAvail =
                        hospBeds
                            .find {
                                it.bedTypeName.contains(
                                    "General",
                                    ignoreCase = true
                                )
                            }
                            ?.available ?: 0

                    HospitalWithDistance(
                        hospital = hosp,
                        distanceKm = dist,
                        distanceFormatted =
                            DistanceCalculator.formatDistance(dist),
                        bedAvailabilities = hospBeds,
                        totalIcuAvailable = icuAvail,
                        totalEmergencyAvailable = emAvail,
                        totalGeneralAvailable = genAvail
                    )
                }
                .filter { item ->

                    val matchesQuery =
                        filter.query.isBlank() ||
                                item.hospital.name.contains(
                                    filter.query,
                                    ignoreCase = true
                                ) ||
                                item.hospital.area.contains(
                                    filter.query,
                                    ignoreCase = true
                                ) ||
                                item.hospital.address.contains(
                                    filter.query,
                                    ignoreCase = true
                                )

                    val matchesType =
                        filter.type == null ||
                                item.hospital.type == filter.type

                    val matchesIcu =
                        !filter.requireIcu ||
                                item.totalIcuAvailable > 0

                    val matchesEm =
                        !filter.requireEmergencyBed ||
                                item.totalEmergencyAvailable > 0

                    val matchesDist =
                        item.distanceKm <= filter.maxDistanceKm

                    matchesQuery &&
                            matchesType &&
                            matchesIcu &&
                            matchesEm &&
                            matchesDist
                }
                .sortedBy { it.distanceKm }
                .take(30)
        }


    fun updateLocation(
        lat: Double,
        lng: Double,
        locationName: String
    ) {
        _currentLocation.value = Pair(lat, lng)
        _currentLocationName.value = locationName
    }


    fun updateFilter(filter: HospitalFilterState) {
        _filterState.value = filter
    }


    fun resetFilter() {
        _filterState.value = HospitalFilterState()
    }


    fun setRole(role: UserRole) {
        _activeRole.value = role
    }


    fun setLanguage(lang: String) {
        prefs?.edit()
            ?.putString("selected_language", lang)
            ?.apply()

        _currentLanguage.value = lang
    }


    // ---------------------------------------------------------
    // TRIAGE
    // ---------------------------------------------------------

    suspend fun performTriage(
        input: TriageInput
    ): HealthAssessmentEntity =
        withContext(Dispatchers.IO) {

            val assessment =
                TriageEngine.evaluateTriage(
                    input,
                    "guest_active"
                )

            dao.insertHealthAssessment(assessment)

            assessment
        }


    // ---------------------------------------------------------
    // SAVED HOSPITAL
    // ---------------------------------------------------------

    suspend fun saveHospital(
        hospitalId: String
    ) = withContext(Dispatchers.IO) {

        dao.insertSavedHospital(
            SavedHospitalEntity(
                id = "saved_${System.currentTimeMillis()}",
                userId = "user_default",
                hospitalId = hospitalId
            )
        )
    }


    suspend fun removeSavedHospital(
        hospitalId: String
    ) = withContext(Dispatchers.IO) {

        dao.deleteSavedHospital(hospitalId)
    }


    // ---------------------------------------------------------
    // AMBULANCE
    // ---------------------------------------------------------

    suspend fun requestAmbulance(
        pickupAddress: String,
        destinationHospitalId: String?,
        destinationName: String,
        vehicleType: String = "ALS - ICU Mobile Unit"
    ): AmbulanceRequestEntity =
        withContext(Dispatchers.IO) {

            val request =
                AmbulanceRequestEntity(
                    id = "amb_req_${System.currentTimeMillis()}",
                    ambulanceId = "amb_1",
                    providerName = "WB EMRI 108 Emergency Service",
                    vehicleNumber = "WB-04-1081",
                    pickupAddress = pickupAddress,
                    pickupLat = _currentLocation.value.first,
                    pickupLng = _currentLocation.value.second,
                    destinationHospitalId = destinationHospitalId,
                    destinationName = destinationName,
                    status = AmbulanceStatus.DISPATCHED,
                    etaMinutes = 9
                )

            dao.insertAmbulanceRequest(request)

            dao.insertApiSyncLog(
                ApiSyncLogEntity(
                    id = "sync_amb_${System.currentTimeMillis()}",
                    apiId = "api_ambulance_108",
                    hospitalName = destinationName,
                    status = SyncStatus.SUCCESS,
                    recordsUpdated = 1
                )
            )

            request
        }


    suspend fun updateAmbulanceStatus(
        request: AmbulanceRequestEntity,
        newStatus: AmbulanceStatus
    ) = withContext(Dispatchers.IO) {

        dao.updateAmbulanceRequest(
            request.copy(status = newStatus)
        )
    }


    // =========================================================
    // CAREPATH AI CHAT
    // =========================================================

    suspend fun sendChatMessage(
        userMessage: String
    ): AIResponse = withContext(Dispatchers.IO) {

        val chatSessionId = "session_main"

        Log.i(
            "CarePathAI",
            "=========================================="
        )

        Log.i(
            "CarePathAI",
            "[CarePathAI] sendChatMessage() START"
        )

        Log.i(
            "CarePathAI",
            "[CarePathAI] User message: $userMessage"
        )

        // -----------------------------------------------------
        // 1. SAVE USER MESSAGE
        // -----------------------------------------------------

        try {

            val userMsgEntity =
                ChatMessageEntity(
                    id = "msg_user_${System.currentTimeMillis()}",
                    chatSessionId = chatSessionId,
                    sender = ChatSender.USER,
                    message = userMessage
                )

            dao.insertChatMessage(userMsgEntity)

            Log.i(
                "CarePathAI",
                "[CarePathAI] User message saved to Room"
            )

        } catch (e: Exception) {

            Log.e(
                "CarePathAI",
                "[CarePathAI] Room user-message save failed: ${e.message}",
                e
            )

            // IMPORTANT:
            // Do NOT stop AI request if local Room save fails.
        }


        // -----------------------------------------------------
        // 2. LANGUAGE
        // -----------------------------------------------------

        val currentLang =
            _currentLanguage.value

        Log.i(
            "CarePathAI",
            "[CarePathAI] Language: $currentLang"
        )


        // -----------------------------------------------------
        // 3. DIRECT CAREPATH FEATURE ACTION
        // -----------------------------------------------------

        val directResponse =
            try {

                if (
                    AIActionEngine.isCarePathFeatureRequest(
                        userMessage
                    )
                ) {

                    Log.i(
                        "CarePathAI",
                        "[CarePathAI] Direct CarePath feature request detected"
                    )

                    AIActionEngine.processDirectFeatureQuery(
                        userMessage,
                        currentLang
                    )

                } else {

                    null
                }

            } catch (e: Exception) {

                Log.e(
                    "CarePathAI",
                    "[CarePathAI] AIActionEngine error: ${e.message}",
                    e
                )

                null
            }


        // -----------------------------------------------------
        // 4. IF DIRECT ACTION EXISTS
        // -----------------------------------------------------

        val aiResponse: AIResponse

        if (
            directResponse != null &&
            directResponse.actionType != null
        ) {

            Log.i(
                "CarePathAI",
                "[CarePathAI] Direct action selected: ${directResponse.actionType}"
            )

            aiResponse = directResponse

        } else {

            // -------------------------------------------------
            // 5. DIRECT RENDER AI REQUEST
            // -------------------------------------------------

            Log.i(
                "CarePathAI",
                "[CarePathAI] Preparing DIRECT Render request..."
            )

            val remoteResponse =
                try {

                    // IMPORTANT:
                    // Do NOT read Room chat history before network call.
                    // We send the current user message directly to Render.

                    val historyList =
                        listOf(
                            "user" to userMessage
                        )

                    Log.i(
                        "CarePathAI",
                        "[CarePathAI] Calling Render backend..."
                    )

                    val result =
                        CarePathBackendClient.queryMedicalAI(
                            conversationHistory = historyList,
                            language = currentLang
                        )

                    Log.i(
                        "CarePathAI",
                        "[CarePathAI] Render call returned. Success=${result != null}"
                    )

                    result

                } catch (e: Exception) {

                    Log.e(
                        "CarePathAI",
                        "[CarePathAI] Render call EXCEPTION: ${e.message}",
                        e
                    )

                    null
                }


            // -------------------------------------------------
            // 6. PROCESS RENDER RESPONSE
            // -------------------------------------------------

            if (
                remoteResponse != null &&
                remoteResponse.replyText.isNotBlank()
            ) {

                Log.i(
                    "CarePathAI",
                    "[CarePathAI] =================================="
                )

                Log.i(
                    "CarePathAI",
                    "[CarePathAI] AI RESPONSE RECEIVED"
                )

                Log.i(
                    "CarePathAI",
                    "[CarePathAI] ${remoteResponse.replyText}"
                )

                Log.i(
                    "CarePathAI",
                    "[CarePathAI] =================================="
                )

                aiResponse = remoteResponse

            } else {

                val errorDetail =
                    CarePathBackendClient.lastConnectionError

                Log.e(
                    "CarePathAI",
                    "[CarePathAI] =================================="
                )

                Log.e(
                    "CarePathAI",
                    "[CarePathAI] RENDER REQUEST FAILED"
                )

                Log.e(
                    "CarePathAI",
                    "[CarePathAI] Last connection error: $errorDetail"
                )

                Log.e(
                    "CarePathAI",
                    "[CarePathAI] =================================="
                )


                // Emergency local fallback
                val hasEmergency =
                    try {
                        AIActionEngine.hasEmergencySymptoms(
                            userMessage
                        )
                    } catch (e: Exception) {
                        false
                    }


                if (hasEmergency) {

                    aiResponse =
                        AIActionEngine.getFallbackMedicalResponse(
                            currentLang,
                            true
                        )

                } else {

                    val errMsg =
                        when (currentLang.uppercase()) {

                            "HI" ->
                                "क्षमा करें, मैं अभी केयरपाथ एआई सेवा से कनेक्ट नहीं हो सका। कृपया पुनः प्रयास करें।"

                            "BN" ->
                                "দুঃখিত, আমি এই মুহূর্তে কেয়ারপাথ এআই সার্ভিসে সংযোগ করতে পারছি না। অনুগ্রহ করে আবার চেষ্টা করুন।"

                            else ->
                                "Sorry, I couldn't connect to the CarePath AI service right now. Please try again."
                        }


                    aiResponse =
                        AIResponse(
                            replyText = errMsg,
                            actionType = null,
                            actionLabel = null,
                            actionPayload = null
                        )
                }
            }
        }


        // -----------------------------------------------------
        // 7. SAVE AI RESPONSE
        // -----------------------------------------------------

        try {

            val aiMsgEntity =
                ChatMessageEntity(
                    id = "msg_ai_${System.currentTimeMillis()}",
                    chatSessionId = chatSessionId,
                    sender = ChatSender.AI,
                    message = aiResponse.replyText,
                    actionType = aiResponse.actionType,
                    actionPayload = aiResponse.actionPayload
                )

            dao.insertChatMessage(aiMsgEntity)

            Log.i(
                "CarePathAI",
                "[CarePathAI] AI response saved to Room"
            )

        } catch (e: Exception) {

            Log.e(
                "CarePathAI",
                "[CarePathAI] Room AI-response save failed: ${e.message}",
                e
            )

            // Do not break the UI just because local storage failed.
        }


        Log.i(
            "CarePathAI",
            "[CarePathAI] sendChatMessage() END"
        )

        Log.i(
            "CarePathAI",
            "=========================================="
        )

        return@withContext aiResponse
    }


    // ---------------------------------------------------------
    // APPOINTMENT
    // ---------------------------------------------------------

    suspend fun bookAppointment(
        patientName: String,
        doctor: DoctorEntity,
        date: String,
        timeSlot: String,
        type: ConsultationType,
        notes: String
    ): AppointmentEntity =
        withContext(Dispatchers.IO) {

            val appt =
                AppointmentEntity(
                    id = "appt_${System.currentTimeMillis()}",
                    patientName = patientName,
                    doctorId = doctor.id,
                    doctorName = doctor.name,
                    hospitalId = doctor.hospitalId,
                    hospitalName = doctor.hospitalName,
                    specialization = doctor.specialization,
                    appointmentDate = date,
                    timeSlot = timeSlot,
                    type = type,
                    status = AppointmentStatus.CONFIRMED,
                    tokenNumber = "A-${(20..45).random()}",
                    notes = notes
                )

            dao.insertAppointment(appt)

            appt
        }


    // ---------------------------------------------------------
    // MEDICAL REPORT
    // ---------------------------------------------------------

    suspend fun addMedicalReport(
        hospitalName: String,
        reportType: String,
        fileName: String,
        aiAnalysis: String
    ) = withContext(Dispatchers.IO) {

        val report =
            MedicalReportEntity(
                id = "rep_${System.currentTimeMillis()}",
                hospitalName = hospitalName,
                reportType = reportType,
                fileName = fileName,
                reportDate = "Today",
                aiAnalysis = aiAnalysis
            )

        dao.insertMedicalReport(report)
    }


    // ---------------------------------------------------------
    // PRESCRIPTION
    // ---------------------------------------------------------

    suspend fun addPrescription(
        doctorName: String,
        hospitalName: String,
        diagnosis: String,
        instructions: String
    ) = withContext(Dispatchers.IO) {

        val rx =
            PrescriptionEntity(
                id = "rx_${System.currentTimeMillis()}",
                doctorName = doctorName,
                hospitalName = hospitalName,
                date = "Today",
                diagnosis = diagnosis,
                instructions = instructions
            )

        dao.insertPrescription(rx)
    }


    // ---------------------------------------------------------
    // REFERRAL
    // ---------------------------------------------------------

    suspend fun createReferral(
        patientName: String,
        patientAge: Int,
        patientGender: String,
        fromFacility: String,
        toHospital: HospitalEntity,
        specialization: String,
        clinicalSummary: String,
        urgency: UrgencyLevel,
        healthWorker: String
    ): ReferralEntity =
        withContext(Dispatchers.IO) {

            val ref =
                ReferralEntity(
                    id = "ref_${System.currentTimeMillis()}",
                    patientName = patientName,
                    patientAge = patientAge,
                    patientGender = patientGender,
                    fromFacility = fromFacility,
                    toHospitalId = toHospital.id,
                    toHospitalName = toHospital.name,
                    specializationRequired = specialization,
                    clinicalSummary = clinicalSummary,
                    urgency = urgency,
                    status = ReferralStatus.SENT,
                    createdByWorker = healthWorker
                )

            dao.insertReferral(ref)

            ref
        }


    suspend fun updateReferralStatus(
        referral: ReferralEntity,
        newStatus: ReferralStatus
    ) = withContext(Dispatchers.IO) {

        dao.updateReferral(
            referral.copy(
                status = newStatus,
                updatedAt = System.currentTimeMillis()
            )
        )
    }


    suspend fun updateBedAvailability(
        bed: BedAvailabilityEntity,
        available: Int
    ) = withContext(Dispatchers.IO) {

        dao.updateBedAvailability(
            bed.copy(
                available = available,
                lastUpdated = System.currentTimeMillis()
            )
        )
    }


    // ---------------------------------------------------------
    // USER
    // ---------------------------------------------------------

    suspend fun registerUser(
        name: String,
        email: String,
        phone: String,
        mergeGuestActivity: Boolean
    ): UserEntity =
        withContext(Dispatchers.IO) {

            val user =
                UserEntity(
                    id = "user_${System.currentTimeMillis()}",
                    name = name,
                    email = email,
                    phone = phone,
                    role = UserRole.PATIENT
                )

            dao.insertUser(user)

            _activeRole.value = UserRole.PATIENT

            user
        }


    suspend fun updateHealthProfile(
        profile: HealthProfileEntity
    ) = withContext(Dispatchers.IO) {

        dao.insertHealthProfile(profile)
    }


    // ---------------------------------------------------------
    // EMERGENCY CONTACTS
    // ---------------------------------------------------------

    suspend fun addEmergencyContact(
        name: String,
        relation: String,
        phone: String
    ) = withContext(Dispatchers.IO) {

        val contact =
            EmergencyContactEntity(
                contactId = "ec_${System.currentTimeMillis()}",
                name = name,
                relationship = relation,
                phone = phone,
                priority = 1
            )

        dao.insertEmergencyContact(contact)
    }


    suspend fun deleteEmergencyContact(
        contact: EmergencyContactEntity
    ) = withContext(Dispatchers.IO) {

        dao.deleteEmergencyContact(contact)
    }


    // ---------------------------------------------------------
    // FOLLOW UP
    // ---------------------------------------------------------

    suspend fun addFollowUp(
        patientName: String,
        riskLevel: RiskLevel,
        purpose: String,
        hospitalName: String,
        doctorName: String,
        dueDate: String
    ) = withContext(Dispatchers.IO) {

        val followUp =
            FollowUpEntity(
                id = "fu_${System.currentTimeMillis()}",
                patientName = patientName,
                riskLevel = riskLevel,
                purpose = purpose,
                hospitalName = hospitalName,
                doctorName = doctorName,
                dueDate = dueDate
            )

        dao.insertFollowUp(followUp)
    }
}