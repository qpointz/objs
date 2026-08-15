import { Navigate, Route } from 'react-router-dom'
import { AppLayout } from './AppLayout'
import { ApplicationDetailPage } from './pages/ApplicationDetailPage'
import { ApplicationFormPage } from './pages/ApplicationFormPage'
import { ApplicationsPage } from './pages/ApplicationsPage'
import { AssetDetailPage } from './pages/AssetDetailPage'
import { AssetsPage } from './pages/AssetsPage'
import { PortfoliosPage } from './pages/PortfoliosPage'
import { PortfolioFormPage } from './pages/PortfolioFormPage'
import { PortfolioWorkspace } from './pages/PortfolioWorkspace'
import { SchemaPortalPage } from './SchemaPortalPage'
import { SchemaViewPage } from './SchemaViewPage'
import { SchemasWorkspace } from './SchemasWorkspace'
import { VersionDetailPage } from './pages/VersionDetailPage'

export const appRoutes = (
  <Route element={<AppLayout />}>
    <Route path="/" element={<Navigate to="/applications" replace />} />
    <Route path="/schemas" element={<SchemasWorkspace />}>
      <Route index element={<SchemaPortalPage />} />
      <Route path=":type" element={<SchemaViewPage />} />
      <Route path=":type/:version" element={<SchemaViewPage />} />
    </Route>
    <Route path="/applications" element={<ApplicationsPage />} />
    <Route path="/applications/new" element={<ApplicationFormPage />} />
    <Route path="/applications/assets" element={<AssetsPage />}>
      <Route path=":assetId" element={<AssetDetailPage />} />
    </Route>
    <Route path="/applications/:id" element={<ApplicationDetailPage />} />
    <Route path="/applications/:id/versions/:versionId" element={<VersionDetailPage />} />
    <Route path="/portfolios" element={<PortfoliosPage />} />
    <Route path="/portfolios/new" element={<PortfolioFormPage />} />
    <Route path="/portfolios/:id" element={<PortfolioWorkspace />} />
  </Route>
)
