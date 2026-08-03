import { Navigate, Route, createBrowserRouter, createRoutesFromElements, RouterProvider } from 'react-router-dom'
import { AppLayout } from './AppLayout'
import { GraphExplorerPage } from './GraphExplorerPage'
import { ObjectLinterPage } from './ObjectLinterPage'
import { SchemaExplorerPage } from './SchemaExplorerPage'
import { SchemaLinterPage } from './SchemaLinterPage'

const router = createBrowserRouter(
  createRoutesFromElements(
    <Route element={<AppLayout />}>
      <Route index element={<Navigate to="/graph" replace />} />
      <Route path="graph" element={<GraphExplorerPage />} />
      <Route path="object-linter" element={<ObjectLinterPage />} />
      <Route path="linter" element={<SchemaLinterPage />} />
      <Route path="schemas/:type/:version/lint" element={<SchemaLinterPage />} />
      <Route path="schemas" element={<SchemaExplorerPage />} />
      <Route path="schemas/:type" element={<SchemaExplorerPage />} />
      <Route path="schemas/:type/:version" element={<SchemaExplorerPage />} />
    </Route>,
  ),
  { basename: '/ui' },
)

export default function App() {
  return <RouterProvider router={router} />
}
