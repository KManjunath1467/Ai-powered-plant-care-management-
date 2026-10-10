import React, { useState, useContext, useEffect } from 'react'
import { userApi } from '../services/apiService'
import { AuthContext } from '../contexts/AuthContext'
import Navbar from '../components/Navbar'
import Sidebar from '../components/Sidebar'
import Loader from '../components/Loader'
import { User, Bell, Shield, Palette, Save, MapPin } from 'lucide-react'
import { toast } from 'react-toastify'

export default function ProfileSettings() {
  const { user, updateUser } = useContext(AuthContext)
  const [loading, setLoading] = useState(false)
  const [profileForm, setProfileForm] = useState({
    firstName: user?.firstName || '',
    lastName: user?.lastName || '',
    email: user?.email || '',
    city: user?.city || '',
    phoneNumber: user?.phoneNumber || ''
  })

  const [preferences, setPreferences] = useState({
    emailNotifications: true,
    whatsappNotifications: true,
    pushNotifications: false,
    weatherAlerts: true,
    careReminders: true,
    theme: 'light'
  })

  useEffect(() => {
    // Load user preferences (mock data for now)
    setPreferences({
      emailNotifications: true,
      whatsappNotifications: true,
      pushNotifications: false,
      weatherAlerts: true,
      careReminders: true,
      theme: 'light'
    })
  }, [])

  const handleProfileUpdate = async (e) => {
    e.preventDefault()
    setLoading(true)
    try {
      await userApi.updateProfile(user.id, profileForm)
      updateUser(profileForm)
      toast.success('Profile updated successfully')
    } catch (err) {
      toast.error('Failed to update profile')
      console.error('Profile update error:', err)
    } finally {
      setLoading(false)
    }
  }

  const handlePreferencesUpdate = async () => {
    setLoading(true)
    try {
      await userApi.updatePreferences(user.id, preferences)
      toast.success('Preferences updated successfully')
    } catch (err) {
      toast.error('Failed to update preferences')
      console.error('Preferences update error:', err)
    } finally {
      setLoading(false)
    }
  }

  const handleCityUpdate = async () => {
    setLoading(true)
    try {
      await userApi.updateCity(user.id, { city: profileForm.city })
      updateUser({ city: profileForm.city })
      toast.success('Location updated successfully')
    } catch (err) {
      toast.error('Failed to update location')
      console.error('City update error:', err)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-bg via-soft to-sage/20">
      <Navbar />
      <div className="flex max-w-7xl mx-auto px-6">
        <Sidebar />
        <main className="flex-1 py-6">
          <div className="max-w-4xl mx-auto">
            {/* Header */}
            <div className="mb-8">
              <h1 className="text-3xl font-bold text-gray-900 mb-2">Profile & Settings</h1>
              <p className="text-gray-600">Manage your account and preferences</p>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
              {/* Profile Information */}
              <div className="lg:col-span-2 space-y-6">
                <div className="card p-6">
                  <h3 className="text-xl font-semibold text-gray-800 mb-6 flex items-center gap-2">
                    <User className="w-5 h-5 text-primary-600" />
                    Profile Information
                  </h3>

                  <form onSubmit={handleProfileUpdate} className="space-y-4">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                          First Name
                        </label>
                        <input
                          type="text"
                          value={profileForm.firstName}
                          onChange={(e) => setProfileForm({...profileForm, firstName: e.target.value})}
                          className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-all"
                        />
                      </div>

                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                          Last Name
                        </label>
                        <input
                          type="text"
                          value={profileForm.lastName}
                          onChange={(e) => setProfileForm({...profileForm, lastName: e.target.value})}
                          className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-all"
                        />
                      </div>
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">
                        Email
                      </label>
                      <input
                        type="email"
                        value={profileForm.email}
                        onChange={(e) => setProfileForm({...profileForm, email: e.target.value})}
                        className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-all"
                      />
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2 flex items-center gap-2">
                          <MapPin className="w-4 h-4" />
                          City
                        </label>
                        <input
                          type="text"
                          value={profileForm.city}
                          onChange={(e) => setProfileForm({...profileForm, city: e.target.value})}
                          className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-all"
                          placeholder="Your city for weather data"
                        />
                      </div>

                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                          Phone Number
                        </label>
                        <input
                          type="tel"
                          value={profileForm.phoneNumber}
                          onChange={(e) => setProfileForm({...profileForm, phoneNumber: e.target.value})}
                          className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-all"
                        />
                      </div>
                    </div>

                    <div className="flex gap-4 pt-4">
                      <button
                        type="submit"
                        disabled={loading}
                        className="btn-primary flex items-center gap-2"
                      >
                        <Save className="w-4 h-4" />
                        {loading ? 'Saving...' : 'Save Profile'}
                      </button>

                      <button
                        type="button"
                        onClick={handleCityUpdate}
                        disabled={loading}
                        className="btn-secondary flex items-center gap-2"
                      >
                        <MapPin className="w-4 h-4" />
                        Update Location
                      </button>
                    </div>
                  </form>
                </div>

                {/* Notification Preferences */}
                <div className="card p-6">
                  <h3 className="text-xl font-semibold text-gray-800 mb-6 flex items-center gap-2">
                    <Bell className="w-5 h-5 text-accent" />
                    Notification Preferences
                  </h3>

                  <div className="space-y-4">
                    {[
                      { key: 'emailNotifications', label: 'Email Notifications', desc: 'Receive care reminders via email' },
                      { key: 'whatsappNotifications', label: 'WhatsApp Notifications', desc: 'Get instant reminders on WhatsApp' },
                      { key: 'pushNotifications', label: 'Push Notifications', desc: 'Browser push notifications' },
                      { key: 'weatherAlerts', label: 'Weather Alerts', desc: 'Notifications about weather affecting plant care' },
                      { key: 'careReminders', label: 'Care Reminders', desc: 'Regular watering and care reminders' }
                    ].map(({ key, label, desc }) => (
                      <div key={key} className="flex items-center justify-between p-4 bg-gray-50 rounded-xl">
                        <div>
                          <div className="font-medium text-gray-900">{label}</div>
                          <div className="text-sm text-gray-600">{desc}</div>
                        </div>
                        <label className="relative inline-flex items-center cursor-pointer">
                          <input
                            type="checkbox"
                            checked={preferences[key]}
                            onChange={(e) => setPreferences({...preferences, [key]: e.target.checked})}
                            className="sr-only peer"
                          />
                          <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-primary-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-600"></div>
                        </label>
                      </div>
                    ))}

                    <div className="pt-4">
                      <button
                        onClick={handlePreferencesUpdate}
                        disabled={loading}
                        className="btn-primary w-full flex items-center justify-center gap-2"
                      >
                        <Save className="w-4 h-4" />
                        {loading ? 'Saving...' : 'Save Preferences'}
                      </button>
                    </div>
                  </div>
                </div>

                {/* Groq Cloud LLM Configuration & Status */}
                <div className="card p-6 bg-white space-y-4">
                  <div className="flex items-center justify-between border-b border-gray-100 pb-3">
                    <h3 className="text-lg font-semibold text-gray-800 flex items-center gap-2">
                      <Shield className="w-5 h-5 text-emerald-600" />
                      Groq Cloud LLM Integration
                    </h3>
                    <span className="px-3 py-1 rounded-full text-xs font-bold bg-emerald-50 text-emerald-700">
                      Primary AI Provider
                    </span>
                  </div>

                  <div className="p-4 rounded-2xl bg-slate-50 border border-slate-100 space-y-2 text-xs">
                    <div className="flex items-center justify-between">
                      <span className="text-gray-500 font-semibold">Active Model:</span>
                      <code className="font-mono text-emerald-800 font-bold bg-white px-2 py-0.5 rounded border border-gray-200">
                        llama-3.3-70b-versatile
                      </code>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-gray-500 font-semibold">Key Storage:</span>
                      <span className="text-emerald-700 font-semibold">Server-Side Environment Variable Only</span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-gray-500 font-semibold">Fallback Protection:</span>
                      <span className="text-gray-700">Deterministic Botanical Rule Engine</span>
                    </div>
                  </div>

                  <div className="text-xs text-gray-600 space-y-2 leading-relaxed bg-emerald-50/50 p-4 rounded-2xl border border-emerald-100">
                    <div className="font-bold text-emerald-900">How to configure your Groq API Key:</div>
                    <ol className="list-decimal list-inside space-y-1 text-emerald-950">
                      <li>Obtain a free API key from the official <strong>Groq Console</strong> at <code>https://console.groq.com/keys</code>.</li>
                      <li>In your backend environment (or <code>.env</code> file), specify:</li>
                    </ol>
                    <pre className="p-2.5 rounded-xl bg-slate-900 text-emerald-400 font-mono text-[11px] overflow-x-auto">
GROQ_API_KEY=gsk_your_actual_key_here
GROQ_MODEL=llama-3.3-70b-versatile
                    </pre>
                    <p className="text-[11px] text-gray-500 italic">
                      Security guarantee: The API key remains strictly server-side and is never returned in client network payloads.
                    </p>
                  </div>
                </div>
              </div>

              {/* Sidebar */}
              <div className="space-y-6">
                {/* Account Security */}
                <div className="card p-6">
                  <h3 className="text-lg font-semibold text-gray-800 mb-4 flex items-center gap-2">
                    <Shield className="w-5 h-5 text-red-600" />
                    Account Security
                  </h3>

                  <div className="space-y-3">
                    <button className="w-full btn-secondary text-left">
                      Change Password
                    </button>
                    <button className="w-full btn-secondary text-left">
                      Two-Factor Authentication
                    </button>
                    <button className="w-full text-red-600 hover:bg-red-50 px-4 py-2 rounded-xl transition-colors text-left">
                      Delete Account
                    </button>
                  </div>
                </div>

                {/* Theme Settings */}
                <div className="card p-6">
                  <h3 className="text-lg font-semibold text-gray-800 mb-4 flex items-center gap-2">
                    <Palette className="w-5 h-5 text-purple-600" />
                    Appearance
                  </h3>

                  <div className="space-y-3">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">
                        Theme
                      </label>
                      <select
                        value={preferences.theme}
                        onChange={(e) => setPreferences({...preferences, theme: e.target.value})}
                        className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-primary-500 focus:border-transparent transition-all"
                      >
                        <option value="light">Light</option>
                        <option value="dark">Dark</option>
                        <option value="auto">Auto</option>
                      </select>
                    </div>
                  </div>
                </div>

                {/* Account Stats */}
                <div className="card p-6">
                  <h3 className="text-lg font-semibold text-gray-800 mb-4">Account Statistics</h3>

                  <div className="space-y-3">
                    <div className="flex justify-between">
                      <span className="text-gray-600">Plants Added</span>
                      <span className="font-semibold">12</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600">Watering Records</span>
                      <span className="font-semibold">47</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600">Member Since</span>
                      <span className="font-semibold">Jan 2024</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  )
}