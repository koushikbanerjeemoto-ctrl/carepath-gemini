package com.example.domain

data class AIResponse(
    val replyText: String,
    val actionType: String? = null,
    val actionLabel: String? = null,
    val actionPayload: String? = null
)

object AIActionEngine {

    /**
     * Identifies if a query is an explicit request for an existing CarePath tool/action
     * rather than a conversational medical inquiry.
     */
    fun isCarePathFeatureRequest(userQuery: String): Boolean {
        val lower = userQuery.lowercase().trim()

        // Quick Chip Labels (exact or close match)
        val chips = listOf(
            "request emergency ambulance", "find icu beds near me", "show government hospitals",
            "check my symptoms (triage)", "explain blood test report", "book a doctor appointment",
            "search medicines in pharmacy", "check my symptoms", "check symptoms"
        )
        if (chips.any { lower.contains(it) }) return true

        // Explicit Emergency dispatch / SOS request
        if (lower.contains("ambulance") || lower.contains("ambulence") || lower.contains("অ্যাম্বুলেন্স") || lower.contains("एम्बुलेंस") ||
            lower.contains("emergency") || lower.contains("জরুরি") || lower.contains("आपातकाल") || lower.contains("sos") ||
            lower.contains("108") || lower.contains("save me")) {
            if (lower.contains("call") || lower.contains("need") || lower.contains("request") || lower.contains("dispatch") ||
                lower.contains("send") || lower.contains("emergency") || lower.contains("sos") || lower.contains("ডাকুন") ||
                lower.contains("জরুরি সাহায্য") || lower.contains("आपातकालीन मदद")) {
                return true
            }
        }

        // Explicit Hospital & ICU Search
        val hasHospital = lower.contains("hospital") || lower.contains("হাসপাতাল") || lower.contains("अस्पताल")
        val hasSearchOrNeed = lower.contains("need") || lower.contains("find") || lower.contains("nearby") ||
                              lower.contains("near me") || lower.contains("show") || lower.contains("search") ||
                              lower.contains("view") || lower.contains("locate") || lower.contains("খুঁজুন") ||
                              lower.contains("খুঁজ") || lower.contains("দেখুন") || lower.contains("खोजें") ||
                              lower.contains("पास") || lower.contains("कास") || lower.contains("কাছে")
        if (hasHospital && hasSearchOrNeed) return true
        if (lower.contains("icu") || lower.contains("ventilator") || lower.contains("আইসিইউ") || lower.contains("आईसीयू")) return true
        if (lower.contains("govt hospital") || lower.contains("government hospital") || lower.contains("nearest hospital")) return true

        // Explicit Doctor consultation / Booking request
        val hasDoctor = lower.contains("doctor") || lower.contains("ডাক্তার") || lower.contains("डॉक्टर")
        val hasDocAction = lower.contains("appointment") || lower.contains("consult") || lower.contains("book") ||
                           lower.contains("need") || lower.contains("find") || lower.contains("see a doctor") ||
                           lower.contains("অ্যাপয়েন্টমেন্ট") || lower.contains("পরামর্শ") || lower.contains("বুক") ||
                           lower.contains("अपॉइंटमेंट") || lower.contains("परामर्श")
        if (hasDoctor && hasDocAction) return true

        // Explicit Symptom Triage tool activation
        if (lower.contains("check symptoms") || lower.contains("check my symptoms") || lower.contains("start triage") ||
            lower.contains("symptom triage") || lower.contains("triage assessment") || lower.contains("digital triage") ||
            lower.contains("লক্ষণ পরীক্ষা") || lower.contains("লক্ষণ মূল্যায়ন") || lower.contains("लक्षण मूल्यांकन")) {
            return true
        }

        // Explicit Reports / Prescriptions / Referrals / Medicine Catalog search
        if (lower.contains("view report") || lower.contains("upload report") || lower.contains("medical report") ||
            lower.contains("my prescription") || lower.contains("view prescription") || lower.contains("track referral") ||
            lower.contains("live queue") || lower.contains("opd token") || lower.contains("pharmacy stock") ||
            lower.contains("search medicine") || lower.contains("ঔষধ প্রাপ্যতা") || lower.contains("दवा उपलब्धता")) {
            return true
        }

        return false
    }

