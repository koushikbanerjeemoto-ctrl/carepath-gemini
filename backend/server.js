const http = require('http');
const fs = require('fs');
const path = require('path');
const { GoogleGenAI } = require('@google/genai');

// Load .env files without requiring an additional dotenv dependency.
function loadEnv() {
  const envPaths = [
    path.join(__dirname, '.env'),
    path.join(__dirname, '..', '.env'),
    path.join(process.cwd(), '.env'),
    path.join(process.cwd(), 'backend', '.env'),
    '/app/applet/.env',
    '/app/applet/backend/.env',
    '/app/.dev.env.json'
  ];

  for (const filePath of envPaths) {
    if (!fs.existsSync(filePath)) continue;
    try {
      if (filePath.endsWith('.json')) {
        const data = JSON.parse(fs.readFileSync(filePath, 'utf8'));
        for (const [key, value] of Object.entries(data)) {
          if (key && !process.env[key]) process.env[key] = String(value);
        }
        continue;
      }

      const content = fs.readFileSync(filePath, 'utf8');
      for (const line of content.split(/\r?\n/)) {
        const trimmed = line.trim();
        if (!trimmed || trimmed.startsWith('#')) continue;
        const eqIdx = trimmed.indexOf('=');
        if (eqIdx === -1) continue;
        const key = trimmed.substring(0, eqIdx).trim();
        let value = trimmed.substring(eqIdx + 1).trim();
        if ((value.startsWith('"') && value.endsWith('"')) ||
            (value.startsWith("'") && value.endsWith("'"))) {
          value = value.substring(1, value.length - 1);
        }
        if (key && !process.env[key]) process.env[key] = value;
      }
    } catch (error) {
      console.warn(`[ENV] Could not read ${filePath}: ${error.message}`);
    }
  }
}

loadEnv();

const PORT = parseInt(process.env.PORT || process.env.APP_PORT || '3000', 10);
const DEFAULT_MODEL = 'gemini-3.8-flash';

function getGeminiApiKey() {
  loadEnv();
  return (process.env.GEMINI_API_KEY || '').trim();
}

function getGeminiModel() {
  loadEnv();
  return (process.env.GEMINI_MODEL || DEFAULT_MODEL).trim();
}

const SYSTEM_INSTRUCTION_BASE = `You are CarePath AI, a friendly, empathetic, and knowledgeable healthcare assistant inside the CarePath healthcare application.

CORE BEHAVIOR & STYLE:
1. Warm & Conversational:
   - When greeted (e.g. "Hello", "Hi", "Good morning", "নমস্কার", "नमस्ते"), reply warmly and conversationally.
   - When asked "What can you help me with?" or about CarePath capabilities, explain clearly that you can help explore CarePath (finding hospitals, ICU beds, emergency ambulance, booking doctors, pharmacy medicine stock, medical reports, and symptom assessments), understand health symptoms, access emergency assistance, and answer general healthcare questions.
2. Natural Healthcare Explanations:
   - When asked a general healthcare question, provide a clear, informative, conversational explanation. Explain common causes, practical self-care steps, and when to seek medical evaluation.
3. Medical Safety & Boundaries:
   - You are a supportive healthcare assistant, NOT a licensed doctor. Never present yourself as a physician or offer a definitive clinical diagnosis.
   - Never claim certainty about medical conditions based on a chat.
   - Do NOT prescribe dangerous, controlled, or prescription-only medications with specific dosages.
   - For potentially serious symptoms, guide the user toward the existing CarePath symptom assessment or a qualified doctor.
   - For medical emergencies (such as acute chest pain, difficulty breathing, severe bleeding, or unconsciousness), urgently advise immediate professional emergency care and using CarePath Emergency SOS.
4. CarePath Features:
   - If the user asks to use CarePath features (such as finding nearby hospitals, booking doctors, calling an ambulance, checking ICU beds, or viewing prescriptions), acknowledge their request and allow the built-in CarePath action buttons to assist them.
5. Conversation Context:
   - Maintain the immediate conversation context naturally across turns.
6. Multilingual Fluency:
   - Respond in the language used by the user: English, Hindi, or Bengali. If the user mixes languages naturally, respond in their dominant language.`;

