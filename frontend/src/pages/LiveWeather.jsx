import React, { useEffect, useState, useContext } from 'react'
import Navbar from '../components/Navbar'
import Sidebar from '../components/Sidebar'
import Loader from '../components/Loader'
import { weatherApi, plantsApi } from '../services/apiService'
import { AuthContext } from '../contexts/AuthContext'
import {
  CloudSun,
  MapPin,
  Wind,
  Droplets,
  Compass,
  Calendar,
  Clock,
  RefreshCw,
  Search,
  ArrowUpRight,
  Sprout
} from 'lucide-react'

export default function LiveWeather() {
  const { user } = useContext(AuthContext)
  const [cityInput, setCityInput] = useState('')
  const [currentCity, setCurrentCity] = useState(user?.city || 'London')
  const [weatherData, setWeatherData] = useState(null)
  const [plants, setPlants] = useState([])
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [errorMsg, setErrorMsg] = useState(null)

  useEffect(() => {
    loadAll()
  }, [currentCity])

  const loadAll = async () => {
    try {
      setLoading(true)
      setErrorMsg(null)
      const [wRes, pRes] = await Promise.all([
        weatherApi.getWeatherByCity(currentCity),
        plantsApi.getPlants()
      ])
      setWeatherData(wRes.data)
      setPlants(pRes.data || [])
    } catch (err) {
      console.error('Weather load error:', err)
      setErrorMsg('Could not retrieve weather for this location.')
    } finally {
      setLoading(false)
    }
  }

  const handleSearch = (e) => {
    e.preventDefault()
    if (cityInput.trim()) {
      setCurrentCity(cityInput.trim())
    }
  }

  const handleRefresh = async () => {
    try {
      setRefreshing(true)
      const res = await weatherApi.getWeatherByCity(currentCity)
      setWeatherData(res.data)
    } catch (err) {
      console.error(err)
    } finally {
      setRefreshing(false)
    }
  }

  const useBrowserLocation = () => {
    if (!navigator.geolocation) {
      alert('Geolocation is not supported by your browser.')
      return
    }
    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        try {
          setLoading(true)
          // Open-Meteo reverse geocoding via city API or direct fetch
          setCurrentCity(`${pos.coords.latitude.toFixed(2)}, ${pos.coords.longitude.toFixed(2)}`)
        } catch (e) {
          console.error(e)
        } finally {
          setLoading(false)
        }
      },
      (err) => {
        alert('Location permission denied or unavailable.')
      }
    )
  }

  if (loading) return <Loader />

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#F4F7F4] via-[#EBF2EB] to-[#DCEADC]/40">
      <Navbar />
      <div className="flex max-w-7xl mx-auto px-6">
        <Sidebar />
        <main className="flex-1 py-6 space-y-8">
          {/* Header */}
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 animate-fade-in">
            <div>
              <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-sky-100 text-sky-800 text-xs font-semibold uppercase tracking-wider mb-2">
                <CloudSun className="w-3.5 h-3.5 text-sky-600" />
                Live Meteorological Telemetry
              </div>
              <h1 className="text-3xl font-bold text-gray-900 tracking-tight">Real-Time Climate & Forecast</h1>
              <p className="text-gray-600 text-sm mt-1">
                Precision weather data from Open-Meteo powering intelligent watering schedules.
              </p>
            </div>

            <div className="flex items-center gap-3">
              <button
                onClick={useBrowserLocation}
                className="btn-secondary text-xs flex items-center gap-2"
                title="Use Current GPS Coordinates"
              >
                <MapPin className="w-4 h-4 text-emerald-600" /> GPS Location
              </button>
              <button
                onClick={handleRefresh}
                disabled={refreshing}
                className="btn-secondary text-xs flex items-center gap-2"
              >
                <RefreshCw className={`w-4 h-4 text-gray-600 ${refreshing ? 'animate-spin' : ''}`} />
                {refreshing ? 'Refreshing...' : 'Refresh'}
              </button>
            </div>
          </div>

          {/* Search bar */}
          <form onSubmit={handleSearch} className="flex gap-3 max-w-md">
            <div className="relative flex-1">
              <Search className="w-4 h-4 absolute left-3.5 top-3.5 text-gray-400" />
              <input
                type="text"
                placeholder="Search city (e.g. Paris, Tokyo, Seattle)..."
                value={cityInput}
                onChange={(e) => setCityInput(e.target.value)}
                className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-gray-200 text-sm focus:ring-2 focus:ring-emerald-500 bg-white"
              />
            </div>
            <button type="submit" className="btn-primary text-sm px-5 py-2.5">Search</button>
          </form>

          {weatherData && (
            <div className="space-y-8 animate-fade-in">
              {/* Primary Current Weather Banner */}
              <div className="card p-8 bg-gradient-to-r from-emerald-800 to-teal-900 text-white shadow-xl relative overflow-hidden">
                <div className="relative z-10 grid grid-cols-1 md:grid-cols-12 gap-8 items-center">
                  <div className="md:col-span-6 space-y-3">
                    <div className="flex items-center gap-2 text-emerald-200 text-sm font-semibold">
                      <MapPin className="w-4 h-4" /> {weatherData.city}
                      {weatherData.lastUpdated && (
                        <span className="text-xs text-emerald-300 ml-2">
                          &bull; Updated {new Date(weatherData.lastUpdated).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </span>
                      )}
                    </div>
                    <div className="flex items-baseline gap-4">
                      <span className="text-6xl font-black tracking-tight">{Math.round(weatherData.temperature)}°C</span>
                      <span className="text-xl text-emerald-100 font-medium">{weatherData.weatherDescription}</span>
                    </div>
                    <p className="text-xs text-emerald-100 max-w-md leading-relaxed bg-white/10 p-3 rounded-xl backdrop-blur-sm">
                      {weatherData.careTip}
                    </p>
                  </div>

                  <div className="md:col-span-6 grid grid-cols-2 sm:grid-cols-3 gap-4 text-xs">
                    <div className="bg-white/10 backdrop-blur-sm p-3.5 rounded-2xl border border-white/10">
                      <div className="flex items-center gap-1.5 text-emerald-200 mb-1">
                        <Droplets className="w-4 h-4 text-sky-300" /> Humidity
                      </div>
                      <div className="text-xl font-bold">{weatherData.humidity}%</div>
                    </div>
                    <div className="bg-white/10 backdrop-blur-sm p-3.5 rounded-2xl border border-white/10">
                      <div className="flex items-center gap-1.5 text-emerald-200 mb-1">
                        <Wind className="w-4 h-4 text-teal-300" /> Wind Speed
                      </div>
                      <div className="text-xl font-bold">{weatherData.windSpeed || 0} km/h</div>
                    </div>
                    <div className="bg-white/10 backdrop-blur-sm p-3.5 rounded-2xl border border-white/10">
                      <div className="flex items-center gap-1.5 text-emerald-200 mb-1">
                        <CloudSun className="w-4 h-4 text-amber-300" /> Precip. Prob.
                      </div>
                      <div className="text-xl font-bold">{weatherData.precipitationProbability || 0}%</div>
                    </div>
                  </div>
                </div>
              </div>

              {/* 24-Hour Forecast Timeline */}
              {weatherData.hourlyForecast && weatherData.hourlyForecast.length > 0 && (
                <div className="card p-6 space-y-4">
                  <div className="flex items-center gap-2 text-base font-bold text-gray-800">
                    <Clock className="w-5 h-5 text-emerald-600" /> Hourly Forecast (Next 24 Hours)
                  </div>
                  <div className="flex gap-4 overflow-x-auto pb-3 pt-1 scrollbar-thin">
                    {weatherData.hourlyForecast.slice(0, 16).map((h, idx) => (
                      <div key={idx} className="flex-shrink-0 w-24 p-3 rounded-2xl bg-slate-50 border border-slate-100 text-center space-y-2 hover:bg-emerald-50 transition-colors">
                        <div className="text-xs font-semibold text-gray-500">{h.time}</div>
                        <div className="text-base font-bold text-gray-800">{Math.round(h.temperature)}°C</div>
                        <div className="text-[11px] text-sky-600 flex items-center justify-center gap-1">
                          <Droplets className="w-3 h-3" /> {h.precipitationProbability}%
                        </div>
                        <div className="text-[10px] text-gray-500 truncate">{h.condition}</div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* 7-Day Extended Forecast */}
              {weatherData.dailyForecast && weatherData.dailyForecast.length > 0 && (
                <div className="card p-6 space-y-4">
                  <div className="flex items-center gap-2 text-base font-bold text-gray-800">
                    <Calendar className="w-5 h-5 text-emerald-600" /> 7-Day Extended Agricultural Outlook
                  </div>
                  <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-7 gap-3">
                    {weatherData.dailyForecast.map((d, idx) => (
                      <div key={idx} className="p-3.5 rounded-2xl bg-slate-50 border border-slate-100 text-center space-y-1.5">
                        <div className="text-xs font-bold text-gray-600">
                          {new Date(d.date).toLocaleDateString([], { weekday: 'short' })}
                        </div>
                        <div className="text-xs text-gray-400">{d.date.substring(5)}</div>
                        <div className="text-sm font-bold text-gray-800">
                          {Math.round(d.maxTemp)}° / <span className="text-gray-500 font-normal">{Math.round(d.minTemp)}°</span>
                        </div>
                        <div className="text-[11px] text-sky-600">{d.precipitationProbability}% rain</div>
                        <div className="text-[11px] text-gray-500 truncate">{d.condition}</div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Plants Independent Locations Section */}
              <div className="card p-6 space-y-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2 text-base font-bold text-gray-800">
                    <Sprout className="w-5 h-5 text-emerald-600" /> Your Plants & Location Environments
                  </div>
                  <div className="text-xs text-gray-500">{plants.length} Plants in Garden</div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                  {plants.map((p) => (
                    <div key={p.id} className="p-4 rounded-2xl border border-gray-100 bg-white hover:shadow-md transition-shadow space-y-2">
                      <div className="flex items-center justify-between">
                        <div className="font-bold text-gray-900">{p.name}</div>
                        <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${p.isIndoor ? 'bg-purple-50 text-purple-700' : 'bg-emerald-50 text-emerald-700'}`}>
                          {p.isIndoor ? 'Indoor' : 'Outdoor'}
                        </span>
                      </div>
                      <div className="text-xs text-gray-500 flex items-center gap-1">
                        <MapPin className="w-3.5 h-3.5 text-gray-400" /> Location: {p.location}
                      </div>
                      <div className="text-xs text-gray-600 bg-slate-50 p-2.5 rounded-xl">
                        {p.isIndoor 
                          ? 'Shielded from wind & rain; adjusted for ambient indoor temperature.'
                          : 'Exposed to active precipitation and seasonal evapotranspiration.'}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  )
}
