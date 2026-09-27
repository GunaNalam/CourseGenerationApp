import { useAuth0 } from '@auth0/auth0-react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { LoadingSpinner } from './components/LoadingSpinner'
import { Navbar } from './components/Navbar'
import { AdminPage } from './pages/AdminPage'
import { CoursePage } from './pages/CoursePage'
import { Home } from './pages/Home'
import { Landing } from './pages/Landing'
import { LessonPage } from './pages/LessonPage'
import { SettingsPage } from './pages/SettingsPage'

function App() {
  const { isLoading, isAuthenticated } = useAuth0()

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <LoadingSpinner size="lg" label="Loading…" />
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Landing />
  }

  return (
    <div className="min-h-screen">
      <Navbar />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/courses/:courseId" element={<CoursePage />} />
        <Route path="/courses/:courseId/modules/:moduleId/lessons/:lessonId" element={<LessonPage />} />
        <Route path="/admin" element={<AdminPage />} />
        <Route path="/settings" element={<SettingsPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  )
}

export default App
