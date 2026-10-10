import axios from '../api/axiosInstance'

export const authApi = {
  login: (payload) => axios.post('/auth/login', payload),
  register: (payload) => axios.post('/auth/register', payload)
}

export const userApi = {
  getProfile: () => axios.get('/users/profile'),
  updatePreferences: (id, payload) => axios.put(`/users/${id}/preferences`, payload),
  updateCity: (id, payload) => axios.put(`/users/${id}/city`, payload)
}

export const plantsApi = {
  getPlants: () => axios.get('/plants'),
  getPlant: (id) => axios.get(`/plants/${id}`),
  createPlant: (payload) => axios.post('/plants', payload),
  updatePlant: (id, payload) => axios.put(`/plants/${id}`, payload),
  deletePlant: (id) => axios.delete(`/plants/${id}`),
  markWatered: (id) => axios.post(`/plants/${id}/water`),
  uploadImage: (id, formData) => axios.post(`/plants/${id}/upload-image`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const dashboardApi = {
  getDashboard: () => axios.get('/dashboard'),
  getTasks: () => axios.get('/dashboard/tasks')
}

export const weatherApi = {
  getWeatherByCity: (city) => axios.get(`/weather/${encodeURIComponent(city)}`),
  getPlantWeather: (plantId) => axios.get(`/plants/${plantId}/weather`),
  getRecommendation: (plantId) => axios.get(`/weather/recommendations/${plantId}`)
}

export const aiApi = {
  analyzePlantImage: (formData) =>
    axios.post('/ai/analyze-plant', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }),
  getWateringPrediction: (temperature, humidity, plantType) =>
    axios.get('/ai/watering-prediction', {
      params: { temperature, humidity, plantType }
    })
}

export const groqApi = {
  getStatus: () => axios.get('/ai/groq/status'),
  askAssistant: (plantId, payload) => axios.post(`/plants/${plantId}/assistant`, payload),
  getRecommendations: (plantId) => axios.get(`/plants/${plantId}/recommendations`),
  getHealthSummary: (plantId) => axios.get(`/plants/${plantId}/health-summary`),
  inspectDisease: (plantId, payload) => axios.post(`/plants/${plantId}/disease-inspections`, payload),
  getDiseaseHistory: (plantId) => axios.get(`/plants/${plantId}/disease-inspections`)
}

export const sensorApi = {
  registerDevice: (payload) => axios.post('/sensors/register', payload),
  getMyDevices: () => axios.get('/sensors/my-devices'),
  pairDevice: (deviceId, plantId) => axios.post(`/sensors/${deviceId}/pair/${plantId}`),
  unpairDevice: (deviceId) => axios.post(`/sensors/${deviceId}/unpair`),
  getPlantSensorStatus: (plantId) => axios.get(`/plants/${plantId}/sensors`),
  ingestReading: (deviceId, payload) => axios.post(`/sensors/${deviceId}/readings`, payload)
}

export const notificationsApi = {
  getAll: () => axios.get('/notifications'),
  getUnread: () => axios.get('/notifications/unread'),
  getCount: () => axios.get('/notifications/unread/count'),
  clearAll: () => axios.delete('/notifications/clear'),
  markRead: (id) => axios.put(`/notifications/${id}/read`)
}

export const wateringHistoryApi = {
  record: (plantId, payload) => axios.post(`/watering-history/record/${plantId}`, payload),
  getPlantHistory: (plantId) => axios.get(`/watering-history/plant/${plantId}`),
  getUserHistory: () => axios.get('/watering-history'),
  getRecent: () => axios.get('/watering-history/recent')
}