function detectLanguage(userText, requestedLang) {
  if (userText) {
    if (/[\u0980-\u09FF]/.test(userText)) return 'BN';
    if (/[\u0900-\u097F]/.test(userText)) return 'HI';
  }
  const code = (requestedLang || 'EN').toUpperCase();
  return code === 'BN' || code === 'HI' ? code : 'EN';
}

function getLanguagePrompt(lang) {
  if (lang === 'BN') {
    return '\n\nLANGUAGE INSTRUCTION: The user is communicating in Bengali. Respond naturally, warmly, and completely in Bengali (বাংলা).';
  }
  if (lang === 'HI') {
    return '\n\nLANGUAGE INSTRUCTION: The user is communicating in Hindi. Respond naturally, warmly, and completely in Hindi (हिन्दी).';
  }
  return '\n\nLANGUAGE INSTRUCTION: The user is communicating in English. Respond in clear, empathetic, and natural English.';
}

function detectEmergencyAction(text, lang) {
  if (!text) return null;
  const lower = text.toLowerCase();
  const emergencyKeywords = [
    'chest pain', 'heart attack', 'stroke', 'difficulty breathing',
    'severe bleeding', 'unconscious', 'anaphylaxis', 'seizure',
    'severe burn', 'head trauma', 'choking', 'poisoning',
    'বুক ব্যথা', 'বুকে ব্যথা', 'বুকে প্রচণ্ড ব্যথা', 'স্ট্রোক', 'শ্বাসকষ্ট', 'শ্বাস নিতে কষ্ট', 'অজ্ঞান',
    'छाती में दर्द', 'छाती में तेज दर्द', 'दौरा', 'बेहोश', 'बेहोशी', 'सांस लेने में तकलीफ'
  ];

  if (!emergencyKeywords.some(keyword => lower.includes(keyword))) return null;

  const label = lang === 'BN'
    ? 'জরুরি কেন্দ্র খুলুন (এসওএস)'
    : lang === 'HI'
      ? 'आपातकालीन केंद्र खोलें (एसओएस)'
      : 'Open Emergency Center (SOS)';

  return {
    actionType: 'NAV_EMERGENCY',
    actionLabel: label,
    actionPayload: 'open_emergency'
  };
}

function detectExplicitFeatureAction(text, lang) {
  if (!text) return null;
  const lower = text.toLowerCase().trim();

  if (lower.includes('check my symptoms') || lower.includes('check symptoms') ||
      lower.includes('start triage') || lower.includes('symptom assessment') ||
      lower.includes('লক্ষণ পরীক্ষা') || lower.includes('লক্ষণ মূল্যায়ন') || lower.includes('लक्षण मूल्यांकन')) {
    const label = lang === 'BN' ? 'লক্ষণ মূল্যায়ন শুরু করুন' : lang === 'HI' ? 'लक्षण मूल्यांकन शुरू करें' : 'Check Symptoms';
    return { actionType: 'NAV_SYMPTOMS', actionLabel: label, actionPayload: 'start_triage' };
  }

  if (lower.includes('find hospitals near me') || lower.includes('nearby hospitals') ||
      lower.includes('show me nearby hospitals') || lower.includes('hospital near me') ||
      lower.includes('hospitals near me') || lower.includes('अस्पताल खोजें') || lower.includes('कাছের হাসপাতাল')) {
    const label = lang === 'BN' ? 'কাছের হাসপাতাল খুঁজুন' : lang === 'HI' ? 'निकटतम अस्पताल खोजें' : 'Find Nearest Facilities';
    return { actionType: 'NAV_HOSPITALS_NEAREST', actionLabel: label, actionPayload: 'filter_distance' };
  }

  if (lower.includes('emergency help') || lower.includes('i need emergency') ||
      lower.includes('জরুরি সাহায্য') || lower.includes('आपातकालीन मदद')) {
    const label = lang === 'BN' ? 'জরুরি কেন্দ্র খুলুন (এসওএস)' : lang === 'HI' ? 'आपातकालीन केंद्र खोलें (एसओएस)' : 'Open Emergency Center (SOS)';
    return { actionType: 'NAV_EMERGENCY', actionLabel: label, actionPayload: 'open_emergency' };
  }

  return null;
}

