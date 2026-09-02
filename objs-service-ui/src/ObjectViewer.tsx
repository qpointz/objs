import { useState, type ReactNode } from 'react'
import { ActionIcon, Anchor, Group, Skeleton, Stack, Text, Title, Tooltip } from '@mantine/core'
import { IconCheck, IconCopy, IconX } from '@tabler/icons-react'
import { Link } from 'react-router-dom'
import { schemaDetailPath } from './api'
import { EntityPayloadView } from './EntityCardNode'
import { ObjectGraphRowContent } from './ObjectGraphRowContent'
import { ObjectViewerSection } from './ObjectViewerSection'
import { ObjectVersionRowContent } from './ObjectVersionRowContent'
import {
  formatInstanceVersionLabel,
  OBJECT_VERSION_PREVIEW_N,
} from './objectViewerTitle'
import type { ObjectVersionRow } from './objectVersionRows'
import type { BoMGraphHeader, PayloadFieldKind } from './types'
import { objectViewerKvGridStyle } from './objectViewerLayout'

/** Field / annotation keys — smaller than section labels; shared column with payload. */
function MetaRow({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div style={{ ...objectViewerKvGridStyle, rowGap: 4 }}>
      <Text
        size="xs"
        fw={600}
        c="dimmed"
        style={{ lineHeight: 1.35, fontSize: 10 }}
      >
        {label}
      </Text>
      <div style={{ minWidth: 0 }}>{children}</div>
    </div>
  )
}

function EndpointValue({
  label,
  present,
  onGo,
}: {
  label: string
  present: boolean
  onGo: () => void
}) {
  if (present) {
    return (
      <Anchor component="button" type="button" size="sm" onClick={onGo}>
        {label}
      </Anchor>
    )
  }
  return (
    <Text size="sm" c="dimmed" td="line-through" style={{ wordBreak: 'break-word' }}>
      {label} (deleted)
    </Text>
  )
}

function VersionPreviewRow({
  row,
  onClick,
}: {
  row: ObjectVersionRow
  onClick: () => void
}) {
  return (
    <Anchor
      component="button"
      type="button"
      underline="never"
      c="inherit"
      onClick={onClick}
      style={{ display: 'block', width: '100%', textAlign: 'left' }}
    >
      <ObjectVersionRowContent row={row} density="preview" />
    </Anchor>
  )
}

function GraphPreviewRow({
  header,
  onOpenAsContext,
  onOpenInExplorer,
  onEditInComposer,
}: {
  header: BoMGraphHeader
  onOpenAsContext: (graphId: string) => void
  onOpenInExplorer: (graphId: string) => void
  onEditInComposer: (graphId: string) => void
}) {
  return (
    <div data-tour="object-graph-preview-row" style={{ width: '100%' }}>
      <ObjectGraphRowContent
        header={header}
        density="preview"
        onOpenAsContext={onOpenAsContext}
        onOpenInExplorer={onOpenInExplorer}
        onEditInComposer={onEditInComposer}
      />
    </div>
  )
}

export type ObjectViewerRelation = {
  sourceLabel: string
  targetLabel: string
  /** False when endpoint is not in the current canvas / context. */
  sourcePresent: boolean
  targetPresent: boolean
  onSource: () => void
  onTarget: () => void
}

export type ObjectViewerVersionsProps = {
  headVersion?: number | null
  /** Stats loading */
  loading?: boolean
  total?: number
  recent?: ObjectVersionRow[]
  error?: string | null
  onOpenBrowser: () => void
  onSelectVersion: (version: number | null) => void
}

export type ObjectViewerGraphsProps = {
  loading?: boolean
  total?: number
  recent?: BoMGraphHeader[]
  error?: string | null
  onOpenBrowser: () => void
  onOpenAsContext: (graphId: string) => void
  onOpenInExplorer: (graphId: string) => void
  onEditInComposer: (graphId: string) => void
}

