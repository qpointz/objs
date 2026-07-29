import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AppLayout } from './AppLayout'
import { GraphExplorerPage } from './GraphExplorerPage'
import { ObjectLinterPage } from './ObjectLinterPage'
import { SchemaExplorerPage } from './SchemaExplorerPage'
import { SchemaLinterPage } from './SchemaLinterPage'

export default function App() {
  return (
    <BrowserRouter basename="/ui">
      <Routes>
        <Route element={<AppLayout />}>
          <Route index element={<Navigate to="/graph" replace />} />
          <Route path="graph" element={<GraphExplorerPage />} />
          <Route path="object-linter" element={<ObjectLinterPage />} />
          <Route path="linter" element={<SchemaLinterPage />} />
          <Route path="schemas/:type/:version/lint" element={<SchemaLinterPage />} />
          <Route path="schemas" element={<SchemaExplorerPage />} />
          <Route path="schemas/:type" element={<SchemaExplorerPage />} />
          <Route path="schemas/:type/:version" element={<SchemaExplorerPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