function normalizeHistory(messages) {
  return messages
    .slice(-10)
    .filter(message => message && message.role && message.content)
    .map(message => ({
      role: message.role === 'user' ? 'user' : 'model',
      parts: [{ text: String(message.content) }]
    }));
}

async function generateGeminiReply(messages, language) {
  const apiKey = getGeminiApiKey();
  if (!apiKey) throw new Error('GEMINI_API_KEY is not configured');

  const ai = new GoogleGenAI({ apiKey });
  const history = normalizeHistory(messages);

  // The last user message is sent separately; previous turns become chat history.
  const lastMessage = history.pop();
  if (!lastMessage || lastMessage.role !== 'user') {
    throw new Error('The final chat message must be from the user');
  }

  const chat = ai.chats.create({
    model: getGeminiModel(),
    history,
    config: {
      systemInstruction: SYSTEM_INSTRUCTION_BASE + getLanguagePrompt(language),
      temperature: 0.4,
      maxOutputTokens: 800
    }
  });

  const response = await chat.sendMessage({
    message: lastMessage.parts[0].text
  });

  const reply = typeof response.text === 'string' ? response.text.trim() : '';
  if (!reply) throw new Error('Gemini returned an empty response');
  return reply;
}

function localFallback(lastUserMsg, activeLang, emergencyAction) {
  const lower = lastUserMsg.toLowerCase().trim();

  if (emergencyAction) {
    return activeLang === 'BN'
      ? '⚠️ জরুরি সতর্কতা: আপনার বর্ণিত লক্ষণগুলো গুরুতর চিকিৎসা সমস্যার লক্ষণ হতে পারে। অবিলম্বে জরুরি চিকিৎসা সাহায্য গ্রহণ করুন অথবা কেয়ারপাথ জরুরি এসওএস সক্রিয় করুন।'
      : activeLang === 'HI'
        ? '⚠️ आपातकालीन चेतावनी: आपके द्वारा वर्णित लक्षण गंभीर स्थिति का संकेत हो सकते हैं। कृपया तुरंत आपातकालीन चिकित्सा सहायता प्राप्त करें या केयरपाथ आपातकालीन सेवा (SOS) का उपयोग करें।'
        : '⚠️ CRITICAL MEDICAL ALERT: The symptoms you described require urgent professional evaluation. Please seek immediate emergency medical care or use CarePath Emergency SOS right away.';
  }

  if (lower.includes('hello') || lower.includes('hi') || lower.includes('hey') || lower.includes('নমস্কার') || lower.includes('হ্যালো') || lower.includes('नमस्ते')) {
    return activeLang === 'BN'
      ? 'হ্যালো! আজ আপনাকে কীভাবে সাহায্য করতে পারি?'
      : activeLang === 'HI'
        ? 'नमस्ते! आज मैं आपकी कैसे मदद कर सकता हूँ?'
        : 'Hello! How can I help you today?';
  }

  return activeLang === 'BN'
    ? 'দুঃখিত, এই মুহূর্তে কেয়ারপাথ এআই সার্ভিসে সংযোগ করতে পারছি না। অনুগ্রহ করে আবার চেষ্টা করুন।'
    : activeLang === 'HI'
      ? 'क्षमा करें, मैं अभी केयरपाथ एआई सेवा से कनेक्ट नहीं हो सका। कृपया पुनः प्रयास करें।'
      : 'Sorry, I could not connect to the CarePath AI service right now. Please try again.';
}