    /**
     * Processes deterministic CarePath feature actions.
     */
    fun processDirectFeatureQuery(userQuery: String, userLang: String = "EN"): AIResponse {
        val lower = userQuery.lowercase().trim()
        val lang = userLang.uppercase()

        return when {
            // Emergency & Ambulance
            lower.contains("ambulance") || lower.contains("ambulence") || lower.contains("গাড়ি") || lower.contains("एम्बुलेंस") || lower.contains("অ্যাম্বুলেন্স") -> {
                val reply = when (lang) {
                    "HI" -> "मैं तुरंत आपके वर्तमान स्थान पर आपातकालीन एम्बुलेंस भेजने और समन्वय करने में आपकी सहायता कर सकता हूँ।"
                    "BN" -> "আমি অবিলম্বে আপনার বর্তমান অবস্থানে জরুরি অ্যাম্বুলেন্স পাঠানো ও সমন্বয় করতে আপনাকে সাহায্য করতে পারি।"
                    else -> "I can immediately help you coordinate and request an emergency ambulance dispatched to your current location."
                }
                val label = when (lang) {
                    "HI" -> "अब एम्बुलेंस का अनुरोध करें"
                    "BN" -> "এখনই অ্যাম্বুলেন্স ডাকুন"
                    else -> "Request Ambulance Now"
                }
                AIResponse(replyText = reply, actionType = "NAV_AMBULANCE", actionLabel = label, actionPayload = "open_ambulance")
            }

            lower.contains("emergency") || lower.contains("জরুরি") || lower.contains("आपातकाल") || lower.contains("sos") -> {
                val reply = when (lang) {
                    "HI" -> "⚠️ आपातकालीन सहायता सक्रिय की जा रही है। कृपया तुरंत आपातकालीन केंद्र से संपर्क करें।"
                    "BN" -> "⚠️ জরুরি চিকিৎসা সহায়তা সক্রিয় করা হচ্ছে। অনুগ্রহ করে অবিলম্বে জরুরি কেন্দ্র খুলুন।"
                    else -> "⚠️ Accessing Emergency Assistance. Please open the CarePath Emergency Center immediately for instant response and 108 dispatch."
                }
                val label = when (lang) {
                    "HI" -> "आपातकालीन केंद्र खोलें (एसओएस)"
                    "BN" -> "জরুরি কেন্দ্র খুলুন (এসওএস)"
                    else -> "Open Emergency Center (SOS)"
                }
                AIResponse(replyText = reply, actionType = "NAV_EMERGENCY", actionLabel = label, actionPayload = "open_emergency")
            }

            // ICU & Beds
            lower.contains("icu") || lower.contains("ventilator") || lower.contains("critical care") || lower.contains("आईसीयू") || lower.contains("আইসিইউ") -> {
                val reply = when (lang) {
                    "HI" -> "सोनारपुर, नरेंद्रपुर और कोलकाता में लाइव उपलब्ध आईसीयू और क्रिटिकल केयर बेड वाले निकटवर्ती अस्पताल यहाँ दिए गए हैं।"
                    "BN" -> "সোনারপুর, নরেন্দ্রপুর ও কলকাতায় লাইভ উপলব্ধ আইসিইউ এবং ক্রিটিকাল কেয়ার বেড থাকা কাছের হাসপাতালগুলো এখানে দেওয়া হলো।"
                    else -> "Here are the nearby hospitals with live available ICU and Critical Care beds in Sonarpur, Narendrapur, and Kolkata."
                }
                val label = when (lang) {
                    "HI" -> "आईसीयू बेड वाले अस्पताल देखें"
                    "BN" -> "আইসিইউ বেড থাকা হাসপাতাল দেখুন"
                    else -> "Show Hospitals with ICU Beds"
                }
                AIResponse(replyText = reply, actionType = "NAV_HOSPITALS_ICU", actionLabel = label, actionPayload = "filter_icu")
            }

            // Government Hospitals
            lower.contains("government") || lower.contains("govt") || lower.contains("सरकारी") || lower.contains("সরকারি") -> {
                val reply = when (lang) {
                    "HI" -> "सत्यापित सरकारी अस्पताल और स्वास्थ्य केंद्र दिखाए जा रहे हैं जो निःशुल्क सार्वजनिक स्वास्थ्य सेवाएं और 24x7 आपातकालीन देखभाल प्रदान करते हैं।"
                    "BN" -> "যাচাইকৃত সরকারি হাসপাতাল ও স্বাস্থ্যকেন্দ্রগুলো দেখানো হচ্ছে যা বিনামূল্যে সরকারি স্বাস্থ্যসেবা এবং ২৪x৭ জরুরি চিকিৎসা প্রদান করে।"
                    else -> "Displaying verified Government hospitals and rural health centres offering free public health services and 24x7 emergency care."
                }
                val label = when (lang) {
                    "HI" -> "सरकारी अस्पताल दिखाएं"
                    "BN" -> "সরকারি হাসপাতাল দেখান"
                    else -> "Show Government Hospitals"
                }
                AIResponse(replyText = reply, actionType = "NAV_HOSPITALS_GOVT", actionLabel = label, actionPayload = "filter_govt")
            }

            // Nearest Hospital & Navigation
            lower.contains("hospital") || lower.contains("near me") || lower.contains("directions") ||
            lower.contains("কাছের হাসপাতাল") || lower.contains("पास का अस्पताल") -> {
                val reply = when (lang) {
                    "HI" -> "मैंने वास्तविक समय की जीपीएस दूरी द्वारा निकटतम सत्यापित चिकित्सा सुविधाओं की पहचान की है।"
                    "BN" -> "আমি রিয়েল-টাইম জিপিএস দূরত্বের ভিত্তিতে নিকটতম যাচাইকৃত চিকিৎসা কেন্দ্রগুলো চিহ্নিত করেছি।"
                    else -> "I have identified the nearest verified medical facilities ranked dynamically by real-time GPS distance."
                }
                val label = when (lang) {
                    "HI" -> "निकटतम अस्पताल खोजें"
                    "BN" -> "কাছের হাসপাতাল খুঁজুন"
                    else -> "Find Nearest Hospitals"
                }
                AIResponse(replyText = reply, actionType = "NAV_HOSPITALS_NEAREST", actionLabel = label, actionPayload = "filter_distance")
            }

            // Doctor & Appointments
            lower.contains("doctor") || lower.contains("appointment") || lower.contains("consult") || lower.contains("teleconsult") || lower.contains("ডাক্তার") || lower.contains("डॉक्टर") -> {
                val reply = when (lang) {
                    "HI" -> "आप विभिन्न विशेषज्ञताओं (कार्डियोलॉजी, न्यूरोलॉजी, सामान्य चिकित्सा, आर्थोपेडिक्स) में डॉक्टरों को खोज सकते हैं और ओपीडी या टेली-परामर्श बुक कर सकते हैं।"
                    "BN" -> "আপনি বিভিন্ন বিশেষজ্ঞতার (কার্ডিওলজি, নিউরোলজি, সাধারণ মেডিসিন, অর্থোপেডিক্স) ডাক্তারদের খুঁজে ওপিডি বা টেলি-পরামর্শ বুক করতে পারেন।"
                    else -> "You can search verified doctors across specializations (Cardiology, Neurology, Medicine, Orthopedics) and book OPD or teleconsultation slots."
                }
                val label = when (lang) {
                    "HI" -> "डॉक्टर अपॉइंटमेंट बुक करें"
                    "BN" -> "ডাক্তার অ্যাপয়েন্টমেন্ট বুক করুন"
                    else -> "Book Doctor Appointment"
                }
                AIResponse(replyText = reply, actionType = "NAV_DOCTORS", actionLabel = label, actionPayload = "open_doctors")
            }

            // Report Analysis
            lower.contains("report") || lower.contains("blood test") || lower.contains("cbc") || lower.contains("x-ray") || lower.contains("রিপোর্ট") || lower.contains("रिपोर्ट") -> {
                val reply = when (lang) {
                    "HI" -> "अपनी मेडिकल रिपोर्ट (पीडीएफ या छवि) अपलोड करें ताकि सामान्य संदर्भ श्रेणियों और मापदंडों का स्पष्टीकरण प्राप्त हो सके। (नोट: एआई स्पष्टीकरण डॉक्टर के निदान का विकल्प नहीं है)।"
                    "BN" -> "আপনার মেডিকেল রিপোর্ট (পিডিএফ বা ছবি) আপলোড করুন যাতে পরিভাষা ও স্বাভাবিক রেফারেন্স সীমার শিক্ষামূলক ব্যাখ্যা পাওয়া যায়। (উল্লেখ্য: এআই ব্যাখ্যা চিকিৎসকের বিকল্প নয়)।"
                    else -> "Upload your medical report (PDF or Image) to receive an educational explanation of terminology, normal reference ranges, and parameters. (Note: AI explanations do not replace a physician's diagnosis)."
                }
                val label = when (lang) {
                    "HI" -> "मेडिकल रिपोर्ट देखें / अपलोड करें"
                    "BN" -> "মেডিকেল রিপোর্ট দেখুন / আপলোড করুন"
                    else -> "View / Upload Medical Reports"
                }
                AIResponse(replyText = reply, actionType = "NAV_REPORTS", actionLabel = label, actionPayload = "open_reports")
            }

            // Prescriptions
            lower.contains("prescription") || lower.contains("rx") || lower.contains("প্রেসক্রিপশন") || lower.contains("नुस्खा") -> {
                val reply = when (lang) {
                    "HI" -> "दवाइयों के समय, खुराक के दिशानिर्देश और उपयोग निर्देशों के लिए अपने नुस्खे अपलोड करें या देखें।"
                    "BN" -> "ওষুধের সময়সূচি, ডোজ নির্দেশিকা এবং সাধারণ ব্যবহারের নির্দেশনার জন্য আপনার প্রেসক্রিপশন আপলোড করুন বা দেখুন।"
                    else -> "Upload or view your prescriptions for structured medication schedules, dosage guidelines, and general usage instructions."
                }
                val label = when (lang) {
                    "HI" -> "नुस्खे खोलें"
                    "BN" -> "প্রেসক্রিপশন খুলুন"
                    else -> "Open Prescriptions"
                }
                AIResponse(replyText = reply, actionType = "NAV_PRESCRIPTIONS", actionLabel = label, actionPayload = "open_prescriptions")
            }

            // Pharmacy Stock & Medicines
            lower.contains("pharmacy") || lower.contains("medicine") || lower.contains("tablet") || lower.contains("ঔষধ") || lower.contains("দवा") || lower.contains("ওষুধ") -> {
                val reply = when (lang) {
                    "HI" -> "जेनेरिक दवाओं के नाम, खुराक दिशानिर्देश और अस्पताल फार्मेसियों में उपलब्ध स्टॉक खोजें।"
                    "BN" -> "জেনেরিক ওষুধের নাম, ব্যবহারের নিয়মাবলী এবং হাসপাতাল ফার্মেসিতে উপলব্ধ স্টক সন্ধান করুন।"
                    else -> "Search generic medicine names, usage guidelines, standard dosages, and verified stock availability across hospital pharmacies."
                }
                val label = when (lang) {
                    "HI" -> "दवा उपलब्धता खोजें"
                    "BN" -> "ওষুধ প্রাপ্যতা সন্ধান করুন"
                    else -> "Search Medicine Availability"
                }
                AIResponse(replyText = reply, actionType = "NAV_MEDICINES", actionLabel = label, actionPayload = "open_medicines")
            }

            // Referrals & Queue
            lower.contains("referral") || lower.contains("queue") || lower.contains("token") || lower.contains("wait") || lower.contains("টোকেন") || lower.contains("कतार") -> {
                val reply = when (lang) {
                    "HI" -> "अपने रेफरल की स्थिति और लाइव ओपीडी कतार टोकन प्रतीक्षा समय को रीयल-टाइम में ट्रैक करें।"
                    "BN" -> "আপনার রেফারেল স্ট্যাটাস এবং লাইভ ওপিডি টোকেন অপেক্ষার সময় রিয়েল-টাইমে ট্র্যাক করুন।"
                    else -> "Track your inter-facility clinical referral status and live OPD queue token wait time in real-time."
                }
                val label = when (lang) {
                    "HI" -> "रेफरल और लाइव कतार ट्रैक करें"
                    "BN" -> "রেফারেল ও লাইভ লাইন ট্র্যাক করুন"
                    else -> "Track Referrals & Live Queue"
                }
                AIResponse(replyText = reply, actionType = "NAV_REFERRALS", actionLabel = label, actionPayload = "open_referrals")
            }

            // Symptom Triage tool (only when symptoms/triage explicitly requested)
            lower.contains("symptom") || lower.contains("triage") || lower.contains("লক্ষণ") || lower.contains("लक्षण") -> {
                val reply = when (lang) {
                    "HI" -> "आइए आपके लक्षणों और गंभीरता (1-10) का मूल्यांकन करने के लिए डिजिटल ट्राइएज मूल्यांकन शुरू करें।"
                    "BN" -> "আসুন আপনার লক্ষণ ও তীব্রতা (১-১০) মূল্যায়ন করতে ডিজিটাল ট্রায়াজ মূল্যায়ন শুরু করি।"
                    else -> "Let's perform a digital triage assessment to evaluate your symptoms, severity (1-10), and determine the recommended level of care."
                }
                val label = when (lang) {
                    "HI" -> "लक्षण मूल्यांकन शुरू करें"
                    "BN" -> "লক্ষণ মূল্যায়ন শুরু করুন"
                    else -> "Start Symptom Assessment"
                }
                AIResponse(replyText = reply, actionType = "NAV_SYMPTOMS", actionLabel = label, actionPayload = "start_triage")
            }

            // General or unknown query: NO forced action
            else -> {
                AIResponse(replyText = "", actionType = null, actionLabel = null, actionPayload = null)
            }
        }
    }

