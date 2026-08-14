import { Navigate, Route, Routes } from 'react-router-dom'
import { AppLayout } from './AppLayout'
import { CollectionBrowsePage } from './CollectionBrowsePage'
import { CollectionFormPage } from './CollectionFormPage'
import { CollectionPortalPage } from './CollectionPortalPage'
import { CollectionsWorkspace } from './CollectionsWorkspace'
import { ObjectDetailPage, ObjectFormPage } from './ObjectPages'
import { SchemaPortalPage } from './SchemaPortalPage'
import { SchemaViewPage } from './SchemaViewPage'
import { SchemasWorkspace } from './SchemasWorkspace'

export function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route element={<CollectionsWorkspace />}>
          <Route index element={<CollectionPortalPage />} />
          <Route path="collections" element={<CollectionPortalPage />} />
          <Route path="collections/new" element={<CollectionFormPage mode="create" />} />
          <Route path="collections/:id" element={<CollectionBrowsePage />} />
          <Route path="collections/:id/edit" element={<CollectionFormPage mode="edit" />} />
          <Route path="collections/:id/objects/new" element={<ObjectFormPage mode="create" />} />
          <Route path="collections/:id/objects/:objectId" element={<ObjectDetailPage />} />
          <Route path="collections/:id/objects/:objectId/edit" element={<ObjectFormPage mode="edit" />} />
        </Route>
        <Route path="schemas" element={<SchemasWorkspace />}>
          <Route index element={<SchemaPortalPage />} />
          <Route path=":type" element={<SchemaViewPage />} />
          <Route path=":type/:version" element={<SchemaViewPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  )
}
