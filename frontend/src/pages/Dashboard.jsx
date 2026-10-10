import React, { useEffect, useState, useContext } from 'react'
import { useNavigate } from 'react-router-dom'
import { dashboardApi, weatherApi } from '../services/apiService'
import Navbar from '../components/Navbar'
import Sidebar from '../components/Sidebar'
import WeatherCard from '../components/WeatherCard'
import ReminderCard from '../components/ReminderCard'
import PlantCard from '../components/PlantCard'
import StatsCard from '../components/StatsCard'
import ActivityTimeline from '../components/ActivityTimeline'
import Loader from '../components/Loader'
import { AuthContext } from '../contexts/AuthContext'
import { Sprout, Droplets, Calendar, TrendingUp } from 'lucide-react'

export default function Dashboard() {
  const [dashboard, setDashboard] = useState(null)
  const [weather, setWeather] = useState(null)
  const { user } = useContext(AuthContext)
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true)
        const res = await dashboardApi.getDashboard()
        setDashboard(res.data)
        if (user?.city) {
          const w = await weatherApi.getWeatherByCity(user.city)
          setWeather(w.data)
        }
      } catch (err) {
        console.error(err)
      } finally { setLoading(false) }
    }
    load()
  }, [user?.city])

  if (loading) return <Loader />

  const stats = [
    {
      title: 'Total Plants',
      value: dashboard?.stats?.totalPlants ?? 0,
      change: 0,
      icon: Sprout,
      color: 'primary'
    },
    {
      title: 'Plants Watered Today',
      value: dashboard?.stats?.plantsWateredToday ?? 0,
      change: 0,
      icon: Droplets,
      color: 'sky'
    },
    {
      title: 'Due for Watering',
      value: dashboard?.stats?.plantsDueForWatering ?? 0,
      change: 0,
      icon: Calendar,
      color: 'accent'
    },
    {
      title: 'Health Score',
      value: `${dashboard?.stats?.healthScore ?? 0}%`,
      change: 0,
      icon: TrendingUp,
      color: 'sage'
    }
  ]

  const activities = dashboard?.recentlyWatered?.map((entry) => ({
    type: 'watering',
    description: `Watered ${entry.plantName}`,
    timestamp: entry.wateredDate
  })) || [
    {
      type: 'reminder',
      description: 'You are ready to add a new plant with AI guidance.',
      timestamp: 'Today'
    }
  ]

  return (
    <div className="min-h-screen bg-gradient-to-br from-bg via-soft to-sage/20">
      <Navbar />
      <div className="flex max-w-7xl mx-auto px-6">
        <Sidebar />
        <main className="flex-1 py-6 space-y-8">
          <div className="animate-fade-in">
            <h1 className="text-3xl font-bold text-gray-900 mb-2">
              Welcome back, {user?.firstName || 'Plant Lover'}! 🌱
            </h1>
            <p className="text-gray-600">Here's what's happening with your plants today.</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {stats.map((stat, index) => (
              <StatsCard key={index} {...stat} />
            ))}
          </div>

          {/* Groq Intelligence Quick Actions */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <button
              onClick={() => navigate('/identify')}
              className="p-5 rounded-2xl bg-white border border-emerald-100 hover:border-emerald-400 shadow-sm hover:shadow-md transition-all text-left group"
            >
              <div className="text-xs font-bold text-emerald-700 uppercase tracking-wider mb-1 flex items-center justify-between">
                <span>AI Photo Workflow</span>
                <span className="text-emerald-500 group-hover:translate-x-1 transition-transform">&rarr;</span>
              </div>
              <div className="text-base font-bold text-gray-900">Identify Plant & Anatomy</div>
              <p className="text-xs text-gray-500 mt-1">
                Upload a photograph to identify taxonomy and synthesize botanical anatomy.
              </p>
            </button>

            <button
              onClick={() => navigate('/health-monitoring')}
              className="p-5 rounded-2xl bg-white border border-sky-100 hover:border-sky-400 shadow-sm hover:shadow-md transition-all text-left group"
            >
              <div className="text-xs font-bold text-sky-700 uppercase tracking-wider mb-1 flex items-center justify-between">
                <span>ESP32 / ESP8266</span>
                <span className="text-sky-500 group-hover:translate-x-1 transition-transform">&rarr;</span>
              </div>
              <div className="text-base font-bold text-gray-900">IoT Sensor Rods</div>
              <p className="text-xs text-gray-500 mt-1">
                Monitor real-time soil moisture, EC, pH, and canopy temperature telemetry.
              </p>
            </button>

            <button
              onClick={() => navigate('/assistant')}
              className="p-5 rounded-2xl bg-white border border-purple-100 hover:border-purple-400 shadow-sm hover:shadow-md transition-all text-left group"
            >
              <div className="text-xs font-bold text-purple-700 uppercase tracking-wider mb-1 flex items-center justify-between">
                <span>Groq LLM</span>
                <span className="text-purple-500 group-hover:translate-x-1 transition-transform">&rarr;</span>
              </div>
              <div className="text-base font-bold text-gray-900">AI Plant Assistant</div>
              <p className="text-xs text-gray-500 mt-1">
                Natural-language Q&A contextualized with your plant&apos;s sensors and forecast.
              </p>
            </button>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
            <div className="lg:col-span-2 space-y-6">
              <div className="card p-6">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-xl font-semibold text-gray-800 flex items-center gap-2">
                    <Calendar className="w-6 h-6 text-primary-600" />
                    Upcoming Tasks
                  </h3>
                  <button
                    onClick={() => navigate('/plants')}
                    className="text-sm font-semibold text-primary-600 hover:text-primary-800"
                  >
                    View all plants
                  </button>
                </div>
                <div className="space-y-3">
                  {dashboard?.upcomingTasks?.length ? (
                    dashboard.upcomingTasks.map((task) => (
                      <ReminderCard
                        key={task.plantId}
                        task={task}
                        onClick={() => navigate(`/plants/${task.plantId}`)}
                      />
                    ))
                  ) : (
                    <div className="text-center py-8 text-gray-500">
                      <Calendar className="w-12 h-12 mx-auto mb-4 text-gray-300" />
                      <p>No upcoming tasks. Your plants are well cared for! 🎉</p>
                    </div>
                  )}
                </div>
              </div>

              <div className="card p-6">
                <h3 className="text-xl font-semibold text-gray-800 mb-4 flex items-center gap-2">
                  <Droplets className="w-6 h-6 text-sky-600" />
                  Recently Watered
                </h3>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {dashboard?.recentlyWatered?.length ? (
                    dashboard.recentlyWatered.map((plant) => (
                      <PlantCard
                        key={plant.plantId}
                        plant={{
                          id: plant.plantId,
                          name: plant.plantName,
                          plantType: plant.plantType || 'Plant',
                          lastWateredDate: plant.wateredDate
                        }}
                        onEdit={() => navigate(`/plants/${plant.plantId}`)}
                        onDelete={() => {}}
                        onWater={() => {}}
                      />
                    ))
                  ) : (
                    <div className="col-span-full text-center py-8 text-gray-500">
                      <Droplets className="w-12 h-12 mx-auto mb-4 text-gray-300" />
                      <p>No recent watering records.</p>
                    </div>
                  )}
                </div>
              </div>
            </div>

            <div className="space-y-6">
              <WeatherCard weather={weather} />
              <ActivityTimeline activities={activities} />
            </div>
          </div>
        </main>
      </div>
    </div>
  )
}