export type ObjectViewerProps = {
  title: string
  /** Section label for identity block (Node / Graph). */
  identityLabel?: string
  /** Optional close / clear selection */
  onClose?: () => void
  /** Sticky banner when inspecting a historical version */
  inspectingVersionLabel?: string | null
  type?: string | null
  schemaVersion?: string | null
  id?: string | null
  payload?: Record<string, unknown>
  annotations?: Record<string, string>
  fieldKinds?: Record<string, PayloadFieldKind>
  /** Edge relation (before payload). */
  relation?: ObjectViewerRelation | null
  /** Payload section label (default payload; edges use properties). */
  payloadLabel?: string
  showAnnotations?: boolean
  /** When false, Versions section omitted (e.g. historical inspect). */
  showVersions?: boolean
  versions?: ObjectViewerVersionsProps | null
  /** Live graphs containing this entity (entity inspect only). */
  showGraphs?: boolean
  graphs?: ObjectViewerGraphsProps | null
}

/**
 * Reusable sectioned object details viewer (Note 5 / G-UX-odetail).
 */
export function ObjectViewer({
  title,
  identityLabel = 'Node',
  onClose,
  inspectingVersionLabel,
  type,
  schemaVersion,
  id,
  payload = {},
  annotations = {},
  fieldKinds,
  relation,
  payloadLabel = 'payload',
  showAnnotations = true,
  showVersions = true,
  versions,
  showGraphs = false,
  graphs,
}: ObjectViewerProps) {
  const [copied, setCopied] = useState(false)

  async function copyId() {
    if (!id) return
    try {
      await navigator.clipboard.writeText(id)
      setCopied(true)
      window.setTimeout(() => setCopied(false), 1200)
    } catch {
      // ignore
    }
  }

  const typeValue =
    type && schemaVersion ? (
      <Anchor
        component={Link}
        to={schemaDetailPath(type, schemaVersion)}
        target="_blank"
        rel="noopener noreferrer"
        size="sm"
      >
        {type}@{schemaVersion}
      </Anchor>
    ) : type ? (
      <Text size="sm">{type}</Text>
    ) : (
      <Text size="sm" c="dimmed">
        —
      </Text>
    )

  const versionsTotal = versions?.total ?? 0
  const showVersionsSection =
    showVersions &&
    versions != null &&
    (versions.loading === true || versionsTotal > 0 || (versions.error != null && versions.error !== ''))

  const graphsTotal = graphs?.total ?? 0
  const showGraphsSection =
    showGraphs &&
    graphs != null &&
    (graphs.loading === true || graphsTotal > 0 || (graphs.error != null && graphs.error !== ''))

  return (
    <Stack gap="xs" data-tour="object-viewer">
      <Group justify="space-between" align="flex-start" wrap="nowrap" gap="xs">
        <div style={{ minWidth: 0, flex: 1 }}>
          {inspectingVersionLabel != null && inspectingVersionLabel !== '' && (
            <Text size="xs" c="dimmed" mb={4}>
              Inspecting {inspectingVersionLabel}
            </Text>
          )}
          <Title order={5} style={{ wordBreak: 'break-word' }}>
            {title}
          </Title>
        </div>
        {onClose && (
          <ActionIcon
            size="sm"
            variant="subtle"
            aria-label="Clear selection"
            onClick={onClose}
          >
            <IconX size={14} />
          </ActionIcon>
        )}
      </Group>

      <ObjectViewerSection label={identityLabel}>
        <Stack gap={8}>
          {(type != null || schemaVersion != null) && <MetaRow label="type">{typeValue}</MetaRow>}
          {id != null && (
            <MetaRow label="id">
              <Group gap={4} wrap="nowrap" align="center" style={{ maxWidth: '100%' }}>
                <Text size="xs" ff="monospace" style={{ wordBreak: 'break-all' }}>
                  {id}
                </Text>
                <Tooltip label={copied ? 'Copied' : 'Copy id'} withArrow>
                  <ActionIcon
                    size="xs"
                    variant="subtle"
                    aria-label="Copy id"
                    onClick={() => void copyId()}
                    style={{ flexShrink: 0 }}
                  >
                    {copied ? <IconCheck size={12} /> : <IconCopy size={12} />}
                  </ActionIcon>
                </Tooltip>
              </Group>
            </MetaRow>
          )}
        </Stack>
      </ObjectViewerSection>

      {relation && (
        <ObjectViewerSection label="Relation">
          <Stack gap={8}>
            <MetaRow label="source">
              <EndpointValue
                label={relation.sourceLabel}
                present={relation.sourcePresent}
                onGo={relation.onSource}
              />
            </MetaRow>
            <MetaRow label="target">
              <EndpointValue
                label={relation.targetLabel}
                present={relation.targetPresent}
                onGo={relation.onTarget}
              />
            </MetaRow>
          </Stack>
        </ObjectViewerSection>
      )}

      <ObjectViewerSection label={payloadLabel === 'properties' ? 'Properties' : 'Payload'}>
        <EntityPayloadView
          payload={payload}
          fieldKinds={fieldKinds}
          size="panel"
          showLabel={false}
          denseKeys
        />
      </ObjectViewerSection>

      {showAnnotations && (
        <ObjectViewerSection label="Annotations">
          {Object.keys(annotations).length === 0 ? (
            <Text size="xs" c="dimmed" fs="italic">
              none
            </Text>
          ) : (
            <Stack gap={8}>
              {Object.entries(annotations).map(([k, v]) => (
                <MetaRow key={k} label={k}>
                  <Text size="sm" style={{ wordBreak: 'break-word' }}>
                    {v}
                  </Text>
                </MetaRow>
              ))}
            </Stack>
          )}
        </ObjectViewerSection>
      )}

      {showVersionsSection && versions && (
        <ObjectViewerSection label="Versions">
          <Stack gap={8}>
            {versions.loading ? (
              <Stack gap={6}>
                <Skeleton height={14} width="40%" />
                <Skeleton height={36} />
                <Skeleton height={36} />
              </Stack>
            ) : versions.error ? (
              <Text size="xs" c="red">
                {versions.error}
              </Text>
            ) : (
              <>
                <MetaRow label="version">
                  <Group gap="sm" wrap="nowrap" align="center" justify="space-between">
                    <Text size="sm">{formatInstanceVersionLabel(versions.headVersion)}</Text>
                    <Anchor
                      component="button"
                      type="button"
                      size="sm"
                      onClick={versions.onOpenBrowser}
                      data-tour="object-versions-link"
                      style={{ flexShrink: 0 }}
                    >
                      Versions {'>>'}
                    </Anchor>
                  </Group>
                </MetaRow>
                <Stack gap={8}>
                  {(versions.recent ?? []).slice(0, OBJECT_VERSION_PREVIEW_N).map((row) => (
                    <VersionPreviewRow
                      key={row.version}
                      row={row}
                      onClick={() => versions.onSelectVersion(row.version)}
                    />
                  ))}
                </Stack>
              </>
            )}
          </Stack>
        </ObjectViewerSection>
      )}

      {showGraphsSection && graphs && (
        <ObjectViewerSection label="Graphs">
          <Stack gap={8}>
            {graphs.loading ? (
              <Stack gap={6}>
                <Skeleton height={14} width="40%" />
                <Skeleton height={36} />
                <Skeleton height={36} />
              </Stack>
            ) : graphs.error ? (
              <Text size="xs" c="red">
                {graphs.error}
              </Text>
            ) : (
              <>
                <MetaRow label="count">
                  <Group gap="sm" wrap="nowrap" align="center" justify="space-between">
                    <Text size="sm">{graphs.total ?? 0}</Text>
                    <Anchor
                      component="button"
                      type="button"
                      size="sm"
                      onClick={graphs.onOpenBrowser}
                      data-tour="object-graphs-link"
                      style={{ flexShrink: 0 }}
                    >
                      Graphs {'>>'}
                    </Anchor>
                  </Group>
                </MetaRow>
                <Stack gap={8}>
                  {(graphs.recent ?? []).slice(0, OBJECT_VERSION_PREVIEW_N).map((header) => (
                    <GraphPreviewRow
                      key={header.id}
                      header={header}
                      onOpenAsContext={graphs.onOpenAsContext}
                      onOpenInExplorer={graphs.onOpenInExplorer}
                      onEditInComposer={graphs.onEditInComposer}
                    />
                  ))}
                </Stack>
              </>
            )}
          </Stack>
        </ObjectViewerSection>
      )}
    </Stack>
  )
}
