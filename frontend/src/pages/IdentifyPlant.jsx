import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import Sidebar from '../components/Sidebar'
import Loader from '../components/Loader'
import { aiApi, plantsApi } from '../services/apiService'
import {
  Sparkles,
  Upload,
  Camera,
  CheckCircle,
  AlertCircle,
  Leaf,
  Layers,
  Droplets,
  Sun,
  Thermometer,
  ShieldAlert,
  ArrowRight,
  Info
} from 'lucide-react'

export default function IdentifyPlant() {
  const navigate = useNavigate()
  const [selectedFile, setSelectedFile] = useState(null)
  const [previewUrl, setPreviewUrl] = useState(null)
  const [plantHint, setPlantHint] = useState('')
  const [analyzing, setAnalyzing] = useState(false)
  const [saving, setSaving] = useState(false)
  const [analysisResult, setAnalysisResult] = useState(null)
  const [activeAnatomyTab, setActiveAnatomyTab] = useState('leaves')
  const [errorMsg, setErrorMsg] = useState(null)

  // Editable plant form state
  const [plantForm, setPlantForm] = useState({
    name: '',
    plantType: '',
    scientificName: '',
    plantFamily: '',
    potSize: 'Medium (6-8 inches)',
    isIndoor: true,
    location: 'Living Room',
    customWateringInterval: 7,
    notes: ''
  })

  const handleFileChange = (e) => {
    const file = e.target.files[0]
    if (!file) return

    if (!file.type.startsWith('image/')) {
      setErrorMsg('Please select a valid image file (JPEG, PNG, WEBP).')
      return
    }

    setErrorMsg(null)
    setSelectedFile(file)
    setPreviewUrl(URL.createObjectURL(file))
    setAnalysisResult(null)
  }

  const handleAnalyze = async () => {
    if (!selectedFile && !plantHint.trim()) {
      setErrorMsg('Please upload a plant photo or enter a plant name hint.')
      return
    }

    try {
      setAnalyzing(true)
      setErrorMsg(null)

      const formData = new FormData()
      if (selectedFile) {
        formData.append('image', selectedFile)
      }
      if (plantHint.trim()) {
        formData.append('plantName', plantHint.trim())
      }

      const res = await aiApi.analyzePlantImage(formData)
      const data = res.data
      setAnalysisResult(data)

      // Pre-fill editable save form
      const detectedName = data.candidateName || data.detectedPlant?.plantName || 'My Plant'
      setPlantForm({
        name: detectedName,
        plantType: detectedName,
        scientificName: data.scientificName || '',
        plantFamily: data.plantFamily || '',
        potSize: data.detectedPlant?.recommendedPotSize || 'Medium (6-8 inches)',
        isIndoor: true,
        location: 'Living Room Window',
        customWateringInterval: data.detectedPlant?.baseWateringDays || 7,
        notes: data.tips || data.detectedPlant?.description || ''
      })
    } catch (err) {
      console.error('Plant analysis failed:', err)
      setErrorMsg('AI Analysis encountered an issue. Please try again or provide a hint.')
    } finally {
      setAnalyzing(false)
    }
  }

  const handleSavePlant = async (e) => {
    e.preventDefault()
    try {
      setSaving(true)
      const payload = {
        name: plantForm.name,
        plantType: plantForm.plantType,
        scientificName: plantForm.scientificName,
        plantFamily: plantForm.plantFamily,
        potSize: plantForm.potSize,
        isIndoor: plantForm.isIndoor,
        location: plantForm.location,
        customWateringInterval: Number(plantForm.customWateringInterval) || 7,
        notes: plantForm.notes,
        anatomyJson: analysisResult?.anatomy ? JSON.stringify(analysisResult.anatomy) : null
      }

      const plantRes = await plantsApi.createPlant(payload)
      const createdId = plantRes.data.id

      // If user uploaded an image, upload it to the plant record
      if (selectedFile && createdId) {
        try {
          const imgData = new FormData()
          imgData.append('image', selectedFile)
          await plantsApi.uploadImage(createdId, imgData)
        } catch (imgErr) {
          console.warn('Image file upload to plant storage was skipped:', imgErr)
        }
      }

      navigate(`/plants/${createdId}`)
    } catch (err) {
      console.error('Failed to save identified plant:', err)
      setErrorMsg('Failed to save plant profile to your collection.')
    } finally {
      setSaving(false)
    }
  }

  const anatomySections = analysisResult?.anatomy ? [
    { key: 'leaves', label: '🍃 Leaves & Lamina', data: analysisResult.anatomy.leaves },
    { key: 'roots', label: '🌱 Root System', data: analysisResult.anatomy.roots },
    { key: 'stem', label: '🪵 Stem & Petioles', data: analysisResult.anatomy.stem },
    { key: 'vascular', label: '🔄 Xylem & Phloem', data: analysisResult.anatomy.vascularSystem },
    { key: 'flowers', label: '🌸 Floral Organs', data: analysisResult.anatomy.flowers },
    { key: 'buds', label: '🌿 Growth Meristems', data: analysisResult.anatomy.budsAndNewGrowth },
  ] : []

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#F4F7F4] via-[#EBF2EB] to-[#DCEADC]/40">
      <Navbar />
      <div className="flex max-w-7xl mx-auto px-6">
        <Sidebar />
        <main className="flex-1 py-6 space-y-8">
          <div className="animate-fade-in">
            <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-emerald-100 text-emerald-800 text-xs font-semibold uppercase tracking-wider mb-2">
              <Sparkles className="w-3.5 h-3.5 text-emerald-600" />
              Groq LLM Botanical Intelligence
            </div>
            <h1 className="text-3xl font-bold text-gray-900 tracking-tight">Identify & Explore Plant Anatomy</h1>
            <p className="text-gray-600 mt-1">
              Upload a photograph to identify plant taxonomy, explore botanical anatomy, and add it to your care schedule.
            </p>
          </div>

          {errorMsg && (
            <div className="p-4 rounded-2xl bg-amber-50 border border-amber-200 text-amber-800 flex items-center gap-3 animate-fade-in">
              <AlertCircle className="w-5 h-5 text-amber-600 flex-shrink-0" />
              <div className="text-sm font-medium">{errorMsg}</div>
            </div>
          )}

          {/* Upload and Preview Card */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
            <div className="lg:col-span-6 card p-6 space-y-6">
              <h2 className="text-lg font-semibold text-gray-800 flex items-center gap-2">
                <Camera className="w-5 h-5 text-emerald-600" />
                Plant Photograph Upload
              </h2>

              <div className="border-2 border-dashed border-emerald-200 rounded-3xl p-6 text-center hover:border-emerald-500 transition-colors bg-white/60">
                {previewUrl ? (
                  <div className="space-y-4">
                    <img
                      src={previewUrl}
                      alt="Plant Preview"
                      className="w-full h-64 object-cover rounded-2xl shadow-md mx-auto"
                    />
                    <label className="btn-secondary cursor-pointer inline-flex items-center gap-2 text-xs">
                      <Upload className="w-4 h-4" /> Change Image
                      <input type="file" accept="image/*" onChange={handleFileChange} className="hidden" />
                    </label>
                  </div>
                ) : (
                  <label className="cursor-pointer block py-8">
                    <div className="w-16 h-16 rounded-full bg-emerald-50 text-emerald-600 mx-auto flex items-center justify-center mb-4">
                      <Upload className="w-8 h-8" />
                    </div>
                    <div className="text-base font-semibold text-gray-800">Click to upload plant photo</div>
                    <div className="text-xs text-gray-500 mt-1">PNG, JPG, WEBP up to 10MB</div>
                    <input type="file" accept="image/*" onChange={handleFileChange} className="hidden" />
                  </label>
                )}
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-gray-600 mb-2">
                  Optional Botanical Hint / Common Name
                </label>
                <input
                  type="text"
                  placeholder="e.g. Monstera, Snake Plant, Ficus..."
                  value={plantHint}
                  onChange={(e) => setPlantHint(e.target.value)}
                  className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-emerald-500 focus:border-transparent text-sm"
                />
              </div>

              <button
                type="button"
                onClick={handleAnalyze}
                disabled={analyzing}
                className="w-full btn-primary flex items-center justify-center gap-2"
              >
                {analyzing ? (
                  <>
                    <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                    Analyzing with Groq Intelligence...
                  </>
                ) : (
                  <>
                    <Sparkles className="w-5 h-5" />
                    Identify & Generate Anatomy
                  </>
                )}
              </button>
            </div>

            {/* Analysis Result Overview */}
            <div className="lg:col-span-6 space-y-6">
              {analyzing ? (
                <div className="card p-12 text-center space-y-4 flex flex-col items-center justify-center min-h-[380px]">
                  <div className="w-16 h-16 rounded-full bg-emerald-100 flex items-center justify-center text-emerald-600 animate-pulse">
                    <Leaf className="w-8 h-8" />
                  </div>
                  <h3 className="text-lg font-bold text-gray-800">Consulting Groq Botanical Model...</h3>
                  <p className="text-xs text-gray-500 max-w-sm">
                    Structuring taxonomic classification, validating botanical families, and synthesizing microscopic xylem/phloem and morphological anatomy.
                  </p>
                </div>
              ) : analysisResult ? (
                <div className="card p-6 space-y-6 animate-fade-in">
                  <div className="flex items-start justify-between">
                    <div>
                      <div className="text-xs font-semibold uppercase tracking-wider text-emerald-600">Identified Species</div>
                      <h2 className="text-2xl font-bold text-gray-900">{analysisResult.candidateName}</h2>
                      <p className="text-sm italic text-gray-500 font-serif">
                        {analysisResult.scientificName} &bull; Family: {analysisResult.plantFamily}
                      </p>
                    </div>
                    <div className="px-3 py-1 rounded-full bg-emerald-50 border border-emerald-200 text-emerald-700 text-xs font-bold">
                      {(analysisResult.confidence * 100).toFixed(0)}% Match
                    </div>
                  </div>

                  {/* Care Parameters Grid */}
                  <div className="grid grid-cols-2 gap-3 text-xs">
                    <div className="p-3 rounded-xl bg-slate-50 border border-slate-100">
                      <div className="flex items-center gap-1.5 text-gray-500 font-medium mb-1">
                        <Sun className="w-4 h-4 text-amber-500" /> Sunlight Needs
                      </div>
                      <div className="font-semibold text-gray-800">
                        {analysisResult.careRecommendations?.sunlightNeeds || 'Bright Indirect'}
                      </div>
                    </div>
                    <div className="p-3 rounded-xl bg-slate-50 border border-slate-100">
                      <div className="flex items-center gap-1.5 text-gray-500 font-medium mb-1">
                        <Droplets className="w-4 h-4 text-sky-500" /> Watering Cycle
                      </div>
                      <div className="font-semibold text-gray-800">
                        {analysisResult.careRecommendations?.wateringSchedule || 'Every 7-10 days'}
                      </div>
                    </div>
                    <div className="p-3 rounded-xl bg-slate-50 border border-slate-100">
                      <div className="flex items-center gap-1.5 text-gray-500 font-medium mb-1">
                        <Thermometer className="w-4 h-4 text-rose-500" /> Temperature
                      </div>
                      <div className="font-semibold text-gray-800">
                        {analysisResult.careRecommendations?.temperatureRange || '18-24°C'}
                      </div>
                    </div>
                    <div className="p-3 rounded-xl bg-slate-50 border border-slate-100">
                      <div className="flex items-center gap-1.5 text-gray-500 font-medium mb-1">
                        <ShieldAlert className="w-4 h-4 text-amber-600" /> Pet Toxicity
                      </div>
                      <div className="font-semibold text-gray-800">
                        {analysisResult.careRecommendations?.toxicity || 'Non-toxic'}
                      </div>
                    </div>
                  </div>

                  {analysisResult.taxonomy?.generalCareSummary && (
                    <div className="p-3.5 rounded-2xl bg-emerald-50/70 border border-emerald-100 text-xs text-emerald-900 leading-relaxed">
                      <span className="font-bold">Botanical Summary: </span>
                      {analysisResult.taxonomy.generalCareSummary}
                    </div>
                  )}
                </div>
              ) : (
                <div className="card p-12 text-center space-y-4 flex flex-col items-center justify-center min-h-[380px] text-gray-400">
                  <Leaf className="w-12 h-12 text-gray-300" />
                  <p className="text-sm">Upload a photo on the left to begin identification.</p>
                </div>
              )}
            </div>
          </div>

          {/* Plant Anatomy Section */}
          {analysisResult?.anatomy && (
            <div className="card p-8 space-y-6 animate-fade-in">
              <div className="flex items-center justify-between border-b border-gray-100 pb-4">
                <div className="flex items-center gap-3">
                  <div className="p-2.5 rounded-2xl bg-emerald-100 text-emerald-700">
                    <Layers className="w-6 h-6" />
                  </div>
                  <div>
                    <h2 className="text-xl font-bold text-gray-900">Comprehensive Botanical Anatomy</h2>
                    <p className="text-xs text-gray-500">
                      Morphological structure generated for {analysisResult.candidateName} ({analysisResult.scientificName})
                    </p>
                  </div>
                </div>
              </div>

              {/* Anatomy Tabs */}
              <div className="flex flex-wrap gap-2">
                {anatomySections.map((sec) => (
                  <button
                    key={sec.key}
                    type="button"
                    onClick={() => setActiveAnatomyTab(sec.key)}
                    className={`px-4 py-2 rounded-xl text-xs font-semibold transition-all ${
                      activeAnatomyTab === sec.key
                        ? 'bg-emerald-600 text-white shadow-md'
                        : 'bg-white border border-gray-200 text-gray-700 hover:bg-gray-50'
                    }`}
                  >
                    {sec.label}
                  </button>
                ))}
              </div>

              {/* Selected Anatomy Details */}
              {anatomySections.map((sec) => {
                if (sec.key !== activeAnatomyTab || !sec.data) return null
                return (
                  <div key={sec.key} className="p-6 rounded-2xl bg-slate-50 border border-slate-100 space-y-4 animate-fade-in">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      {Object.entries(sec.data).map(([prop, val]) => (
                        <div key={prop} className="bg-white p-4 rounded-xl border border-gray-100 shadow-sm">
                          <div className="text-xs uppercase font-bold tracking-wider text-emerald-700 mb-1">
                            {prop.replace(/([A-Z])/g, ' $1')}
                          </div>
                          <div className="text-sm text-gray-700 leading-relaxed">{val}</div>
                        </div>
                      ))}
                    </div>
                  </div>
                )
              })}

              {analysisResult.anatomy.anatomicalSummary && (
                <div className="text-xs text-gray-500 italic bg-white p-4 rounded-xl border border-gray-100">
                  <span className="font-semibold text-gray-700">Note: </span>
                  {analysisResult.anatomy.anatomicalSummary}
                </div>
              )}
            </div>
          )}

          {/* Confirm & Save Form */}
          {analysisResult && (
            <div className="card p-8 space-y-6 animate-fade-in border-2 border-emerald-200 bg-white">
              <div className="flex items-center gap-3">
                <CheckCircle className="w-6 h-6 text-emerald-600" />
                <div>
                  <h2 className="text-xl font-bold text-gray-900">Review & Add to Your Garden</h2>
                  <p className="text-xs text-gray-500">Confirm details before saving this plant profile to your database.</p>
                </div>
              </div>

              <form onSubmit={handleSavePlant} className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-1">Plant Nickname *</label>
                  <input
                    type="text"
                    required
                    value={plantForm.name}
                    onChange={(e) => setPlantForm({ ...plantForm, name: e.target.value })}
                    className="w-full px-4 py-2.5 border border-gray-200 rounded-xl text-sm"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-1">Botanical Type *</label>
                  <input
                    type="text"
                    required
                    value={plantForm.plantType}
                    onChange={(e) => setPlantForm({ ...plantForm, plantType: e.target.value })}
                    className="w-full px-4 py-2.5 border border-gray-200 rounded-xl text-sm"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-1">Pot Size / Substrate Volume</label>
                  <input
                    type="text"
                    value={plantForm.potSize}
                    onChange={(e) => setPlantForm({ ...plantForm, potSize: e.target.value })}
                    className="w-full px-4 py-2.5 border border-gray-200 rounded-xl text-sm"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-1">Location in Home</label>
                  <input
                    type="text"
                    value={plantForm.location}
                    onChange={(e) => setPlantForm({ ...plantForm, location: e.target.value })}
                    className="w-full px-4 py-2.5 border border-gray-200 rounded-xl text-sm"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-1">Watering Interval (days)</label>
                  <input
                    type="number"
                    min="1"
                    max="60"
                    value={plantForm.customWateringInterval}
                    onChange={(e) => setPlantForm({ ...plantForm, customWateringInterval: e.target.value })}
                    className="w-full px-4 py-2.5 border border-gray-200 rounded-xl text-sm"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-1">Environment</label>
                  <select
                    value={plantForm.isIndoor ? 'true' : 'false'}
                    onChange={(e) => setPlantForm({ ...plantForm, isIndoor: e.target.value === 'true' })}
                    className="w-full px-4 py-2.5 border border-gray-200 rounded-xl text-sm"
                  >
                    <option value="true">Indoor Plant</option>
                    <option value="false">Outdoor Plant</option>
                  </select>
                </div>

                <div className="md:col-span-2">
                  <label className="block text-xs font-bold text-gray-700 mb-1">Care Notes & Tips</label>
                  <textarea
                    rows={2}
                    value={plantForm.notes}
                    onChange={(e) => setPlantForm({ ...plantForm, notes: e.target.value })}
                    className="w-full px-4 py-2.5 border border-gray-200 rounded-xl text-sm"
                  />
                </div>

                <div className="md:col-span-2 flex justify-end gap-3">
                  <button
                    type="button"
                    onClick={() => navigate('/plants')}
                    className="btn-secondary"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={saving}
                    className="btn-primary flex items-center gap-2"
                  >
                    {saving ? 'Saving...' : 'Save Plant Profile'} <ArrowRight className="w-4 h-4" />
                  </button>
                </div>
              </form>
            </div>
          )}
        </main>
      </div>
    </div>
  )
}