    /**
     * Checks if the text indicates an emergency requiring immediate attention.
     */
    fun hasEmergencySymptoms(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("chest pain") || lower.contains("heart attack") ||
               lower.contains("stroke") || lower.contains("difficulty breathing") ||
               lower.contains("unconscious") || lower.contains("severe bleeding") ||
               (lower.contains("বুক") && lower.contains("ব্যথা")) || lower.contains("শ্বাসকষ্ট") ||
               (lower.contains("छाती") && lower.contains("दर्द")) || lower.contains("बेहोश")
    }

    /**
     * Fallback response if the LLM backend cannot be reached.
     */
    fun getFallbackMedicalResponse(userLang: String = "EN", hasEmergency: Boolean = false): AIResponse {
        val lang = userLang.uppercase()
        if (hasEmergency) {
            val reply = when (lang) {
                "HI" -> "⚠️ आपातकालीन सूचना: आपके द्वारा वर्णित लक्षण तत्काल चिकित्सा ध्यान देने योग्य हो सकते हैं। कृपया तुरंत आपातकालीन सेवा से संपर्क करें।"
                "BN" -> "⚠️ জরুরি সতর্কতা: আপনার বর্ণিত লক্ষণগুলোর জন্য দ্রুত চিকিৎসা প্রয়োজন হতে পারে। অবিলম্বে জরুরি সেবা গ্রহণ করুন।"
                else -> "⚠️ CRITICAL MEDICAL NOTICE: The symptoms described may indicate an acute emergency. Please seek immediate medical care or use CarePath Emergency SOS right away."
            }
            val label = when (lang) {
                "HI" -> "आपातकालीन केंद्र खोलें (एसओएस)"
                "BN" -> "জরুরি কেন্দ্র খুলুন (এসওএস)"
                else -> "Open Emergency Center (SOS)"
            }
            return AIResponse(replyText = reply, actionType = "NAV_EMERGENCY", actionLabel = label, actionPayload = "open_emergency")
        }

        val reply = when (lang) {
            "HI" -> "CarePath AI स्वास्थ्य सहायक उपलब्ध है। मैं आपके स्वास्थ्य संबंधी सवालों के जवाब देने, लक्षण समझने या आपातकालीन सहायता प्राप्त करने में आपकी मदद कर सकता हूँ।"
            "BN" -> "কেয়ারপাথ এআই স্বাস্থ্য সহকারী সক্রিয় রয়েছে। আমি আপনার স্বাস্থ্য সম্পর্কিত প্রশ্নের উত্তর দিতে, লক্ষণ বুঝতে বা জরুরি সহায়তা প্রদানে সাহায্য করতে পারি।"
            else -> "CarePath Medical AI is here to help. Feel free to describe your symptoms or ask healthcare-related questions. For serious concerns, please consult a healthcare professional."
        }
        // General conversational queries have NO ACTION BUTTON
        return AIResponse(replyText = reply, actionType = null, actionLabel = null, actionPayload = null)
    }
}
