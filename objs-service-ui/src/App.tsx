import { Navigate, Route, createBrowserRouter, createRoutesFromElements, RouterProvider, useParams } from 'react-router-dom'
import { AppLayout } from './AppLayout'
import { GraphExplorerPage } from './GraphExplorerPage'
import { ObjectLinterPage } from './ObjectLinterPage'
import { ObjectsPage } from './ObjectsPage'
import { PolicyPlayPage } from './PolicyPlayPage'
import { QueryPage } from './QueryPage'
import { SchemaExplorerPage } from './SchemaExplorerPage'
import { SchemaLinterPage } from './SchemaLinterPage'

function RedirectSchemasType() {
  const { type } = useParams()
  return <Navigate to={`/model/${type ?? ''}`} replace />
}

function RedirectSchemasTypeVersion() {
  const { type, version } = useParams()
  return <Navigate to={`/model/${type ?? ''}/${version ?? ''}`} replace />
}

const router = createBrowserRouter(
  createRoutesFromElements(
    <Route element={<AppLayout />}>
      <Route index element={<Navigate to="/explorer" replace />} />
      <Route path="explorer" element={<GraphExplorerPage />} />
      <Route path="objects" element={<ObjectsPage />} />
      <Route path="composer" element={<ObjectLinterPage />} />
      <Route path="query" element={<QueryPage />} />
      <Route path="policy" element={<PolicyPlayPage />} />
      <Route path="model" element={<SchemaExplorerPage />} />
      <Route path="model/:type" element={<SchemaExplorerPage />} />
      <Route path="model/:type/:version" element={<SchemaExplorerPage />} />

      {/* Legacy paths under /workbench */}
      <Route path="graph" element={<Navigate to="/explorer" replace />} />
      <Route path="object-linter" element={<Navigate to="/composer" replace />} />
      <Route path="linter" element={<SchemaLinterPage />} />
      <Route path="schemas/:type/:version/lint" element={<SchemaLinterPage />} />
      <Route path="schemas" element={<Navigate to="/model" replace />} />
      <Route path="schemas/:type" element={<RedirectSchemasType />} />
      <Route path="schemas/:type/:version" element={<RedirectSchemasTypeVersion />} />
    </Route>,
  ),
  { basename: '/workbench' },
)

export default function App() {
  return <RouterProvider router={router} />
}
