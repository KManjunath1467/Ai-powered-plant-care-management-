import React from 'react'
import { NavLink } from 'react-router-dom'
import {
  Home,
  Clipboard,
  Sparkles,
  CloudSun,
  Activity,
  Bot,
  Bell,
  Settings
} from 'lucide-react'

const LinkItem = ({ to, icon: Icon, label }) => (
  <NavLink
    to={to}
    className={({ isActive }) =>
      `flex items-center gap-3 px-3.5 py-2.5 rounded-xl transition-all duration-200 group text-xs font-semibold ${
        isActive
          ? 'bg-gradient-to-r from-emerald-700 to-teal-800 text-white shadow-md'
          : 'text-gray-700 hover:bg-white/80 hover:text-emerald-800'
      }`
    }
  >
    <Icon className="w-4 h-4 flex-shrink-0" />
    <span>{label}</span>
  </NavLink>
)

export default function Sidebar() {
  return (
    <aside className="w-60 p-4 glass-card mr-6 animate-fade-in flex-shrink-0 hidden md:block">
      <div className="space-y-1.5">
        <div className="text-[11px] font-bold text-gray-400 uppercase tracking-wider px-3 py-2">
          Platform Navigation
        </div>
        <LinkItem to="/" icon={Home} label="Dashboard" />
        <LinkItem to="/plants" icon={Clipboard} label="My Plants" />
        <LinkItem to="/identify" icon={Sparkles} label="Identify & Anatomy" />
        <LinkItem to="/weather" icon={CloudSun} label="Live Weather" />
        <LinkItem to="/health-monitoring" icon={Activity} label="Sensor Rods (IoT)" />
        <LinkItem to="/assistant" icon={Bot} label="AI Plant Assistant" />
        <LinkItem to="/notifications" icon={Bell} label="Notifications" />
        <LinkItem to="/profile" icon={Settings} label="Settings & Profile" />
      </div>
    </aside>
  )
}
