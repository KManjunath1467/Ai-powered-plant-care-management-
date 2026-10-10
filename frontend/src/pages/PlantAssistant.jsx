import React, { useEffect, useState } from 'react'
import Navbar from '../components/Navbar'
import Sidebar from '../components/Sidebar'
import Loader from '../components/Loader'
import { plantsApi, groqApi } from '../services/apiService'
import {
  Bot,
  Send,
  Sparkles,
  Leaf,
  Droplet,
  Sun,
  ShieldCheck,
  AlertCircle,
  HelpCircle,
  RefreshCw
} from 'lucide-react'

export default function PlantAssistant() {
  const [plants, setPlants] = useState([])
  const [selectedPlantId, setSelectedPlantId] = useState('')
  const [messages, setMessages] = useState([])
  const [inputQuestion, setInputQuestion] = useState('')
  const [asking, setAsking] = useState(false)
  const [groqStatus, setGroqStatus] = useState(null)
  const [loading, setLoading] = useState(true)

  const quickQuestions = [
    'Should I water this plant today?',
    'Why are some leaf tips turning brown?',
    'What does the latest soil moisture reading indicate?',
    'Is the current local weather suitable for this plant?',
    'How can I encourage healthy new foliage growth?'
  ]

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    try {
      setLoading(true)
      const [pRes, sRes] = await Promise.all([
        plantsApi.getPlants(),
        groqApi.getStatus()
      ])
      setPlants(pRes.data || [])
      setGroqStatus(sRes.data)

      if (pRes.data && pRes.data.length > 0) {
        setSelectedPlantId(pRes.data[0].id)
        setMessages([
          {
            role: 'assistant',
            text: `Hello! I'm your Groq-powered Botanical Plant Assistant for ${pRes.data[0].name}. Ask me any questions about watering, soil moisture, sunlight, or health signs!`
          }
        ])
      }
    } catch (err) {
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  const handlePlantChange = (newId) => {
    setSelectedPlantId(newId)
    const plant = plants.find((p) => p.id === Number(newId))
    setMessages([
      {
        role: 'assistant',
        text: `Switched context to ${plant?.name || 'plant'}. How can I help care for it today?`
      }
    ])
  }

  const handleSend = async (questionToSend) => {
    const q = questionToSend || inputQuestion
    if (!q || !q.trim() || !selectedPlantId) return

    const userMsg = { role: 'user', text: q.trim() }
    setMessages((prev) => [...prev, userMsg])
    setInputQuestion('')

    try {
      setAsking(true)
      const res = await groqApi.askAssistant(selectedPlantId, {
        question: q.trim()
      })
      const botMsg = {
        role: 'assistant',
        text: res.data.answer,
        provider: res.data.provider
      }
      setMessages((prev) => [...prev, botMsg])
    } catch (err) {
      console.error(err)
      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          text: 'I encountered an error retrieving botanical advice. Please verify your connection or try again.'
        }
      ])
    } finally {
      setAsking(false)
    }
  }

  if (loading) return <Loader />

  const selectedPlant = plants.find((p) => p.id === Number(selectedPlantId))

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#F4F7F4] via-[#EBF2EB] to-[#DCEADC]/40">
      <Navbar />
      <div className="flex max-w-7xl mx-auto px-6">
        <Sidebar />
        <main className="flex-1 py-6 space-y-6">
          {/* Header */}
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 animate-fade-in">
            <div>
              <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-emerald-100 text-emerald-800 text-xs font-semibold uppercase tracking-wider mb-2">
                <Sparkles className="w-3.5 h-3.5 text-emerald-600" />
                Contextual Botanical Intelligence
              </div>
              <h1 className="text-3xl font-bold text-gray-900 tracking-tight">AI Plant Assistant</h1>
              <p className="text-gray-600 text-sm mt-1">
                Ask natural-language questions grounded in your plant&apos;s species, weather forecast, and IoT sensors.
              </p>
            </div>

            {groqStatus && (
              <div className="flex items-center gap-2 bg-white px-3.5 py-2 rounded-2xl border border-gray-100 shadow-sm text-xs">
                <div className={`w-2 h-2 rounded-full ${groqStatus.configured ? 'bg-emerald-500' : 'bg-amber-500'}`} />
                <span className="font-bold text-gray-700">
                  {groqStatus.configured ? `Groq: ${groqStatus.model}` : 'Botanical Engine Fallback'}
                </span>
              </div>
            )}
          </div>

          {/* Plant Context Selector Card */}
          <div className="card p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white">
            <div className="flex items-center gap-3">
              <label className="text-xs font-bold text-gray-700 uppercase tracking-wider">Select Context Plant:</label>
              <select
                value={selectedPlantId}
                onChange={(e) => handlePlantChange(e.target.value)}
                className="px-4 py-2 border border-gray-200 rounded-xl text-sm font-semibold bg-slate-50 focus:ring-2 focus:ring-emerald-500"
              >
                {plants.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name} ({p.plantType})
                  </option>
                ))}
              </select>
            </div>

            {selectedPlant && (
              <div className="text-xs text-gray-500 flex items-center gap-3">
                <span>Location: <strong>{selectedPlant.location}</strong></span>
                <span>Health: <strong className="text-emerald-700">{selectedPlant.health}</strong></span>
                <span>Streak: <strong>{selectedPlant.wateringStreak || 0} days</strong></span>
              </div>
            )}
          </div>

          {/* Chat Window */}
          <div className="card p-6 bg-white space-y-6 flex flex-col h-[520px]">
            {/* Messages Scroll Area */}
            <div className="flex-1 overflow-y-auto space-y-4 pr-2 scrollbar-thin">
              {messages.map((m, idx) => (
                <div
                  key={idx}
                  className={`flex items-start gap-3 ${m.role === 'user' ? 'flex-row-reverse' : ''}`}
                >
                  <div
                    className={`w-9 h-9 rounded-2xl flex items-center justify-center flex-shrink-0 text-white ${
                      m.role === 'user'
                        ? 'bg-emerald-600'
                        : 'bg-gradient-to-br from-emerald-700 to-teal-800'
                    }`}
                  >
                    {m.role === 'user' ? 'You' : <Bot className="w-5 h-5" />}
                  </div>

                  <div
                    className={`max-w-[80%] rounded-2xl p-4 text-sm leading-relaxed ${
                      m.role === 'user'
                        ? 'bg-emerald-600 text-white shadow-sm'
                        : 'bg-slate-50 border border-slate-100 text-gray-800'
                    }`}
                  >
                    <div className="whitespace-pre-line">{m.text}</div>
                    {m.provider && (
                      <div className="text-[10px] text-gray-400 mt-2 italic">
                        Answered via {m.provider}
                      </div>
                    )}
                  </div>
                </div>
              ))}

              {asking && (
                <div className="flex items-center gap-3 text-xs text-gray-500 italic">
                  <div className="w-8 h-8 rounded-full bg-emerald-100 flex items-center justify-center text-emerald-600 animate-spin">
                    <RefreshCw className="w-4 h-4" />
                  </div>
                  Synthesizing botanical advice with Groq...
                </div>
              )}
            </div>

            {/* Quick Question Buttons */}
            <div className="pt-2 border-t border-gray-100">
              <div className="text-[11px] font-bold uppercase tracking-wider text-gray-500 mb-2">
                Frequently Asked Inquiries
              </div>
              <div className="flex flex-wrap gap-2">
                {quickQuestions.map((qq, idx) => (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => handleSend(qq)}
                    disabled={asking}
                    className="text-xs px-3 py-1.5 rounded-full bg-slate-50 border border-slate-200 text-gray-700 hover:bg-emerald-50 hover:border-emerald-300 transition-colors"
                  >
                    {qq}
                  </button>
                ))}
              </div>
            </div>

            {/* Input Bar */}
            <form
              onSubmit={(e) => {
                e.preventDefault()
                handleSend()
              }}
              className="flex gap-3"
            >
              <input
                type="text"
                placeholder={`Ask about ${selectedPlant?.name || 'this plant'}...`}
                value={inputQuestion}
                onChange={(e) => setInputQuestion(e.target.value)}
                disabled={asking}
                className="flex-1 px-4 py-3 border border-gray-200 rounded-2xl text-sm focus:ring-2 focus:ring-emerald-500 bg-slate-50"
              />
              <button
                type="submit"
                disabled={asking || !inputQuestion.trim()}
                className="btn-primary px-6 rounded-2xl flex items-center gap-2"
              >
                <Send className="w-4 h-4" /> Send
              </button>
            </form>
          </div>
        </main>
      </div>
    </div>
  )
}
