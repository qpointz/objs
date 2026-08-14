import { ActionIcon, Group, Skeleton, Text, Tooltip } from '@mantine/core'
import { IconRefresh } from '@tabler/icons-react'
import { useCallback, useEffect, useState, type MouseEvent } from 'react'
import { getCollectionStatistics, type CollectionStatistics } from './api'

const cache = new Map<string, CollectionStatistics>()
const inflight = new Map<string, Promise<CollectionStatistics>>()

function loadStatistics(collectionId: string, force: boolean): Promise<CollectionStatistics> {
  if (!force) {
    const cached = cache.get(collectionId)
    if (cached) return Promise.resolve(cached)
    const pending = inflight.get(collectionId)
    if (pending) return pending
  }
  const request = getCollectionStatistics(collectionId).then((stats) => {
    cache.set(collectionId, stats)
    inflight.delete(collectionId)
    return stats
  })
  inflight.set(collectionId, request)
  return request
}

export function useCollectionStatistics(collectionId: string) {
  const [stats, setStats] = useState<CollectionStatistics | null>(() => cache.get(collectionId) ?? null)
  const [loading, setLoading] = useState(() => !cache.has(collectionId))

  const refresh = useCallback(
    async (force = false) => {
      if (!force && cache.has(collectionId)) {
        setStats(cache.get(collectionId) ?? null)
        setLoading(false)
        return
      }
      setLoading(true)
      try {
        setStats(await loadStatistics(collectionId, force))
      } finally {
        setLoading(false)
      }
    },
    [collectionId],
  )

  useEffect(() => {
    void refresh(false)
  }, [collectionId, refresh])

  return { stats, loading, refresh: () => refresh(true) }
}

export function CollectionObjectCount({ collectionId }: { collectionId: string }) {
  const { stats, loading, refresh } = useCollectionStatistics(collectionId)
  const count = stats?.objectCount

  function onRefresh(e: MouseEvent) {
    e.preventDefault()
    e.stopPropagation()
    void refresh()
  }

  return (
    <Group gap={6} wrap="nowrap">
      {loading || count == null ? (
        <Skeleton height={12} width={72} radius="sm" />
      ) : (
        <Text size="xs" c="dimmed">
          {count} object{count === 1 ? '' : 's'}
        </Text>
      )}
      <Tooltip label="Refresh statistics" withArrow>
        <ActionIcon
          size="xs"
          variant="subtle"
          color="gray"
          aria-label="Refresh statistics"
          onClick={onRefresh}
        >
          <IconRefresh size={12} stroke={1.75} />
        </ActionIcon>
      </Tooltip>
    </Group>
  )
}