function sendJson(res, statusCode, payload) {
  res.writeHead(statusCode, { 'Content-Type': 'application/json; charset=utf-8' });
  res.end(JSON.stringify(payload));
}

const server = http.createServer(async (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  if (req.method === 'GET' && (req.url === '/' || req.url === '/api/health' || req.url === '/health')) {
    sendJson(res, 200, {
      status: 'ok',
      service: 'CarePath Medical AI Backend',
      provider: 'Google Gemini',
      model: getGeminiModel(),
      geminiConfigured: !!getGeminiApiKey(),
      timestamp: new Date().toISOString()
    });
    return;
  }

  if (req.method === 'POST' && (req.url === '/api/chat' || req.url === '/chat')) {
    let body = '';
    req.on('data', chunk => {
      body += chunk;
      if (body.length > 1e6) req.destroy();
    });

    req.on('end', async () => {
      let userMessages = [];
      let activeLang = 'EN';

      try {
        const payload = JSON.parse(body || '{}');
        userMessages = payload.messages || [];
        const requestedLang = payload.language || 'EN';

        if (!Array.isArray(userMessages) || userMessages.length === 0) {
          sendJson(res, 400, { error: 'messages array is required' });
          return;
        }

        const lastUserMsg = String(userMessages[userMessages.length - 1]?.content || '');
        activeLang = detectLanguage(lastUserMsg, requestedLang);
        const emergencyAction = detectEmergencyAction(lastUserMsg, activeLang);

        console.log(`[CHAT] Gemini request: "${lastUserMsg.substring(0, 120)}"`);
        console.log(`[CHAT] GEMINI_API_KEY present: ${!!getGeminiApiKey()}`);
        console.log(`[CHAT] Model: ${getGeminiModel()}`);

        let assistantReply = '';
        try {
          assistantReply = await generateGeminiReply(userMessages, activeLang);
          console.log(`[CHAT] Gemini response: "${assistantReply.substring(0, 120)}..."`);
        } catch (geminiError) {
          console.error('[Gemini API] Request failed:', geminiError.message);
          assistantReply = localFallback(lastUserMsg, activeLang, emergencyAction);
        }

        const explicitAction = emergencyAction || detectExplicitFeatureAction(lastUserMsg, activeLang);
        sendJson(res, 200, {
          reply: assistantReply,
          response: assistantReply,
          actionType: explicitAction ? explicitAction.actionType : null,
          actionLabel: explicitAction ? explicitAction.actionLabel : null,
          actionPayload: explicitAction ? explicitAction.actionPayload : null
        });
      } catch (error) {
        console.error('[CHAT] Endpoint error:', error);
        const lastUserMsg = String(userMessages[userMessages.length - 1]?.content || '');
        const explicitAction = detectEmergencyAction(lastUserMsg, activeLang) || detectExplicitFeatureAction(lastUserMsg, activeLang);
        const fallbackReply = explicitAction?.actionType === 'NAV_EMERGENCY'
          ? '⚠️ CRITICAL MEDICAL ALERT: Please seek immediate emergency medical care or use CarePath Emergency SOS.'
          : 'Sorry, I could not process your request right now. Please try again.';

        sendJson(res, 200, {
          reply: fallbackReply,
          response: fallbackReply,
          actionType: explicitAction ? explicitAction.actionType : null,
          actionLabel: explicitAction ? explicitAction.actionLabel : null,
          actionPayload: explicitAction ? explicitAction.actionPayload : null
        });
      }
    });
    return;
  }

  sendJson(res, 404, { error: 'Endpoint not found' });
});

(PORT, '0.0.0.0', () => {
  console.log(`CarePath Gemini Backend running on port ${PORT}`);
  conserver.listensole.log(`Model: ${getGeminiModel()}`);
  console.log(`Gemini API Key Configured: ${!!getGeminiApiKey()}`);
});
