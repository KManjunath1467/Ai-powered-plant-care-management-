import React, { useEffect, useState } from 'react'
import Navbar from '../components/Navbar'
import Sidebar from '../components/Sidebar'
import Loader from '../components/Loader'
import { sensorApi, plantsApi } from '../services/apiService'
import {
  Cpu,
  Activity,
  AlertTriangle,
  CheckCircle2,
  Plus,
  Unlink,
  Link2,
  Radio,
  Clock,
  Battery,
  Droplet,
  Thermometer,
  Zap,
  TestTube,
  Sun,
  ShieldCheck,
  Send
} from 'lucide-react'

export default function HealthMonitoring() {
  const [plants, setPlants] = useState([])
  const [devices, setDevices] = useState([])
  const [selectedPlantId, setSelectedPlantId] = useState('')
  const [plantSensorData, setPlantSensorData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [loadingSensor, setLoadingSensor] = useState(false)
  const [showRegisterModal, setShowRegisterModal] = useState(false)
  const [showSimulateModal, setShowSimulateModal] = useState(false)

  // Registration Form
  const [newDeviceId, setNewDeviceId] = useState('')
  const [newDeviceName, setNewDeviceName] = useState('')
  const [registering, setRegistering] = useState(false)

  // Simulation Form (for testing IoT telemetry without physical hardware)
  const [simForm, setSimForm] = useState({
    deviceId: '',
    soilMoisture: 42.5,
    soilTemperature: 22.0,
    soilEc: 1.2,
    soilPh: 6.5,
    ambientTemperature: 24.5,
    ambientHumidity: 58.0,
    lightLux: 2200,
    batteryLevel: 96
  })
  const [simulating, setSimulating] = useState(false)

  useEffect(() => {
    loadInitialData()
  }, [])

  useEffect(() => {
    if (selectedPlantId) {
      loadPlantSensor(selectedPlantId)
    }
  }, [selectedPlantId])

  const loadInitialData = async () => {
    try {
      setLoading(true)
      const [pRes, dRes] = await Promise.all([
        plantsApi.getPlants(),
        sensorApi.getMyDevices()
      ])
      setPlants(pRes.data || [])
      setDevices(dRes.data || [])
      if (pRes.data && pRes.data.length > 0) {
        setSelectedPlantId(pRes.data[0].id)
      }
    } catch (err) {
      console.error('Failed to load sensor and plant data:', err)
    } finally {
      setLoading(false)
    }
  }

  const loadPlantSensor = async (plantId) => {
    try {
      setLoadingSensor(true)
      const res = await sensorApi.getPlantSensorStatus(plantId)
      setPlantSensorData(res.data)
    } catch (err) {
      console.error('Failed to load plant sensor status:', err)
      setPlantSensorData(null)
    } finally {
      setLoadingSensor(false)
    }
  }

  const handleRegisterDevice = async (e) => {
    e.preventDefault()
    if (!newDeviceId.trim()) return
    try {
      setRegistering(true)
      await sensorApi.registerDevice({
        deviceId: newDeviceId.trim(),
        name: newDeviceName.trim() || undefined
      })
      setNewDeviceId('')
      setNewDeviceName('')
      setShowRegisterModal(false)
      const dRes = await sensorApi.getMyDevices()
      setDevices(dRes.data || [])
    } catch (err) {
      alert(err.response?.data?.message || 'Device registration failed')
    } finally {
      setRegistering(false)
    }
  }

  const handlePair = async (deviceId) => {
    if (!selectedPlantId) return
    try {
      await sensorApi.pairDevice(deviceId, selectedPlantId)
      const [pRes, dRes] = await Promise.all([
        sensorApi.getPlantSensorStatus(selectedPlantId),
        sensorApi.getMyDevices()
      ])
      setPlantSensorData(pRes.data)
      setDevices(dRes.data || [])
    } catch (err) {
      alert('Failed to pair device: ' + (err.response?.data?.message || err.message))
    }
  }

  const handleUnpair = async (deviceId) => {
    try {
      await sensorApi.unpairDevice(deviceId)
      const [pRes, dRes] = await Promise.all([
        sensorApi.getPlantSensorStatus(selectedPlantId),
        sensorApi.getMyDevices()
      ])
      setPlantSensorData(pRes.data)
      setDevices(dRes.data || [])
    } catch (err) {
      alert('Failed to unpair device.')
    }
  }

  const handleSimulate = async (e) => {
    e.preventDefault()
    if (!simForm.deviceId) {
      alert('Please select a device.')
      return
    }
    try {
      setSimulating(true)
      await sensorApi.ingestReading(simForm.deviceId, {
        soilMoisture: Number(simForm.soilMoisture),
        soilTemperature: Number(simForm.soilTemperature),
        soilEc: Number(simForm.soilEc),
        soilPh: Number(simForm.soilPh),
        ambientTemperature: Number(simForm.ambientTemperature),
        ambientHumidity: Number(simForm.ambientHumidity),
        lightLux: Number(simForm.lightLux),
        batteryLevel: Number(simForm.batteryLevel)
      })
      setShowSimulateModal(false)
      if (selectedPlantId) {
        loadPlantSensor(selectedPlantId)
      }
    } catch (err) {
      alert('Simulation failed: ' + (err.response?.data?.message || err.message))
    } finally {
      setSimulating(false)
    }
  }

  if (loading) return <Loader />

  const selectedPlant = plants.find((p) => p.id === Number(selectedPlantId))

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#F4F7F4] via-[#EBF2EB] to-[#DCEADC]/40">
      <Navbar />
      <div className="flex max-w-7xl mx-auto px-6">
        <Sidebar />
        <main className="flex-1 py-6 space-y-8">
          {/* Header */}
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 animate-fade-in">
            <div>
              <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-emerald-100 text-emerald-800 text-xs font-semibold uppercase tracking-wider mb-2">
                <Cpu className="w-3.5 h-3.5 text-emerald-600" />
                IoT Hardware Telemetry Integration
              </div>
              <h1 className="text-3xl font-bold text-gray-900 tracking-tight">Sensor Rod Health Monitoring</h1>
              <p className="text-gray-600 text-sm mt-1">
                Substrate and environmental metrics from ESP32/ESP8266 probe rods with strict empty-state validation.
              </p>
            </div>

            <div className="flex items-center gap-3">
              <button
                onClick={() => setShowRegisterModal(true)}
                className="btn-primary text-xs flex items-center gap-2"
              >
                <Plus className="w-4 h-4" /> Register Sensor Rod
              </button>
              <button
                onClick={() => {
                  if (devices.length > 0) {
                    setSimForm({ ...simForm, deviceId: devices[0].deviceId })
                  }
                  setShowSimulateModal(true)
                }}
                className="btn-secondary text-xs flex items-center gap-2"
                title="Send test telemetry payload"
              >
                <Send className="w-4 h-4 text-emerald-600" /> Test Hardware Reading
              </button>
            </div>
          </div>

          {/* Plant Selector */}
          <div className="card p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white">
            <div className="flex items-center gap-3">
              <label className="text-xs font-bold text-gray-700 uppercase tracking-wider">Select Plant:</label>
              <select
                value={selectedPlantId}
                onChange={(e) => setSelectedPlantId(Number(e.target.value))}
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
              <div className="text-xs text-gray-500">
                Location: <span className="font-semibold text-gray-800">{selectedPlant.location}</span> &bull;{' '}
                {selectedPlant.isIndoor ? 'Indoor' : 'Outdoor'}
              </div>
            )}
          </div>

          {loadingSensor ? (
            <div className="card p-12 text-center text-gray-500">
              <div className="w-8 h-8 border-2 border-emerald-600 border-t-transparent rounded-full animate-spin mx-auto mb-3" />
              Loading telemetry from paired rod...
            </div>
          ) : plantSensorData && !plantSensorData.hasSensor ? (
            /* MANDATORY EMPTY-STATE RULE: Strictly no fake data when unpaired */
            <div className="card p-12 text-center space-y-4 border-2 border-dashed border-gray-200 bg-white">
              <div className="w-16 h-16 rounded-full bg-slate-100 text-gray-400 mx-auto flex items-center justify-center">
                <Radio className="w-8 h-8" />
              </div>
              <div className="max-w-md mx-auto space-y-2">
                <h3 className="text-lg font-bold text-gray-800">No Sensor Rod Paired</h3>
                <p className="text-sm text-gray-600">
                  Connect a compatible sensor rod to enable soil-based health monitoring for this plant.
                </p>
                <p className="text-xs text-gray-400">
                  Soil moisture, electrical conductivity (EC), and pH cannot be displayed without physical telemetry.
                </p>
              </div>

              {devices.length > 0 ? (
                <div className="pt-4 max-w-sm mx-auto space-y-2">
                  <div className="text-xs font-semibold text-gray-500">Pair an available sensor device:</div>
                  <div className="flex gap-2">
                    <select
                      id="deviceSelect"
                      className="flex-1 px-3 py-2 text-xs border border-gray-200 rounded-xl bg-slate-50"
                    >
                      {devices.filter((d) => !d.plantId).map((d) => (
                        <option key={d.deviceId} value={d.deviceId}>
                          {d.name} ({d.deviceId})
                        </option>
                      ))}
                    </select>
                    <button
                      onClick={() => {
                        const sel = document.getElementById('deviceSelect')?.value
                        if (sel) handlePair(sel)
                      }}
                      className="btn-primary text-xs px-4 py-2"
                    >
                      Pair Now
                    </button>
                  </div>
                </div>
              ) : (
                <button
                  onClick={() => setShowRegisterModal(true)}
                  className="btn-primary text-xs inline-flex items-center gap-2"
                >
                  <Plus className="w-4 h-4" /> Register Your First Sensor Rod
                </button>
              )}
            </div>
          ) : plantSensorData && plantSensorData.hasSensor ? (
            /* ACTIVE TELEMETRY VIEW */
            <div className="space-y-6 animate-fade-in">
              {/* Device Status Bar */}
              <div className="card p-6 flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white">
                <div className="flex items-center gap-4">
                  <div className={`p-3 rounded-2xl ${plantSensorData.isStale ? 'bg-amber-100 text-amber-700' : 'bg-emerald-100 text-emerald-700'}`}>
                    <Cpu className="w-6 h-6" />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h3 className="text-base font-bold text-gray-900">{plantSensorData.device?.name}</h3>
                      <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold ${
                        plantSensorData.isStale
                          ? 'bg-amber-100 text-amber-800'
                          : 'bg-emerald-100 text-emerald-800'
                      }`}>
                        {plantSensorData.isStale ? 'STALE TELEMETRY (>2h)' : 'LIVE & ACTIVE'}
                      </span>
                    </div>
                    <div className="text-xs text-gray-500 flex items-center gap-3 mt-1">
                      <span>Device ID: <code className="font-mono">{plantSensorData.device?.deviceId}</code></span>
                      {plantSensorData.device?.lastActiveAt && (
                        <span>Last Contact: {new Date(plantSensorData.device.lastActiveAt).toLocaleTimeString()}</span>
                      )}
                    </div>
                  </div>
                </div>

                <div className="flex items-center gap-4">
                  {plantSensorData.device?.batteryLevel != null && (
                    <div className="flex items-center gap-1.5 text-xs text-gray-600 bg-slate-50 px-3 py-1.5 rounded-xl border border-gray-100">
                      <Battery className="w-4 h-4 text-emerald-600" />
                      {plantSensorData.device.batteryLevel}% Battery
                    </div>
                  )}
                  <button
                    onClick={() => handleUnpair(plantSensorData.device.deviceId)}
                    className="btn-secondary text-xs text-rose-600 hover:bg-rose-50 border-rose-200 flex items-center gap-1.5"
                  >
                    <Unlink className="w-3.5 h-3.5" /> Unpair Rod
                  </button>
                </div>
              </div>

              {plantSensorData.isStale && (
                <div className="p-4 rounded-2xl bg-amber-50 border border-amber-200 text-amber-900 flex items-center gap-3">
                  <AlertTriangle className="w-5 h-5 text-amber-600 flex-shrink-0" />
                  <div className="text-xs">
                    <strong>Telemetry Stale Warning:</strong> The sensor rod has not transmitted readings in over 2 hours.
                    Values below reflect the last recorded state and should be manually verified.
                  </div>
                </div>
              )}

              {/* 4 Core Soil Measurements */}
              {plantSensorData.latestReading && (
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                  {/* Soil Moisture */}
                  <div className="card p-5 space-y-2 bg-white">
                    <div className="flex items-center justify-between text-gray-500 text-xs font-semibold">
                      <span className="flex items-center gap-1.5">
                        <Droplet className="w-4 h-4 text-sky-500" /> Soil Moisture
                      </span>
                      <span className={`font-bold ${
                        plantSensorData.latestReading.soilMoisture < 30 ? 'text-rose-600' : 'text-emerald-600'
                      }`}>
                        {plantSensorData.latestReading.soilMoisture < 30 ? 'Low' : 'Healthy'}
                      </span>
                    </div>
                    <div className="text-3xl font-black text-gray-900">
                      {plantSensorData.latestReading.soilMoisture?.toFixed(1)}%
                    </div>
                    <div className="w-full bg-gray-100 rounded-full h-2 overflow-hidden">
                      <div
                        className="bg-sky-500 h-2 rounded-full transition-all duration-500"
                        style={{ width: `${Math.min(100, plantSensorData.latestReading.soilMoisture || 0)}%` }}
                      />
                    </div>
                  </div>

                  {/* Soil Temperature */}
                  <div className="card p-5 space-y-2 bg-white">
                    <div className="flex items-center justify-between text-gray-500 text-xs font-semibold">
                      <span className="flex items-center gap-1.5">
                        <Thermometer className="w-4 h-4 text-rose-500" /> Soil Temp
                      </span>
                      <span className="text-emerald-600 font-bold">Optimal</span>
                    </div>
                    <div className="text-3xl font-black text-gray-900">
                      {plantSensorData.latestReading.soilTemperature?.toFixed(1)}°C
                    </div>
                    <div className="text-xs text-gray-400">Target range: 18 - 25°C</div>
                  </div>

                  {/* Soil EC */}
                  <div className="card p-5 space-y-2 bg-white">
                    <div className="flex items-center justify-between text-gray-500 text-xs font-semibold">
                      <span className="flex items-center gap-1.5">
                        <Zap className="w-4 h-4 text-amber-500" /> Conductivity (EC)
                      </span>
                      <span className="text-gray-600 font-bold">
                        {plantSensorData.latestReading.soilEc > 2.0 ? 'High Salts' : 'Normal'}
                      </span>
                    </div>
                    <div className="text-3xl font-black text-gray-900">
                      {plantSensorData.latestReading.soilEc?.toFixed(2)} <span className="text-sm font-normal text-gray-500">mS/cm</span>
                    </div>
                    <div className="text-xs text-gray-400">Dissolved nutrient conductivity</div>
                  </div>

                  {/* Soil pH */}
                  <div className="card p-5 space-y-2 bg-white">
                    <div className="flex items-center justify-between text-gray-500 text-xs font-semibold">
                      <span className="flex items-center gap-1.5">
                        <TestTube className="w-4 h-4 text-purple-500" /> Substrate pH
                      </span>
                      <span className="text-emerald-600 font-bold">Balanced</span>
                    </div>
                    <div className="text-3xl font-black text-gray-900">
                      {plantSensorData.latestReading.soilPh?.toFixed(1)}
                    </div>
                    <div className="text-xs text-gray-400">Target range: 6.0 - 7.0 pH</div>
                  </div>
                </div>
              )}

              {/* Ambient Sensors */}
              {plantSensorData.latestReading && (
                <div className="card p-6 bg-white space-y-4">
                  <h3 className="text-sm font-bold text-gray-800 uppercase tracking-wider">
                    Micro-Ambient Conditions Surrounding Canopy
                  </h3>
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs">
                    <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-100 flex items-center justify-between">
                      <span className="text-gray-500">Ambient Air Temp</span>
                      <span className="text-base font-bold text-gray-800">{plantSensorData.latestReading.ambientTemperature}°C</span>
                    </div>
                    <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-100 flex items-center justify-between">
                      <span className="text-gray-500">Relative Humidity</span>
                      <span className="text-base font-bold text-gray-800">{plantSensorData.latestReading.ambientHumidity}%</span>
                    </div>
                    <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-100 flex items-center justify-between">
                      <span className="text-gray-500">Light Intensity</span>
                      <span className="text-base font-bold text-gray-800">{plantSensorData.latestReading.lightLux} lux</span>
                    </div>
                  </div>
                </div>
              )}

              {/* Interpretation card */}
              {plantSensorData.interpretation && (
                <div className="card p-6 bg-emerald-50/50 border border-emerald-100 space-y-3">
                  <div className="flex items-center gap-2 text-emerald-800 font-bold text-sm">
                    <ShieldCheck className="w-5 h-5 text-emerald-600" /> Agronomic Telemetry Interpretation
                  </div>
                  <p className="text-xs text-emerald-950 leading-relaxed">
                    {plantSensorData.interpretation.summary || JSON.stringify(plantSensorData.interpretation)}
                  </p>
                </div>
              )}
            </div>
          ) : null}

          {/* Registered Devices List */}
          <div className="card p-6 bg-white space-y-4">
            <div className="flex items-center justify-between border-b border-gray-100 pb-3">
              <h3 className="text-base font-bold text-gray-800">Your IoT Sensor Rod Fleet</h3>
              <span className="text-xs text-gray-500">{devices.length} registered hardware units</span>
            </div>

            {devices.length === 0 ? (
              <div className="text-center py-6 text-xs text-gray-400">
                No sensor devices registered yet. Click &quot;Register Sensor Rod&quot; above to add one.
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {devices.map((d) => (
                  <div key={d.deviceId} className="p-4 rounded-2xl border border-gray-100 bg-slate-50 space-y-3">
                    <div className="flex items-center justify-between">
                      <span className="font-bold text-gray-800 text-sm">{d.name}</span>
                      <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                        d.status === 'ONLINE' ? 'bg-emerald-100 text-emerald-800' : 'bg-gray-200 text-gray-700'
                      }`}>
                        {d.status}
                      </span>
                    </div>
                    <div className="text-xs text-gray-500 space-y-1">
                      <div>Hardware ID: <code className="font-mono text-gray-700">{d.deviceId}</code></div>
                      <div>Firmware: {d.firmwareVersion}</div>
                      <div>
                        Assigned to: {d.plantName ? <span className="font-bold text-emerald-700">{d.plantName}</span> : <span className="italic text-gray-400">Unpaired</span>}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </main>
      </div>

      {/* Modal: Register Sensor Rod */}
      {showRegisterModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4">
          <div className="bg-white rounded-3xl p-6 max-w-md w-full space-y-5 shadow-2xl animate-fade-in">
            <h3 className="text-lg font-bold text-gray-900">Register ESP32 / ESP8266 Sensor Rod</h3>
            <form onSubmit={handleRegisterDevice} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">Device Hardware ID *</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. ROD-ESP32-84F3"
                  value={newDeviceId}
                  onChange={(e) => setNewDeviceId(e.target.value)}
                  className="w-full px-4 py-2.5 border border-gray-200 rounded-xl text-sm"
                />
              </div>
              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">Friendly Name</label>
                <input
                  type="text"
                  placeholder="e.g. Garden Rod #1"
                  value={newDeviceName}
                  onChange={(e) => setNewDeviceName(e.target.value)}
                  className="w-full px-4 py-2.5 border border-gray-200 rounded-xl text-sm"
                />
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowRegisterModal(false)}
                  className="btn-secondary text-xs"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={registering}
                  className="btn-primary text-xs"
                >
                  {registering ? 'Registering...' : 'Register Device'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Simulate / Ingest Reading */}
      {showSimulateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4">
          <div className="bg-white rounded-3xl p-6 max-w-lg w-full space-y-5 shadow-2xl animate-fade-in max-h-[90vh] overflow-y-auto">
            <h3 className="text-lg font-bold text-gray-900">Send Test Telemetry Payload</h3>
            <p className="text-xs text-gray-500">
              Simulate an HTTP reading payload from an ESP32 microcontroller to test the live health dashboard.
            </p>
            <form onSubmit={handleSimulate} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">Select Target Device *</label>
                <select
                  required
                  value={simForm.deviceId}
                  onChange={(e) => setSimForm({ ...simForm, deviceId: e.target.value })}
                  className="w-full px-4 py-2.5 border border-gray-200 rounded-xl text-sm"
                >
                  <option value="">Select registered rod</option>
                  {devices.map((d) => (
                    <option key={d.deviceId} value={d.deviceId}>
                      {d.name} ({d.deviceId})
                    </option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-3 text-xs">
                <div>
                  <label className="block font-bold text-gray-700 mb-1">Soil Moisture (%)</label>
                  <input
                    type="number"
                    step="0.1"
                    value={simForm.soilMoisture}
                    onChange={(e) => setSimForm({ ...simForm, soilMoisture: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-200 rounded-xl"
                  />
                </div>
                <div>
                  <label className="block font-bold text-gray-700 mb-1">Soil Temp (°C)</label>
                  <input
                    type="number"
                    step="0.1"
                    value={simForm.soilTemperature}
                    onChange={(e) => setSimForm({ ...simForm, soilTemperature: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-200 rounded-xl"
                  />
                </div>
                <div>
                  <label className="block font-bold text-gray-700 mb-1">Soil EC (mS/cm)</label>
                  <input
                    type="number"
                    step="0.05"
                    value={simForm.soilEc}
                    onChange={(e) => setSimForm({ ...simForm, soilEc: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-200 rounded-xl"
                  />
                </div>
                <div>
                  <label className="block font-bold text-gray-700 mb-1">Soil pH</label>
                  <input
                    type="number"
                    step="0.1"
                    value={simForm.soilPh}
                    onChange={(e) => setSimForm({ ...simForm, soilPh: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-200 rounded-xl"
                  />
                </div>
                <div>
                  <label className="block font-bold text-gray-700 mb-1">Ambient Air Temp (°C)</label>
                  <input
                    type="number"
                    step="0.1"
                    value={simForm.ambientTemperature}
                    onChange={(e) => setSimForm({ ...simForm, ambientTemperature: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-200 rounded-xl"
                  />
                </div>
                <div>
                  <label className="block font-bold text-gray-700 mb-1">Ambient Humidity (%)</label>
                  <input
                    type="number"
                    step="0.1"
                    value={simForm.ambientHumidity}
                    onChange={(e) => setSimForm({ ...simForm, ambientHumidity: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-200 rounded-xl"
                  />
                </div>
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowSimulateModal(false)}
                  className="btn-secondary text-xs"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={simulating}
                  className="btn-primary text-xs"
                >
                  {simulating ? 'Ingesting...' : 'Ingest Telemetry'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
