import { Navigate, useParams } from 'react-router-dom'

export function VersionDetailPage() {
  const { id = '', versionId = '' } = useParams()
  return <Navigate to={`/applications/${id}?version=${versionId}`} replace />
}
