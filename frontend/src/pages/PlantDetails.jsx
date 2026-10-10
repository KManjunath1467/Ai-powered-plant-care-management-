import React, { useEffect, useState, useContext } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import Sidebar from '../components/Sidebar'
import Loader from '../components/Loader'
import {
  plantsApi,
  wateringHistoryApi,
  sensorApi,
  weatherApi,
  groqApi
} from '../services/apiService'
import {
  ArrowLeft,
  Droplet,
  Sun,
  TrendingUp,
  Cpu,
  Layers,
  Sparkles,
  ShieldCheck,
  AlertTriangle,
  Upload,
  Calendar,
  Thermometer,
  Bot,
  Send,
  Zap,
  TestTube,
  CheckCircle2,
  RefreshCw,
  Search
} from 'lucide-react'

export default function PlantDetails() {
  const { id } = useParams()
  const navigate = useNavigate()

  const [plant, setPlant] = useState(null)
  const [wateringHistory, setWateringHistory] = useState([])
  const [sensorStatus, setSensorStatus] = useState(null)
  const [plantWeather, setPlantWeather] = useState(null)
  const [recommendations, setRecommendations] = useState([])
  const [healthSummary, setHealthSummary] = useState(null)
  const [diseaseHistory, setDiseaseHistory] = useState([])
  const [loading, setLoading] = useState(true)

  // Tab State
  const [activeTab, setActiveTab] = useState('overview') // overview, anatomy, sensors, recommendations, assistant, disease, history

  // Assistant In-Plant Chat
  const [chatMessages, setChatMessages] = useState([])
  const [chatInput, setChatInput] = useState('')
  const [chatLoading, setChatLoading] = useState(false)

  // Disease Form
  const [symptomInput, setSymptomInput] = useState('')
  const [diseaseNotes, setDiseaseNotes] = useState('')
  const [inspectingDisease, setInspectingDisease] = useState(false)

  // Image Upload
  const [uploadingImage, setUploadingImage] = useState(false)

  useEffect(() => {
    loadPlantData()
  }, [id])

  const loadPlantData = async () => {
    try {
      setLoading(true)
      const [pRes, hRes, sRes, wRes, rRes, sumRes, disRes] = await Promise.all([
        plantsApi.getPlant(id),
        wateringHistoryApi.getPlantHistory(id),
        sensorApi.getPlantSensorStatus(id).catch(() => ({ data: { hasSensor: false } })),
        weatherApi.getPlantWeather(id).catch(() => ({ data: null })),
        groqApi.getRecommendations(id).catch(() => ({ data: [] })),
        groqApi.getHealthSummary(id).catch(() => ({ data: null })),
        groqApi.getDiseaseHistory(id).catch(() => ({ data: [] }))
      ])

      setPlant(pRes.data)
      setWateringHistory(hRes.data || [])
      setSensorStatus(sRes.data)
      setPlantWeather(wRes.data)
      setRecommendations(rRes.data || [])
      setHealthSummary(sumRes.data)
      setDiseaseHistory(disRes.data || [])

      setChatMessages([
        {
          role: 'assistant',
          text: `Hi! I'm your plant care assistant for ${pRes.data.name}. Ask me about watering, soil moisture readings, or health observations!`
        }
      ])
    } catch (err) {
      console.error('Failed to load plant data:', err)
    } finally {
      setLoading(false)
    }
  }

  const handleWater = async () => {
    try {
      await plantsApi.markWatered(id)
      await wateringHistoryApi.record(id, { notes: 'Watered via plant dashboard', onTime: true })
      loadPlantData()
    } catch (err) {
      console.error('Watering record error:', err)
    }
  }

  const handleImageUpload = async (e) => {
    const file = e.target.files[0]
    if (!file) return
    const formData = new FormData()
    formData.append('image', file)
    try {
      setUploadingImage(true)
      await plantsApi.uploadImage(id, formData)
      loadPlantData()
    } catch (err) {
      alert('Upload failed: ' + (err.response?.data?.message || err.message))
    } finally {
      setUploadingImage(false)
    }
  }

  const handleSendAssistant = async (e) => {
    e.preventDefault()
    if (!chatInput.trim()) return
    const q = chatInput.trim()
    setChatMessages((prev) => [...prev, { role: 'user', text: q }])
    setChatInput('')

    try {
      setChatLoading(true)
      const res = await groqApi.askAssistant(id, { question: q })
      setChatMessages((prev) => [
        ...prev,
        { role: 'assistant', text: res.data.answer, provider: res.data.provider }
      ])
    } catch (err) {
      setChatMessages((prev) => [
        ...prev,
        { role: 'assistant', text: 'Sorry, I encountered an issue contacting the botanical assistant.' }
      ])
    } finally {
      setChatLoading(false)
    }
  }

  const handleDiseaseInspect = async (e) => {
    e.preventDefault()
    if (!symptomInput.trim()) return
    try {
      setInspectingDisease(true)
      const res = await groqApi.inspectDisease(id, {
        symptoms: symptomInput.trim(),
        notes: diseaseNotes.trim()
      })
      setDiseaseHistory((prev) => [res.data, ...prev])
      setSymptomInput('')
      setDiseaseNotes('')
    } catch (err) {
      alert('Disease inspection failed.')
    } finally {
      setInspectingDisease(false)
    }
  }

  if (loading) return <Loader />

  if (!plant) {
    return (
      <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center p-6 text-center">
        <h2 className="text-xl font-bold text-gray-800">Plant Not Found</h2>
        <button onClick={() => navigate('/plants')} className="btn-primary mt-4">Back to My Plants</button>
      </div>
    )
  }

  let anatomyObj = null
  try {
    if (plant.anatomyJson) {
      anatomyObj = JSON.parse(plant.anatomyJson)
    }
  } catch (e) {}

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#F4F7F4] via-[#EBF2EB] to-[#DCEADC]/40">
      <Navbar />
      <div className="flex max-w-7xl mx-auto px-6">
        <Sidebar />
        <main className="flex-1 py-6 space-y-6">
          {/* Top Bar Navigation */}
          <div className="flex items-center justify-between">
            <button
              onClick={() => navigate('/plants')}
              className="p-2.5 rounded-2xl bg-white border border-gray-200 text-gray-600 hover:bg-gray-50 transition-colors flex items-center gap-2 text-xs font-semibold"
            >
              <ArrowLeft className="w-4 h-4" /> Back to My Plants
            </button>

            <button
              onClick={handleWater}
              className="btn-primary flex items-center gap-2 text-xs"
            >
              <Droplet className="w-4 h-4" /> Water Plant Now
            </button>
          </div>

          {/* Plant Hero Banner */}
          <div className="card p-8 bg-white shadow-md relative overflow-hidden">
            <div className="flex flex-col md:flex-row items-start md:items-center gap-6">
              <div className="relative group flex-shrink-0">
                <img
                  src={plant.imageUrl || 'https://images.unsplash.com/photo-1545241047-6083a3684587?w=500&auto=format&fit=crop&q=60'}
                  alt={plant.name}
                  className="w-32 h-32 md:w-36 md:h-36 object-cover rounded-3xl border-4 border-emerald-100 shadow-md"
                />
                <label className="absolute inset-0 bg-black/40 rounded-3xl flex items-center justify-center text-white opacity-0 group-hover:opacity-100 cursor-pointer transition-opacity text-xs font-medium">
                  <Upload className="w-4 h-4 mr-1" />
                  {uploadingImage ? '...' : 'Upload'}
                  <input type="file" accept="image/*" onChange={handleImageUpload} className="hidden" />
                </label>
              </div>

              <div className="space-y-2 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="px-3 py-1 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800">
                    {plant.plantType}
                  </span>
                  <span className={`px-3 py-1 rounded-full text-xs font-bold ${
                    plant.health === 'Healthy' ? 'bg-teal-50 text-teal-700' : 'bg-amber-50 text-amber-700'
                  }`}>
                    {plant.health}
                  </span>
                  <span className="px-3 py-1 rounded-full text-xs font-semibold bg-gray-100 text-gray-700">
                    {plant.isIndoor ? 'Indoor' : 'Outdoor'} &bull; {plant.location}
                  </span>
                </div>

                <h1 className="text-3xl font-extrabold text-gray-900 tracking-tight">{plant.name}</h1>
                <p className="text-sm italic font-serif text-gray-500">
                  {plant.scientificName ? plant.scientificName : 'Plantae Botanical Specimen'}
                  {plant.plantFamily && ` • Family: ${plant.plantFamily}`}
                </p>

                {/* Groq Health Summary Banner */}
                {healthSummary?.summary && (
                  <div className="p-3.5 rounded-2xl bg-emerald-50/70 border border-emerald-100 text-xs text-emerald-950 flex items-start gap-2.5 mt-3">
                    <Sparkles className="w-4 h-4 text-emerald-600 flex-shrink-0 mt-0.5" />
                    <div>{healthSummary.summary}</div>
                  </div>
                )}
              </div>

              {/* Watering Streak Mini-Card */}
              <div className="flex-shrink-0 text-center bg-slate-50 p-4 rounded-2xl border border-slate-100 min-w-[130px]">
                <div className="text-xs uppercase font-bold text-gray-500 tracking-wider">Watering Streak</div>
                <div className="text-3xl font-black text-emerald-600 my-1">{plant.wateringStreak || 0}</div>
                <div className="text-[11px] text-gray-400">Consecutive cycles</div>
              </div>
            </div>
          </div>

          {/* Tab Navigation */}
          <div className="flex flex-wrap gap-2 border-b border-gray-200 pb-3">
            {[
              { id: 'overview', label: 'Overview & Care Rules' },
              { id: 'anatomy', label: 'Botanical Anatomy' },
              { id: 'sensors', label: 'IoT Sensor Rod Health' },
              { id: 'recommendations', label: 'AI Care Recommendations' },
              { id: 'assistant', label: 'AI Plant Assistant' },
              { id: 'disease', label: 'Disease Observation' },
              { id: 'history', label: 'Watering History' }
            ].map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`px-4 py-2.5 rounded-2xl text-xs font-bold transition-all ${
                  activeTab === tab.id
                    ? 'bg-emerald-600 text-white shadow-md'
                    : 'bg-white text-gray-700 border border-gray-200 hover:bg-gray-50'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {/* TAB 1: OVERVIEW & CARE RULES */}
          {activeTab === 'overview' && (
            <div className="space-y-6 animate-fade-in">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className="card p-6 bg-white space-y-3">
                  <div className="flex items-center gap-2 text-xs font-bold text-emerald-700 uppercase tracking-wider">
                    <Droplet className="w-4 h-4 text-sky-500" /> Watering Schedule
                  </div>
                  <div className="text-lg font-bold text-gray-900">
                    {plant.wateringFrequency || `Every ${plant.customWateringInterval || 7} days`}
                  </div>
                  <div className="text-xs text-gray-500">
                    Last watered: {plant.lastWateredDate || 'Never recorded'}
                  </div>
                </div>

                <div className="card p-6 bg-white space-y-3">
                  <div className="flex items-center gap-2 text-xs font-bold text-emerald-700 uppercase tracking-wider">
                    <Sun className="w-4 h-4 text-amber-500" /> Sunlight Requirement
                  </div>
                  <div className="text-lg font-bold text-gray-900">{plant.sunlightNeeds || 'Bright indirect light'}</div>
                  <div className="text-xs text-gray-500">Difficulty: {plant.difficultyLevel || 'Beginner'}</div>
                </div>

                <div className="card p-6 bg-white space-y-3">
                  <div className="flex items-center gap-2 text-xs font-bold text-emerald-700 uppercase tracking-wider">
                    <Thermometer className="w-4 h-4 text-rose-500" /> Ideal Temperature
                  </div>
                  <div className="text-lg font-bold text-gray-900">{plant.temperatureRange || '18-24°C'}</div>
                  <div className="text-xs text-gray-500">Humidity: {plant.humidityPreference || 'Moderate'}</div>
                </div>
              </div>

              {plantWeather && (
                <div className="card p-6 bg-white space-y-3">
                  <h3 className="text-sm font-bold text-gray-800">Local Weather for Plant Location ({plant.location})</h3>
                  <div className="flex flex-wrap items-center gap-6 text-xs text-gray-700">
                    <span>Temp: <strong>{Math.round(plantWeather.temperature)}°C</strong></span>
                    <span>Humidity: <strong>{plantWeather.humidity}%</strong></span>
                    <span>Condition: <strong>{plantWeather.description}</strong></span>
                    <span>Rainfall: <strong>{plantWeather.rainfall || 0} mm</strong></span>
                    <span className="text-emerald-700 italic">{plantWeather.careTip}</span>
                  </div>
                </div>
              )}

              {plant.notes && (
                <div className="card p-6 bg-white space-y-2">
                  <h3 className="text-sm font-bold text-gray-800">Care Notes & Observations</h3>
                  <p className="text-xs text-gray-600 leading-relaxed whitespace-pre-line">{plant.notes}</p>
                </div>
              )}
            </div>
          )}

          {/* TAB 2: BOTANICAL ANATOMY */}
          {activeTab === 'anatomy' && (
            <div className="card p-8 bg-white space-y-6 animate-fade-in">
              <div className="flex items-center gap-3 border-b border-gray-100 pb-4">
                <Layers className="w-6 h-6 text-emerald-600" />
                <div>
                  <h2 className="text-lg font-bold text-gray-900">Botanical Anatomy Breakdown</h2>
                  <p className="text-xs text-gray-500">
                    Morphological structure for {plant.name} ({plant.scientificName || plant.plantType})
                  </p>
                </div>
              </div>

              {anatomyObj ? (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {Object.entries(anatomyObj).map(([key, val]) => {
                    if (typeof val === 'string') {
                      return (
                        <div key={key} className="p-4 rounded-2xl bg-slate-50 border border-slate-100 md:col-span-2">
                          <div className="text-xs font-bold uppercase tracking-wider text-emerald-700 mb-1">
                            {key.replace(/([A-Z])/g, ' $1')}
                          </div>
                          <div className="text-xs text-gray-700 leading-relaxed">{val}</div>
                        </div>
                      )
                    }
                    return (
                      <div key={key} className="p-4 rounded-2xl bg-slate-50 border border-slate-100 space-y-2">
                        <div className="text-xs font-bold uppercase tracking-wider text-emerald-700">
                          {key.replace(/([A-Z])/g, ' $1')}
                        </div>
                        {Object.entries(val).map(([subK, subV]) => (
                          <div key={subK} className="text-xs text-gray-700">
                            <span className="font-semibold text-gray-500 capitalize">{subK}: </span>
                            {subV}
                          </div>
                        ))}
                      </div>
                    )
                  })}
                </div>
              ) : (
                <div className="text-center py-8 text-xs text-gray-500 space-y-3">
                  <p>Detailed anatomical breakdown has not been cached yet for this plant profile.</p>
                  <button
                    onClick={() => navigate('/identify')}
                    className="btn-primary text-xs"
                  >
                    Run AI Identification & Anatomy Generation
                  </button>
                </div>
              )}
            </div>
          )}

          {/* TAB 3: IOT SENSOR ROD HEALTH */}
          {activeTab === 'sensors' && (
            <div className="space-y-6 animate-fade-in">
              {sensorStatus && !sensorStatus.hasSensor ? (
                /* MANDATORY EMPTY-STATE RULE: Strictly empty if unpaired */
                <div className="card p-12 text-center space-y-4 border-2 border-dashed border-gray-200 bg-white">
                  <div className="w-16 h-16 rounded-full bg-slate-100 text-gray-400 mx-auto flex items-center justify-center">
                    <Cpu className="w-8 h-8" />
                  </div>
                  <div className="max-w-md mx-auto space-y-2">
                    <h3 className="text-lg font-bold text-gray-800">Sensor Monitoring Inactive</h3>
                    <p className="text-sm text-gray-600">
                      Connect a compatible sensor rod to enable soil-based health monitoring for this plant.
                    </p>
                    <p className="text-xs text-gray-400">
                      Soil moisture, electrical conductivity (EC), and pH cannot be measured without physical probe rod telemetry.
                    </p>
                  </div>
                  <button
                    onClick={() => navigate('/health-monitoring')}
                    className="btn-primary text-xs"
                  >
                    Pair Sensor Rod in Health Monitoring
                  </button>
                </div>
              ) : sensorStatus && sensorStatus.latestReading ? (
                /* LIVE TELEMETRY */
                <div className="space-y-6">
                  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                    <div className="card p-5 bg-white space-y-1">
                      <div className="text-xs text-gray-500 font-semibold flex items-center gap-1.5">
                        <Droplet className="w-4 h-4 text-sky-500" /> Soil Moisture
                      </div>
                      <div className="text-3xl font-black text-gray-900">
                        {sensorStatus.latestReading.soilMoisture?.toFixed(1)}%
                      </div>
                      <div className="text-[11px] text-gray-400">
                        Status: {sensorStatus.latestReading.soilMoisture < 30 ? 'Low' : 'Satisfactory'}
                      </div>
                    </div>

                    <div className="card p-5 bg-white space-y-1">
                      <div className="text-xs text-gray-500 font-semibold flex items-center gap-1.5">
                        <Thermometer className="w-4 h-4 text-rose-500" /> Soil Temperature
                      </div>
                      <div className="text-3xl font-black text-gray-900">
                        {sensorStatus.latestReading.soilTemperature?.toFixed(1)}°C
                      </div>
                      <div className="text-[11px] text-gray-400">Substrate thermal level</div>
                    </div>

                    <div className="card p-5 bg-white space-y-1">
                      <div className="text-xs text-gray-500 font-semibold flex items-center gap-1.5">
                        <Zap className="w-4 h-4 text-amber-500" /> Soil EC
                      </div>
                      <div className="text-3xl font-black text-gray-900">
                        {sensorStatus.latestReading.soilEc?.toFixed(2)} mS/cm
                      </div>
                      <div className="text-[11px] text-gray-400">Electrical conductivity</div>
                    </div>

                    <div className="card p-5 bg-white space-y-1">
                      <div className="text-xs text-gray-500 font-semibold flex items-center gap-1.5">
                        <TestTube className="w-4 h-4 text-purple-500" /> Soil pH
                      </div>
                      <div className="text-3xl font-black text-gray-900">
                        {sensorStatus.latestReading.soilPh?.toFixed(1)}
                      </div>
                      <div className="text-[11px] text-gray-400">Acid-alkaline balance</div>
                    </div>
                  </div>

                  {sensorStatus.interpretation && (
                    <div className="card p-6 bg-white space-y-2 border-l-4 border-emerald-500">
                      <div className="text-xs font-bold uppercase tracking-wider text-emerald-800">
                        Sensor Rod Interpretation
                      </div>
                      <p className="text-xs text-gray-700 leading-relaxed">
                        {sensorStatus.interpretation.summary || JSON.stringify(sensorStatus.interpretation)}
                      </p>
                    </div>
                  )}
                </div>
              ) : null}
            </div>
          )}

          {/* TAB 4: CARE RECOMMENDATIONS */}
          {activeTab === 'recommendations' && (
            <div className="space-y-4 animate-fade-in">
              <div className="flex items-center justify-between">
                <h2 className="text-lg font-bold text-gray-900">Evidence-Grounded Care Recommendations</h2>
                <span className="text-xs text-gray-500">Generated with Groq botanical reasoning</span>
              </div>

              {recommendations.length === 0 ? (
                <div className="card p-8 text-center text-xs text-gray-400">
                  No active recommendations. Conditions are within normal limits.
                </div>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {recommendations.map((rec) => (
                    <div key={rec.id} className="card p-6 bg-white space-y-3 border-l-4 border-emerald-500">
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-bold text-emerald-700 uppercase tracking-wider">
                          {rec.category}
                        </span>
                        <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                          rec.priority === 'HIGH' ? 'bg-rose-100 text-rose-800' : 'bg-emerald-100 text-emerald-800'
                        }`}>
                          {rec.priority} Priority
                        </span>
                      </div>
                      <h3 className="text-base font-bold text-gray-900">{rec.title}</h3>
                      <p className="text-xs text-gray-600 leading-relaxed">{rec.message}</p>
                      {rec.recommendedAction && (
                        <div className="p-3 rounded-xl bg-slate-50 text-xs text-gray-800 font-medium">
                          <strong>Action: </strong>{rec.recommendedAction}
                        </div>
                      )}
                      {rec.evidence && (
                        <div className="text-[11px] text-gray-400 italic">
                          Evidence: {rec.evidence}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* TAB 5: AI PLANT ASSISTANT CHAT */}
          {activeTab === 'assistant' && (
            <div className="card p-6 bg-white space-y-6 flex flex-col h-[520px] animate-fade-in">
              <div className="flex items-center justify-between border-b border-gray-100 pb-3">
                <div className="flex items-center gap-2">
                  <Bot className="w-5 h-5 text-emerald-600" />
                  <h3 className="text-base font-bold text-gray-800">Ask Assistant about {plant.name}</h3>
                </div>
                <span className="text-xs text-emerald-700 font-semibold">Groq Grounded Context</span>
              </div>

              <div className="flex-1 overflow-y-auto space-y-4 pr-2 scrollbar-thin">
                {chatMessages.map((m, idx) => (
                  <div
                    key={idx}
                    className={`flex items-start gap-3 ${m.role === 'user' ? 'flex-row-reverse' : ''}`}
                  >
                    <div
                      className={`w-8 h-8 rounded-2xl flex items-center justify-center flex-shrink-0 text-white text-xs ${
                        m.role === 'user' ? 'bg-emerald-600' : 'bg-emerald-800'
                      }`}
                    >
                      {m.role === 'user' ? 'You' : <Bot className="w-4 h-4" />}
                    </div>
                    <div
                      className={`max-w-[80%] rounded-2xl p-4 text-xs leading-relaxed ${
                        m.role === 'user'
                          ? 'bg-emerald-600 text-white shadow-sm'
                          : 'bg-slate-50 border border-slate-100 text-gray-800'
                      }`}
                    >
                      <div className="whitespace-pre-line">{m.text}</div>
                    </div>
                  </div>
                ))}

                {chatLoading && (
                  <div className="flex items-center gap-2 text-xs text-gray-500 italic">
                    <RefreshCw className="w-3.5 h-3.5 animate-spin text-emerald-600" />
                    Consulting Groq plant model...
                  </div>
                )}
              </div>

              <form onSubmit={handleSendAssistant} className="flex gap-3">
                <input
                  type="text"
                  placeholder={`Why are the leaves on my ${plant.name} dropping?`}
                  value={chatInput}
                  onChange={(e) => setChatInput(e.target.value)}
                  disabled={chatLoading}
                  className="flex-1 px-4 py-2.5 border border-gray-200 rounded-xl text-xs focus:ring-2 focus:ring-emerald-500 bg-slate-50"
                />
                <button
                  type="submit"
                  disabled={chatLoading || !chatInput.trim()}
                  className="btn-primary text-xs px-5 py-2.5"
                >
                  Send
                </button>
              </form>
            </div>
          )}

          {/* TAB 6: DISEASE OBSERVATION */}
          {activeTab === 'disease' && (
            <div className="space-y-6 animate-fade-in">
              <div className="card p-6 bg-white space-y-4">
                <h3 className="text-base font-bold text-gray-800 flex items-center gap-2">
                  <ShieldCheck className="w-5 h-5 text-emerald-600" /> Log Disease Observation & Get Groq Diagnostic Assessment
                </h3>
                <form onSubmit={handleDiseaseInspect} className="space-y-4">
                  <div>
                    <label className="block text-xs font-bold text-gray-700 mb-1">Observed Foliage Symptoms *</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. Yellowing leaf margins with brown necrotic center, powdery mildew on underside..."
                      value={symptomInput}
                      onChange={(e) => setSymptomInput(e.target.value)}
                      className="w-full px-4 py-2.5 border border-gray-200 rounded-xl text-xs"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-gray-700 mb-1">Additional Environmental Notes</label>
                    <textarea
                      rows={2}
                      placeholder="e.g. Kept close to cold AC vent; recently fertilized 3 days ago..."
                      value={diseaseNotes}
                      onChange={(e) => setDiseaseNotes(e.target.value)}
                      className="w-full px-4 py-2.5 border border-gray-200 rounded-xl text-xs"
                    />
                  </div>
                  <div className="flex justify-end">
                    <button
                      type="submit"
                      disabled={inspectingDisease}
                      className="btn-primary text-xs flex items-center gap-2"
                    >
                      {inspectingDisease ? 'Analyzing Symptoms...' : 'Analyze with Groq'}
                    </button>
                  </div>
                </form>
              </div>

              {diseaseHistory.length > 0 && (
                <div className="space-y-4">
                  <h3 className="text-base font-bold text-gray-800">Past Disease Inspections</h3>
                  {diseaseHistory.map((d) => (
                    <div key={d.id} className="card p-6 bg-white space-y-3 border-l-4 border-amber-500">
                      <div className="flex items-center justify-between text-xs">
                        <span className="font-bold text-gray-800">Symptoms: {d.symptoms}</span>
                        <span className="text-gray-400">{new Date(d.inspectedAt).toLocaleDateString()}</span>
                      </div>
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs bg-slate-50 p-3.5 rounded-xl">
                        <div>
                          <strong className="text-emerald-800">Possible Causes: </strong>{d.possibleCauses}
                        </div>
                        <div>
                          <strong className="text-emerald-800">Inspection: </strong>{d.recommendedInspection}
                        </div>
                        <div className="md:col-span-2">
                          <strong className="text-emerald-800">Next Steps: </strong>{d.nextSteps}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* TAB 7: WATERING HISTORY */}
          {activeTab === 'history' && (
            <div className="card p-6 bg-white space-y-4 animate-fade-in">
              <h2 className="text-base font-bold text-gray-800">Watering Event Journal</h2>
              {wateringHistory.length === 0 ? (
                <div className="text-center py-8 text-xs text-gray-400">
                  No watering records logged yet. Click &quot;Water Plant Now&quot; to log your first event!
                </div>
              ) : (
                <div className="space-y-2">
                  {wateringHistory.map((w) => (
                    <div key={w.id} className="p-3.5 rounded-2xl bg-slate-50 border border-slate-100 flex items-center justify-between text-xs">
                      <div className="flex items-center gap-3">
                        <Droplet className="w-4 h-4 text-sky-500" />
                        <div>
                          <div className="font-bold text-gray-800">{w.notes || 'Watering recorded'}</div>
                          <div className="text-gray-400 text-[11px]">{new Date(w.wateredAt).toLocaleString()}</div>
                        </div>
                      </div>
                      <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800">
                        {w.onTime ? 'On Time' : 'Adjusted'}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </main>
      </div>
    </div>
  )
}