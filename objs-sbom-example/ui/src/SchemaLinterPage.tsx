import { Navigate, useParams, useSearchParams } from 'react-router-dom'
import { schemaDetailPath } from './api'
import type { BoMSchemaNode, BoMSchemaUsage, EdgeRelationRequest } from './types'

export type SchemaExpertDocument = {
  type: string
  version: string
  usages: BoMSchemaUsage[]
  contentSchema: BoMSchemaNode
  allowedRelations?: EdgeRelationRequest[]
}

export function parseSchemaExpertDocument(
  value: unknown,
): { ok: true; value: SchemaExpertDocument } | { ok: false; error: string } {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return { ok: false, error: 'Expert document must be an object' }
  }
  const doc = value as Partial<SchemaExpertDocument>
  if (typeof doc.type !== 'string' || !doc.type.trim()) {
    return { ok: false, error: 'Expert document type must not be blank' }
  }
  if (typeof doc.version !== 'string' || !doc.version.trim()) {
    return { ok: false, error: 'Expert document version must not be blank' }
  }
  if (!Array.isArray(doc.usages) || doc.usages.length === 0) {
    return { ok: false, error: 'Expert document usages must be a non-empty array' }
  }
  if (doc.usages.some((usage) => usage !== 'ENTITY' && usage !== 'EDGE_PROPERTIES')) {
    return { ok: false, error: 'Expert document contains an unsupported usage' }
  }
  if (!doc.contentSchema || typeof doc.contentSchema !== 'object') {
    return { ok: false, error: 'Expert document contentSchema is required' }
  }
  if (doc.allowedRelations != null && !Array.isArray(doc.allowedRelations)) {
    return { ok: false, error: 'Expert document allowedRelations must be an array' }
  }
  return {
    ok: true,
    value: {
      type: doc.type,
      version: doc.version,
      usages: doc.usages,
      contentSchema: doc.contentSchema,
      allowedRelations: doc.allowedRelations ?? [],
    },
  }
}

/** Deprecated route: redirect into the unified Schemas workbench. */
export function SchemaLinterPage() {
  const params = useParams()
  const [searchParams] = useSearchParams()
  const type = params.type ? decodeURIComponent(params.type) : undefined
  const version = params.version ? decodeURIComponent(params.version) : undefined
  const mode = searchParams.get('mode')
  if (!type || !version) {
    return <Navigate to="/schemas/new?kind=object" replace />
  }
  const target = schemaDetailPath(type, version)
  return <Navigate to={mode === 'create-version' ? `${target}?mode=create-version` : target} replace />
}
